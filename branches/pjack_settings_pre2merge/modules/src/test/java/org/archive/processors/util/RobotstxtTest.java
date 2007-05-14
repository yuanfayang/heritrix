/* RobotstxtTest
 *
 * $Id$
 *
 * Created Sep 1, 2005
 *
 * Copyright (C) 2005 Internet Archive.
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
package org.archive.processors.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;

public class RobotstxtTest extends TestCase {
    public void testParseRobots() throws IOException {
        LinkedList<String> userAgents = new LinkedList<String>();
        HashMap<String,List<String>> disallows
         = new HashMap<String,List<String>>();
        BufferedReader reader = new BufferedReader(new StringReader("BLAH"));
        assertFalse(Robotstxt.parse(reader, userAgents, disallows));
        assertTrue(disallows.size() == 0);
        // Parse archive robots.txt with heritrix agent.
        String agent = "archive.org_bot";
        reader = new BufferedReader(
            new StringReader("User-agent: " + agent + "\n" +
            "Disallow: /cgi-bin/\n" +
            "Disallow: /details/software\n"));
        assertFalse(Robotstxt.parse(reader, userAgents, disallows));
        assertTrue(disallows.size() == 1);
        assertTrue(userAgents.size() == 1);
        assertEquals(userAgents.get(0), agent);
        // Parse archive robots.txt with star agent.
        agent = "*";
        reader = new BufferedReader(
            new StringReader("User-agent: " + agent + "\n" +
            "Disallow: /cgi-bin/\n" +
            "Disallow: /details/software\n"));
        disallows = new HashMap<String,List<String>>();
        userAgents = new LinkedList<String>();
        assertFalse(Robotstxt.parse(reader, userAgents, disallows));
        assertTrue(disallows.size() == 1);
        assertTrue(userAgents.size() == 1);
        assertEquals(userAgents.get(0), "");
    }
}
