/*
 * Created on Jul 21, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.archive.crawler.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class Sorts {
	
	// Sorts by value not key
	public static Object[] sortStringIntHashMap (HashMap hm){
		Object[] keys = hm.keySet().toArray();
		Object[] values = hm.values().toArray();
		
		ArrayList unsortedList = new ArrayList();
		
		for (int i = 0; i < keys.length; i++)
			unsortedList.add(
				i,
				new StringIntPair(
					(String) keys[i],
					((Integer) values[i]).intValue()));
		
		Object[] sortedArray = unsortedList.toArray();
		Arrays.sort(sortedArray, new StringIntPairComparator());

		return sortedArray;
	}

}
