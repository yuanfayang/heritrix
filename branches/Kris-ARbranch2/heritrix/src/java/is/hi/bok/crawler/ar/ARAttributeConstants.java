/* Constants
 * 
 * $Id$
 * 
 * Created on 26.11.2004
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
package is.hi.bok.crawler.ar;

/**
 * Defines static constants for the Adaptive Revisiting module defining data
 * keys in the CrawlURI AList. 
 *
 * @author Kristinn Sigurdsson
 * 
 * @see org.archive.crawler.datamodel.CoreAttributeConstants
 */
public interface ARAttributeConstants {

    /** Designates a field in the CrawlURIs AList for the content digest of
     *  an earlier visit. */
    public static final String A_LAST_CONTENT_DIGEST = "last-content-digest";
    public static final String A_TIME_OF_NEXT_PROCESSING = 
        "time-of-next-processing";
    public static final String A_WAIT_INTERVAL = "wait-interval";
    public static final String A_NUMBER_OF_VISITS = "number-of-visits";
    public static final String A_NUMBER_OF_VERSIONS = "number-of-versions";

}
