/* 
 * LongSet.java
 * Created on Oct 19, 2003
 *
 * $Header$
 */
package org.archive.util;

/**
 * Set for holding primitive long fingerprints. 
 * 
 * @author Gordon Mohr
 */
public interface LongFPSet {
	boolean add(long l);
	boolean contains(long l);
	boolean remove(long l);
	long count();
	
	/**
	 * Do a contains() check that doesn't require laggy
	 * activity (eg disk IO). If this returns true, 
	 * fp is definitely contained; if this returns 
	 * false, fp  *MAY* still be contained -- must use
	 * full-cost contains() to be sure. 
	 * 
	 * @param fp
	 * @return
	 */
	boolean quickContains(long fp);
}
