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


import java.util.Arrays;

import javax.management.remote.JMXConnector;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.archive.crawler.framework.CrawlJobManager;


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
        Remote<CrawlJobManager> remote = open(request);
        CrawlJobManager manager = remote.getObject();
        try {
            String[] profiles = manager.listProfiles();
            Arrays.sort(profiles);
            request.setAttribute("profiles", Arrays.asList(profiles));

            String[] active = manager.listActiveJobs();
            Arrays.sort(active);
            request.setAttribute("active", Arrays.asList(active));

            String[] completed = manager.listCompletedJobs();
            Arrays.sort(completed);
            request.setAttribute("completed", Arrays.asList(completed));
        } finally {
            remote.close();
        }
        
        Misc.forward(request, response, "page_crawler.jsp");
    }
    
    
    public static void showLaunchProfile(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) {
        Crawler c = Home.getCrawler(request);
        request.setAttribute("crawler", c);
        Misc.forward(request, response, "page_launch_profile.jsp");
    }
    
    
    public static void launchProfile(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        String profile = request.getParameter("profile");
        String job = request.getParameter("job");
        Remote<CrawlJobManager> remote = open(request);
        CrawlJobManager manager = remote.getObject();
        try {
            manager.launchProfile(profile, job);
        } finally {
            remote.close();
        }
        showCrawler(sc, request, response);
    }
    
    
    public static Remote<CrawlJobManager> open(HttpServletRequest request) {
        Crawler c = Home.getCrawler(request);
        request.setAttribute("crawler", c);
        JMXConnector jmx = c.connect();
        return Remote.make(jmx, c.getObjectName(), CrawlJobManager.class);
    }

}
