/* ARCRecord
 *
 * $Id$
 *
 * Created on Jan 7, 2004
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
package org.archive.io.warc;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.archive.io.ArchiveRecord;
import org.archive.io.ArchiveRecordHeader;


/**
 * A WARC file Record.
 *
 * @author stack
 */
public class WARCRecord extends ArchiveRecord implements WARCConstants {    
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
     * @param identfier Identifier for this the hosting Reader.
     * @param headers Header Line and ANVL Named fields.  If null, assumes we're
     * aligned at start of Record and will try parse.
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
    
    protected ArchiveRecordHeader parseHeaders(final InputStream in,
    		final String identifier, final long offset, final boolean strict) {
    	final Map<Object, Object> m = new HashMap<Object, Object>();
    	m.put(ABSOLUTE_OFFSET_KEY, new Long(offset));
    	m.put(READER_IDENTIFIER_FIELD_KEY, identifier);
    	// TODO: Parse of Header Line.
    	return new ArchiveRecordHeader() {
    		private Map<Object, Object> fields = m;

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

			public void setDigest(String digest) {
				this.fields.put(NAMED_FIELD_CHECKSUM_LABEL, digest);
			}
    	};
    }
}