/*
 * URIFrontier.java
 * Created on Oct 1, 2003
 *
 * $Header$
 */
package org.archive.crawler.framework;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.exceptions.FatalConfigurationException;

/**
 * @author gojomo
 *
 */
public interface URIFrontier {
	void initialize(CrawlController c) throws FatalConfigurationException;

	void schedule(CandidateURI caUri);
	CrawlURI next(int timeout);
	void finished(CrawlURI curi);
	
	boolean isEmpty();

	/**
	 * Schedule at top priority (for example, before any
	 * subsequently finished() items that must be retried) 
	 * 
	 * @param caUri
	 */
	void scheduleHigh(CandidateURI caUri);

	/**
	 * @return
	 */
	int successfullyFetchedCount();

	/**
	 * @return
	 */
	int discoveredUriCount();

	/**
	 * @return
	 */
	int failedFetchCount();
}
