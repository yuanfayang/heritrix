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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.NoSuchElementException;
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
     * Constructor
     *
     * Makes a pool w/ a maximum of DEFAULT_MAX_ACTIVE ARCWriters.
     *
     * @param arcsDir Directory we dump ARC files to.
     * @param prefix ARC file prefix to use.
     * @throws IOException Passed directory is not writeable or we were unable
     * to create the directory.
     */
    public ARCWriterPool(File arcsDir, String prefix)
        throws IOException
    {
        this(arcsDir, prefix, null, ARCConstants.DEFAULT_COMPRESS,
            ARCConstants.DEFAULT_MAX_ARC_FILE_SIZE, null, DEFAULT_MAX_ACTIVE,
            DEFAULT_MAXIMUM_WAIT);
    }

    /**
     * Constructor
     *
     * Makes a pool w/ a maximum of DEFAULT_MAX_ACTIVE ARCWriters.
     *
     * @param arcsDir Directory we dump ARC files to.
     * @param prefix ARC file prefix to use.
     * @param compress Whether to compress the ARCs made.
     * @throws IOException Passed directory is not writeable or we were unable
     * to create the directory.
     */
    public ARCWriterPool(File arcsDir, String prefix, boolean compress)
        throws IOException
    {
        this(arcsDir, prefix, null, compress,
            ARCConstants.DEFAULT_MAX_ARC_FILE_SIZE, null,
            DEFAULT_MAX_ACTIVE, DEFAULT_MAXIMUM_WAIT);
    }

    /**
     * Constructor
     *
     * @param arcsDir Directory we dump ARC files to.
     * @param prefix ARC file prefix to use.
     * @param compress Whether to compress the ARCs made.
     * @param arcMaxSize Maximum size for arcs.
     * @param maxActive Maximum active ARCWriters.  Tactic is to block waiting
     * a maximum of MAXIMUM_WAIT till an ARC comes available.
     * @param maxWait Time to wait on an ARCWriter when pool is all checked
     * out (Milliseconds).
     * @throws IOException Passed directory is not writeable or we were unable
     * to create the directory.
     */
    public ARCWriterPool(File arcsDir, String prefix, boolean compress,
            int arcMaxSize, int maxActive, int maxWait) throws IOException {
        this(arcsDir, prefix, null, compress, arcMaxSize, null, maxActive,
            maxWait);
    }

    /**
     * Constructor
     *
     * @param arcsDir Directory we dump ARC files to.
     * @param prefix ARC file prefix to use.
     * @param suffix Suffix to tag on to arc file names.  May be null.  If
     * '${HOSTNAME}' will interpolate hostname.
     * @param compress Whether to compress the ARCs made.
     * @param arcMaxSize Maximum size for arcs.
     * @param metadata Arc file meta data.  Can be null.  Is list of File and/or
     * String objects.
     * @param maxActive Maximum active ARCWriters.  Tactic is to block waiting
     * a maximum of MAXIMUM_WAIT till an ARC comes available.
     * @param maxWait Time to wait on an ARCWriter when pool is all checked
     * out (Milliseconds).
     * @throws IOException Passed directory is not writeable or we were unable
     * to create the directory.
     */
    public ARCWriterPool(File arcsDir, String prefix, String suffix,
            boolean compress, int arcMaxSize, List metadata, int maxActive,
            int maxWait)
        throws IOException
    {        
        logger.fine("Configuration: prefix=" + prefix +
                ", suffix=" + suffix +
                ", compress=" + compress +
                ", maxSize=" + arcMaxSize +
                ", maxActive=" + maxActive +
                ", maxWait=" + maxWait);
        this.pool = new GenericObjectPool(
            new ARCWriterFactory(arcsDir, prefix, suffix, compress, arcMaxSize,
                metadata),
            maxActive, GenericObjectPool.WHEN_EXHAUSTED_BLOCK, maxWait);
    }

    /**
     * Check out an ARCWriter from the pool.
     *
     * This method must be answered by a call to
     * {@link #returnARCWriter(ARCWriter)}.
     *
     * @return An ARCWriter checked out of a pool of ARCWriters.
     * @throws IOException Problem getting ARCWriter from pool (Converted
     * from Exception to IOException so this pool can live as a good
     * citizen down in depths of ARCSocketFactory).
     * @throws NoSuchElementException If we time out waiting on a pool member.
     */
    public ARCWriter borrowARCWriter()
        throws IOException
    {
        ARCWriter writer = null;
        for (int i = 0; writer == null; i++) {
        long waitStart = System.currentTimeMillis();
        try {
            writer = (ARCWriter)this.pool.borrowObject();
            logger.fine("Borrowed " + writer + " (Pool State: " +
                getPoolState(waitStart) + ").");
        } catch(NoSuchElementException e) {
            // Let this exception out.  Unit test at least depends on it.
            // Log current state of the pool.
            logger.severe(e.getMessage() + ": Retry #" + i + " of " +
                " max of " + this.arbitraryRetryMax +
                ": NSEE Pool State: " + getPoolState(waitStart));
            if (i >= this.arbitraryRetryMax) {
                throw e;
            }
        } catch(Exception e) {
            // Convert.
            logger.severe(e.getMessage() + ": E Pool State: " +
                getPoolState(waitStart));
            throw new IOException("Failed getting ARCWriter from pool: " +
                e.getMessage());
        }
        }
        return writer;
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
        throws UnsupportedOperationException
    {
        return this.pool.getNumActive();
    }

    /**
     * @return Number of ARCWriters still in the ARCWriter pool.
     * @throws java.lang.UnsupportedOperationException
     */
    public int getNumIdle()
        throws UnsupportedOperationException
    {
        return this.pool.getNumIdle();
    }

    /**
     * @param writer ARCWriter to return to the pool.
     * @throws IOException Problem getting ARCWriter from pool (Converted
     * from Exception to IOException so this pool can live as a good
     * citizen down in depths of ARCSocketFactory).
     */
    public void returnARCWriter(ARCWriter writer)
        throws IOException
    {
        try
        {
            logger.fine("Returned " + writer);
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
         * Directory into which we drop ARC files.
         */
        private File arcsDir = null;

        /**
         * The prefix to give new arc files.
         */
        private String prefix = null;

        /**
         * Compress ARC files.
         */
        private boolean compress = ARCConstants.DEFAULT_COMPRESS;

        /**
         * Arc suffix.
         */
        private final String suffix;

        /**
         * Maximum size for arc.
         */
		private final int arcMaxSize;

        /**
         * Value to interpolate with actual hostname.
         */
        private static final String HOSTNAME_VARIABLE = "${HOSTNAME}";

        /**
         * Arc file meta data list.
         *
         * Can be null.  Else list of string and/or file objects.
         */
        private final List metadata;


        /**
         * Constructor
         *
         * @param arcsDir Directory we drop ARC files into.
         * @param prefix ARC file prefix to use.
         * @param compress True if ARC files should be compressed.
         * @param arcMaxSize Maximum size for arc file.
         * @param suffix Suffix to tag on to arc file names.  May be null.  If
         * '${HOSTNAME}' will interpolate hostname.
         * @param metadata Arc file meta data.  Can be null.  Is list of File
         * and/or String objects.
         *
         * @throws IOException Passed directory is not writeable or we were
         * unable to create the directory.
         */
        public ARCWriterFactory(File arcsDir, String prefix, String suffix,
                boolean compress, int arcMaxSize, List metadata)
            throws IOException
        {
            super();
            this.arcsDir = ArchiveUtils.ensureWriteableDirectory(arcsDir);
            this.prefix = prefix;
            this.compress = compress;
            this.arcMaxSize = arcMaxSize;
            if (suffix != null && suffix.trim().equals(HOSTNAME_VARIABLE)) {
                String str = "localhost.localdomain";
                try {
                    str = InetAddress.getLocalHost().getHostName();
                } catch (UnknownHostException ue) {
                    logger.severe("Failed getHostAddress for this host: " + ue);
                }
                suffix = str;
            }
            this.suffix = suffix;
            this.metadata = metadata;
        }

        public Object makeObject() throws Exception
        {
            return new ARCWriter(this.arcsDir, this.prefix, this.suffix,
                this.compress, this.arcMaxSize, this.metadata);
        }

        public void destroyObject(Object arcWriter) throws Exception
        {
            ((ARCWriter)arcWriter).close();
            super.destroyObject(arcWriter);
        }
    }
}
