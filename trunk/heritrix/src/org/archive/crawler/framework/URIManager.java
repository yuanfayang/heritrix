/*
 * URIManager.java
 * Created on Apr 17, 2003
 *
 * $Header$
 */
package org.archive.crawler.framework;

import org.archive.crawler.datamodel.*;

/**
 * @author gojomo
 *
 */
public interface URIManager {
	public static Object ENCOUNTERED = new Object();
	
	/**
	 * @return
	 */
	public URIFrontier getFrontier();
	/**
	 * @param nuri
	 * @return
	 */
	public CrawlURI getCrawlURI(UURI uuri);
	
	/**
	 * Inserts a URI into this URIManager.
	 * 
	 * The URIPolicy in effect will be applied to determine
	 * what happens to the URI.
	 * 
	 * @param uri
	 */
	public void insert(UURI uuri);
	
	/**
	 * Returns a CrawlURI after processing (or a processing
	 * attempt) to the URIManager.
	 * 
	 * The URIPolicy in effect will be applied to determine
	 * what happens (reattempts, fanout of discovered URIs, etc.)
	 * 
	 * @param curi
	 */
	public void inter(CrawlURI curi);
	
	/**
	 * @param uri
	 */
	public void hasSeen(String uri);
}
