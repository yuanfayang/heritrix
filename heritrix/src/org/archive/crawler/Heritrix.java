/*
 * Main.java
 * Created on May 15, 2003
 *
 * $Header$
 */
package org.archive.crawler;

import java.util.logging.Logger;

import org.archive.crawler.admin.SimpleCrawlJob;
import org.archive.crawler.admin.SimpleHandler;
import org.archive.crawler.admin.SimpleHttpServer;
import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.CrawlJob;
import org.archive.crawler.framework.exceptions.InitializationException;

/**
 * Main class for Heritrix crawler.
 * 
 * Initially takes a single command-line argument, which
 * should be an XML crawl-order file describing the crawl to 
 * undertake, and begins that crawl.
 * 
 * Alternatively, can start a web UI and await further 
 * instructions.
 * 
 * @author gojomo
 *
 */
public class Heritrix {
	private static Logger logger =
		Logger.getLogger("org.archive.crawler.Heritrix");
		
	// TODO: Make implementation of CrawlJobHandler configurable
	private static SimpleHandler handler;

	public static void main(String[] args) {
		(new Heritrix()).instanceMain(args);
	}

	public void instanceMain(String[] args) {
		String crawlOrderFile = "test-config/order.xml";

		try {
			switch (args.length) {
				case 1 :
					crawlOrderFile = args[0];
					handler = new SimpleHandler();
				
					CrawlJob cjob = new SimpleCrawlJob(handler.getNextJobUID(),"Auto launched",crawlOrderFile, CrawlJob.PRIORITY_HIGH);
				
					handler.addJob(cjob);
					handler.startCrawler();
				case 0 :
					startServer();
					break;
				case 2 :
					if (args[0].equals("-no-wui")) {
						CrawlController controller = new CrawlController();
						crawlOrderFile = args[1];
						CrawlOrder order = CrawlOrder.readFromFile(crawlOrderFile);
						controller.initialize(order);
						controller.startCrawl();
						break;
					}
				default :
					usage();
					return;
			}
		// catch all configuration exceptions, which at this level are fatal
		}catch(InitializationException e){
			System.out.println("Fatal configuration exception: " + e.toString());
		}
		
		logger.info("exitting main thread");
		
	}
	
	private void usage() {
		System.out.println(
			"USAGE: java Heritrix [-no-wui] order.xml");
		System.out.println(
				"\t-no-wui start crawler without Web User Interface");
	}

	private void startServer() {
		try {
			SimpleHttpServer server = new SimpleHttpServer();
			server.startServer();
		} catch (Exception e) {
			e.printStackTrace(System.out);
		}

	}

	private void startServer(CrawlController c) {
		try {
			SimpleHttpServer server = new SimpleHttpServer();
			server.startServer();
		} catch (Exception e) {
			e.printStackTrace(System.out);
		}
	}
	
	public static SimpleHandler getHandler()
	{
		return handler;	
	}
}