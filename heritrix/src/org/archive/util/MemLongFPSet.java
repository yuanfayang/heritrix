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
public class MemLongFPSet implements LongFPSet {
	boolean containsZero = false;
	int capacityPowerOfTwo;
	long[] raw;
	long size;
	
	/**
	 * 
	 */
	public MemLongFPSet() {
		this(10);
	}

	/**
	 * @param i
	 */
	public MemLongFPSet(int capacityPowerOfTwo) {
		this.capacityPowerOfTwo = capacityPowerOfTwo;
		raw = new long[2^capacityPowerOfTwo];
		size = 0;
	}

	/* (non-Javadoc)
	 * @see org.archive.util.LongSet#add(long)
	 */
	public boolean add(long l) {
		if (l == 0) {
			if(containsZero) {
				return false;
			} else {
				containsZero = true;
				size++;
				return true;
			}
		}
		int i = indexFor(l);
		if (i>=0) {
			// already in set
			return false;
		}
		size++;
		raw[-i+1]=l;
		return true;
	}

	/**
	 * @param l
	 * @return
	 */
	private int indexFor(long l) {
		if(size>(2^(capacityPowerOfTwo-1))) {
			grow();
		}
		int candidateIndex = (int) (l >> (64 - capacityPowerOfTwo));
		while (true) {
			if (raw[candidateIndex]==0) {
				// not present: return negative insertion index -1 
				return -candidateIndex-1;
			}
			if (raw[candidateIndex]==l) {
				// present: return actual position
				return candidateIndex;
			}
			candidateIndex++;
		}
	}

	/**
	 * 
	 */
	private void grow() {
		long[] oldRaw = raw;
		capacityPowerOfTwo++;
		raw = new long[2^capacityPowerOfTwo];
		size=0;
		for(int i = 0; i< oldRaw.length; i++) {
			if(oldRaw[i]!=0) {
				add(oldRaw[i]);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.archive.util.LongSet#contains(long)
	 */
	public boolean contains(long l) {
		return indexFor(l)>=0;
	}

	/* (non-Javadoc)
	 * @see org.archive.util.LongSet#remove(long)
	 */
	public boolean remove(long l) {
		if(l==0) {
			if (containsZero) {
				containsZero=false;
				size--;
				return true;
			} else {
				return false;
			}
		}
		int i = indexFor(l);
		if (i<0) {
			// not present, not changed
			return false;
		}
		removeAt(i);
		return true;
	}

	/**
	 * @param i
	 */
	private void removeAt(int index) {
		assert raw[index] != 0 : "removeAt bad index";
		raw[index]=0;
		size--;
		int probeIndex = index+1;
		while(raw[probeIndex]!=0) {
			long move = raw[probeIndex];
			raw[probeIndex]=0;
			raw[indexFor(move)]=move;
			probeIndex++;
		}
	}

	/* (non-Javadoc)
	 * @see org.archive.util.LongFPSet#length()
	 */
	public long size() {
		return size;
	}

}
