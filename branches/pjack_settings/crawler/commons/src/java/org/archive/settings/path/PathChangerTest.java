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
 * $Header: /cvsroot/archive-crawler/ArchiveOpenCrawler/src/java/org/archive/settings/path/Attic/PathChangerTest.java,v 1.1.2.4 2007/01/17 01:48:00 paul_jack Exp $
 */
package org.archive.settings.path;

import java.util.ArrayList;
import java.util.List;

//import org.archive.processors.deciderules.DecideRule;
//import org.archive.processors.deciderules.DecideRuleSequence;
//import org.archive.processors.extractor.ExtractorHTML;
import org.archive.settings.SingleSheet;

public class PathChangerTest extends PathTestBase {

/*
    public void testOnlineChange() {
        SingleSheet defaults = manager.getDefault();
        List<PathChange> list = new ArrayList<PathChange>();
        list.add(new PathChange("root.html.enabled", "boolean", "false"));
        new PathChanger().change(defaults, list);
        Boolean b = defaults.get(html, ExtractorHTML.ENABLED);
        assertEquals(Boolean.FALSE, b);
        
        list.clear();
        list.add(new PathChange("root.html.decide-rules.rules.1", "object",
                "org.archive.processors.deciderules.MatchesRegExpDecideRule"));
        list.add(new PathChange("root.html.decide-rules.rules.1.regexp",
                "pattern", ".*?"));
        new PathChanger().change(defaults, list);
        assertEquals(2, this.htmlRules.size());
        
        list.clear();
        list.add(new PathChange("root.html.decide-rules.rules", "object",
                "java.util.LinkedList"));
        new PathChanger().change(defaults, list);
        List<DecideRule> r = defaults.get(htmlSeq, DecideRuleSequence.RULES);
        assertFalse(r == htmlRules);
        assertEquals(0, r.size());
    }
    
    
    public void testOfflineChange() {
        SingleSheet defaults = offlineManager.getDefault();
        List<PathChange> list = new ArrayList<PathChange>();
        list.add(new PathChange("root.html.enabled", "boolean", "false"));
        new PathChanger().change(defaults, list);
        Boolean b = defaults.get(offlineHtml, ExtractorHTML.ENABLED);
        assertEquals(Boolean.FALSE, b);
        
        list.clear();
        list.add(new PathChange("root.html.decide-rules.rules.1", "object",
                "org.archive.processors.deciderules.MatchesRegExpDecideRule"));
        list.add(new PathChange("root.html.decide-rules.rules.1.regexp",
                "pattern", ".*?"));
        new PathChanger().change(defaults, list);
        assertEquals(2, this.offlineHtmlRules.size());
        
        list.clear();
        list.add(new PathChange("root.html.decide-rules.rules", "object",
                "java.util.LinkedList"));
        new PathChanger().change(defaults, list);
        List r = (List)defaults.resolve(offlineHtmlSeq, DecideRuleSequence.RULES).getValue();
        assertFalse(r == offlineHtmlRules);
        assertEquals(0, r.size());

    }
*/
}
