/* Copyright (C) 2003 Internet Archive.
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
