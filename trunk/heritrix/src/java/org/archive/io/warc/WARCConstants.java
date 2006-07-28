/*
 * WARCConstants
 *
 * $Id$
 *
 * Created on July 27th, 2006
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

import java.util.Arrays;
import java.util.List;

public interface WARCConstants {
    /**
     * WARC file extention.
     */
    public static final String WARC_FILE_EXTENSION = "warc";
    
    /**
     * Encoding to use getting bytes from strings.
     *
     * Specify an encoding rather than leave it to chance: i.e whatever the
     * JVMs encoding.  Use an encoding that gets the stream as bytes, not chars.
     * 
     * <p>TODO: ARC uses ISO-8859-1.  In general, we should use UTF-8 but we
     * probably need a single byte encoding if we're out for preserving the
     * binary data as received over the net (We probably don't want to transform
     * the supra-ASCII characters to UTF-8 before storing in ARC).  For now,
     * till we figure it, DEFAULT_ENCODING is single-byte charset -- same as
     * ARCs.
     */
    public static final String DEFAULT_ENCODING = "ISO-8859-1";
    public static final String HEADER_LINE_ENCODING = DEFAULT_ENCODING;
    
    /**
     * WARC Record Types.
     */
    public static final String WARCINFO = "warcinfo";
    public static final String RESPONSE = "response";
    public static final String RESOURCE = "resource";
    public static final String REQUEST = "request";
    public static final String METADATA = "metadata";
    public static final String REVISIT = "revist";
    public static final String CONVERSION = "conversion";
    public static final String CONTINUATION = "continuation";
    
    public static final String TYPE = "type";
    
    // List of all WARC Record TYPES
    public static final String [] TYPES = {WARCINFO, RESPONSE, RESOURCE,
    	REQUEST, METADATA, REVISIT, CONVERSION, CONTINUATION};
    
    // Indices into TYPES array.
    public static final int WARCINFO_INDEX = 0;
    public static final int RESPONSE_INDEX = 1;
    public static final int RESOURCE_INDEX = 2;
    public static final int REQUEST_INDEX = 3;
    public static final int METADATA_INDEX = 4;
    public static final int REVISIT_INDEX = 5;
    public static final int CONVERSION_INDEX = 6;
    public static final int CONTINUATION_INDEX = 7;
    
    // TYPES as List.
    public static final List TYPES_LIST = Arrays.asList(TYPES);
    
    /**
     * WARC-ID
     */
    public static final String WARC_ID = "WARC/0.9";
    
    /**
     * Header NEWLINE.
     */
    public static final String NEWLINE = "\r\n";
        
    /**
     * Header field seperator character.
     */
    public static final char HEADER_FIELD_SEPARATOR = ' ';
    
    /**
     * WSP
     * One of a space or horizontal tab character.
     * TODO: WSP undefined.  Fix.
     */
    public static final Character [] WSP = {HEADER_FIELD_SEPARATOR, '\t'};

    /**
     * Placeholder for length in Header line.
     * Placeholder is same size as the fixed field size allocated for length,
     * 12 characters.  12 characters allows records of size almost 1TB.
     */
    public static final String PLACEHOLDER_RECORD_LENGTH_STRING =
        "000000000000";
}
