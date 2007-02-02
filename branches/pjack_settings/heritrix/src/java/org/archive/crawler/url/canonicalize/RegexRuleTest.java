/* RegexRuleTest
 * 
 * Created on Oct 6, 2004
 *
 * Copyright (C) 2004 Internet Archive.
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
 */
package org.archive.crawler.url.canonicalize;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.management.InvalidAttributeValueException;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.url.CanonicalizationRule;
import org.archive.state.ExampleStateProvider;
import org.archive.state.StateProcessorTestBase;


/**
 * Test the regex rule.
 * @author stack
 * @version $Date$, $Revision$
 */
public class RegexRuleTest extends StateProcessorTestBase {


    private List<CanonicalizationRule> rules;
    private ExampleStateProvider context;


    @Override
    protected Class getModuleClass() {
        return RegexRule.class;
    }

    @Override
    protected Object makeModule() throws Exception {
        return new RegexRule();
    }

    protected void setUp() throws Exception {
        super.setUp();
        this.rules = new ArrayList<CanonicalizationRule>();
        this.context = new ExampleStateProvider();
    }
    
    public void testCanonicalize()
    throws URIException, InvalidAttributeValueException {
        final String url = "http://www.aRchive.Org/index.html";
        RegexRule rr = new RegexRule();
        this.rules.add(rr);
        rr.canonicalize(url, context);
        String product = rr.canonicalize(url, context);
        assertTrue("Default doesn't work.",  url.equals(product));
    }

    public void testSessionid()
    throws InvalidAttributeValueException {
        final String urlBase = "http://joann.com/catalog.jhtml";
        final String urlMinusSessionid = urlBase + "?CATID=96029";
        final String url = urlBase +
		    ";$sessionid$JKOFFNYAAKUTIP4SY5NBHOR50LD3OEPO?CATID=96029";
        RegexRule rr = new RegexRule();
        context.set(rr, RegexRule.REGEX, 
         Pattern.compile("^(.+)(?:;\\$sessionid\\$[A-Z0-9]{32})(\\?.*)+$"));
        context.set(rr, RegexRule.FORMAT, "$1$2");
        this.rules.add(rr);
        String product = rr.canonicalize(url, context);
        assertTrue("Failed " + url, urlMinusSessionid.equals(product));
    }
    
    public void testNullFormat()
    throws InvalidAttributeValueException {
        final String urlBase = "http://joann.com/catalog.jhtml";
        final String url = urlBase +
            ";$sessionid$JKOFFNYAAKUTIP4SY5NBHOR50LD3OEPO";
        RegexRule rr = new RegexRule();
        context.set(rr, RegexRule.REGEX, Pattern.compile(
            "^(.+)(?:;\\$sessionid\\$[A-Z0-9]{32})$"));
        context.set(rr, RegexRule.FORMAT, "$1$2");
        this.rules.add(rr);
        String product = rr.canonicalize(url, context);
        assertTrue("Failed " + url, urlBase.equals(product));
    }
}
