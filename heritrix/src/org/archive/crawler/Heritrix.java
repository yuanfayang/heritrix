/*
 * Main.java
 * Created on May 15, 2003
 *
 * $Header$
 */
package org.archive.crawler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import org.archive.crawler.admin.SimpleHttpServer;
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
	private static Logger logger =
		Logger.getLogger("org.archive.crawler.Heritrix");

	public static void main(String[] args) {
		(new Heritrix()).instanceMain(args);
	}

	public void instanceMain(String[] args) {
		// Default crawlOrder
		String crawlOrderFile = "test-config/order.xml";
		CrawlOrder order;
		CrawlController controller = new CrawlController();

		switch (args.length) {
			case 1 :
				crawlOrderFile = args[0];
			case 0 :
				order = CrawlOrder.readFromFile(crawlOrderFile);
				controller.setOrder(order);
				startServer(controller);
				break;
			case 2 :
				if (args[0].equals("-no-wui")) {
					crawlOrderFile = args[1];
					order = CrawlOrder.readFromFile(crawlOrderFile);
					controller.initialize(order);
					controller.startCrawl();
					break;
				}
			default :
				usage();
				return;
		}
		logger.info("exitting main thread");
	}
	private void usage() {
		System.out.println(
			"USAGE: java Heritrix [-no-wui] order.xml");
		System.out.println(
				"\t-no-wui start crawler without Web User Interface");
	}
	private void startServer(CrawlController c) {
		SimpleHttpServer server = new SimpleHttpServer();
		server.setControler(c);
		try {
			server.startServer();
		} catch (Exception e) {
			e.printStackTrace(System.out);
		}

	}
}