/* CrawlJobHandler
 * 
 * $Id$
 * 
 * Copyright (C) 2003 Internet Archive.
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.ReflectionException;

import org.archive.crawler.*;
import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.datamodel.settings.ComplexType;
import org.archive.crawler.datamodel.settings.CrawlerSettings;
import org.archive.crawler.datamodel.settings.SettingsHandler;
import org.archive.crawler.datamodel.settings.XMLSettingsHandler;
import org.archive.crawler.event.CrawlStatusListener;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.exceptions.FatalConfigurationException;
import org.archive.crawler.framework.exceptions.InitializationException;
import org.archive.util.ArchiveUtils;
import org.archive.util.FileUtils;

/**
 * This class manages CrawlJobs. Submitted crawl jobs are queued up and run
 * in order when the crawler is running.<br>
 * Basically this provides a layer between any potential user interface and 
 * the CrawlController and the details of a crawl.
 * <p>
 * The jobs managed by the handler can be divided into the following:
 * <ul>
 *  <li> <code>Pending</code> - Jobs that are ready to run and are waiting their
 *                              turn. These can be edited, viewed, deleted etc.
 *  <li> <code>Running</code> - Only one job can be running at a time. There may
 *                              be no job running. The running job can be viewed 
 *                              and edited to some extent. It can also be 
 *                              terminated. This job should have a 
 *                              StatisticsTracking module attached to it for more
 *                              details on the crawl.
 * <li><code>Completed</code> - Jobs that have finished crawling or have been
 *                              deleted from the pending queue or terminated
 *                              while running. They can not be edited but can be
 *                              viewed. They retain the StatisticsTracking
 *                              module from their run.
 *  <li> <code>New job</code> - At any given time their can be one 'new job' the 
 *                              new job is not considered ready to run. It can
 *                              be edited or discarded (in which case it will be
 *                              totally destroyed, including any files on disk).
 *                              Once an operator deems the job ready to run it 
 *                              can be moved to the pending queue.
 * <li> <code>Profiles</code> - Jobs under profiles are not actual jobs. They can 
 *                              be edited normally but can not be submitted to
 *                              to the pending queue. New jobs can be created 
 *                              using a profile as it's template. 
 * 
 * @author Kristinn Sigurdsson
 * 
 * @see org.archive.crawler.admin.CrawlJob
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
     * Job currently being crawled.
     */
    private CrawlJob currentJob = null;

    /**
     * A new job that is being created/configured. Not yet ready for crawling.
     */
    private CrawlJob newJob = null;

    /**
     * A list of pending CrawlJobs.
     */
    private Vector pendingCrawlJobs = new Vector();

    /**
     * A list of completed CrawlJobs
     */
    private Vector completedCrawlJobs = new Vector();
    
    /**
     * A list of profile CrawlJobs.
     */
    private Vector profileJobs = new Vector();
    // The UIDs of profiles should be NOT be timestamps. A descriptive name is ideal.
    private String defaultProfile = null;

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
        loadProfiles();
    }
    
    /**
     * Returns the directory where profiles are stored. 
     * @return the directory where profiles are stored. 
     */
    private String getProfilesDirectory(){
        return Heritrix.getConfdir().getAbsolutePath() + File.separator +
            "profiles";
    }
    
    /**
     * Loads all profiles found on disk.
     */
    private void loadProfiles() {
        File profileDir = new File(getProfilesDirectory());
        File[] profiles = profileDir.listFiles();
        for (int i = 0; i < profiles.length; i++) {
            if (profiles[i].isDirectory()) {
                // Each directory in the profiles directory should contain the file order.xml.
                File profile =
                    new File(profiles[i].getPath() + File.separator + "order.xml");
                if (profile != null && profile.canRead()) {
                    // Ok, got the order file for this profile.
                    try {
                        // The directory name denotes the profiles UID and name.
                        XMLSettingsHandler newSettingsHandler = new XMLSettingsHandler(profile);
                        newSettingsHandler.initialize();
                        addProfile(new CrawlJob(profiles[i].getName(),newSettingsHandler));
                    } catch (InvalidAttributeValueException e) {
                        System.err.println(
                            "Failed to load profile '"
                                + profiles[i].getName()
                                + "'. InvalidAttributeValueException.");
                    }
                }
            }
        }
        // TODO: set default profile (read from some properties).
    }
    
    /**
     * Add a new profile
     * @param profile The new profile
     */
    public void addProfile(CrawlJob profile){
        profileJobs.add(profile);
    }
    
    /**
     * Returns all known profiles.
     * @return a Vector of all known profiles. 
     */
    public Vector getProfiles(){
        return profileJobs;
    }

    /**
     * Submit a job to the handler. Job will be scheduled for crawling. At present 
     * it will not take the job's* priority into consideration.
     * 
     * @param job A new job for the handler
     */
    public void addJob(CrawlJob job) {
        if(job.isProfile()){
            return;     // Can't crawl profiles. 
        }
        job.setStatus(CrawlJob.STATUS_PENDING);
        if(job.isNew()){
            // Are adding the new job to the pending queue.
            newJob = null;
            job.setNew(false);
        }
        pendingCrawlJobs.add(job);
        if(crawling == false && isRunning())
        {
            // Start crawling
            startNextJob();
        }
    }
    
    /**
     * Returns the default profile. If no default profile has been set it will
     * return the first profile that was set/loaded and still exists. If no 
     * profiles exist it will return null
     * @return the default profile.
     */
    public CrawlJob getDefaultProfile(){
        if(defaultProfile!=null){
            for(int i=0 ; i<profileJobs.size() ; i++){
                CrawlJob item = (CrawlJob)profileJobs.get(i);
                if(item.getJobName().equals(defaultProfile)){
                    // Found it.
                    return item;
                }
            }
        }
        if(profileJobs.size()>0){
            return (CrawlJob)profileJobs.get(0);
        }
        return null;    
    }
    
    public void setDefaultProfile(CrawlJob profile){
        defaultProfile = profile.getJobName();
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
     * Doesn't matter if it's pending, currently running,has finished running is
     * new or a profile.
     * 
     * @param jobUID The unique ID of the job.
     * @return The job with the UID or null if no such job is found
     */
    public CrawlJob getJob(String jobUID) {
        if(jobUID == null){
            return null; //UID can't be null
        }
        // First check currently running job
        if (currentJob != null && currentJob.getUID().equals(jobUID)) {
            return currentJob;
        } else if (newJob != null && newJob.getUID().equals(jobUID)) {
            // Then check the 'new job'
            return newJob;
        } else {
            // Then check pending jobs.
            Iterator itPend = pendingCrawlJobs.iterator();
            while (itPend.hasNext()) {
                CrawlJob cj = (CrawlJob) itPend.next();
                if (cj.getUID().equals(jobUID)) {
                    return cj;
                }
            }

            // The check completed jobs.
            Iterator itComp = completedCrawlJobs.iterator();
            while (itComp.hasNext()) {
                CrawlJob cj = (CrawlJob) itComp.next();
                if (cj.getUID().equals(jobUID)) {
                    return cj;
                }
            }
            
            // And finally check the profiles.
            Iterator itProfile = profileJobs.iterator();
            while (itProfile.hasNext()) {
                CrawlJob cj = (CrawlJob) itProfile.next();
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
     * Creates a new job. The new job will be returned and also registered as the
     * handler's 'new job'. The new job will be based on the settings provided but
     * created in a new location on disk.
     * @param baseOn A CrawlJob (with a valid settingshandler) to use as the 
     *               template for the new job.
     * @param name The name of the new job. 
     * @param description
     * @param seeds
     * @return The new crawl job.
     * @throws FatalConfigurationException
     */
    public CrawlJob newJob(CrawlJob baseOn, String name, String description,
            String seeds)
        throws FatalConfigurationException
    {
        if (newJob !=null){
            //There already is a new job. Discard it.
            discardNewJob();
        }
        String UID = getNextJobUID();
        newJob =
            new CrawlJob(
                UID,
                name,
                makeNew(
                    baseOn,
                    name,
                    description,
                    seeds,
                    getJobdir(name, UID).getAbsolutePath(),
                    "job-"+name+".xml",
                    "seeds-"+name+".txt"),
                CrawlJob.PRIORITY_AVERAGE);
        return newJob;
    }
    
    /**
     * @param name Basename for job.
     * @param UID Job unique ID.
     * @return Job directory for passed job name and UID.
     */
    private File getJobdir(String name, String UID)
    {
        return new File(Heritrix.getJobsdir(), name + "-" + UID);
    }
    
    /**
     * Utility method that will return crawl job's directory output directory.
     * 
     * @param job Job whose directory we want.
     * 
     * @return CrawlJob's output directory.  Result is unpredictable if job
     * was not created by this CrawlJobHandler.
     */
    public File getJobdir(CrawlJob job)
    {
        return getJobdir(job.getJobName(), job.getUID());
    }
    
    /**
     * Creates a new profile. The new profile will be returned and also registered as the
     * handler's 'new job'. The new profile will be based on the settings provided but
     * created in a new location on disk.
     * @param baseOn A CrawlJob (with a valid settingshandler) to use as the 
     *               template for the new profile.
     * @param name The name of the new profile. 
     * @param description Description of the new profile
     * @param seeds The contents of the new profiles' seed file 
     * @return The new profile.
     * @throws FatalConfigurationException
     */
    public CrawlJob newProfile(CrawlJob baseOn, String name, String description, String seeds)
        throws FatalConfigurationException{
    CrawlJob newProfile =
        new CrawlJob(
            name,
            makeNew(
                baseOn,
                name,
                description,
                seeds,
                getProfilesDirectory() + File.separator + name,
                "order.xml",
                "seeds.txt"));
       addProfile(newProfile);
       return newProfile;
    }
    
    /**
     * Creates a new settings handler based on an existing job. Basically all the
     * settings file for the 'based on' will be copied to the specified directory.
     * @param baseOn A CrawlJob (with a valid settingshandler) to use as the 
     *               template for the new profile.
     * @param name Name for the new settings
     * @param description Description of the new settings.
     * @param seeds The contents of the new settings' seed file.
     * @param path The directory where the new settings should be stored.
     * @return The new settings handler.
     * @throws FatalConfigurationException If there are problems with reading the
     *         'base on' configuration, with writing the new configuration or it's
     *         seed file.
     */
    private XMLSettingsHandler makeNew(CrawlJob baseOn, 
                                       String name, 
                                       String description, 
                                       String seeds, 
                                       String path,
                                       String filename,
                                       String seedfile) 
                                       throws FatalConfigurationException{
        XMLSettingsHandler newHandler;

        try {
            newHandler = new XMLSettingsHandler(baseOn.getSettingsHandler().getOrderFile());
            newHandler.initialize();
        } catch (InvalidAttributeValueException e2) {
            throw new FatalConfigurationException("InvalidAttributeValueException occured while creating new settings handler for new job/profile\n" + e2.getMessage());
        }

        // Create filenames etc.
        File newSettingsDir = new File(path);
        newSettingsDir.mkdirs();
        
        try {
            // Set the seed file
            ((ComplexType)newHandler.getOrder().getAttribute("scope")).setAttribute(new Attribute("seedsfile",seedfile));
        } catch (AttributeNotFoundException e1) {
            throw new FatalConfigurationException("AttributeNotFoundException occured while setting seed file for new job/profile\n" + e1.getMessage());
        } catch (InvalidAttributeValueException e1) {
            throw new FatalConfigurationException("InvalidAttributeValueException occured while setting seed file for new job/profile\n" + e1.getMessage());
        } catch (MBeanException e1) {
            throw new FatalConfigurationException("MBeanException occured while setting seed file for new job/profile\n" + e1.getMessage());
        } catch (ReflectionException e1) {
            throw new FatalConfigurationException("ReflectionException occured while setting seed file for new job/profile\n" + e1.getMessage());
        }
        
        File newFile = new File(path,filename);
        
        try {
            newHandler.copySettings(newFile,(String)newHandler.getOrder().getAttribute(CrawlOrder.ATTR_SETTINGS_DIRECTORY));
        } catch (IOException e3) {
            throw new FatalConfigurationException("IOException occured while writing new settings files for new job/profile\n" + e3.getMessage());
        } catch (AttributeNotFoundException e) {
            throw new FatalConfigurationException("AttributeNotFoundException occured while writing new settings files for new job/profile\n" + e.getMessage());
        } catch (MBeanException e) {
            throw new FatalConfigurationException("MBeanException occured while writing new settings files for new job/profile\n" + e.getMessage());
        } catch (ReflectionException e) {
            throw new FatalConfigurationException("ReflectionException occured while writing new settings files for new job/profile\n" + e.getMessage());
        } 
        CrawlerSettings orderfile = newHandler.getSettingsObject(null);
 
        orderfile.setName(name);
        orderfile.setDescription(description);
        
        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new FileWriter(new File(newHandler.getPathRelativeToWorkingDirectory(seedfile))));
            if (writer != null) {
                writer.write(seeds);
                writer.close();
            }
        } catch (IOException e) {
            throw new FatalConfigurationException("IOException occured while writing seed file for new job/profile\n" + e.getMessage());
        }
        return newHandler;
    }

    /**
     * Discard the handler's 'new job'. This will remove any files/directories
     * written to disk.
     */
    public void discardNewJob(){
        FileUtils.deleteDir(new File(newJob.getDirectory()));
    }
    
    /**
     * Get the handler's 'new job'
     * @return the handler's 'new job'
     */
    public CrawlJob getNewJob(){
        return newJob;
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
        try {
            controller.initialize(settingsHandler);
        } catch (InitializationException e) {
            // Can't load current job since it is misconfigured.
            currentJob.setStatus(CrawlJob.STATUS_MISCONFIGURED);
            currentJob.setErrorMessage(
                "A fatal InitializationException occured when loading job:\n"
                    + e.getMessage());
            completedCrawlJobs.add(currentJob);
            currentJob = null;
            controller = null;
            startNextJob(); //Load the next job if there is one.
            return;
        }
        crawling = true;
        controller.startCrawl();
        currentJob.setStatus(CrawlJob.STATUS_RUNNING);
        currentJob.setRunning(true);
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
        currentJob.setReadOnly(); //Further changes have no meaning
        currentJob.setRunning(false);
        completedCrawlJobs.add(currentJob);
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
    
    /**
     * Forward a 'kick' update to current controller if any.
     * @see CrawlController#kickUpdate()
     */
    public void kickUpdate(){
        if(controller!=null){
            controller.kickUpdate();
        }
    }
}
