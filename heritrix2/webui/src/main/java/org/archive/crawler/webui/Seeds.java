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
 * Seeds.java
 *
 * Created on Jun 25, 2007
 *
 * $Id:$
 */

package org.archive.crawler.webui;

import java.io.File;

import javax.management.remote.JMXConnector;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.archive.crawler.framework.CrawlJobManager;
import org.archive.crawler.framework.JobStage;
import org.archive.crawler.scope.SeedModuleInterface;
import org.archive.openmbeans.annotations.BeanProxy;
import org.archive.settings.jmx.JMXSheetManager;

/**
 * @author pjack
 *
 */
public class Seeds {

    final private static int LINES_PER_PAGE = 10000;
    
    final private static int MAX_FILE_SIZE = 32 * 1024;
    
    final private static String SETTINGS_PATH = "root:seeds:seedsfile";
    
    
    public static void showSeeds(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        Crawler crawler = Home.getCrawler(request);
        Remote<JMXSheetManager> remote = Sheets.getSheetManager(request);

        try {
            CrawlJob job = (CrawlJob)request.getAttribute("job");
            JMXConnector jmxc = remote.getJMXConnector();
            JMXSheetManager sheetManager = remote.getObject(); 
            CrawlJobManager crawlJobManager = BeanProxy.proxy(
                    jmxc.getMBeanServerConnection(), 
                    crawler.getObjectName(), 
                    CrawlJobManager.class);
            long size = crawlJobManager.getFileSize(job.encode(), 
                    SETTINGS_PATH);
            request.setAttribute("overflow", size > MAX_FILE_SIZE);

            File f = new File(sheetManager.getFilePath(SETTINGS_PATH));
            String seeds = crawlJobManager.readFile(job.encode(),
                    SETTINGS_PATH,
                    null,
                    0,
                    MAX_FILE_SIZE);
            request.setAttribute("seeds", seeds);
            request.setAttribute("seedfile", f.getAbsolutePath());
            Misc.forward(request, response, 
                "/seeds/page_seed_editor.jsp");
        } finally {
            remote.close();
        }
        
    }

    
    public static void refreshSeeds(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        Crawler crawler = Home.getCrawler(request);
        JMXConnector jmxc = crawler.connect();
        try {
            CrawlJob job = CrawlJob.fromRequest(request, jmxc);
            SeedModuleInterface seedModule = getSeedModule(jmxc, job);
            seedModule.refreshSeeds();
        } finally {
            Misc.close(jmxc);
        }
        
        new Flash("Successfully sent seeds refresh.").addToSession(request);
        CrawlerArea.showCrawler(sc, request, response);
    }
    
    
    public static void saveSeeds(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        Crawler crawler = Home.getCrawler(request);
        Remote<JMXSheetManager> remote = Sheets.getSheetManager(request);
        CrawlJob job = (CrawlJob)request.getAttribute("job");
        JMXConnector jmxc = remote.getJMXConnector();
        try {
            CrawlJobManager crawlJobManager = BeanProxy.proxy(
                    jmxc.getMBeanServerConnection(), 
                    crawler.getObjectName(), 
                    CrawlJobManager.class);
            int page = 0;
            String seeds = request.getParameter("seeds");
            crawlJobManager.writeLines(job.encode(),
                    "root:seeds:seedsfile",
                    null,
                    page * LINES_PER_PAGE,
                    LINES_PER_PAGE,
                    seeds);
            if (job.getJobStage() == JobStage.ACTIVE) {
                SeedModuleInterface seedModule = getSeedModule(jmxc, job);
                seedModule.refreshSeeds();
            }
        } finally {
            remote.close();
        }
        new Flash("Your seeds were updated successfully.").addToSession(request);
        showSeeds(sc, request, response);
    }


    private static SeedModuleInterface getSeedModule(JMXConnector jmxc, 
            CrawlJob job) {
        SeedModuleInterface seedModule = Misc.find(
                jmxc, 
                job.getName(), 
                SeedModuleInterface.class);
        return seedModule;
    }
}
