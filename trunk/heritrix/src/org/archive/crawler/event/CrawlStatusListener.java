package org.archive.crawler.event;

/**
 * Listen for CrawlStatus events.
 * <p>
 * Classes that implement this interface can register themselves with
 * a CrawlController to receive notifications about the events that
 * affect a crawl job's current status.
 *   
 * @author Kristinn Sigurdsson
 * 
 * @see org.archive.crawler.framework.CrawlController#addCrawlStatusListener(CrawlStatusListener)
 */

public interface CrawlStatusListener
{
	/**
	 * Called when a CrawlController is ending a crawl (for any reason)
	 * 
	 * @param sExitMessage Type of exit. Should be one of the STATUS constants in defined in CrawlJob.
	 * 
	 * @see org.archive.crawler.framework.CrawlJob
	 */
	public void crawlEnding(String sExitMessage);	
	
	/**
	 * Called when a CrawlController has ended a crawl and is about to exit.
	 * 
	 * @param sExitMessage Type of exit. Should be one of the STATUS constants in defined in CrawlJob.
	 * 
	 * @see org.archive.crawler.framework.CrawlJob
	 */
	public void crawlEnded(String sExitMessage);

	/**
	 * Called when a CrawlController is going to be paused. 
	 * 
	 * @param statusMessage Should be {@link org.archive.crawler.framework.CrawlJob#STATUS_WAITING_FOR_PAUSE  STATUS_WAITING_FOR_PAUSE}.
	 *                      Passed for convenience 
	 */
	public void crawlPausing(String statusMessage);

	/**
	 * Called when a CrawlController is actually paused (all threads are idle).
	 * 
	 * @param statusMessage Should be {@link org.archive.crawler.framework.CrawlJob#STATUS_PAUSED}.
	 * 						Passed for convenience
	 */	
	public void crawlPaused(String statusMessage);

	/**
	 * Called when a CrawlController is resuming a crawl that had been paused.
	 * 
	 * @param statusMessage Should be {@link org.archive.crawler.framework.CrawlJob#STATUS_RUNNING}.
	 * 						Passed for convenience
	 */	
	public void crawlResuming(String statusMessage);
}
