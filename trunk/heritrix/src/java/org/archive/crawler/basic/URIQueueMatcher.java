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
import org.archive.util.QueueItemMatcher;


/**
 * An implementation ofthe <code>QueueItemMatcher</code> suitible for the  
 * queues used by the <code>Frontier</code>
 * @author Kristinn Sigurdsson
 */
public class URIQueueMatcher implements QueueItemMatcher {
    
    private Pattern p;
    
    /**
     * Constructor. 
     * @param pattern A regular expression that will be applied to the 
     *                CandidateURIs' URIstring to determine if they 'match'.
     */
    public URIQueueMatcher(String pattern){
        p = Pattern.compile(pattern);
    }
    
    /* (non-Javadoc)
     * @see org.archive.util.QueueItemMatcher#match(java.lang.Object)
     */
    public boolean match(Object o) {
        if(o instanceof CandidateURI){
            CandidateURI CaURI = (CandidateURI)o;
            Matcher m = p.matcher(CaURI.getURIString());
            if(m.matches()){
                return true;
            }
        }
        return false;
    }

}
