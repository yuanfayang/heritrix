/* 
 * CrawlURI.java
 * Created on Apr 16, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

import org.archive.crawler.basic.URIStoreable;
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
public class CrawlURI implements URIStoreable {
	AList alist;
	UURI uuri; 
	Processor nextProcessor;
	CrawlHost host;
	CrawlFetch fetch;


	/**
	 * @param uuri
	 */
	public CrawlURI(UURI uuri) {
	
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * @return
	 */
	public UURI getUURI() {
		return uuri;
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
	/**
	 * @return
	 */
	public Object getClassKey() {
		// TODO Auto-generated method stub
		return null;
	}
	/* (non-Javadoc)
	 * @see org.archive.crawler.basic.URIStoreable#getStoreState()
	 */
	public Object getStoreState() {
		// TODO Auto-generated method stub
		return null;
	}
	/* (non-Javadoc)
	 * @see org.archive.crawler.basic.URIStoreable#setStoreState(java.lang.Object)
	 */
	public void setStoreState(Object s) {
		// TODO Auto-generated method stub
		
	}
	/* (non-Javadoc)
	 * @see org.archive.crawler.basic.URIStoreable#getWakeTime()
	 */
	public long getWakeTime() {
		// TODO Auto-generated method stub
		return 0;
	}
	/* (non-Javadoc)
	 * @see org.archive.crawler.basic.URIStoreable#setWakeTime(long)
	 */
	public void setWakeTime(long w) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * 
	 */
	public AList getAList() {
		// TODO Auto-generated method stub
		return alist;
	}
	
	
}
