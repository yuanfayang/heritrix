/* Copyright (C) 2003 Internet Archive.
 *
 * This file is part of the Heritrix web crawler (crawler.archive.org).
 *
 * Heritrix is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * any later version.
 *
 * Heritrix is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with Heritrix; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * FetchHTTP.java
 * Created on Jun 5, 2003
 *
 * $Header$
 */
package org.archive.crawler.fetcher;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.logging.Logger;

import org.apache.commons.httpclient.Cookie;
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
 * @author gojomo, igor, others
 *
 */
public class FetchHTTP
	extends Processor
	implements CoreAttributeConstants, FetchStatusCodes {
	private static String XP_TIMEOUT_SECONDS = "@timeout-seconds";
	private static String XP_SOTIMEOUT_MS = "@sotimeout-ms";
	private static String XP_MAX_LENGTH_BYTES = "@max-length-bytes";
	private static String XP_MAX_FETCH_ATTEMPTS = "@max-fetch-attempts";
	private static String XP_LOAD_COOKIES = "//cookies/@src";
	private static int DEFAULT_TIMEOUT_SECONDS = 10;
	private static int DEFAULT_SOTIMEOUT_MS = 5000;
	private static long DEFAULT_MAX_LENGTH_BYTES = Long.MAX_VALUE;
	private static int DEFAULT_MAX_FETCH_ATTEMPTS = 30;

	private static Logger logger =
		Logger.getLogger("org.archive.crawler.fetcher.FetchHTTP");
	HttpClient http;
	private int soTimeout;

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Processor#process(org.archive.crawler.datamodel.CrawlURI)
	 */
	protected void innerProcess(CrawlURI curi) {

		if (!canFetch(curi)) {
            // cannot fetch this, due to protocol, retries, or other problems
            return;
        }
 
		// note begin time
		long now = System.currentTimeMillis();
		curi.getAList().putLong(A_FETCH_BEGAN_TIME, now);

		// setup GET
		GetMethod get = new GetMethod(curi.getUURI().getUriString());
        setupGet(curi, get);
        
		// setup recording of data -- for subsequent processor modules
		HttpRecorder rec =
			((ToeThread) Thread.currentThread()).getHttpRecorder();
		get.setHttpRecorder(rec);

		try {
			// TODO: make this initial reading subject to the same
			// length/timeout limits; currently only the soTimeout
			// is effective here, once the connection succeeds
			http.executeMethod(get);
		} catch (IOException e) {
			curi.addLocalizedError(this.getName(), e, "executeMethod");
			curi.setFetchStatus(S_CONNECT_FAILED);
			rec.closeRecorders();
			get.releaseConnection();
			return;
		} catch (IllegalArgumentException e) {
			// httpclient may throw this for bad cookies
			curi.addLocalizedError(this.getName(), e, "executeMethod");
			curi.setFetchStatus(S_CONNECT_FAILED);
			rec.closeRecorders();
			get.releaseConnection();
			return;
		} catch (ArrayIndexOutOfBoundsException e) {
			// for weird windows-only ArrayIndex exceptions from native code
			// see http://forum.java.sun.com/thread.jsp?forum=11&thread=378356
			// treating as if it were an IOException
			curi.addLocalizedError(this.getName(), e, "executeMethod");
			curi.setFetchStatus(S_CONNECT_FAILED);
			rec.closeRecorders();
			get.releaseConnection();
			return;
		}

		try {
			// force read-to-end, so that any socket hangs occur here,
			// not in later modules			
			rec.getRecordedInput().readFullyOrUntil(
				getLongAt(XP_MAX_LENGTH_BYTES, DEFAULT_MAX_LENGTH_BYTES),
				1000 * getIntAt(XP_TIMEOUT_SECONDS, DEFAULT_TIMEOUT_SECONDS));
		} catch (RecorderTimeoutException ex) {
			curi.addAnnotation("timeTrunc");
		} catch (RecorderLengthExceededException ex) {
			curi.addAnnotation("lengthTrunc");
		} catch (IOException e) {
			curi.addLocalizedError(this.getName(), e, "readFully");
			curi.setFetchStatus(S_CONNECT_LOST);
			return;
		} catch (ArrayIndexOutOfBoundsException e) {
			// for weird windows-only ArrayIndex exceptions from native code
			// see http://forum.java.sun.com/thread.jsp?forum=11&thread=378356
			// treating as if it were an IOException
			curi.addLocalizedError(this.getName(),e,"readFully");
			curi.setFetchStatus(S_CONNECT_LOST);
			return;
		} finally {
			rec.closeRecorders();
			get.releaseConnection();
		}

        // note completion time
        curi.getAList().putLong(
            A_FETCH_COMPLETED_TIME,
            System.currentTimeMillis());

		long contentSize = get.getHttpRecorder().getRecordedInput().getSize();
		logger.fine(
			curi.getUURI().getUriString()+": "+ get.getStatusCode()+" "+ contentSize);

		curi.setFetchStatus(get.getStatusCode());
		curi.setContentSize(contentSize);
		curi.getAList().putObject(A_HTTP_TRANSACTION, get);
		Header ct = get.getResponseHeader("content-type");
		if (ct != null) {
			curi.getAList().putString(A_CONTENT_TYPE, ct.getValue());
		}
	}
    
    
    /**
     * Can this processor fetch the given CrawlURI? May set a fetch
     * status if this processor would usually handle the CrawlURI,
     * but cannot in this instance. 
     * 
     * @param curi
     * @return
     */
    private boolean canFetch(CrawlURI curi) {
        String scheme = curi.getUURI().getScheme();
         if (!(scheme.equals("http") || scheme.equals("https"))) {
             // only handles plain http for now
             return false;
         }

         // only try so many times...
         if (curi.getFetchAttempts()
             >= getIntAt(XP_MAX_FETCH_ATTEMPTS, DEFAULT_MAX_FETCH_ATTEMPTS)) {
             curi.setFetchStatus(S_TOO_MANY_RETRIES);
             return false;
         }

         // make sure the dns lookup succeeded
         if (curi.getServer().getHost().getIP() == null
             && curi.getServer().getHost().hasBeenLookedUp()) {
             curi.setFetchStatus(S_DOMAIN_UNRESOLVABLE);
             return false;
         }
        return true;
    }


    /**
     * Configure the GetMethod as necessary, setting options and headers.
     * 
     * @param curi
     * @param get
     */
    private void setupGet(CrawlURI curi, GetMethod get) {
        // don't auto-follow redirects
        get.setFollowRedirects(false); 
        // use only HTTP/1.0 (to avoid receiving chunked responses)
        get.getParams().setVersion(HttpVersion.HTTP_1_0);
        get.getParams().makeLenient();
        String userAgent = curi.getUserAgent();
        if (userAgent == null) {
        	userAgent = controller.getOrder().getUserAgent();
        }
        get.setRequestHeader("User-Agent", userAgent);
        get.setRequestHeader("From", controller.getOrder().getFrom());
    }

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Processor#initialize(org.archive.crawler.framework.CrawlController)
	 */
	public void initialize(CrawlController c) {
		super.initialize(c);
		//		timeout = 1000*getIntAt(XP_TIMEOUT_SECONDS, DEFAULT_TIMEOUT_SECONDS);
		soTimeout = getIntAt(XP_SOTIMEOUT_MS, DEFAULT_SOTIMEOUT_MS);
		//		maxLength = getLongAt(XP_MAX_LENGTH_BYTES, DEFAULT_MAX_LENGTH_BYTES);
		//		maxTries = getIntAt(XP_MAX_FETCH_ATTEMPTS, DEFAULT_MAX_FETCH_ATTEMPTS);
		CookiePolicy.setDefaultPolicy(CookiePolicy.COMPATIBILITY);
		MultiThreadedHttpConnectionManager connectionManager =
			new MultiThreadedHttpConnectionManager();
		// ensure there will be as many http connections available as worker threads
		connectionManager.setMaxTotalConnections(controller.getToeCount());
		http = new HttpClient(connectionManager);
		
		// load cookies from a file if specified in the order file.
		loadCookies(getStringAt(XP_LOAD_COOKIES));
		
		// set connection timeout: considered same as overall timeout, for now
		// TODO: restore this when HTTPClient stops using monitor thread
		//((HttpClientParams)http.getParams()).setConnectionTimeout((int)timeout);
		// set per-read() timeout: overall timeout will be checked at least this
		// frequently
		 ((HttpClientParams) http.getParams()).setSoTimeout(soTimeout);
	}

	/**
	 * Load cookies from a file before the first fetch.
	 * <p> 
	 * The file is a text file in the Netscape's 'cookies.txt' file format.<br>
	 * Example entry of cookies.txt file:<br>
	 * <br>
	 * www.archive.org FALSE / FALSE 1074567117 details-visit texts-cralond<br>
	 * <br>
	 * Each line has 7 tab-separated fields:<br>
	 * <li>1. DOMAIN: The domain that created and have access to the cookie 
	 * value.
	 * <li>2. FLAG: A TRUE or FALSE value indicating if hosts within the given 
	 * domain can access the cookie value.
     * <li>3. PATH: The path within the domain that the cookie value is valid 
     * for.
     * <li>4. SECURE: A TRUE or FALSE value indicating if to use a secure 
     * connection to access the cookie value.
     * <li>5. EXPIRATION: The expiration time of the cookie value (unix style.)
     * <li>6. NAME: The name of the cookie value
     * <li>7. VALUE: The cookie value
     * 
	 * @param cookiesFile file in the Netscape's 'cookies.txt' format.
	 */
	private void loadCookies(String cookiesFile) {
		// Do nothing if cookiesFile is not specified.
		if (cookiesFile == null) {
			return;
		}
		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(cookiesFile, "r");
			String[] cookieParts;
			String line;
			Cookie cookie = null;
			while ((line = raf.readLine()) != null) {
				// Line that starts with # is commented line, therefore skip it.
				if (!line.startsWith("#")) {
					cookieParts = line.split("\\t");
					if (cookieParts.length == 7) {
						// Create cookie with not expiration date (-1 value).
						// TODO: add this as an option.
						cookie =
							new Cookie(
								cookieParts[0],
								cookieParts[5],
								cookieParts[6],
								cookieParts[2],
								-1,
								(new Boolean(cookieParts[3])).booleanValue());
						if (cookieParts[1].toLowerCase().equals("true")) {
							cookie.setDomainAttributeSpecified(true);
						} else {
							cookie.setDomainAttributeSpecified(false);
						}
						this.http.getState().addCookie(cookie);
						logger.fine(
							"Adding cookie: " + cookie.toExternalForm());
					}
				}
			}
		} catch (FileNotFoundException e) {
			// We should probably throw FatalConfigurationException.
			System.out.println(
				"Could not find file: "
					+ cookiesFile
					+ " (Element: "
					+ XP_LOAD_COOKIES
					+ ")");
		} catch (IOException e) {
			// We should probably throw FatalConfigurationException.
			e.printStackTrace();
		} finally {
			try {
				if (raf != null) {
					raf.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
