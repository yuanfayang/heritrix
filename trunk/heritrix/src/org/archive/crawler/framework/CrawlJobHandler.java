/**
 * @author Kristinn Sigurdsson
 * 
 * Defines a framework for a handler that accepts crawl jobs and excecutes them
 * according to it's programming (sequential as in a queue, paralel on multiple crawlers etc.)
 */

package org.archive.crawler.framework;

import java.util.Vector;

import org.archive.crawler.datamodel.CrawlOrder;

public interface CrawlJobHandler
{

	/**
	 * Submit a job to the handler
	 * 
	 * @param job A new job for the handler
	 */
	
	public void addJob(CrawlJob job); 
	
	/**
	 * A list of all pending jobs
	 *  
	 * @return A list of all pending jobs in an ArrayList.  
	 * No promises are made about the order of the list
	 */
	public Vector getPendingJobs();
	
	/**
	 * A list of jobs currently being crawled
	 * 
	 * @return A list of all jobs currently being crawled (and having gone through the CJH
	 * framework) as an ArrayList.  
	 */
	public Vector getCurrentJobs();
	
	/**
	 * A list of all finished jobs
	 * 
	 * @return A list of all finished jobs as a Vector.
	 */
	public Vector getCompletedJobs();
	
	/**
	 * Remove a specific job that is pending.
	 * 
	 * @param job The job that is to be removed.
	 * 
	 * @return false if the job is no longer in the queue - has begun running or was removed 
	 * by another thread. The job is removed and true is returned otherwise.     
	 */
	public boolean removeJob(String jobUID);
	
	/**
	 * Returns the default crawl order - all new crawl orders are based on this
	 * 
	 * @return the default crawl order
	 */
	public CrawlOrder getDefaultCrawlOrder();
	
	/**
	 * 
	 * @return An unused UID for a job. No two calls to this method (for the same object) can ever return the same value until the integer overflows.
	 */
	public String getNextJobUID();
}