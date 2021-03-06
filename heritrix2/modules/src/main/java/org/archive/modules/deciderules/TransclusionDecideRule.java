/* TransclusionDecideRule
*
* $Id$
*
* Created on Apr 1, 2005
*
* Copyright (C) 2005 Internet Archive.
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
package org.archive.modules.deciderules;

import org.archive.modules.ProcessorURI;
import org.archive.modules.extractor.Hop;
import org.archive.state.Key;
import org.archive.state.KeyManager;



/**
 * Rule ACCEPTs any CrawlURIs whose path-from-seed ('hopsPath' -- see
 * {@link CandidateURI#getPathFromSeed()}) ends 
 * with at least one, but not more than, the given number of 
 * non-navlink ('L') hops. 
 * 
 * Otherwise, if the path-from-seed is empty or if a navlink ('L') occurs
 * within max-trans-hops of the tail of the path-from-seed, this rule
 * returns PASS.
 *  
 * <p>Thus, it allows things like embedded resources (frames/images/media) 
 * and redirects to be transitively included ('transcluded') in a crawl, 
 * even if they otherwise would not, for some reasonable number of hops
 * (1-4).
 *
 * @see <a href="http://www.google.com/search?q=define%3Atransclusion&sourceid=mozilla&start=0&start=0&ie=utf-8&oe=utf-8">Transclusion</a>
 *
 * @author gojomo
 */
public class TransclusionDecideRule extends PredicatedAcceptDecideRule {

    private static final long serialVersionUID = -3975688876990558918L;

    /**
     * Maximum number of non-navlink (non-'L') hops to ACCEPT.
     */
    public static final Key<Integer> MAX_TRANS_HOPS = Key.make(2);

    
    /**
     * Maximum number of speculative ('X') hops to ACCEPT.
     */
    public static final Key<Integer> MAX_SPECULATIVE_HOPS = Key.make(1);

    
    static {
        KeyManager.addKeys(TransclusionDecideRule.class);
    }

    /**
     * Usual constructor. 
     */
    public TransclusionDecideRule() {
    }

    /**
     * Evaluate whether given object is within the threshold number of
     * transitive hops.
     * 
     * @param object Object to make decision on.
     * @return true if the transitive hops >0 and <= max
     */
    protected boolean evaluate(ProcessorURI curi) {
        String hopsPath = curi.getPathFromSeed();
        if (hopsPath == null || hopsPath.length() == 0) {
            return false; 
        }
        int count = 0;
        int specCount = 0; 
        for (int i = hopsPath.length() - 1; i >= 0; i--) {
            char c = hopsPath.charAt(i);
            if (c != Hop.NAVLINK.getHopChar()) {
                count++;
                if(c == Hop.SPECULATIVE.getHopChar()) {
                    specCount++;
                }
            } else {
                break;
            }
        }
        if (count <= 0) {
            return false;
        }
        
        if (specCount > curi.get(this, MAX_SPECULATIVE_HOPS)) {
            return false;
        }
        
        return count <= curi.get(this, MAX_TRANS_HOPS);
    }


}
