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
	private boolean paused;
	private boolean shouldCrawl;
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
		while ( shouldCrawl ) {
			processingLoop();
		} 
		controller.toeFinished(this);
	}
	
	private void processingLoop() {
		assert currentCuri == null;
		
		while ( paused ) {
			try {
				this.wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		currentCuri = controller.crawlUriFor(this);
		
		if ( currentCuri != null ) {
		
			while ( currentCuri.nextProcessor() != null ) {
				currentCuri.nextProcessor().process(currentCuri);
			}
	
			controller.getSelector().inter(currentCuri);
			currentCuri = null;
		}
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
	public void unpause() {
		paused = false;
		this.notify();
	}
	
	public void pauseAfterCurrent() {
		paused = true;
	}
	
	public void stopAfterCurrent() {
		shouldCrawl = false;
	}
}
