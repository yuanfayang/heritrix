/*
 * Main.java
 * Created on May 15, 2003
 *
 * $Header$
 */
package org.archive.crawler;

import java.util.logging.Logger;

import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.framework.CrawlController;

/**
 * Main class for Heritrix crawler.
 * 
 * Currently takes a single command-line argument, which
 * should be an XML crawl-order file describing the crawl to 
 * undertake, and begins that crawl.
 * 
 * (Eventually, will start web UI and await further
 * instructions.)
 * 
 * @author gojomo
 *
 */
public class Heritrix {
	private static Logger logger = Logger.getLogger("org.archive.crawler.Heritrix");

	public static void main( String[] args ) {
		(new Heritrix()).instanceMain( args );
	}

	public void instanceMain( String[] args ) {
		String crawlOrderFile = args[0]; 
		CrawlOrder order = CrawlOrder.readFromFile(crawlOrderFile);
		
		CrawlController controller = new CrawlController();
		controller.initialize(order);
		controller.startCrawl();
		logger.info("exitting main thread");
	}

}