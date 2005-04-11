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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.ReflectionException;

import org.archive.crawler.Heritrix;
import org.archive.crawler.checkpoint.Checkpoint;
import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.framework.StatisticsTracking;
import org.archive.crawler.settings.XMLSettingsHandler;

/**
 * A CrawlJob encapsulates a 'crawl order' with any and all information and
 * methods needed by a CrawlJobHandler to accept and execute them.
 *
 * <p>A given crawl job may also be a 'profile' for a crawl. In that case it
 * should not be executed as a crawl but can be edited and used as a template
 * for creating new CrawlJobs.
 *
 * <p>All of it's constructors are protected since only a CrawlJobHander
 * should construct new CrawlJobs.
 *
 * @author Kristinn Sigurdsson
 *
 * @see org.archive.crawler.admin.CrawlJobHandler#newJob(CrawlJob, String, String, String, int)
 * @see org.archive.crawler.admin.CrawlJobHandler#newProfile(CrawlJob, String, String, String)
 */

public class CrawlJob {
    private static final Logger logger =
        Logger.getLogger(CrawlJob.class.getName());
    /*
     * Possible values for Priority
     */
    /** lowest */
    public static final int PRIORITY_MINIMAL = 0;
    /** low */
    public static final int PRIORITY_LOW = 1;
    /** average */
    public static final int PRIORITY_AVERAGE = 2;
    /** high */
    public static final int PRIORITY_HIGH = 3;
    /** highest */
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
    /** Job was deleted by user, will not be displayed in UI. */
    public static final String STATUS_DELETED = "Deleted";
    /** Job was terminted by user input while crawling */
    public static final String STATUS_ABORTED = "Ended by operator";
    /** Something went very wrong */
    public static final String STATUS_FINISHED_ABNORMAL = "Abnormal exit from crawling";
    /** Job finished normally having completed its crawl. */
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
    
    public static final String STATUS_PREPARING = "Preparing";

    // Class variables
    private String UID;       //A UID issued by the CrawlJobHandler.
    private String name;
    private String status;
    private boolean isReadOnly = false;
    private boolean isNew = true;
    private boolean isProfile = false;
    private boolean isRunning = false;
    private int priority;
    private int numberOfJournalEntries = 0;

    // TODO: Statistics tracker will be saved at end of crawl. We will also want to save it at checkpoints.
    private StatisticsTracking stats;
    private String statisticsFileSave = "";

    private String errorMessage = null;

    private File jobDir = null;

    private CrawlJobErrorHandler errorHandler = null;

    protected XMLSettingsHandler settingsHandler;

    // all discovered on-disk checkpoints for this job
    private Collection checkpoints = null;

    // Checkpoint to resume
    private Checkpoint resumeFrom = null;

    /**
     * A constructor for jobs.
     *
     * <p> Create, ready to crawl, jobs.
     * @param UID A unique ID for this job. Typically emitted by the
     *            CrawlJobHandler.
     * @param name The name of the job
     * @param settingsHandler The associated settings
     * @param errorHandler The crawl jobs settings error handler.
     *           <tt>null</tt> means none is set
     * @param priority job priority.
     * @param dir The directory that is considered this jobs working directory.
     */
    public CrawlJob(String UID, String name, XMLSettingsHandler settingsHandler,
            CrawlJobErrorHandler errorHandler, int priority, File dir) {
        this.UID = UID;
        this.name = name;
        this.settingsHandler = settingsHandler;
        this.priority = priority;
        jobDir = dir;
        this.errorHandler = errorHandler;
    }

    /**
     * A constructor for profiles.
     *
     * <p> Any job created with this constructor will be
     * considered a profile. Profiles are not stored on disk (only their
     * settings files are stored on disk). This is because their data is
     * predictible given any settings files.
     * @param UIDandName A unique ID for this job. For profiles this is the same
     *           as name
     * @param settingsHandler The associated settings
     * @param errorHandler The crawl jobs settings error handler.
     *           <tt>null</tt> means none is set
     */
    protected CrawlJob(String UIDandName, XMLSettingsHandler settingsHandler,
            CrawlJobErrorHandler errorHandler) {
        this.UID = UIDandName;
        this.name = UIDandName;
        this.settingsHandler = settingsHandler;
        isProfile = true;
        isNew = false;
        status = STATUS_PROFILE;
        this.errorHandler = errorHandler;
    }

    /**
     * A constructor for reloading jobs from disk. Jobs (not profiles) have
     * their data written to persistent storage in the file system. This method
     * is used to load the job from such storage. This is done by the
     * <code>CrawlJobHandler</code>.
     * <p>
     * Proper structure of a job file (TODO: Maybe one day make this an XML file)
     * Line 1. UID <br>
     * Line 2. Job name (string) <br>
     * Line 3. Job status (string) <br>
     * Line 4. is job read only (true/false) <br>
     * Line 5. is job running (true/false) <br>
     * Line 6. job priority (int) <br>
     * Line 7. number of journal entries <br>
     * Line 8. setting file (with path) <br>
     * Line 9. statistics tracker file (with path) <br>
     * Line 10-?. error message (String, empty for null), can be many lines <br>
     *
     * @param jobFile
     *            a file containing information about the job to load.
     * @param errorHandler The crawl jobs settings error handler.
     *            null means none is set
     * @throws InvalidJobFileException
     *            if the specified file does not refer to a valid job file.
     * @throws IOException
     *            if io operations fail
     */
    protected CrawlJob(File jobFile, CrawlJobErrorHandler errorHandler)
            throws InvalidJobFileException, IOException {
        this.errorHandler = errorHandler;
        // Open file
        // Read data and set up class variables accordingly...
        BufferedReader jobReader =
            new BufferedReader(new FileReader(jobFile),4096);
        // UID
        UID = jobReader.readLine();
        // name
        name = jobReader.readLine();
        // status
        status = jobReader.readLine();
        if(status.equals(STATUS_ABORTED)==false
                && status.equals(STATUS_CREATED)==false
                && status.equals(STATUS_DELETED)==false
                && status.equals(STATUS_FINISHED)==false
                && status.equals(STATUS_FINISHED_ABNORMAL)==false
                && status.equals(STATUS_FINISHED_DATA_LIMIT)==false
                && status.equals(STATUS_FINISHED_DOCUMENT_LIMIT)==false
                && status.equals(STATUS_FINISHED_TIME_LIMIT)==false
                && status.equals(STATUS_MISCONFIGURED)==false
                && status.equals(STATUS_PAUSED)==false
                && status.equals(STATUS_PENDING)==false
                && status.equals(STATUS_RUNNING)==false
                && status.equals(STATUS_WAITING_FOR_PAUSE)==false
                && status.equals(STATUS_PREPARING)==false){
            // status is invalid. Must be one of the above
            throw new InvalidJobFileException("Status (line 3) in job file " +
                    "is not valid: '" + status + "'");
        }
        // isReadOnly
        String tmp = jobReader.readLine();
        if(tmp.equals("true")){
            isReadOnly = true;
        } else if(tmp.equals("false")){
            isReadOnly = false;
        } else {
            throw new InvalidJobFileException("isReadOnly (line 4) in job" +
                    " file '" + jobFile.getAbsolutePath() + "' is not " +
                    "valid: '" + tmp + "'");
        }
        // isRunning
        tmp = jobReader.readLine();
        if(tmp.equals("true")){
            isRunning = true;
        } else if(tmp.equals("false")){
            isRunning = false;
        } else {
            throw new InvalidJobFileException("isRunning (line 5) in job " +
                    "file '" + jobFile.getAbsolutePath() + "' is not valid: " +
                    "'" + tmp + "'");
        }
        // priority
        tmp = jobReader.readLine();
        try{
            priority = Integer.parseInt(tmp);
        } catch(NumberFormatException e){
            throw new InvalidJobFileException("priority (line 5) in job " +
                    "file '" + jobFile.getAbsolutePath() + "' is not valid: " +
                    "'" + tmp + "'");
        }
        // numberOfJournalEntries
        tmp = jobReader.readLine();
        try{
            numberOfJournalEntries = Integer.parseInt(tmp);
        } catch(NumberFormatException e){
            throw new InvalidJobFileException("numberOfJournalEntries " +
                    "(line 5) in job file '" + jobFile.getAbsolutePath() +
                    "' is not valid: " + "'" + tmp + "'");
        }
        // settingsHandler
        tmp = jobReader.readLine();
        try {
            settingsHandler = new XMLSettingsHandler(new File(tmp));
            if(this.errorHandler != null){
                settingsHandler.registerValueErrorHandler(errorHandler);
            }
            settingsHandler.initialize();
        } catch (InvalidAttributeValueException e1) {
            throw new InvalidJobFileException("Problem reading from settings " +
                    "file (" + tmp + ") specified in job file '" +
                    jobFile.getAbsolutePath() + "'\n" + e1.getMessage());
        }
        // Statistics tracker.
        jobReader.readLine();
        // errorMessage
        // TODO: Multilines
        tmp = jobReader.readLine();
        errorMessage = "";
        while(tmp!=null){
            errorMessage+=tmp+'\n';
            tmp = jobReader.readLine();
        }
        if(errorMessage.length()==0){
            // Empty error message should be null
            errorMessage = null;
        }
        jobDir = jobFile.getParentFile();
        // TODO: Load stattrack if needed.

        // TODO: This should be inside a finally block.
        jobReader.close();
    }

    /**
     * Cause the job to be written to persistent storage.
     * This will also save the statistics tracker if it is not null and the
     * job status is finished (regardless of how it's finished)
     */
    private void writeJobFile() {
        if (isProfile) {
            return;
        }
        
        FileWriter jobWriter = null;
        if (!jobDir.exists() || !jobDir.canWrite()) {
            logger.warning("Can't update status on " +
                jobDir.getAbsolutePath() + " because file does not" +
                " exist (or is unwriteable)");
            return;
        }
        File f = new File(jobDir.getAbsolutePath(), "state.job");
        
        try {
            jobWriter = new FileWriter(f, false);
            try {
                jobWriter.write(UID+"\n");
                jobWriter.write(name+"\n");
                jobWriter.write(status+"\n");
                jobWriter.write(isReadOnly+"\n");
                jobWriter.write(isRunning+"\n");
                jobWriter.write(priority+"\n");
                jobWriter.write(numberOfJournalEntries+"\n");
                jobWriter.write(getSettingsDirectory()+"\n");
                jobWriter.write(statisticsFileSave+"\n");// TODO: Is this right?
                // Can be multiple lines so we keep it last
                jobWriter.write(errorMessage==null?"":errorMessage+"\n");
            } finally {
                jobWriter.close();
            }
        } catch (IOException e) {
            Heritrix.addAlert(new Alert("IOException saving job " + name,
                    "An IOException occured when saving job " +
                    name + " (" + UID + ")",e, Level.WARNING));
        }
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
     * Return the combination of given name and UID most commonly
     * used in administrative interface.
     *
     * @return Job's name with UID notation
     */
    public String getDisplayName() {
        return getJobName()+" ["+getUID()+"]";
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

    /**
     * Set the stat tracking helper object.
     *
     * @param tracker
     */
    public void setStatisticsTracking(StatisticsTracking tracker){
        this.stats = tracker;
    }

    /**
     * @return the stat tracking helper object
     */
    public StatisticsTracking getStatisticsTracking(){
        return stats;
    }

    /**
     * @param settingsHandler
     */
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
     * located.
     *
     * @return the directory where the configuration files for this job are
     *         located
     */
    public String getSettingsDirectory() {
        return settingsHandler.getOrderFile().getPath();
    }

    /**
     * Returns the path of the job's base directory. For profiles this is always
     * equal to <code>new File(getSettingsDirectory())</code>.
     * @return the path of the job's base directory.
     */
    public File getDirectory(){
        return isProfile? new File(getSettingsDirectory()): jobDir;
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

    /**
     * @return Returns the number of journal entries.
     */
    public int getNumberOfJournalEntries() {
        return numberOfJournalEntries;
    }

    /**
     * @param numberOfJournalEntries The number of journal entries to set.
     */
    public void setNumberOfJournalEntries(int numberOfJournalEntries) {
        this.numberOfJournalEntries = numberOfJournalEntries;
        writeJobFile();
    }

    /**
     * @return Returns the error handler for this crawl job
     */
    public CrawlJobErrorHandler getErrorHandler() {
        return errorHandler;
    }

    /**
     * Read all the checkpoints found in the job's checkpoints
     * directory into Checkpoint instances
     */
    public void scanCheckpoints() {
        File checkpointsDirectory = settingsHandler.getOrder().getCheckpointsDirectory();
        File[] perCheckpointDirs = checkpointsDirectory.listFiles();
        checkpoints = new ArrayList();
        for(int i = 0; i < perCheckpointDirs.length; i++) {
            Checkpoint cp = new Checkpoint(perCheckpointDirs[i]);
            checkpoints.add(cp);
        }
    }


    /**
     * @return collection of Checkpoint instances available
     * on disk for this job
     */
    public Collection getCheckpoints() {
        return checkpoints;
    }

    /**
     * @param chkptName Name of chkpoint.
     * @return checkpoint matching the given name
     */
    public Checkpoint getCheckpoint(String chkptName) {
        if(checkpoints==null) {
            scanCheckpoints();
        }
        Iterator iter = checkpoints.iterator();
        while(iter.hasNext()) {
            Checkpoint candidate = (Checkpoint) iter.next();
            if (candidate.getName().equals(chkptName)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * @return the checkpoint to resume from, or null
     */
    public Checkpoint getResumeFromCheckpoint() {
        return resumeFrom;
    }

    /**
     * Update the job's state to reflect that it should
     * be resumed from the given checkpoint.
     *
     * @param cp
     */
    public void configureForResume(Checkpoint cp) {
        // TODO clear old data? reset any values?

        // mark as resume, remember checkpoint
        resumeFrom  = cp;
    }

    /**
     * Returns the absolute path of the specified log.
     * Note: If crawl has not begun, this file may not exist.
     * @param log
     * @return the absolute path for the specified log.
     * @throws AttributeNotFoundException
     * @throws ReflectionException
     * @throws MBeanException
     */
    public String getLogPath(String log) 
    throws AttributeNotFoundException, MBeanException, ReflectionException {
        String logsPath = (String)settingsHandler.getOrder().
            getAttribute(CrawlOrder.ATTR_LOGS_PATH);
        CrawlOrder order = settingsHandler.getOrder();
        String diskPath = (String) order.getAttribute(null,
            CrawlOrder.ATTR_DISK_PATH);
        File disk = settingsHandler.
            getPathRelativeToWorkingDirectory(diskPath);
        File f = new File(logsPath, log);
        if (!f.isAbsolute()) {
            f = new File(disk.getPath(), f.getPath());
        }
        return f.getAbsolutePath();
    }
}
