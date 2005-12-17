/* ARCWriterManager
 *
 * $Id$
 *
 * Created on Jan 22, 2004
 *
 * Copyright (C) 2004 Internet Archive.
 *
 * This file is part of the Heritrix web crawler (crawler.archive.org).
 *
 * Heritrix is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * any later version.
 *
 * Heritrix is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with Heritrix; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.archive.io.arc;

import java.io.File;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.archive.util.ArchiveUtils;


/**
 * A pool of ARCWriters.
 *
 * @author stack
 */
public class ARCWriterPool {
    // Be robust against trivial implementation changes
    private static final long serialVersionUID =
        ArchiveUtils.classnameBasedUID(ARCWriterPool.class, 1);

    /**
     * Logger instance used by this class.
     */
    static final Logger logger =
        Logger.getLogger(ARCWriterPool.class.getName());

    /**
     * Default maximum active number of ARCWriters in the pool.
     */
    public static final int DEFAULT_MAX_ACTIVE = 5;

    /**
     * Maximum time to wait on a free ARCWriter.
     */
    public static final int DEFAULT_MAXIMUM_WAIT = 1000 * 60 * 5;

    /**
     * Don't enforce a maximum number of idle instances in pool.
     * (To do so means GenericObjectPool will close ARCWriters
     * prematurely.)
     */
    private static final int NO_MAX_IDLE = -1;
    
    /**
     * Pool instance.
     */
    private GenericObjectPool pool = null;
    
    /**
     * Retry getting an ARC on fail the below arbitrary amount of times.
     * This facility is not configurable.  If we fail this many times
     * getting an ARC writer, something is seriously wrong.
     */
    private final int arbitraryRetryMax = 10;
    
    /**
     * Instance of arc writer settings.
     */
    private final ARCWriterSettings settings;
    
    private static final String INVALID_SUFFIX = ".invalid";
    

    /**
     * Constructor
     *
     * @param settings Settings for this pool.
     * @param poolMaximumActive
     * @param poolMaximumWait
     */
    public ARCWriterPool(final ARCWriterSettings settings,
            final int poolMaximumActive, final int poolMaximumWait) {
        logger.info("Initial configuration:" +
                " prefix=" + settings.getArcPrefix() +
                ", suffix=" + settings.getArcSuffix() +
                ", compress=" + settings.isCompressed() +
                ", maxSize=" + settings.getArcMaxSize() +
                ", maxActive=" + poolMaximumActive +
                ", maxWait=" + poolMaximumWait);
        this.settings = settings;
        this.pool = new GenericObjectPool(new ARCWriterFactory(),
            poolMaximumActive,
            GenericObjectPool.WHEN_EXHAUSTED_BLOCK,
            poolMaximumWait, NO_MAX_IDLE);
    }
    
    public void close() {
        this.pool.clear();
    }

    /**
     * @return Returns the settings.
     */
    public ARCWriterSettings getSettings() {
        return this.settings;
    }
    
    /**
     * Check out an ARCWriter from the pool.
     * 
     * This method must be answered by a call to
     * {@link #returnARCWriter(ARCWriter)}.
     * 
     * @return An ARCWriter checked out of a pool of ARCWriters.
     * @throws IOException Problem getting ARCWriter from pool (Converted
     * from Exception to IOException so this pool can live as a good citizen
     * down in depths of ARCSocketFactory).
     * @throws NoSuchElementException If we time out waiting on a pool member.
     */
    public ARCWriter borrowARCWriter()
    throws IOException {
        ARCWriter writer = null;
        for (int i = 0; writer == null; i++) {
            long waitStart = System.currentTimeMillis();
            try {
                writer = (ARCWriter)this.pool.borrowObject();
                if (logger.getLevel() == Level.FINE) {
                    logger.fine("Borrowed " + writer + " (Pool State: "
                        + getPoolState(waitStart) + ").");
                }
            } catch (NoSuchElementException e) {
                // Let this exception out. Unit test at least depends on it.
                // Log current state of the pool.
                logger.severe(e.getMessage() + ": Retry #" + i + " of "
                    + " max of " + this.arbitraryRetryMax
                    + ": NSEE Pool State: " + getPoolState(waitStart));
                if (i >= this.arbitraryRetryMax) {
                    throw e;
                }
            } catch (Exception e) {
                // Convert.
                logger.severe(e.getMessage() + ": E Pool State: " +
                    getPoolState(waitStart));
                throw new IOException("Failed getting ARCWriter from pool: " +
                    e.getMessage());
            }
        }
        return writer;
    }
    
    public void invalidateARCWriter(ARCWriter writer)
    throws IOException {
        try {
            this.pool.invalidateObject(writer);
        } catch (Exception e) {
            // Convert exception.
            throw new IOException(e.getMessage());
        }
        // It'll have been closed.  Rename with an '.invalid' suffix so it
        // gets attention.
        writer.getArcFile().renameTo(
            new File(writer.getArcFile().getAbsoluteFile() + INVALID_SUFFIX));
    }
    
    /**
     * @return State of the pool string
     */
    protected String getPoolState() {
        return getPoolState(-1);
    }
    
    /**
     * @param startTime If we are passed a start time, we'll add difference
     * between it and now to end of string.  Pass -1 if don't want this
     * added to end of state string.
     * @return State of the pool string
     */
    protected String getPoolState(long startTime) {
        StringBuffer buffer = new StringBuffer("Active ");
        buffer.append(getNumActive());
        buffer.append(" of max ");
        buffer.append(this.pool.getMaxActive());
        buffer.append(", idle ");
        buffer.append(this.pool.getNumIdle());
        if (startTime != -1) {
            buffer.append(", time ");
            buffer.append(System.currentTimeMillis() - startTime);
            buffer.append("ms of max ");
            buffer.append(this.pool.getMaxWait());
            buffer.append("ms");
        }
        return buffer.toString();
    }

    /**
     * @return Number of ARCWriters checked out of the ARCWriter pool.
     * @throws java.lang.UnsupportedOperationException
     */
    public int getNumActive()
    throws UnsupportedOperationException {
        return this.pool.getNumActive();
    }

    /**
     * @return Number of ARCWriters still in the ARCWriter pool.
     * @throws java.lang.UnsupportedOperationException
     */
    public int getNumIdle()
    throws UnsupportedOperationException {
        return this.pool.getNumIdle();
    }

    /**
     * @param writer ARCWriter to return to the pool.
     * @throws IOException Problem getting ARCWriter from pool (Converted
     * from Exception to IOException so this pool can live as a good
     * citizen down in depths of ARCSocketFactory).
     */
    public void returnARCWriter(ARCWriter writer)
    throws IOException {
        try {
            if (logger.getLevel() == Level.FINE) {
                logger.fine("Returned " + writer);
            }
            this.pool.returnObject(writer);
        }
        catch(Exception e)
        {
            throw new IOException("Failed restoring ARCWriter to pool: " +
                    e.getMessage());
        }
    }
    
    /**
     * Factory that creates ARCWriters.
     *
     * @author stack
     * @see ARCWriterPool
     */
    private class ARCWriterFactory extends BasePoolableObjectFactory {
        /**
         * Constructor
         */
        public ARCWriterFactory() {
            super();
        }

        public Object makeObject() throws Exception {
            return new ARCWriter(getSettings());
        }

        public void destroyObject(Object arcWriter)
        throws Exception {
            ((ARCWriter)arcWriter).close();
            super.destroyObject(arcWriter);
        }
    }
}
