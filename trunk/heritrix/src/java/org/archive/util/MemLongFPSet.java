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
 * MemLongSet.java
 * Created on Oct 19, 2003
 *
 * $Header$
 */
package org.archive.util;

/**
 * Open-addressing in-memory hash set for holding primitive long fingerprints. 
 * 
 * @author Gordon Mohr
 */
public class MemLongFPSet extends AbstractLongFPSet implements LongFPSet {
	static final int DEFAULT_CAPACITY_POWER_OF_TWO = 10;
	static final float DEFAULT_LOAD_FACTOR = 0.75f;
	;
	byte[] slots;
	long[] values;
	
	/**
	 * 
	 */
	public MemLongFPSet() {
		this(DEFAULT_CAPACITY_POWER_OF_TWO, 0.75f);
	}

	/**
	 * @param capacityPowerOfTwo
	 * @param loadFactor
	 */
	public MemLongFPSet(int capacityPowerOfTwo, float loadFactor) {
		this.capacityPowerOfTwo = capacityPowerOfTwo;
		this.loadFactor = loadFactor;
		slots = new byte[1<<capacityPowerOfTwo];
		for(int i = 0; i < (1<<capacityPowerOfTwo); i++) {
			slots[i]=EMPTY; // flag value for unused
		}
		values = new long[1<<capacityPowerOfTwo];
		count = 0;
	}

	protected void setAt(long i, long val) {
		slots[(int)i]=1;
		values[(int)i]=val;
	}
	
	protected long getAt(long i) {
		return values[(int)i];
	}

	/**
	 * 
	 */
	protected void makeSpace() {
		grow();
	}

	private void grow() {
		long[] oldValues = values;
		byte[] oldSlots = slots;
		capacityPowerOfTwo++;
		values = new long[1<<capacityPowerOfTwo];
		slots = new byte[1<<capacityPowerOfTwo];
		for(int i = 0; i < (1<<capacityPowerOfTwo); i++) {
			slots[i]=EMPTY; // flag value for unused
		}
		count=0;
		for(int i = 0; i< oldValues.length; i++) {
			if(oldSlots[i]>=0) {
				add(oldValues[i]);
			}
		}
	}

	protected void relocate(long val, long oldIndex, long newIndex) {
		values[(int)newIndex] = values[(int)oldIndex];
		slots[(int)newIndex] = slots[(int)oldIndex];
		slots[(int)oldIndex] = EMPTY;
	}

	/* (non-Javadoc)
	 * @see org.archive.util.AbstractLongFPSet#getSlotState(long)
	 */
	protected int getSlotState(long i) {
		return slots[(int)i];
	}

	/* (non-Javadoc)
	 * @see org.archive.util.AbstractLongFPSet#clearAt(long)
	 */
	protected void clearAt(long index) {
		slots[(int)index]=EMPTY;
		values[(int)index]=0;
	}
}
