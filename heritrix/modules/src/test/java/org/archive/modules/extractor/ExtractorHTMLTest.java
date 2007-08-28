package org.archive.modules.extractor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.httpclient.URIException;
import org.archive.modules.DefaultProcessorURI;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;
import org.archive.state.ExampleStateProvider;
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
        ExtractorHTML result = new ExtractorHTML();
        UriErrorLoggerModule ulm = new UnitTestUriLoggerModule();
        ExampleStateProvider dsp = new ExampleStateProvider();
        dsp.set(result, Extractor.URI_ERROR_LOGGER_MODULE, ulm);
        result.initialTasks(dsp);
        return result;
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

    /**
     * Test a missing whitespace issue found in form
     * 
     * [HER-1128] ExtractorHTML fails to extract FRAME SRC link without
     * whitespace before SRC http://webteam.archive.org/jira/browse/HER-1128
     */
    public void testNoWhitespaceBeforeValidAttribute() throws URIException {
        expectSingleLink(
                "http://expected.example.com/",
                "<frame name=\"main\"src=\"http://expected.example.com/\"> ");
    }
    
    /**
     * Expect the extractor to find the single given URI in the supplied
     * source material. Fail if that one lik is not found. 
     * 
     * TODO: expand to capture expected Link instance characteristics 
     * (source, hop, context, etc?)
     * 
     * @param expected String target URI that should be extracted
     * @param source CharSequence source material to extract
     * @throws URIException
     */
    protected void expectSingleLink(String expected, CharSequence source) throws URIException {
        DefaultProcessorURI puri = new DefaultProcessorURI(UURIFactory
                .getInstance("http://www.example.com"), null);
        ExtractorHTML extractor = (ExtractorHTML)makeExtractor();
        extractor.extract(puri, source);
        Link[] links = puri.getOutLinks().toArray(new Link[0]);
        assertTrue("did not find single link",links.length==1);
        assertTrue("expected link not found", 
                links[0].getDestination().toString().equals(expected));
    }
}
