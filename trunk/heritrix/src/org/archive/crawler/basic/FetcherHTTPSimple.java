/*
 * SimpleHTTPFetcher.java
 * Created on Jun 5, 2003
 *
 * $Header$
 */
package org.archive.crawler.basic;

import java.io.IOException;
import java.util.logging.Logger;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.datamodel.InstancePerThread;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.Processor;

/**
 * Basic class for using the Apache Jakarta HTTPClient library
 * for fetching an HTTP URI. 
 * 
 * @author gojomo
 *
 */
public class FetcherHTTPSimple extends Processor implements InstancePerThread, CoreAttributeConstants, FetchStatusCodes {
	private static String XP_TIMEOUT_SECONDS = "//params/@timeout-seconds";
	private static int DEFAULT_TIMEOUT_SECONDS = 10;
	public static int MAX_HTTP_FETCH_ATTEMPTS = 3;
	
	private static Logger logger = Logger.getLogger("org.archive.crawler.basic.FetcherHTTPSimple");
	HttpClient http;
	private int timeout;


	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Processor#process(org.archive.crawler.datamodel.CrawlURI)
	 */
	protected void innerProcess(CrawlURI curi) {

		if(!curi.getUURI().getUri().getScheme().equals("http")) {
			// only handles plain http for now
			return;
		}
		
		// make sure there are not restrictions on when we should fetch this
		if(curi.dontFetchYet()){
			return;
		}
		
		// only try so many times...
		if(curi.getFetchAttempts() >= MAX_HTTP_FETCH_ATTEMPTS){
			curi.setFetchStatus(S_CONNECT_FAILED);
		}
		
		// give it a go
		curi.incrementFetchAttempts();
		
		// make sure the dns lookup succeeded
		if(curi.getHost().getIP() == null && curi.getHost().hasBeenLookedUp()){
			curi.setFetchStatus(S_DOMAIN_UNRESOLVABLE);
			return;
		}
				
		// attempt to get the page
		long now = System.currentTimeMillis();
		
		curi.getAList().putLong(A_FETCH_BEGAN_TIME,now);
		GetMethod get = new GetMethod(curi.getUURI().getUri().toASCIIString());
		get.setFollowRedirects(false);
		get.setRequestHeader("User-Agent",controller.getOrder().getBehavior().getUserAgent());
		get.setRequestHeader("From",controller.getOrder().getBehavior().getFrom());
		
		//controller.getKicker().kickMeAt(Thread.currentThread(),now+timeout);

		try {
						
			http.executeMethod(get);

			// force read-to-end, so that any socket hangs occur here,
			// not in later modules
			// 
			// (if we weren't planning to get the whole thing
			// anyway -- for example if we weren't an archival
			// spider, just one doing some indexing/analysis --
			// this might be wasteful. As it is, it just moves 
			// the cost here rather than elsewhere. )
			get.getResponseBody(); 	
			
			Header contentLength = get.getResponseHeader("Content-Length");
			logger.info(
				curi.getUURI().getUri()+": "
				+get.getStatusCode()+" "
				+(contentLength==null ? "na" : contentLength.getValue()));

			// TODO consider errors more carefully
			curi.setFetchStatus(get.getStatusCode());
			curi.getAList().putObject(A_HTTP_TRANSACTION,get);
			curi.getAList().putLong(A_FETCH_COMPLETED_TIME,System.currentTimeMillis());
			Header ct = get.getResponseHeader("content-type");
			if ( ct!=null ) {
				curi.getAList().putString(A_CONTENT_TYPE, ct.getValue());
			}

		} catch (HttpException e) {
			logger.warning(e+" on "+curi);
			//TODO make sure we're using the right codes (unclear what HttpExceptions are right now)
			curi.setFetchStatus(S_CONNECT_FAILED);
		} catch (IOException e) {
			logger.warning(e+" on "+curi);
			curi.setFetchStatus(S_CONNECT_FAILED);
		} finally {
			//controller.getKicker().cancelKick(Thread.currentThread());
		}
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Processor#initialize(org.archive.crawler.framework.CrawlController)
	 */
	public void initialize(CrawlController c) {
		super.initialize(c);
		timeout = 1000*getIntAt(XP_TIMEOUT_SECONDS, DEFAULT_TIMEOUT_SECONDS);
		CookiePolicy.setDefaultPolicy(CookiePolicy.COMPATIBILITY);
		http = new HttpClient();
	}

}
