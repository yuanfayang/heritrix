/*
 * LongFPSetCache.java
 * Created on Oct 21, 2003
 *
 * $Header$
 */
package org.archive.util;

/**
 * Like a MemLongFPSet, but with fixed capacity and maximum size.
 * When an add would expand past the maximum size, an old entry
 * is deleted via a clock/counter algorithm.  
 * 
 * @author gojomo
 *
 */
public class LongFPSetCache extends MemLongFPSet {
	byte[] counter;
	int sweepHand;
	int maxSize;
	
	
	
	/**
	 * 
	 */
	public LongFPSetCache() {
		super();
	}

	/**
	 * @param capacityPowerOfTwo
	 */
	public LongFPSetCache(int capacityPowerOfTwo) {
		super(capacityPowerOfTwo);
		counter = new byte[1<<capacityPowerOfTwo];
	}

	/* (non-Javadoc)
	 * @see org.archive.util.AbstractLongFPSet#indexFor(long)
	 */
	protected int indexFor(long l) {
		int i = super.indexFor(l);
		if (i>)
	}

	/* (non-Javadoc)
	 * @see org.archive.util.LongFPSet#remove(long)
	 */
	public boolean remove(long l) {
		// TODO Auto-generated method stub
		return super.remove(l);
	}

}
