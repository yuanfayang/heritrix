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
import org.archive.io.RecorderLengthExceededException;
import org.archive.io.RecorderTimeoutException;
import org.archive.util.HttpRecorder;

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
	private static String XP_SOTIMEOUT_MS = "@sotimeout-ms";
	private static String XP_MAX_LENGTH_BYTES = "@max-length-bytes";
	private static String XP_MAX_FETCH_ATTEMPTS = "@max-fetch-attempts";
	private static int DEFAULT_TIMEOUT_SECONDS = 10;
	private static int DEFAULT_SOTIMEOUT_MS = 5000;
	private static long DEFAULT_MAX_LENGTH_BYTES = Long.MAX_VALUE;
	private static int DEFAULT_MAX_FETCH_ATTEMPTS = 3;
	
	private static Logger logger = Logger.getLogger("org.archive.crawler.fetcher.FetchHTTP");
	HttpClient http;
	private long timeout;
	private int soTimeout;
	private long maxLength;
	private int maxTries;

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Processor#process(org.archive.crawler.datamodel.CrawlURI)
	 */
	protected void innerProcess(CrawlURI curi) {

		String scheme = curi.getUURI().getScheme();
		if(!(scheme.equals("http")||scheme.equals("https"))) {
			// only handles plain http for now
			return;
		}

// WRONG PLACE TO DO THIS; is a scheduling matter		
//		// make sure there are not restrictions on when we should fetch this
//		if(curi.dontFetchYet()){
//			return;
//		}
		
		// only try so many times...
		if(curi.getFetchAttempts() >= maxTries){
			curi.setFetchStatus(S_TOO_MANY_RETRIES);
			return; 
		}
		
		// give it a go
		//curi.incrementFetchAttempts();
		
		// make sure the dns lookup succeeded
		if (curi.getServer().getHost().getIP() == null
			&& curi.getServer().getHost().hasBeenLookedUp()) {
			curi.setFetchStatus(S_DOMAIN_UNRESOLVABLE);
			return;
		}
				
		// attempt to get the page
		long now = System.currentTimeMillis();
		
		curi.getAList().putLong(A_FETCH_BEGAN_TIME, now);
		GetMethod get = new GetMethod(curi.getUURI().getUriString());
		get.setFollowRedirects(false); // don't auto-follow redirects
		get.getParams().setVersion(HttpVersion.HTTP_1_0);
		get.getParams().makeLenient();
		// use only HTTP/1.0 (to avoid receiving chunked responses)
		String userAgent = curi.getUserAgent();
		if(userAgent == null) {
			userAgent = controller.getOrder().getUserAgent();
		}
		get.setRequestHeader(
			"User-Agent",
			userAgent);
		get.setRequestHeader(
			"From",
			controller.getOrder().getFrom());
		
		HttpRecorder rec = ((ToeThread)Thread.currentThread()).getHttpRecorder();
		get.setHttpRecorder(rec);
		
		long executeRead = 0;
		long readFullyRead = 0;
		
		try {

			// TODO: make this initial reading subject to the same
			// length/timeout limits; currently only the soTimeout
			// is effective here, once the connection succeeds
			http.executeMethod(get);
			executeRead = rec.getRecordedInput().getSize();
		} catch (IOException e) {
			curi.addLocalizedError(
					this.getName(),
					e,
					"executeMethod " +executeRead + ":" + readFullyRead);
			curi.setFetchStatus(S_CONNECT_FAILED);
			rec.closeRecorders();
			get.releaseConnection();
			return;
		} catch (IllegalArgumentException e) {
			// httpclient may throw this for bad cookies
			curi.addLocalizedError(
					this.getName(),
					e,
					"executeMethod " +executeRead + ":" + readFullyRead);
			// try to continue: it may not be fatal
		} catch (ArrayIndexOutOfBoundsException e) {
			// for weird windows-only ArrayIndex exceptions from native code
			// see http://forum.java.sun.com/thread.jsp?forum=11&thread=378356
			// treating as if it were an IOException
			curi.addLocalizedError(
					this.getName(),
					e,
					"executeMethod " +executeRead + ":" + readFullyRead);
			curi.setFetchStatus(S_CONNECT_FAILED);
			rec.closeRecorders();
			get.releaseConnection();
			return;
		}
			
		try {
			// force read-to-end, so that any socket hangs occur here,
			// not in later modules			
			rec.getRecordedInput().readFullyOrUntil(maxLength,timeout);
		} catch (RecorderTimeoutException ex) {
			logger.info(curi.getUURI().getUriString()+": time limit exceeded");
			// but, continue processing whatever was retrieved
			// TODO: set indicator in curi and/or otherwise log
		} catch (RecorderLengthExceededException ex) {
			logger.info(curi.getUURI().getUriString()+": length limit exceeded");
			// but, continue processing whatever was retrieved
			// TODO: set indicator in curi and/or otherwise log
		} catch (IOException e) {
			readFullyRead = rec.getRecordedInput().getSize();
			curi.addLocalizedError(
					this.getName(),
					e,
					"readFully " +executeRead + ":" + readFullyRead);
			curi.setFetchStatus(S_CONNECT_LOST);
			return;
		} catch (ArrayIndexOutOfBoundsException e) {
			// for weird windows-only ArrayIndex exceptions from native code
			// see http://forum.java.sun.com/thread.jsp?forum=11&thread=378356
			// treating as if it were an IOException
			readFullyRead = rec.getRecordedInput().getSize();
			curi.addLocalizedError(
					this.getName(),
					e,
					"readFully " +executeRead + ":" + readFullyRead);
			curi.setFetchStatus(S_CONNECT_LOST);
			return;
		} finally  {
			rec.closeRecorders();
			get.releaseConnection();
		}
			
		Header contentLength = get.getResponseHeader("Content-Length");
		logger.fine(
			curi.getUURI().getUriString()+": "
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
		//rec.closeRecorders();
		//get.releaseConnection();
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Processor#initialize(org.archive.crawler.framework.CrawlController)
	 */
	public void initialize(CrawlController c) {
		super.initialize(c);
		timeout = 1000*getIntAt(XP_TIMEOUT_SECONDS, DEFAULT_TIMEOUT_SECONDS);
		soTimeout = getIntAt(XP_SOTIMEOUT_MS, DEFAULT_SOTIMEOUT_MS);
		maxLength = getLongAt(XP_MAX_LENGTH_BYTES, DEFAULT_MAX_LENGTH_BYTES);
		maxTries = getIntAt(XP_MAX_FETCH_ATTEMPTS, DEFAULT_MAX_FETCH_ATTEMPTS);
		CookiePolicy.setDefaultPolicy(CookiePolicy.COMPATIBILITY);
		
		MultiThreadedHttpConnectionManager connectionManager = 
					new MultiThreadedHttpConnectionManager();
		// ensure there will be as many http connections available as worker threads
		connectionManager.setMaxTotalConnections(controller.getToeCount());
		http = new HttpClient(connectionManager);
		// set connection timeout: considered same as overall timeout, for now
		// TODO: restore this when HTTPClient stops using monitor thread
		//((HttpClientParams)http.getParams()).setConnectionTimeout((int)timeout);
		// set per-read() timeout: overall timeout will be checked at least this
		// frequently
		((HttpClientParams)http.getParams()).setSoTimeout(soTimeout);
	}

}
