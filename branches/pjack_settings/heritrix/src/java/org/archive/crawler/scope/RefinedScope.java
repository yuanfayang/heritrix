/* RefinedScope
*
* $Id$
*
* Created on Jul 16, 2004
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
package org.archive.crawler.scope;


import org.archive.processors.ProcessorURI;
import org.archive.processors.deciderules.DecideResult;
import org.archive.processors.deciderules.DecideRule;
import org.archive.state.Key;

/**
 * Superclass for Scopes which make use of "additional focus"
 * to add items by pattern, or want to swap in alternative
 * transitive filter. 
 * 
 * @author gojomo
 */
public abstract class RefinedScope extends ClassicScope {
    
    
    final public static Key<DecideRule> TRANSITIVE_RULE = 
        Key.makeNull(DecideRule.class);
    
    final public static Key<DecideRule> ADDITIONAL_FOCUS_RULE = 
        Key.makeNull(DecideRule.class);


//    Filter additionalFocusFilter;
//    Filter transitiveFilter;

    public RefinedScope() {
        super();
    }

    /**
     * @param o
     * @return True if transitive filter accepts passed object.
     */
    @Override
    protected boolean transitiveAccepts(ProcessorURI o) {
        DecideRule rule = o.get(this, TRANSITIVE_RULE);
        return rule.decisionFor(o) == DecideResult.ACCEPT;
    }

    
    @Override
    protected boolean additionalFocusAccepts(ProcessorURI o) {
        DecideRule rule = o.get(this, ADDITIONAL_FOCUS_RULE);
        return rule.decisionFor(o) == DecideResult.ACCEPT;
    }

}
