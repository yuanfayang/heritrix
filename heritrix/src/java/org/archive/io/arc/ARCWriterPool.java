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
import java.util.logging.Logger;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.archive.util.ArchiveUtils;


/**
 * A pool of ARCWriters.
 *
 * @author stack
 */
public class ARCWriterPool
{
    /**
     * Default maximum active number of ARCWriters in the pool.
     */
    public static final int DEFAULT_MAX_ACTIVE = 3;

    /**
     * Maximum time to wait on a free ARCWriter.
     */
    public static final int DEFAULT_MAXIMUM_WAIT = 1000 * 60 * 5;

    /**
     * Pool instance.
     */
    private ObjectPool pool = null;

    /**
     * Logger instance used by this class.
     */
    private static Logger logger =
        Logger.getLogger("org.archive.io.arc.ARCWriterPool");


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
        this(arcsDir, prefix, ARCConstants.DEFAULT_COMPRESS, DEFAULT_MAX_ACTIVE,
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
        this(arcsDir, prefix, compress, DEFAULT_MAX_ACTIVE,
            DEFAULT_MAXIMUM_WAIT);
    }

    /**
     * Constructor
     *
     * @param arcsDir Directory we dump ARC files to.
     * @param prefix ARC file prefix to use.
     * @param compress Whether to compress the ARCs made.
     * @param maxActive Maximum active ARCWriters.  Tactic is to block waiting
     * a maximum of MAXIMUM_WAIT till an ARC comes available.
     * @param maxWait Time to wait on an ARCWriter when pool is all checked
     * out (Milliseconds).
     * @throws IOException Passed directory is not writeable or we were unable
     * to create the directory.
     */
    public ARCWriterPool(File arcsDir, String prefix, boolean compress,
            int maxActive, int maxWait)
        throws IOException
    {
        this.pool = new GenericObjectPool(
            new ARCWriterFactory(arcsDir, prefix, compress),
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
        try {
            writer = (ARCWriter)this.pool.borrowObject();
            logger.fine("Borrowed " + writer + " (" + getNumActive() +
                " active).");
        } catch(NoSuchElementException e) {
            // Let this exception out.  Unit test at least depends on it.
            throw e;
        } catch(Exception e) {
            // Convert.
            throw new IOException("Failed getting ARCWriter from pool: " +
                e.getMessage());
        }
        return writer;
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
    public class ARCWriterFactory
        extends BasePoolableObjectFactory
    {
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
         * Constructor
         *
         * @param arcsDir Directory we drop ARC files into.
         * @param prefix ARC file prefix to use.
         * @param compress True if ARC files should be compressed.
         *
         * @throws IOException Passed directory is not writeable or we were unable
         * to create the directory.
         */
        public ARCWriterFactory(File arcsDir, String prefix, boolean compress)
            throws IOException
        {
            super();
            this.arcsDir = ArchiveUtils.ensureWriteableDirectory(arcsDir);
            this.prefix = prefix;
            this.compress = compress;
        }

        /* (non-Javadoc)
         * @see org.apache.commons.pool.PoolableObjectFactory#makeObject()
         */
        public Object makeObject() throws Exception
        {
            return new ARCWriter(this.arcsDir, this.prefix, this.compress,
                ARCConstants.DEFAULT_MAX_ARC_FILE_SIZE);
        }

        /* (non-Javadoc)
         * @see org.apache.commons.pool.PoolableObjectFactory#destroyObject(java.lang.Object)
         */
        public void destroyObject(Object arcWriter) throws Exception
        {
            ((ARCWriter)arcWriter).close();
            super.destroyObject(arcWriter);
        }
    }
}
