/*
 * URIDB.java
 * Created on May 14, 2003
 *
 * $Header$
 */
package org.archive.crawler.framework;

/**
 * Handles all persistence for Scheduler and Selector, allowing
 * them to be stateless (and somewhat indifferent to the strategies
 * used for giant URI queues/sets).
 * 
 * @author gojomo
 *
 */
public interface URIStore {

	/**
	 * 
	 */
	void initialize(CrawlController c);
	
	public int urisInFrontier();
	
	public int discoveredUriCount();
}
