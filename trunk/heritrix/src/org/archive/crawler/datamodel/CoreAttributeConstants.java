/*
 * CoreAttributeConstants.java
 * Created on Jun 17, 2003
 *
 * $Header: /cvsroot/archive-crawler/ArchiveOpenCrawler/src//**
 * @author gojomo
 *
 */
package org.archive.crawler.datamodel;

/**
 * @author gojomo
 *
 */
public interface CoreAttributeConstants {
	
	/**
	 * Extracted MIME type of fetched content; should be
	 * set immediately by fetching module if possible 
	 * (rather than waiting for a later analyzer)
	 */
	public static String A_CONTENT_TYPE = "content-type";
	
	/**
	 * Multiplier of last fetch duration to wait before
	 * fetching another item of the same class (eg host)
	 */
	public static String A_DELAY_FACTOR = "delay-factor";
	/**
	 * Minimum delay before fetching another item of th
	 * same class (eg host). Even if lastFetchTime*delayFactor
	 * is less than this, this period will be waited. 
	 */
	public static String A_MINIMUM_DELAY = "minimum-delay";

	public static String A_RRECORD_SET_LABEL = "dns-records";
	public static String A_DNS_FETCH_TIME	= "dns-fetch-time";	
	public static String A_FETCH_BEGAN_TIME= "fetch-began-time";
	public static String A_FETCH_COMPLETED_TIME = "fetch-completed-time";
	public static String A_HTTP_TRANSACTION = "http-transaction";

}
