/*
 * MemFPUURISet.java
 * Created on Oct 20, 2003
 *
 * $Header$
 */
package org.archive.crawler.util;

import org.archive.util.MemLongFPSet;

/**
 * @author gojomo
 *
 */
public class MemFPUURISet extends AbstractFPUURISet {

	/**
	 * 
	 */
	public MemFPUURISet() {
		super();
		fpset = new MemLongFPSet(16);
	}
}
