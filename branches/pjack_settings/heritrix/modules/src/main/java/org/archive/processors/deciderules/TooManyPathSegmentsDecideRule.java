/* AcceptRule
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
package org.archive.processors.deciderules;

import org.archive.processors.ProcessorURI;
import org.archive.state.Key;
import org.archive.state.KeyManager;



/**
 * Rule REJECTs any CrawlURIs whose total number of path-segments (as
 * indicated by the count of '/' characters not including the first '//')
 * is over a given threshold.
 *
 * @author gojomo
 */
public class TooManyPathSegmentsDecideRule extends PredicatedRejectDecideRule {

    private static final long serialVersionUID = 3L;

    
    /**
     * Number of path segments beyond which this rule will reject URIs.
     */
    final public static Key<Integer> MAX_PATH_DEPTH = Key.make(20);

    static {
        KeyManager.addKeys(TooManyPathSegmentsDecideRule.class);
    }

    /**
     * Usual constructor. 
     */
    public TooManyPathSegmentsDecideRule() {
    }

    /**
     * Evaluate whether given object is over the threshold number of
     * path-segments.
     * 
     * @param object
     * @return true if the path-segments is exceeded
     */
    @Override
    protected boolean evaluate(ProcessorURI curi) {
        String uri = curi.toString();
        int count = 0;
        int threshold = curi.get(this, MAX_PATH_DEPTH);
        for (int i = 0; i < uri.length(); i++) {
            if (uri.charAt(i) == '/') {
                count++;
            }
            if (count > threshold) {
                return true;
            }
        }
        return false;
    }

}
