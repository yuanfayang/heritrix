/* 
 * ToeThread.java
 * Created on May 14, 2003
 *
 * $Header$
 */
package org.archive.crawler.core;

import org.archive.crawler.datamodel.CrawlURI;

/**
 * 
 * @author Gordon Mohr
 */
public class ToeThread extends Thread {
	CrawlController controller;
	boolean shouldRun = true;
	boolean shouldExit = false;
	Object gate = new Object();
	CrawlURI currentCuri;
	// in-process/on-hold curis?
	// a queue of curis to do next?
	
	
	

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		controller.scheduler.curiWantedFor(this);
		while ( currentCuri == null ) {
			gate.wait();
		}
		// TODO 
		
		
	}

}
