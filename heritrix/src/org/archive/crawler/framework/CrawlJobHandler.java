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

import java.util.Vector;
import org.archive.crawler.datamodel.CrawlOrder;

/**
 * Defines a framework for a handler that accepts crawl jobs and excecutes them
 * according to it's programming (sequential as in a queue, parallel on multiple crawlers etc.)
 * 
 * @author Kristinn Sigurdsson
 * 
 * @see org.archive.crawler.framework.CrawlJob
 * @see org.archive.crawler.admin.SimpleHandler
 */

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
	 * Return a job with the given UID.  
	 * Doesn't matter if it's pending, currently running or has finished running.
	 * 
	 * @param jobUID The unique ID of the job.
	 * @return The job with the UID or null if no such job is found
	 */
	public CrawlJob getJob(String jobUID);
	
	/**
	 * Remove a specific job that is pending.
	 * 
	 * @param jobUID The UID (unique ID) of the job that is to be removed.
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
	 * Returns a unique job ID.
	 * No two calls to this method (on the same instance of this class) can ever return the same value.
	 * 
	 * @return An unused UID for a job.
	 */
	public String getNextJobUID();
}
