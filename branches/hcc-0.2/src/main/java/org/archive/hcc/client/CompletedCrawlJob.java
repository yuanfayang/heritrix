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

/**
 * Defines the interface for interacting with a completed crawl job.
 * @author Daniel Bernstein (dbernstein@archive.org)
 */
public interface CompletedCrawlJob extends CrawlJob {
    /**
     * Returns a crawl report which is some statistics summarizing
     * the end state of the crawl.
     * @return
     * @throws ClusterException
     */
    public String getCrawlReport() throws ClusterException;
    /**
     * Returns a seed report which details final crawl status of all the seeds.
     * @return
     * @throws ClusterException
     */

    public String getSeedReport() throws ClusterException;

    /**
     * A count of bytes and docs collected by host.
     * @return
     * @throws ClusterException
     */
    public String getHostReport() throws ClusterException;
   
    /**
     * A count of docs per host per source seed.
     * @return
     * @throws ClusterException
     */
    public String getSourceReport() throws ClusterException;

    /**
     * A count of bytes and docs collected by mime type.
     * @return
     * @throws ClusterException
     */
    
    public String getMimeTypeReport() throws ClusterException;
}
