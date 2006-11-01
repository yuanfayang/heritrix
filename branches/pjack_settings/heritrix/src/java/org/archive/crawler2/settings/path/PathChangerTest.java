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
 * PathChangerTest.java
 * Created on October 24, 2006
 *
 * $Header$
 */
package org.archive.crawler2.settings.path;

import java.util.ArrayList;
import java.util.List;

import org.archive.crawler2.extractor.ExtractorJS;
import org.archive.crawler2.deciderules.DecideRule;
import org.archive.crawler2.deciderules.DecideRuleSequence;
import org.archive.crawler2.extractor.ExtractorHTML;
import org.archive.crawler2.settings.SingleSheet;

public class PathChangerTest extends PathTestBase {


    public void testChange() {
        SingleSheet defaults = manager.getDefault();
        List<PathChange> list = new ArrayList<PathChange>();
        list.add(new PathChange("html.ENABLED", "false"));
        new PathChanger().change(defaults, list);
        Boolean b = defaults.get(html, ExtractorHTML.ENABLED);
        assertEquals(Boolean.FALSE, b);
        
        list.clear();
        list.add(new PathChange("html.DECIDE_RULES.RULES.1._impl",
                "org.archive.crawler2.deciderules.MatchesRegExpDecideRule"));
        list.add(new PathChange("html.DECIDE_RULES.RULES.1.REGEXP", ".*?"));
        new PathChanger().change(defaults, list);
        assertEquals(2, this.htmlRules.size());
        
        list.clear();
        list.add(new PathChange("html.DECIDE_RULES.RULES._impl",
                "java.util.LinkedList"));
        new PathChanger().change(defaults, list);
        List<DecideRule> r = defaults.get(htmlSeq, DecideRuleSequence.RULES);
        assertFalse(r == htmlRules);
        assertEquals(0, r.size());
        
        list.clear();
        list.add(new PathChange("css._impl", 
                "org.archive.crawler2.extractor.ExtractorJS"));
        new PathChanger().change(defaults, list);
        ExtractorJS x = (ExtractorJS)manager.getRoot("css");
        DecideRuleSequence seq = defaults.get(x, ExtractorJS.DECIDE_RULES);
        assertTrue(seq == cssSeq);
        
        
    }

}
