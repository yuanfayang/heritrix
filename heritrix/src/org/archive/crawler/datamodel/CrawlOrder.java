/*
 * CrawlOrder.java
 * Created on May 15, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

/**
 * @author gojomo
 *
 */
public class CrawlOrder {
	CrawlScope scope;
	CrawlerBehavior crawler;
	
	/**
	 * @param crawlOrderFile
	 * @return
	 */
	public static CrawlOrder readFromFile(String crawlOrderFile) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * 
	 */
	public CrawlerBehavior getCrawler() {
		return crawler;
		
	}

}
