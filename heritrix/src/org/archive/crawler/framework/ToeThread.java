/* 
 * ToeThread.java
 * Created on May 14, 2003
 *
 * $Header$
 */
package org.archive.crawler.framework;

import java.util.HashMap;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.datamodel.InstancePerThread;
import org.archive.util.HttpRecorder;

/**
 * One "worker thread"; asks for CrawlURIs, processes them, 
 * repeats unless told otherwise. 
 * 
 * @author Gordon Mohr
 */
public class ToeThread extends Thread implements CoreAttributeConstants, FetchStatusCodes {
	private static Logger logger = Logger.getLogger("org.archive.crawler.framework.ToeThread");

	private ToePool pool;
	private boolean shouldCrawl = true;
	CrawlController controller;
	int serialNumber;
	HttpRecorder httpRecorder;
	HashMap localProcessors = new HashMap();
	
	CrawlURI currentCuri;
	long lastStartTime;
	long lastFinishTime;
	// in-process/on-hold curis? not for now
	// a queue of curis to do next? not for now

	/**
	 * @param c
	 */
	public ToeThread(CrawlController c, ToePool p, int sn) {
		controller = c;
		pool = p;
		serialNumber = sn;
		setName("ToeThread #"+serialNumber);
		httpRecorder = new HttpRecorder(controller.getScratchDisk(),"tt"+sn+"http");
		lastFinishTime = System.currentTimeMillis();
	}


	public synchronized void crawl(CrawlURI curi) {
		assert currentCuri == null : "attempt to clobber crawlUri";
		currentCuri = curi;
		currentCuri.setThreadNumber(serialNumber);
		notify();
	}
	
	public boolean isAvailable() {
		return currentCuri == null;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		String name = controller.getOrder().getName();
		logger.fine(getName()+" started for order '"+name+"'");
		try {
			while ( shouldCrawl ) {
				processingLoop();
			} 
			controller.toeFinished(this);
		} catch (OutOfMemoryError e) {
			e.printStackTrace();
			logger.warning(getName()+" exitting: out of memory error");
			shouldCrawl = false;
		}
		
		// Do cleanup so that objects can be GC.
		pool = null;
		controller = null;
		httpRecorder.closeRecorders();
		httpRecorder = null;
		localProcessors = null;
		
		logger.fine(getName()+" finished for order '"+name+"'");
	}
	
	private synchronized void processingLoop() {
		if ( currentCuri != null ) {
			lastStartTime = System.currentTimeMillis();
			
			try {
				while ( currentCuri.nextProcessor() != null ) {
					Processor currentProcessor = getProcessor(currentCuri.nextProcessor());
					currentProcessor.process(currentCuri);
				}
			} catch (RuntimeException e) {
				currentCuri.setFetchStatus(S_INTERNAL_ERROR);
				// store exception temporarily for logging
				currentCuri.getAList().putObject(A_RUNTIME_EXCEPTION,(Object)e);
			}
		
			controller.getFrontier().finished(currentCuri);
			lastFinishTime = System.currentTimeMillis();
			synchronized(pool) {
				currentCuri = null;
				pool.noteAvailable(this);
			}
		}
		
		try {
			wait(); // until master thread gives a new work URI
		} catch (InterruptedException e) {
			e.printStackTrace();
			logger.warning(getName()+" interrupted");
		}
	}

	/**
	 * @param processor
	 */
	private Processor getProcessor(Processor processor) {
		if(!(processor instanceof InstancePerThread)) {
			// just use the shared Processor
			 return processor;
		}
		// must use local copy of processor
		Processor localProcessor = (Processor) localProcessors.get(processor.getClass().getName());
		if (localProcessor == null) {
			localProcessor = processor.spawn();
			localProcessors.put(processor.getClass().getName(),localProcessor);
		}
		return localProcessor;
	}

	/**
	 * @return
	 */
	private boolean shouldCrawl() {
		return shouldCrawl;
	}
	
	public synchronized void stopAfterCurrent() {
		logger.info("ToeThread " + serialNumber + " has been told to stopAfterCurrent()");
		shouldCrawl = false;
		if(isAvailable())
		{
			notify();
		}
	}

	/**
	 * 
	 */
	public int getSerialNumber() {
		return serialNumber;
	}

	/**
	 * @return
	 */
	public HttpRecorder getHttpRecorder() {
		return httpRecorder;
	}

	/**
	 * @param recorder
	 */
	public void setHttpRecorder(HttpRecorder recorder) {
		httpRecorder = recorder;
	}
	
	/**
	 * Compiles and returns a report on it's status.
	 * 
	 * @return
	 */
	public String report()
	{
		StringBuffer rep = new StringBuffer();
		
		rep.append("     Serial number: "+serialNumber+"\n");
		
		long now = System.currentTimeMillis();
		
		if(lastFinishTime > lastStartTime)
		{
			// That means we finished something after we last started something
			// or in other words we are not working on anything.
			String time = Long.toString((now-lastFinishTime)/10);
			int timelength = time.length();
			rep.append("     Status:    WAITING\n");
			if(timelength>3)
			{
				time = time.substring(0,(timelength-3))+"."+time.substring((timelength-3),timelength)+" sek ago";			
			}
			else
			{
				time = time + " msek ago";
			}
			rep.append("     Finished:  "+time+"\n");
		}
		else if(lastStartTime > 0)
		{
			// We are working on something
			rep.append("     Status:    ACTIVE\n");
			String time = Long.toString((now-lastStartTime)/10);
			int timelength = time.length();
			if(timelength>3)
			{
				time = time.substring(0,(timelength-3))+"."+time.substring((timelength-3),timelength)+" sek ago";			
			}
			else
			{
				time = time + " msek ago";
			}
			rep.append("     Started:   "+time+"\n");
			if(currentCuri!=null)
			{
				rep.append("     CrawlURI:  "+currentCuri.getURIString()+"\n");
				rep.append("       Fetch attempts: "+currentCuri.getFetchAttempts()+"\n");
			}
			else
			{
				rep.append("     CrawlURI:  null\n");
			}
		}
		
		return rep.toString();		
	}
}
