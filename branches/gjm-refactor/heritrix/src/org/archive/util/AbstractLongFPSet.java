/*
 * AbstractLongFPSet.java
 * Created on Oct 20, 2003
 *
 * $Header$
 */
package org.archive.util;


/**
 * @author gojomo
 *
 */
public abstract class AbstractLongFPSet {
	protected boolean containsZero = false;
	protected int capacityPowerOfTwo;
	protected long count;

	public boolean contains(long l) {
		return indexFor(l)>=0;
	}

	public long count() {
		return count;
	}

	public boolean add(long val) {
		if (val == 0) {
			if(containsZero) {
				return false;
			} else {
				containsZero = true;
				count++;
				return true;
			}
		}
		int i = (int) indexFor(val);
		if (i>=0) {
			// already in set
			return false;
		}
		count++;
		if(count>(1<<(capacityPowerOfTwo))) {
			makeSpace();
			// find new i
			i = (int) indexFor(val);
		}
		setAt(-(i+1), val);
		return true;
	}

	/**
	 * 
	 */
	protected abstract void makeSpace();

	/**
	 * @param i
	 * @param l
	 */
	protected abstract void setAt(long i, long l);

	protected abstract long getAt(long i);

	/**
		 * @param l
		 * @return
		 */
	protected long indexFor(long val) {
		int candidateIndex = (int) startIndexFor(val);
		while (true) {
			long at = getAt(candidateIndex);
			if (at==0) {
				// not present: return negative insertion index -1 
				return -candidateIndex-1;
			}
			if (at==val) {
				// present: return actual position
				return candidateIndex;
			}
			candidateIndex++;
		}
	}

	protected long startIndexFor(long l) {
		return (l >>> (64 - capacityPowerOfTwo));
	}

	public boolean remove(long l) {
		if(l==0) {
			if (containsZero) {
				containsZero=false;
				count--;
				return true;
			} else {
				return false;
			}
		}
		long i = indexFor(l);
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
	protected void removeAt(long index) {
		count--;
		setAt(index,0);
		long probeIndex = index+1;
		while(true) {
			if (probeIndex>count) {
				probeIndex=0; //wraparound
			}
			long at = getAt(probeIndex);
			if (at==0) {
				break;
			}
			// at != 0
			long newIndex = indexFor(at);
			if(newIndex!=index) {
				// value must shift down
				relocate(index, newIndex);
			}
			probeIndex++;
		}
	}

	/**
	 * @param index
	 * @param newIndex
	 */
	abstract void relocate(long index, long newIndex);

}
