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



    public void testValidate() {
        validateDefaults();
        validateOverride1();
        validateOverride2();
        validateBundle();
    }

    private void validateDefaults() {
        SingleSheet sheet = manager.getDefault();
        Object o = PathValidator.validate(sheet, "html");
        System.out.println(o);
        assertTrue(html == PathValidator.validate(sheet, "html"));
        assertEquals(Boolean.TRUE, PathValidator.validate(sheet, "html.TREAT_FRAMES_AS_EMBED_LINKS"));
        assertEquals(Boolean.FALSE, PathValidator.validate(sheet, "html.IGNORE_FORM_ACTION_URLS"));
        assertEquals(Boolean.FALSE, PathValidator.validate(sheet, "html.IGNORE_UNEXPECTED_HTML"));
        assertEquals(Boolean.TRUE, PathValidator.validate(sheet, "html.OVERLY_EAGER_LINK_DETECTION"));
        assertTrue(htmlSeq == PathValidator.validate(sheet, "html.DECIDE_RULES"));
        assertTrue(htmlRules == PathValidator.validate(sheet, "html.DECIDE_RULES.RULES"));
        assertTrue(htmlRule0 == PathValidator.validate(sheet, "html.DECIDE_RULES.RULES.0"));
        
        assertTrue(css == PathValidator.validate(sheet, "css"));
        assertTrue(cssSeq == PathValidator.validate(sheet, "css.DECIDE_RULES"));
        assertTrue(cssRules == PathValidator.validate(sheet, "css.DECIDE_RULES.RULES"));
        assertTrue(cssRule0 == PathValidator.validate(sheet, "css.DECIDE_RULES.RULES.0"));
        assertTrue(cssRule1 == PathValidator.validate(sheet, "css.DECIDE_RULES.RULES.1"));
        assertTrue(cssRule1_list == PathValidator.validate(sheet, "css.DECIDE_RULES.RULES.1.RULES"));
        assertTrue(cssRule1_0 == PathValidator.validate(sheet, "css.DECIDE_RULES.RULES.1.RULES.0"));

        assertTrue(js == PathValidator.validate(sheet, "js"));
        assertTrue(jsSeq == PathValidator.validate(sheet, "js.DECIDE_RULES"));
        assertTrue(jsRules == PathValidator.validate(sheet, "js.DECIDE_RULES.RULES"));
    }
    
    
    private void validateOverride1() {
        Sheet sheet = manager.getSheet("override1");
        assertTrue(html == PathValidator.validate(sheet, "html"));
        assertEquals(Boolean.TRUE, PathValidator.validate(sheet, "html.TREAT_FRAMES_AS_EMBED_LINKS"));
        assertEquals(Boolean.FALSE, PathValidator.validate(sheet, "html.IGNORE_FORM_ACTION_URLS"));
        assertEquals(Boolean.FALSE, PathValidator.validate(sheet, "html.IGNORE_UNEXPECTED_HTML"));
        assertEquals(Boolean.TRUE, PathValidator.validate(sheet, "html.OVERLY_EAGER_LINK_DETECTION"));
        assertTrue(htmlSeq == PathValidator.validate(sheet, "html.DECIDE_RULES"));
        assertTrue(htmlRules == PathValidator.validate(sheet, "html.DECIDE_RULES.RULES"));
        assertTrue(htmlRule0 == PathValidator.validate(sheet, "html.DECIDE_RULES.RULES.0"));
        
        assertTrue(css == PathValidator.validate(sheet, "css"));
        assertTrue(cssSeq == PathValidator.validate(sheet, "css.DECIDE_RULES"));
        assertTrue(o1cssRules == PathValidator.validate(sheet, "css.DECIDE_RULES.RULES"));
        assertTrue(o1cssRule0 == PathValidator.validate(sheet, "css.DECIDE_RULES.RULES.0"));
        assertTrue(o1cssRule1 == PathValidator.validate(sheet, "css.DECIDE_RULES.RULES.1"));
        assertTrue(o1cssRule2 == PathValidator.validate(sheet, "css.DECIDE_RULES.RULES.2"));

        assertTrue(js == PathValidator.validate(sheet, "js"));
        assertTrue(jsSeq == PathValidator.validate(sheet, "js.DECIDE_RULES"));
        assertTrue(jsRules == PathValidator.validate(sheet, "js.DECIDE_RULES.RULES"));
    }

    
    private void validateOverride2() {
        Sheet sheet = manager.getSheet("override2");
        assertTrue(html == PathValidator.validate(sheet, "html"));
        assertEquals(Boolean.TRUE, PathValidator.validate(sheet, "html.TREAT_FRAMES_AS_EMBED_LINKS"));
        assertEquals(Boolean.FALSE, PathValidator.validate(sheet, "html.IGNORE_FORM_ACTION_URLS"));
        assertEquals(Boolean.TRUE, PathValidator.validate(sheet, "html.IGNORE_UNEXPECTED_HTML"));
        assertEquals(Boolean.TRUE, PathValidator.validate(sheet, "html.OVERLY_EAGER_LINK_DETECTION"));
        
        assertTrue(htmlSeq == PathValidator.validate(sheet, "html.DECIDE_RULES"));
        assertTrue(htmlRules == PathValidator.validate(sheet, "html.DECIDE_RULES.RULES"));
        assertTrue(htmlRule0 == PathValidator.validate(sheet, "html.DECIDE_RULES.RULES.0"));
        
        assertTrue(css == PathValidator.validate(sheet, "css"));
        assertTrue(cssSeq == PathValidator.validate(sheet, "css.DECIDE_RULES"));
        assertTrue(cssRules == PathValidator.validate(sheet, "css.DECIDE_RULES.RULES"));
        assertTrue(cssRule0 == PathValidator.validate(sheet, "css.DECIDE_RULES.RULES.0"));
        assertTrue(cssRule1 == PathValidator.validate(sheet, "css.DECIDE_RULES.RULES.1"));
        assertTrue(cssRule1_list == PathValidator.validate(sheet, "css.DECIDE_RULES.RULES.1.RULES"));
        assertTrue(cssRule1_0 == PathValidator.validate(sheet, "css.DECIDE_RULES.RULES.1.RULES.0"));

        assertTrue(js == PathValidator.validate(sheet, "js"));
        assertTrue(jsSeq == PathValidator.validate(sheet, "js.DECIDE_RULES"));
        assertTrue(jsRules == PathValidator.validate(sheet, "js.DECIDE_RULES.RULES"));
    }


    private void validateBundle() {
        Sheet sheet = manager.getSheet("bundle");
        assertTrue(html == PathValidator.validate(sheet, "html"));
        assertEquals(Boolean.TRUE, PathValidator.validate(sheet, "html.TREAT_FRAMES_AS_EMBED_LINKS"));
        assertEquals(Boolean.FALSE, PathValidator.validate(sheet, "html.IGNORE_FORM_ACTION_URLS"));
        assertEquals(Boolean.TRUE, PathValidator.validate(sheet, "html.IGNORE_UNEXPECTED_HTML"));
        assertEquals(Boolean.TRUE, PathValidator.validate(sheet, "html.OVERLY_EAGER_LINK_DETECTION"));
        assertTrue(htmlSeq == PathValidator.validate(sheet, "html.DECIDE_RULES"));
        assertTrue(htmlRules == PathValidator.validate(sheet, "html.DECIDE_RULES.RULES"));
        assertTrue(htmlRule0 == PathValidator.validate(sheet, "html.DECIDE_RULES.RULES.0"));
        
        assertTrue(css == PathValidator.validate(sheet, "css"));
        assertTrue(cssSeq == PathValidator.validate(sheet, "css.DECIDE_RULES"));
        assertTrue(o1cssRules == PathValidator.validate(sheet, "css.DECIDE_RULES.RULES"));
        assertTrue(o1cssRule0 == PathValidator.validate(sheet, "css.DECIDE_RULES.RULES.0"));
        assertTrue(o1cssRule1 == PathValidator.validate(sheet, "css.DECIDE_RULES.RULES.1"));
        assertTrue(o1cssRule2 == PathValidator.validate(sheet, "css.DECIDE_RULES.RULES.2"));

        assertTrue(js == PathValidator.validate(sheet, "js"));
        assertTrue(jsSeq == PathValidator.validate(sheet, "js.DECIDE_RULES"));
        assertTrue(jsRules == PathValidator.validate(sheet, "js.DECIDE_RULES.RULES"));        
    }
}
