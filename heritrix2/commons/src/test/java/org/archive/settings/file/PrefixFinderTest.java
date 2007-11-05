/* 
 * Copyright (C) 2007 Internet Archive.
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
 * PrefixFinderTest.java
 *
 * Created on Jun 26, 2007
 *
 * $Id:$
 */

package org.archive.settings.file;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;

import junit.framework.TestCase;

/**
 * Unit test for PrefixFinder.
 * 
 * @author pjack
 */
public class PrefixFinderTest extends TestCase {


    public void testFind() {
        for (int i = 0; i < 100; i++) {
            doTest();
        }
    }

    
    public void testSmallSet() {
        SortedSet<String> testData = new TreeSet<String>();
        testData.add("foo");
        List<String> result = new ArrayList<String>();
        int count = PrefixFinder.find(testData, "baz", result);
        assertTrue(result.isEmpty());
        assertEquals(count, 0);
    }


    
    private void doTest() {
        // Generate test data.
        SortedSet<String> testData = new TreeSet<String>();
        long seed = System.currentTimeMillis();
        System.out.println("Used seed: " + seed);
        Random random = new Random(seed);
        String prefix = "0";
        testData.add(prefix);
        for (int i = 1; i < 10000; i++) {
            if (random.nextInt(1024) == 0) {
                prefix += " " + i;
                testData.add(prefix);
            } else {
                testData.add(prefix + " " + i);
            }
        }

        // Brute-force to get the expected results.
        List<String> expected = new ArrayList<String>();
        for (String value: testData) {
            if (prefix.startsWith(value)) {
                expected.add(value);
            }
        }
        
        // Results go from longest to shortest.
        Collections.reverse(expected);

        final List<String> result = new ArrayList<String>();
        int count = PrefixFinder.find(testData, prefix, result);

        if (!result.equals(expected)) {
            System.out.println("Expected: " + expected);
            System.out.println("Result:   " + result);
        }
        assertEquals(result, expected);
        
        // I pulled "100" out of nowhere.  The point here is to make sure
        // the algorithm isn't iterating over the entire map.
        assertTrue("Operation count too high: " + count + " > 100", count < 100);

        // Double-check.
        for (String value: result) {
            if (!prefix.startsWith(value)) {
                System.out.println("Result: " + result);                
                fail("Prefix string \"" + prefix 
                        + "\" does not start with result key \"" 
                        + value + "\"");
            }
        }
    }

    
}
