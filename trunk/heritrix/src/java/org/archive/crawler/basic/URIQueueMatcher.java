/* URIQueueMatcher
 * 
 * $Id$
 * 
 * Created on Feb 26, 2004
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
package org.archive.crawler.basic;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.framework.URIFrontier;
import org.archive.util.QueueItemMatcher;


/**
 * An implementation of the <code>QueueItemMatcher</code> suitible for the  
 * queues used by the <code>Frontier</code>
 * @author Kristinn Sigurdsson
 * 
 * @see org.archive.util.QueueItemMatcher
 * @see org.archive.crawler.basic.Frontier
 */
public class URIQueueMatcher implements QueueItemMatcher {
    
    private Pattern p;
    private boolean delete = false;
    private URIFrontier frontier;
    
    /**
     * Constructor. 
     * @param pattern A regular expression that will be applied to the 
     *                CandidateURIs' URIstring to determine if they 'match'.
     * @param delete If true then each time a match is hit, the related 
     *                CandidateURI will have it's fetch status set to 
     *                {@link org.archive.crawler.datamodel.FetchStatusCodes#S_DELETED_BY_USER
     *                'Deleted by user'} and sent to the frontier's 
     *                <code>finish</code> method for final disposition.
     * @param frontier The parent frontier. This can be null if delete is false.
     *                Must be valid if delete is true.
     * @see Frontier#finished(CrawlURI);
     */
    public URIQueueMatcher(String pattern, boolean delete, URIFrontier frontier){
        p = Pattern.compile(pattern);
        this.delete = delete;
        this.frontier = frontier;
    }
    
    /* (non-Javadoc)
     * @see org.archive.util.QueueItemMatcher#match(java.lang.Object)
     */
    public boolean match(Object o) {
        if(o instanceof CandidateURI){
            CandidateURI CaURI = (CandidateURI)o;
            Matcher m = p.matcher(CaURI.getURIString());
            if(m.matches()){
                if(delete && frontier != null){
                    CrawlURI tmp = new CrawlURI(CaURI);
                    tmp.setFetchStatus(FetchStatusCodes.S_DELETED_BY_USER);
                    frontier.finished(tmp);
                }
                return true;
            }
        }
        return false;
    }
}
