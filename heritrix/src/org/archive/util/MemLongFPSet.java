/* 
 * MemLongSet.java
 * Created on Oct 19, 2003
 *
 * $Header$
 */
package org.archive.util;

/**
 * Open-addressing in-memory hash set for holding primitive long fingerprints. 
 * Assumes fingerprints are already well-distributed. Capacity is always a power
 * of 2. Load factor is always kept below 50%. 
 * 
 * @author Gordon Mohr
 */
public class MemLongFPSet extends AbstractLongFPSet implements LongFPSet {
	static final int DEFAULT_CAPACITY_POWER_OF_TWO = 10;
	
	long[] rawArray;
	
	/**
	 * 
	 */
	public MemLongFPSet() {
		this(DEFAULT_CAPACITY_POWER_OF_TWO);
	}

	/**
	 * @param i
	 */
	public MemLongFPSet(int capacityPowerOfTwo) {
		this.capacityPowerOfTwo = capacityPowerOfTwo;
		rawArray = new long[1<<capacityPowerOfTwo];
		count = 0;
	}

	protected void setAt(long i, long val) {
		rawArray[(int)i]=val;
	}
	
	protected long getAt(long i) {
		return rawArray[(int)i];
	}

	/**
	 * 
	 */
	protected void makeSpace() {
		long[] oldRaw = rawArray;
		capacityPowerOfTwo++;
		rawArray = new long[1<<capacityPowerOfTwo];
		count=0;
		for(int i = 0; i< oldRaw.length; i++) {
			if(oldRaw[i]!=0) {
				add(oldRaw[i]);
			}
		}
	}

	protected void relocate(long index, long newIndex) {
		rawArray[(int)newIndex] = rawArray[(int)index];
		rawArray[(int)index] = 0;
	}
}
