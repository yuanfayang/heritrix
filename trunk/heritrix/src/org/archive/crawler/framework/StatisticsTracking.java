package org.archive.crawler.framework;

/**
 * An interface for objects that want to collect statistics on 
 * running crawls. An implementation of this is referenced in the
 * crawl order and loaded when the crawl begins.
 * <p>
 * It will be given a reference to the relevant CrawlController. 
 * The CrawlController will contain any additional configuration
 * information needed.
 * <p>
 * Any class that implements this interface can be specified as a 
 * statistics tracker in a crawl order.  The CrawlController will
 * then create and initialize a copy of it and call it's start() 
 * method.
 * <p>
 * It is recommended that it register for {@link org.archive.crawler.event.CrawlStatusListener CrawlStatus} events and
 * {@link org.archive.crawler.event.CrawlURIDispositionListener CrawlURIDisposition} events to be able to properly monitor a
 * crawl. Both are registered with the CrawlController.
 * 
 * @author Kristinn Sigurdsson
 * 
 * @see AbstractTracker
 * @see org.archive.crawler.event.CrawlStatusListener
 * @see org.archive.crawler.event.CrawlURIDispositionListener
 * @see org.archive.crawler.framework.CrawlController
 */
public interface StatisticsTracking extends Runnable
{
	/**
	 * Do initialization. 
	 * The CrawlController will call this method before calling the start() method.
	 * 
	 * @param c The {@link CrawlController CrawlController} running the crawl that this class is to gather statistics on.
	 */
	public void initalize(CrawlController c);
	
}