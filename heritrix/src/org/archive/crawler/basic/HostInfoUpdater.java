/*
 * HostCacheMaintainer.java
 * Created on Jun 5, 2003
 *
 * $Header$
 */
package org.archive.crawler.basic;

import org.apache.commons.httpclient.methods.GetMethod;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.framework.Processor;

/**
 * A step, late in the processing of a CrawlURI, for
 * updating the per-host information that may have
 * been affected by the fetch. This will initially
 * be robots and ip address info; it could include 
 * other per-host stats that would affect the crawl
 * (like total pages visited at the site) as well.
 * 
 * @author gojomo
 *
 */
public class HostInfoUpdater extends Processor implements CoreAttributeConstants, FetchStatusCodes {

	public static int MAX_DNS_FETCH_ATTEMPTS = 3;

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Processor#process(org.archive.crawler.datamodel.CrawlURI)
	 */
	protected void innerProcess(CrawlURI curi) {
	
		// if it's a dns entry set the expire time
		if(curi.getUURI().getUri().getScheme().equals("dns")){

			// if we've looked up the host update the expire time
			if(curi.getHost().hasBeenLookedUp()){
				long expires = curi.getHost().getIpExpires();
				
				if(expires > 0){
					curi.setDontRetryBeforeSmart(expires);
				}
				
			}else{
				// if we've tried too many times give up
				if(curi.getFetchAttempts() >= MAX_DNS_FETCH_ATTEMPTS){
					curi.setFetchStatus(S_DOMAIN_UNRESOLVABLE);
				}
			}
			
			// if a robots.txt uri doesn't exist, create it and set the expire to be
			// the same as the dns expire time
			
			
			return;
		}
			
		// if it's not dns make sure it's http, 'cause we don't know nuthin' else
		if(!curi.getUURI().getUri().getScheme().equals("http")){
			return;
		}
		
		if (curi.getUURI().getUri().getPath().equals("/robots.txt")) {
			// update host with robots info
			if(curi.getAList().containsKey("http-transaction")) {
				GetMethod get = (GetMethod)curi.getAList().getObject("http-transaction");
				curi.getHost().updateRobots(get);
				
				// see which epires first, the dns or the robots.txt
				long expireCuri = ( curi.getHost().getRobotsExpires() < curi.getHost().getIpExpires()) ? curi.getHost().getRobotsExpires() : curi.getHost().getIpExpires();
				curi.setDontRetryBeforeSmart(expireCuri);
			}
		}
	}
}
