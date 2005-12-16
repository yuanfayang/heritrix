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
    
}
