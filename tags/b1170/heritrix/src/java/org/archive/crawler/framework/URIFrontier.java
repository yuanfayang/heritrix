/* Copyright (C) 2003 Internet Archive.
 *
 * This file is part of the Heritrix web crawler (crawler.archive.org).
 *
 * Heritrix is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * any later version.
 *
 * Heritrix is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with Heritrix; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
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
