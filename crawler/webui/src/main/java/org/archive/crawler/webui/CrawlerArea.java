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


import javax.management.ObjectName;
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
        request.setAttribute("crawler", Home.getCrawler(request));
        
        Misc.forward(request, response, "page_crawler.jsp");
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
        request.setAttribute("job", job);
        Console.showJobConsole(sc, request, response);
    }
    
    
    public static Remote<CrawlJobManager> open(HttpServletRequest request) {
        Crawler c = Home.getCrawler(request);
        request.setAttribute("crawler", c);
        JMXConnector jmx = c.connect();
        return Remote.make(jmx, c.getObjectName(), CrawlJobManager.class);
    }

    public static void showAbout(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response){
        Crawler crawler = Home.getCrawler(request);
        request.setAttribute("crawler", crawler);
        JMXConnector jmxc = crawler.connect();
        CrawlJobManager cjm = open(request).getObject();
        request.setAttribute("heritrix.version", cjm.getHeritrixVersion());
        
        try{
            // System properites
            request.setAttribute(
                "system.properties", 
                jmxc.getMBeanServerConnection().getAttribute(
                        new ObjectName("java.lang:type=Runtime"), 
                    "SystemProperties"));
        } catch (Exception e){
            e.printStackTrace();
        }

        
        Misc.forward(request, response, "page_about_crawler.jsp");
    }
}
