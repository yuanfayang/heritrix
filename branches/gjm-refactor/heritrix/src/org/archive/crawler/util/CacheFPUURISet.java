/*
 * CacheFPUURISet.java
 * Created on Oct 20, 2003
 *
 * $Header$
 */
package org.archive.crawler.util;

import org.archive.crawler.datamodel.UURI;

/**
 * Only holds at most a fixed number of FPs. Past that
 * point, each add removes an "older" FP, using a clock/counter
 * replacement strategy.
 * @author gojomo
 *
 */
public class CacheFPUURISet extends MemFPUURISet {
	byte[] statuses;
	int clockPosition;
	
	/* (non-Javadoc)
	 * @see org.archive.crawler.datamodel.UURISet#add(org.archive.crawler.datamodel.UURI)
	 */
	public void add(UURI u) {
		super.add(u);
		//if(size)
	}

}
