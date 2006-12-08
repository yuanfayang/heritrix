/* CanonicalizerTest
 * 
 * Created on Oct 7, 2004
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
package org.archive.crawler.url;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.url.canonicalize.FixupQueryStr;
import org.archive.crawler.url.canonicalize.LowercaseRule;
import org.archive.crawler.url.canonicalize.StripSessionIDs;
import org.archive.crawler.url.canonicalize.StripUserinfoRule;
import org.archive.crawler.url.canonicalize.StripWWWRule;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;
import org.archive.state.ExampleStateProvider;
import org.archive.util.TmpDirTestCase;

/**
 * Test canonicalization.
 * @author stack
 * @version $Date$, $Revision$
 */
public class CanonicalizerTest extends TmpDirTestCase {

    public static class TestURI extends ExampleStateProvider {
        private UURI uuri;
        
        public TestURI(String uri) {
            try {
                this.uuri = UURIFactory.getInstance(uri);
            } catch (URIException e) {
                throw new RuntimeException(e);
            }
        }
        
        public String toString() {
            return uuri.toString();
        }
    }


    private List<CanonicalizationRule> rules = null;
    
    protected void setUp() throws Exception {
        super.setUp();        
        this.rules = new ArrayList<CanonicalizationRule>();
        this.rules.add(new LowercaseRule());
        this.rules.add(new StripUserinfoRule());
        this.rules.add(new StripWWWRule());
        this.rules.add(new StripSessionIDs());
        this.rules.add(new FixupQueryStr());
    }
    
    public void testCanonicalize() throws URIException {
        ExampleStateProvider context = new ExampleStateProvider();
        final String scheme = "http://";
        final String nonQueryStr = "archive.org/index.html";
        final String result = scheme + nonQueryStr;
        assertTrue("Mangled original", result.equals(
            Canonicalizer.canonicalize(context, result, rules)));
        String tmp = scheme + "www." + nonQueryStr;
        assertTrue("Mangled www", result.equals(
            Canonicalizer.canonicalize(context, tmp, rules)));
        tmp = scheme + "www." + nonQueryStr +
            "?jsessionid=01234567890123456789012345678901";
        assertTrue("Mangled sessionid", result.equals(
            Canonicalizer.canonicalize(context, tmp, rules)));
        tmp = scheme + "www." + nonQueryStr +
            "?jsessionid=01234567890123456789012345678901";
        assertTrue("Mangled sessionid", result.equals(
             Canonicalizer.canonicalize(context, tmp, rules)));
    }
}
