/* Copyright (C) 2006 Internet Archive.
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
 *
 * DecideRuleSequence.java
 * Created on October 5, 2006
 *
 * $Header$
 */
package org.archive.modules.deciderules;


import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.modules.ProcessorURI;


public class DecideRuleSequence extends DecideRule  {
    final private static Logger LOGGER = 
        Logger.getLogger(DecideRuleSequence.class.getName());
    private static final long serialVersionUID = 3L;
    
    @SuppressWarnings("unchecked")
    public List<DecideRule> getRules() {
        return (List<DecideRule>) kp.get("rules");
    }
    public void setRules(List rules) {
        kp.put("rules", rules);
    }

    public DecideResult innerDecide(ProcessorURI uri) {
        DecideResult result = DecideResult.PASS;
        List<DecideRule> rules = getRules();
        int max = rules.size();
        for (int i = 0; i < max; i++) {
            DecideRule rule = rules.get(i);
            if (rule.onlyDecision(uri) != result) {
                DecideResult r = rule.decisionFor(uri);
                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.finest("DecideRule #" + i + " " + 
                            rule.getClass().getName() + " returned " + r);
                }
                if (r != DecideResult.PASS) {
                    result = r;
                }
            }
        }
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("DecideRuleSequence returned " + result);
        }
        return result;
    }
}
