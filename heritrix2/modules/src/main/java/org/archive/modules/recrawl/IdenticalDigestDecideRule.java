/* IdenticalDigestDecideRule
*
* $Id: HopsPathMatchesRegExpDecideRule.java 4649 2006-09-25 17:16:55 +0000 (Mon, 25 Sep 2006) paul_jack $
*
* Created on Feb 17, 2007
*
* Copyright (C) 2007 Internet Archive.
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
package org.archive.modules.recrawl;


import java.util.Map;

import org.archive.modules.ProcessorURI;
import org.archive.modules.deciderules.PredicatedRejectDecideRule;

import static org.archive.modules.recrawl.RecrawlAttributeConstants.*;


/**
 * Rule applies configured decision to any CrawlURIs whose prior-history
 * content-digest matches the latest fetch. 
 *
 * @author gojomo
 */
public class IdenticalDigestDecideRule extends PredicatedRejectDecideRule {

    private static final long serialVersionUID = 4275993790856626949L;

    /**
     * Usual constructor. 
     */
    public IdenticalDigestDecideRule() {
    }

    /**
     * Evaluate whether given CrawlURI's content-digest exactly 
     * matches that of preceding fetch. 
     *
     * @param object should be CrawlURI
     * @return true if current-fetch content-digest matches previous
     */
    protected boolean evaluate(ProcessorURI curi) {
        return hasIdenticalDigest(curi);
    }


    /**
     * Utility method for testing if a CrawlURI's last two history 
     * entiries (one being the most recent fetch) have identical 
     * content-digest information. 
     * 
     * @param curi CrawlURI to test
     * @return true if last two history entries have identical digests, 
     * otherwise false
     */
    public static boolean hasIdenticalDigest(ProcessorURI curi) {
        if(curi.containsDataKey(A_FETCH_HISTORY)) {
            Map[] history = (Map[])curi.getData().get(A_FETCH_HISTORY);
            return history[0] != null 
                   && history[0].containsKey(A_CONTENT_DIGEST)
                   && history[1] != null
                   && history[1].containsKey(A_CONTENT_DIGEST)
                   && history[0].get(A_CONTENT_DIGEST).equals(
                           history[1].get(A_CONTENT_DIGEST));
        } else {
            return false;
        }
    }

}
