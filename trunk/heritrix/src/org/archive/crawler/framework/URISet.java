/*
 * URISet.java
 * Created on Apr 17, 2003
 *
 * $Header$
 */
package org.archive.crawler.framework;


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
	public boolean contains(NormalizedURIString u);
	
	public void add(NormalizedURIString u);
	public void remove(NormalizedURIString u);
	
	public void add(CrawlURI curi);
	public void remove(CrawlURI curi);

}
