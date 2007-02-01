package org.archive.processors.extractor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.archive.net.UURI;
import org.archive.net.UURIFactory;
import org.archive.processors.DefaultProcessorURI;
import org.archive.util.Recorder;

public class ExtractorHTMLTest extends StringExtractorTestBase {

    
    final public static String[] VALID_TEST_DATA = new String[] {
        "<a href=\"http://www.slashdot.org\">yellow journalism</a> A",
        "http://www.slashdot.org",

        "<a href='http://www.slashdot.org'>yellow journalism</a> A",
        "http://www.slashdot.org",

        "<a href=http://www.slashdot.org>yellow journalism</a> A",
        "http://www.slashdot.org",

        "<a href=\"http://www.slashdot.org\">yellow journalism A",
        "http://www.slashdot.org",

        "<a href='http://www.slashdot.org'>yellow journalism A",
        "http://www.slashdot.org",

        "<a href=http://www.slashdot.org>yellow journalism A",
        "http://www.slashdot.org",

        "<a href=\"http://www.slashdot.org\"/>yellow journalism A",
        "http://www.slashdot.org",

        "<a href='http://www.slashdot.org'/>yellow journalism A",
        "http://www.slashdot.org",

        "<a href=http://www.slashdot.org/>yellow journalism A",
        "http://www.slashdot.org",

        "<img src=\"foo.gif\"> IMG",
        "http://www.archive.org/start/foo.gif",
    };
    
        
    @Override
    protected String[] getValidTestData() {
        return VALID_TEST_DATA;
    }

    @Override
    protected Class getModuleClass() {
        return ExtractorHTML.class;
    }

    @Override
    protected Extractor makeExtractor() {
        return new ExtractorHTML();
    }
    
    @Override
    protected Collection<TestData> makeData(String content, String destURI)
    throws Exception {
        List<TestData> result = new ArrayList<TestData>();
        UURI src = UURIFactory.getInstance("http://www.archive.org/start/");
        DefaultProcessorURI euri = new DefaultProcessorURI(src, 
                LinkContext.NAVLINK_MISC);
        Recorder recorder = createRecorder(content);
        euri.setContentType("text/html");
        euri.setRecorder(recorder);
        euri.setContentLength(content.length());
                
        UURI dest = UURIFactory.getInstance(destURI);
        LinkContext context = determineContext(content);
        Hop hop = determineHop(content);
        Link link = new Link(src, dest, context, hop);
        result.add(new TestData(euri, link));
        
        euri = new DefaultProcessorURI(src, LinkContext.NAVLINK_MISC);
        recorder = createRecorder(content);
        euri.setContentType("application/xhtml");
        euri.setRecorder(recorder);
        euri.setContentLength(content.length());
        result.add(new TestData(euri, link));
        
        return result;
    }

    
    private static Hop determineHop(String s) {
        if (s.endsWith(" IMG")) {
            return Hop.EMBED;
        }
        return Hop.NAVLINK;
    }
    
    
    private static LinkContext determineContext(String s) {
        if (s.endsWith(" A")) {
            return new HTMLLinkContext("a/@href");
        }
        if (s.endsWith(" IMG")) {
            return new HTMLLinkContext("img/@src");
        }
        return LinkContext.NAVLINK_MISC;
    }

}
