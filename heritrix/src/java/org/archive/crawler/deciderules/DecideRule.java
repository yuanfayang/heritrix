/* DecideRule
*
* $Id$
*
* Created on Mar 3, 2005
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
package org.archive.crawler.deciderules;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.settings.ModuleType;


/**
 * Interface for rules which, given an object to evaluate,
 * respond with a decision: {@link DecideRule#ACCEPT}, 
 * {@link DecideRule#REJECT}, or 
 * {@link DecideRule#PASS}.
 * 
 * Rules return {@link #PASS} by default.
 *
 * @author gojomo
 * @see org.archive.crawler.deciderules.DecideRuleSequence
 */
public class DecideRule extends ModuleType {
    // enumeration of 'actions'
    public static final Object ACCEPT = "ACCEPT";
    public static final Object REJECT = "REJECT";
    public static final Object PASS = "PASS";

    /**
     * Constructor.
     * @param name Name of this rule.
     */
    public DecideRule(String name) {
        super(name);
    }

    /**
     * Make decision on passed <code>object</code>.
     * @param object Object to rule on.
     * @return {@link #ACCEPT}, {@link #REJECT}, or {@link #PASS}.
     */
    public Object decisionFor(Object object) {
        return PASS;
    }

    /**
     * Respond to a settings update, refreshing any internal settings-derived
     * state.
     */
    public void kickUpdate() {
        // by default do nothing
    }
    
    /**
     * Utility method to coerce a CandidateURI or UURI to 
     * plain string.
     * 
     * @param o
     * @return Passed object as string.
     */
    protected String asString(Object o) {
        String input;
        // TODO consider changing this to ask o for its matchString.
        if(o instanceof CandidateURI) {
            input = ((CandidateURI)o).getURIString();
        } else if (o instanceof UURI ){
            input = ((UURI)o).toString();
        } else {
            //TODO handle other inputs
            input = o.toString();
        }
        return input;
    }
}
