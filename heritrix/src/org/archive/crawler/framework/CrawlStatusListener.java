package org.archive.crawler.framework;

/**
 * @author Kristinn Sigurdsson
 *
 * Classes that implement this interface can register themselves with
 * a CrawlController to receive notifications about the events that
 * affect a job's current status.  
 */

public interface CrawlStatusListener
{
	/**
	 * Called when a CrawlController is ending a crawl (for any reason)
	 * 
	 * @param sExitMessage - Type of exit (human readable)
	 */
	public void crawlEnding(String sExitMessage);	

	/**
	 * Called when a CrawlController is going to be paused. 
	 * 
	 * @param statusMessage is @link{CrawlJob#STATUS_WAITING_FOR_PAUSE}
	 */
	public void crawlPausing(String statusMessage);

	/**
	 * Called when a CrawlController is actually paused (all threads are idle).
	 * 
	 * @param statusMessage is @link{CrawlJob#STATUS_PAUSED}
	 */	
	public void crawlPaused(String statusMessage);

	/**
	 * Called when a CrawlController is resuming a crawl that had been paused.
	 * 
	 * @param statusMessage is @link{CrawlJob#STATUS_RESUMED}
	 */	
	public void crawlResuming(String statusMessage);
}
