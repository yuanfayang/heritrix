/* 
 * CrawlController.java
 * Created on May 14, 2003
 *
 * $Header$
 */
package org.archive.crawler.framework;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.ArrayList;

import org.archive.crawler.admin.AdminConstants;
import org.archive.crawler.admin.StatisticsTracker;
import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.ServerCache;
import org.archive.crawler.framework.exceptions.FatalConfigurationException;
import org.archive.crawler.framework.exceptions.InitializationException;
import org.archive.crawler.io.LocalErrorFormatter;
import org.archive.crawler.io.RuntimeErrorFormatter;
import org.archive.crawler.io.StatisticsLogFormatter;
import org.archive.crawler.io.UriErrorFormatter;
import org.archive.crawler.io.UriProcessingFormatter;
import org.archive.util.ArchiveUtils;

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
public class CrawlController extends Thread{
	private static Logger logger = Logger.getLogger("org.archive.crawler.framework.CrawlController");

	private static final String LOGNAME_PROGRESS_STATISTICS = "progress-statistics";
	private static final String LOGNAME_URI_ERRORS = "uri-errors";
	private static final String LOGNAME_RUNTIME_ERRORS = "runtime-errors";
	private static final String LOGNAME_LOCAL_ERRORS = "local-errors";
	private static final String LOGNAME_CRAWL = "crawl";
	public static final String XP_STATS_LEVEL = "//loggers/crawl-statistics/@level";
	public static final String XP_STATS_INTERVAL = "//loggers/crawl-statistics/@interval-seconds";
	public static final String XP_DISK_PATH = "//behavior/@disk-path";
	public static final String XP_PROCESSORS = "//behavior/processors/processor";
	public static final String XP_FRONTIER = "//behavior/frontier";
	public static final String XP_CRAWL_SCOPE = "//scope";
	
	private String sExit; 

	public static final int DEFAULT_STATISTICS_REPORT_INTERVAL = 60;
	
	public static final int DEFAULT_MASTER_THREAD_PRIORITY = Thread.NORM_PRIORITY + 1;

	private int timeout = 1000; // to wait for CrawlURI from frontier before spinning
	private Thread controlThread;
	private ToePool toePool;
	private URIFrontier frontier;
	private boolean shouldCrawl;

	private File disk;
	private File scratchDisk;
	public Logger uriProcessing = Logger.getLogger(LOGNAME_CRAWL);
	public Logger runtimeErrors = Logger.getLogger(LOGNAME_RUNTIME_ERRORS);
	public Logger localErrors = Logger.getLogger(LOGNAME_LOCAL_ERRORS);
	public Logger uriErrors = Logger.getLogger(LOGNAME_URI_ERRORS);
	public Logger progressStats = Logger.getLogger(LOGNAME_PROGRESS_STATISTICS);
	
	// create a statistic tracking object and have it write to the log every 
	protected StatisticsTracker statistics = null;
	
	private ArrayList registeredListeners;	

	CrawlOrder order;
	CrawlScope scope;
		
	Processor firstProcessor;
    Processor postprocessor;
	LinkedHashMap processors = new LinkedHashMap(); 
	int nextToeSerialNumber = 0;
	
	ServerCache serverCache;
	//ThreadKicker kicker;
	
	private boolean paused = false;
	private boolean finished = false;

	/**
	 * Starting from nothing, set up CrawlController and associated
	 * classes to be ready for crawling. 
	 * 
	 * @param o CrawlOrder
	 * @throws InitializationException
	 */
	public void initialize(CrawlOrder o) throws InitializationException {
		order = o;	
		order.initialize();
		
		checkUserAgentAndFrom();
		
		sExit = "";
		
		// read from the configuration file
		try {

			setupDisk();
			setupLogs();
			
		} catch (IOException e) {
			throw new InitializationException("Unable to create log file(s): " + e.toString(), e);
		}

		setupStatTracking();
		setupToePool();
		setupCrawlModules();
		
	}
	
	/**
	 * If an owner is specified then the CrawlController will call the 
	 * owners jobFinished() method just before it exits.
	 * 
	 * @param owner
	 */
	public void addListener(CrawlListener cl)
	{
		if(registeredListeners == null)
		{
			registeredListeners = new ArrayList();	
		}
		registeredListeners.add(cl);
	}

	private void setupCrawlModules() throws FatalConfigurationException {
		scope = (CrawlScope) order.instantiate(XP_CRAWL_SCOPE);
		frontier = (URIFrontier) order.instantiate(XP_FRONTIER);
		
		firstProcessor = (Processor) order.instantiateAllInto(XP_PROCESSORS,processors);
		
		// try to initialize each scope and frontier from the config file

		
		scope.initialize(this);
		try {
			frontier.initialize(this);
		} catch (IOException e) {
			throw new FatalConfigurationException("unable to initialize frontier: "+e);
		}
			
		serverCache = new ServerCache();
		
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
		scratchDisk = new File(diskPath,"scratch");
		scratchDisk.mkdirs();
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
	}


	private void setupLogs() throws IOException {
		String diskPath = disk.getAbsolutePath() + File.separatorChar;
		
		FileHandler up = new FileHandler(diskPath+LOGNAME_CRAWL+".log");
		up.setFormatter(new UriProcessingFormatter());
		uriProcessing.addHandler(up);
		uriProcessing.setUseParentHandlers(false);
		
		FileHandler cerr = new FileHandler(diskPath+LOGNAME_RUNTIME_ERRORS+".log");
		cerr.setFormatter(new RuntimeErrorFormatter());
		runtimeErrors.addHandler(cerr);
		runtimeErrors.setUseParentHandlers(false);
		
		FileHandler lerr = new FileHandler(diskPath+LOGNAME_LOCAL_ERRORS+".log");
		lerr.setFormatter(new LocalErrorFormatter());
		localErrors.addHandler(lerr);
		localErrors.setUseParentHandlers(false);
		
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
		shouldCrawl=true;
		logger.info("Should start Crawl");

		this.start();
	}

	public void run() {
		logger.fine(getName()+" started for CrawlController");
		sExit = CrawlJob.STATUS_FINISHED_ABNORMAL; // A proper exit will change this value.
		assert controlThread == null: "non-null control thread";
		controlThread = Thread.currentThread();
		controlThread.setName("crawlControl");
		controlThread.setPriority(DEFAULT_MASTER_THREAD_PRIORITY);

		// start periodic background logging of crawl statistics
		Thread statLogger = new Thread(statistics);
		statLogger.setName("StatLogger");
		statLogger.start();

		while(shouldCrawl()) {
			 CrawlURI curi = frontier.next(timeout);
			 if(curi != null) {
				curi.setNextProcessor(firstProcessor);
			 	ToeThread toe = toePool.available();
			 	//if (toe !=null) {
			 		logger.fine(toe.getName()+" crawl: "+curi.getURIString());
			 		toe.crawl(curi);
			 	//} 
			 }
		}
		controlThread = null;
		logger.info("exitting run");
		
		//Do cleanup to facilitate GC.
		while(registeredListeners.size()>0)
		{
			// Let the listeners know that the crawler is finished.
			((CrawlListener)registeredListeners.get(0)).crawlEnding(sExit);
			registeredListeners.remove(0);
		}

		frontier = null;
		disk = null;
		scratchDisk = null;
		
		toePool = null;
		registeredListeners = null;
		order = null;
		scope = null;
		firstProcessor = null;
		postprocessor = null;
		processors = null; 
		serverCache = null;

		logger.fine(getName()+" finished for order CrawlController");
	}

	/**
	 * @return
	 */
	private boolean shouldCrawl() {
		if(frontier.isEmpty())
		{
			sExit = CrawlJob.STATUS_FINISHED;
		}
		return shouldCrawl && !frontier.isEmpty();
	}


	public void stopCrawl() {
		sExit = CrawlJob.STATUS_ABORTED;
		shouldCrawl = false;
	}	

	
	public int getActiveToeCount(){
		return toePool.getActiveToeCount();
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
	public ServerCache getServerCache() {
		return serverCache;
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
		
		int kPerSec = statistics.currentProcessedKBPerSec()/1000;
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

	/**
	 * @return
	 */
	public Processor getPostprocessor() {
		return postprocessor;
	}

	/**
	 * @param processor
	 */
	public void setPostprocessor(Processor processor) {
		postprocessor = processor;
	}


	/**
	 * 
	 */
	public File getDisk() {
		return disk;
	}


	/**
	 * 
	 */
	public File getScratchDisk() {
		return scratchDisk;
	}

	/**
	 * @return
	 */
	public int getToeCount() {
		return toePool.getToeCount();
	}

	/**
	 * Compiles and returns a human readable report on the ToeThreads in it's ToePool.
	 * 
	 * @return
	 */
	public String reportThreads()
	{
		StringBuffer rep = new StringBuffer();	
		
		rep.append("Toe threads report - " + ArchiveUtils.TIMESTAMP12.format(new Date()) + "\n");
		rep.append(" Job being crawled:         " + getOrder().getStringAt(AdminConstants.XP_CRAWL_ORDER_NAME)+"\n");
		
		rep.append(" Number of toe threads in pool: " + toePool.getToeCount() + " (" + toePool.getActiveToeCount() + " active)\n");
		for(int i=0 ; i < toePool.getToeCount() ; i++)
		{
			rep.append("   ToeThread #"+(i+1)+"\n");
			rep.append(toePool.getReport(i));
			rep.append("\n");
		}
		
		rep.append("\n");
		rep.append("\n");
		
		
		
		return rep.toString();
	}
	
	/**
	 * Set's a new CrawlOrder for this controller.  Objects that do not cache the values they
	 * read from the CrawlOrder will be updated automatically.  
	 * 
	 * @param o The new CrawlOrder
	 */
	public void updateOrder(CrawlOrder o)
	{
		// Prepare the new CrawlOrder
		o.initialize();
		// Replace the old CrawlOrder
		order = o;
		// Resize the ToePool
		toePool.setSize(order.getMaxToes());
		// Prepare the new CrawlScope	
		CrawlScope newscope = (CrawlScope) order.instantiate(XP_CRAWL_SCOPE);
		newscope.initialize(this);
		// Replace the old CrawlScope		
		scope = newscope;
	}
}
