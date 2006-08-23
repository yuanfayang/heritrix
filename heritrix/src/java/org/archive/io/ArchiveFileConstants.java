/* $Id$
 *
 * Created on August 16th, 2006.
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
package org.archive.io;

/**
 * Constants used by Archive files and in Archive file processing.
 * @author stack
 * @version $Date$ $Revision$
 */
public interface ArchiveFileConstants {
    /**
     * Suffix given to files currently in use.
     */
    public static final String OCCUPIED_SUFFIX = ".open";
    
    /**
     * Suffix appended to 'broken' files.
     */
    public static final String INVALID_SUFFIX = ".invalid";
    
    /**
     * Compressed file extention.
     */
    public static final String COMPRESSED_FILE_EXTENSION = "gz";
    
    /**
     * Dot plus compressed file extention.
     */
    public static final String DOT_COMPRESSED_FILE_EXTENSION = "." +
        COMPRESSED_FILE_EXTENSION;
    
    /**
     * Key for the Archive File Header version field.
     */
    public static final String VERSION_HEADER_FIELD_KEY = "hdr-version";
    
    /**
     * Key for the Archive File Header Line length field.
     */
    public static final String LENGTH_HEADER_FIELD_KEY = "hdr-length";
    
    /**
     * Key for the Archive File Header type field.
     */
    public static final String TYPE_HEADER_FIELD_KEY = "hdr-type";
    
    /**
     * Key for the Archive File Header Line URL field.
     */
    public static final String URL_HEADER_FIELD_KEY = "hdr-subject-uri";
    
    /**
     * Key for the Archive File Header Line Creation Date field.
     */
    public static final String DATE_HEADER_FIELD_KEY = "hdr-creation-date";

    /**
     * Key for the Archive File Header Line mimetype field.
     */
    public static final String MIMETYPE_HEADER_FIELD_KEY = "hdr-content-type";
    
    /**
     * Key for the Archive Record absolute offset into Archive file.
     */
    public static final String ABSOLUTE_OFFSET_KEY = "archive-offset";
}
