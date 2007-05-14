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
 * Sheets.java
 *
 * Created on May 9, 2007
 *
 * $Id:$
 */

package org.archive.crawler.webui;

import java.util.Arrays;
import java.util.Set;

import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.remote.JMXConnector;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.archive.crawler.framework.CrawlJobManager;
import org.archive.settings.jmx.JMXSheetManager;

/**
 * @author pjack
 *
 */
public class Sheets {

    
    private Sheets() {
    }
    

    public static void showSheets(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        Remote<JMXSheetManager> remote = getSheetManager(request);
        JMXSheetManager sheetManager = remote.getObject();
        try {
            String[] sheets = sheetManager.getSheets();
            request.setAttribute("sheets", Arrays.asList(sheets));
            Misc.forward(request, response, "page_sheets.jsp");
        } finally {
            remote.close();
        }
    }

    
    public static void showSheetDetail(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        Remote<JMXSheetManager> remote = getSheetManager(request);
        JMXSheetManager sheetManager = remote.getObject();
        String sheet = request.getParameter("sheet");
        request.setAttribute("sheet", sheet);
        try {
            CompositeData[] settings = sheetManager.getAll(sheet);
            request.setAttribute("settings", Arrays.asList(settings));
            Misc.forward(request, response, "page_sheet_detail.jsp");
        } finally {
            remote.close();
        }
    }

    
    
    /**
     * Returns the JMXSheetManager specified in the request.
     * 
     * Needs four request parameters.  The first three are host, port and
     * id; these identify the CrawlJobManager like always.  The last is
     * either a parameter named "job", which indicates we're dealing with
     * an active crawl job, or a parameter named "profile", which indicates
     * we're dealing with an offline profile.
     * 
     * <p>If "profile" is provided, then this method will invoke 
     * openProfile to create the remote JMXSheetManager if it does not 
     * already exist.  Otherwise it will find the remote JMXSheetManager
     * for the specified job name.
     * 
     * @param request   the request containing the parameters described above
     * @return   the remote JMXSheetManager specified in the request
     * @throws Exception   if anything goes wrong
     */
    private static Remote<JMXSheetManager> getSheetManager(
            HttpServletRequest request) throws Exception {
        Crawler crawler = Home.getCrawler(request);
        boolean profile = false;
        String job = request.getParameter("job");
        if (job == null) {
            profile = true;
            job = request.getParameter("profile");
            if (job == null) {
                throw new IllegalStateException("Must specify job or profile.");
            }
            request.setAttribute("profile", job);
        } else {
            request.setAttribute("job", job);
        }
        
        JMXConnector jmxc = crawler.connect();
        String query = "org.archive.crawler:*," + 
            "name=" + job + 
            ",type=" + JMXSheetManager.class.getName();

        Set<ObjectName> set = Misc.find(jmxc, query);
        if (set.size() == 1) {
            ObjectName name = set.iterator().next();
            return Remote.make(jmxc, name, JMXSheetManager.class);
        }

        if (set.size() != 0) {
            throw new IllegalStateException("Expected unique SheetManager for "
                    + query + " but found " + set);
        }
        
        if (!profile) {
            throw new IllegalStateException("Can't open sheets for completed job.");
        }
        
        Remote<CrawlJobManager> manager = Remote.make(
                jmxc, crawler.getObjectName(), CrawlJobManager.class);
        manager.getObject().openProfile(job);
        
        ObjectName name = Misc.waitFor(jmxc, query, true);
        return Remote.make(jmxc, name, JMXSheetManager.class);
    }
    
}
