/* $Id$
 *
 * Created on August 1st, 2006.
 *
 * Copyright (C) 2006 Internet Archive.
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.processors.fetcher.FetchStatusCodes;
import org.archive.crawler.framework.WriterPoolProcessor;
import org.archive.io.WriterPoolMember;
import org.archive.io.WriterPoolSettings;
import org.archive.io.warc.ExperimentalWARCWriter;
import org.archive.io.warc.WARCConstants;
import org.archive.io.warc.WARCWriterPool;
import org.archive.processors.ProcessResult;
import org.archive.processors.ProcessorURI;
import org.archive.processors.extractor.Link;
import org.archive.state.Global;
import org.archive.state.Key;
import org.archive.state.KeyManager;
import org.archive.uid.GeneratorFactory;
import org.archive.util.ArchiveUtils;
import org.archive.util.anvl.ANVLRecord;


/**
 * Experimental WARCWriterProcessor.
 * 
 * @author stack
 */
public class ExperimentalWARCWriterProcessor extends WriterPoolProcessor
implements CoreAttributeConstants, 
FetchStatusCodes, WARCConstants {

    private static final long serialVersionUID = 3L;

    private static final Logger logger = 
        Logger.getLogger(ExperimentalWARCWriterProcessor.class.getName());


    /**
     * Where to save files. Supply absolute or relative path. If relative, files
     * will be written relative to the order.disk-path setting. If more than one
     * path specified, we'll round-robin dropping files to each. This setting is
     * safe to change midcrawl (You can remove and add new dirs as the crawler
     * progresses).
     */
    @Global
    final public static Key<List<String>> PATH = 
        Key.make(Collections.singletonList("warcs"));

    
    static {
        KeyManager.addKeys(ExperimentalWARCWriterProcessor.class);
    }
    
    
    /**
     * @param name Name of this writer.
     */
    public ExperimentalWARCWriterProcessor() {
        super();
    }


    @Override
    protected void setupPool(AtomicInteger serialNo) {
        int maxActive = getMaxActive();
        int maxWait = getMaxWait();
        WriterPoolSettings wps = getWriterPoolSettings();
        setPool(new WARCWriterPool(serialNo, wps, maxActive, maxWait));
    }
    
    
    @Override
    protected boolean shouldProcess(ProcessorURI puri) {
        if (!(puri instanceof CrawlURI)) {
            return false;
        }
        
        CrawlURI curi = (CrawlURI)puri;
        
        // If failure, or we haven't fetched the resource yet, return
        if (curi.getFetchStatus() <= 0) {
            return false;
        }
        
        // If no content, don't write record.
        int recordLength = (int)curi.getContentSize();
        if (recordLength <= 0) {
                // Write nothing.
                return false;
        }

        String scheme = curi.getUURI().getScheme().toLowerCase();
        if (shouldWrite(curi)) {
            return true;
        } else {
            logger.info("This writer does not write out scheme " +
                    scheme + " content");
            return false;
        }
    }
    
    
    /**
     * Writes a CrawlURI and its associated data to store file.
     * 
     * Currently this method understands the following uri types: dns, http, and
     * https.
     * 
     * @param curi
     *            CrawlURI to process.
     * 
     */
    @Override
    protected ProcessResult innerProcessResult(ProcessorURI puri) {
        CrawlURI curi = (CrawlURI)puri;
        
        String scheme = curi.getUURI().getScheme().toLowerCase();
        try {
            return write(scheme, curi);
        } catch (IOException e) {
            curi.getNonFatalFailures().add(e);
            logger.log(Level.SEVERE, "Failed write of Record: " +
                curi.toString(), e);
            return ProcessResult.PROCEED;
        }
    }
    
    protected ProcessResult write(final String lowerCaseScheme, final CrawlURI curi)
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
        
        ExperimentalWARCWriter w = (ExperimentalWARCWriter)writer;
        try {
            // Write a request, response, and metadata all in the one
            // 'transaction'.
            final URI baseid = getRecordID();
            final String timestamp =
                ArchiveUtils.get14DigitDate(curi.getFetchBeginTime());
            if (lowerCaseScheme.startsWith("http")) {
                // Add named fields for ip, checksum, and relate the metadata
                // and request to the resource field.
                ANVLRecord r = new ANVLRecord();
                if (curi.getContentDigest() != null) {
                    // TODO: This is digest for content -- doesn't include
                    // response headers.
                    r.addLabelValue(NAMED_FIELD_CHECKSUM_LABEL,
                        curi.getContentDigestSchemeString());
                }
                r.addLabelValue(NAMED_FIELD_IP_LABEL, getHostAddress(curi));
                URI rid = writeResponse(w, timestamp, HTTP_RESPONSE_MIMETYPE,
                	baseid, curi, r);
                r = new ANVLRecord(1);
                r.addLabelValue(NAMED_FIELD_RELATED_LABEL, rid.toString());
                writeRequest(w, timestamp, HTTP_REQUEST_MIMETYPE,
                	baseid, curi, r);
                writeMetadata(w, timestamp, baseid, curi, r);
            } else if (lowerCaseScheme.equals("dns")) {
                String ip = (String)curi.getData().get(A_DNS_SERVER_IP_LABEL);
                ANVLRecord r = null;
                if (ip != null && ip.length() > 0) {
                	r = new ANVLRecord();
                    r.addLabelValue(NAMED_FIELD_IP_LABEL, ip);
                }
                writeResponse(w, timestamp, curi.getContentType(), baseid,
                    curi, r);
            } else {
                logger.warning("No handler for scheme " + lowerCaseScheme);
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
    
    protected URI writeRequest(final ExperimentalWARCWriter w,
            final String timestamp, final String mimetype,
            final URI baseid, final CrawlURI curi,
            final ANVLRecord namedFields) 
    throws IOException {
        final URI uid = qualifyRecordID(baseid, TYPE, REQUEST);
        w.writeRequestRecord(curi.toString(), timestamp, mimetype, uid,
            namedFields,
            curi.getRecorder().getRecordedOutput().getReplayInputStream(),
            curi.getRecorder().getRecordedOutput().getSize());
        return uid;
    }
    
    protected URI writeResponse(final ExperimentalWARCWriter w,
            final String timestamp, final String mimetype,
            final URI baseid, final CrawlURI curi,
            final ANVLRecord namedFields) 
    throws IOException {
        w.writeResponseRecord(curi.toString(), timestamp, mimetype, baseid,
            namedFields,
            curi.getRecorder().getRecordedInput().getReplayInputStream(),
            curi.getRecorder().getRecordedInput().getSize());
        return baseid;
    }
    
    protected URI writeMetadata(final ExperimentalWARCWriter w,
            final String timestamp,
            final URI baseid, final CrawlURI curi,
            final ANVLRecord namedFields) 
    throws IOException {
        final URI uid = qualifyRecordID(baseid, TYPE, METADATA);
        // Get some metadata from the curi.
        // TODO: Get all curi metadata.
        ANVLRecord r = new ANVLRecord();
        if (curi.isSeed()) {
            r.addLabel("seed");
        } else {
        	if (curi.forceFetch()) {
        		r.addLabel("force-fetch");
        	}
            r.addLabelValue("via", curi.flattenVia());
            r.addLabelValue("pathFromSeed", curi.getPathFromSeed());
        }
        Collection<Link> links = curi.getOutLinks();
        if (links != null || links.size() > 0) {
            for (Link link: links) {
                r.addLabelValue("outlink", link.toString());
            }
        }
        if (isTruncatedFetch(curi)) {
        	Collection<String> anno = curi.getAnnotations();
            String value = anno.contains(TIMER_TRUNC) ?
                    NAMED_FIELD_TRUNCATED_VALUE_TIME:
                anno.contains(LENGTH_TRUNC) ?
                        NAMED_FIELD_TRUNCATED_VALUE_LEN:
                anno.contains(HEADER_TRUNC) ?
                        NAMED_FIELD_TRUNCATED_VALUE_HEAD:
                NAMED_FIELD_TRUNCATED_VALUE_UNSPECIFIED;
                      
            r.addLabelValue(NAMED_FIELD_TRUNCATED, value);
        }
        
        // TODO: Other curi fields to write to metadata.
        // 
        // Credentials
        // 
        // fetch-began-time: 1154569278774
        // fetch-completed-time: 1154569281816
        //
        // Annotations.
        
        byte [] b = r.getUTF8Bytes();
        w.writeMetadataRecord(curi.toString(), timestamp, ANVLRecord.MIMETYPE,
            uid, namedFields, new ByteArrayInputStream(b), b.length);
        return uid;
    }
    
    
    private boolean isTruncatedFetch(CrawlURI curi) {
    	Collection<String> anno = curi.getAnnotations();
    	return anno.contains(LENGTH_TRUNC) 
    	 || anno.contains(TIMER_TRUNC) 
    	 || anno.contains(HEADER_TRUNC);
    }
    
    protected URI getRecordID() throws IOException {
        URI result;
        try {
            result = GeneratorFactory.getFactory().getRecordID();
        } catch (URISyntaxException e) {
            throw new IOException(e.toString());
        }
        return result;
    }
    
    protected URI qualifyRecordID(final URI base, final String key,
            final String value)
    throws IOException {
        URI result;
        Map<String, String> qualifiers = new HashMap<String, String>(1);
        qualifiers.put(key, value);
        try {
            result = GeneratorFactory.getFactory().
                qualifyRecordID(base, qualifiers);
        } catch (URISyntaxException e) {
            throw new IOException(e.toString());
        }
        return result;
    }  

    public List getMetadata() {
        // TODO: As ANVL?
        return null;
    }
    
    
    protected Key<List<String>> getPathKey() {
        return PATH;
    }
}
