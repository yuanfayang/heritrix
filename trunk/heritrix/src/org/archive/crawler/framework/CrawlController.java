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

import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.HostCache;
import org.archive.crawler.io.CrawlErrorFormatter;
import org.archive.crawler.io.UriErrorFormatter;
import org.archive.crawler.io.UriProcessingFormatter;
import org.archive.crawler.basic.StatisticsTracker;

/**
 * 
 * @author Gordon Mohr
 */
public class CrawlController {
	private File disk;
	public Logger uriProcessing = Logger.getLogger("uri-processing");
	public Logger crawlErrors = Logger.getLogger("crawl-errors");
	public Logger uriErrors = Logger.getLogger("uri-errors");
	
	protected StatisticsTracker statistics = new StatisticsTracker(this);

	CrawlOrder order;
	
	URIScheduler scheduler;
	URIStore store;
	URISelector selector;
	
	Processor firstProcessor;
	LinkedHashMap processors = new LinkedHashMap(); 
	List toes = new LinkedList(); /* of ToeThreads */;
	int nextToeSerialNumber = 0;
	
	HostCache hostCache;
	//ThreadKicker kicker;
	
	private boolean paused = false;
	private boolean finished = false;

	/** Return the list of toes (threads) this controller is using.
	 */
	public List getToes(){
		return toes;
	}


	public void initialize(CrawlOrder o) {
		order = o;
				
		store = (URIStore) order.getBehavior().instantiate("//store");
		scheduler = (URIScheduler) order.getBehavior().instantiate("//scheduler");
		selector = (URISelector) order.getBehavior().instantiate("//selector");
		
		firstProcessor = (Processor) order.getBehavior().instantiateAllInto("//processors/processor",processors);
		
		store.initialize(this);
		scheduler.initialize(this);
		selector.initialize(this);
		
		hostCache = new HostCache();
		//kicker = new ThreadKicker();
		//kicker.start();
		
		Iterator iter = processors.entrySet().iterator();
		while (iter.hasNext()) {
			Object obj = iter.next();
			System.out.println(obj);
			Processor p = (Processor) ((Map.Entry)obj).getValue();
			p.initialize(this);
		}
		
		try {
			String diskPath = order.getStringAt("//disk/@path");
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
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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


	/**
	 * @param thread
	 * @return
	 */
	public CrawlURI crawlUriFor(ToeThread thread) {
		if( paused ) {
			thread.pauseAfterCurrent();
			return null;
		}
		// TODO check global limits, etc to see if finished
		if ( finished  ) {	
			thread.stopAfterCurrent();
			return null;
		}
		CrawlURI curi = scheduler.curiFor(thread);
		if (curi != null) {
			curi.setNextProcessor(firstProcessor);
			curi.setThreadNumber(thread.getSerialNumber());
			curi.setController(this);
		}
		return curi;
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
		// assume scheduler/URIStore already loaded state
		
		// start toes
		adjustToeCount();
		Iterator iter = toes.iterator();
		while(iter.hasNext()) {
			((ToeThread)iter.next()).unpause();
		}
	}
	
	public int getActiveToeCount(){
		List toes = getToes();
		int active = 0;
		
		Iterator list = toes.listIterator();
		
		while(list.hasNext()){
			ToeThread t = (ToeThread)list.next();
			if(!t.isPaused()){
				active++;
			}
		}
		return active;	
	}
	
	public int getToeCount(){
		return toes.size();
	}
	
	private void adjustToeCount() {
		while(toes.size()<order.getBehavior().getMaxToes()) {
			// TODO make number of threads self-optimizing
			ToeThread newThread = new ToeThread(this,nextToeSerialNumber);
			nextToeSerialNumber++;
			toes.add(newThread);
			newThread.start();
		}
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
	public HostCache getHostCache() {
		return hostCache;
	}

	/**
	 * 
	 */
	//public ThreadKicker getKicker() {
	//	return kicker;
	//}


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
			System.out.println("No code sistribution statistics.");
		}


	}
}
