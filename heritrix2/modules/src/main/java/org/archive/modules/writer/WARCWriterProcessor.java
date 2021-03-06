/* $Id: ExperimentalWARCWriterProcessor.java 4935 2007-02-23 00:27:24Z gojomo $
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
package org.archive.modules.writer;

import static org.archive.modules.ModuleAttributeConstants.A_DNS_SERVER_IP_LABEL;
import static org.archive.modules.ModuleAttributeConstants.A_HTTP_TRANSACTION;
import static org.archive.modules.ModuleAttributeConstants.A_SOURCE_TAG;
import static org.archive.modules.ModuleAttributeConstants.HEADER_TRUNC;
import static org.archive.modules.ModuleAttributeConstants.LENGTH_TRUNC;
import static org.archive.modules.ModuleAttributeConstants.TIMER_TRUNC;
import static org.archive.io.warc.WARCConstants.HEADER_KEY_CONCURRENT_TO;
import static org.archive.io.warc.WARCConstants.HEADER_KEY_ETAG;
import static org.archive.io.warc.WARCConstants.HEADER_KEY_IP;
import static org.archive.io.warc.WARCConstants.HEADER_KEY_LAST_MODIFIED;
import static org.archive.io.warc.WARCConstants.HEADER_KEY_PAYLOAD_DIGEST;
import static org.archive.io.warc.WARCConstants.HEADER_KEY_PROFILE;
import static org.archive.io.warc.WARCConstants.HEADER_KEY_TRUNCATED;
import static org.archive.io.warc.WARCConstants.HTTP_REQUEST_MIMETYPE;
import static org.archive.io.warc.WARCConstants.HTTP_RESPONSE_MIMETYPE;
import static org.archive.io.warc.WARCConstants.METADATA;
import static org.archive.io.warc.WARCConstants.NAMED_FIELD_TRUNCATED_VALUE_HEAD;
import static org.archive.io.warc.WARCConstants.NAMED_FIELD_TRUNCATED_VALUE_LENGTH;
import static org.archive.io.warc.WARCConstants.NAMED_FIELD_TRUNCATED_VALUE_TIME;
import static org.archive.io.warc.WARCConstants.PROFILE_REVISIT_IDENTICAL_DIGEST;
import static org.archive.io.warc.WARCConstants.PROFILE_REVISIT_NOT_MODIFIED;
import static org.archive.io.warc.WARCConstants.REQUEST;
import static org.archive.io.warc.WARCConstants.TYPE;
import static org.archive.modules.recrawl.RecrawlAttributeConstants.A_ETAG_HEADER;
import static org.archive.modules.recrawl.RecrawlAttributeConstants.A_LAST_MODIFIED_HEADER;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;
import org.archive.io.ReplayInputStream;
import org.archive.io.WriterPoolMember;
import org.archive.io.WriterPoolSettings;
import org.archive.io.warc.WARCWriter;
import org.archive.io.warc.WARCWriterPool;
import org.archive.modules.ProcessResult;
import org.archive.modules.ProcessorURI;
import org.archive.modules.deciderules.recrawl.IdenticalDigestDecideRule;
import org.archive.modules.extractor.Link;
import org.archive.state.Expert;
import org.archive.state.Global;
import org.archive.state.Key;
import org.archive.state.KeyManager;
import org.archive.state.StateProvider;
import org.archive.uid.GeneratorFactory;
import org.archive.util.ArchiveUtils;
import org.archive.util.anvl.ANVLRecord;

/**
 * WARCWriterProcessor.
 * Goes against the 0.18 version of the WARC specification (which
 * is functionally identical to 0.17 except in the protocol 
 * identifier string). 
 * See http://archive-access.sourceforge.net/warc/  
 * 
 * <p>TODO: Remove ANVLRecord. Rename NameValue or use RFC822
 * (commons-httpclient?) or find something else.
 * 
 * @author stack
 */
public class WARCWriterProcessor extends WriterPoolProcessor {


    private static final long serialVersionUID = 6182850087635847443L;

    private static final Logger logger = 
        Logger.getLogger(WARCWriterProcessor.class.getName());
    
    /**
     * Whether to write 'request' type records. Default is true.
     */
    @Expert
    final public static Key<Boolean> WRITE_REQUESTS = Key.make(true);

    
    /**
     * Whether to write 'metadata' type records. Default is true.
     */
    @Expert
    final public static Key<Boolean> WRITE_METADATA = Key.make(true);


    /**
     * Whether to write 'revisit' type records when a URI's history indicates
     * the previous fetch had an identical content digest. Default is true.
     */
    @Expert
    final public static Key<Boolean> WRITE_REVISIT_FOR_IDENTICAL_DIGESTS =
        Key.make(true);

    
    /**
     * Whether to write 'revisit' type records when a 304-Not Modified response
     * is received. Default is true.
     */
    @Expert
    final public static Key<Boolean> WRITE_REVISIT_FOR_NOT_MODIFIED =
        Key.make(true);

    /**
     * Default path list.
     */
    private static final String [] DEFAULT_PATH = {"warcs"};
    
    /**
     * Where to save files. Supply absolute or relative path. If relative, files
     * will be written relative to the order.disk-path setting. If more than one
     * path specified, we'll round-robin dropping files to each. This setting is
     * safe to change midcrawl (You can remove and add new dirs as the crawler
     * progresses).
     */
    @Global
    final public static Key<List<String>> PATH = Key.makeSimpleList(String.class, "warcs");


    static {
        KeyManager.addKeys(WARCWriterProcessor.class);
    }

    private transient List<String> cachedMetadata;

    protected String [] getDefaultPath() {
        return DEFAULT_PATH;
    }
    
    /**
     * Constructor.
     */
    public WARCWriterProcessor() {
    }

    @Override
    protected void setupPool(final AtomicInteger serialNo) {
        int maxActive = getMaxActive();
        int maxWait = getMaxWait();
        WriterPoolSettings wps = getWriterPoolSettings();
        setPool(new WARCWriterPool(serialNo, wps, maxActive, maxWait));
    }


    /**
     * Writes a ProcessorURI and its associated data to store file.
     * 
     * Currently this method understands the following uri types: dns, http, and
     * https.
     * 
     * @param curi ProcessorURI to process.
     * 
     */
    @Override
    protected ProcessResult innerProcessResult(ProcessorURI puri) {
        ProcessorURI curi = (ProcessorURI)puri;
        String scheme = curi.getUURI().getScheme().toLowerCase();
        try {
            if (shouldWrite(curi)) {
                return write(scheme, curi);
            } else {
                logger.info("This writer does not write out scheme " +
                        scheme + " content");
            }
        } catch (IOException e) {
            curi.getNonFatalFailures().add(e);
            logger.log(Level.SEVERE, "Failed write of Record: " +
                curi.toString(), e);
        }
        return ProcessResult.PROCEED;
    }
    
    protected ProcessResult write(final String lowerCaseScheme, 
            final ProcessorURI curi)
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
        
        WARCWriter w = (WARCWriter)writer;
        try {
            // Write a request, response, and metadata all in the one
            // 'transaction'.
            final URI baseid = getRecordID();
            final String timestamp =
                ArchiveUtils.getLog14Date(curi.getFetchBeginTime());
            if (lowerCaseScheme.startsWith("http")) {
                // Add named fields for ip, checksum, and relate the metadata
                // and request to the resource field.
                // TODO: Use other than ANVL (or rename ANVL as NameValue or
                // use RFC822 (commons-httpclient?).
                ANVLRecord headers = new ANVLRecord(5);
                if (curi.getContentDigest() != null) {
                    headers.addLabelValue(HEADER_KEY_PAYLOAD_DIGEST,
                            curi.getContentDigestSchemeString());
                }
                headers.addLabelValue(HEADER_KEY_IP, getHostAddress(curi));
                URI rid;
                
                if (IdenticalDigestDecideRule.hasIdenticalDigest(curi) && 
                        curi.get(this, WRITE_REVISIT_FOR_IDENTICAL_DIGESTS)) {
                    rid = writeRevisitDigest(w, timestamp, HTTP_RESPONSE_MIMETYPE,
                            baseid, curi, headers);
                } else if (curi.getFetchStatus() == HttpStatus.SC_NOT_MODIFIED && 
                        curi.get(this, WRITE_REVISIT_FOR_NOT_MODIFIED)) {
                    rid = writeRevisitNotModified(w, timestamp,
                            baseid, curi, headers);
                } else {
                    // Check for truncated annotation
                    String value = null;
                    Collection<String> anno = curi.getAnnotations();
                    if (anno.contains(TIMER_TRUNC)) {
                        value = NAMED_FIELD_TRUNCATED_VALUE_TIME;
                    } else if (anno.contains(LENGTH_TRUNC)) {
                        value = NAMED_FIELD_TRUNCATED_VALUE_LENGTH;
                    } else if (anno.contains(HEADER_TRUNC)) {
                        value = NAMED_FIELD_TRUNCATED_VALUE_HEAD;
                    }
                    // TODO: Add annotation for TRUNCATED_VALUE_UNSPECIFIED
                    if (value != null) {
                        headers.addLabelValue(HEADER_KEY_TRUNCATED, value);
                    }
                    rid = writeResponse(w, timestamp, HTTP_RESPONSE_MIMETYPE,
                    	baseid, curi, headers);
                }
                
                headers = new ANVLRecord(1);
                headers.addLabelValue(HEADER_KEY_CONCURRENT_TO,
                    '<' + rid.toString() + '>');

                if (curi.get(this, WRITE_REQUESTS)) {
                    writeRequest(w, timestamp, HTTP_REQUEST_MIMETYPE,
                            baseid, curi, headers);
                }
                if (curi.get(this, WRITE_METADATA)) {
                    writeMetadata(w, timestamp, baseid, curi, headers);
                } 
            } else if (lowerCaseScheme.equals("dns")) {
                ANVLRecord headers = null;
                String ip = (String)curi.getData().get(A_DNS_SERVER_IP_LABEL);
                if (ip != null && ip.length() > 0) {
                    headers = new ANVLRecord(1);
                    headers.addLabelValue(HEADER_KEY_IP, ip);
                }
                writeResponse(w, timestamp, curi.getContentType(), baseid,
                    curi, headers);
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
    
    protected URI writeRequest(final WARCWriter w,
            final String timestamp, final String mimetype,
            final URI baseid, final ProcessorURI curi,
            final ANVLRecord namedFields) 
    throws IOException {
        final URI uid = qualifyRecordID(baseid, TYPE, REQUEST);
        ReplayInputStream ris =
            curi.getRecorder().getRecordedOutput().getReplayInputStream();
        try {
            w.writeRequestRecord(curi.toString(), timestamp, mimetype, uid,
                namedFields, ris,
                curi.getRecorder().getRecordedOutput().getSize());
        } finally {
            if (ris != null) {
                ris.close();
            }
        }
        return uid;
    }
    
    protected URI writeResponse(final WARCWriter w,
            final String timestamp, final String mimetype,
            final URI baseid, final ProcessorURI curi,
            final ANVLRecord namedFields) 
    throws IOException {
        ReplayInputStream ris =
            curi.getRecorder().getRecordedInput().getReplayInputStream();
        try {
            w.writeResponseRecord(curi.toString(), timestamp, mimetype, baseid,
                namedFields, ris,
                curi.getRecorder().getRecordedInput().getSize());
        } finally {
            if (ris != null) {
                ris.close();
            }
        }
        return baseid;
    }
    
    protected URI writeResource(final WARCWriter w,
            final String timestamp, final String mimetype,
            final URI baseid, final ProcessorURI curi,
            final ANVLRecord namedFields) 
    throws IOException {
        ReplayInputStream ris =
            curi.getRecorder().getRecordedInput().getReplayInputStream();
        try {
            w.writeResourceRecord(curi.toString(), timestamp, mimetype, baseid,
                namedFields, ris,
                curi.getRecorder().getRecordedInput().getSize());
        } finally {
            if (ris != null) {
                ris.close();
            }
        }
        return baseid;
    }

    
    protected URI writeRevisitDigest(final WARCWriter w,
            final String timestamp, final String mimetype,
            final URI baseid, final ProcessorURI curi,
            final ANVLRecord namedFields) 
    throws IOException {
        long revisedLength = curi.getRecorder().getRecordedInput().getContentBegin();
        revisedLength = revisedLength > 0 
            ? revisedLength 
            : curi.getRecorder().getRecordedInput().getSize();
        namedFields.addLabelValue(
        		HEADER_KEY_PROFILE, PROFILE_REVISIT_IDENTICAL_DIGEST);
        namedFields.addLabelValue(
        		HEADER_KEY_TRUNCATED, NAMED_FIELD_TRUNCATED_VALUE_LENGTH);
        ReplayInputStream ris =
            curi.getRecorder().getRecordedInput().getReplayInputStream();
        try {
            w.writeRevisitRecord(curi.toString(), timestamp, mimetype, baseid,
                namedFields, ris, revisedLength);
        } finally {
            if (ris != null) {
                ris.close();
            }
        }
        curi.getAnnotations().add("warcRevisit:digest");
        return baseid;
    }
    
    protected URI writeRevisitNotModified(final WARCWriter w,
            final String timestamp, 
            final URI baseid, final ProcessorURI curi,
            final ANVLRecord namedFields) 
    throws IOException {
        namedFields.addLabelValue(
        		HEADER_KEY_PROFILE, PROFILE_REVISIT_NOT_MODIFIED);
        // save just enough context to understand basis of not-modified
        if(curi.containsDataKey(A_HTTP_TRANSACTION)) {
            HttpMethodBase method = 
                (HttpMethodBase) curi.getData().get(A_HTTP_TRANSACTION);
            saveHeader(A_ETAG_HEADER,method,namedFields,HEADER_KEY_ETAG);
            saveHeader(A_LAST_MODIFIED_HEADER,method,namedFields,
            		HEADER_KEY_LAST_MODIFIED);
        }
        // truncate to zero-length (all necessary info is above)
        namedFields.addLabelValue(HEADER_KEY_TRUNCATED,
            NAMED_FIELD_TRUNCATED_VALUE_LENGTH);
        ReplayInputStream ris =
            curi.getRecorder().getRecordedInput().getReplayInputStream();
        try {
            w.writeRevisitRecord(curi.toString(), timestamp, null, baseid,
                namedFields, ris, 0);
        } finally {
            if (ris !=  null) {
                ris.close();
            }
        }
        curi.getAnnotations().add("warcRevisit:notModified");
        return baseid;
    }
    
    /**
     * Save a header from the given HTTP operation into the 
     * provider headers under a new name
     * 
     * @param origName header name to get if present
     * @param method http operation containing headers
     */
    protected void saveHeader(String origName, HttpMethodBase method, 
    		ANVLRecord headers, String newName) {
        Header header = method.getResponseHeader(origName);
        if(header!=null) {
            headers.addLabelValue(newName, header.getValue());
        }
    }

	protected URI writeMetadata(final WARCWriter w,
            final String timestamp,
            final URI baseid, final ProcessorURI curi,
            final ANVLRecord namedFields) 
    throws IOException {
        final URI uid = qualifyRecordID(baseid, TYPE, METADATA);
        // Get some metadata from the curi.
        // TODO: Get all curi metadata.
        // TODO: Use other than ANVL (or rename ANVL as NameValue or use
        // RFC822 (commons-httpclient?).
        ANVLRecord r = new ANVLRecord();
        if (curi.isSeed()) {
            r.addLabel("seed");
        } else {
        	if (curi.forceFetch()) {
        		r.addLabel("force-fetch");
        	}
            r.addLabelValue("via", flattenVia(curi));
            r.addLabelValue("hopsFromSeed", curi.getPathFromSeed());
            if (curi.containsDataKey(A_SOURCE_TAG)) {
                r.addLabelValue("sourceTag", 
                        (String)curi.getData().get(A_SOURCE_TAG));
            }
        }
        long duration = curi.getFetchCompletedTime() - curi.getFetchBeginTime();
        if (duration > -1) {
            r.addLabelValue("fetchTimeMs", Long.toString(duration));
        }
        
        // Add outlinks though they are effectively useless without anchor text.
        Collection<Link> links = curi.getOutLinks();
        if (links != null && links.size() > 0) {
            for (Link link: links) {
                r.addLabelValue("outlink", link.toString());
            }
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


    @Override
    protected Key<List<String>> getPathKey() {
        return PATH;
    }


    
    public List<String> getMetadata(StateProvider global) {
        if (cachedMetadata != null) {
            return cachedMetadata;
        }
        ANVLRecord record = new ANVLRecord(7);
        record.addLabelValue("software", "Heritrix/" +
                ArchiveUtils.VERSION + " http://crawler.archive.org");
        try {
            InetAddress host = InetAddress.getLocalHost();
            record.addLabelValue("ip", host.getHostAddress());
            record.addLabelValue("hostname", host.getHostName());
        } catch (UnknownHostException e) {
            logger.log(Level.WARNING,"unable top obtain local crawl engine host",e);
        }
        
        // conforms to ISO 28500:2009 as of May 2009
        // as described at http://bibnum.bnf.fr/WARC/ 
        // latest draft as of November 2008
        record.addLabelValue("format","WARC File Format 1.0"); 
        record.addLabelValue("conformsTo","http://bibnum.bnf.fr/WARC/WARC_ISO_28500_version1_latestdraft.pdf");

        // Get other values from metadata provider

        MetadataProvider provider = global.get(this, METADATA_PROVIDER);

        addIfNotBlank(record,"operator", provider.getJobOperator());
        addIfNotBlank(record,"publisher", provider.getOrganization());
        addIfNotBlank(record,"audience", provider.getAudience());
        addIfNotBlank(record,"isPartOf", provider.getJobName());
        // TODO: make date match 'job creation date' as in Heritrix 1.x
        // until then, leave out (plenty of dates already in WARC 
        // records
//            String rawDate = provider.getBeginDate();
//            if(StringUtils.isNotBlank(rawDate)) {
//                Date date;
//                try {
//                    date = ArchiveUtils.parse14DigitDate(rawDate);
//                    addIfNotBlank(record,"created",ArchiveUtils.getLog14Date(date));
//                } catch (ParseException e) {
//                    logger.log(Level.WARNING,"obtaining warc created date",e);
//                }
//            }
        addIfNotBlank(record,"description", provider.getJobDescription());
        addIfNotBlank(record,"robots", provider.getRobotsPolicy().toLowerCase());

        addIfNotBlank(record,"http-header-user-agent",
                provider.getUserAgent());
        addIfNotBlank(record,"http-header-from",
                provider.getFrom());

        // really ugly to return as List<String>, but changing would require 
        // larger refactoring
        return Collections.singletonList(record.toString());
    }
    
    protected void addIfNotBlank(ANVLRecord record, String label, String value) {
        if(StringUtils.isNotBlank(value)) {
            record.addLabelValue(label, value);
        }
    }
    // good to keep at end of source: must run after all per-Key 
    // initialization values are set.
    static {
        KeyManager.addKeys(WARCWriterProcessor.class);
    }
}