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
import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.ServerCache;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.event.CrawlStatusListener;
import org.archive.crawler.event.CrawlURIDispositionListener;
import org.archive.crawler.framework.exceptions.FatalConfigurationException;
import org.archive.crawler.framework.exceptions.InitializationException;
import org.archive.crawler.io.LocalErrorFormatter;
import org.archive.crawler.io.PassthroughFormatter;
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
public class CrawlController extends Thread {
	private static Logger logger = Logger.getLogger("org.archive.crawler.framework.CrawlController");

	private static final String LOGNAME_PROGRESS_STATISTICS = "progress-statistics";
	private static final String LOGNAME_URI_ERRORS = "uri-errors";
	private static final String LOGNAME_RUNTIME_ERRORS = "runtime-errors";
	private static final String LOGNAME_LOCAL_ERRORS = "local-errors";
	private static final String LOGNAME_CRAWL = "crawl";
	private static final String LOGNAME_RECOVER = "recover";
	public static final String XP_STATS_LEVEL = "//loggers/crawl-statistics/@level";
	public static final String XP_STATS_INTERVAL = "//loggers/crawl-statistics/@interval-seconds";
	public static final String XP_DISK_PATH = "//behavior/@disk-path";
	public static final String XP_PROCESSORS = "//behavior/processors/processor";
	public static final String XP_FRONTIER = "//behavior/frontier";
	public static final String XP_CRAWL_SCOPE = "//scope";
	public static final String XP_MAX_BYTES_DOWNLOAD = "//behavior/@max-bytes-download";
	public static final String XP_MAX_DOCUMENT_DOWNLOAD = "//behavior/@max-document-download";
	public static final String XP_MAX_TIME = "//behavior/@max-time-sec";
	
	protected String sExit; 

	public static final int DEFAULT_STATISTICS_REPORT_INTERVAL = 60;
	
	public static final int DEFAULT_MASTER_THREAD_PRIORITY = Thread.NORM_PRIORITY + 1;

	private int timeout = 1000; // to wait for CrawlURI from frontier before spinning
	private Thread controlThread;
	private ToePool toePool;
	private URIFrontier frontier;
	private boolean shouldCrawl;
	private boolean shouldPause;

	private File disk;
	private File scratchDisk;
	public Logger uriProcessing = Logger.getLogger(LOGNAME_CRAWL);
	public Logger runtimeErrors = Logger.getLogger(LOGNAME_RUNTIME_ERRORS);
	public Logger localErrors = Logger.getLogger(LOGNAME_LOCAL_ERRORS);
	public Logger uriErrors = Logger.getLogger(LOGNAME_URI_ERRORS);
	public Logger progressStats = Logger.getLogger(LOGNAME_PROGRESS_STATISTICS);
	public Logger recover = Logger.getLogger(LOGNAME_RECOVER);
	
	// create a statistic tracking object and have it write to the log every 
	protected StatisticsTracking statistics = null;
	
	protected ArrayList registeredCrawlStatusListeners;
	// Since there is a high probability that there will only ever by one
	// CrawlURIDispositionListner we will use this while there is only one:
	CrawlURIDispositionListener registeredCrawlURIDispositionListener; 
	// And then switch to the array once there is more then one.	
	protected ArrayList registeredCrawlURIDispositionListeners;

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

	public CrawlController() {
	}

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
	 * Register for CrawlStatus events.
	 * 
	 * @param cl a class implementing the CrawlStatusListener interface
	 * 
	 * @see CrawlStatusListener
	 */
	public void addCrawlStatusListener(CrawlStatusListener cl) {
		if(registeredCrawlStatusListeners == null) {
			registeredCrawlStatusListeners = new ArrayList();	
		}
		registeredCrawlStatusListeners.add(cl);
	}

	/**
	 * Register for CrawlURIDisposition events.
	 * 
	 * @param cl a class implementing the CrawlURIDispostionListener interface
	 * 
	 * @see CrawlURIDispositionListener
	 */
	public void addCrawlURIDispositionListener(CrawlURIDispositionListener cl) {
		registeredCrawlURIDispositionListener = null;
		if(registeredCrawlURIDispositionListeners == null)
		{
			// First listener;
			registeredCrawlURIDispositionListener = cl; //Only used for the first one while it is the only one.
			registeredCrawlURIDispositionListeners = new ArrayList(1); //We expect it to be very small.
		}
		registeredCrawlURIDispositionListeners.add(cl);
	}
	
	/**
	 * Allows an external class to raise a CrawlURIDispostion crawledURISuccessful event
	 * that will be broadcast to all listeners that have registered with the CrawlController.
	 * 
	 * @param curi - The CrawlURI that will be sent with the event notification.
	 * 
	 * @see CrawlURIDispositionListener#crawledURISuccessful(CrawlURI)
	 */
	public void throwCrawledURISuccessfulEvent(CrawlURI curi)
	{
		if(registeredCrawlURIDispositionListener != null)
		{
			// Then we'll just use that.
			registeredCrawlURIDispositionListener.crawledURISuccessful(curi);	
		}
		else
		{
			// Go through the list.
			if(registeredCrawlURIDispositionListeners != null && registeredCrawlURIDispositionListeners.size() > 0)
			{
				Iterator it = registeredCrawlURIDispositionListeners.iterator();
				while(it.hasNext()) {
					((CrawlURIDispositionListener) it.next()).crawledURISuccessful(curi);
				}
			}	
		}
	}

	/**
	 * Allows an external class to raise a CrawlURIDispostion crawledURINeedRetry event
	 * that will be broadcast to all listeners that have registered with the CrawlController.
	 * 
	 * @param curi - The CrawlURI that will be sent with the event notification.
	 * 
	 * @see CrawlURIDispositionListener#crawledURINeedRetry(CrawlURI)
	 */
	public void throwCrawledURINeedRetryEvent(CrawlURI curi)
	{
		if(registeredCrawlURIDispositionListener != null)
		{
			// Then we'll just use that.
			registeredCrawlURIDispositionListener.crawledURINeedRetry(curi);	
		}
		else
		{
			// Go through the list.
			if(registeredCrawlURIDispositionListeners != null && registeredCrawlURIDispositionListeners.size() > 0)
			{
				Iterator it = registeredCrawlURIDispositionListeners.iterator();
				while(it.hasNext()) {
					((CrawlURIDispositionListener) it.next()).crawledURINeedRetry(curi);
				}
			}	
		}
	}
	
	/**
	 * Allows an external class to raise a CrawlURIDispostion crawledURIDisregard event
	 * that will be broadcast to all listeners that have registered with the CrawlController.
	 * 
	 * @param curi - The CrawlURI that will be sent with the event notification.
	 * 
	 * @see CrawlURIDispositionListener#crawledURIDisregard(CrawlURI)
	 */
	public void throwCrawledURIDisregardEvent(CrawlURI curi)
	{
		if(registeredCrawlURIDispositionListener != null)
		{
			// Then we'll just use that.
			registeredCrawlURIDispositionListener.crawledURIDisregard(curi);	
		}
		else
		{
			// Go through the list.
			if(registeredCrawlURIDispositionListeners != null && registeredCrawlURIDispositionListeners.size() > 0)
			{
				Iterator it = registeredCrawlURIDispositionListeners.iterator();
				while(it.hasNext()) {
					((CrawlURIDispositionListener) it.next()).crawledURIDisregard(curi);
				}
			}	
		}
	}

	/**
	 * Allows an external class to raise a CrawlURIDispostion crawledURIFailure event
	 * that will be broadcast to all listeners that have registered with the CrawlController.
	 * 
	 * @param curi - The CrawlURI that will be sent with the event notification.
	 * 
	 * @see CrawlURIDispositionListener#crawledURIFailure(CrawlURI)
	 */
	public void throwCrawledURIFailureEvent(CrawlURI curi)
	{
		if(registeredCrawlURIDispositionListener != null)
		{
			// Then we'll just use that.
			registeredCrawlURIDispositionListener.crawledURIFailure(curi);	
		}
		else
		{
			// Go through the list.
			if(registeredCrawlURIDispositionListeners != null && registeredCrawlURIDispositionListeners.size() > 0)
			{
				Iterator it = registeredCrawlURIDispositionListeners.iterator();
				while(it.hasNext()) {
					((CrawlURIDispositionListener) it.next()).crawledURIFailure(curi);
				}
			}	
		}
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
			logger.info(obj.toString());
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
		statistics = new StatisticsTracker(); //TODO: Read from configuration file what implementation of StatisticsTracking to use
		statistics.initalize(this);
	
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
		
		FileHandler reco = new FileHandler(diskPath+LOGNAME_RECOVER+".log");
		reco.setFormatter(new PassthroughFormatter());
		recover.addHandler(reco);
		recover.setUseParentHandlers(false);
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
	
	/** 
     * @return Object this controller is using to track crawl statistics
	 */
	public StatisticsTracking getStatistics(){
		return statistics;
	}
	
	/**
	 * 
	 */
	public void startCrawl() {
		// assume Frontier state already loaded
		shouldCrawl=true;
		shouldPause=false;
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
			if (shouldPause) {
				synchronized (this) {
					try {
						// Wait until all ToeThreads are finished with their work
						while (getActiveToeCount() > 0) {
							wait(200);
						}
						paused = true;
						
						// Tell everyone that we have paused
						logger.info("Crawl job paused");
						Iterator it = registeredCrawlStatusListeners.iterator();
						while(it.hasNext()) {
							((CrawlStatusListener) it.next()).crawlPaused(CrawlJob.STATUS_PAUSED);
						}
						
						wait();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					paused = false;
					logger.info("Crawl job resumed");

					// Tell everyone that we have resumed from pause
					Iterator it = registeredCrawlStatusListeners.iterator();
					while(it.hasNext()) {
						((CrawlStatusListener) it.next()).crawlResuming(CrawlJob.STATUS_RUNNING);
					}
				}
			}
		
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

		// Tell everyone that this crawl is ending (threads will take this to mean that they are to exit.
		Iterator it = registeredCrawlStatusListeners.iterator();
		while(it.hasNext()) {
			((CrawlStatusListener) it.next()).crawlEnding(sExit);
		}
		
		// Ok, now we are ready to exit.		
		while(registeredCrawlStatusListeners.size()>0)
		{
			// Let the listeners know that the crawler is finished.
			((CrawlStatusListener)registeredCrawlStatusListeners.get(0)).crawlEnded(sExit);
			registeredCrawlStatusListeners.remove(0);
		}

		logger.info("exitting run");

		//Do cleanup to facilitate GC.
		controlThread = null;
		frontier = null;
		disk = null;
		scratchDisk = null;
		
		toePool = null;
		registeredCrawlStatusListeners = null;
		order = null;
		scope = null;
		firstProcessor = null;
		postprocessor = null;
		processors = null; 
		serverCache = null;

		logger.fine(getName()+" finished for order CrawlController");
	}

	private boolean shouldCrawl() {
		if(frontier.isEmpty())
		{
			sExit = CrawlJob.STATUS_FINISHED;
		}
		if(order.getLongAt(XP_MAX_BYTES_DOWNLOAD,0) > 0 && frontier.totalBytesWritten()>= order.getLongAt(XP_MAX_BYTES_DOWNLOAD,0))
		{
			// Hit the max byte download limit!
			sExit = CrawlJob.STATUS_FINISHED_DATA_LIMIT;
			shouldCrawl = false;
		}
		else if(order.getLongAt(XP_MAX_DOCUMENT_DOWNLOAD,0) > 0 && frontier.successfullyFetchedCount()>= order.getLongAt(XP_MAX_DOCUMENT_DOWNLOAD,0))
		{
			// Hit the max document download limit!
			sExit = CrawlJob.STATUS_FINISHED_DOCUMENT_LIMIT;
			shouldCrawl = false;
		}
		else if(order.getLongAt(XP_MAX_TIME,0) > 0 && statistics.crawlDuration()>= order.getLongAt(XP_MAX_TIME,0)*1000)
		{
			// Hit the max byte download limit!
			sExit = CrawlJob.STATUS_FINISHED_TIME_LIMIT;
			shouldCrawl = false;
		}
		return shouldCrawl && !frontier.isEmpty();
	}


	public void stopCrawl() {
		sExit = CrawlJob.STATUS_ABORTED;
		shouldCrawl = false;
		// If crawl is paused it should be resumed first so it
		// can be stopped properly
		resumeCrawl();
	}	

	/**
	 * Stop the crawl temporarly.
	 */
	public synchronized void pauseCrawl() {
		if(shouldPause) {
			// Already about to pause
			return;
		}
		sExit = CrawlJob.STATUS_WAITING_FOR_PAUSE;
		shouldPause = true;
		logger.info("Pausing crawl job ...");

		// Notify listeners that we are going to pause
		Iterator it = registeredCrawlStatusListeners.iterator();
		while(it.hasNext()) {
			((CrawlStatusListener) it.next()).crawlPausing(sExit);
		}
	}
	
	/**
	 * Tell if the controller is paused
	 * @return true if paused
	 */
	public boolean isPaused() {
		return paused;
	}
	
	/**
	 * Resume crawl from paused state
	 */
	public synchronized void resumeCrawl() {
		if(!paused ) {
			// Can't resume if not been told to pause
			return;
		}
		
		shouldPause = false;
		notify();
	}


	
	public int getActiveToeCount(){
		return toePool.getActiveToeCount();
	}
	
	private void setupToePool() {
		toePool = new ToePool(this,order.getMaxToes());
	}

	public CrawlOrder getOrder() {
		return order;
	}

	public HashMap getProcessors() {
		return processors;
	}

	public ServerCache getServerCache() {
		return serverCache;
	}

	/**
	 * @param o
	 */
	public void setOrder(CrawlOrder o){
		order = o;	
	}

	/** Print to stdout basic statistics about the crawl (for stat testing) * @return
	 */
//	public void printStatistics(){
//	
//		//System.out.println(":");
//		//System.out.println("\t:\t" + statistics.);
//		
//		System.out.println("Fetch Progress:");
//		System.out.println("\tCompleted:\t" + statistics.percentOfDiscoveredUrisCompleted() + "% (fetched/discovered)");
//		
//		int kPerSec = statistics.currentProcessedKBPerSec()/1000;
//		System.out.println("\tDisk Write Rate:\t" + kPerSec + " kb/sec.");
//		
//		System.out.println("\tDiscovered URIs:\t" + statistics.urisEncounteredCount());
//		System.out.println("\tFrontier (unfetched):\t" + statistics.urisInFrontierCount());
//		System.out.println("\tFetch Attempts:\t" + statistics.totalFetchAttempts());
//		System.out.println("\tSuccesses:\t" + statistics.successfulFetchAttempts());
//		//System.out.println("\tFailures:\t" + statistics.failedFetchAttempts());
//
//		System.out.println("Threads:");
//	
//		System.out.println("\tTotal:\t" + statistics.threadCount());
//		System.out.println("\tActive:\t" + statistics.activeThreadCount());
//
//
//		HashMap dist = statistics.getFileDistribution();
//		
//		if(dist.size() > 0){
//			Iterator keyIterator = dist.keySet().iterator();
//
//			System.out.println("Fetched Resources MIME Distribution:");
//	
//			while(keyIterator.hasNext()){
//				String key = (String)keyIterator.next();
//				String val = ((Integer)dist.get(key)).toString();
//				
//				System.out.println("\t" + key + "\t" + val);	
//			}
//		}else{
//			System.out.println("No mime statistics");
//		}
//		
//		HashMap codeDist = statistics.getStatusCodeDistribution();
//		
//		if(codeDist.size() > 0){
//			
//			Iterator keyIterator = codeDist.keySet().iterator();
//
//			System.out.println("Status Code Distribution:");
//	
//			while(keyIterator.hasNext()){
//				String key = (String)keyIterator.next();
//				String val = ((Integer)codeDist.get(key)).toString();
//				
//				System.out.println("\t" + key + "\t" + val);	
//			}
//		}else{
//			System.out.println("No code distribution statistics.");
//		}
//
//
//	}

	/**
	 * @return The frontier.
	 */
	public URIFrontier getFrontier() {
		return frontier;
	}

	public CrawlScope getScope() {
		return scope;
	}

	public Processor getPostprocessor() {
		return postprocessor;
	}

	/**
	 * @param processor
	 */
	public void setPostprocessor(Processor processor) {
		postprocessor = processor;
	}

	public File getDisk() {
		return disk;
	}

	public File getScratchDisk() {
		return scratchDisk;
	}

	/**
	 * @return The number of ToeThreads
	 * 
	 * @see ToePool#getToeCount()
	 */
	public int getToeCount() {
		return toePool.getToeCount();
	}

	/**
	 * @return Compiles and returns a human readable report on the
     * ToeThreads in it's ToePool.
	 */
	public String reportThreads(){
		StringBuffer rep = new StringBuffer();	
		
		rep.append("Toe threads report - " + ArchiveUtils.TIMESTAMP12.format(new Date()) + "\n");
		rep.append(" Job being crawled:         " + getOrder().getStringAt(AdminConstants.XP_CRAWL_ORDER_NAME)+"\n");
		
		rep.append(" Number of toe threads in pool: " + toePool.getToeCount() + " (" + toePool.getActiveToeCount() + " active)\n");
		for(int i=0 ; i < toePool.getToeCount() ; i++){
			rep.append("   ToeThread #"+(i+1)+"\n");
			rep.append(toePool.getReport(i));
			rep.append("\n");
		}
		
		return rep.toString();
	}
	
	/**
	 * Set's a new CrawlOrder for this controller.  Objects that do not cache the values they
	 * read from the CrawlOrder will be updated automatically.  
	 * 
	 * Some changes may not take effect until the Crawl is stopped and restarted.
	 * 
	 * The following changes will by applied immediately:
	 *  - Max link hops
	 *  - Max trans hops
	 *  - Crawl mode
	 *  - All politeness rules
	 *  - All HTTP Fetch rules except sotimeout
	 *  - All request header settings
	 *  - Number of worker threads
	 *  - Logging interval for crawl statistics
	 *  - Seeds (Any seeds that have been already crawled - regardless of wether 
	 *    they are old seeds or were simply encountered - will be ignored. Other 
	 *    valid seeds will be scheduled high.)
	 * 
	 * @param o The new CrawlOrder
	 */
	public void updateOrder(CrawlOrder o) {
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
		// Update the seed list in the frontier
		Iterator iter = getScope().getSeedsIterator();
		while (iter.hasNext()) {
			UURI u = (UURI) iter.next();
			CandidateURI caUri = new CandidateURI(u);
			caUri.setIsSeed(true);
			frontier.scheduleHigh(caUri);
		}
	}
}
