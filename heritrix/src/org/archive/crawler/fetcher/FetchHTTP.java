/*
 * FetchHTTP.java
 * Created on Jun 5, 2003
 *
 * $Header$
 */
package org.archive.crawler.fetcher;

import java.io.IOException;
import java.util.logging.Logger;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.Processor;
import org.archive.crawler.framework.ToeThread;

/**
 * Basic class for using the Apache Jakarta HTTPClient library
 * for fetching an HTTP URI. 
 * 
 * @author gojomo
 *
 */
public class FetchHTTP
	extends Processor
	implements CoreAttributeConstants, FetchStatusCodes {
	private static String XP_TIMEOUT_SECONDS = "@timeout-seconds";
	private static String XP_MAX_LENGTH_BYTES = "@max-length-bytes";
	private static String XP_MAX_FETCH_ATTEMPTS = "@max-fetch-attempts";
	private static int DEFAULT_TIMEOUT_SECONDS = 10;
	private static long DEFAULT_MAX_LENGTH_BYTES = Long.MAX_VALUE;
	private static int DEFAULT_MAX_FETCH_ATTEMPTS = 3;
	
	private static Logger logger = Logger.getLogger("org.archive.crawler.basic.FetchHTTPSimple");
	HttpClient http;
	private long timeout;
	private long maxLength;
	private int maxTries;

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Processor#process(org.archive.crawler.datamodel.CrawlURI)
	 */
	protected void innerProcess(CrawlURI curi) {

		String scheme = curi.getUURI().getUri().getScheme();
		if(!(scheme.equals("http")||scheme.equals("https"))) {
			// only handles plain http for now
			return;
		}
		
		// make sure there are not restrictions on when we should fetch this
		if(curi.dontFetchYet()){
			return;
		}
		
		// only try so many times...
		if(curi.getFetchAttempts() >= maxTries){
			curi.setFetchStatus(S_CONNECT_FAILED);
		}
		
		// give it a go
		curi.incrementFetchAttempts();
		
		// make sure the dns lookup succeeded
		if (curi.getServer().getHost().getIP() == null
			&& curi.getServer().getHost().hasBeenLookedUp()) {
			curi.setFetchStatus(S_DOMAIN_UNRESOLVABLE);
			return;
		}
				
		// attempt to get the page
		long now = System.currentTimeMillis();
		
		curi.getAList().putLong(A_FETCH_BEGAN_TIME, now);
		GetMethod get = new GetMethod(curi.getUURI().getUri().toASCIIString());
		get.setFollowRedirects(false); // don't auto-follow redirects
		get.getParams().setVersion(HttpVersion.HTTP_1_0);
		// use only HTTP/1.0 (to avoid receiving chunked responses)
		get.setRequestHeader(
			"User-Agent",
			controller.getOrder().getUserAgent());
		get.setRequestHeader(
			"From",
			controller.getOrder().getFrom());
		
		get.setHttpRecorder(((ToeThread)Thread.currentThread()).getHttpRecorder());
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
			//InputStream is = get.getResponseBodyAsStream(); 
			//while(is.read()!=-1) {} // TODOSOON: read in bigger chunks!
			get.getHttpRecorder().getRecordedInput().readFullyOrUntil(maxLength,timeout);
			get.getHttpRecorder().close();
			
			Header contentLength = get.getResponseHeader("Content-Length");
			logger.info(
				curi.getUURI().getUri()+": "
				+get.getStatusCode()+" "
				+(contentLength==null ? "na" : contentLength.getValue()));

			// TODO consider errors more carefully
			curi.setFetchStatus(get.getStatusCode());
			curi.setContentSize(get.getHttpRecorder().getRecordedInput().getSize());
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
			get.releaseConnection();
		}
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Processor#initialize(org.archive.crawler.framework.CrawlController)
	 */
	public void initialize(CrawlController c) {
		super.initialize(c);
		timeout = 1000*getIntAt(XP_TIMEOUT_SECONDS, DEFAULT_TIMEOUT_SECONDS);
		maxLength = getLongAt(XP_MAX_LENGTH_BYTES, DEFAULT_MAX_LENGTH_BYTES);
		maxTries = getIntAt(XP_MAX_FETCH_ATTEMPTS, DEFAULT_MAX_FETCH_ATTEMPTS);
		CookiePolicy.setDefaultPolicy(CookiePolicy.COMPATIBILITY);
		MultiThreadedHttpConnectionManager connectionManager = 
					new MultiThreadedHttpConnectionManager();
		http = new HttpClient(connectionManager);
		// set connection timeout: considered same as overall timeout, for now
		((HttpClientParams)http.getParams()).setConnectionTimeout((int)timeout);
		// set per-read() timeout: overall timeout will be checked at least this
		// frequently
		((HttpClientParams)http.getParams()).setSoTimeout(1000);
	}

}
