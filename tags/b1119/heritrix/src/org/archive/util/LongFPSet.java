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
}
