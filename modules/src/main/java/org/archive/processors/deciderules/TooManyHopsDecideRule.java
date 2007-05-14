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



/**
 * Rule REJECTs any CrawlURIs whose total number of hops (length of the 
 * hopsPath string, traversed links of any type) is over a threshold.
 * Otherwise returns PASS.
 *
 * @author gojomo
 */
public class TooManyHopsDecideRule extends PredicatedRejectDecideRule {

    private static final long serialVersionUID = 3L;

    
    /**
     * Max path depth for which this filter will match.
     */
    final public static Key<Integer> MAX_HOPS = Key.make(20);

    
    /**
     * Default access so available to test code.
     */
    static final Integer DEFAULT_MAX_HOPS = new Integer(20);

    /**
     * Usual constructor. 
     */
    public TooManyHopsDecideRule() {
    }

    /**
     * Evaluate whether given object is over the threshold number of
     * hops.
     * 
     * @param object
     * @return true if the mx-hops is exceeded
     */
    @Override
    protected boolean evaluate(ProcessorURI uri) {
        String hops = uri.getPathFromSeed();
        if (hops == null) {
            return false;
        }
        if (hops.length() <= uri.get(this, MAX_HOPS)) {
            return false;
        }
        return true;
    }

}
