/*
 * URISelector.java
 * Created on May 15, 2003
 *
 * $Header$
 */
package org.archive.crawler.framework;

import org.archive.crawler.datamodel.CrawlURI;

/**
 * Decides how incoming CrawlURIs and associated URI strings
 * should be handled.
 * 
 * All knowledge about URI seen/visited/etc histories should 
 * be taken from URIDB.
 * 
 * @author gojomo
 *
 */
public interface URISelector {
	CrawlController controller = null;
	
	/**
	 * @param currentCuri
	 */
	void inter(CrawlURI currentCuri);

	/**
	 * 
	 */
	void initialize(CrawlController c);
	
	int successfullyFetchedCount();
	
	int failedFetchCount();

}
