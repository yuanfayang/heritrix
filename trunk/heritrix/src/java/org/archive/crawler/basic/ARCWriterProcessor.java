/*
 * ARCWriter
 *
 * $Id$
 *
 * Created on Jun 5, 2003
 *
 * Copyright (C) 2003 Internet Archive.
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
package org.archive.crawler.basic;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;

import org.apache.commons.httpclient.methods.GetMethod;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.settings.SimpleType;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.Processor;
import org.archive.io.arc.ARCConstants;
import org.archive.io.arc.ARCWriter;
import org.archive.io.arc.ARCWriterPool;
import org.archive.util.ArchiveUtils;
import org.xbill.DNS.Record;


/**
 * Processor module for writing the results of successful fetches (and
 * perhaps someday, certain kinds of network failures) to the Internet Archive
 * ARC file format.
 *
 * Assumption is that there is only one of this ARCWriterProcessors per
 * Heritrix instance.
 *
 * @author Parker Thompson
 */
public class ARCWriterProcessor
    extends Processor implements CoreAttributeConstants, ARCConstants
{
    /**
     * Key to use asking settings for compression value.
     */
    public static final String ATTR_COMPRESS = "compress";

    /**
     * Key to use asking settings for prefix value.
     */
    public static final String ATTR_PREFIX = "prefix";

    /**
     * Key to use asking settings for max size value.
     */
    public static final String ATTR_MAX_SIZE_BYTES = "max-size-bytes";

    /**
     * Key to use asking settings for arc path value.
     */
    public static final String ATTR_PATH ="path";

    /**
     * Key to get maximum pool size.
     *
     * This key is for maximum ARC writers active in the pool of ARC writers.
     */
    public static final String ATTR_POOL_MAX_ACTIVE = "pool-max-active";

    /**
     * Key to get maximum wait on pool object before we give up and
     * throw IOException.
     */
    public static final String ATTR_POOL_MAX_WAIT = "pool-max-wait";

    /**
     * Max size we want ARC files to be (bytes).
     *
     * Default is ARCConstants.DEFAULT_MAX_ARC_FILE_SIZE.  Note that ARC
     * files will usually be bigger than maxSize; they'll be maxSize + length
     * to next boundary.
     */
    private int arcMaxSize = DEFAULT_MAX_ARC_FILE_SIZE;

    /**
     * File prefix for ARCs.
     *
     * Default is ARCConstants.DEFAULT_ARC_FILE_PREFIX.
     */
    private String arcPrefix = DEFAULT_ARC_FILE_PREFIX;

    /**
     * Use compression flag.
     *
     * Default is ARCConstants.DEFAULT_COMPRESS.
     */
    private boolean useCompression = DEFAULT_COMPRESS;

    /**
     * Where to drop ARC files.
     */
    private String outputDir = "";

    /**
     * Maximum active elements in the ARCWriterPool.
     */
    private int poolMaximumActive = ARCWriterPool.DEFAULT_MAX_ACTIVE;

    /**
     * Maximum time to wait on pool elements.
     */
    private int poolMaximumWait = ARCWriterPool.DEFAULT_MAXIMUM_WAIT;

    /**
     * Reference to an ARCWriter.
     */
    ARCWriterPool pool = null;


    /**
     * @param name
     */
    public ARCWriterProcessor(String name) {
        super(name, "ARCWriter processor");
        addElementToDefinition(
            new SimpleType(
                ATTR_COMPRESS,
                "Compress arc files",
                new Boolean(useCompression)));
        addElementToDefinition(
            new SimpleType(ATTR_PREFIX, "Prefix", arcPrefix));
        addElementToDefinition(
            new SimpleType(
                ATTR_MAX_SIZE_BYTES,
                "Max size of arc file",
                new Integer(arcMaxSize)));
        addElementToDefinition(
            new SimpleType(ATTR_PATH, "Where to store arc files", outputDir));
        addElementToDefinition(new SimpleType(ATTR_POOL_MAX_ACTIVE,
            "Maximum active ARC writers in pool",
            new Integer(poolMaximumActive)));
        addElementToDefinition(new SimpleType(ATTR_POOL_MAX_WAIT,
            "Maximum time to wait on ARC writer pool element (milliseconds)",
            new Integer(poolMaximumWait)));
    }

    public void initialize(CrawlController c) throws AttributeNotFoundException
    {
        super.initialize(c);

        // readConfiguration populates settings used creating ARCWriter.
        try {
            readConfiguration();
        } catch (MBeanException e) {
            throw new AttributeNotFoundException(e.getMessage());
        } catch (ReflectionException e) {
            throw new AttributeNotFoundException(e.getMessage());
        }

        try
        {
            // Set up the pool of ARCWriters.
            this.pool = new ARCWriterPool(new File(outputDir), this.arcPrefix,
                this.useCompression, poolMaximumActive, poolMaximumWait);
        }

        catch (IOException e)
        {
            e.printStackTrace();
            // TODO: This is critical failure.  Should be let out.
        }
    }

    protected void readConfiguration()
        throws AttributeNotFoundException, MBeanException, ReflectionException {
        // set up output directory
        setUseCompression(
            ((Boolean) getAttribute(ATTR_COMPRESS)).booleanValue());
        setArcPrefix((String) getAttribute(ATTR_PREFIX));
        setArcMaxSize(((Integer) getAttribute(ATTR_MAX_SIZE_BYTES)).intValue());
        setOutputDir((String) getAttribute(ATTR_PATH));
        setPoolMaximumActive(
            ((Integer)getAttribute(ATTR_POOL_MAX_ACTIVE)).intValue());
        setPoolMaximumWait(
            ((Integer)getAttribute(ATTR_POOL_MAX_WAIT)).intValue());
    }

    /**
     * Takes a CrawlURI and generates an arc record, writing it to disk.
     *
     * Currently this method understands the following uri types: dns, http.
     *
     * @param curi CrawlURI to process.
     */
    protected void innerProcess(CrawlURI curi)
    {
        // If failure, or we haven't fetched the resource yet, return
        if (curi.getFetchStatus() <= 0) {
            return;
        }

        // Find the write protocol and write this sucker
        String scheme = curi.getUURI().getScheme();

        try
        {
            if (scheme.equals("dns")) {
                writeDns(curi);
            } else if (scheme.equals("http")) {
                writeHttp(curi);
            }
        }
        catch (IOException e)
        {
            curi.addLocalizedError(this.getName(), e, "WriteARCRecord");
        }
    }

    protected void writeHttp(CrawlURI curi)
        throws IOException
    {
        if (curi.getFetchStatus() <= 0)
        {
            // Error; do not write to ARC (for now)
            return;
        }

        GetMethod get = (GetMethod) curi.getAList().
            getObject("http-transaction");
        if (get == null)
        {
            // Some error occurred; nothing to write.
            // TODO: capture some network errors in the ARC file for posterity
            return;
        }

        int recordLength = (int)curi.getContentSize();
        if (recordLength == 0)
        {
            // Write nothing.
            return;
        }

        ARCWriter writer = this.pool.borrowARCWriter();
        try
        {
            writer.write(curi.getURIString(), curi.getContentType(),
               curi.getServer().getHost().getIP().getHostAddress(),
               curi.getAList().getLong(A_FETCH_BEGAN_TIME), recordLength,
               get.getHttpRecorder().getRecordedInput().getReplayInputStream());
        }
        finally
        {
            this.pool.returnARCWriter(writer);
        }
    }

    protected void writeDns(CrawlURI curi)
        throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // Start the record with a 14-digit date per RFC 2540
        long ts = curi.getAList().getLong(A_FETCH_BEGAN_TIME);
        byte[] fetchDate = ArchiveUtils.get14DigitDate(ts).getBytes();
        baos.write(fetchDate);
        // Don't forget the newline
        baos.write("\n".getBytes());
        int recordLength = fetchDate.length + 1;
        Record[] rrSet = (Record[]) curi.getAList().
            getObject(A_RRECORD_SET_LABEL);
        if (rrSet != null)
        {
            for (int i = 0; i < rrSet.length; i++)
            {
                byte[] record = rrSet[i].toString().getBytes();
                recordLength += record.length;
                baos.write(record);
                // Add the newline between records back in
                baos.write("\n".getBytes());
                recordLength += 1;
            }
        }

        ARCWriter writer = this.pool.borrowARCWriter();
        try
        {
            writer.write(curi.getURIString(), curi.getContentType(),
               curi.getServer().getHost().getIP().getHostAddress(),
               curi.getAList().getLong(A_FETCH_BEGAN_TIME), recordLength, baos);
        }
        finally
        {
            this.pool.returnARCWriter(writer);
        }

        // Save the calculated contentSize for logging purposes
        // TODO handle this need more sensibly
        curi.setContentSize((long)recordLength);
    }

    // getters and setters
    public int getArcMaxSize() {
        return arcMaxSize;
    }

    public String getArcPrefix() {
            return arcPrefix;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public void setArcMaxSize(int i) {
        arcMaxSize = i;
    }

    public void setArcPrefix(String buffer) {
        arcPrefix = buffer;
    }

    public void setUseCompression(boolean use) {
        useCompression = use;
    }

    public boolean useCompression() {
        return useCompression;
    }

    public void setOutputDir(String buffer) {
        // make sure it's got a trailing file.separator so the
        // dir is not treated as a file prefix
        if ((buffer.length() > 0) && !buffer.endsWith(File.separator)) {
            buffer = new String(buffer + File.separator);
        }

        File newDir = new File(controller.getDisk(), buffer);

        if (!newDir.exists()) {
            try {
                newDir.mkdirs();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        outputDir = newDir.getAbsolutePath()+ File.separatorChar;
    }

    /**
     * @return Returns the poolMaximumActive.
     */
    public int getPoolMaximumActive()
    {
        return this.poolMaximumActive;
    }

    /**
     * @param poolMaximumActive The poolMaximumActive to set.
     */
    public void setPoolMaximumActive(int poolMaximumActive)
    {
        this.poolMaximumActive = poolMaximumActive;
    }

    /**
     * @return Returns the poolMaximumWait.
     */
    public int getPoolMaximumWait()
    {
        return this.poolMaximumWait;
    }

    /**
     * @param poolMaximumWait The poolMaximumWait to set.
     */
    public void setPoolMaximumWait(int poolMaximumWait)
    {
        this.poolMaximumWait = poolMaximumWait;
    }
}
