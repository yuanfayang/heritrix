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
	long[] rawArray;
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
		rawArray = new long[1<<capacityPowerOfTwo];
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
		setAt(-(i+1), l);
		return true;
	}

	protected void setAt(int i, long l) {
		rawArray[i]=l;
	}

	/**
	 * @param l
	 * @return
	 */
	protected int indexFor(long l) {
		if(size>(1<<(capacityPowerOfTwo-1))) {
			grow();
		}
		int candidateIndex = (int) (l >>> (64 - capacityPowerOfTwo));
		while (true) {
			if (rawArray[candidateIndex]==0) {
				// not present: return negative insertion index -1 
				return -candidateIndex-1;
			}
			if (rawArray[candidateIndex]==l) {
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
		long[] oldRaw = rawArray;
		capacityPowerOfTwo++;
		rawArray = new long[1<<capacityPowerOfTwo];
		size=0;
		for(int i = 0; i< oldRaw.length; i++) {
			if(oldRaw[i]!=0) {
				add(oldRaw[i]);
			}
		}
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
	protected void removeAt(int index) {
		size--;
		clearAt(index);
		int probeIndex = index+1;
		if (probeIndex>size) {
			probeIndex=0; //wraparound
		}
		while(rawArray[probeIndex]!=0) {
			reposition(probeIndex);
			probeIndex++;
			if (probeIndex>size) {
				probeIndex=0;  //wraparound
			}
		}
	}

	
	protected void reposition(int index) {
		long newIndex = indexFor(rawArray[index]);
		if(newIndex!=index) {
			relocate(index, newIndex);
		}
	}

	private void relocate(long index, long newIndex) {
		rawArray[(int)newIndex] = rawArray[(int)index];
		rawArray[(int)index] = 0;
	}

	protected void clearAt(int index) {
		assert rawArray[index] != 0 : "removeAt bad index";
		rawArray[index]=0;
	}

}
