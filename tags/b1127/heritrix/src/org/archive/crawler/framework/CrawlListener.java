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
}
