package org.archive.crawler.framework;

/**
 * @author Kristinn Sigurdsson
 *
 * Classes that implement this interface can register themselves with
 * a CrawlController to receive notifications about the controllers
 * change of status.  
 */

public interface CrawlListener
{
	/**
	 * Called when a CrawlController is ending a crawl (for any reason)
	 * 
	 * @param sExitMessage - Type of exit (human readable)
	 */
	public void crawlEnding(String sExitMessage);	

	/**
	 * Called when a CrawlController is going to be paused. It is called again when
	 * alle threads are idle and the CrawlController actually has paused. It will also
	 * get called when the CrawlController is resuming from a pause.
	 * 
	 * @param statusMessage is @link{CrawlJob#STATUS_WAITING_FOR_PAUSE}
	 * when CrawlController is asked to pause, and is @link{CrawlJob#STATUS_PAUSED}
	 * when the CrawlController is paused. When the CrawlController is resuming from a
	 * pause the statusMessage will be @link{CrawlJob#STATUS_RESUMED}
	 */
	public void crawlPausing(String statusMessage);
}
