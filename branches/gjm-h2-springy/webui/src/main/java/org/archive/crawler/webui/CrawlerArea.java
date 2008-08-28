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
 * CrawlerArea.java
 *
 * Created on May 8, 2007
 *
 * $Id:$
 */

package org.archive.crawler.webui;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.archive.crawler.framework.Engine;
import org.archive.crawler.framework.EngineImpl;
import org.archive.crawler.framework.JobStage;
import org.archive.settings.jmx.JMXSheetManager;


/**
 * @author pjack
 *
 */
public class CrawlerArea {

    
    private CrawlerArea() {
    }
    
    
    public static void showCrawler(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) {
        Remote<Engine> remote = open(request);
        JMXConnector jmxc = remote.getJMXConnector();
        List<CrawlJob> active = new ArrayList<CrawlJob>();
        List<CrawlJob> ready = new ArrayList<CrawlJob>();
        List<CrawlJob> profile = new ArrayList<CrawlJob>();
        List<CrawlJob> completed = new ArrayList<CrawlJob>();
        try {
            String[] jobs = remote.getObject().listJobs();
            for (String job: jobs) {
                JobStage stage = JobStage.getJobStage(job);
                String name = JobStage.getJobName(job);
                switch (stage) {
                case ACTIVE:
                    active.add(CrawlJob.determineCrawlStatus(jmxc, name, stage));
                    break;
                case READY:
                    ready.add(new CrawlJob(name, stage, null));
                    break;
                case PROFILE:
                    profile.add(new CrawlJob(name, stage, null));
                    break;
                case COMPLETED:
                    completed.add(new CrawlJob(name, stage, null));
                    break;
                }
            }            
        } finally {
            remote.close();
        }
        
        request.setAttribute("active", active);
        request.setAttribute("ready", ready);
        request.setAttribute("profiles", profile);
        request.setAttribute("completed", completed);
        String url = request.getContextPath() 
            + "/crawler_area/page_crawler.jsp";
        Misc.forward(request, response, url);
    }
    
    
    public static void showLaunchProfile(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) {
        Home.getCrawler(request);
        String profile = request.getParameter("profile");
        request.setAttribute("profile", profile);
        Misc.forward(request, response, "page_launch_profile.jsp");
    }
    
    
    public static void launch(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        
//        Remote<JMXSheetManager> remoteSheetManager = Sheets.getSheetManager(request);
//        JMXSheetManager sheetManager = remoteSheetManager.getObject();
//        // ensure no uncommitted checked-out sheets before launch
//        String[] checkouts = sheetManager.getCheckedOutSheets();
//        if(checkouts.length>0) {
//            StringBuilder builder = new StringBuilder("Job may not be " +
//                    "launched until the following sheets are committed: <br/>");
//            for(String s: checkouts) {
//                builder.append(s);
//                builder.append("<br/>");
//            }
//            new Flash(Flash.Kind.NACK,builder.toString()).addToSession(request);
//            CrawlerArea.showCrawler(sc, request, response);
//            return;
//        }
//        
//        // ensure no problem (unvalidatable) sheets before launch
//        String[] problems = sheetManager.getProblemSingleSheetNames();
//        if(problems.length>0) {
//            StringBuilder builder = new StringBuilder("Job may not be " +
//                    "launched until the following sheets are corrected: <br/>");
//            for(String s: problems) {
//                builder.append(s);
//                builder.append("<br/>");
//            }
//            new Flash(Flash.Kind.NACK,builder.toString()).addToSession(request);
//            CrawlerArea.showCrawler(sc, request, response);
//            return;
//        }
//        // TODO: check for pending config errors before attempting launch
        
        Remote<Engine> remote = open(request);
        Engine manager = remote.getObject();
        JMXConnector jmxc = remote.getJMXConnector();
        try {
            CrawlJob job = CrawlJob.fromRequest(request, jmxc);
            String jobEnc = JobStage.encode(job.getJobStage(), job.getName());
            manager.launchJob(jobEnc);
        } finally {
            remote.close();
        }
        response.sendRedirect(
                "/console/do_show_job_console.jsp?"+Text.jobQueryString(request));
    }

    
    
    public static Remote<Engine> open(HttpServletRequest request) {
        Crawler c = Home.getCrawler(request);
        request.setAttribute("crawler", c);
        JMXConnector jmx = c.connect();
        return Remote.make(jmx, c.getObjectName(), Engine.class);
    }

    public static void showAbout(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response){
        Crawler crawler = Home.getCrawler(request);
        request.setAttribute("crawler", crawler);
        JMXConnector jmxc = crawler.connect();
        Engine cjm = open(request).getObject();
        request.setAttribute("heritrix.version", cjm.getHeritrixVersion());
        
        try{
            // System properites
            ObjectName runtimeOName = new ObjectName("java.lang:type=Runtime");
            request.setAttribute(
                "system.properties", 
                jmxc.getMBeanServerConnection().getAttribute(
                        runtimeOName, 
                        "SystemProperties"));
            // Uptime
            request.setAttribute(
                    "runtime.uptime", 
                    jmxc.getMBeanServerConnection().getAttribute(
                            runtimeOName, 
                            "Uptime"));
            // Start time
            request.setAttribute(
                    "runtime.starttime", 
                    jmxc.getMBeanServerConnection().getAttribute(
                            runtimeOName, 
                            "StartTime"));
            // Input arguments
            request.setAttribute(
                    "runtime.inputarguments", 
                    jmxc.getMBeanServerConnection().getAttribute(
                            runtimeOName, 
                            "InputArguments"));
            
            // Operating system
            ObjectName osOName = 
                new ObjectName("java.lang:type=OperatingSystem");
            request.setAttribute(
                    "os.availableprocessors", 
                    jmxc.getMBeanServerConnection().getAttribute(
                            osOName, "AvailableProcessors"));
        } catch (Exception e){
            e.printStackTrace();
        }

        
        Misc.forward(request, response, "page_about_crawler.jsp");
    }


    public static void showCopy(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response){
        Crawler c = Home.getCrawler(request);
        JMXConnector jmxc = c.connect();
        try {
            CrawlJob cj = CrawlJob.fromRequest(request, jmxc);
            String defaultName = getCopyDefaultName(cj);
            request.setAttribute("defaultName", defaultName);
        } finally {
            Misc.close(jmxc);
        }
        
        Misc.forward(request, response, "page_copy.jsp");
    }
    
    
    private static String getCopyDefaultName(CrawlJob cj) {
        String name = cj.getName();
        return EngineImpl.getCopyDefaultName(name);
    }


    public static void copy(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response){
        Remote<Engine> remote = open(request);
        Engine cjm = remote.getObject();
        JMXConnector jmxc = remote.getJMXConnector();
        try {
            CrawlJob job = CrawlJob.fromRequest(request, jmxc);
            String newStageString = request.getParameter("newStage");
            JobStage newStage = JobStage.valueOf(newStageString);
            String newName = request.getParameter("newName");
            
            String oldJobEnc = JobStage.encode(job.getJobStage(), job.getName());
            String newJobEnc = JobStage.encode(newStage, newName);
            try {
                cjm.copy(oldJobEnc, newJobEnc);
            } catch (IOException e) {
                String msg = "IO error during copy: " + e.getMessage();                
                request.setAttribute("error", msg);
                showCopy(sc, request, response);
                return;
            } catch (IllegalArgumentException e) {
                request.setAttribute("error", e.getMessage());
                showCopy(sc, request, response);
                return;
            }
        } finally {
            remote.close();
        }
        
        showCrawler(sc, request, response);
    }

    
    public static void showRecover(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) {
        Remote<Engine> remote = open(request);
        Engine cjm = remote.getObject();
        JMXConnector jmxc = remote.getJMXConnector();
        try {
            CrawlJob job = CrawlJob.fromRequest(request, jmxc);
            String[] checkpoints = cjm.listCheckpoints(job.encode());
            Arrays.sort(checkpoints, Collections.reverseOrder());
            String defaultName = getCopyDefaultName(job);
            request.setAttribute("checkpoints", Arrays.asList(checkpoints));
            request.setAttribute("defaultName", defaultName);
            Misc.forward(request, response, "page_recover.jsp");
        } finally {
            remote.close();
        }
    }

    
    public static void recover(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) {
        Remote<Engine> remote = open(request);
        Engine cjm = remote.getObject();
        JMXConnector jmxc = remote.getJMXConnector();
        try {
            CrawlJob job = CrawlJob.fromRequest(request, jmxc);
            String recoverJob = JobStage.encode(JobStage.ACTIVE, 
                    request.getParameter("newName"));
            String checkpoint = request.getParameter("checkpoint");
            cjm.recoverCheckpoint(job.encode(), recoverJob, checkpoint, 
                    new String[0], new String[0]);
            
            String[] checkpoints = cjm.listCheckpoints(job.encode());
            request.setAttribute("checkpoints", Arrays.asList(checkpoints));
            showCrawler(sc, request, response);
        } finally {
            remote.close();
        }
    }

    
    public static void showDelete(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) {
        Remote<Engine> remote = open(request);
        JMXConnector jmxc = remote.getJMXConnector();
        try {
            CrawlJob.fromRequest(request, jmxc);
        } finally {
            remote.close();
        }
        Misc.forward(request, response, "page_delete.jsp");
    }

    
    public static void delete(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) {
        Remote<Engine> remote = open(request);
        Engine cjm = remote.getObject();
        JMXConnector jmxc = remote.getJMXConnector();
        try {
            CrawlJob job = CrawlJob.fromRequest(request, jmxc);
            cjm.deleteJob(job.encode());
        } finally {
            remote.close();
        }
        showCrawler(sc, request, response);
    }


}
