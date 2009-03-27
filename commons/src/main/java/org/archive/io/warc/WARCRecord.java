/* $Id: WARCRecord.java 4566 2006-08-31 16:51:41Z stack-sf $
 *
 * Created on August 25th, 2006
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
package org.archive.io.warc;

import it.unimi.dsi.fastutil.io.RepositionableStream;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpParser;
import org.archive.io.ArchiveRecord;
import org.archive.io.ArchiveRecordHeader;
import org.archive.io.ReplayInputStream;
import org.archive.util.anvl.ANVLRecord;


/**
 * A WARC file Record.
 *
 * @author stack
 */
public class WARCRecord extends ArchiveRecord implements WARCConstants {
    
    private Pattern WHITESPACE = Pattern.compile("\\s");
    
    /** WARC record fields **/
    protected String type;
    protected String date;
    protected String mimeType;
    protected URI id;
    protected ANVLRecord namedFields;
    protected InputStream inputStream;
    protected long length;
    protected String url;
    protected boolean enforceLengthFlag;
    
    public WARCRecord(InputStream in)
    throws IOException {
        super(in, null, 0, false, false);
    }

    /**
     * Constructor.
     *
     * @param in Stream cue'd up to be at the start of the record this instance
     * is to represent.
     * @throws IOException
     */
    public WARCRecord (
            InputStream in, 
            final String identifier, 
            final long offset)
    throws IOException {
        this(in, identifier, offset, true, false);
    }
    
    /**
     * Constructor.
     * @param in Stream cue'd up just past Header Line and Named Fields.
     * @param headers Header Line and ANVL Named fields.
     * @throws IOException
     */
    public WARCRecord(InputStream in, ArchiveRecordHeader headers)
    throws IOException {
        super(in, headers, 0, true, false);
    }

    /**
     * Constructor for reading WARC records
     *
     * @param in Stream cue'd up to be at the start of the record this instance
     * is to represent or, if <code>headers</code> is not null, just past the
     * Header Line and Named Fields.
     * @param identifier Identifier for this the hosting Reader.
     * @param offset Current offset into <code>in</code> (Used to keep
     * <code>position</code> properly aligned).  Usually 0.
     * @param digest True if we're to calculate digest for this record.  Not
     * digesting saves about ~15% of cpu during parse.
     * @param strict Be strict parsing (Parsing stops if file inproperly
     * formatted).
     * @throws IOException
     */
    public WARCRecord(
            final InputStream in, 
            final String identifier,
            final long offset, 
            boolean digest, 
            boolean strict) 
    throws IOException {
        super(in, null, 0, digest, strict);
        setHeader(parseHeaders(in, identifier, offset, strict));
    }
    
    
    /** 
     * Constructor for writing WARC records from an InputStream with mandatory
     * params as defined by the WARC spec.
     * @param is InputStream to be written
     * @param id WARC-Record-ID
     * @param length Content-Length
     * @param date WARC-Date
     * @param type WARC-Type 
     * @throws IOException
     */
    public WARCRecord(
            InputStream is, 
            URI id, 
            long length, 
            String date, 
            String type)
    throws IOException {
        super(is);
        setID(id);
        setLength(length);
        setDate(date);
        setType(type);
    }
    
    /** set the type of WARC record 
     * @param type <code>WARC-Type</code> in WARC header
     */
    public void setType(final String type) {
        this.type = type;
    }
    
    /** set the 14-digit [ISO8601] WARC-Date (YYYY-MM-DDThh:mm:ssZ)
     * representing the instant that data capture for record creation began.
     * @param date the <code>WARC-Date</code> in the WARC header 
     **/
    public void setDate(final String date) {
        this.date = date;
    }
    
    /** 
     * set the MIME type [RFC2045] of the information contained in 
     * the record's block.   
     *  @param mimeType <code>Content-Type</code>
     */
    public void setMimeType (final String mimeType) {
        this.mimeType = mimeType;  
    }

    /** 
     * set a globally unique identifier for this record
     * @param recordID the <code>WARC-Record-ID</code> in the WARC header 
     */
    public void setID (final URI recordID) {
        this.id = recordID;
    }

    /** 
     * set named-fields giving information about the current record,
     * expandable, and beginning with "WARC-" for WARC specific purposes. 
     * @param namedFields the WARC named-fields in the WARC header
     */
    public void setNamedFields (final ANVLRecord namedFields) {
        this.namedFields = namedFields;
    }

    /**
     * set the original URI whose capture gave rise to the information content 
     * in this record. 
     * @param url the <code>WARC-Target-URI</code> in the WARC header
     */
    public void setUrl (final String url) {
        this.url = url;
    }

    /**
     * set the number of octets in the record content block
     * @param length <code>Content-Length</code>
     */
    public void setLength(long length) {
        this.length = length;
    }

    /**
     * set enforceLengthFlag which will throw IOE when read length differs 
     * from specified length
     * @param enforce true to enforce length
     */
    public void setEnforceLengthFlag (boolean enforce) {
        this.enforceLengthFlag = enforce;
    }

    
    /**
     * Parse WARC Header Line and Named Fields.
     * @param in Stream to read.
     * @param identifier Identifier for the hosting Reader.
     * @param offset Absolute offset into Reader.
     * @param strict Whether to be loose parsing or not.
     * @return An ArchiveRecordHeader.
     * @throws IOException 
     */
    protected ArchiveRecordHeader parseHeaders(
            final InputStream in,
            final String identifier,
            final long offset,
            final boolean strict)
    throws IOException {

        final Map<Object, Object> m = new HashMap<Object, Object>();
    	m.put(ABSOLUTE_OFFSET_KEY, new Long(offset));
    	m.put(READER_IDENTIFIER_FIELD_KEY, identifier);
        
        long startPosition = -1;
        if (in instanceof RepositionableStream) {
            startPosition = ((RepositionableStream)in).position();
        }
        String firstLine =
            new String(HttpParser.readLine(in, WARC_HEADER_ENCODING));
        if (firstLine == null || firstLine.length() <=0) {
            throw new IOException("Failed to read WARC_MAGIC");
        }
        if (!firstLine.startsWith(WARC_MAGIC)) {
            throw new IOException("Failed to find WARC MAGIC: " + firstLine);
        }
        // Here we start reading off the inputstream but we're reading the
        // stream direct rather than going via WARCRecord#read.  The latter will
        // keep count of bytes read, digest and fail properly if EOR too soon...
        // We don't want digesting while reading Headers.
        // 
        Header [] h = HttpParser.parseHeaders(in, WARC_HEADER_ENCODING);
        for (int i = 0; i < h.length; i++) {
            m.put(h[i].getName(), h[i].getValue());
        }
        int headerLength = -1;
        if (in instanceof RepositionableStream) {
            headerLength =
                (int)(((RepositionableStream)in).position() - startPosition);
        }
        final int contentOffset = headerLength;
        incrementPosition(contentOffset);
   
    	return new ArchiveRecordHeader() {
    		private Map<Object, Object> headers = m;
            private int contentBegin = contentOffset;

			public String getDate() {
				return (String)this.headers.get(HEADER_KEY_DATE);
			}

			public String getDigest() {
                return null;
                // TODO: perhaps return block-digest? 
                // superclass def implies this is calculated ("only after
                // read in totality"), not pulled from header, so
                // below prior implementation was misleading
//              return (String)this.headers.get(HEADER_KEY_CHECKSUM);
			}

			public String getReaderIdentifier() {
				return (String)this.headers.get(READER_IDENTIFIER_FIELD_KEY);
			}

			public Set getHeaderFieldKeys() {
				return this.headers.keySet();
			}

			public Map getHeaderFields() {
				return this.headers;
			}

			public Object getHeaderValue(String key) {
				return this.headers.get(key);
			}

			public long getLength() {
				Object o = this.headers.get(CONTENT_LENGTH);
				if (o == null) {
					return -1;
				}
				long contentLength = (o instanceof Long)?
                    ((Long)o).longValue(): Long.parseLong((String)o);
                return contentLength + contentOffset;
			}

			public String getMimetype() {
				return (String)this.headers.get(CONTENT_TYPE);
			}

			public long getOffset() {
				Object o = this.headers.get(ABSOLUTE_OFFSET_KEY);
				if (o == null) {
					return -1;
				}
				return (o instanceof Long)?
                    ((Long)o).longValue(): Long.parseLong((String)o);
			}

			public String getRecordIdentifier() {
				return (String)this.headers.get(RECORD_IDENTIFIER_FIELD_KEY);
			}

			public String getUrl() {
				return (String)this.headers.get(HEADER_KEY_URI);
			}

			public String getVersion() {
				return (String)this.headers.get(VERSION_FIELD_KEY);
			}
            
            public int getContentBegin() {
                return this.contentBegin;
            }
            
            @Override
            public String toString() {
                return this.headers.toString();
            }
    	};
    }
    
    @Override
    protected String getMimetype4Cdx(ArchiveRecordHeader h) {
        final String m = super.getMimetype4Cdx(h);
        // Mimetypes can have spaces in WARCs.  Emitting for CDX, just
        // squash them for now.  Later, quote them since squashing spaces won't
        // work for params that have quoted-string values.
        Matcher matcher = WHITESPACE.matcher(m);
        return matcher.replaceAll("");
    }

}