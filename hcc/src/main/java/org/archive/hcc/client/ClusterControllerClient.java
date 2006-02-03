/* ClusterControllerClient
 * 
 * $Id$
 * 
 * Created on Dec 15, 2005
 *
 * Copyright (C) 2005 Internet Archive.
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
package org.archive.hcc.client;

import java.net.InetSocketAddress;
import java.util.Collection;

/**
 * 
 * @author dbernstein
 *
 */
public interface ClusterControllerClient {
    
    /**
     * Creates a new instance of the crawler.
     * @return
     * @throws InsufficientCrawlingResourcesException If no crawling machines have capicity for another crawler instance.
     * @throws ClusterException
     */
    public Crawler createCrawler()
        throws InsufficientCrawlingResourcesException, ClusterException;
    
    /**
     * Lists all the crawler instances in the cluster.
     * @return
     * @throws ClusterException
     */
    public Collection<Crawler> listCrawlers() throws ClusterException;
    
    
    /**
     * Issues destroy commands to all the crawlers managed by the controller.
     * @throws ClusterException
     */
    public void destroyAllCrawlers() throws ClusterException;
    
    /**
     * Destroys the cluster controller bean which the client is communicating with.
	 * It doesn't actually affect any objects within the cluster - ie containers,
	 * crawlers, and jobs.
     *
     */
    public void destroy();
    
    /**
     * Returns the matching crawler.
     * @param uid A crawl job's id.
     * @param address The remote address of the crawler (ie not the hcc proxied address)
     * @return The crawler or null if the parent cannot be found.
     * @throws ClusterException
     */
    public Crawler findCrawlJobParent(String uid, InetSocketAddress address)
                                            throws ClusterException;
    
    
    /**
     * Returns the current job running on the specified crawler. If the crawler is not
     * found or the crawler is not currently running a job, null will be returned.
     * @param crawler
     * @return
     * @throws ClusterException
     */
    public CurrentCrawlJob getCurrentCrawlJob(Crawler crawler) throws ClusterException;
    /**
     * Adds a crawler lifecycle listener.
     * @param l
     */
    public void addCrawlerLifecycleListener(CrawlerLifecycleListener l);

    /**
     * Removes a crawler lifecycle listener.
     * @param l
     */
    public void removeCrawlerLifecycleListener(CrawlerLifecycleListener l);

    /**
     * Adds a crawl job listener.
     * @param l
     */
    public void addCrawlJobListener(CurrentCrawlJobListener l);

    /**
     * Removes a crawl job listener.
     * @param l
     */
    public void removeCrawlJobListener(CurrentCrawlJobListener l);
    
    /**
     * Returns the maximum number of instances allowed for this container.
     * If the container does not exist, -1 is returned.
     * @param hostname
     * @param port
     * @return
     */
    public int getMaxInstances(String hostname, int port) 
    	throws ClusterException;
    
    /**
     * Sets the maximum number of instances that may run on a 
     * specified container defined by a host and port.
     * @param hostname
     * @param port
     * @param maxInstances
     */
    public void setMaxInstances(String hostname, int port, int maxInstances)
    	throws ClusterException;
   
    
    /**
     * 
     * @return true if pause was successfully invoked on all running jobs.
     * @throws ClusterControllerException
     */
    public boolean pauseAllJobs() 
    	throws ClusterException;
    
    /**
     * 
     * @return true if resume was successfully invoked on all paused or pausing jobs.
     * @throws ClusterControllerException
     */
    public boolean resumeAllPausedJobs() 
    	throws ClusterException;
    

}	
