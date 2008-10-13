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
import java.util.LinkedList;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

/**
 * 
 * @author Daniel Bernstein (dbernstein@archive.org)
 *
 */
public class CrawlerImpl extends ProxyBase implements Crawler {
    public void startPendingJobQueue() {
        try {
            this.connection.invoke(
                    this.name,
                    "startCrawling",
                    new Object[0],
                    new String[0]);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
    }

    public void stopPendingJobQueue() {
        try {
            this.connection.invoke(
                    this.name,
                    "stopCrawling",
                    new Object[0],
                    new String[0]);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }

    }

    public CrawlerImpl(ObjectName name, MBeanServerConnection connection) {
        super(name, connection);
    }

    public boolean isCrawling(){
        try {
            return this.connection
                    .getAttribute(this.name, "Status")
                    .toString()
                    .contains("isCrawling=true");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    
    public boolean isPendingJobQueueRunning() {
        try {
            return this.connection
                    .getAttribute(this.name, "Status")
                    .toString()
                    .contains("isRunning=true");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public String getVersion() {
        try {
            return this.connection
                    .getAttribute(this.name, "Version")
                    .toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void destroy() {
        try {
            this.connection.invoke(
                    this.name,
                    "destroy",
                    new Object[0],
                    new String[0]);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public String addJob(JobOrder order) {
        try {
            return (String) this.connection.invoke(
                    this.name,
                    "addJob",
                    new Object[] { order.getJarFile().getAbsolutePath(),
                            order.getName(), order.getDescription(), "" },
                    new String[] { "java.lang.String", "java.lang.String",
                            "java.lang.String", "java.lang.String" });

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

    }

    public void terminateCurrentJob() {
        try {
            this.connection.invoke(
                    this.name,
                    "terminateCurrentJob",
                    new Object[0],
                    new String[0]);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

    }
    

    String getCrawlReport(Long uid) throws ClusterException{
        return getReport("crawl-report", uid);
    }
    
    String getHostsReport(Long uid) throws ClusterException{
        return getReport("hosts-report", uid);
    }
    
    String getSourceReport(Long uid) throws ClusterException{
        return getReport("source-report", uid);
    }

    String getSeedsReport(Long uid) throws ClusterException{
        return getReport("seeds-report",uid);
    }

    String getMimeTypesReport(Long uid) throws ClusterException{
        return getReport("mimetype-report",uid);
    }

    
    /**
     * 
     * @param reportName
     * @param uid
     * @return
     * @throws ClusterException
     */
    String getReport(String reportName, Long uid) throws ClusterException{
        
        try {
            return (String)this.connection.invoke(
                                this.name, 
                                "crawlendReport", 
                                new Object[]{uid.toString(), reportName}, 
                                new String[]{"java.lang.String", "java.lang.String"});
        } catch (Exception e) {
            e.printStackTrace();
            throw new ClusterException(e);
        } 
    }


    public boolean deleteCompletedCrawlJob(CompletedCrawlJob job) throws ClusterException{
        try {
            this.connection.invoke(
                                this.name, 
                                "deleteJob", 
                                new Object[]{job.getUid().toString()}, 
                                new String[]{"java.lang.String"});
                                return true;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ClusterException(e);
        } 
    }

    public boolean deletePendingCrawlJob(PendingCrawlJob job) {
        //TODO implement this 
        throw new UnsupportedOperationException("deletePendingCrawlJob not implemented yet!");
    }

    public Collection<CompletedCrawlJob> listCompletedCrawlJobs() {
        Collection<CompletedCrawlJob> completedJobs = new LinkedList<CompletedCrawlJob>();

        try {
            
            TabularData td = (TabularData)this.connection.invoke(
                    this.name, 
                    "completedJobs", 
                    new Object[0], 
                    new String[0]);
            

            if(td != null){
                for(CompositeData cd: (Collection<CompositeData>)td.values()){
                    
                    CompletedCrawlJobImpl ccj = 
                        new CompletedCrawlJobImpl(
                                new Long((String)cd.get("uid")), 
                                (String)cd.get("name"), 
                                this, 
                                this.connection);
                    
                    completedJobs.add(ccj);
                    
                }
                
            }
            
            
        } catch (Exception e) {
            e.printStackTrace();
        } 
        
        return completedJobs;
    }

    public Collection<PendingCrawlJob> listPendingCrawlJobs() {
        //TODO implement this 
        throw new UnsupportedOperationException("listPendingCrawlJobs not implemented yet!");
    }
}