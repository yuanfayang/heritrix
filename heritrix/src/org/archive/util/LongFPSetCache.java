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
	long sweepHand = 0;
	
	/**
	 * 
	 */
	public LongFPSetCache() {
		super();
	}

	/**
	 * @param capacityPowerOfTwo
	 */
	public LongFPSetCache(int capacityPowerOfTwo, float loadFactor) {
		super(capacityPowerOfTwo, loadFactor);
	}

	protected void noteAccess(long index) {
		if(slots[(int)index]<Byte.MAX_VALUE) {
			slots[(int)index]++;
		}
	}
	
	protected void makeSpace() {
		discard(1);
	}
	
	/**
	 * @param i
	 */
	private void discard(int i) {
		int toDiscard = i;
		while(true) {
			if(slots[(int)sweepHand]==0) {
				removeAt(sweepHand);
				toDiscard--;
			} else {
				if (slots[(int)sweepHand]>0) {
					slots[(int)sweepHand]--;
				}
			}
			sweepHand++;
			if (sweepHand==count) {
				sweepHand = 0;
			}
			if (toDiscard==0) {
				break;
			}
		}
	}
}
