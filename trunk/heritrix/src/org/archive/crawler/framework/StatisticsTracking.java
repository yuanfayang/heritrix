package org.archive.crawler.framework;

/**
 * @author Kristinn Sigurdsson
 *
 * An interface for objects that want to collect statistics on 
 * running crawls. An implementation of this is referenced in the
 * crawl order and loaded when the crawl begins.
 * <p>
 * It will be given a reference to the relevant CrawlController. 
 * The CrawlController will contain any additional configuration
 * information needed.
 * <p>
 * It is recommended that it register for CrawlState events and
 * CrawlURIDisposition events to be able to properly monitor a
 * crawl. Both are registered with the CrawlController.
 * 
 * @see AbstractTracker
 * 
 */
public interface StatisticsTracking extends Runnable
{
	/**
	 * Do initialization
	 * 
	 * @param c The CrawlController running the crawl that this class is to gather statistics on.
	 */
	public void initalize(CrawlController c);
	
}