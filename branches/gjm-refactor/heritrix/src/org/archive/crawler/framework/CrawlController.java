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
 * CrawlController collects all the classes which cooperate to
 * perform a crawl, provides a high-level interface to the
 * running crawl, and executes the "master thread" which doles
 * out URIs from the Frontier to the ToeThreads. 
 * 
 * As the "global context" for a crawl, subcomponents will 
 * usually reach each other through the CrawlController. 
 * 
 * @author Gordon Mohr
 */
public class CrawlController {
	private static final String LOGNAME_PROGRESS_STATISTICS = "progress-statistics";
	private static final String LOGNAME_URI_ERRORS = "uri-errors";
	private static final String LOGNAME_RUNTIME_ERRORS = "runtime-errors";
	private static final String LOGNAME_CRAWL = "crawl";
	private static final String XP_STATS_LEVEL = "//loggers/crawl-statistics/@level";
	private static final String XP_STATS_INTERVAL = "//loggers/crawl-statistics/@interval";
	private static final String XP_DISK_PATH = "//behavior/@disk-path";
	private static final String XP_PROCESSORS = "//behavior/processors/processor";
	private static final String XP_FRONTIER = "//behavior/frontier";
	private static final String XP_CRAWL_SCOPE = "//scope";
	private int timeout = 1000; // to wait for CrawlURI from frontier before spinning
	private ToePool toePool;
	private URIFrontier frontier;
	private boolean shouldCrawl;

	public static final int DEFAULT_STATISTICS_REPORT_INTERVAL = 60;

	private File disk;
	public Logger uriProcessing = Logger.getLogger(LOGNAME_CRAWL);
	public Logger crawlErrors = Logger.getLogger(LOGNAME_RUNTIME_ERRORS);
	public Logger uriErrors = Logger.getLogger(LOGNAME_URI_ERRORS);
	public Logger progressStats = Logger.getLogger(LOGNAME_PROGRESS_STATISTICS);
	
	// create a statistic tracking object and have it write to the log every 
	protected StatisticsTracker statistics = null;

	CrawlOrder order;
	CrawlScope scope;
		
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


	/**
	 * Starting from nothing, set up CrawlController and associated
	 * classes to be ready fro crawling. 
	 * 
	 * @param o CrawlOrder
	 * @throws InitializationException
	 */
	public void initialize(CrawlOrder o) throws InitializationException {
		order = o;	
		order.initialize();
		
		checkUserAgentAndFrom();
		
		// read from the configuration file
		try {

			setupDisk();
			setupLogs();
			
		} catch (IOException e) {
			throw new InitializationException("Unable to create log file(s): " + e.toString(), e);
		}

		setupStatTracking();
		setupCrawlModules();
	}

	private void setupCrawlModules() throws FatalConfigurationException {
		scope = (CrawlScope) order.instantiate(XP_CRAWL_SCOPE);
		frontier = (URIFrontier) order.instantiate(XP_FRONTIER);
		
		firstProcessor = (Processor) order.instantiateAllInto(XP_PROCESSORS,processors);
		
		// try to initialize each scope and frontier from the config file
		try {
			scope.initialize(this);
		} catch (NullPointerException e) {
			throw new FatalConfigurationException(
				"Can't initialize scope, class specified in configuration file not found",
				order.getCrawlOrderFilename(),
				XP_CRAWL_SCOPE);
		}
		try {
			frontier.initialize(this);
		} catch (NullPointerException e) {
			throw new FatalConfigurationException(
				"Can't initialize frontier, class specified in configuration file not found",
				order.getCrawlOrderFilename(),
				XP_FRONTIER);
		}
			
		hostCache = new ServerCache();
		
		Iterator iter = processors.entrySet().iterator();
		while (iter.hasNext()) {
			Object obj = iter.next();
			System.out.println(obj);
			Processor p = (Processor) ((Map.Entry)obj).getValue();
			p.initialize(this);
		}
	}


	private void setupDisk() throws FatalConfigurationException {
		String diskPath = order.getStringAt(XP_DISK_PATH);
		if(diskPath == null || diskPath.length() == 0){
			throw new FatalConfigurationException("No output Directory specified", 
									order.getCrawlOrderFilename(), 
									XP_DISK_PATH
			);
		}
		
		if(! diskPath.endsWith(File.separator)){
			diskPath = diskPath + File.separator;
		}
		disk = new File(diskPath);
		disk.mkdirs();
	}


	private void setupStatTracking() {
		// the statistics object must be created before modules that use it if those 
		// modules retrieve the object from the controller during initialization 
		// (which some do).  So here we go with that.
		int interval = order.getIntAt(XP_STATS_INTERVAL, DEFAULT_STATISTICS_REPORT_INTERVAL);
		statistics = new StatisticsTracker(this, interval);
		
		// set the log level
		String logLevel = order.getStringAt(XP_STATS_LEVEL);
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
		
		// start periodic background logging of crawl statistics
		Thread statLogger = new Thread(statistics);
		statLogger.start();
		// TODO pause stat sampling when crawler paused
	}


	private void setupLogs() throws IOException {
		String diskPath = disk.getAbsolutePath();
		
		FileHandler up = new FileHandler(diskPath+LOGNAME_CRAWL+".log");
		up.setFormatter(new UriProcessingFormatter());
		uriProcessing.addHandler(up);
		uriProcessing.setUseParentHandlers(false);
		
		FileHandler cerr = new FileHandler(diskPath+LOGNAME_RUNTIME_ERRORS+".log");
		cerr.setFormatter(new CrawlErrorFormatter());
		crawlErrors.addHandler(cerr);
		crawlErrors.setUseParentHandlers(false);
		
		FileHandler uerr = new FileHandler(diskPath+LOGNAME_URI_ERRORS+".log");
		uerr.setFormatter(new UriErrorFormatter());
		uriErrors.addHandler(uerr);
		uriErrors.setUseParentHandlers(false);
		
		FileHandler stat = new FileHandler(diskPath+LOGNAME_PROGRESS_STATISTICS+".log");
		stat.setFormatter(new StatisticsLogFormatter());
		progressStats.addHandler(stat);
		progressStats.setUseParentHandlers(false);
	}

	// must include a bot name and info URL
	private static String ACCEPTABLE_USER_AGENT = 
	 "\\S+.*\\(\\+http://\\S*\\).*";
	// must include a contact email address
	private static String ACCEPTABLE_FROM =
	 "\\S+@\\S+\\.\\S+";
	 
	private void checkUserAgentAndFrom() throws InitializationException {
		// don't start the crawl if they're using the default user-agent
		String userAgent = order.getUserAgent();
		String from = order.getFrom();
		if(!userAgent.matches(ACCEPTABLE_USER_AGENT)||!from.matches(ACCEPTABLE_FROM)) {
			throw new FatalConfigurationException(
				"You must set the User-Agent and From HTTP header values " +
				"to acceptable strings before proceeding. \n" +
				" User-Agent: [software-name](+[info-url])[misc]\n" +
				" From: [email-address]");
		}
	}
	
	/**
	 * @param thread
	 */
	public void toeFinished(ToeThread thread) {
		// for now do nothing
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
		toePool = new ToePool(this,order.getMaxToes());
	}

	/**
	 * 
	 */
	public CrawlOrder getOrder() {
		return order;
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


	/**
	 * 
	 */
	public CrawlScope getScope() {
		return scope;
	}
}
