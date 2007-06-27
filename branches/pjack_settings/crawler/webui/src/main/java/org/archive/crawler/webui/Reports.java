package org.archive.crawler.webui;


import java.util.TreeSet;

import javax.management.remote.JMXConnector;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.archive.crawler.framework.JobController;
import org.archive.crawler.framework.StatisticsTracker;
import org.archive.crawler.framework.StatisticsTracking;

public class Reports {

    private enum ReportPages{
        OVERVIEW ("page_reports.jsp"),
        CRAWL ("page_crawl_report.jsp"),
        SEEDS ("page_seeds_report.jsp"),
        FRONTIER ("page_frontier_report.jsp"),
        PROCESSORS ("page_processors_report.jsp"),
        THREADS ("page_threads_report.jsp");
        
        String jsp;
        
        ReportPages(String jsp) {
            this.jsp = jsp;
        }
    }
    
    private static void handleReports(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response,
            ReportPages page) {
        // Do generic stuff:
        Crawler crawler = Home.getCrawler(request);
        request.setAttribute("crawler", crawler);
        String job = request.getParameter("job");
        request.setAttribute("job", job);
        request.setAttribute("crawljob", new CrawlJob(job,crawler));
        
        JMXConnector jmxc = crawler.connect();
        // Access to the JobController is 
        
        // Do page specific stuff:
        switch(page){
        case OVERVIEW : 
        case THREADS : 
        case FRONTIER : 
        case PROCESSORS : 
            // Need JobController 
            request.setAttribute("controller", 
                    Misc.find(jmxc, job, JobController.class));
            break;
        case CRAWL : 
            // Need access to remote statistics tracker.
            StatisticsTracking stats = 
                Misc.find(jmxc, job, StatisticsTracking.class);
            request.setAttribute("stats",stats);
            // Build status code report
            String orderStatusCodeBy = "uris";
            if(request.getParameter("statusorder") != null){
                 orderStatusCodeBy = request.getParameter("statusorder");
            }
            request.setAttribute("statusorder", orderStatusCodeBy);
            String[] status_keys = stats.getReportKeys(
                    StatisticsTracker.Reports.STATUSCODE.toString());
            TreeSet<ReportLine> status = 
                new TreeSet<ReportLine>();
            for(String statusKey : status_keys){
                ReportLine scw = new ReportLine();
                scw.legend = statusKey;
                scw.numberOfURIS = stats.getReportValue(
                        StatisticsTracker.Reports.STATUSCODE.toString(), 
                        statusKey);
                if(orderStatusCodeBy.equalsIgnoreCase("uris")){
                    scw.orderby = scw.numberOfURIS;
                }
                status.add(scw);
            }
            request.setAttribute("statuscode", status);
            
            // Build file type report
            String orderFileTypeBy = "uris";
            if(request.getParameter("fileorder") != null){
                orderFileTypeBy = request.getParameter("fileorder");
            }
            request.setAttribute("fileorder", orderFileTypeBy);
            String[] file_keys = stats.getReportKeys(
                    StatisticsTracker.Reports.FILETYPE_URIS.toString());
            TreeSet<ReportLine> filetypes = 
                new TreeSet<ReportLine>();
            for(String fileKey : file_keys){
                ReportLine scw = new ReportLine();
                scw.legend = fileKey;
                scw.numberOfURIS = stats.getReportValue(
                        StatisticsTracker.Reports.FILETYPE_URIS.toString(), 
                        fileKey);
                scw.bytes = stats.getReportValue(
                        StatisticsTracker.Reports.FILETYPE_BYTES.toString(), 
                        fileKey);
                if(orderFileTypeBy.equalsIgnoreCase("uris")){
                    scw.orderby = scw.numberOfURIS;
                } else if(orderFileTypeBy.equalsIgnoreCase("bytes")){
                    scw.orderby = scw.bytes;
                } 
                filetypes.add(scw);
            }
            request.setAttribute("filetypes", filetypes);
            
            
            // Build hosts report
            String orderHostsBy = "uris";
            if(request.getParameter("hostsorder") != null){
                orderHostsBy = request.getParameter("hostsorder");
            }
            request.setAttribute("hostsorder", orderHostsBy);
            String[] hosts_keys = null; 
            if(orderHostsBy.equals("lastactive")){
                hosts_keys = stats.getReportKeys(
                        StatisticsTracker.Reports.HOST_LAST_ACTIVE.toString());
            } else if(orderHostsBy.equals("bytes")){
                hosts_keys = stats.getReportKeys(
                        StatisticsTracker.Reports.HOST_BYTES.toString());
            } else {
                hosts_keys = stats.getReportKeys(
                        StatisticsTracker.Reports.HOST_URIS.toString());
            }
                    
            TreeSet<ReportLine> hosts = 
                new TreeSet<ReportLine>();
            for(String hostKey : hosts_keys){
                ReportLine scw = new ReportLine();
                scw.legend = hostKey;
                scw.numberOfURIS = stats.getReportValue(
                        StatisticsTracker.Reports.HOST_URIS.toString(), 
                        hostKey);
                scw.bytes = stats.getReportValue(
                        StatisticsTracker.Reports.HOST_BYTES.toString(), 
                        hostKey);
                scw.lastActive = stats.getReportValue(
                        StatisticsTracker.Reports.HOST_LAST_ACTIVE.toString(), 
                        hostKey);
                if(orderHostsBy.equalsIgnoreCase("lastactive")){
                    scw.orderby = scw.lastActive;
                } else if(orderHostsBy.equalsIgnoreCase("bytes")){
                    scw.orderby = scw.bytes;
                } else {
                    scw.orderby = scw.numberOfURIS;
                }
                hosts.add(scw);
            }
            request.setAttribute("hosts", hosts);
            
            break;
        case SEEDS :
            // Need access to remote statistics tracker.
            stats = Misc.find(jmxc, job, StatisticsTracking.class);
            request.setAttribute("stats", stats);
            break;
        }
        
        Misc.forward(request, response, page.jsp);
    }
    
    public static void showReports(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) {
        handleReports(sc, request, response, ReportPages.OVERVIEW);
    }

    public static void showCrawlReport(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) {
        handleReports(sc, request, response, ReportPages.CRAWL);
    }
    
    public static void showThreadsReport(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) {
        handleReports(sc, request, response, ReportPages.THREADS);
    }
    
    public static void showFrontierReport(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) {
        handleReports(sc, request, response, ReportPages.FRONTIER);
    }
    
    public static void showProcessorsReport(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) {
        handleReports(sc, request, response, ReportPages.PROCESSORS);
    }
    
    public static void showSeedsReport(
            ServletContext sc,
            HttpServletRequest request,
            HttpServletResponse response) {
        handleReports(sc, request, response, ReportPages.SEEDS);
    }
    
}


