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

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.UURIFactory;

import junit.framework.TestCase;

/**
 * Test the regex rule.
 * @author stack
 * @version $Date$, $Revision$
 */
public class RegexRuleTest extends TestCase {

    public void testCanonicalize() throws URIException {
        final String url = "http://www.aRchive.Org/index.html";
        final String urlMinusWWW = "http://aRchive.Org/index.html";
        
        // Test escaping works.
        assertTrue("Reproduce strip www don't work.", ("$1" + urlMinusWWW).
             equals((new RegexRule("test", "(https?://)(?:www\\.)(.*)",
                 "\\$1$1$2")).canonicalize(url,
                        UURIFactory.getInstance(url))));
        
        assertTrue("Default doesn't work.",
            url.equals((new RegexRule("test")).
                canonicalize(url, UURIFactory.getInstance(url))));
        assertTrue("Basic test doesn't work.",
            ("PREFIX" + url + url + url + "SUFFIX").
                equals((new RegexRule("test", "(.*)", "PREFIX$1$1$1SUFFIX")).
                    canonicalize(url, UURIFactory.getInstance(url))));
        assertTrue("Reproduce strip www don't work.",
                (urlMinusWWW + urlMinusWWW).
                    equals((new RegexRule("test", "(https?://)(?:www\\.)(.*)",
                            "$1$2${1}${2}")).
                        canonicalize(url, UURIFactory.getInstance(url))));
    }

}
