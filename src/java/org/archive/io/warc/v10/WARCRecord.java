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
package org.archive.io.warc.v10;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.archive.io.ArchiveRecord;
import org.archive.io.ArchiveRecordHeader;
import org.archive.io.warc.WARCConstants;
import org.archive.util.LongWrapper;
import org.archive.util.anvl.ANVLRecord;


/**
 * A WARC file Record.
 *
 * @author stack
 */
public class WARCRecord extends ArchiveRecord implements WARCConstants {
    /**
     * Header-Line pattern;
     * I heart http://www.fileformat.info/tool/regex.htm
     */
    private final static Pattern HEADER_LINE = Pattern.compile(
        "^WARC/([0-9]+\\.[0-9]+(?:\\.[0-9]+)?)" +// Regex group 1: WARC lead-in.
        "[\\t ]+" +                 // Multiple tabs or spaces.
        "([0-9]+)" +                // Regex group 2: Length.
        "[\\t ]+" +                 // Multiple tabs or spaces.
        "(request|response|warcinfo|resource|metadata|" +
            "revisit|conversion)" + // Regex group 3: Type of WARC Record.
        "[\\t ]+" +                 // Multiple tabs or spaces.
        "([^\\t ]+)" +              // Regex group 4: Subject-uri.
        "[\\t ]+" +                 // Multiple tabs or spaces.
        "([0-9]{14})" +             // Regex group 5: Date
        "[\\t ]+" +                 // Multiple tabs or spaces.
        "([^\\t ]+)" +              // Regex group 6: Record-Id
        "[\\t ]+" +                 // Multiple tabs or spaces.
        "(.+)$");                   // Regex group 7: Mimetype.
    

    private Pattern WHITESPACE = Pattern.compile("\\s");
    
    /**
     * Constructor.
     *
     * @param in Stream cue'd up to be at the start of the record this instance
     * is to represent.
     * @throws IOException
     */
    public WARCRecord(InputStream in, final String identifier,
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
     * Constructor.
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
    public WARCRecord(final InputStream in, final String identifier,
    	final long offset, boolean digest, boolean strict) 
    throws IOException {
        super(in, null, 0, digest, strict);
        setHeader(parseHeaders(in, identifier, offset, strict));
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
    protected ArchiveRecordHeader parseHeaders(final InputStream in,
        final String identifier, final long offset, final boolean strict)
    throws IOException {
    	final Map<Object, Object> m = new HashMap<Object, Object>();
    	m.put(ABSOLUTE_OFFSET_KEY, new Long(offset));
    	m.put(READER_IDENTIFIER_FIELD_KEY, identifier);
        // Here we start reading off the inputstream but we're reading the
        // stream direct rather than going via WARCRecord#read.  The latter will
        // keep count of bytes read, digest and fail properly if EOR too soon...
        // We don't want digesting while reading Header Line and Named Fields.
        // 
        // The returned length includes terminating CRLF.
        int headLineLength = parseHeaderLine(in, m, strict);
        
        // Now, doing the ANVL parse, hard to know how many bytes have been
        // read since passed Stream doesn't keep count and the ANVL parse can
        // throw away bytes (e.g. if white space padding at start of a folded
        // Value or if a Value has a newline in it and it gets converted to a
        // CRNL in the ANVL representation).  Wrap the stream in a
        // byte-counting stream.
        //
        // TODO: Buffering.  Currently, we rely on the deflate buffer when
        // file is gzipped.  Otherwise, if uncompressed, no buffering.
        final LongWrapper anvlParseLength = new LongWrapper(0);
        InputStream countingStream = new InputStream() {
            @Override
            public int read() throws IOException {
                int c = in.read();
                if (c != -1) {
                    anvlParseLength.longValue++;
                }
                return c;
            }
        };
        parseNamedFields(countingStream, m);
        // Set offset at which content begins. Its the Header Line length plus
        // whatever we read parsing ANVL.
        final int contentOffset =
            (int)(headLineLength + anvlParseLength.longValue);
        incrementPosition(contentOffset);
   
    	return new ArchiveRecordHeader() {
    		private Map<Object, Object> fields = m;
            private int contentBegin = contentOffset;

			public String getDate() {
				return (String)this.fields.get(DATE_FIELD_KEY);
			}

			public String getDigest() {
				return (String)this.fields.get(NAMED_FIELD_CHECKSUM_LABEL);
			}

			public String getReaderIdentifier() {
				return (String)this.fields.get(READER_IDENTIFIER_FIELD_KEY);
			}

			public Set getHeaderFieldKeys() {
				return this.fields.keySet();
			}

			public Map getHeaderFields() {
				return this.fields;
			}

			public Object getHeaderValue(String key) {
				return this.fields.get(key);
			}

			public long getLength() {
				Object o = this.fields.get(LENGTH_FIELD_KEY);
				if (o == null) {
					return -1;
				}
				return ((Long)o).longValue();
			}

			public String getMimetype() {
				return (String)this.fields.get(MIMETYPE_FIELD_KEY);
			}

			public long getOffset() {
				Object o = this.fields.get(ABSOLUTE_OFFSET_KEY);
				if (o == null) {
					return -1;
				}
				return ((Long)o).longValue();
			}

			public String getRecordIdentifier() {
				return (String)this.fields.get(RECORD_IDENTIFIER_FIELD_KEY);
			}

			public String getUrl() {
				return (String)this.fields.get(URL_FIELD_KEY);
			}

			public String getVersion() {
				return (String)this.fields.get(VERSION_FIELD_KEY);
			}
            
            public int getContentBegin() {
                return this.contentBegin;
            }
            
            @Override
            public String toString() {
                return this.fields.toString();
            }
    	};
    }
    
    protected int parseHeaderLine(final InputStream in,
            final Map<Object, Object> fields, final boolean strict) 
    throws IOException {
        byte [] line = readLine(in, strict);
        if (line.length <= 2) {
            throw new IOException("No Header Line found");
        }
        // Strip the CRLF.
        String headerLine = new String(line, 0, line.length - 2,
            HEADER_LINE_ENCODING);
        Matcher m = HEADER_LINE.matcher(headerLine);
        if (!m.matches()) {
            throw new IOException("Failed parse of Header Line: " +
                headerLine);
        }
        for (int i = 0; i < HEADER_FIELD_KEYS.length; i++) {
            if (i == 1) {
                // Do length of Record as a Long.
                fields.put(HEADER_FIELD_KEYS[i],
                    Long.parseLong(m.group(i + 1)));
                continue;
            }
            fields.put(HEADER_FIELD_KEYS[i], m.group(i + 1));
        }
        
        return line.length;
    }

    /**
     * Read a line.
     * A 'line' in this context ends in CRLF and contains ascii-only and no
     * control-characters.
     * @param in InputStream to read.
     * @param strict Strict parsing (If false, we'll eat whitespace before the
     * record.
     * @return All bytes in line including terminating CRLF.
     * @throws IOException
     */
    protected byte [] readLine(final InputStream in, final boolean strict) 
    throws IOException {
        boolean done = false;
        boolean recordStart = strict;
        int read = 0;
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024 /*SWAG*/);
        for (int c  = -1, previousCharacter; !done;) {
            if (read++ >= MAX_LINE_LENGTH) {
                throw new IOException("Read " + MAX_LINE_LENGTH +
                    " bytes without finding CRLF");
            }
            previousCharacter = c;
            c = in.read();
            if (c == -1) {
                throw new IOException("End-Of-Stream before CRLF:\n" +
                    new String(baos.toByteArray()));
            }
            if (isLF((char)c) && isCR((char)previousCharacter)) {
                done = true;
            } else if (!recordStart && Character.isWhitespace(c)) {
                // Skip any whitespace at start.
                continue;
            } else {
                if (isCR((char)previousCharacter)) {
                    // If previous character was a CR and this character is not
                    // a LF, we tested above, thats illegal.
                    throw new IOException("CR in middle of Header:\n" +
                        new String(baos.toByteArray()));
                }
                
                // Not whitespace so start record if we haven't already.
                if (!recordStart) {
                    recordStart = true;
                }
            }
            baos.write(c);
        }
        return baos.toByteArray();
    }
 
    protected void parseNamedFields(final InputStream in,
        final Map<Object, Object> fields) 
    throws IOException {
        ANVLRecord r = ANVLRecord.load(in);
        fields.putAll(r.asMap());
    }
    
    public static boolean isCROrLF(final char c) {
        return isCR(c) || isLF(c);
    }
    
    public static boolean isCR(final char c) {
        return c == CRLF.charAt(0);
    }
    
    public static boolean isLF(final char c) {
        return c == CRLF.charAt(1);
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
