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
import org.archive.util.DevUtils;
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
	 * @param p
	 * @param sn
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
		String name = controller.getOrder().getCrawlOrderName();
		logger.fine(getName()+" started for order '"+name+"'");
		// OutOfMemory catch might interfere with usual IBM JVM
		// heapdump: so commenting out. memory problems will be fatal
		// try {
			while ( shouldCrawl ) {
				processingLoop();
			} 
			controller.toeFinished(this);
		//} catch (OutOfMemoryError e) {
		//	e.printStackTrace();
		//	logger.warning(getName()+" exitting: out of memory error");
		//	shouldCrawl = false;
		//}
		
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
				currentCuri.setFetchStatus(S_RUNTIME_EXCEPTION);
				// store exception temporarily for logging
				currentCuri.getAList().putObject(A_RUNTIME_EXCEPTION,(Object)e);
			} catch (Error err) {
				// OutOfMemory & StackOverflow & etc.
				System.err.println(err);
				System.err.println(DevUtils.extraInfo());
				err.printStackTrace(System.err);
				currentCuri.setFetchStatus(S_SERIOUS_ERROR);
			}
		
			controller.getFrontier().finished(currentCuri);
			synchronized(pool) {
				currentCuri = null;
                lastFinishTime = System.currentTimeMillis();
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
		Processor localProcessor = (Processor) localProcessors.get(
                    processor.getClass().getName());
		if (localProcessor == null) {
			localProcessor = processor.spawn(this.getSerialNumber());
			localProcessors.put(processor.getClass().getName(),localProcessor);
		}
		return localProcessor;
	}

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

	public int getSerialNumber() {
		return serialNumber;
	}

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
	 * @return Compiles and returns a report on it's status.
	 */
	public String report()
	{
		StringBuffer rep = new StringBuffer();
		
		rep.append("     Serial number: "+serialNumber+"\n");
		
		long now = System.currentTimeMillis();
        
            rep.append("     Availible: "+(isAvailable()?"True":"False")+"\n");
		
		if(lastFinishTime > lastStartTime)
		{
            if(isAvailable()==false){
                rep.append("     ERROR THIS THREAD SHOULD BE AVAILIBLE!!!!!");
                rep.append("       currentCuri: "+currentCuri.getURIString()+"\n");
            }
			// That means we finished something after we last started something
			// or in other words we are not working on anything.
			rep.append("     Status:    WAITING\n");

			long time = now-lastFinishTime;
			rep.append("     Finished:  ");
			if(time>3600000)
			{
				//got hours.
				rep.append(time/3600000 + " hours., ");
				time = time % 3600000;
			}
			if(time > 60000)
			{
				rep.append(time/60000 + " min., ");
				time = time % 60000;
			}
			if(time > 1000)
			{
				rep.append(time/1000 + " sec. and ");
				time = time % 60;
			}
			rep.append(time + " msek ago\n");
		}
		else if(lastStartTime > 0)
		{
			// We are working on something
			rep.append("     Status:    ACTIVE\n");

			long time = now-lastStartTime;
			rep.append("     Started:   ");
			if(time>3600000)
			{
				//got hours.
				rep.append(time/3600000 + " hours., ");
				time = time % 3600000;
			}
			if(time > 60000)
			{
				rep.append(time/60000 + " min., ");
				time = time % 60000;
			}
			if(time > 1000)
			{
				rep.append(time/1000 + " sec. and ");
				time = time % 60;
			}
			rep.append(time + " msek ago\n");

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
