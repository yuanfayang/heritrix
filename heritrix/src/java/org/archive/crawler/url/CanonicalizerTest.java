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

import junit.framework.TestCase;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.UURIFactory;
import org.archive.crawler.url.canonicalize.FixupQueryStr;
import org.archive.crawler.url.canonicalize.LowercaseRule;
import org.archive.crawler.url.canonicalize.StripSessionIDs;
import org.archive.crawler.url.canonicalize.StripUserinfoRule;
import org.archive.crawler.url.canonicalize.StripWWWRule;

/**
 * Test canonicalization.
 * @author stack
 * @version $Date$, $Revision$
 */
public class CanonicalizerTest extends TestCase {

    private List rules = null;
    
    protected void setUp() throws Exception {
        super.setUp();
        this.rules = new ArrayList();
        this.rules.add(new LowercaseRule("lowercase"));
        this.rules.add(new StripUserinfoRule("userinfo"));
        this.rules.add(new StripWWWRule("www"));
        this.rules.add(new StripSessionIDs("ids"));
        this.rules.add(new FixupQueryStr("querystr"));
    }
    
    public void testCanonicalize() throws URIException {
        final String scheme = "http://";
        final String nonQueryStr = "archive.org/index.html";
        final String result = scheme + nonQueryStr;
        assertTrue("Mangled original", result.equals(
            Canonicalizer.canonicalize(UURIFactory.getInstance(result),
                this.rules.iterator())));
        String tmp = scheme + "www." + nonQueryStr;
        assertTrue("Mangled www", result.equals(
            Canonicalizer.canonicalize(UURIFactory.getInstance(tmp),
                this.rules.iterator())));
        tmp = scheme + "www." + nonQueryStr +
            "?jsessionid=01234567890123456789012345678901";
        assertTrue("Mangled sessionid", result.equals(
            Canonicalizer.canonicalize(UURIFactory.getInstance(tmp),
                this.rules.iterator())));
    }
}
