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
 * ExtractorCSSTest.java
 * Created on October 5, 2006
 *
 * $Header$
 */
package org.archive.crawler2.extractor;


import java.util.HashSet;

import org.archive.net.UURI;
import org.archive.net.UURIFactory;


/**
 * Unit test for ExtractorCSS.
 * 
 * @author pjack
 */
public class ExtractorCSSTest extends ContentExtractorTestBase {

    
    /**
     * Test data. a[n] is sample CSS input, a[n + 1] is expected extracted URI
     */
    final public static String[] VALID_TEST_DATA = new String[] {
        "@import url(http://www.archive.org)", 
        "http://www.archive.org",

        "@import url('http://www.archive.org')", 
        "http://www.archive.org",

        "@import url(    \"  http://www.archive.org  \"   )", 
        "http://www.archive.org",

        "table { border: solid black 1px}\n@import url(style.css)", 
        "http://www.archive.org/start/style.css",

    };
    
    @Override
    protected Class getProcessorClass() {
        return ExtractorCSS.class;
    }
    
    @Override
    protected Extractor makeExtractor() {
        return new ExtractorCSS();
    }
    
    
    /**
     * Tests each CSS/URI pair in the test data array.
     * 
     * @throws Exception   just in case
     */
    public void testExtraction() throws Exception {
        for (int i = 0; i < VALID_TEST_DATA.length; i += 2) {
            testOne(VALID_TEST_DATA[i], VALID_TEST_DATA[i + 1]);
        }
    }
    
    
    /**
     * Runs the given CSS text through ExtractorCSS, expecting the given
     * URL to be extracted.
     * 
     * @param css    the CSS text to process
     * @param expectedURL   the URL that should be extracted from the CSS
     * @throws Exception  just in case
     */
    private void testOne(String css, String expectedURL) throws Exception {
        UURI uuri = UURIFactory.getInstance("http://www.archive.org/start/");
        DefaultExtractorURI euri = new DefaultExtractorURI(uuri, null);
        euri.setContent(css, "text/css");
        ExtractorCSS extractor = new ExtractorCSS();
        extractor.process(euri);
        
        Link link = new Link(euri.getUURI(), 
                UURIFactory.getInstance(expectedURL),
                LinkContext.EMBED_MISC, Hop.EMBED);
        HashSet<Link> expected = new HashSet<Link>();
        expected.add(link);
        assertEquals(expected, euri.getOutLinks());
        assertNoSideEffects(euri);
    }

}
