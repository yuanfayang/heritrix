/*
 * DiskFPUURISet.java
 * Created on Oct 20, 2003
 *
 * $Header$
 */
package org.archive.crawler.util;

import java.io.File;
import java.io.IOException;

import org.archive.crawler.datamodel.UURISet;
import org.archive.util.DiskLongFPSet;


/**
 * @author gojomo
 *
 */
public class DiskFPUURISet extends AbstractFPUURISet implements UURISet {

	/**
	 * 
	 */
	public DiskFPUURISet(File dir, String name) throws IOException {
		super();
		fpset = new DiskLongFPSet(dir, name, 8);
	}

}
