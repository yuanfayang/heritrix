/* 
 * CrawlURI.java
 * Created on Apr 16, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

import org.archive.crawler.framework.Processor;


/**
 * Represents a URI and the associated state it collects as
 * it is crawled.
 * 
 * Except for a few special components, state is in a flexible
 * attibute list.
 * 
 * Should only be instantiated via URIStore.getCrawlURI(...), 
 * which will assure only one CrawlURI can exist per 
 * UURI within a distinct "crawler".
 * 
 * @author Gordon Mohr
 */
public class CrawlURI {
	AttributeList alist;
	UURI uuri; 
	Processor nextProcessor;
	CrawlHost host;
	CrawlFetch fetch;

	/**
	 * @return
	 */
	public UURI getUURI() {
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * 
	 */
	public Processor nextProcessor() {
		return nextProcessor;
	}
	/**
	 * @param processor
	 */
	public void setNextProcessor(Processor processor) {
		nextProcessor = processor;
	}
	
	
}
