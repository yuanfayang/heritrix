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
package org.archive.modules.extractor;

import java.util.Collection;
import java.util.Collections;

import org.archive.modules.DefaultProcessorURI;
import org.archive.modules.extractor.Extractor;
import org.archive.modules.extractor.ExtractorCSS;
import org.archive.modules.extractor.Hop;
import org.archive.modules.extractor.Link;
import org.archive.modules.extractor.StringExtractorTestBase;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;
import org.archive.state.ExampleStateProvider;
import org.archive.util.Recorder;

import static org.archive.modules.extractor.LinkContext.EMBED_MISC;
import static org.archive.modules.extractor.LinkContext.NAVLINK_MISC;


/**
 * Unit test for ExtractorCSS.
 * 
 * @author pjack
 */
public class ExtractorCSSTest extends StringExtractorTestBase {
    
    public void testUrlNoQuotes() throws Exception {
        testOne("@import url(http://www.archive.org)", 
                "http://www.archive.org");
    }

    public void testImportQuotedUrl() throws Exception {
        testOne("@import \"http://www.archive.org\";", 
                "http://www.archive.org");
    }
    
    /**
     * this one, identical to the last minus trailing semicolon, fails
     * TODO: should it succeed by spec or what browsers do?  
     */
    public void xestImportQuotedUrlNoSemicolon() throws Exception {
        testOne("@import \"http://www.archive.org\"", 
                "http://www.archive.org");
    }
    
    public void testImportSingleQuoteUrl() throws Exception {
        testOne("@import url('http://www.archive.org')", 
                "http://www.archive.org");
    }
    
    public void testImportSpaceDoubleQuotesUrl() throws Exception {
        testOne("@import url(    \"  http://www.archive.org  \"   )", 
                "http://www.archive.org");
    }
    
    public void testRelative() throws Exception {
        // FIXME: I don't think this is legal CSS
        testOne("table { border: solid black 1px}\n@import url(style.css)", 
                "http://www.archive.org/start/style.css");
    }
    
    public void testParensInQuotedUrl() throws Exception {
        testOne("@import \"http://www.archive.org/index-new(2).css\";",
                "http://www.archive.org/index-new(2).css");
    }
    
    public void testParensInUrlUnquotedUrl() throws Exception {
        testOne("@import \"http://www.archive.org/index-new(2).css\";",
                "http://www.archive.org/index-new(2).css");
    }
    public void testParensInUrlQuotedUrl() throws Exception {
        testOne("@import url(\"http://www.archive.org/index-new(2).css\")",
                "http://www.archive.org/index-new(2).css");
    }
    
    /**
     * Fails currently, see http://webteam.archive.org/jira/browse/HER-1578
     */  
    public void xestParensInUrlUnquotedUrlNoSemicolon() throws Exception {
        testOne("@import url(http://www.archive.org/index-new(2).css)",
                "http://www.archive.org/index-new(2).css");
    }

    
    /**
     * Currently fails; see [HER-1537]
     */
    public void xestInlineComment() throws Exception {
        testOne("@import /**/\"http://www.archive.org\";", 
                "http://www.archive.org");
    }
    
    // TODO: add test of url() inside styles, not with @import

    /**
     * Test data. a[n] is sample input, a[n + 1] is expected extracted URI
     */
    final public static String[] VALID_TEST_DATA = new String[] {
        /// replaced with named per-method tests
    };

    @Override
    protected Class<ExtractorCSS> getModuleClass() {
        return ExtractorCSS.class;
    }
    
    @Override
    protected Extractor makeExtractor() {
        ExtractorCSS result = new ExtractorCSS();
        UriErrorLoggerModule ulm = new UnitTestUriLoggerModule();
        ExampleStateProvider dsp = new ExampleStateProvider();
        dsp.set(result, Extractor.URI_ERROR_LOGGER_MODULE, ulm);
        result.initialTasks(dsp);
        return result;    
    }
 

    @Override
    protected Collection<TestData> makeData(String content, String uri) 
    throws Exception {
        UURI src = UURIFactory.getInstance("http://www.archive.org/start/");
        DefaultProcessorURI euri = new DefaultProcessorURI(src, NAVLINK_MISC);
        Recorder recorder = createRecorder(content);
        euri.setContentType("text/css");
        euri.setRecorder(recorder);
        euri.setContentLength(content.length());
        
        UURI dest = UURIFactory.getInstance(uri);
        Link link = new Link(src, dest, EMBED_MISC, Hop.EMBED);
        TestData td = new TestData(euri, link);
        return Collections.singleton(td);
    }


    @Override
    public String[] getValidTestData() {
        return VALID_TEST_DATA;
    }

}
