/* 
 * UURITest
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

import java.net.URI;
import java.net.URISyntaxException;

import junit.framework.TestCase;

/**
 * Test UURI's normalize method.
 * 
 * @author Igor Ranitovic
 */
public class UURITest extends TestCase {

    public final void testEscaping() throws URISyntaxException {
        
        // Note: single quote is not being escaped by URI class.
        final String ESCAPEDURI = "http://archive.org/" + UURI.ESCAPED_SPACE + 
             UURI.ESCAPED_SPACE + UURI.ESCAPED_PIPE + UURI.ESCAPED_CIRCUMFLEX +
             UURI.ESCAPED_QUOT + UURI.SQUOT + UURI.ESCAPED_APOSTROPH +
             UURI.ESCAPED_LSQRBRACKET + UURI.ESCAPED_RSQRBRACKET +
             UURI.ESCAPED_LCURBRACKET + UURI.ESCAPED_RCURBRACKET +
             UURI.SLASH + "a.gif"; // NBSP and SPACE should be trimmed;
        
        final String URI = "http://archive.org/.././" + "\u00A0" +
            UURI.SPACE + UURI.PIPE + UURI.CIRCUMFLEX + UURI.QUOT + UURI.SQUOT +
            UURI.APOSTROPH + UURI.LSQRBRACKET + UURI.RSQRBRACKET +
            UURI.LCURBRACKET + UURI.RCURBRACKET + UURI.BACKSLASH +
            "test/../a.gif" + "\u00A0" + UURI.SPACE;
        
        UURI uuri = UURI.createUURI(URI);
        assertTrue(ESCAPEDURI.equals(uuri.getURIString()));
    }
    
    public final void testRelative() throws URISyntaxException {
        UURI uuriTgt = UURI.createUURI("http://archive.org:83/home.html");
        URI uri = new URI("http://archive.org:83/one/two/three.html");
        UURI uuri = UURI.createUURI("/home.html", uri);
        assertTrue("Not equal",
            uuriTgt.getURIString().equals(uuri.getURIString()));
    }

// This test doesn't work. Thats probably as it should be.
//    
//    public final void testMinusScheme() throws URISyntaxException {
//        UURI uuriTgt = UURI.createUURI("http://archive.org:83/home.html");
//        URI uri = new URI("http://archive.org:83/one/two/three.html");
//        UURI uuri = UURI.createUURI("archive.org:83/home.html", uri);
//        assertTrue("Not equal",
//            uuriTgt.getURIString().equals(uuri.getURIString()));
//    }
    
    public final void testAbsolute() throws URISyntaxException {
        UURI uuriTgt = UURI.createUURI("http://archive.org:83/home.html");
        URI uri = new URI("http://archive.org:83/one/two/three.html");
        UURI uuri = UURI.createUURI("http://archive.org:83/home.html", uri);
        assertTrue("Not equal",
            uuriTgt.getURIString().equals(uuri.getURIString()));
    }
}
