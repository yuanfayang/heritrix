/* Rule
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

import org.archive.crawler.settings.ModuleType;


/**
 * Interface for rules which, given an object to evaluate,
 * respond with a decision: Rule.ACCEPT, Rule.REJECT, or 
 * Rule.PASS. This is friendlier to chaining and common
 * usage scenarios than strictly binary filtering, and 
 * may be less confusing in its terminology/effect/nesting.
 *
 * @author gojomo
 */
public class DecideRule extends ModuleType {
    // enumeration of 'actions'
    public static final String ACCEPT = "ACCEPT";
    public static final String REJECT = "REJECT";
    public static final String PASS = "PASS";

    /**
     * @param name
     */
    public DecideRule(String name) {
        super(name);
        // TODO Auto-generated constructor stub
    }

    public Object decisionFor(Object object) {
        return PASS;
    }

    /**
     * Respond to a settings update, refreshing any internal settings-derived
     * state
     */
    public void kickUpdate() {
        // by default do nothing
    }
}
