package org.archive.crawler.framework;
/**
 * @author Kristinn Sigurdsson
 *
 * A CrawlJob encapsulates a CrawlOrder with any and all information and methods needed
 * by a CrawlJobHandler to accept and execute them.
 */

import org.archive.crawler.admin.StatisticsTracker;
import org.archive.crawler.datamodel.CrawlOrder;

public interface CrawlJob
{
	/**
	 * Possible values for Priority
	 */
	public static final int PRIORITY_MINIMAL = 0;
	public static final int PRIORITY_LOW = 1;
	public static final int PRIORITY_AVERAGE = 2;
	public static final int PRIORITY_HIGH = 3;
	public static final int PRIORITY_CRITICAL = 4;

	/**
	 * Possible states for a Job.
	 */
	public static final String STATUS_CREATED = "Created"; // Inital value.  
	public static final String STATUS_PENDING = "Pending"; // Job has been successfully submitted to a CrawlJobHandler 
	public static final String STATUS_RUNNING = "Running"; // Job is being crawled
	public static final String STATUS_DELETED = "Deleted"; // Job was deleted from the CrawlJobHandler before it was crawled.
	public static final String STATUS_ABORTED = "Aborted by user"; //Job was terminted by user input while crawling 
	public static final String STATUS_FINISHED_ABNORMAL = "Abnormal exit from crawling"; //Something went very wrong 
	public static final String STATUS_FINISHED = "Finished"; // Job finished normally having completed it's crawl.
	public static final String STATUS_FINISHED_TIME_LIMIT = "Finished - Timelimit hit"; // Job finished normally when the specified timelimit was hit.
	public static final String STATUS_FINISHED_DATA_LIMIT = "Finished - Maximum amount of data limit hit"; // Job finished normally when the specifed amount of data (MB) had been downloaded
	public static final String STATUS_FINISHED_DOCUMENT_LIMIT = "Finished - Maximum number of documents limit hit"; //Job finished normally when the specified number of documents had been fetched.
	public static final String STATUS_WAITING_FOR_PAUSE = "Pausing - Waiting for threads to finish"; // Job is going to be temporarly stopped after active threads are finished.
	public static final String STATUS_PAUSED = "Paused"; // Job was temporarly stopped. State is kept so it can be resumed
	public static final String STATUS_RESUMED = "Resumed"; // Job has resumed from pause
	
	/**
	 * Each job needs to be assigned a ID.
	 *
	 */
	public String getUID();
	
	/**
	 * 
	 * @param jobname should be equal to the crawl-order xml's name attribute
	 */
	public void setJobName(String jobname);
	
	/**
	 * @return JobName should be equal to the crawl-order xml's name attribute 
	 */
	public String getJobName();
	
	/**
	 * @return this object's CrawlOrder
	 */
	public CrawlOrder getCrawlOrder();
	
	/**
	 * Set the file containing this job's crawl order (a properly formatted XML)
	 * This method respects the ReadOnly property.
	 *  
	 * @param crawlOrderFile The filename with full (relative or absolute) path.
	 * 
	 * @return if isReadOnly() returns true then no action will be taken and this 
	 * method returns false.  Otherwise the relevant change is applied and true is returned.
	 */
	public boolean setCrawlOrder(String crawlOrderFile);
	
	/**
	 * 
	 * @return the filename (with path) of the crawl order file.
	 */
	public String getCrawlOrderFile();
	
	/**
	 * 
	 * @param iPriority The level of priority 
	 *        (see constants defined here beginning with PRIORITY
	 */
	public void setJobPriority(int priority);
	
	/**
	 * 
	 * @return this job's set priority
	 */
	public int getJobPriority();
	
	/**
	 * Called by the CrawlJobHandler when the job is sent to the crawler.  
	 * Once called no changes can be made to the crawl order file.
	 * Typically this is done once a crawl is completed and further changes
	 * to the crawl order are therefor meaningless.
	 */
	public void setReadOnly();  

	/**
	 * 
	 * @return false until setReadOnly has been invoked, after that it returns true.
	 */	
	public boolean isReadOnly();
	
	/**
	 * 
	 * @param status Current status of CrawlJob 
	 * 		  (see constants defined here beginning with STATUS)
	 */
	public void setStatus(String status);
	
	/**
	 * 
	 * @return The current status of this CrawlJob 
	 *         (see constants defined here beginning with STATUS)
	 */
	public String getStatus();
	
	/**
	 * Returns the current version of the order file 
	 * (this should be initially set to 1 and then 
	 * incremented each time setCrawlOrder() is called.
	 * 
	 * @return The version number of the current order file. 
	 */
	public int getOrderVersion();
	
	// TODO: Rewrite this once an interface for a general statistics tracker has been implemented
	public void setStatisticsTracker(StatisticsTracker tracker);
	public StatisticsTracker getStatisticsTracker();
}