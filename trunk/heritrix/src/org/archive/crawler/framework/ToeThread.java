/* 
 * ToeThread.java
 * Created on May 14, 2003
 *
 * $Header$
 */
package org.archive.crawler.framework;

import org.archive.crawler.datamodel.CrawlURI;

/**
 * 
 * @author Gordon Mohr
 */
public class ToeThread extends Thread {
	CrawlController controller;
	CrawlURI currentCuri;
	// in-process/on-hold curis? not for now
	// a queue of curis to do next? not for now

	/**
	 * @param c
	 */
	public ToeThread(CrawlController c) {
		controller = c;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		processingLoop();
		controller.toeFinished(this);
	}
	
	private void processingLoop() {
		assert currentCuri == null;
		
		currentCuri = controller.crawlUriFor(this);
		
		while ( currentCuri != null ) {
		
			while ( currentCuri.nextProcessor() != null ) {
				currentCuri.nextProcessor().process(currentCuri);
			}
	
			finishCurrentCuri();
			
			currentCuri = controller.crawlUriFor(this);
			
		}
		
	}

	private void finishCurrentCuri() {
		controller.getSelector().inter(currentCuri);
		currentCuri = null;
	}

	/**
	 * 
	 */
	public void startCrawling() {
		// TODO Auto-generated method stub
		
	}
}
