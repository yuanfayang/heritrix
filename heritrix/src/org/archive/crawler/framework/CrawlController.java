/* 
 * CrawlController.java
 * Created on May 14, 2003
 *
 * $Header$
 */
package org.archive.crawler.framework;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import org.archive.crawler.basic.StatisticsTracker;
import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FatalConfigurationException;
import org.archive.crawler.datamodel.ServerCache;
import org.archive.crawler.datamodel.InitializationException;
import org.archive.crawler.io.CrawlErrorFormatter;
import org.archive.crawler.io.StatisticsLogFormatter;
import org.archive.crawler.io.UriErrorFormatter;
import org.archive.crawler.io.UriProcessingFormatter;

/**
 * 
 * @author Gordon Mohr
 */
public class CrawlController {
	private int timeout = 1000; // to wait for CrawlURI from frontier before spinning
	private ToePool toePool;
	private URIFrontier frontier;
	private boolean shouldCrawl;

	public static final int DEFAULT_STATISTICS_REPORT_INTERVAL = 60;

	private File disk;
	public Logger uriProcessing = Logger.getLogger("crawl");
	public Logger crawlErrors = Logger.getLogger("runtime-errors");
	public Logger uriErrors = Logger.getLogger("uri-errors");
	public Logger progressStats = Logger.getLogger("progress-statistics");
	
	// create a statistic tracking object and have it write to the log every 
	protected StatisticsTracker statistics = null;

	CrawlOrder order;
	CrawlScope scope;
	
	URIScheduler scheduler;
	URIStore store;
	URISelector selector;
		
	Processor firstProcessor;
	LinkedHashMap processors = new LinkedHashMap(); 
	List toes = new LinkedList(); /* of ToeThreads */;
	int nextToeSerialNumber = 0;
	
	ServerCache hostCache;
	//ThreadKicker kicker;
	
	private boolean paused = false;
	private boolean finished = false;

	/** Return the list of toes (threads) this controller is using.
	 */
	public List getToes(){
		return toes;
	}


	public void initialize(CrawlOrder o) throws InitializationException {
		order = o;	
		order.initialize();
		
		checkUserAgentAndFrom();
		
		String diskPath = order.getStringAt("//disk/@path");
		if(diskPath == null || diskPath.length() == 0){

			throw new FatalConfigurationException("No output Directory specified", 
									order.crawlOrderFilename, 
									"//disk/@path"
			);
		}
		
			
		// read from the configuration file
		try {

			if(! diskPath.endsWith(File.separator)){
				diskPath = diskPath + File.separator;
			}
			disk = new File(diskPath);
			disk.mkdirs();
			
			FileHandler up = new FileHandler(diskPath+"uri-processing.log");
			up.setFormatter(new UriProcessingFormatter());
			uriProcessing.addHandler(up);
			uriProcessing.setUseParentHandlers(false);
			
			FileHandler cerr = new FileHandler(diskPath+"crawl-errors.log");
			cerr.setFormatter(new CrawlErrorFormatter());
			crawlErrors.addHandler(cerr);
			crawlErrors.setUseParentHandlers(false);
			
			FileHandler uerr = new FileHandler(diskPath+"uri-errors.log");
			uerr.setFormatter(new UriErrorFormatter());
			uriErrors.addHandler(uerr);
			uriErrors.setUseParentHandlers(false);
			
			FileHandler stat = new FileHandler(diskPath+"progress-statistics.log");
			stat.setFormatter(new StatisticsLogFormatter());
			progressStats.addHandler(stat);
			progressStats.setUseParentHandlers(false);
			
			
		} catch (IOException e) {
			throw new InitializationException("Unable to create log file(s): " + e.toString(), e);
		}

		// the statistics object must be created before modules that use it if those 
		// modules retrieve the object from the controller during initialization 
		// (which some do).  So here we go with that.
		int interval = order.getIntAt("//crawl-statistics/interval", DEFAULT_STATISTICS_REPORT_INTERVAL);
		statistics = new StatisticsTracker(this, interval);
		
		// set the log level
		String logLevel = order.getStringAt("//loggers/crawl-statistics/level");
		if(logLevel != null){
			if(logLevel.toLowerCase().equals("mercator")){
				statistics.setLogLevel(StatisticsTracker.MERCATOR_LOGGING);
			}else if(logLevel.toLowerCase().equals("human")){
				statistics.setLogLevel(StatisticsTracker.HUMAN_LOGGING);
			}else if(logLevel.toLowerCase().equals("verbose")){
				statistics.setLogLevel(StatisticsTracker.VERBOSE_LOGGING);
			}			
		}
		//statistics.setLogLevel(StatisticsTracker.VERBOSE_LOGGING);
			
		scope = (CrawlScope) order.instantiate("//scope");
		frontier = (URIFrontier) order.instantiate("//frontier");
		
		firstProcessor = (Processor) order.getBehavior().instantiateAllInto("//processors/processor",processors);
		
		// try to initialize each scope and frontier from the config file
		try {
			scope.initialize(this);
		} catch (NullPointerException e) {
			throw new FatalConfigurationException(
				"Can't initialize scope, class specified in configuration file not found",
				order.crawlOrderFilename,
				"//scope");
		}
		try {
			frontier.initialize(this);
		} catch (NullPointerException e) {
			throw new FatalConfigurationException(
				"Can't initialize frontier, class specified in configuration file not found",
				order.crawlOrderFilename,
				"//frontier");
		}			
		hostCache = new ServerCache();
		
		Iterator iter = processors.entrySet().iterator();
		while (iter.hasNext()) {
			Object obj = iter.next();
			System.out.println(obj);
			Processor p = (Processor) ((Map.Entry)obj).getValue();
			p.initialize(this);
		}
		
		// start periodic background logging of crawl statistics
		Thread statLogger = new Thread(statistics);
		statLogger.start();
	}

	// must include a bot name and info URL
	private static String ACCEPTABLE_USER_AGENT = 
	 "\\S+.*\\(\\+http://\\S*\\).*";
	// must include a contact email address
	private static String ACCEPTABLE_FROM =
	 "\\S+@\\S+\\.\\S+";
	 
	private void checkUserAgentAndFrom() throws InitializationException {
		// don't start the crawl if they're using the default user-agent
		String userAgent = order.getBehavior().getUserAgent();
		String from = order.getBehavior().getFrom();
		if(!userAgent.matches(ACCEPTABLE_USER_AGENT)||!from.matches(ACCEPTABLE_FROM)) {
			throw new FatalConfigurationException(
				"You must set the User-Agent and From HTTP header values " +
				"to acceptable strings before proceeding. \n" +
				" User-Agent: [software-name](+[info-url])[misc]\n" +
				" From: [email-address]");
		}
	}
	
	/**
	 * 
	 */
	public URIScheduler getScheduler() {
		return scheduler;
		
	}

	/**
	 * @param thread
	 */
	public void toeFinished(ToeThread thread) {
		// TODO Auto-generated method stub
		
	}


	/**
	 * 
	 */
	public URISelector getSelector() {
		return selector;
	}

	
	/** Return the object this controller is using to track crawl statistics
	 */
	public StatisticsTracker getStatistics(){
		return statistics;
	}
	
	
	/**
	 * 
	 */
	public void startCrawl() {
		// assume Frontier state already loaded
		setupToePool();
		shouldCrawl=true;
		runCrawl();
	}


	public void runCrawl() {
		while(shouldCrawl) {
			 CrawlURI curi = frontier.next(timeout);
			 if(curi != null) {
				curi.setNextProcessor(firstProcessor);
			 	toePool.available().crawl(curi);
			 } 
		}
	}

	public void stopCrawl() {
		shouldCrawl = false;
	}	

	
	public int getActiveToeCount(){
		return toePool.getActiveToeCount();
	}
	
	public int getToeCount(){
		return toes.size();
	}
	
	private void setupToePool() {
		toePool = new ToePool(this,order.getBehavior().getMaxToes());
	}

	/**
	 * 
	 */
	public CrawlOrder getOrder() {
		return order;
	}

	/**
	 * @return
	 */
	public URIStore getStore() {
		return store;
	}

	/**
	 * 
	 */
	public HashMap getProcessors() {
		return processors;
	}

	/**
	 * 
	 */
	public ServerCache getHostCache() {
		return hostCache;
	}

	/**
	 * 
	 */
	//public ThreadKicker getKicker() {
	//	return kicker;
	//}


	public void setOrder(CrawlOrder o){
		order = o;	
	}

	/** Print to stdout basic statistics about the crawl (for stat testing) */
	public void printStatistics(){
	
		//System.out.println(":");
		//System.out.println("\t:\t" + statistics.);
		
		System.out.println("Fetch Progress:");
		System.out.println("\tCompleted:\t" + statistics.percentOfDiscoveredUrisCompleted() + "% (fetched/discovered)");
		
		int kPerSec = statistics.approximateDiskWriteRate()/1000;
		System.out.println("\tDisk Write Rate:\t" + kPerSec + " kb/sec.");
		
		System.out.println("\tDiscovered URIs:\t" + statistics.urisEncounteredCount());
		System.out.println("\tFrontier (unfetched):\t" + statistics.urisInFrontierCount());
		System.out.println("\tFetch Attempts:\t" + statistics.totalFetchAttempts());
		System.out.println("\tSuccesses:\t" + statistics.successfulFetchAttempts());
		//System.out.println("\tFailures:\t" + statistics.failedFetchAttempts());

		System.out.println("Threads:");
	
		System.out.println("\tTotal:\t" + statistics.threadCount());
		System.out.println("\tActive:\t" + statistics.activeThreadCount());


		HashMap dist = statistics.getFileDistribution();
		
		if(dist.size() > 0){
			Iterator keyIterator = dist.keySet().iterator();

			System.out.println("Fetched Resources MIME Distribution:");
	
			while(keyIterator.hasNext()){
				String key = (String)keyIterator.next();
				String val = ((Integer)dist.get(key)).toString();
				
				System.out.println("\t" + key + "\t" + val);	
			}
		}else{
			System.out.println("No mime statistics");
		}
		
		HashMap codeDist = statistics.getStatusCodeDistribution();
		
		if(codeDist.size() > 0){
			
			Iterator keyIterator = codeDist.keySet().iterator();

			System.out.println("Status Code Distribution:");
	
			while(keyIterator.hasNext()){
				String key = (String)keyIterator.next();
				String val = ((Integer)codeDist.get(key)).toString();
				
				System.out.println("\t" + key + "\t" + val);	
			}
		}else{
			System.out.println("No code distribution statistics.");
		}


	}


	/**
	 * 
	 */
	public URIFrontier getFrontier() {
		return frontier;
	}
}
