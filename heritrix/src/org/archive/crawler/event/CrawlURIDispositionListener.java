package org.archive.crawler.event;

import org.archive.crawler.datamodel.CrawlURI;

/**
 * An interface for objects that want to be notified
 * of a CrawlURI disposition (happens each time a 
 * curi has been through the processors). 
 * Classes implementing this interface can register with
 * the CrawlController to receive these events.
 * <p>
 * This interface is to facilitate the gathering of  
 * statistics on a running crawl.
 * <p>
 * <b>WARNING:</b> One of these methods <i>will</i> be 
 * called for <b>each</b> CrawlURI that is processed. 
 * It is therefor imperative that the methods execute
 * quickly!
 * <p>
 * Also note that the object implementing this interface
 * must under <b>no circumstances</b> maintain a reference
 * to the CrawlURI beyond the scope of the relevant method 
 * body!
 * 
 * @author Kristinn Sigurdsson
 * 
 * @see org.archive.crawler.framework.CrawlController
 */
public interface CrawlURIDispositionListener
{
	/**
	 * Notification of a successfully crawled URI
	 * 
	 * @param curi The relevant CrawlURI
	 */
	public void crawledURISuccessful(CrawlURI curi);

	/**
	 * Notification of a failed crawl of a URI that 
	 * will be retried (failure due to possible transient
	 * problems).
	 * 
	 * @param curi The relevant CrawlURI
	 */
	public void crawledURINeedRetry(CrawlURI curi);

	/**
	 * Notification of a crawled URI that is to be disregarded.
	 * Usually this means that the robots.txt file for the 
	 * relevant site forbids this from being crawled and we are
	 * therefor not going to keep it.  Other reasons may apply.
	 * In all cases this means that it <i>was</i> successfully
	 * downloaded but will not be stored.
	 * 
	 * @param curi The relevant CrawlURI
	 */
	public void crawledURIDisregard(CrawlURI curi);

	/**
	 * Notification of a failed crawling of a URI. The failure
	 * is of a type that precludes retries (either by it's very
	 * nature or because it has been retried to many times)
	 * 
	 * @param curi The relevant CrawlURI
	 */
	public void crawledURIFailure(CrawlURI curi);
	
}