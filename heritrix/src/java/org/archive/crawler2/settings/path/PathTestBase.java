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
 * PathTestBase.java
 * Created on October 24, 2006
 *
 * $Header$
 */
package org.archive.crawler2.settings.path;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.archive.crawler2.deciderules.AcceptDecideRule;
import org.archive.crawler2.deciderules.DecideRule;
import org.archive.crawler2.deciderules.DecideRuleSequence;
import org.archive.crawler2.deciderules.RejectDecideRule;
import org.archive.crawler2.extractor.ExtractorCSS;
import org.archive.crawler2.extractor.ExtractorHTML;
import org.archive.crawler2.extractor.ExtractorJS;
import org.archive.crawler2.settings.MemorySheetManager;
import org.archive.crawler2.settings.Sheet;
import org.archive.crawler2.settings.SheetManager;
import org.archive.crawler2.settings.SingleSheet;

import junit.framework.TestCase;

public class PathTestBase extends TestCase {

    SheetManager manager;

    // Objects in the default sheet
    ExtractorHTML html;
    DecideRuleSequence htmlSeq;
    List<DecideRule> htmlRules;
    AcceptDecideRule htmlRule0;
    
    ExtractorCSS css;
    DecideRuleSequence cssSeq;
    List<DecideRule> cssRules;
    RejectDecideRule cssRule0;
    DecideRuleSequence cssRule1;
    List<DecideRule> cssRule1_list;
    AcceptDecideRule cssRule1_0;
    
    ExtractorJS js;
    DecideRuleSequence jsSeq;
    List<DecideRule> jsRules;

    // Objects in sheet override1
    List<DecideRule> o1cssRules;
    AcceptDecideRule o1cssRule0;
    AcceptDecideRule o1cssRule1;
    AcceptDecideRule o1cssRule2;


    public void setUp() {
        manager = new MemorySheetManager();
        SingleSheet defaults = manager.getDefault();

        html = new ExtractorHTML();
        manager.addRoot("html", html);
        htmlSeq = new DecideRuleSequence();
        htmlRules = new ArrayList<DecideRule>();
        htmlRule0 = new AcceptDecideRule();
        htmlRules.add(htmlRule0);
        defaults.set(html, ExtractorHTML.IGNORE_UNEXPECTED_HTML, false);
        defaults.set(html, ExtractorHTML.DECIDE_RULES, htmlSeq);
        defaults.set(htmlSeq, DecideRuleSequence.RULES, htmlRules);

        css = new ExtractorCSS();
        manager.addRoot("css", css);
        cssSeq = new DecideRuleSequence();
        cssRules = new ArrayList<DecideRule>();
        cssRule0 = new RejectDecideRule();
        cssRule1 = new DecideRuleSequence();
        cssRule1_list = new ArrayList<DecideRule>();
        cssRule1_0 = new AcceptDecideRule();
        cssRules.add(cssRule0);
        cssRules.add(cssRule1);
        cssRule1_list.add(cssRule1_0);
        defaults.set(css, ExtractorCSS.DECIDE_RULES, cssSeq);
        defaults.set(cssSeq, DecideRuleSequence.RULES, cssRules);
        defaults.set(cssRule1, DecideRuleSequence.RULES, cssRule1_list);
        
        
        js = new ExtractorJS();
        manager.addRoot("js", js);
        jsSeq = new DecideRuleSequence();
        jsRules = new ArrayList<DecideRule>();
        defaults.set(js, ExtractorJS.DECIDE_RULES, jsSeq);
        defaults.set(jsSeq, DecideRuleSequence.RULES, jsRules);
        
        SingleSheet override1 = manager.createSingleSheet("override1");
        o1cssRules = new ArrayList<DecideRule>();
        o1cssRule0 = new AcceptDecideRule();
        o1cssRule1 = new AcceptDecideRule();
        o1cssRule2 = new AcceptDecideRule();
        o1cssRules.add(o1cssRule0);
        o1cssRules.add(o1cssRule1);
        o1cssRules.add(o1cssRule2);
        override1.set(cssSeq, DecideRuleSequence.RULES, o1cssRules);
        
        SingleSheet override2 = manager.createSingleSheet("override2");
        override2.set(html, ExtractorHTML.IGNORE_UNEXPECTED_HTML, true);
        
        manager.createSheetBundle("bundle", Arrays.asList(new Sheet[] { override2, override1 }));
    }

}
