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
 * A step, late in the processing of a CrawlURI, for
 * updating the per-host information that may have
 * been affected by the fetch. This will initially
 * be robots and ip address info; it could include 
 * other per-host stats that would affect the crawl
 * (like total pages visited at the site) as well.
 * 
 * @author gojomo
 *
 */
public class HostInfoUpdater extends Processor {

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Processor#process(org.archive.crawler.datamodel.CrawlURI)
	 */
	protected void innerProcess(CrawlURI curi) {
		
		// make sure we only process schemes we understand (i.e. not dns)
		if(!curi.getUURI().getUri().getScheme().equals("http")){
			return;
		}
		
		if (curi.getUURI().getUri().getPath().equals("/robots.txt")) {
			// update host with robots info
			if(curi.getAList().containsKey("http-transaction")) {
				GetMethod get = (GetMethod)curi.getAList().getObject("http-transaction");
				curi.getHost().updateRobots(get);
			}
		}
	}
}
