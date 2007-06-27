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
 * PrefixFinder.java
 *
 * Created on Jun 26, 2007
 *
 * $Id:$
 */

package org.archive.settings.file;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedMap;

import org.archive.util.SimpleMapEntry;

/**
 * Utility class for extracting prefixes of a given string from a SortedMap.
 * 
 * @author pjack
 */
class PrefixFinder {


    /**
     * Extracts prefixes of a given string from a SortedMap.  If a key in the
     * sorted map is a prefix of the given string, then the key/value pair
     * is added to the result list.  
     * 
     * <p>Put another way, for every element in the result list, the following 
     * expression will be true: <tt>string.startsWith(element.getKey())</tt>.
     * 
     * @param <T>     the value type of the map
     * @param map     the sorted map containing potential prefixes
     * @param string  the string whose prefixes to find 
     * @param result  the list of prefix/value mappings 
     * @return   the number of times the map was consulted; 
     *            used for unit testing
     */
    public static <T> int find(
            SortedMap<String,T> map, 
            String string, 
            List<Map.Entry<String,T>> result) {
        int opCount = 0;
        while (!map.isEmpty()) {
            T sheet = map.get(string);
            opCount++;
            if (sheet != null) {
                SimpleMapEntry<String,T> sme = 
                    new SimpleMapEntry<String,T>(string, sheet); 
                result.add(sme);
                map = map.headMap(string);
                string = string.substring(0, string.length() - 1);
            } else {
                map = map.headMap(string);
                String last;
                try {
                    last = map.lastKey();
                } catch (NoSuchElementException e) {
                    return opCount;
                }
                string = longestCommonPrefix(string, last);
                if (string.length() == 0) {
                    return opCount;
                }
            }
        }

        return opCount;
    }
    

    /**
     * Returns the longest common prefix of two strings.
     * 
     * @param one   the first string to compare
     * @param two   the second string to compare
     * @return   the longest common prefix of those strings
     */
    private static String longestCommonPrefix(String one, String two) {
        // Make sure one is shorter than two
        if (two.length() < one.length()) {
            String temp = one;
            one = two;
            two = temp;
        }
        
        for (int i = 0; i < one.length(); i++) {
            if (one.charAt(i) != two.charAt(i)) {
                return one.substring(0, i);
            }
        }
        
        return one;
    }
    
}
