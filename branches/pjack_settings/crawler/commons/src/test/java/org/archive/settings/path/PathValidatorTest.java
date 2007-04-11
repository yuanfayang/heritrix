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
 * PathValidatorTest.java
 * Created on October 24, 2006
 *
 * $Header$
 */
package org.archive.settings.path;


import java.util.Arrays;
import java.util.List;

import org.archive.settings.SettingsList;
import org.archive.settings.Sheet;
import org.archive.settings.SingleSheet;


/**
 * Unit test for PathValidator.  The test works by constructing an in-memory
 * chain of three root processors (ExtractorHTML, ExtractorCSS, and ExtractorJS,
 * in that order).  The default sheet is then modified so that the processors
 * have DecideRuleSequences.
 * 
 * <p>A new sheet named "override1" is then created.  It overrides the 
 * DecideRuleSequence associated with the ExtractorCSS.
 * 
 * <p>A second sheet named "override2" overrides various simple properties.
 * 
 * <p>And a final sheet bundled named "bundle" bundles override2 and 
 * override1 in that order.
 * 
 * <p>The test keeps references to every object in every configuration, and
 * then tests each valid path of each of the four sheets to make sure it
 * resolves to the correct object.
 * 
 * @author pjack
 */
public class PathValidatorTest extends PathTestBase {

/*
    public void testValidate() {
        validateDefaults();
        validateOverride1();
        validateOverride2();
        validateBundle();

        validateOfflineDefaults();
        validateOfflineOverride1();
        validateOfflineOverride2();
        validateOfflineBundle();
    }

    private void validateDefaults() {
        SingleSheet sheet = manager.getDefault();
        assertTrue(html == PathValidator.validate(sheet, "root.html"));
        assertEquals(Boolean.TRUE, PathValidator.validate(sheet, "root.html.treat-frames-as-embed-links"));
        assertEquals(Boolean.FALSE, PathValidator.validate(sheet, "root.html.ignore-form-action-urls"));
        assertEquals(Boolean.FALSE, PathValidator.validate(sheet, "root.html.ignore-unexpected-html"));
        assertEquals(Boolean.TRUE, PathValidator.validate(sheet, "root.html.overly-eager-link-detection"));
        assertTrue(htmlSeq == PathValidator.validate(sheet, "root.html.decide-rules"));
        assertTrue(htmlRules.equals(PathValidator.validate(sheet, "root.html.decide-rules.rules")));
        assertTrue(htmlRule0 == PathValidator.validate(sheet, "root.html.decide-rules.rules.0"));
        
        assertTrue(css == PathValidator.validate(sheet, "root.css"));
        assertTrue(cssSeq == PathValidator.validate(sheet, "root.css.decide-rules"));
        assertTrue(cssRules.equals(PathValidator.validate(sheet, "root.css.decide-rules.rules")));
        assertTrue(cssRule0 == PathValidator.validate(sheet, "root.css.decide-rules.rules.0"));
        assertTrue(cssRule1 == PathValidator.validate(sheet, "root.css.decide-rules.rules.1"));
        assertTrue(cssRule1_list.equals(PathValidator.validate(sheet, "root.css.decide-rules.rules.1.rules")));
        assertTrue(cssRule1_0 == PathValidator.validate(sheet, "root.css.decide-rules.rules.1.rules.0"));

        assertTrue(js == PathValidator.validate(sheet, "root.js"));
        assertTrue(jsSeq == PathValidator.validate(sheet, "root.js.decide-rules"));
        assertTrue(jsRules.equals(PathValidator.validate(sheet, "root.js.decide-rules.rules")));
    }
    
    
    private void validateOverride1() {
        SingleSheet sheet = (SingleSheet)manager.getSheet("override1");
        assertTrue(html == PathValidator.validate(sheet, "root.html"));
        assertEquals(Boolean.TRUE, PathValidator.validate(sheet, "root.html.treat-frames-as-embed-links"));
        assertEquals(Boolean.FALSE, PathValidator.validate(sheet, "root.html.ignore-form-action-urls"));
        assertEquals(Boolean.FALSE, PathValidator.validate(sheet, "root.html.ignore-unexpected-html"));
        assertEquals(Boolean.TRUE, PathValidator.validate(sheet, "root.html.overly-eager-link-detection"));
        assertTrue(htmlSeq == PathValidator.validate(sheet, "root.html.decide-rules"));
        assertTrue(htmlRules.equals(PathValidator.validate(sheet, "root.html.decide-rules.rules")));
        assertTrue(htmlRule0 == PathValidator.validate(sheet, "root.html.decide-rules.rules.0"));
        
        assertTrue(css == PathValidator.validate(sheet, "root.css"));
        assertTrue(cssSeq == PathValidator.validate(sheet, "root.css.decide-rules"));
        assertTrue(o1cssRules.equals(PathValidator.check(sheet, "root.css.decide-rules.rules")));
        
        // Lists are merged, so override1's elements should appear after 
        // default's elements.
        assertTrue(cssRule0 == PathValidator.validate(sheet, "root.css.decide-rules.rules.0"));
        assertTrue(cssRule1 == PathValidator.validate(sheet, "root.css.decide-rules.rules.1"));
        assertTrue(o1cssRule0 == PathValidator.validate(sheet, "root.css.decide-rules.rules.2"));
        assertTrue(o1cssRule1 == PathValidator.validate(sheet, "root.css.decide-rules.rules.3"));
        assertTrue(o1cssRule2 == PathValidator.validate(sheet, "root.css.decide-rules.rules.4"));

        assertTrue(js == PathValidator.validate(sheet, "root.js"));
        assertTrue(jsSeq == PathValidator.validate(sheet, "root.js.decide-rules"));
        assertTrue(jsRules.equals(PathValidator.validate(sheet, "root.js.decide-rules.rules")));
    }

    
    private void validateOverride2() {
        Sheet sheet = manager.getSheet("override2");
        assertTrue(html == PathValidator.validate(sheet, "root.html"));
        assertEquals(Boolean.TRUE, PathValidator.validate(sheet, "root.html.treat-frames-as-embed-links"));
        assertEquals(Boolean.FALSE, PathValidator.validate(sheet, "root.html.ignore-form-action-urls"));
        assertEquals(Boolean.TRUE, PathValidator.validate(sheet, "root.html.ignore-unexpected-html"));
        assertEquals(Boolean.TRUE, PathValidator.validate(sheet, "root.html.overly-eager-link-detection"));
        
        assertTrue(htmlSeq == PathValidator.validate(sheet, "root.html.decide-rules"));
        assertTrue(htmlRules.equals(PathValidator.validate(sheet, "root.html.decide-rules.rules")));
        assertTrue(htmlRule0 == PathValidator.validate(sheet, "root.html.decide-rules.rules.0"));
        
        assertTrue(css == PathValidator.validate(sheet, "root.css"));
        assertTrue(cssSeq == PathValidator.validate(sheet, "root.css.decide-rules"));
        assertTrue(cssRules.equals(PathValidator.validate(sheet, "root.css.decide-rules.rules")));

        assertTrue(cssRule0 == PathValidator.validate(sheet, "root.css.decide-rules.rules.0"));
        assertTrue(cssRule1 == PathValidator.validate(sheet, "root.css.decide-rules.rules.1"));
        assertTrue(cssRule1_list.equals(PathValidator.validate(sheet, "root.css.decide-rules.rules.1.rules")));
        assertTrue(cssRule1_0 == PathValidator.validate(sheet, "root.css.decide-rules.rules.1.rules.0"));

        assertTrue(js == PathValidator.validate(sheet, "root.js"));
        assertTrue(jsSeq == PathValidator.validate(sheet, "root.js.decide-rules"));
        assertTrue(jsRules.equals(PathValidator.validate(sheet, "root.js.decide-rules.rules")));
    }


    private void validateBundle() {
        Sheet sheet = manager.getSheet("bundle");
        assertTrue(html == PathValidator.validate(sheet, "root.html"));
        assertEquals(Boolean.TRUE, PathValidator.validate(sheet, "root.html.treat-frames-as-embed-links"));
        assertEquals(Boolean.FALSE, PathValidator.validate(sheet, "root.html.ignore-form-action-urls"));
        assertEquals(Boolean.TRUE, PathValidator.validate(sheet, "root.html.ignore-unexpected-html"));
        assertEquals(Boolean.TRUE, PathValidator.validate(sheet, "root.html.overly-eager-link-detection"));
        assertTrue(htmlSeq == PathValidator.validate(sheet, "root.html.decide-rules"));
        assertTrue(htmlRules.equals(PathValidator.validate(sheet, "root.html.decide-rules.rules")));
        assertTrue(htmlRule0 == PathValidator.validate(sheet, "root.html.decide-rules.rules.0"));
        
        
        assertTrue(css == PathValidator.validate(sheet, "root.css"));
        assertTrue(cssSeq == PathValidator.validate(sheet, "root.css.decide-rules"));
        List list = Arrays.asList(new Object[] { 
                cssRule0, 
                cssRule1, 
                o1cssRule0, 
                o1cssRule1, 
                o1cssRule2 });
        assertTrue(list.equals(PathValidator.validate(sheet, "root.css.decide-rules.rules")));
        assertTrue(cssRule0 == PathValidator.validate(sheet, "root.css.decide-rules.rules.0"));
        assertTrue(cssRule1 == PathValidator.validate(sheet, "root.css.decide-rules.rules.1"));
        assertTrue(o1cssRule0 == PathValidator.validate(sheet, "root.css.decide-rules.rules.2"));
        assertTrue(o1cssRule1 == PathValidator.validate(sheet, "root.css.decide-rules.rules.3"));
        assertTrue(o1cssRule2 == PathValidator.validate(sheet, "root.css.decide-rules.rules.4"));

        assertTrue(js == PathValidator.validate(sheet, "root.js"));
        assertTrue(jsSeq == PathValidator.validate(sheet, "root.js.decide-rules"));
        assertTrue(jsRules.equals(PathValidator.validate(sheet, "root.js.decide-rules.rules")));        
    }


    private void validateOfflineDefaults() {
        SingleSheet sheet = offlineManager.getDefault();
        assertTrue(offlineHtml == PathValidator.validate(sheet, "root.html"));
        assertEquals(Boolean.TRUE, PathValidator.validate(sheet, "root.html.treat-frames-as-embed-links"));
        assertEquals(Boolean.FALSE, PathValidator.validate(sheet, "root.html.ignore-form-action-urls"));
        assertEquals(Boolean.FALSE, PathValidator.validate(sheet, "root.html.ignore-unexpected-html"));
        assertEquals(Boolean.TRUE, PathValidator.validate(sheet, "root.html.overly-eager-link-detection"));
        assertTrue(offlineHtmlSeq == PathValidator.validate(sheet, "root.html.decide-rules"));
        assertTrue(offlineHtmlRules.equals(PathValidator.validate(sheet, "root.html.decide-rules.rules")));
        assertTrue(offlineHtmlRule0 == PathValidator.validate(sheet, "root.html.decide-rules.rules.0"));
        
        assertTrue(offlineCss == PathValidator.validate(sheet, "root.css"));
        assertTrue(offlineCssSeq == PathValidator.validate(sheet, "root.css.decide-rules"));
        assertTrue(offlineCssRules.equals(PathValidator.validate(sheet, "root.css.decide-rules.rules")));
        assertTrue(offlineCssRule0 == PathValidator.validate(sheet, "root.css.decide-rules.rules.0"));
        assertTrue(offlineCssRule1 == PathValidator.validate(sheet, "root.css.decide-rules.rules.1"));
        assertTrue(offlineCssRule1_list.equals(PathValidator.validate(sheet, "root.css.decide-rules.rules.1.rules")));
        assertTrue(offlineCssRule1_0 == PathValidator.validate(sheet, "root.css.decide-rules.rules.1.rules.0"));

        assertTrue(offlineJs == PathValidator.validate(sheet, "root.js"));
        assertTrue(offlineJsSeq == PathValidator.validate(sheet, "root.js.decide-rules"));
        assertTrue(offlineJsRules.equals(PathValidator.validate(sheet, "root.js.decide-rules.rules")));
    }


    private void validateOfflineOverride1() {
        SingleSheet sheet = (SingleSheet)offlineManager.getSheet("override1");
        assertTrue(offlineHtml == PathValidator.validate(sheet, "root.html"));
        assertEquals(Boolean.TRUE, PathValidator.validate(sheet, "root.html.treat-frames-as-embed-links"));
        assertEquals(Boolean.FALSE, PathValidator.validate(sheet, "root.html.ignore-form-action-urls"));
        assertEquals(Boolean.FALSE, PathValidator.validate(sheet, "root.html.ignore-unexpected-html"));
        assertEquals(Boolean.TRUE, PathValidator.validate(sheet, "root.html.overly-eager-link-detection"));
        assertTrue(offlineHtmlSeq == PathValidator.validate(sheet, "root.html.decide-rules"));
        assertTrue(offlineHtmlRules.equals(PathValidator.validate(sheet, "root.html.decide-rules.rules")));
        assertTrue(offlineHtmlRule0 == PathValidator.validate(sheet, "root.html.decide-rules.rules.0"));
        
        assertTrue(offlineCss == PathValidator.validate(sheet, "root.css"));
        assertTrue(offlineCssSeq == PathValidator.validate(sheet, "root.css.decide-rules"));        
        List list = Arrays.asList(new Object[] { 
                offlineCssRule0, 
                offlineCssRule1, 
                offlineO1cssRule0, 
                offlineO1cssRule1, 
                offlineO1cssRule2 });
        assertTrue(list.equals(PathValidator.validate(sheet, "root.css.decide-rules.rules")));

        assertTrue(offlineCssRule0 == PathValidator.validate(sheet, "root.css.decide-rules.rules.0"));
        assertTrue(offlineCssRule1 == PathValidator.validate(sheet, "root.css.decide-rules.rules.1"));
        assertTrue(offlineCssRule1_list.equals(PathValidator.validate(sheet, "root.css.decide-rules.rules.1.rules")));
        assertTrue(offlineCssRule1_0 == PathValidator.validate(sheet, "root.css.decide-rules.rules.1.rules.0"));
        assertTrue(offlineO1cssRule0 == PathValidator.validate(sheet, "root.css.decide-rules.rules.2"));
        assertTrue(offlineO1cssRule1 == PathValidator.validate(sheet, "root.css.decide-rules.rules.3"));
        assertTrue(offlineO1cssRule2 == PathValidator.validate(sheet, "root.css.decide-rules.rules.4"));

        assertTrue(offlineJs == PathValidator.validate(sheet, "root.js"));
        assertTrue(offlineJsSeq == PathValidator.validate(sheet, "root.js.decide-rules"));
        assertTrue(offlineJsRules.equals(PathValidator.validate(sheet, "root.js.decide-rules.rules")));
    }


    private void validateOfflineOverride2() {
        Sheet sheet = offlineManager.getSheet("override2");
        assertTrue(offlineHtml == PathValidator.validate(sheet, "root.html"));
        assertEquals(Boolean.TRUE, PathValidator.validate(sheet, "root.html.treat-frames-as-embed-links"));
        assertEquals(Boolean.FALSE, PathValidator.validate(sheet, "root.html.ignore-form-action-urls"));
        assertEquals(Boolean.TRUE, PathValidator.validate(sheet, "root.html.ignore-unexpected-html"));
        assertEquals(Boolean.TRUE, PathValidator.validate(sheet, "root.html.overly-eager-link-detection"));
        
        assertTrue(offlineHtmlSeq == PathValidator.validate(sheet, "root.html.decide-rules"));
        assertTrue(offlineHtmlRules.equals(PathValidator.validate(sheet, "root.html.decide-rules.rules")));
        assertTrue(offlineHtmlRule0 == PathValidator.validate(sheet, "root.html.decide-rules.rules.0"));
        
        assertTrue(offlineCss == PathValidator.validate(sheet, "root.css"));
        assertTrue(offlineCssSeq == PathValidator.validate(sheet, "root.css.decide-rules"));
        assertTrue(offlineCssRules.equals(PathValidator.validate(sheet, "root.css.decide-rules.rules")));
        assertTrue(offlineCssRule0 == PathValidator.validate(sheet, "root.css.decide-rules.rules.0"));
        assertTrue(offlineCssRule1 == PathValidator.validate(sheet, "root.css.decide-rules.rules.1"));
        assertTrue(offlineCssRule1_list.equals(PathValidator.validate(sheet, "root.css.decide-rules.rules.1.rules")));
        assertTrue(offlineCssRule1_0 == PathValidator.validate(sheet, "root.css.decide-rules.rules.1.rules.0"));

        assertTrue(offlineJs == PathValidator.validate(sheet, "root.js"));
        assertTrue(offlineJsSeq == PathValidator.validate(sheet, "root.js.decide-rules"));
        assertTrue(offlineJsRules.equals(PathValidator.validate(sheet, "root.js.decide-rules.rules")));
    }

    
    private void validateOfflineBundle() {
        Sheet sheet = offlineManager.getSheet("bundle");
        assertTrue(offlineHtml == PathValidator.validate(sheet, "root.html"));
        assertEquals(Boolean.TRUE, PathValidator.validate(sheet, "root.html.treat-frames-as-embed-links"));
        assertEquals(Boolean.FALSE, PathValidator.validate(sheet, "root.html.ignore-form-action-urls"));
        assertEquals(Boolean.TRUE, PathValidator.validate(sheet, "root.html.ignore-unexpected-html"));
        assertEquals(Boolean.TRUE, PathValidator.validate(sheet, "root.html.overly-eager-link-detection"));
        assertTrue(offlineHtmlSeq == PathValidator.validate(sheet, "root.html.decide-rules"));
        assertTrue(offlineHtmlRules.equals(PathValidator.validate(sheet, "root.html.decide-rules.rules")));
        assertTrue(offlineHtmlRule0 == PathValidator.validate(sheet, "root.html.decide-rules.rules.0"));

        assertTrue(offlineCss == PathValidator.validate(sheet, "root.css"));
        assertTrue(offlineCssSeq == PathValidator.validate(sheet, "root.css.decide-rules"));

        assertTrue(offlineCssRule0 == PathValidator.validate(sheet, "root.css.decide-rules.rules.0"));
        assertTrue(offlineCssRule1 == PathValidator.validate(sheet, "root.css.decide-rules.rules.1"));
        assertFalse(offlineCssRule1_list == PathValidator.validate(sheet, "root.css.decide-rules.rules.1.rules"));
        assertTrue(offlineCssRule1_0 == PathValidator.validate(sheet, "root.css.decide-rules.rules.1.rules.0"));
        assertTrue(offlineO1cssRule0 == PathValidator.validate(sheet, "root.css.decide-rules.rules.2"));
        assertTrue(offlineO1cssRule1 == PathValidator.validate(sheet, "root.css.decide-rules.rules.3"));
        assertTrue(offlineO1cssRule2 == PathValidator.validate(sheet, "root.css.decide-rules.rules.4"));

        assertTrue(offlineJs == PathValidator.validate(sheet, "root.js"));
        assertTrue(offlineJsSeq == PathValidator.validate(sheet, "root.js.decide-rules"));
        assertTrue(offlineJsRules.equals(PathValidator.validate(sheet, "root.js.decide-rules.rules")));        
    }
*/
}
