/*
 * CrawlOrder.java
 * Created on May 15, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

import javax.xml.transform.TransformerException;

import org.apache.xpath.XPathAPI;
import org.archive.crawler.framework.*;
import org.w3c.dom.Document;

/**
 * @author gojomo
 *
 */
public class CrawlOrder extends XMLConfig {
	String name;
	CrawlScope scope;
	CrawlerBehavior behavior;
	
	/**
	 * @param crawlOrderFile
	 * @return
	 */
	public static CrawlOrder readFromFile(String crawlOrderFile) {
		Document doc = readDocumentFromFile(crawlOrderFile);
		return new CrawlOrder(doc);
	}

	/**
	 * @param doc
	 */
	public CrawlOrder(Document doc) {
		xNode = doc;
		try {
			name = getNodeAt("/crawl-order/@name").getNodeValue();
			scope = new CrawlScope(XPathAPI.selectSingleNode(doc,"//crawl-scope"));
			behavior = new CrawlerBehavior(XPathAPI.selectSingleNode(doc,"//crawler-behavior"));
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 
	 */
	public CrawlerBehavior getBehavior() {
		return behavior;
		
	}

	/**
	 * 
	 */
	public CrawlScope getScope() {
		return scope;
	}

	/**
	 * @return
	 */
	public String getName() {
		return name;
	}

}
