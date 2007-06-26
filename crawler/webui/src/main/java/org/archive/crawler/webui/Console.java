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
 * Jobs.java
 *
 * Created on May 10, 2007
 *
 * $Id:$
 */

package org.archive.crawler.webui;

import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.archive.crawler.framework.JobController;
import org.archive.crawler.framework.StatisticsTracking;


/**
 * @author pjack
 *
 */
public class Console {

    private enum Action{
        START,
        STOP,
        PAUSE,
        RESUME,
        CHECKPOINT;
    }
    
    private Console() {
    }

    private static void consoleAction(ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response,
            Action action) {
        Crawler crawler = Home.getCrawler(request);
        JMXConnector jmxc = crawler.connect();
        String job = request.getParameter("job");
        if (job == null) {
            throw new IllegalStateException("job must not be null");
        }
        request.setAttribute("job", job);
        JobController controller;
        StatisticsTracking stats;
        
        CrawlJob crawlJob = new CrawlJob(job,crawler); 

        try {
            // TODO: Better exception handling here.
            request.setAttribute(
                    "memory", 
                    jmxc.getMBeanServerConnection().getAttribute(
                            new ObjectName("java.lang:type=Memory"), 
                        "HeapMemoryUsage"));

            if(crawlJob.state==CrawlJob.State.ACTIVE){
                controller = Misc.find(jmxc, job, JobController.class);
                stats = Misc.find(jmxc, job, StatisticsTracking.class);
                request.setAttribute("controller", controller);
                request.setAttribute("stats", stats);
    
                
                // Handle action
                if(action!=null){
                    switch(action){
                    case CHECKPOINT : controller.requestCrawlCheckpoint(); break;
                    case PAUSE : controller.requestCrawlPause(); break;
                    case RESUME : controller.requestCrawlResume(); break;
                    case START : controller.requestCrawlStart(); break;
                    case STOP : controller.requestCrawlStop(); break;
                    }
                    // Rebuild the crawljob in case the crawl status has changed
                    // as result of our actions
                    crawlJob = new CrawlJob(job,crawler); 
                }

            }

            request.setAttribute("crawljob", crawlJob);
            
            if(crawlJob.state==CrawlJob.State.ACTIVE){
                Misc.forward(request, response, "/console/page_job_console.jsp");
            } else {
                Misc.forward(request, response, "/console/page_job_console_completed.jsp");
            }

                
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            Misc.close(jmxc);
        }

        
    }

    
    public static void showJobConsole(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) {
        consoleAction(sc, request, response, null);
    }

    
    public static void start(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) {
        consoleAction(sc, request, response, Action.START);
    }

    
    public static void stop(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) {
        consoleAction(sc, request, response, Action.STOP);
    }
    
    
    public static void pause(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) {
        consoleAction(sc, request, response, Action.PAUSE);
    }


    public static void resume(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) {
        consoleAction(sc, request, response, Action.RESUME);
    }
    
    
    public static void checkpoint(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) {
        consoleAction(sc, request, response, Action.CHECKPOINT);
    }
}
