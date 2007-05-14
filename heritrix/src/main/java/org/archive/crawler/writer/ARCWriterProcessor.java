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
package org.archive.crawler.writer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.archive.crawler.datamodel.CoreAttributeConstants.*;
import org.archive.crawler.datamodel.CrawlURI;
import static org.archive.processors.fetcher.FetchStatusCodes.*;
import org.archive.crawler.framework.WriterPoolProcessor;
import org.archive.io.ReplayInputStream;
import org.archive.io.WriterPoolMember;
import org.archive.io.WriterPoolSettings;
import org.archive.io.arc.ARCWriter;
import org.archive.io.arc.ARCWriterPool;
import org.archive.processors.ProcessResult;
import org.archive.processors.ProcessorURI;
import org.archive.state.Global;
import org.archive.state.Key;
import org.archive.state.KeyMaker;
import org.archive.state.KeyManager;


/**
 * Processor module for writing the results of successful fetches (and
 * perhaps someday, certain kinds of network failures) to the Internet Archive
 * ARC file format.
 *
 * Assumption is that there is only one of these ARCWriterProcessors per
 * Heritrix instance.
 *
 * @author Parker Thompson
 */
public class ARCWriterProcessor extends WriterPoolProcessor {

    private static final long serialVersionUID = 3L;

    private static final Logger logger = 
        Logger.getLogger(ARCWriterProcessor.class.getName());

    /**
     * Where to save files. Supply absolute or relative path. If relative, files
     * will be written relative to the order.disk-path setting. If more than one
     * path specified, we'll round-robin dropping files to each. This setting is
     * safe to change midcrawl (You can remove and add new dirs as the crawler
     * progresses).
     */
    @Global
    final public static Key<List<String>> PATH = makePath();

    
    static {
        KeyManager.addKeys(ARCWriterProcessor.class);
    }
        
        
    /**
     * Default path list.
     */
    private static final String [] DEFAULT_PATH = {"arcs"};


    
    
    /**
     * @param name Name of this writer.
     */
    public ARCWriterProcessor() {
    }
    
    protected String [] getDefaultPath() {
    	return DEFAULT_PATH;
	}

    @Override
    protected void setupPool(AtomicInteger serialNo) {
        int maxActive = getMaxActive();
        int maxWait = getMaxWait();
        WriterPoolSettings wps = getWriterPoolSettings();
        setPool(new ARCWriterPool(serialNo, wps, maxActive, maxWait));
    }


    protected boolean shouldProcess(ProcessorURI uri) {
        if (!(uri instanceof CrawlURI)) {
            return false;
        }
        
        CrawlURI curi = (CrawlURI)uri;
        // If failure, or we haven't fetched the resource yet, return
        if (curi.getFetchStatus() <= 0) {
            return false;
        }
        
        // If no content, don't write record.
        long recordLength = curi.getContentSize();
        if (recordLength <= 0) {
            // Write nothing.
            return false;
        }

        return true;
    }
    
    
    /**
     * Writes a CrawlURI and its associated data to store file.
     *
     * Currently this method understands the following uri types: dns, http, 
     * and https.
     *
     * @param curi CrawlURI to process.
     */
    protected ProcessResult innerProcessResult(ProcessorURI puri) {
        CrawlURI curi = (CrawlURI)puri;
        
        int recordLength = (int)curi.getContentSize();
        
        String scheme = curi.getUURI().getScheme().toLowerCase();
        try {
            // TODO: Since we made FetchDNS work like FetchHTTP, IF we
            // move test for success of different schemes -- DNS, HTTP(S) and 
            // soon FTP -- up into CrawlURI#isSuccess (Have it read list of
            // supported schemes from heritrix.properties and cater to each's
            // notions of 'success' appropriately), then we can collapse this
            // if/else into a lone if (curi.isSuccess).  See WARCWriter for
            // an example.
            if ((scheme.equals("dns") &&
            		curi.getFetchStatus() == S_DNS_SUCCESS)) {
            	InputStream is = curi.getRecorder().getRecordedInput().
            		getReplayInputStream();
                write(curi, recordLength, is,
                    (String)curi.getData().get(A_DNS_SERVER_IP_LABEL));
            } else if ((scheme.equals("http") || scheme.equals("https")) &&
            		curi.getFetchStatus() > 0 && curi.isHttpTransaction()) {
                InputStream is = curi.getRecorder().getRecordedInput().
            		getReplayInputStream();
                write(curi, recordLength, is, getHostAddress(curi));
            } else if (scheme.equals("ftp") && (curi.getFetchStatus() == 200)) {
                InputStream is = curi.getRecorder().getRecordedInput().
                 getReplayInputStream();
                write(curi, recordLength, is, getHostAddress(curi));
            } else {
                logger.info("This writer does not write out scheme " + scheme +
                    " content");
            }
         } catch (IOException e) {
            curi.getNonFatalFailures().add(e);
            logger.log(Level.SEVERE, "Failed write of Record: " +
                curi.toString(), e);
        }
        return ProcessResult.PROCEED;
    }
    
    protected ProcessResult write(CrawlURI curi, int recordLength, 
            InputStream in, String ip)
    throws IOException {
        WriterPoolMember writer = getPool().borrowFile();
        long position = writer.getPosition();
        // See if we need to open a new file because we've exceeed maxBytes.
        // Call to checkFileSize will open new file if we're at maximum for
        // current file.
        writer.checkSize();
        if (writer.getPosition() != position) {
            // We just closed the file because it was larger than maxBytes.
            // Add to the totalBytesWritten the size of the first record
            // in the file, if any.
            setTotalBytesWritten(getTotalBytesWritten() +
            	(writer.getPosition() - position));
            position = writer.getPosition();
        }
        
        ARCWriter w = (ARCWriter)writer;
        try {
            if (in instanceof ReplayInputStream) {
                w.write(curi.toString(), curi.getContentType(),
                    ip, curi.getFetchBeginTime(),
                    recordLength, (ReplayInputStream)in);
            } else {
                w.write(curi.toString(), curi.getContentType(),
                    ip, curi.getFetchBeginTime(),
                    recordLength, in);
            }
        } catch (IOException e) {
            // Invalidate this file (It gets a '.invalid' suffix).
            getPool().invalidateFile(writer);
            // Set the writer to null otherwise the pool accounting
            // of how many active writers gets skewed if we subsequently
            // do a returnWriter call on this object in the finally block.
            writer = null;
            throw e;
        } finally {
            if (writer != null) {
            	setTotalBytesWritten(getTotalBytesWritten() +
            	     (writer.getPosition() - position));
                getPool().returnFile(writer);
            }
        }
        return checkBytesWritten(curi);
    }


    @Override
    protected Key<List<String>> getPathKey() {
        return PATH;
    }


    private static Key<List<String>> makePath() {
        KeyMaker<List<String>> km = KeyMaker.makeList(String.class);
        km.def = Collections.singletonList("arcs");
        return km.toKey();
    }

}
