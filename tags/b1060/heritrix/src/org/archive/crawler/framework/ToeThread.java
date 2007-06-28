/* 
 * ToeThread.java
 * Created on May 14, 2003
 *
 * $Header$
 */
package org.archive.crawler.framework;

import java.util.HashMap;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.datamodel.InstancePerThread;

/**
 * One "worker thread"; asks for CrawlURIs, processes them, 
 * repeats unless told otherwise. 
 * 
 * @author Gordon Mohr
 */
public class ToeThread extends Thread implements CoreAttributeConstants, FetchStatusCodes {
	private static Logger logger = Logger.getLogger("org.archive.crawler.framework.ToeThread");

	private boolean paused = false;
	private boolean shouldCrawl = true;
	CrawlController controller;
	int serialNumber;
	HashMap localProcessors = new HashMap();
	
	CrawlURI currentCuri;
	// in-process/on-hold curis? not for now
	// a queue of curis to do next? not for now

	/**
	 * @param c
	 */
	public ToeThread(CrawlController c, int sn) {
		controller = c;
		serialNumber = sn;
		setName("ToeThread #"+serialNumber);
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		logger.info(getName()+" started for order '"+controller.getOrder().getName()+"'");
		while ( shouldCrawl ) {
			processingLoop();
		} 
		controller.toeFinished(this);
		logger.info(getName()+" finished for order '"+controller.getOrder().getName()+"'");
	}
	
	private synchronized void processingLoop() {
		assert currentCuri == null;
		
		while ( paused ) {
			try {
				this.wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		try {
			currentCuri = controller.crawlUriFor(this);
			
			if ( currentCuri != null ) {
			
				try {
					while ( currentCuri.nextProcessor() != null ) {
						getProcessor(currentCuri.nextProcessor()).process(currentCuri);
					}
				} catch (RuntimeException e) {
					currentCuri.setFetchStatus(S_INTERNAL_ERROR);
					// store exception temporarily for logging
					currentCuri.getAList().putObject(A_RUNTIME_EXCEPTION,(Object)e);
				}
			
				controller.getSelector().inter(currentCuri);
				currentCuri = null;
			} else {
				// self-pause, because there's nothing left to crawl
				logger.warning(getName()+" pausing: nothing to crawl");
				paused = true;
			}
		} catch (OutOfMemoryError e) {
			e.printStackTrace();
			logger.warning(getName()+" pausing: out of memory error");
			paused = true;
		}
	}

	/**
	 * @param processor
	 */
	private Processor getProcessor(Processor processor) {
		if(!(processor instanceof InstancePerThread)) {
			// just use the shared Processor
			 return processor;
		}
		// must use local copy of processor
		Processor localProcessor = (Processor) localProcessors.get(processor.getClass().getName());
		if (localProcessor == null) {
			localProcessor = processor.spawn();
			localProcessors.put(processor.getClass().getName(),localProcessor);
		}
		return localProcessor;
	}

	/**
	 * @return
	 */
	private boolean shouldCrawl() {
		return shouldCrawl;
	}

	/**
	 * 
	 */
	public synchronized void unpause() {
		if(!paused) return;
		paused = false;
		this.notify();
	}
	
	public void pauseAfterCurrent() {
		paused = true;
	}
	
	public void stopAfterCurrent() {
		shouldCrawl = false;
	}

	/**
	 * 
	 */
	public int getSerialNumber() {
		return serialNumber;
	}
	
	public boolean isPaused(){
		return paused;
	}
}