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

    
    private Console() {
    }

    
    public static void showJobConsole(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) {
        Crawler crawler = Home.getCrawler(request);
        JMXConnector jmxc = crawler.connect();
        String job = getJob(request);
        JobController controller;
        StatisticsTracking stats;
        try {
            controller = Misc.find(jmxc, job, JobController.class);
            stats = Misc.find(jmxc, job, StatisticsTracking.class);
            request.setAttribute("controller", controller);
            request.setAttribute("stats", stats);
            Misc.forward(request, response, "/console/page_job_console.jsp");
        } finally {
            Misc.close(jmxc);
        }
    }

    
    public static void start(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) {
        Crawler crawler = Home.getCrawler(request);
        JMXConnector jmxc = crawler.connect();
        String job = getJob(request);
        JobController controller;
        StatisticsTracking stats;
        try {
            controller = Misc.find(jmxc, job, JobController.class);
            controller.requestCrawlStart();
            stats = Misc.find(jmxc, job, StatisticsTracking.class);
            request.setAttribute("controller", controller);
            request.setAttribute("stats", stats);
            Misc.forward(request, response, "page_job_console.jsp");
        } finally {
            Misc.close(jmxc);
        }
    }

    
    public static void stop(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) {
        Crawler crawler = Home.getCrawler(request);
        JMXConnector jmxc = crawler.connect();
        String job = getJob(request);
        JobController controller;
        StatisticsTracking stats;
        try {
            controller = Misc.find(jmxc, job, JobController.class);
            controller.requestCrawlStop();
            stats = Misc.find(jmxc, job, StatisticsTracking.class);
            request.setAttribute("controller", controller);
            request.setAttribute("stats", stats);
            Misc.forward(request, response, "page_job_console.jsp");
        } finally {
            Misc.close(jmxc);
        }
    }
    
    
    public static void pause(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) {
        Crawler crawler = Home.getCrawler(request);
        String job = getJob(request);
        JMXConnector jmxc = crawler.connect();
        JobController controller;
        StatisticsTracking stats;
        try {
            controller = Misc.find(jmxc, job, JobController.class);
            controller.requestCrawlPause();
            stats = Misc.find(jmxc, job, StatisticsTracking.class);
            request.setAttribute("controller", controller);
            request.setAttribute("stats", stats);
            Misc.forward(request, response, "page_job_console.jsp");
        } finally {
            Misc.close(jmxc);
        }
    }


    public static void resume(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) {
        Crawler crawler = Home.getCrawler(request);
        JMXConnector jmxc = crawler.connect();
        String job = getJob(request);
        JobController controller;
        StatisticsTracking stats;
        try {
            controller = Misc.find(jmxc, job, JobController.class);
            controller.requestCrawlResume();
            stats = Misc.find(jmxc, job, StatisticsTracking.class);
            request.setAttribute("controller", controller);
            request.setAttribute("stats", stats);
            Misc.forward(request, response, "page_job_console.jsp");
        } finally {
            Misc.close(jmxc);
        }
    }
    
    
    public static void checkpoint(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) {
        Crawler crawler = Home.getCrawler(request);
        JMXConnector jmxc = crawler.connect();
        String job = getJob(request);
        JobController controller;
        StatisticsTracking stats;
        try {
            controller = Misc.find(jmxc, job, JobController.class);
            controller.requestCrawlCheckpoint();
            stats = Misc.find(jmxc, job, StatisticsTracking.class);
            request.setAttribute("controller", controller);
            request.setAttribute("stats", stats);
            Misc.forward(request, response, "page_job_console.jsp");
        } finally {
            Misc.close(jmxc);
        }
    }



    private static String getJob(HttpServletRequest request) {
        String job = request.getParameter("job");
        if (job == null) {
            throw new IllegalStateException("job must not be null");
        }
        request.setAttribute("job", job);
        return job;
    }
}
