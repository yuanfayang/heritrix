/* 
 * SchedulingComparator.java
 * Created on Jul 17, 2003
 *
 * $Header$
 */
package org.archive.crawler.basic;

import java.util.Comparator;

/**
 * 
 * @author Gordon Mohr
 */
public class SchedulingComparator implements Comparator {

	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	public int compare(Object o1, Object o2) {
		if(o1==o2) {
			return 0; // for exact identity only
		}
		if (((URIStoreable)o1).getWakeTime() > ((URIStoreable)o2).getWakeTime()) {
			return -1;
		} 
		if (((URIStoreable)o1).getWakeTime() < ((URIStoreable)o2).getWakeTime()) {
			return 1;
		} 
		// at this point, the ordering is arbitrary, but still
		// must be consistent/stable over time
		
		return ((URIStoreable)o1).getSortFallback().compareTo(((URIStoreable)o2).getSortFallback());	
	}

}
