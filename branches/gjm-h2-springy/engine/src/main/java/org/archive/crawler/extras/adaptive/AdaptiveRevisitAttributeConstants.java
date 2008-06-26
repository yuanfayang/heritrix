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
package org.archive.crawler.extras.adaptive;

import org.archive.crawler.datamodel.CoreAttributeConstants;

/**
 * Defines static constants for the Adaptive Revisiting module defining data
 * keys in the CrawlURI AList. 
 *
 * @author Kristinn Sigurdsson
 * 
 * @see org.archive.crawler.datamodel.CoreAttributeConstants
 */
public interface AdaptiveRevisitAttributeConstants
extends CoreAttributeConstants {

    /** Designates a field in the CrawlURIs AList for the content digest of
     *  an earlier visit. */
    public static final String A_LAST_CONTENT_DIGEST = "last-content-digest";
    public static final String A_TIME_OF_NEXT_PROCESSING = 
        "time-of-next-processing";
    public static final String A_WAIT_INTERVAL = "wait-interval";
    public static final String A_NUMBER_OF_VISITS = "number-of-visits";
    public static final String A_NUMBER_OF_VERSIONS = "number-of-versions";
    public static final String A_FETCH_OVERDUE = "fetch-overdue";
    
    public static final String A_LAST_ETAG = "last-etag";
    public static final String A_LAST_DATESTAMP = "last-datestamp";
    
    public static final String A_WAIT_REEVALUATED = "wait-reevaluated";
    
    /** No knowledge of URI content. Possibly not fetched yet, unable
     *  to check if different or an error occured on last fetch attempt. */
    public static final int CONTENT_UNKNOWN = -1;
    
    /** URI content has not changed between the two latest, successfully
     *  completed fetches. */
    public static final int CONTENT_UNCHANGED = 0;
    
    /** URI content had changed between the two latest, successfully completed
     *  fetches. By definition, content has changed if there has only been one
     *  successful fetch made. */
    public static final int CONTENT_CHANGED = 1;

    /**
     * Key to use getting state of crawluri from the CrawlURI alist.
     */
    public static final String A_CONTENT_STATE_KEY = "ar-state";
}
