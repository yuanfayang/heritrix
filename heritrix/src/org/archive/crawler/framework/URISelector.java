/*
 * URISelector.java
 * Created on May 15, 2003
 *
 * $Header$
 */
package org.archive.crawler.framework;

import org.archive.crawler.datamodel.CrawlURI;

/**
 * @author gojomo
 *
 */
public interface URISelector {

	/**
	 * @param currentCuri
	 */
	void inter(CrawlURI currentCuri);

}
