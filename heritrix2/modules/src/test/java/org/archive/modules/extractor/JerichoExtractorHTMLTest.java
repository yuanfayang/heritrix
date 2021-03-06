/* JerichoExtractorHTMLTest
 *
 * Copyright (C) 2006 Olaf Freyer
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
 * 
 */
package org.archive.modules.extractor;


import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.httpclient.URIException;
import org.archive.modules.DefaultProcessorURI;
import org.archive.modules.ProcessorURI;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;
import org.archive.state.ExampleStateProvider;


/**
 * Test html extractor.
 *
 * @author stack
 * @version $Revision$, $Date$
 */
public class JerichoExtractorHTMLTest extends ExtractorHTMLTest {
    
    
    @Override
    protected Extractor makeExtractor() {
        JerichoExtractorHTML result = new JerichoExtractorHTML();
        UriErrorLoggerModule ulm = new UnitTestUriLoggerModule();
        ExampleStateProvider dsp = new ExampleStateProvider();
        dsp.set(result, Extractor.URI_ERROR_LOGGER_MODULE, ulm);
        result.initialTasks(dsp);
        return result;
    }
    
    
    /**
     * Test a GET FORM ACTION extraction
     * 
     * @throws URIException
     */
    public void testFormsLinkGet() throws URIException {
        UURI uuri = UURIFactory.getInstance("http://www.example.org");
        ProcessorURI curi = new DefaultProcessorURI(uuri, null);
        CharSequence cs = 
        	"<form name=\"testform\" method=\"GET\" action=\"redirect_me?form=true\"> " +
        	"  <INPUT TYPE=CHECKBOX NAME=\"checked[]\" VALUE=\"1\" CHECKED> "+
        	"  <INPUT TYPE=CHECKBOX NAME=\"unchecked[]\" VALUE=\"1\"> " +
        	"  <select name=\"selectBox\">" +
        	"    <option value=\"selectedOption\" selected>option1</option>" +
        	"    <option value=\"nonselectedOption\">option2</option>" +
        	"  </select>" +
        	"  <input type=\"submit\" name=\"test\" value=\"Go\">" +
        	"</form>";   
        JerichoExtractorHTML ex = (JerichoExtractorHTML)makeExtractor();
        ex.extract(curi, cs);
        curi.getOutLinks();
        assertTrue(CollectionUtils.exists(curi.getOutLinks(), new Predicate() {
            public boolean evaluate(Object object) {
                return ((Link) object).getDestination().toString().indexOf(
                        "/redirect_me?form=true&checked[]=1&unchecked[]=&selectBox=selectedOption&test=Go")>=0;
            }
        }));
    }
    
    /**
     * Test a POST FORM ACTION being properly ignored 
     * 
     * @throws URIException
     */
    public void testFormsLinkIgnorePost() throws URIException {
        UURI uuri = UURIFactory.getInstance("http://www.example.org");
        ProcessorURI curi = new DefaultProcessorURI(uuri, null);
        CharSequence cs = 
            "<form name=\"testform\" method=\"POST\" action=\"redirect_me?form=true\"> " +
            "  <INPUT TYPE=CHECKBOX NAME=\"checked[]\" VALUE=\"1\" CHECKED> "+
            "  <INPUT TYPE=CHECKBOX NAME=\"unchecked[]\" VALUE=\"1\"> " +
            "  <select name=\"selectBox\">" +
            "    <option value=\"selectedOption\" selected>option1</option>" +
            "    <option value=\"nonselectedOption\">option2</option>" +
            "  </select>" +
            "  <input type=\"submit\" name=\"test\" value=\"Go\">" +
            "</form>";   
        JerichoExtractorHTML ex = (JerichoExtractorHTML)makeExtractor();
        ex.extract(curi, cs);
        curi.getOutLinks();
        assertTrue(!CollectionUtils.exists(curi.getOutLinks(), new Predicate() {
            public boolean evaluate(Object object) {
                return ((Link) object).getDestination().toString().indexOf(
                        "/redirect_me?form=true&checked[]=1&unchecked[]=&selectBox=selectedOption&test=Go")>=0;
            }
        }));
    }
    
    /**
     * Test a POST FORM ACTION being found with non-default setting
     * 
     * @throws URIException
     */
    public void testFormsLinkFindPost() throws URIException {
        UURI uuri = UURIFactory.getInstance("http://www.example.org");
        DefaultProcessorURI curi = new DefaultProcessorURI(uuri, null);
        CharSequence cs = 
            "<form name=\"testform\" method=\"POST\" action=\"redirect_me?form=true\"> " +
            "  <INPUT TYPE=CHECKBOX NAME=\"checked[]\" VALUE=\"1\" CHECKED> "+
            "  <INPUT TYPE=CHECKBOX NAME=\"unchecked[]\" VALUE=\"1\"> " +
            "  <select name=\"selectBox\">" +
            "    <option value=\"selectedOption\" selected>option1</option>" +
            "    <option value=\"nonselectedOption\">option2</option>" +
            "  </select>" +
            "  <input type=\"submit\" name=\"test\" value=\"Go\">" +
            "</form>";   
        JerichoExtractorHTML ex = (JerichoExtractorHTML)makeExtractor();
        curi.set(ex, ExtractorHTML.EXTRACT_ONLY_FORM_GETS, Boolean.FALSE);
        ex.extract(curi, cs);
        curi.getOutLinks();
        assertTrue(CollectionUtils.exists(curi.getOutLinks(), new Predicate() {
            public boolean evaluate(Object object) {
                return ((Link) object).getDestination().toString().indexOf(
                        "/redirect_me?form=true&checked[]=1&unchecked[]=&selectBox=selectedOption&test=Go")>=0;
            }
        }));
    }
    
    public void testMultipleAttributesPerElement() throws URIException {
        UURI uuri = UURIFactory.getInstance("http://www.example.org");
        DefaultProcessorURI curi = new DefaultProcessorURI(uuri, null);
        CharSequence cs = "<a src=\"http://www.example.com/\" href=\"http://www.archive.org/\"> "; 
        JerichoExtractorHTML ex = (JerichoExtractorHTML)makeExtractor();
        ex.extract(curi, cs);
        assertTrue("not all links found", curi.getOutLinks().size() == 2);
    }
}
