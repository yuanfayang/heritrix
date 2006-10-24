package org.archive.crawler2.extractor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.httpclient.URIException;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;

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
    protected Class getProcessorClass() {
        return ExtractorHTML.class;
    }

    @Override
    protected Extractor makeExtractor() {
        return new ExtractorHTML();
    }
    
    @Override
    protected Collection<TestData> makeData(String content, String destURI)
    throws URIException {
        List<TestData> result = new ArrayList<TestData>();
        UURI src = UURIFactory.getInstance("http://www.archive.org/start/");
        DefaultExtractorURI euri = new DefaultExtractorURI(src, 
                LinkContext.NAVLINK_MISC);
        euri.setContent(content, "text/html");
        
        UURI dest = UURIFactory.getInstance(destURI);
        LinkContext context = determineContext(content);
        Hop hop = determineHop(content);
        Link link = new Link(src, dest, context, hop);
        
        euri.setContent(content, "text/html");
        result.add(new TestData(euri.duplicate(), link));
        euri.setContent(content, "application/xhtml");
        result.add(new TestData(euri.duplicate(), link));
        
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
