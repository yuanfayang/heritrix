/*
 * HostCacheMaintainer.java
 * Created on Jun 5, 2003
 *
 * $Header$
 */
package org.archive.crawler.basic;

import org.apache.commons.httpclient.methods.GetMethod;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.Processor;

/**
 * @author gojomo
 *
 */
public class HostCacheMaintainer extends Processor {

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Processor#process(org.archive.crawler.datamodel.CrawlURI)
	 */
	public void process(CrawlURI curi) {
		super.process(curi);
		if (curi.getUURI().getUri().getPath().equals("/robots.txt")) {
			// update host with robots info
			if(curi.getAList().containsKey("http-transaction")) {
				GetMethod get = (GetMethod)curi.getAList().getObject("http-transaction");
				curi.getHost().updateRobots(get);
					
			}
		}
	}
}
