/* CrawlJob
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

import java.io.File;

import org.archive.crawler.datamodel.settings.XMLSettingsHandler;
import org.archive.crawler.framework.StatisticsTracking;


/**
 * A CrawlJob encapsulates a 'crawl order' with any and all information and
 * methods needed by a CrawlJobHandler to accept and execute them. A given crawl
 * job may also be a 'profile' for a crawl. In that case it should not be executed
 * as a crawl but can be edited and used as a template for creating new CrawlJobs.
 * <p>
 * All of it's constructors are protected since only the CrawlJobHander (or it's
 * subclasses) should construct new CrawlJobs.
 *
 * @author Kristinn Sigurdsson
 *
 * @see org.archive.crawler.admin.CrawlJobHandler#newJob(CrawlJob, String, String, String)
 * @see org.archive.crawler.admin.CrawlJobHandler#newProfile(CrawlJob, String, String, String)
 */

public class CrawlJob
{
    /*
     * Possible values for Priority
     */
    public static final int PRIORITY_MINIMAL = 0;
    public static final int PRIORITY_LOW = 1;
    public static final int PRIORITY_AVERAGE = 2;
    public static final int PRIORITY_HIGH = 3;
    public static final int PRIORITY_CRITICAL = 4;

    /*
     * Possible states for a Job.
     */
    /** Inital value. May not be ready to run/incomplete. */
    public static final String STATUS_CREATED = "Created";
    /** Job has been successfully submitted to a CrawlJobHandler */
    public static final String STATUS_PENDING = "Pending";
    /** Job is being crawled */
    public static final String STATUS_RUNNING = "Running";
    /** Job was deleted from the CrawlJobHandler before it was crawled. */
    public static final String STATUS_DELETED = "Deleted";
    /** Job was terminted by user input while crawling */
    public static final String STATUS_ABORTED = "Aborted by user";
    /** Something went very wrong */
    public static final String STATUS_FINISHED_ABNORMAL = "Abnormal exit from crawling";
    /** Job finished normally having completed it's crawl. */
    public static final String STATUS_FINISHED = "Finished";
    /** Job finished normally when the specified timelimit was hit. */
    public static final String STATUS_FINISHED_TIME_LIMIT = "Finished - Timelimit hit";
    /** Job finished normally when the specifed amount of data (MB) had been downloaded */
    public static final String STATUS_FINISHED_DATA_LIMIT = "Finished - Maximum amount of data limit hit";
    /** Job finished normally when the specified number of documents had been fetched. */
    public static final String STATUS_FINISHED_DOCUMENT_LIMIT = "Finished - Maximum number of documents limit hit";
    /** Job is going to be temporarly stopped after active threads are finished. */
    public static final String STATUS_WAITING_FOR_PAUSE = "Pausing - Waiting for threads to finish";
    /** Job was temporarly stopped. State is kept so it can be resumed */
    public static final String STATUS_PAUSED = "Paused";
    /** Job could not be launced due to an InitializationException */
    public static final String STATUS_MISCONFIGURED = "Could not launch job - Fatal InitializationException";
    /** Job is actually a profile */
    public static final String STATUS_PROFILE = "Profile";

    // Class variables
    private String UID;       //A UID issued by the CrawlJobHandler.
    private String name;
    private String status;
    private boolean isReadOnly = false;
    private boolean isNew = true;
    private boolean isProfile = false;
    private boolean isRunning = false;
    // TODO: Statistics tracker will be saved at end of crawl. We will also want to save it at checkpoints.
    private StatisticsTracking stats;
    private int priority;

    private String errorMessage = null;
    
    private File jobDir = null;

    protected XMLSettingsHandler settingsHandler;

    /**
     * Simple constructor. Settings need to be added seperately before a job 
     * created with this constructor can be submitted to the CrawlJobHandler.
     *
     * @param UID A unique ID for this job. Typically emitted by the CrawlJobHandler.
     * @param dir The directory to store the job file.
     *
     * @see CrawlJobHandler#getNextJobUID()
     */
//    protected CrawlJob(String UID, File dir){
//        this.UID = UID;
//        isReadOnly = false;
//        jobDir = dir;
//    }

    /**
     * A constructor for jobs. Create, ready to crawl, jobs.
     * @param UID A unique ID for this job. Typically emitted by the CrawlJobHandler.
     * @param name The name of the job
     * @param settingsHandler The associated settings
     * @param priority job priority.
     */
    public CrawlJob(String UID, String name, XMLSettingsHandler settingsHandler, int priority, File dir) {
        this.UID = UID;
        this.name = name;
        this.settingsHandler = settingsHandler;
        this.priority = priority;
        jobDir = dir;
    }

    /**
     * A constructor for profiles. Any job created with this constructor will be
     * considered a profile. Profiles are not stored on disk (only their 
     * settings files are stored on disk). This is because their data is 
     * predictible given any settings files.
     * @param UIDandName A unique ID for this job. For profiles this is the same as name
     * @param settingsHandler
     */
    protected  CrawlJob(String UIDandName, XMLSettingsHandler settingsHandler) {
        this.UID = UIDandName;
        this.name = UIDandName;
        this.settingsHandler = settingsHandler;
        isProfile = true;
        isNew = false;
        status = STATUS_PROFILE;
    }
    
    /**
     * A constructor for reloading jobs from disk. Jobs (not profiles) have 
     * their data written to persistent storage in the file system. This method 
     * is used to load the job from such storage. This is done by the 
     * <code>CrawlJobHandler</code>
     * @param jobFile
     */
    protected CrawlJob(File jobFile){
        
    }

    /**
     * Cause the job to be written to persistent storage. This will also
     * save the statistics tracker if it is not null and the job status is
     * finished (regardless of how it's finished)
     */
    private void writeJobFile(){
        
    }
    

    /**
     * Returns this jobs unique ID (UID) that was issued by the CrawlJobHandler()
     * when this job was first created.
     *
     * @return Job This jobs UID.
     * @see CrawlJobHandler#getNextJobUID()
     */
    public String getUID(){
        return UID;
    }

    /**
     * Set the 'name' of this job. The name corrisponds to the 'name' tag in the
     * 'meta' section of the settings file.
     *
     * @param jobname The 'name' of the job.
     */
//Should not be editable
//    protected void setJobName(String jobname){
//        this.name = jobname;
//    }

    /**
     * Returns this job's 'name'. The name comes from the settings for this job,
     * need not be unique and may change. For a unique identifier use
     * {@link #getUID() getUID()}.
     * <p>
     * The name corrisponds to the value of the 'name' tag in the 'meta' section
     * of the settings file.
     *
     * @return This job's 'name'
     */
    public String getJobName(){
        return name;
    }

    /**
     * Set this job's level of priority.
     *
     * @param priority The level of priority
     *
     * @see #getJobPriority()
     * @see #PRIORITY_MINIMAL
     * @see #PRIORITY_LOW
     * @see #PRIORITY_AVERAGE
     * @see #PRIORITY_HIGH
     * @see #PRIORITY_CRITICAL
     */
    // TODO: Either implement the priority or get rid of it.
    public void setJobPriority(int priority){
        this.priority = priority;
    }

    /**
     * Get this job's level of priority.
     *
     * @return this job's priority
     * @see #setJobPriority(int)
     * @see #PRIORITY_MINIMAL
     * @see #PRIORITY_LOW
     * @see #PRIORITY_AVERAGE
     * @see #PRIORITY_HIGH
     * @see #PRIORITY_CRITICAL
     */
    public int getJobPriority(){
        return priority;
    }

    /**
     * Once called no changes can be made to the settings for this job.
     * Typically this is done once a crawl is completed and further changes
     * to the crawl order are therefor meaningless.
     */
    public void setReadOnly(){
        isReadOnly = true;
        writeJobFile(); //Save changes
    }

    /**
     * Is job read only?
     * @return false until setReadOnly has been invoked, after that it returns true.
     */
    public boolean isReadOnly(){
        return isReadOnly;
    }

    /**
     * Set the status of this CrawlJob.
     *
     * @param status Current status of CrawlJob
     *         (see constants defined here beginning with STATUS)
     */
    public void setStatus(String status){
        this.status = status;
        writeJobFile(); //Save changes
        // TODO: If job finished, save StatisticsTracker!
    }

    /**
     * Get the current status of this CrawlJob
     *
     * @return The current status of this CrawlJob
     *         (see constants defined here beginning with STATUS)
     */
    public String getStatus(){
        return status;
    }

    public void setStatisticsTracking(StatisticsTracking tracker){
        this.stats = tracker;
    }

    public StatisticsTracking getStatisticsTracking(){
        return stats;
    }

    public void setSettingsHandler(XMLSettingsHandler settingsHandler){
        this.settingsHandler = settingsHandler;
        // TODO: Is this method needed? Probably not.
    }

    /**
     * Returns the settigns handler for this job. It will have been initialized.
     * @return the settigns handler for this job.
     */
    public XMLSettingsHandler getSettingsHandler(){
        return settingsHandler;
    }
    /**
     * Is this a new job?
     * @return True if is new.
     */
    public boolean isNew() {
        return isNew;
    }

    /**
     * Set if the job is considered to be a profile
     * @return True if is a profile.
     */
    public boolean isProfile() {
        return isProfile;
    }

    /**
     * Set if the job is considered a new job or not.
     * @param b Is the job considered to be new.
     */
    public void setNew(boolean b) {
        isNew = b;
        writeJobFile(); //Save changes
    }

    /**
     * Set's wether or not this job is a profile.
     * @param b True/False
     */
// Can't change a profile into a job directly or vica versa!
//    public void setProfile(boolean b) {
//        isProfile = b;
//        // TODO: Is this method needed? Probably not.
//    }

    /**
     * Returns true if the job is being crawled.
     * @return true if the job is being crawled
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Set if job is being crawled.
     * @param b Is job being crawled.
     */
    public void setRunning(boolean b) {
        isRunning = b;
        writeJobFile(); //Save changes
        //TODO: Job ending -> Save statistics tracker.
        //TODO: This is likely to happen as the CrawlEnding event occurs, need to ensure that the StatisticsTracker is saved to disk on CrawlEnded. Maybe move responsibility for this into the StatisticsTracker? 
    }

    /**
     * Returns the directory where the configuration files for this job are
     * located or null if no settings handler has been set.
     *
     * @return the directory where the configuration files for this job are
     *         located
     */
    // TODO: Split into two methods. getJobDirectory() and getSettingsDirectory()
    public String getDirectory() {
        if(settingsHandler!=null){
            return settingsHandler.getOrderFile().getPath();
        }
        return null;
    }
    
    /**
     * Get the error message associated with this job. Will return null if there
     * is no error message.
     * @return the error message associated with this job
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Set an error message for this job. Generally this only occurs if the job
     * is misconfigured.
     * @param string the error message associated with this job
     */
    public void setErrorMessage(String string) {
        errorMessage = string;
        writeJobFile(); //Save changes
    }

}