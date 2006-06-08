/* $Id$
 *
 * (Created on Dec 12, 2005
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
 * @author dbernstein
 * 
 */
public interface Crawler extends
        Proxy {

    /**
     * 
     * 
     */
    public void startPendingJobQueue();

    /**
     * 
     * 
     */
    public void stopPendingJobQueue();

    /**
     * 
     * @return
     */
    public boolean isPendingJobQueueRunning();

    public String addJob(JobOrder order);

    public void terminateCurrentJob();

    /**
     * 
     * @return
     */
    public boolean isCrawling();

    /**
     * 
     * @return
     */
    public String getVersion();

    public void destroy();

    public boolean deletePendingCrawlJob(PendingCrawlJob job);

    public boolean deleteCompletedCrawlJob(CompletedCrawlJob job);

    public Collection<PendingCrawlJob> listPendingCrawlJobs();

    public Collection<CompletedCrawlJob> listCompletedCrawlJobs();

}
