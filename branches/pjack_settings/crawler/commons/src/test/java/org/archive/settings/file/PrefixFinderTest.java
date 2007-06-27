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
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

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

    private void doTest() {
        // Generate test data.
        SortedMap<String,String> testData = new TreeMap<String,String>();        
        Random random = new Random();
        String prefix = "0";
        testData.put(prefix, "mapped0");
        for (int i = 1; i < 10000; i++) {
            if (random.nextInt(1024) == 0) {
                prefix += " " + i;
                testData.put(prefix, "mapped" + i);
            } else {
                testData.put(prefix + " " + i, "mapped" + i);
            }
        }

        // Brute-force to get the expected results.
        List<Map.Entry<String,String>> expected 
            = new ArrayList<Map.Entry<String,String>>();
        for (Map.Entry<String,String> me: testData.entrySet()) {
            if (prefix.startsWith(me.getKey())) {
                expected.add(me);
            }
        }
        
        // Results go from longest to shortest.
        Collections.reverse(expected);

        List<Map.Entry<String,String>> result = 
            new ArrayList<Map.Entry<String,String>>();
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
        for (Map.Entry<String,String> me: result) {
            if (!prefix.startsWith(me.getKey())) {
                System.out.println("Result: " + result);                
                assertTrue("Prefix string \"" + prefix 
                        + "\" does not start with result key \"" 
                        + me.getKey() + "\"", false);
            }
        }
    }
    
    
}
