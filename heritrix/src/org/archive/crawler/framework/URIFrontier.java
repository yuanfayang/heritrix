/*
 * URIFrontier.java
 * Created on Oct 1, 2003
 *
 * $Header$
 */
package org.archive.crawler.framework;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FatalConfigurationException;

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
	long size();
}
