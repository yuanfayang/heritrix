/* 
 * Frontier.java
 * Created on Apr 16, 2003
 *
 * $Header$
 */
package org.archive.crawler.framework;

/** 
 * Represents the collection of URIs ready/wanted 
 * for fetching. 
 * 
 * (If it were technically possible and socially
 * acceptable to do so, all URIs in the Frontier
 * would be fetched ASAP.)
 * 
 * Also tracks the CrawlURIs that are "in process",
 * meaning they have been returned by next() but
 * have not yet been reported as completed().
 * 
 * As a URISet, considers both the pending and
 * in-process URIs as contained. 
 * 
 * @author Gordon Mohr
 */
public interface URIFrontier extends URISet {
	/** 
	 * Returns the next most important CrawlURI to process.
	 * 
	 * The CrawlURI will be prepared to be in the "beginning"
	 * state.
	 * 
	 * Exit Filters, if any, is applied before returning 
	 * a CrawlURI, and if the CrawlURI is not accepted by the
	 * Filter, that CrawlURI is inter()ed back to the 
	 * URIManager with a notation that it failed an exit
	 * Filter. Another CrawlURI is then considered for
	 * return by next().
	 *  
	 * @return the next most important URI to process
	 */
	public CrawlURI next();
	
	/**
	 * Informs the URIFrontier that the given CrawlURI 
	 * has be re-inter()ed to the URIManager.
	 * 
	 * The CrawlURI should be removed from the in-process
	 * URISet, if necessary.
	 * 
	 * @param curi
	 */
	public void completed(CrawlURI curi);
	
	/**
	 * Sets an exit Filter that CrawlURIs must pass to
	 * be returned by next()
	 * 
	 * @param f
	 * @see org.archive.crawler.framework.URIFrontier#next()
	 */
	public void addExitFilter(Filter f);

	/**
	 * Returns a URISet of all URIs waiting to be processed.
	 * 
	 * @return
	 */
	public URISet getWaiting();
	
	/**
	 * Returns a URISet of all URIs in process. 
	 * 
	 * @return
	 */
	public URISet getInProcess();
	
	/**
	 * Returns the URIManager associated with this URIFrontier
	 * 
	 * @return
	 * @see	org.archive.crawler.framework.URIManager
	 */
	public URIManager getManager();
}
