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

import javax.management.AttributeNotFoundException;

import org.archive.crawler.event.CrawlStatusListener;
import org.archive.crawler.settings.ModuleType;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.Type;
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
public abstract class AbstractTracker extends ModuleType 
                                   implements StatisticsTracking,
                                              CrawlStatusListener{
    /** default period between logging stat values */
    public static final Integer DEFAULT_STATISTICS_REPORT_INTERVAL = new Integer(20);
    /** attrbiute name for interval setting */
    public static final String ATTR_STATS_INTERVAL = "interval-seconds";

    /** A reference to the CrawlContoller of the crawl that we are to track statistics for.*/
    protected CrawlController controller;

    // Keep track of time.
    protected long crawlerStartTime;
    protected long crawlerEndTime = -1; // Until crawl ends, this value is -1.
    protected long crawlerPauseStarted = 0;
    protected long crawlerTotalPausedTime = 0;

    /** Timestamp of when this logger last wrote something to the log */
    protected long lastLogPointTime;

    protected boolean shouldrun = true;

    /**
     * @param name
     * @param description
     */
    public AbstractTracker(String name, String description) {
        super(name, description);
        Type e = addElementToDefinition(new SimpleType(ATTR_STATS_INTERVAL,
                "The interval between writing progress information to log.",
                DEFAULT_STATISTICS_REPORT_INTERVAL));
        e.setOverrideable(false);
    }

    /**
     * Set's up the Logger (including logInterval) and registers with the CrawlController
     * for CrawlStatus and CrawlURIDisposition events.
     *
     * @param c A crawl controller instance.
     *
     * @see CrawlStatusListener
     * @see org.archive.crawler.event.CrawlURIDispositionListener
     */
    public void initalize(CrawlController c) {
        controller = c;

        // Add listeners
        controller.addCrawlStatusListener(this);
    }

    /**
     * Start thread.  Will call logActivity() at intervals specified by
     * logInterval
     *
     */
    public void run() {
        // don't start logging if we have no logger
        if (controller == null) {
            return;
        }

        crawlerStartTime = System.currentTimeMillis(); //Note the time the crawl starts.
        shouldrun = true; //If we are starting, this should always be true.

        // log the legend
        controller.progressStats.log(Level.INFO,
                "   [timestamp] [discovered]    [queued] [downloaded]"
                    + "   [doc/s(avg)]  [KB/s(avg)]"
                    + " [dl-failures] [busy-thread] [mem-use-KB]"
            );

        lastLogPointTime = System.currentTimeMillis(); // The first interval begins now.

        // keep logging until someone calls stop()
        while (shouldrun)
        {
            // pause before writing the first entry (so we have real numbers)
            // and then pause between entries
            try {
                Thread.sleep(getLogWriteInterval() * 1000);
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

    /**
     * Get the starting time of the crawl (as given by 
     * <code>System.currentTimeMillis()</code> when the crawl started).
     * @return time fo the crawl's start
     */
    public long getCrawlStartTime(){
        return crawlerStartTime;
    }

    /**
     * If crawl has ended it will return the time it ended (given by 
     * <code>System.currentTimeMillis()</code> at that time).
     * <br>
     * If crawl is still going on it will return the same as 
     * <code>System.currentTimeMillis()</code> at the time of the call.
     * @return The time of the crawl ending or the current time if the crawl has
     *         not ended.
     */
    public long getCrawlEndTime()
    {
        if(crawlerEndTime==-1)
        {
            return System.currentTimeMillis();
        }

        return crawlerEndTime;
    }

    /**
     * Returns the number of milliseconds that the crawl spent paused or 
     * otherwise in a nonactive state.
     * @return the number of msec. that the crawl was paused or otherwise
     *         suspended. 
     */
    public long getCrawlTotalPauseTime()
    {
        return crawlerTotalPausedTime;
    }

    /**
     * Get the time when the the crawl was last paused/suspended (as given by 
     * <code>System.currentTimeMillis()</code> at that time). Will be 0 if the
     * crawl is not currently paused.
     * @return time of the crawl's last pause/suspend or 0 if the crawl is not
     *         currently paused. 
     */
    public long getCrawlPauseStartedTime()
    {
        return crawlerPauseStarted;
    }

    /**
     * Total amount of time spent actively crawling so far.<p>
     * Returns the total amount of time (in milliseconds) that has elapsed from
     * the start of the crawl {@link #getCrawlStartTime() getCrawlStartTime()}
     * and until the current time or if the crawl has ended until the the end
     * of the crawl {@link #getCrawlEndTime() getCrawlEndTime()} <b>minus</b> any
     * time spent paused {@link #getCrawlTotalPauseTime() getCrawlTotalPauseTime()}.
     * @return Total amount of time (in msec.) spent crawling so far.
     */
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

    /**
     * The number of seconds to wait between writing snapshot data to log file.
     * @return the number of seconds to wait between writing snapshot data to 
     *         log file.
     */
    protected int getLogWriteInterval() {
        int logInterval;
        try {
            logInterval = ((Integer) getAttribute(null, ATTR_STATS_INTERVAL)).intValue();
        } catch (AttributeNotFoundException e) {
            logInterval = 10;
        }
        return logInterval;
    }
    
    /**
     * @see org.archive.crawler.event.CrawlStatusListener#crawlPausing(java.lang.String)
     */
    public void crawlPausing(String statusMessage) {
        logNote("CRAWL WAITING TO PAUSE");
    }

    private void logNote(String note) {
        controller.progressStats.log(
                    Level.INFO,
                    new PaddingStringBuffer()
                     .append(ArchiveUtils.TIMESTAMP14.format(new Date()))
                     .append(" ")
                     .append(note)
                     .toString()
                );
    }

    /**
     * @see org.archive.crawler.event.CrawlStatusListener#crawlPaused(java.lang.String)
     */
    public void crawlPaused(String statusMessage) {
        crawlerPauseStarted = System.currentTimeMillis();
        logActivity();
        logNote("CRAWL PAUSED");
    }

    /**
     * @see org.archive.crawler.event.CrawlStatusListener#crawlResuming(java.lang.String)
     */
    public void crawlResuming(String statusMessage) {
        if(crawlerPauseStarted>0){
            // Ok, we managed to actually pause before resuming.
            crawlerTotalPausedTime+=(System.currentTimeMillis()-crawlerPauseStarted);
        }
        crawlerPauseStarted = 0;
        logNote("CRAWL RESUMED");
        lastLogPointTime = System.currentTimeMillis();
    }

    /**
     * @see org.archive.crawler.event.CrawlStatusListener#crawlEnding(java.lang.String)
     */
    public void crawlEnding(String sExitMessage) {
        logNote("CRAWL ENDING - " + sExitMessage);
    }


    /**
     * @see org.archive.crawler.event.CrawlStatusListener#crawlEnded(java.lang.String)
     */
    public void crawlEnded(String sExitMessage) {
        crawlerEndTime = System.currentTimeMillis(); //Note the time when the crawl stops.
        logActivity(); //Log end state
        logNote("CRAWL ENDED - " + sExitMessage);
        shouldrun = false;
        controller = null; //Facilitate GC.
    }

    /**
     * @see org.archive.crawler.framework.StatisticsTracking#crawlDuration()
     */
    public long crawlDuration() {
        return getCrawlerTotalElapsedTime();
    }

}
