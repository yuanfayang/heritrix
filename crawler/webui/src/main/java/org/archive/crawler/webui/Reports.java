package org.archive.crawler.webui;

import javax.management.remote.JMXConnector;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.archive.crawler.framework.JobController;
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
            request.setAttribute("stats", 
                    Misc.find(jmxc, job, StatisticsTracking.class));
            break;
        }
        
        Misc.forward(request, response, page.jsp);
        //Misc.close(jmxc);
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
