/*
 * URISet.java
 * Created on Apr 17, 2003
 *
 * $Header$
 */
package org.archive.crawler.framework;

import org.archive.crawler.datamodel.*;


/**
 * Represents a collection of URIs.
 * 
 * Implementors may also choose to include various sorts
 * of "fuzzy" contains tests, to determine when another
 * URI which is "close enough" (eg same except with different
 * in-URI session ID) is included.
 * 
 * @author gojomo
 *
 */
public interface URISet /* extends Set ??? */ {
	public boolean contains(UURI u);
	public boolean contains(CrawlURI curi);
	
	public void add(UURI u);
	public void remove(UURI u);
	
	public void add(CrawlURI curi);
	public void remove(CrawlURI curi);

}
