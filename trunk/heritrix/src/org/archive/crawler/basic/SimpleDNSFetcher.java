/*
 * SimpleDNSFetcher.java
 * Created on Jun 5, 2003
 *
 * $Header$
 */
package org.archive.crawler.basic;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.Processor;

/**
 * @author gojomo
 *
 */
public class SimpleDNSFetcher extends Processor {

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Processor#process(org.archive.crawler.datamodel.CrawlURI)
	 */
	public void process(CrawlURI curi) {
		super.process(curi);
		if(!curi.getUURI().getUri().getScheme().equals("dns")) {
			// only handles dns
			return;
		}

		// TODO: a DNS lookup, storing result (or error indication)
		// back into the curi
	}
}
