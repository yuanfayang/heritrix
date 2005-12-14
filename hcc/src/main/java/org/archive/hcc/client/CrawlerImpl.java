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

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

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

    public boolean isCrawling() {
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
                            order.getName(), "started via jmx api", "" },
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

    public boolean deleteCompletedCrawlJob(CompletedCrawlJob job) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean deletePendingCrawlJob(PendingCrawlJob job) {
        // TODO Auto-generated method stub
        return false;
    }

    public Collection<CompletedCrawlJob> listCompletedCrawlJobs() {
        // TODO Auto-generated method stub
        return null;
    }

    public Collection<PendingCrawlJob> listPendingCrawlJobs() {
        // TODO Auto-generated method stub
        return null;
    }
}