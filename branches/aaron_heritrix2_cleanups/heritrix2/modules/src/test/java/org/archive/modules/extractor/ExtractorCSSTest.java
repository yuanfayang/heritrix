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
    protected Class getModuleClass() {
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
