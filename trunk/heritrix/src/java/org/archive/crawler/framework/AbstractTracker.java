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
 */
package org.archive.crawler.framework;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.crawler.event.CrawlStatusListener;
import org.archive.crawler.event.CrawlURIDispositionListener;
import org.archive.util.ArchiveUtils;
import org.archive.util.PaddingStringBuffer;


/**
 * A partial implementation of the StatisticsTracking interface.  
 * <p>
 * It covers the thread handling. (Launching, pausing etc.)  Included in this is 
 * keeping track of the total time spent (actually) crawling.  Several methods 
 * to access the time started, finished etc. are provided. 
 * <p>
 * To handle the thread work the class implements the CrawlStatusListener and 
 * uses it's events to pause, resume and stop logging of statistics. The run()
 * method will call logActivity() at intervals specified in the crawl order.
 * <p>
 * Implementation of logActivity (the actual logging) as well as listening for
 * CrawlURIDisposition events is not addressed.
 * 
 * @author Kristinn Sigurdsson
 * 
 * @see org.archive.crawler.framework.StatisticsTracking
 * @see org.archive.crawler.admin.StatisticsTracker
 */
public abstract class AbstractTracker implements StatisticsTracking, 
												 CrawlStatusListener,
												 CrawlURIDispositionListener
{
	// A reference to the CrawlContoller of the crawl that we are to track statistics for. 
	protected CrawlController controller;
	
	protected Logger periodicLogger = null;
	protected int logInterval = 20; // In seconds.

	// Keep track of time.
	protected long crawlerStartTime;
	protected long crawlerEndTime = -1; // Until crawl ends, this value is -1.
	protected long crawlerPauseStarted = 0;
	protected long crawlerTotalPausedTime = 0;

	// Timestamp of when this logger last wrote something to the log
	protected long lastLogPointTime;

	protected boolean shouldrun = true;

	/**
	 * Set's up the Logger (including logInterval) and registers with the CrawlController 
	 * for CrawlStatus and CrawlURIDisposition events.
	 * 
	 * @param c A crawl controller instance.
     * 
	 * @see CrawlStatusListener
	 * @see CrawlURIDispositionListener
	 */
	public void initalize(CrawlController c)
	{
		controller = c;
		periodicLogger = controller.progressStats;
		logInterval = controller.getOrder().getIntAt(CrawlController.XP_STATS_INTERVAL,CrawlController.DEFAULT_STATISTICS_REPORT_INTERVAL);
		
		// Add listeners
		controller.addCrawlStatusListener(this);	
		controller.addCrawlURIDispositionListener(this);	
	}
	
	/**
	 * Start thread.  Will call logActivity() at intvals specified by 
	 * logInterval 
	 *  
	 */
	public void run() {
		// don't start logging if we have no logger
		if (periodicLogger == null) {
			return;
		}
		
		crawlerStartTime = System.currentTimeMillis(); //Note the time the crawl starts.
		shouldrun = true; //If we are starting, this should always be true.
		
		// log the legend	
		periodicLogger.log(Level.INFO,
				"   [timestamp] [discovered]    [queued] [downloaded]"
					+ " [doc/s(avg)]  [KB/s(avg)]"
					+ " [dl-failures] [busy-thread] [mem-use-KB]"
			);

		lastLogPointTime = System.currentTimeMillis(); // The first interval begins now.

		// keep logging until someone calls stop()
		while (shouldrun) 
		{
			// pause before writing the first entry (so we have real numbers)
			// and then pause between entries 
			try {
				Thread.sleep(controller.getOrder().getIntAt(CrawlController.XP_STATS_INTERVAL, CrawlController.DEFAULT_STATISTICS_REPORT_INTERVAL) * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
				controller.runtimeErrors.log(
					Level.INFO,
					"Periodic stat logger interrupted while sleeping.");
			}
			
			// In case stop() was invoked while the thread was sleeping or we are paused.
			if(shouldrun && getCrawlPauseStartedTime()==0){ 
				logActivity();
			}
		}
	}

	/**
	 * A method for logging current state.  
	 * <p>
	 * This method will be called by run() at intervals specified in
	 * the crawl order file.  It is also invoked when pausing or
	 * stopping a crawl to capture the state at that point.
	 * <p>
	 * It is recommended that for implementations of this method it be
	 * carefully considered if it should be synchronized in whole or in 
	 * part. 
	 */
	protected abstract void logActivity();
	
	public void setCrawlStartTime(long mili){
		crawlerStartTime = mili;
	}
	
	public long getCrawlStartTime(){
		return crawlerStartTime;
	}
	
	/**
	 * 
	 * @return If crawl has ended it will return the time 
	 *         it ended (given by System.currentTimeMillis() 
	 * 		   at that time).
	 *         If crawl is still going on it will return the
	 *         same as System.currentTimeMillis()
	 */
	public long getCrawlEndTime()
	{
		if(crawlerEndTime==-1)
		{
			return System.currentTimeMillis();
		}
		
		return crawlerEndTime;
	}
	
	public long getCrawlTotalPauseTime()
	{
		return crawlerTotalPausedTime;
	}
	
	public long getCrawlPauseStartedTime()
	{
		return crawlerPauseStarted;
	}
	
	public long getCrawlerTotalElapsedTime()
	{
		if(getCrawlPauseStartedTime()!=0)
		{
			//Are currently paused, calculate time up to last pause
			return getCrawlPauseStartedTime()-getCrawlTotalPauseTime()-getCrawlStartTime();
		}
		else
		{
			//Not paused, calculate total time.
			return getCrawlEndTime()-getCrawlTotalPauseTime()-getCrawlStartTime();
		}
	}

	public void setLogWriteInterval(int interval) {
		if(interval < 0){
			logInterval = 0;
			return;
		}
		
		logInterval = interval;
	}
	
	public int getLogWriteInterval() {
		return logInterval;
	}	
	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.CrawlListener#crawlPausing(java.lang.String)
	 */
	public void crawlPausing(String statusMessage) {
		periodicLogger.log(
					Level.INFO,
					new PaddingStringBuffer()
					 .append(ArchiveUtils.TIMESTAMP14.format(new Date()))
					 .append(" CRAWL WAITING TO PAUSE")
					 .toString()
				);				
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.CrawlStatusListener#crawlPaused(java.lang.String)
	 */
	public void crawlPaused(String statusMessage) {
		crawlerPauseStarted = System.currentTimeMillis();
		logActivity();
		periodicLogger.log(
					Level.INFO,
					new PaddingStringBuffer()
					 .append(ArchiveUtils.TIMESTAMP14.format(new Date(crawlerPauseStarted)))
					 .append(" CRAWL PAUSED")
					 .toString()
				);				
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.CrawlStatusListener#crawlResuming(java.lang.String)
	 */
	public void crawlResuming(String statusMessage) {
		crawlerTotalPausedTime+=(System.currentTimeMillis()-crawlerPauseStarted);
		crawlerPauseStarted = 0;
		periodicLogger.log(
					Level.INFO,
					new PaddingStringBuffer()
					 .append(ArchiveUtils.TIMESTAMP14.format(new Date()))
					 .append(" CRAWL RESUMED")
					 .toString()
				);			
		lastLogPointTime = System.currentTimeMillis();	
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.CrawlListener#crawlEnding(java.lang.String)
	 */
	public void crawlEnding(String sExitMessage) {
		periodicLogger.log(
					Level.INFO,
					new PaddingStringBuffer()
					 .append(ArchiveUtils.TIMESTAMP14.format(new Date()))
					 .append(" CRAWL ENDING - " + sExitMessage)
					 .toString()
				);				
	}


	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.CrawlStatusListener#crawlEnded(java.lang.String)
	 */
	public void crawlEnded(String sExitMessage) {
		crawlerEndTime = System.currentTimeMillis(); //Note the time when the crawl stops.
		logActivity(); //Log end state		
		periodicLogger.log(
					Level.INFO,
					new PaddingStringBuffer()
					 .append(ArchiveUtils.TIMESTAMP14.format(new Date()))
					 .append(" CRAWL ENDED - " + sExitMessage)
					 .toString()
				);				
		shouldrun = false;
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.StatisticsTracking#crawlDuration()
	 */
	public long crawlDuration() {
		return getCrawlerTotalElapsedTime();
	}

}
