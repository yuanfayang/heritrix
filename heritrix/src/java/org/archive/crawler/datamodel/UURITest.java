/* UURITest
 *
 * $Id$
 *
 * Created on Apr 2, 2004
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

package org.archive.crawler.datamodel;

import java.util.Iterator;
import java.util.TreeMap;

import junit.framework.TestCase;

import org.apache.commons.httpclient.URIException;

/**
 * Test UURI's normalize method.
 *
 * @author Igor Ranitovic
 */
public class UURITest extends TestCase {

    public final void testEscaping() throws URIException {

        // Note: single quote is not being escaped by URI class.
        final String ESCAPED_URISTR = "http://archive.org/" + UURI.ESCAPED_SPACE +
            UURI.ESCAPED_SPACE + UURI.ESCAPED_PIPE + UURI.ESCAPED_CIRCUMFLEX +
            UURI.ESCAPED_QUOT + UURI.SQUOT + UURI.ESCAPED_APOSTROPH +
            UURI.ESCAPED_LSQRBRACKET + UURI.ESCAPED_RSQRBRACKET +
            UURI.ESCAPED_LCURBRACKET + UURI.ESCAPED_RCURBRACKET +
            UURI.SLASH + "a.gif"; // NBSP and SPACE should be trimmed;

        final String URISTR = "http://archive.org/.././" + "\u00A0" +
            UURI.SPACE + UURI.PIPE + UURI.CIRCUMFLEX + UURI.QUOT + UURI.SQUOT +
            UURI.APOSTROPH + UURI.LSQRBRACKET + UURI.RSQRBRACKET +
            UURI.LCURBRACKET + UURI.RCURBRACKET + UURI.BACKSLASH +
            "test/../a.gif" + "\u00A0" + UURI.SPACE;

        UURI uuri = new UURI(URISTR);
        String uuriStr = uuri.toString();
        assertTrue(ESCAPED_URISTR.equals(uuriStr));
    }

    public final void testRelative() throws URIException {
        UURI uuriTgt = new UURI("http://archive.org:83/home.html");
        UURI uri = new UURI("http://archive.org:83/one/two/three.html");
        UURI uuri = new UURI(uri, "/home.html");
        assertTrue("Not equal",
            uuriTgt.toString().equals(uuri.toString()));
    }

    /**
     * Test that an empty uuri does the right thing -- that we get back the
     * base.
     *
     * @throws URIException
     */
    public final void testRelativeEmpty() throws URIException {
        UURI uuriTgt = new UURI("http://archive.org:83/one/two/three.html");
        UURI uri = new UURI("http://archive.org:83/one/two/three.html");
        UURI uuri = new UURI(uri, "");
        assertTrue("Empty length don't work",
            uuriTgt.toString().equals(uuri.toString()));
    }

    public final void testAbsolute() throws URIException {
        UURI uuriTgt = new UURI("http://archive.org:83/home.html");
        UURI uri = new UURI("http://archive.org:83/one/two/three.html");
        UURI uuri = new UURI(uri, "http://archive.org:83/home.html");
        assertTrue("Not equal",
            uuriTgt.toString().equals(uuri.toString()));
    }
    
    /**
     * [ 962892 ] UURI accepting/creating unUsable URIs (bad hosts).
     * https://sourceforge.net/tracker/?func=detail&atid=539099&aid=962892&group_id=73833
     */
    public final void testHostWithLessThan() {
        checkExceptionOnIllegalDomainlabel("http://www.betamobile.com</A");
        checkExceptionOnIllegalDomainlabel(
            "http://C|/unzipped/426/spacer.gif");
        checkExceptionOnIllegalDomainlabel("http://www.lycos.co.uk\"/l/b/\"");
    }    
    
    private void checkExceptionOnIllegalDomainlabel(String uuri) {
        boolean expectedException = false;
        try {
            new UURI(uuri);
        } catch (URIException e) {
            // Expected exception.
            expectedException = true;
        }
        assertTrue("Didn't get expected exception: " + uuri, 
            expectedException); 
    }

    /**
     * [ 788277 ] Doing separate DNS lookup for same host
     *
     * https://sourceforge.net/tracker/?func=detail&aid=788277&group_id=73833&atid=539099
     * @throws URIException
     */
    public final void testHostWithPeriod() throws URIException {
        UURI uuri1 = new UURI("http://www.loc.gov./index.html");
        UURI uuri2 = new UURI("http://www.loc.gov/index.html");
        assertEquals("Failed equating hosts with dot",
            uuri1.getHost(), uuri2.getHost());
    }

    /**
     * [ 874220 ] NPE in java.net.URI.encode
     *
     * https://sourceforge.net/tracker/?func=detail&aid=874220&group_id=73833&atid=539099
     * @throws URIException
     */
    public final void testHostEncodedChars() throws URIException {
        String s = "http://g.msn.co.kr/0nwkokr0/00/19??" +
            "PS=10274&NC=10009&CE=42&CP=949&HL=" +
            "&#65533;&#65533;&#65533;?&#65533;&#65533;";
        assertNotNull("Encoded chars " + s, new UURI(s));
    }

    /**
     * [ 927940 ] java.net.URI parses %20 but getHost null
     *
     * https://sourceforge.net/tracker/?func=detail&aid=927940&group_id=73833&atid=539099
     *
     * @throws URIException If fail to get host.
     */
    public final void testSpaceInHost() {
        boolean expectedException = false;
        try {
            new UURI(
                "http://www.local-regions.odpm%20.gov.uk" +
                	"/lpsa/challenge/pdf/propect.pdf");
        } catch (URIException e) {
            expectedException = true;
        }
        assertTrue("Did not fail with escaped space.", expectedException);
        
        expectedException = false;
        try {
            new UURI(
                "http://www.local-regions.odpm .gov.uk" +
                	"/lpsa/challenge/pdf/propect.pdf");
        } catch (URIException e) {
            expectedException = true;
        }
        assertTrue("Did not fail with real space.", expectedException);
    }

    /**
     * [ 808270 ] java.net.URI chokes on hosts_with_underscores.
     *
     * https://sourceforge.net/tracker/?func=detail&aid=808270&group_id=73833&atid=539099
     * @throws URIException
     */
    public final void testHostWithUnderscores() throws URIException {
        UURI uuri = new UURI(
            "http://x_underscore_underscore.2u.com.tw/nonexistent_page.html");
        assertEquals("Failed get of host with underscore",
            "x_underscore_underscore.2u.com.tw", uuri.getHost());
    }
    

    /**
     * Two dots for igor.
     * 
     * @throws URIException
     */
    public final void testTwoDots() throws URIException {
        boolean expectedException = false;
        try {
            new UURI(
              "http://x_underscore_underscore..2u.com/nonexistent_page.html");
        } catch (URIException e) {
            expectedException = true;
        }
        assertTrue("Two dots did not throw exception", expectedException);
    }

    /**
     * [ 910120 ] java.net.URI#getHost fails when leading digit.
     *
     * https://sourceforge.net/tracker/?func=detail&aid=910120&group_id=73833&atid=539099
     * @throws URIException
     */
    public final void testHostWithDigit() throws URIException {
        UURI uuri = new UURI("http://0204chat.2u.com.tw/nonexistent_page.html");
        assertEquals("Failed get of host with digit",
            "0204chat.2u.com.tw", uuri.getHost());
    }

    /**
     * [ 949548 ] Constraining java URI class.
     *
     * https://sourceforge.net/tracker/?func=detail&aid=949548&group_id=73833&atid=539099
     */
    public final void testPort() {
        checkBadPort("http://www.tyopaikat.com:a/robots.txt");
        checkBadPort("http://158.144.21.3:80808/robots.txt");
        checkBadPort("http://pdb.rutgers.edu:81.rutgers.edu/robots.txt");
        checkBadPort(
            "https://webmail.gse.harvard.edu:9100robots.txt/robots.txt");
    }

    /**
     * Test bad port throws exception.
     * @param uri URI with bad port to check.
     */
    private void checkBadPort(String uri) {
        boolean exception = false;
        try {
            new UURI(uri);
        }
        catch (URIException e) {
            exception = true;
        }
        assertTrue("Didn't throw exception: " + uri, exception);
    }

    /**
     * Preserve userinfo capitalization.
     * @throws URIException
     */
    public final void testUserinfo() throws URIException {
        UURI uuri = new UURI("http://stack:StAcK@www.tyopaikat.com/robots.txt");
        assertEquals("Not equal", uuri.getAuthority(),
            "stack:StAcK@www.tyopaikat.com");
    }

    /**
     * Tests from rfc2396 with amendments to accomodate differences
     * intentionally added to make our URI handling like IEs.
     *
     * <pre>
     *       g:h           =  g:h
     *       g             =  http://a/b/c/g
     *       ./g           =  http://a/b/c/g
     *       g/            =  http://a/b/c/g/
     *       /g            =  http://a/g
     *       //g           =  http://g
     *       ?y            =  http://a/b/c/?y
     *       g?y           =  http://a/b/c/g?y
     *       #s            =  (current document)#s
     *       g#s           =  http://a/b/c/g#s
     *       g?y#s         =  http://a/b/c/g?y#s
     *       ;x            =  http://a/b/c/;x
     *       g;x           =  http://a/b/c/g;x
     *       g;x?y#s       =  http://a/b/c/g;x?y#s
     *       .             =  http://a/b/c/
     *       ./            =  http://a/b/c/
     *       ..            =  http://a/b/
     *       ../           =  http://a/b/
     *       ../g          =  http://a/b/g
     *       ../..         =  http://a/
     *       ../../        =  http://a/
     *       ../../g       =  http://a/g
     * </pre>
     *
     * @throws URIException
     */
    public final void testRFC2396Relative() throws URIException {
        UURI base = new UURI("http://a/b/c/d;p?q");
        TreeMap m = new TreeMap();
        m.put("..", "http://a/b/");
        m.put("../", "http://a/b/");
        m.put("../g", "http://a/b/g");
        m.put("../..", "http://a/");
        m.put("../../", "http://a/");
        m.put("../../g", "http://a/g");
        m.put("g#s", "http://a/b/c/g#s");
        m.put("g?y#s ", "http://a/b/c/g?y#s");
        m.put(";x", "http://a/b/c/;x");
        m.put("g;x", "http://a/b/c/g;x");
        m.put("g;x?y#s", "http://a/b/c/g;x?y#s");
        m.put(".", "http://a/b/c/");
        m.put("./", "http://a/b/c/");
        m.put("g", "http://a/b/c/g");
        m.put("./g", "http://a/b/c/g");
        m.put("g/", "http://a/b/c/g/");
        m.put("/g", "http://a/g");
        m.put("//g", "http://g");
        m.put("?y", "http://a/b/c/?y");
        m.put("g?y", "http://a/b/c/g?y");
        // EXTRAS beyond the RFC set.
        // TODO: That these resolve to a path of /a/g might be wrong.  Perhaps
        // it should be '/g'?.
        m.put("/../../../../../../../../g", "http://a/g");
        m.put("../../../../../../../../g", "http://a/g");
        m.put("../G", "http://a/b/G");
        for (Iterator i = m.keySet().iterator(); i.hasNext();) {
            String key = (String)i.next();
            String value = (String)m.get(key);
            UURI uuri = new UURI(base, key);
            assertTrue("Unexpected " + key + " " + value + " " + uuri,
                uuri.equals(new UURI(value)));
        }
    }
}
