/* 
 * CrawlURI.java
 * Created on Apr 16, 2003
 *
 * $Header$
 */
package org.archive.crawler.framework;

/**
 * Represents a URI and the associated state it collects as
 * it is crawled.
 * 
 * Except for a few special components, state is in a flexible
 * attibute list.
 * 
 * Should only be instantiated via URIManager.getCrawlURI(), 
 * which will assure only one CrawlURI can exist per 
 * NormalizedURIString within a distinct "crawler".
 * 
 * @author Gordon Mohr
 */
public class CrawlURI {
	// AttributeList alist;
	NormalizedURIString nuri; 
	CrawlHost host;
	boolean doProcess;

	/**
	 * @return
	 */
	public NormalizedURIString getNormalizedURIString() {
		return nuri;
	}
	
	
}
