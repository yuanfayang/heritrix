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
 * AbstractLongFPSet.java
 * Created on Oct 20, 2003
 *
 * $Header$
 */
package org.archive.util;

import java.util.logging.Logger;

/**
 * Shell of functionality for a Set of primitive long fingerprints.
 * 
 * Capacity is always a power of 2. * Fingerprints are already assumed 
 * to be well-distributed, so the hashed position for a value is just
 * its high-order bits. 
 * 
 * @author gojomo
 *
 */
public abstract class AbstractLongFPSet implements LongFPSet {
	private static Logger logger = Logger.getLogger("org.archive.util.AbstractLongFPSet");
	// slot states
	protected static byte EMPTY = -1;
	// zero or positive means slot is filled

	protected int capacityPowerOfTwo;
	protected float loadFactor;
	protected long count;

	/**
	 * @param val
	 * @return
	 */
	public boolean contains(long val) {
		long i = indexFor(val);
		if(i>=0) {
			noteAccess(i);
			return true;
		}
		return false;
	}

	/**
	 * Returns -1 if slot is filled; nonegative if full. 
	 * 
	 * @param i
	 * @return
	 */
	protected abstract int getSlotState(long i);

	/**
	 * Note access (hook for subclass cache-replacement strategies)
	 * @param i
	 */
	private void noteAccess(long i) {
		// by default do nothing
		// cache subclasses may use to update access counts, etc.	
	}

	/**
	 * Number of values in the set.
	 * 
	 * @return
	 */
	public long count() {
		return count;
	}

	/**
	 * Add the given value. 
	 * 
	 * @param val
	 * @return true if set has changed
	 */
	public boolean add(long val) {
		logger.finest("Adding "+val);
		long i = indexFor(val);
		if (i>=0) {
			// positive index indicates already in set
			return false;
		}
		count++;
		if(count>(loadFactor*(1<<capacityPowerOfTwo))) {
			makeSpace();
			// find new i
			i = indexFor(val);
			assert i < 0 : "slot should be empty";
		}
		i = -(i + 1); // convert to positive index
		setAt(i, val);
		noteAccess(i);
		return true;
	}

	/**
	 * Make additional space to keep the load under the target
	 * loadFactor level. Subclasses may grow or discard entries
	 * to satisfy. 
	 * 
	 */
	protected abstract void makeSpace();

	/**
	 * Set the stored value at the given slot. 
	 * 
	 * @param i
	 * @param l
	 */
	protected abstract void setAt(long i, long l);

	/**
	 * Get the stored value at the given slot. 
	 * @param i
	 * @return
	 */
	protected abstract long getAt(long i);

	/**
	 * Get the (positive) index where the value already resides, 
	 * or an empty index where it could be inserted (encoded as a
	 * negative number). 
	 * 
	 * @param l
	 * @return
	 */
	protected long indexFor(long val) {
		long candidateIndex = startIndexFor(val);
		while (true) {
			if (getSlotState(candidateIndex) < 0) {
				// slot empty; return negative number encoding index
				return -candidateIndex - 1;
			}
			if (getAt(candidateIndex) == val) {
				// already present; return positive index
				return candidateIndex;
			}
			candidateIndex++;
			if (candidateIndex==1<<capacityPowerOfTwo) {
				candidateIndex = 0; // wraparound
			}
		}
	}

	/**
	 * Return the recommended storage index for the given value. 
	 * Assumes values are already well-distributed; merely uses
	 * high-order bits. 
	 * 
	 * @param val
	 * @return
	 */
	protected long startIndexFor(long val) {
		return (val >>> (64 - capacityPowerOfTwo));
	}

	/**
	 * Remove the given value.
	 * 
	 * @param l
	 * @return
	 */
	public boolean remove(long l) {
		long i = indexFor(l);
		if (i<0) {
			// not present, not changed
			return false;
		}
		removeAt(i);
		return true;
	}

	/**
	 * Remove the value at the given index, relocating its
	 * successors as necessary. 
	 * 
	 *  @param i
	 */
	protected void removeAt(long index) {
		count--;
		clearAt(index);
		long probeIndex = index+1;
		while(true) {
			if (probeIndex==1<<capacityPowerOfTwo) {
				probeIndex=0; //wraparound
			}
			if(getSlotState(probeIndex)<0) {
				// vacant 
				break;
			}
			long val = getAt(probeIndex);
			long newIndex = indexFor(val);
			if(newIndex!=probeIndex) {
				// value must shift down
				newIndex = -(newIndex+1); // positivize
				relocate(val, probeIndex, newIndex);
			}
			probeIndex++;
		}
	}

	/**
	 * @param index
	 */
	protected abstract void clearAt(long index);

	/**
	 * @param index
	 * @param newIndex
	 */
	abstract void relocate(long value, long fromIndex, long toIndex);

	/** 
	 * Low-cost, non-definitive (except when true) contains
	 * test. Default answer of false is acceptable.
	 * 
	 * @see org.archive.util.LongFPSet#quickContains(long)
	 */
	public boolean quickContains(long fp) {
		return false;
	}

}
