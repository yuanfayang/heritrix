/*
 * URIScheduler.java
 * Created on May 15, 2003
 *
 * $Header$
 */
package org.archive.crawler.framework;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FatalConfigurationException;

/**
 * @author gojomo
 *
 */
public interface URIScheduler {
	
	/**
	 * @param thread
	 * @return
	 */
	CrawlURI curiFor(ToeThread thread);

	/**
	 * 
	 */
	void initialize(CrawlController c) throws FatalConfigurationException;

}
