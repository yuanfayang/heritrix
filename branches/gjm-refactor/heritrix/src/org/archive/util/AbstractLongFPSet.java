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
	protected long size;

	/**
	 * @param l
	 * @return
	 */
	abstract int indexFor(long l);

	public boolean contains(long l) {
		return indexFor(l)>=0;
	}

	public long size() {
		return size;
	}

}
