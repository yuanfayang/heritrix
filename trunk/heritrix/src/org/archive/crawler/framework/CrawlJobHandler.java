/**
 * Defines a framework for a handler that accepts crawl jobs and excecutes them
 * according to it's programming (sequential as in a queue, paralel on multiple crawlers etc.)
 */

package org.archive.crawler.framework;

import java.util.ArrayList;

public interface CrawlJobHandler
{
	//	TODO: Define Jobs. Jobs are refered to as 'Object' for now. Related to that, should jobs have uids?
	
	/**
	 * Submit a job to the handler
	 * 
	 * @param job A new job for the handler
	 */
	
	public void addJob(Object job); 
	
	/**
	 * A list of all pending jobs
	 *  
	 * @return A list of all pending jobs in an ArrayList.  
	 * No promises are made about the order of the list
	 */
	public ArrayList getPendingJobs();
	
	/**
	 * A list of jobs currently being crawled
	 * 
	 * @return A list of all jobs currently being crawled (and having gone through the CJH
	 * framework) as an ArrayList.  
	 */
	public ArrayList getCurrentJobs();
	
	/**
	 * A list of all finished jobs
	 * 
	 * @param limit Return only the 'limit' last. Any negative value or 0 will cause the 
	 * entire list to be returned. 
	 * 
	 * @return A list of all finished jobs.  The list is in the order of completion (latest 
	 * completed first).  No promises are made about how long finished jobs are remembered.
	 */
	public ArrayList getFinishedJobs(int limit);
	
	/**
	 * Cancel a specific job.  
	 * 
	 * @param job The job that is to be cancelled.
	 */
	public void cancelJob(Object job);
}