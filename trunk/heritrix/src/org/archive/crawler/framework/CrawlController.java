/* 
 * CrawlController.java
 * Created on May 14, 2003
 *
 * $Header$
 */
package org.archive.crawler.framework;

import java.util.HashMap;
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
	URIStore store;
	URISelector selector;
	
	Processor entryProcessor;
	HashMap processors = new HashMap(); 
	List toes /* of ToeThreads */;

	HostCache hosts;
	
	private boolean paused = false;
	private boolean finished = false;

	public void initialize(CrawlOrder o) {
		order = o;
		
		store = (URIStore) order.getBehavior().instantiate("store");
		scheduler = (URIScheduler) order.getBehavior().instantiate("scheduler");
		selector = (URISelector) order.getBehavior().instantiate("selector");
		
		entryProcessor = (Processor) order.getBehavior().instantiateAllInto("processors/processor",processors);
		
		store.initialize(this);
		scheduler.initialize(this);
		selector.initialize(this);
		
		// TODO: initialize processors?
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
		if( paused ) {
			thread.pauseAfterCurrent();
			return null;
		}
		// TODO check global limits, etc to see if finished
		if ( finished  ) {	
			thread.stopAfterCurrent();
			return null;
		}
		CrawlURI curi = scheduler.curiFor(thread);
		// TODO consider possible case where curi is null
		curi.setNextProcessor(entryProcessor);
		return curi;
	}
	/**
	 * 
	 */
	public void startCrawl() {
		// assume scheduler/URIStore already loaded state
		
		// start toes
		Iterator iter = toes.iterator();
		while(iter.hasNext()) {
			((ToeThread)iter.next()).unpause();
		}
		adjustToeCount();
	}
	
	private void adjustToeCount() {
		while(toes.size()<order.getBehavior().getMaxToes()) {
			// TODO make number of threads self-optimizing
			ToeThread newThread = new ToeThread(this);
			toes.add(newThread);
			newThread.start();
		}
	}

	/**
	 * 
	 */
	public CrawlOrder getOrder() {
		return order;
	}

	/**
	 * @return
	 */
	public URIStore getStore() {
		return store;
	}

}
