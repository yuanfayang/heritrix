/* Copyright (C) 2003 Internet Archive.
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
 * BasicScope.java
 * Created on Oct 1, 2003
 *
 * $Header$
 */
package org.archive.crawler.scope;


import org.archive.crawler.framework.CrawlScope;
import org.archive.processors.ProcessorURI;
import org.archive.processors.deciderules.DecideResult;
import org.archive.processors.deciderules.DecideRuleSequence;
import org.archive.state.Key;
import org.archive.state.StateProvider;

/**
 * DecidingScope: a Scope which makes its accept/reject decision based on 
 * whatever DecideRules have been set up inside it.
 * @author gojomo
 */
public class DecidingScope extends CrawlScope {

    private static final long serialVersionUID = 3L;

    //private static Logger logger =
    //    Logger.getLogger(DecidingScope.class.getName());
    
    final public static Key<DecideRuleSequence> DECIDE_RULES = Key.make(new DecideRuleSequence());
    public static final String ATTR_DECIDE_RULES = "decide-rules";

    public DecidingScope() {
        super();
    }

    
    @Override
    protected DecideResult innerDecide(ProcessorURI uri) {
        return uri.get(this, DECIDE_RULES).decisionFor(uri);
    }
}
