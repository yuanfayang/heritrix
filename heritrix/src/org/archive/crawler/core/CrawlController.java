/* 
 * CrawlController.java
 * Created on May 14, 2003
 *
 * $Header$
 */
package org.archive.crawler.core;

import java.util.List;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.CrawlOrder;
import org.archive.crawler.framework.HostCache;
import org.archive.crawler.framework.Processor;
import org.archive.crawler.framework.URIDB;
import org.archive.crawler.framework.URIScheduler;
import org.archive.crawler.framework.URISelector;

/**
 * 
 * @author Gordon Mohr
 */
public class CrawlController {
	CrawlOrder order;
	
	URIScheduler scheduler;
	URIDB db;
	URISelector selector;
	
	Processor entryProcessor;
	List toes /* of ToeThreads */;

	HostCache hosts;
	
	public void initialize(CrawlOrder o) {
		order = o;
		// set up scheduler, db, selector
		// set up processor chain(/graph)
		
	}
	/**
	 * 
	 */
	public URIScheduler getScheduler() {
		return scheduler;
		
	}



	/**
	 * @param thread
	 */
	public void toeFinished(ToeThread thread) {
		// TODO Auto-generated method stub
		
	}


	/**
	 * 
	 */
	public URISelector getSelector() {
		return selector;
	}


	/**
	 * @param thread
	 * @return
	 */
	public CrawlURI crawlUriFor(ToeThread thread) {
		// TODO 
		// IF RUNNING
		// get from scheduler
		// init CrawlURI nextProcessor to first processor
		// IF PAUSED
		// hold until running
		// IF DONE
		// return null
		return null;
	}
	/**
	 * 
	 */
	public void startCrawl() {
		// TODO Auto-generated method stub
		
	}

}
