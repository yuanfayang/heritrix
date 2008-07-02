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
import java.util.NoSuchElementException;
import java.util.SortedSet;

import org.apache.commons.lang.StringUtils;

import com.sleepycat.util.keyrange.KeyRangeException;


/**
 * Utility class for extracting prefixes of a given string from a SortedMap.
 * 
 * @author pjack
 */
public class PrefixFinder {


    /**
     * Extracts prefixes of a given string from a SortedSet.  If an element
     * of the given set is a prefix of the given string, then that element
     * is added to the result list.
     * 
     * <p>Put another way, for every element in the result list, the following 
     * expression will be true: <tt>string.startsWith(element.getKey())</tt>.
     * 
     * @param set     the sorted set containing potential prefixes
     * @param string  the string whose prefixes to find 
     * @param result  the list of prefixes 
     * @return   the number of times the set was consulted; 
     *            used for unit testing
     */
    public static int find(SortedSet<String> set, String input, List<String> result) {
        // OK, the docs for SortedSet.headSet say that the str + '\0' idiom is best
        // for including the target string in the returned set.  But, using \0
        // causes StoredSortedSet to return the wrong thing -- the returned set will
        // not just include the parameter string, but whatever element is next AFTER
        // that string too.  :(  This appears to be because internal sleepycat code
        // uses a \0 to delineate records, maybe?  I dunno, but using ASCII code 1
        // instead of 0 makes the algorithm work as expected.
        set = set.headSet(input + '\u0001');
        int opCount = 0;
        for (String last = last(set); last != null; last = last(set)) {
            opCount++;
            if (input.startsWith(last)) {
                result.add(last);
            } else {
                // Find the longest common prefix.
                int p = StringUtils.indexOfDifference(input, last);
                if (p <= 0) {
                    return opCount;
                }
                // Can't use a \0 to terminate, as that freaks out bdb
                last = input.substring(0, p) + '\u0001';
            }
            try {
                set = set.headSet(last);
            } catch (KeyRangeException e) {
                // StoredSortedSet incorrectly raises this instead of 
                // returning an empty set from headSet.  This simply means
                // there are no more elements to consider.
                return opCount;
            }
            
        }
        return opCount;
    }

    
    private static String last(SortedSet<String> set) {
        try {
            return set.last();
        } catch (NoSuchElementException e) {
            return null;
        }
        
    }

}
