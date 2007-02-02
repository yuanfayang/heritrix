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

import java.io.Serializable;
import java.util.Date;
import java.util.EventObject;
import java.util.logging.Level;

import org.archive.crawler.event.CrawlStatusListener;
import org.archive.state.Global;
import org.archive.state.Key;
import org.archive.state.Module;
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
public abstract class AbstractTracker 
implements StatisticsTracking, CrawlStatusListener, Serializable, Module {


    /**
     * The interval between writing progress information to log.
     */
    @Global
    final public static Key<Integer> INTERVAL_SECONDS = Key.make(20);


    /** A reference to the CrawlContoller of the crawl that we are to track
     * statistics for.
     */
    final protected transient CrawlController controller;

    // Keep track of time.
    protected long crawlerStartTime;
    protected long crawlerEndTime = -1; // Until crawl ends, this value is -1.
    protected long crawlerPauseStarted = 0;
    protected long crawlerTotalPausedTime = 0;

    /** Timestamp of when this logger last wrote something to the log */
    protected long lastLogPointTime;

    protected boolean shouldrun = true;

    /**
     * Constructor.
     */
    public AbstractTracker(CrawlController controller) {
        this.controller = controller;
        this.controller.addCrawlStatusListener(this);
    }

    
    /**
     * Start thread.  Will call logActivity() at intervals specified by
     * logInterval
     *
     */
    public void run() {
        // Don't start logging if we have no logger
        if (this.controller == null) {
            return;
        }

        shouldrun = true; //If we are starting, this should always be true.

        // Log the legend
        this.controller.logProgressStatistics(progressStatisticsLegend());
        lastLogPointTime = System.currentTimeMillis(); // The first interval begins now.

        // Keep logging until someone calls stop()
        while (shouldrun) {
            // Pause before writing the first entry (so we have real numbers)
            // and then pause between entries
            try {
                Thread.sleep(getLogWriteInterval() * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                controller.runtimeErrors.log(Level.INFO,
                    "Periodic stat logger interrupted while sleeping.");
            }

            // In case stop() was invoked while the thread was sleeping or we
            // are paused.
            if (shouldrun && getCrawlPauseStartedTime() == 0) {
                progressStatisticsEvent(new EventObject(this));
            }
        }
    }

    /**
     * @return legend for progress-statistics lines/log
     */
    public String progressStatisticsLegend() {
        return "           timestamp" +
            "  discovered   " +
            "   queued   downloaded       doc/s(avg)  KB/s(avg) " +
            "  dl-failures   busy-thread   mem-use-KB  heap-size-KB " +
            "  congestion   max-depth   avg-depth";
    }

    /**
     * Notify tracker that crawl has begun. Must be called
     * outside tracker's own thread, to ensure it is noted
     * before other threads start interacting with tracker. 
     */
    public void noteStart() {
        if (this.crawlerStartTime == 0) {
            // Note the time the crawl starts (only if not already set)
            this.crawlerStartTime = System.currentTimeMillis();
        }
    }

    /**
     * A method for logging current crawler state.
     *
     * This method will be called by run() at intervals specified in
     * the crawl order file.  It is also invoked when pausing or
     * stopping a crawl to capture the state at that point.  Default behavior is
     * call to {@link CrawlController#logProgressStatistics} so CrawlController
     * can act on progress statistics event.
     * <p>
     * It is recommended that for implementations of this method it be
     * carefully considered if it should be synchronized in whole or in
     * part
     * @param e Progress statistics event.
     */
    protected synchronized void progressStatisticsEvent(final EventObject e) {
        this.controller.progressStatisticsEvent(e);
    }

    /**
     * Get the starting time of the crawl (as given by
     * <code>System.currentTimeMillis()</code> when the crawl started).
     * @return time fo the crawl's start
     */
    public long getCrawlStartTime() {
        return this.crawlerStartTime;
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
    public long getCrawlEndTime() {
        return (this.crawlerEndTime == -1)?
            System.currentTimeMillis(): this.crawlerEndTime;
    }

    /**
     * Returns the number of milliseconds that the crawl spent paused or
     * otherwise in a nonactive state.
     * @return the number of msec. that the crawl was paused or otherwise
     *         suspended.
     */
    public long getCrawlTotalPauseTime() {
        return this.crawlerTotalPausedTime;
    }

    /**
     * Get the time when the the crawl was last paused/suspended (as given by
     * <code>System.currentTimeMillis()</code> at that time). Will be 0 if the
     * crawl is not currently paused.
     * @return time of the crawl's last pause/suspend or 0 if the crawl is not
     *         currently paused.
     */
    public long getCrawlPauseStartedTime() {
        return this.crawlerPauseStarted;
    }

    public long getCrawlerTotalElapsedTime() {
        if (getCrawlStartTime() == 0) {
            // if no start time set yet, consider elapsed time zero
            return 0;
        }
        
        return (getCrawlPauseStartedTime() != 0)?
            // Are currently paused, calculate time up to last pause
            (getCrawlPauseStartedTime() - getCrawlTotalPauseTime() -
                getCrawlStartTime()):
            // Not paused, calculate total time.
            (getCrawlEndTime() - getCrawlTotalPauseTime() - getCrawlStartTime());
    }

    /**
     * The number of seconds to wait between writing snapshot data to log file.
     * @return the number of seconds to wait between writing snapshot data to
     * log file.
     */
    protected int getLogWriteInterval() {
        return controller.get(this, INTERVAL_SECONDS);
    }

    /**
     * @see org.archive.crawler.event.CrawlStatusListener#crawlPausing(java.lang.String)
     */
    public void crawlPausing(String statusMessage) {
        logNote("CRAWL WAITING - " + statusMessage);
    }

    protected void logNote(final String note) {
        this.controller.logProgressStatistics(new PaddingStringBuffer()
                     .append(ArchiveUtils.TIMESTAMP14.format(new Date()))
                     .append(" ")
                     .append(note)
                     .toString());
    }

    public void crawlPaused(String statusMessage) {
        crawlerPauseStarted = System.currentTimeMillis();
        progressStatisticsEvent(new EventObject(this));
        logNote("CRAWL PAUSED - " + statusMessage);
    }

    public void crawlResuming(String statusMessage) {
        tallyCurrentPause();
        logNote("CRAWL RESUMED - " + statusMessage);
        lastLogPointTime = System.currentTimeMillis();
    }

    /**
     * For a current pause (if any), add paused time to total and reset
     */
    protected void tallyCurrentPause() {
        if (this.crawlerPauseStarted > 0) {
            // Ok, we managed to actually pause before resuming.
            this.crawlerTotalPausedTime
                += (System.currentTimeMillis() - this.crawlerPauseStarted);
        }
        this.crawlerPauseStarted = 0;
    }

    public void crawlEnding(String sExitMessage) {
        logNote("CRAWL ENDING - " + sExitMessage);
    }

    /**
     * @see org.archive.crawler.event.CrawlStatusListener#crawlEnded(java.lang.String)
     */
    public void crawlEnded(String sExitMessage) {
        // Note the time when the crawl stops.
        crawlerEndTime = System.currentTimeMillis();
        progressStatisticsEvent(new EventObject(this));
        logNote("CRAWL ENDED - " + sExitMessage);
        shouldrun = false;
        dumpReports();
        finalCleanup();
    }

    public void crawlStarted(String message) {
        tallyCurrentPause();
        noteStart();
    }
    
    /**
     * Dump reports, if any, on request or at crawl end. 
     */
    protected void dumpReports() {
        // by default do nothing; subclasses may override
    }

    /**
     * Cleanup resources used, at crawl end. 
     */
    protected void finalCleanup() {
        // controller = null; // Facilitate GC.
    }

    /**
     * @see org.archive.crawler.framework.StatisticsTracking#crawlDuration()
     */
    public long crawlDuration() {
        return getCrawlerTotalElapsedTime();
    }
}
