/*
 * Histotable.java
 * 
 * Created on Aug 5, 2004
 *
 * $Id$
 *
 *
 * Copyright (C) 2003 Internet Archive.
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
 

package org.archive.util;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;


/**
 * Collect and report frequency information. 
 * 
 * Assumes external synchornization.
 * 
 * @author gojomo
 */
public class Histotable {
	HashMap<Object,LongWrapper> totals = new HashMap<Object,LongWrapper>();

    // sorted by count
	TreeSet<Map.Entry<Object,LongWrapper>> sorted = 
      new TreeSet<Map.Entry<Object,LongWrapper>>(
       new Comparator<Map.Entry<Object,LongWrapper>>() {
        public int compare(Map.Entry<Object,LongWrapper> e1, 
                Map.Entry<Object,LongWrapper> e2) {
            long firstVal = e1.getValue().longValue;
            long secondVal = e2.getValue().longValue;
            if (firstVal < secondVal) { return 1; }
            if (secondVal < firstVal) { return -1; }
            // If the values are the same, sort by keys.
            String firstKey = (String) ((Map.Entry) e1).getKey();
            String secondKey = (String) ((Map.Entry) e2).getKey();
            return firstKey.compareTo(secondKey);
        }
    });
	
	/**
	 * Record one more occurence of the given object key.
	 * 
	 * @param key Object key.
	 */
	public void tally(Object key) {
        if (totals.containsKey(key)) {
            totals.get(key).longValue += 1;
        } else {
            // if we didn't find this key add it
            totals.put(key, new LongWrapper(1));
        }
	}
	
	/**
	 * @return Return an up-to-date sorted version of the totalled info.
	 */
	public TreeSet getSorted() {
		if(sorted.size()<totals.size()) {
			sorted.clear();
	        sorted.addAll(totals.entrySet());
		}
		return sorted;
	}
	
	/**
	 * Utility method to convert a key-&gt;LongWrapper(count) into
	 * the string "count key".
	 * 
	 * @param e Map key.
	 * @return String 'count key'.
	 */
	public static String entryString(Object e) {
		Map.Entry entry = (Map.Entry) e;
		return ((LongWrapper)entry.getValue()).longValue + " " + entry.getKey();
	}
}
