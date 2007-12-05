/* 
 * Copyright (C) 2007 Internet Archive.
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
 *
 * EngineConfig.java
 *
 * Created on Apr 25, 2007
 *
 * $Id:$
 */

package org.archive.crawler.framework;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;

/**
 * Configuration for EngineImpl.
 * 
 * @author pjack
 */
public class EngineConfig {

    
    private String jobsDirectory = "jobs";
    private MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    private Thread heritrixThread = Thread.currentThread();


    public EngineConfig() {
    }


    public Thread getHeritrixThread() {
        return heritrixThread;
    }

    public void setHeritrixThread(Thread heritrixThread) {
        this.heritrixThread = heritrixThread;
    }

    public String getJobsDirectory() {
        return jobsDirectory;
    }

    public void setJobsDirectory(String jobsDirectory) {
        this.jobsDirectory = jobsDirectory;
    }

    public MBeanServer getServer() {
        return server;
    }

    public void setServer(MBeanServer server) {
        this.server = server;
    }

}
