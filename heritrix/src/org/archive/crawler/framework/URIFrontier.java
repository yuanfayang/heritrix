/*
 * URIFrontier.java
 * Created on Oct 1, 2003
 *
 * $Header$
 */
package org.archive.crawler.framework;

import java.io.IOException;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.exceptions.FatalConfigurationException;

/**
 * @author gojomo
 *
 */
public interface URIFrontier {
	void initialize(CrawlController c) throws FatalConfigurationException, IOException;

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
	long successfullyFetchedCount();

	/**
	 * @return
	 */
	long discoveredUriCount();

	/**
	 * Estimated number of URIs scheduled for prcoessing..
	 *  
	 * @return
	 */
	long pendingUriCount();

	/**
	 * @return
	 */
	long failedFetchCount();

	/**
	 * Put caUri into a queue of items to be scheduled 
	 * later (that is, avoid synchronization overhead)
	 * @param caUri
	 */
	void batchSchedule(CandidateURI caUri);

	/**
	 * @param caUri
	 */
	void batchScheduleHigh(CandidateURI caUri);
	
	/**
	 * Force all batch-scheduled candidates to be 
	 * actually scheduled. 
	 * 
	 */
	void batchFlush();
}
