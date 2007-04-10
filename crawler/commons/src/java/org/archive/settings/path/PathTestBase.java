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
package org.archive.settings.path;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
/*
import org.archive.processors.deciderules.AcceptDecideRule;
import org.archive.processors.deciderules.DecideRule;
import org.archive.processors.deciderules.DecideRuleSequence;
import org.archive.processors.deciderules.RejectDecideRule;
import org.archive.processors.extractor.ExtractorCSS;
import org.archive.processors.extractor.ExtractorHTML;
import org.archive.processors.extractor.ExtractorJS;*/
import org.archive.settings.MemorySheetManager;
import org.archive.settings.Offline;
import org.archive.settings.Sheet;
import org.archive.settings.SheetManager;
import org.archive.settings.SingleSheet;
import org.archive.state.ExampleConcreteProcessor;

import junit.framework.TestCase;

public class PathTestBase extends TestCase {
/*
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

    ExampleConcreteProcessor concrete;

    
    // Objects in sheet override1
    List<DecideRule> o1cssRules;
    AcceptDecideRule o1cssRule0;
    AcceptDecideRule o1cssRule1;
    AcceptDecideRule o1cssRule2;

    

    // For offline tests
    SheetManager offlineManager;
    
    Offline<ExtractorHTML> offlineHtml;
    Offline<DecideRuleSequence> offlineHtmlSeq;
    List<Offline> offlineHtmlRules;
    Offline<AcceptDecideRule> offlineHtmlRule0;

    Offline<ExtractorCSS> offlineCss;
    Offline<DecideRuleSequence> offlineCssSeq;
    List<Offline> offlineCssRules;
    Offline<RejectDecideRule> offlineCssRule0;
    Offline<DecideRuleSequence> offlineCssRule1;
    List<Offline> offlineCssRule1_list;
    Offline<AcceptDecideRule> offlineCssRule1_0;

    Offline<ExtractorJS> offlineJs;
    Offline<DecideRuleSequence> offlineJsSeq;
    List<Offline> offlineJsRules;

    Offline<ExampleConcreteProcessor> offlineConcrete;
    
    List<Offline> offlineO1cssRules;
    Offline<AcceptDecideRule> offlineO1cssRule0;
    Offline<AcceptDecideRule> offlineO1cssRule1;
    Offline<AcceptDecideRule> offlineO1cssRule2;
    
    
    public void setUp() {
        setUpOnline();
        setUpOffline();
    }
    
    
    private void setUpOnline() {
        Map<String,Object> map = new LinkedHashMap<String,Object>();
        manager = new MemorySheetManager(map);
        SingleSheet defaults = manager.getDefault();

        html = new ExtractorHTML();
        map.put("html", html);
        htmlSeq = new DecideRuleSequence();
        htmlRules = new ArrayList<DecideRule>();
        htmlRule0 = new AcceptDecideRule();
        htmlRules.add(htmlRule0);
        defaults.set(html, ExtractorHTML.IGNORE_UNEXPECTED_HTML, false);
        defaults.set(html, ExtractorHTML.DECIDE_RULES, htmlSeq);
        defaults.set(htmlSeq, DecideRuleSequence.RULES, htmlRules);

        css = new ExtractorCSS();
        map.put("css", css);
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
        map.put("js", js);
        jsSeq = new DecideRuleSequence();
        jsRules = new ArrayList<DecideRule>();
        defaults.set(js, ExtractorJS.DECIDE_RULES, jsSeq);
        defaults.set(jsSeq, DecideRuleSequence.RULES, jsRules);

        concrete = new ExampleConcreteProcessor();
        map.put("concrete", concrete);
        
        SingleSheet override1 = manager.addSingleSheet("override1");
        o1cssRules = new ArrayList<DecideRule>();
        o1cssRule0 = new AcceptDecideRule();
        o1cssRule1 = new AcceptDecideRule();
        o1cssRule2 = new AcceptDecideRule();
        o1cssRules.add(o1cssRule0);
        o1cssRules.add(o1cssRule1);
        o1cssRules.add(o1cssRule2);
        override1.set(cssSeq, DecideRuleSequence.RULES, o1cssRules);
        
        SingleSheet override2 = manager.addSingleSheet("override2");
        override2.set(html, ExtractorHTML.IGNORE_UNEXPECTED_HTML, true);

        manager.addSheetBundle("bundle", Arrays.asList(new Sheet[] { override2, override1 }));
    }


    private void setUpOffline() {
        Map<String,Object> map = new LinkedHashMap<String,Object>();
        offlineManager = new MemorySheetManager(map, false);
        SingleSheet defaults = offlineManager.getDefault();

        offlineHtml = Offline.make(ExtractorHTML.class);
        map.put("html", offlineHtml);
        offlineHtmlSeq = Offline.make(DecideRuleSequence.class);
        offlineHtmlRules = new ArrayList<Offline>();
        offlineHtmlRule0 = Offline.make(AcceptDecideRule.class);
        offlineHtmlRules.add(offlineHtmlRule0);
        defaults.setOffline(offlineHtml, ExtractorHTML.IGNORE_UNEXPECTED_HTML, false);
        defaults.setOffline(offlineHtml, ExtractorHTML.DECIDE_RULES, offlineHtmlSeq);
        defaults.setOffline(offlineHtmlSeq, DecideRuleSequence.RULES, offlineHtmlRules);

        offlineCss = Offline.make(ExtractorCSS.class);
        map.put("css", offlineCss);
        offlineCssSeq = Offline.make(DecideRuleSequence.class);
        offlineCssRules = new ArrayList<Offline>();
        offlineCssRule0 = Offline.make(RejectDecideRule.class);
        offlineCssRule1 = Offline.make(DecideRuleSequence.class);
        offlineCssRule1_list = new ArrayList<Offline>();
        offlineCssRule1_0 = Offline.make(AcceptDecideRule.class);
        offlineCssRules.add(offlineCssRule0);
        offlineCssRules.add(offlineCssRule1);
        offlineCssRule1_list.add(offlineCssRule1_0);
        defaults.setOffline(offlineCss, ExtractorCSS.DECIDE_RULES, offlineCssSeq);
        defaults.setOffline(offlineCssSeq, DecideRuleSequence.RULES, offlineCssRules);
        defaults.setOffline(offlineCssRule1, DecideRuleSequence.RULES, offlineCssRule1_list);
        
        offlineJs = Offline.make(ExtractorJS.class);
        map.put("js", offlineJs);
        offlineJsSeq = Offline.make(DecideRuleSequence.class);
        offlineJsRules = new ArrayList<Offline>();
        defaults.setOffline(offlineJs, ExtractorJS.DECIDE_RULES, offlineJsSeq);
        defaults.setOffline(offlineJsSeq, DecideRuleSequence.RULES, offlineJsRules);

        offlineConcrete = Offline.make(ExampleConcreteProcessor.class);
        map.put("concrete", concrete);
        
        SingleSheet override1 = offlineManager.addSingleSheet("override1");
        offlineO1cssRules = new ArrayList<Offline>();
        offlineO1cssRule0 = Offline.make(AcceptDecideRule.class);
        offlineO1cssRule1 = Offline.make(AcceptDecideRule.class);
        offlineO1cssRule2 = Offline.make(AcceptDecideRule.class);
        offlineO1cssRules.add(offlineO1cssRule0);
        offlineO1cssRules.add(offlineO1cssRule1);
        offlineO1cssRules.add(offlineO1cssRule2);
        override1.setOffline(offlineCssSeq, DecideRuleSequence.RULES, offlineO1cssRules);
        
        SingleSheet override2 = offlineManager.addSingleSheet("override2");
        override2.setOffline(offlineHtml, ExtractorHTML.IGNORE_UNEXPECTED_HTML, true);
        
        offlineManager.addSheetBundle("bundle", Arrays.asList(new Sheet[] { override2, override1 }));        
    }

*/
}
