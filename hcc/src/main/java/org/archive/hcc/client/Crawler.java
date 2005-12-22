/* $Id$
 *
 * Created on Dec 12, 2005
 *
 * Copyright (C) 2005 Internet Archive.
 *  
 * This file is part of the Heritrix Cluster Controller (crawler.archive.org).
 *  
 * HCC is free software; you can redistribute it and/or modify
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
package org.archive.hcc.client;

import java.util.Collection;

/**
 * An interface representing an instance of a Crawler.
 * 
 * @author Daniel Bernstein (dbernstein@archive.org)
 */
public interface Crawler extends Proxy {
    /**
     * Starts the pending job queue. Does nothing 
     * if the queue is already started.
     */
    public void startPendingJobQueue();

    /**
     * Stops the pending job queue. Does nothing if the 
     * queue is already stopped.
     *
     */
    public void stopPendingJobQueue();

    /**
     * Returns true if the pending job queue is running.
     * @return
     */
    public boolean isPendingJobQueueRunning();

    /**
     * Adds a job to the pending job queue. 
     * @param order
     * @return
     */
    public String addJob(JobOrder order);

    /**
     * Terminates the currently running job. Does nothing if 
     * if no job is currently running.
     */
    public void terminateCurrentJob();

    /**
     * Returns true if the crawler has a currently running job.
     * @return
     */
    public boolean isCrawling();

    /**
     * Returns the Heritrix version.
     * @return
     */
    public String getVersion();

    /**
     * Destroys the crawler instance and all dependent objects.
     *
     */
    public void destroy();

    /**
     * Deletes a job from the pending queue.
     * @param job
     * @return
     */
    public boolean deletePendingCrawlJob(PendingCrawlJob job);

    /**
     * Deletes a job from the completed list.
     * @param job
     * @return
     * @throws ClusterException
     */
    public boolean deleteCompletedCrawlJob(CompletedCrawlJob job) throws ClusterException;

    /**
     * Returns a list of pending jobs.
     * @return
     */
    public Collection<PendingCrawlJob> listPendingCrawlJobs();

    /**
     * Returns a list of completed jobs.
     * @return
     */
    public Collection<CompletedCrawlJob> listCompletedCrawlJobs();
}