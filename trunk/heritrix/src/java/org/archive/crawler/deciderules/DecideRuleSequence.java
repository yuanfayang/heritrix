/* RuleSequence
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

import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;

import org.archive.crawler.filter.OrFilter;
import org.archive.crawler.framework.Filter;
import org.archive.crawler.settings.MapType;
import org.archive.crawler.settings.ModuleType;
import org.archive.crawler.settings.Type;

/**
 * RuleSequence represents a series of Rules, which are applied in turn
 * to give the final result.
 *
 * @author gojomo
 */
public class DecideRuleSequence extends DecideRule {
    private static final Logger logger =
        Logger.getLogger(OrFilter.class.getName());
    
    public static final String ATTR_RULES = "rules";
    
    /**
     * @param name
     */
    public DecideRuleSequence(String name) {
        super(name);
        setDescription("DecideRuleSequence. Multiple DecideRules applied in " +
                "order, with the last non-PASS having final say.");
        
        addElementToDefinition(new MapType(ATTR_RULES,
                "This is a list of DecideRules to be applied in sequence.", 
                DecideRule.class));
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.rules.Rule#applyTo(java.lang.Object)
     */
    public Object decisionFor(Object object) {
        Object runningAnswer = PASS;
        Iterator iter = getRules(object).iterator(object);
        while(iter.hasNext()) {
            Object answer = ((DecideRule)iter.next()).decisionFor(object);
            if (answer != PASS) {
                runningAnswer = answer;
            }
        }
        return runningAnswer;
    }

    protected MapType getRules(Object o) {
        MapType rules = null;
        try {
            rules = (MapType)getAttribute(o, ATTR_RULES);
        } catch (AttributeNotFoundException e) {
            logger.severe(e.getLocalizedMessage());
        }
        return rules;
    }
}
