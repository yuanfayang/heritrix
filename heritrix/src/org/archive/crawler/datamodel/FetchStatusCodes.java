/*
 * FetchStatusCodes.java
 * Created on Jun 19, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

/**
 * Constant flag codes to be used, in lieu of per-protocol
 * codes (like HTTP's 200, 404, etc.), when network/internal/
 * out-of-band conditions occur. 
 * 
 * The URISelector may use such codes, along with user-configured
 * options, to determine whether, when, and how many times
 * a CrawlURI might be reattempted.
 * 
 * @author gojomo
 *
 */
public interface FetchStatusCodes {
	public static int S_UNATTEMPTED = 0;
	public static int S_DOMAIN_UNRESOLVABLE = -1;
	public static int S_CONNECT_FAILED = -2;
	public static int S_CONNECT_LOST = -3;
	public static int S_TIMEOUT = -4;
	public static int S_INTERNAL_ERROR = -5;
	public static int S_PREREQUISITE_FAILURE = -6;
	
	public static int S_ROBOTS_PRECLUDED = -9998;
	
	public static int S_DNS_SUCCESS = 1001;

}


