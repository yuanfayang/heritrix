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
		boolean noWUI = false;
		int port = -1;
		String crawlOrderFile = null;
		boolean start = false;
		boolean setAsDefault = false;
		
		if(args.length > 4){
			// Too many arguments. Display usage
			usage();
			return;
		}
		else {
			for(int i=0 ; i < args.length ; i++) {
				String arg = args[i];
				if(arg.indexOf("-") == 0) {
					// This is a command, not the crawl order file.
					if(arg.equalsIgnoreCase("--no-wui")){
						if(port != -1){
							// If the port has been set then you can't specify no web UI.
							usage();
							return;
						}
						else{
							noWUI = true;			
						}
					}
					else if(arg.indexOf("--port:") == 0){
						//Defining web UI port.
						if(port != -1){
							// Can't double define port.
							usage();
							return;
						}
						else{
							try{
								String p = arg.substring(7);
								port = Integer.parseInt(p);
							}
							catch(NumberFormatException e){
								usage();
								return;
							}
						}
					}
					else if(arg.equals("--start")){
						if(start){
							// Can't say --start twice
							usage();
							return;
						}
						else{
							start = true;
						}
					}
					else if(arg.equals("--set-as-default")){
						if(setAsDefault){
							// Can't say it twice
							usage();
							return;
						}
						else{
							setAsDefault = true;
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
						// Must end with '.xml'
						if(arg.length() > 4 && arg.substring(arg.length()-4).equalsIgnoreCase(".xml"))
						{
							crawlOrderFile = arg;
						}
						else
						{
							usage();
							return;
						}
					}
				}
			}
			
			if(crawlOrderFile == null){
				if(noWUI){
					// Can't do anything.
					usage();
					return;
				}
				if(setAsDefault)
				{
					//Need to specify a crawl order if you select set-as-default
					usage();
					return;
				}
			}
			
			if(port == -1){
				// No port, use default.
				port = SimpleHttpServer.DEFAULT_PORT;
			}
			
			// Ok, we should now have everything to launch the program.
			
			if(noWUI){
				launch(crawlOrderFile);
			}
			else{
				launch(port,crawlOrderFile,start,setAsDefault);
			}
		}
	}
	
	/**
	 * Launch the crawler without a web UI
	 * 
	 * @param crawlOrderFile The crawl order to crawl.
	 */
	private static void launch(String crawlOrderFile){
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
	 * @param start Should start crawling
	 * @param setAsDefault Set the values in crawlOrderFile as the default values for new jobs. 
	 */
	private static void launch(int port, String crawlOrderFile, boolean start, boolean setAsDefault){
		handler = new SimpleHandler();
		String status = "";

		if(setAsDefault){
			// Set crawl order file as new default 
			handler.setDefaultCrawlOrder(crawlOrderFile);
			status = "\tDefault crawl order updated to match: " + crawlOrderFile + "\n";
		}
		else if(crawlOrderFile != null){
			CrawlJob cjob = new SimpleCrawlJob(handler.getNextJobUID(),"Auto launched",crawlOrderFile, CrawlJob.PRIORITY_HIGH);
			handler.addJob(cjob);
			status = "\tCrawl job scheduled: " + crawlOrderFile + "\n";
		}

		if(start){
			handler.startCrawler();
			status += "\tCrawler started";
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

	/**
	 * Print out the command line argument usage for this program. 
	 * <p>
	 * <pre>USAGE: java Heritrix [--no-wui | --port:xxxx] &lt;crawl order file&gt; [--start] [--set-as-default] [-?]
	 *	--no-wui           Start crawler without Web User Interface
	 *	--port:xxxx        The port that the web UI will run on, 8080 is default
	 *	&lt;crawl order file&gt; The crawl to launch. Optional if --no-wui not specified, in which case the next 
	 *	                   two parameter controls it's behavior.
	 *
	 *	Only if web user interface is being used:
	 *	--start            Start crawling submitted crawl jobs. Without this new jobs (including the one specifed by
	 *	                   the given crawl order) will be held in the pending queue until the operator starts the 
	 *	                   crawler from the web UI.
	 *	--set-as-default   If a crawl order is specified it will be used to update the default crawl order. Selecting
	 *	                   this option means that the crawl order specified will not be started as a crawl job.
	 *
 	 *	-?                 Display this message</pre>
	 */
	public static void usage() {
		System.out.println(
			"Heritrix: Version unknown. Build unknown");
		System.out.println(
			"USAGE: java Heritrix [--no-wui | --port:xxxx] <crawl order file> [--start] [--set-as-default] [-?]");
		System.out.println(
				"\t--no-wui\t\t\tStart crawler without Web User Interface");
		System.out.println(
				"\t--port:xxxx\t\t\tThe port that the web UI will run on, 8080 is default");
		System.out.println(
				"\t<crawl order file>\tThe crawl to launch. Optional if --no-wui not specified, in which case the next"); 
		System.out.println(
				"\t\t\t\t\t\tparameter controls it's behavior.");
		System.out.println(
				"\n\tOnly if web user interface is being used:");
		System.out.println(
				"\t--start\t\t\t\tStart crawling submitted crawl jobs. Without this new jobs (including the one specifed by");
		System.out.println(
				"\t\t\t\t\t\tthe given crawl order) will be held in the pending queue until the operator starts the");
		System.out.println(
				"\t\t\t\t\t\tcrawler from the web UI.");
		System.out.println(
				"\t--set-as-default\tIf a crawl order is specified it will be used to update the default crawl order. Selecting");
		System.out.println(
				"\t\t\t\t\t\tthis option means that the crawl order specified will not be started as a crawl job.");
		System.out.println(
				"\n\t-?\t\t\t\t\tDisplay this message");
	}

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
