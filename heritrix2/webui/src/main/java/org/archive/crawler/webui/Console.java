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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.archive.crawler.framework.AlertTracker;
import org.archive.crawler.framework.Engine;
import org.archive.crawler.framework.EngineImpl;
import org.archive.crawler.framework.Frontier;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.JobStage;
import org.archive.crawler.framework.StatisticsTracking;
import org.json.JSONObject;


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
        Remote<Engine> remote = CrawlerArea.open(request);
        try {
            CrawlJob crawlJob = CrawlJob.lookup(request, remote);
            JMXConnector jmxc = remote.getJMXConnector();
            String job = crawlJob.getName();

            CrawlController controller;
            StatisticsTracking stats;

            // TODO: Better exception handling here.
            request.setAttribute(
                    "memory", 
                    jmxc.getMBeanServerConnection().getAttribute(
                            new ObjectName("java.lang:type=Memory"), 
                        "HeapMemoryUsage"));

            if(crawlJob.getJobStage()==JobStage.ACTIVE){
                controller = Misc.find(jmxc, job, CrawlController.class);
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
                    crawlJob = CrawlJob.lookup(request, remote);
                }
            }
            
            if(crawlJob.getJobStage()==JobStage.ACTIVE){
                Misc.forward(request, response, "/console/page_job_console.jsp");
            } else {
                Misc.forward(request, response, "/console/page_job_console_completed.jsp");
            }

                
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            remote.close();
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


    public static void showFrontierImport(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) {
        Remote<Engine> remote = CrawlerArea.open(request);
        Engine cjm = remote.getObject();
        JMXConnector jmxc = remote.getJMXConnector();
        try {
            CrawlJob.fromRequest(request, jmxc);
            String[] all = cjm.listJobs();
            List<CrawlJob> completed = new ArrayList<CrawlJob>();
            for (String s: all) {
                JobStage stage = JobStage.getJobStage(s);
                if (stage == JobStage.COMPLETED) {
                    CrawlJob j = CrawlJob.determineCrawlStatus(jmxc, 
                            JobStage.getJobName(s), stage);
                    completed.add(j);
                }
            }
            request.setAttribute("completedJobs", completed);
            Misc.forward(request, response, "page_frontier_import.jsp");
        } finally {
            remote.close();
        }
    }

    
    public static void frontierImport(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        Remote<Engine> remote = CrawlerArea.open(request);
        Engine cjm = remote.getObject();
        JMXConnector jmxc = remote.getJMXConnector();
        try {
            
            // just package it all for the other side
            HashMap<String,String> params = new HashMap<String,String>();
            @SuppressWarnings("unchecked")
            Enumeration<String> paramEnum = request.getParameterNames();
            while(paramEnum.hasMoreElements()) {
                String key = paramEnum.nextElement();
                Object value = request.getParameter(key);
                if(value instanceof String) {
                    params.put(key, (String)value);
                }
            }
            
            String source = request.getParameter("source");
            if(!source.equals("file")) {
                // use prior job's recovery ;
                String path = cjm.getFilePath(source, EngineImpl.LOGS_DIR_PATH);
                // FIXME: this will break when WUI is on different platform than engine
                path = path + File.separator + "recover.gz";
                params.put("path",path);
            }
            String jsonParams = new JSONObject(params).toString();

            CrawlJob job = CrawlJob.fromRequest(request, jmxc);
            Frontier frontier = Misc.find(jmxc, job.getName(), Frontier.class);
            // TODO: Do this in a background thread, with WUI indicator 
            // of as long as it stays in progress
            
            frontier.importURIs(jsonParams);
            
            // if no error, report success
            // TODO: better handle lengthy imports with progress-report, 
            // async completion report
            new Flash("URIs imported.").addToSession(request);
            // TODO: redirect to proper (reload-friendly) console URL, 
            // rather than show console at frontier_import URL
            showJobConsole(sc, request, response);
        } finally {
            remote.close();
        }
    }
    
    
    public static void showURIList(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) {        
        Remote<Frontier> remote = getFrontier(request);
        URIList list = getURIList(request);
        list.toAttributes(request);
        remote.close();

        Misc.forward(request, response, "page_uri_list.jsp");
    }

    
    public static void updateURIList(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) {        
        Remote<Frontier> remote = getFrontier(request);
        try {
            URIList list = getURIList(request);
            list.fromForm(request);
            list.update(remote.getObject());
            list.toAttributes(request);
        } finally {
            remote.close();
        }

        Misc.forward(request, response, "page_uri_list.jsp");
    }

    
    public static void resetAlerts(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) {
        Crawler c = Home.getCrawler(request);
        JMXConnector jmxc = c.connect();
        try {
            CrawlJob cj = CrawlJob.fromRequest(request, jmxc);
            AlertTracker at = Misc.find(jmxc, cj.getName(), AlertTracker.class);
            at.resetAlertCount();
        } finally {
            Misc.close(jmxc);
        }
        showJobConsole(sc, request, response);
    }

    
    
    public static void showDeleteURIs(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) {
        Remote<Frontier> remote = getFrontier(request);
        request.setAttribute("regex", "");
        remote.close();
        Misc.forward(request, response, "page_delete_uris.jsp");
    }

    
    public static void deleteURIs(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) {
        Remote<Frontier> remote = getFrontier(request);
        long num;
        try {
            String queueRegex = request.getParameter("queueRegex");
            String uriRegex = request.getParameter("uriRegex");
            num = remote.getObject().deleteURIs(queueRegex, uriRegex);
        } finally {
            remote.close();
        }

        Flash f = new Flash("Deleted " + num + " URIs from the Frontier.");
        f.addToSession(request);
        showJobConsole(sc, request, response);
    }


    static URIList getURIList(HttpServletRequest request) {
        CrawlJob job = (CrawlJob)request.getAttribute("job");
        HttpSession session = request.getSession(true);
        @SuppressWarnings("unchecked")
        Map<CrawlJob,URIList> map = (Map)session.getAttribute("uriLists");
        if (map == null) {
            map = new HashMap<CrawlJob,URIList>();
            session.setAttribute("uriLists", map);
        }
        URIList r = map.get(job);
        if (r == null) {
            r = new URIList();
            map.put(job, r);
        }
        return r;
    }
    
    
    static Remote<Frontier> getFrontier(HttpServletRequest request) {
        Crawler c = Home.getCrawler(request);
        JMXConnector jmxc = c.connect();
        try {
            CrawlJob job = CrawlJob.fromRequest(request, jmxc);
            ObjectName fname = Misc.findUnique(jmxc, job.getName(), 
                    Frontier.class);
            return Remote.make(jmxc, fname, Frontier.class);
        } catch (RuntimeException e) {
            Misc.close(jmxc);
            throw e;
        }
    }

}
