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
		logger.fine(getName()+" started for order '"+controller.getOrder().getName()+"'");
		try {
			while ( shouldCrawl ) {
				processingLoop();
			} 
			controller.toeFinished(this);
		} catch (OutOfMemoryError e) {
			e.printStackTrace();
			logger.warning(getName()+" pausing: out of memory error");
			shouldCrawl = false;
		}
		logger.fine(getName()+" finished for order '"+controller.getOrder().getName()+"'");
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
			currentCuri = null;
			lastFinishTime = System.currentTimeMillis();
		}
		pool.noteAvailable(this);
		
		try {
			wait();
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
	
	public void stopAfterCurrent() {
		shouldCrawl = false;
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

}
