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
package org.archive.crawler.admin;

import java.util.Date;
import java.util.Iterator;
import java.util.Vector;

import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.datamodel.settings.SettingsHandler;
import org.archive.crawler.event.CrawlStatusListener;
import org.archive.crawler.framework.CrawlController;
import org.archive.util.ArchiveUtils;

/**
 * This class manages CrawlJobs. Submitted crawl jobs are queued up and run
 * in order when the crawler is running.<br>
 * Basically this provides a layer between any potential user interface and 
 * the CrawlController and the details of a crawl.
 * 
 * @author Kristinn Sigurdsson
 * 
 * @see org.archive.crawler.framework.CrawlJob
 */

public class CrawlJobHandler implements CrawlStatusListener {
    /**
     * Default order file.
     * 
     * Used whenever an order file has not been explicitly specified.
     * 
     * Default is WEBAPP_PATH + ADMIN_WEBAPP_NAME + 'order.xml'.  To
     * override,  supply a system property of 'heritrix.default.orderfile' on
     * the command line (Or supply alternate webapp path w/
     * 'heritrix.webapp.path' system property). The specification of order file
     * may be absolute or relative to the working directory of the executing
     * crawler.
     */
    private String settingsFile = null;
    
    /**
     *  Default webapp path.
     */
    public static final String DEFAULT_WEBAPP_PATH = "webapps";
    
    /**
     * Name of system property whose specification overrides
     * DEFAULT_WEBAPP_PATH.
     */
    public static final String WEBAPP_PATH_NAME = "heritrix.webapp.path";

    /**
     * Default name of admin webapp.
     */
    public static final String ADMIN_WEBAPP_NAME = "admin";

    /**
     * Name of system property whose specification overrides default order file
     * used.
     * 
     * Default is WEBAPP_PATH + ADMIN_WEBAPP_NAME 
     * + DEFAULT_ORDER_FILE.  Pass an absolute or relative path.
     */
    public static final String DEFAULT_ORDER_FILE_NAME 
        = "heritrix.default.orderfile";
    
    /**
     * Default order file name.
     */
    public static final String DEFAULT_ORDER_FILE = "order.xml";
    /**
     * Default CrawlOrder.
     */
    private CrawlOrder crawlOrder = null;

    /**
     * Job currently being crawled.
     */
    private CrawlJob currentJob = null;

    /**
     * A list of pending CrawlJobs.
     */
    private Vector pendingCrawlJobs = new Vector();

    /**
     * A list of completed CrawlJobs
     */
    private Vector completedCrawlJobs = new Vector();

    /**
     * If true the crawler is 'running'. That is the next pending job will start
     * crawling as soon as the current job (if any) is completed.
     */
    private boolean running = false;

    /**
     * If true then a job is currently crawling.
     */
    private boolean crawling = false;

    private CrawlController controller = null;

    /**
     * Constructor.
     *
     */
    public CrawlJobHandler(){
        // Look to see if a default order file system property has been
        // supplied. If so, use it instead.
        String aOrderFile = System.getProperty(DEFAULT_ORDER_FILE_NAME);
        if (aOrderFile != null) {
                settingsFile = aOrderFile;
        }
        else {
            settingsFile = SimpleHttpServer.getAdminWebappPath() + DEFAULT_ORDER_FILE;
        }
    }
    
    /**
     * Submit a job to the handler. At present it will not take the job's
     * priority into consideration.
     * 
     * @param job A new job for the handler
     */

    public void addJob(CrawlJob job) {
        job.setStatus(CrawlJob.STATUS_PENDING);
        pendingCrawlJobs.add(job);
    }

    /**
     * A list of all pending jobs
     *  
     * @return A list of all pending jobs in an ArrayList.  
     * No promises are made about the order of the list
     */
    public Vector getPendingJobs() {
        return pendingCrawlJobs;
    }

    /**
     * Get the job that is currently being crawled.
     * 
     * @return The job currently being crawled.   
     */
    public CrawlJob getCurrentJob() {
        return currentJob;
    }

    /**
     * A list of all finished jobs
     * 
     * @return A list of all finished jobs as a Vector.
     */
    public Vector getCompletedJobs() {
        return completedCrawlJobs;
    }

    /**
     * Return a job with the given UID.  
     * Doesn't matter if it's pending, currently running or has finished running.
     * 
     * @param jobUID The unique ID of the job.
     * @return The job with the UID or null if no such job is found
     */
    public CrawlJob getJob(String jobUID) {
        // First check currently running job
        if (currentJob != null && currentJob.getUID().equals(jobUID)) {
            return currentJob;
        } else {
            // Then check pending jobs.
            Iterator itPend = pendingCrawlJobs.iterator();
            while (itPend.hasNext()) {
                CrawlJob cj = (CrawlJob) itPend.next();
                if (cj.getUID().equals(jobUID)) {
                    return cj;
                }
            }

            // Finally check completed jobs.
            Iterator itComp = completedCrawlJobs.iterator();
            while (itComp.hasNext()) {
                CrawlJob cj = (CrawlJob) itComp.next();
                if (cj.getUID().equals(jobUID)) {
                    return cj;
                }
            }
        }
        return null; //Nothing found, return null
    }

    /**
     * The specified job will be removed from the pending queue or aborted if 
     * currently running.  It will be placed in the list of completed jobs with
     * approprite status info. If the job is already in the completed list or
     * no job with the given UID is found, no action will be taken.
     * 
     * @param jobUID The UID (unique ID) of the job that is to be deleted.
     * 
     */
    public void deleteJob(String jobUID) {
        // First check to see if we are deleting the current job.
        if (currentJob != null && jobUID.equals(currentJob.getUID())) {
            // Need to terminate the current job.
            controller.stopCrawl(); // This will cause crawlEnding to be invoked. 
                                    // It will handle the clean up.
            crawling = false;

            synchronized (this) {
                try {
                    // Take a few moments so that the controller can change 
                    // states before the UI updates. The CrawlEnding event 
                    // will wake us if it occurs sooner than this.
                    wait(3000);
                } catch (InterruptedException e) {
                    return;
                }
            }

            return; // We're not going to find another job with the same UID
        }
        // Ok, it isn't the current job, let's check the pending jobs.
        for (int i = 0; i < pendingCrawlJobs.size(); i++) {
            CrawlJob cj = (CrawlJob) pendingCrawlJobs.get(i);
            if (cj.getUID().equals(jobUID)) {
                // Found the one to delete.
                cj.setStatus(CrawlJob.STATUS_DELETED);
                pendingCrawlJobs.remove(i);
                completedCrawlJobs.add(cj);
                return; // We're not going to find another job with the same UID
            }
        }
    }

    /**
     * Cause the current job to pause. If no current job is crawling this method 
     * will have to effect. If the job is already paused it may cause the status
     * of the job to be incorrectly states as 'Waiting to pause'.
     *
     * @see CrawlController#pauseCrawl()
     */
    public void pauseJob() {
        if (controller != null) {
            controller.pauseCrawl();
            //We'll do this pre-emptively so that the UI can be updated.
            currentJob.setStatus(CrawlJob.STATUS_WAITING_FOR_PAUSE);
        }
    }

    /**
     * Cause the current job to resume crawling if it was paused. Will have no 
     * effect if the current job was not paused or if there is no current job.
     * If the current job is still waiting to pause, this will not take effect
     * until the job has actually paused. At which time it will immeditatly 
     * resume crawling.
     */
    public void resumeJob() {
        if (controller != null) {
            controller.resumeCrawl();
        }
    }

    /**
      * Returns the default settings filename. This includes path.
      * 
      * @return the default settings filename
      */
    public String getDefaultSettingsFilename() {
        return settingsFile;
    }

    /**
     * Set the default settings filename. 
     * @param filename The filename of the new default settings file, including path.
     */
    public void setDefaultSettingsFilename(String filename) {
        settingsFile = filename;
    }

    /**
     * Returns a unique job ID.
     * <p>
     * No two calls to this method (on the same instance of this class) can ever 
     * return the same value.<br>
     * Currently implemented to return a time stamp. That is subject to change though.
     * 
     * @return A unique job ID.
     * 
     * @see ArchiveUtils#TIMESTAMP17
     */
    public String getNextJobUID() {
        return ArchiveUtils.TIMESTAMP17.format(new Date());
    }

    /**
     * Is the crawler accepting crawl jobs to run?
     * @return True if the next availible CrawlJob will be crawled. False otherwise.
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Is a crawl job being crawled?
     * @return True if a job is actually being crawled (even if it is paused). 
     *         False if no job is being crawled.
     */
    public boolean isCrawling() {
        return crawling;
    }

    /**
     * Allow jobs to be crawled.
     */
    public void startCrawler() {
        running = true;
        if (pendingCrawlJobs.size() > 0 && crawling == false) {
            // Ok, can just start the next job
            startNextJob();
        }
    }

    /**
     * Stop future jobs from being crawled.
     * 
     * This action will not affect the current job.
     */
    public void stopCrawler() {
        running = false;
    }

    /**
     * Start next crawl job.  
     * 
     * If a is job already running this method will do nothing.
     */
    protected void startNextJob() {
        if (pendingCrawlJobs.size() == 0 || isCrawling()) {
            // No job ready or already crawling.
            return;
        }

        currentJob = (CrawlJob) pendingCrawlJobs.get(0);
        pendingCrawlJobs.remove(0);

        // Create new controller.
        controller = new CrawlController();
        // Register as listener to get job finished notice.
        controller.addCrawlStatusListener(this);

        SettingsHandler settingsHandler = currentJob.getSettingsHandler();
        settingsHandler.initialize();
        try {
            controller.initialize(settingsHandler);
        } catch (Exception e) {
            currentJob.setStatus(CrawlJob.STATUS_MISCONFIGURED);
            completedCrawlJobs.add(currentJob);
            currentJob = null;
            controller = null;
            running = false;
            return;
        }
        crawling = true;
        controller.startCrawl();
        currentJob.setStatus(CrawlJob.STATUS_RUNNING);
        currentJob.setStatisticsTracking(controller.getStatistics());
    }

    /**
     * Returns the Frontier report for the running crawl. If no crawl is running  
     * a message to that effect will be returned instead.
     * 
     * @return A report of the frontier's status.
     */
    public String getFrontierReport() {
        if (controller == null || controller.getFrontier() == null) {
            return "Crawler not running";
        } else {
            return controller.getFrontier().report();
        }
    }

    /**
     * Get the CrawlControllers ToeThreads report for the running crawl. If no 
     * crawl is running a message to that effect will be returned instead.
     * @return The CrawlControllers ToeThreads report
     */
    public String getThreadsReport() {
        if (controller == null) {
            return "Crawler not running";
        } else {
            return controller.reportThreads();
        }
    }

    /**
     * Get the Processors report for the running crawl. If no crawl is running a 
     * message to that effect will be returned instead.
     * @return The Processors report for the running crawl.
     */
    public String getProcessorsReport() {
        if (controller == null) {
            return "Crawler not running";
        } else {
            return controller.reportProcessors();
        }
    }

    /**
     * @param statusMessage Message to display.
     * 
     * @see org.archive.crawler.event.CrawlStatusListener#crawlPausing(java.lang.String)
     */
    public void crawlPausing(String statusMessage) {
        currentJob.setStatus(statusMessage);
    }

    /**
     * @param statusMessage Message to display.
     * 
     * @see org.archive.crawler.event.CrawlStatusListener#crawlPaused(java.lang.String)
     */
    public void crawlPaused(String statusMessage) {
        currentJob.setStatus(statusMessage);
    }

    /**
     * @param statusMessage Message to display.
     * 
     * @see org.archive.crawler.event.CrawlStatusListener#crawlResuming(java.lang.String)
     */
    public void crawlResuming(String statusMessage) {
        currentJob.setStatus(statusMessage);
    }

    /**
     * @param sExitMessage Exit message to display.
     * 
     * @see org.archive.crawler.event.CrawlStatusListener#crawlEnding(java.lang.String)
     */
    public void crawlEnding(String sExitMessage) {
        crawling = false;
        currentJob.setStatus(sExitMessage);
        completedCrawlJobs.add(currentJob);
        currentJob.setReadOnly(); //Further changes have no meaning
        currentJob = null;
        // Remove the reference so that the old controller can be gc.
        controller = null;
        if (running) {
            startNextJob();
        }

        synchronized (this) {
            //If the GUI terminated the job then it is waiting for this event.
            notify();
        }
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.event.CrawlStatusListener#crawlEnded(java.lang.String)
     */
    public void crawlEnded(String sExitMessage) {
        // Not interested in this event.
    }

}
