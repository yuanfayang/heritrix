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
 * See {@link #usage() usage()} for how to launch the program.
 * 
 * @author gojomo
 * @author Kristinn Sigurdsson
 *
 */
public class Heritrix {
	protected static Logger logger =
		Logger.getLogger("org.archive.crawler.Heritrix");
		
	// TODO: Make implementation of CrawlJobHandler configurable
	protected static SimpleHandler handler;

	/**
	 * Launch program
	 * 
	 * @param args See usage() for details.
	 * 
	 * @see #usage()
	 */
	public static void main(String[] args) {
//		(new Heritrix()).instanceMain(args);

		boolean noWUI = false;
		int port = -1;
		String crawlOrderFile = null;
		int crawllaunch = -1; // 0 = no crawl order specified, 1 = start, 2 = load, 3 = set as default.
		
		if(args.length > 3){
			// Too many arguments. Display usage
			usage();
			return;
		}
		else {
			for(int i=0 ; i < args.length ; i++) {
				String arg = args[i];
				if(arg.indexOf("-") == 0) {
					// This is a command, not the crawl order file.
					if(arg.equalsIgnoreCase("-no-wui")){
						if(port != -1){
							// If the port has been set then you can't specify no web UI.
							usage();
							return;
						}
						else{
							noWUI = true;			
						}
					}
					else if(arg.indexOf("-port:") == 0){
						//Defining web UI port.
						if(port != -1){
							// Can't double define port.
							usage();
							return;
						}
						else{
							try{
								String p = arg.substring(6);
								port = Integer.parseInt(p);
							}
							catch(NumberFormatException e){
								usage();
								return;
							}
						}
					}
					else if(arg.equals("-start")){
						if(crawllaunch != -1){
							// Can't define crawllaunch twice.
							usage();
							return;
						}
						else{
							crawllaunch = 1;
						}
					}
					else if(arg.equals("-load")){
						if(crawllaunch != -1){
							// Can't define crawllaunch twice.
							usage();
							return;
						}
						else{
							crawllaunch = 2;
						}
					}
					else if(arg.equals("-set-as-default")){
						if(crawllaunch != -1){
							// Can't define crawllaunch twice.
							usage();
							return;
						}
						else{
							crawllaunch = 3;
						}
					}
					else{
						//Unknown or -?
						usage();
						return;
					}
				}
				else {
					// Must be the crawl order file.
					if(crawlOrderFile != null){
						// Trying to set the crawl order file for the second time. This is an error.
						usage();
						return;
					}
					else{
						crawlOrderFile = arg;
					}
				}
			}
			
			if(crawlOrderFile == null){
				if(noWUI){
					// Can't do anything.
					usage();
					return;
				}
				crawllaunch = 0;
			}
			
			if(port == -1){
				// No port, use default.
				port = SimpleHttpServer.DEFAULT_PORT;
			}
			
			if(crawllaunch == -1 && crawlOrderFile != null && noWUI == false)
			{
				//Set default crawllaunch behavior
				crawllaunch = 2;
			}
			// Ok, we should now have everything to launch the program.
			
			if(noWUI){
				launch(crawlOrderFile);
			}
			else{
				launch(port,crawlOrderFile,crawllaunch);
			}
		}
	}
	
	/**
	 * Launch the crawler without a web UI
	 * 
	 * @param crawlOrderFile The crawl order to crawl.
	 */
	protected static void launch(String crawlOrderFile){
		try {
			CrawlController controller = new CrawlController();
			CrawlOrder order = CrawlOrder.readFromFile(crawlOrderFile);
			controller.initialize(order);
			controller.startCrawl();
			// catch all configuration exceptions, which at this level are fatal
		}catch(InitializationException e){
			System.out.println("Fatal configuration exception: " + e.toString());
		}
		System.out.println("Heritrix is running.");
		System.out.println("\tNo web UI");
		System.out.println("\tCrawling " + crawlOrderFile);
	}
	
	/**
	 * Launch the crawler with a web UI
	 * 
	 * @param port The port that the web UI will run on
	 * @param crawlOrderFile A crawl order file to use
	 * @param crawllaunch How to use the crawl order file 
	 * 		              (1 = start crawling, 
	 *                     2 = ready for crawl but don't start,
	 *                     3 = set as default configuration,
	 * 					   Any other = no crawl order specified)
	 */
	protected static void launch(int port, String crawlOrderFile, int crawllaunch){
		handler = new SimpleHandler();
		String status = "";

		if(crawllaunch == 3){
			// Set crawl order file as new default 
			handler.setDefaultCrawlOrder(crawlOrderFile);
			status = "\t- default crawl order updated";
		}
		else if(crawllaunch == 1 || crawllaunch == 2){
			CrawlJob cjob = new SimpleCrawlJob(handler.getNextJobUID(),"Auto launched",crawlOrderFile, CrawlJob.PRIORITY_HIGH);
			handler.addJob(cjob);
			status = "\t1 crawl job ready and pending";
			if(crawllaunch == 1){
				handler.startCrawler();
				status = "\t1 job being crawled";
			}
		}

		try {
			SimpleHttpServer server = new SimpleHttpServer(port);
			server.startServer();
		} catch (Exception e) {
			System.out.println("Fatal IO error: " + e.getMessage());
		}
		System.out.println("Heritrix is running");
		System.out.println("\tWeb UI on port " + port);
		System.out.println(status);
	}

//	public void instanceMain(String[] args) {
//		String crawlOrderFile = "test-config/order.xml";
//
//		try {
//			switch (args.length) {
//				case 1 :
//					crawlOrderFile = args[0];
//					handler = new SimpleHandler();
//				
//					CrawlJob cjob = new SimpleCrawlJob(handler.getNextJobUID(),"Auto launched",crawlOrderFile, CrawlJob.PRIORITY_HIGH);
//				
//					handler.addJob(cjob);
//					handler.startCrawler();
//				case 0 :
//					startServer();
//					break;
//				case 2 :
//					if (args[0].equals("-no-wui")) {
//						CrawlController controller = new CrawlController();
//						crawlOrderFile = args[1];
//						CrawlOrder order = CrawlOrder.readFromFile(crawlOrderFile);
//						controller.initialize(order);
//						controller.startCrawl();
//						break;
//					}
//				default :
//					usage();
//					return;
//			}
//		// catch all configuration exceptions, which at this level are fatal
//		}catch(InitializationException e){
//			System.out.println("Fatal configuration exception: " + e.toString());
//		}
//		
//		logger.info("exitting main thread");
//		
//	}
	
	/**
	 * Print out the command line argument usage for this program.
	 * <p>
	 * java Heritrix [-no-wui | -port:xxxx] &lt;crawl order file&gt; [-start | -load | -set-as-default] [-?]<br>
	 * &nbsp;&nbsp;&nbsp;-no-wui start crawler without Web User Interface<br>
	 * &nbsp;&nbsp;&nbsp;-port:xxxx the port that the web UI will run on, 8080 is default<br>
	 * &nbsp;&nbsp;&nbsp;&lt;crawl order file&gt; optional if -no-wui not specified<br>
	 * &nbsp;&nbsp;&nbsp;-start start crawling as specified by the given crawl order file. Only valid behavior if -no-wui specified.<br>
	 * &nbsp;&nbsp;&nbsp;-load load the job specified by the given crawl order file but do not start crawling. Default behavior unless -no-wui is specified.<br>
	 * &nbsp;&nbsp;&nbsp;-set-as-default set the specified crawl order as the default crawl order<br>
	 */
	protected static void usage() {
		System.out.println(
			"Heritrix: Version unknown. Build unknown");
		System.out.println(
			"USAGE: java Heritrix [-no-wui | -port:xxxx] <crawl order file> [-start | -load | -set-as-default] [-?]");
		System.out.println(
				"\t-no-wui start crawler without Web User Interface");
		System.out.println(
				"\t-port:xxxx the port that the web UI will run on, 8080 is default");
		System.out.println(
				"\t<crawl order file> optional if -no-wui not specified");
		System.out.println(
				"\t-start start crawling as specified by the given crawl order file. Only valid behavior if -no-wui specified.");
		System.out.println(
				"\t-load load the job specified by the given crawl order file but do not start crawling. Default behavior unless -no-wui is specified.");
		System.out.println(
				"\t-set-as-default set the specified crawl order as the default crawl order");
	}

//	private void startServer() {
//		try {
//			SimpleHttpServer server = new SimpleHttpServer();
//			server.startServer();
//		} catch (Exception e) {
//			e.printStackTrace(System.out);
//		}
//
//	}
//
//	private void startServer(CrawlController c) {
//		try {
//			SimpleHttpServer server = new SimpleHttpServer();
//			server.startServer();
//		} catch (Exception e) {
//			e.printStackTrace(System.out);
//		}
//	}
	
	/**
	 * Get the job handler
	 * 
	 * @return The CrawlJobHandler being used.
	 */	
	public static SimpleHandler getHandler()
	{
		return handler;	
	}
}