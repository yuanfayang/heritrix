/*
 * WARCWriter
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
     * <p>ARC uses ISO-8859-1 by default.  Lets go with UTF-8.
     */
    public static final String DEFAULT_ENCODING = "UTF-8";
    
    /**
     * WARC Record Types.
     */
    public final static String [] TYPES = {"warcinfo", "response", "resource",
        "request", "metadata", "revisit", "conversion", "continuation"};
    public final static int WARCINFO_INDEX = 0;
    public final static int RESPONSE_INDEX = 1;
    public final static int RESOURCE_INDEX = 2;
    public final static int REQUEST_INDEX = 3;
    public final static int METADATA_INDEX = 4;
    public final static int REVISIT_INDEX = 5;
    public final static int CONVERSION_INDEX = 6;
    public final static int CONTINUATION_INDEX = 7;
    public final static List TYPES_LIST = Arrays.asList(TYPES);
    
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
     * Amount of space to leave for the length string.
     * Will pad with spaces to 
     */
    public static final int LENGTH_WIDTH = 12;

    public static final int SEPARATORS_PLUS_LENGTH_WIDTH_PLUS_NEWLINE =
        HEADER_FIELD_SEPARATOR * 6 /* Separators between seven Header fields */
        + NEWLINE.length()
        + LENGTH_WIDTH;

}
