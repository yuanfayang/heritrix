/* 
 * CrawlController.java
 * Created on May 14, 2003
 *
 * $Header$
 */
package org.archive.crawler.framework;

import java.util.Iterator;
import java.util.List;

import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.HostCache;

/**
 * 
 * @author Gordon Mohr
 */
public class CrawlController {
	CrawlOrder order;
	
	URIScheduler scheduler;
	URIStore db;
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
		// assume scheduler/URIStore already loaded state
		
		// start toes
		Iterator iter = toes.iterator();
		while(iter.hasNext()) {
			((ToeThread)iter.next()).startCrawling();
		}
		adjustToeCount();
	}
	
	private void adjustToeCount() {
		while(toes.size()<order.getCrawler().getMaxToes()) {
			// TODO make number of threads self-optimizing
			ToeThread newThread = new ToeThread(this);
			toes.add(newThread);
			newThread.start();
		}
	}

}
