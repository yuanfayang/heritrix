/* 
 * SimpleHandler
 * 
 * $Id$
 * 
 * Copyright (C) 2003 Internet Archive.
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
 */
package org.archive.crawler.admin;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;

import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.event.CrawlStatusListener;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.CrawlJob;
import org.archive.crawler.framework.CrawlJobHandler;
import org.archive.crawler.framework.StatisticsTracking;
import org.archive.crawler.framework.exceptions.InitializationException;
import org.archive.util.ArchiveUtils;
import org.w3c.dom.Node;


/**
 * This class provides control access to the crawler web pages and to other
 * control interfaces.
 * 
 * It allows submission of jobs, changes to configurations and the getting of
 * status information. 
 * <p>
 * Jobs are queued and processed in the order in which they were submitted while
 * the crawler is set to run.  
 * <p>
 * If used with web pages an instance of it should reside in "application scope"
 * to be equally accessible to all page instances.
 * 
 * @author Kristinn Sigurdsson
 */

public class SimpleHandler 
    implements AdminConstants, CrawlJobHandler, CrawlStatusListener
{
    /**
     * Default order file.
     * 
     * Used whenever an order file has not been explicitly specified.
     * 
     * Default is WEBAPP_PATH + ADMIN_WEBAPP_NAME + 'order.xml'.  To
     * override,  supply a system property of 'heritrix.default.orderfile' on
     * the command line (Or supply alternate webapp path w/
     * 'heritrix.webapp.path' system property). The specification of order file
     * may be absolute or relative to the working directory of the executing
     * crawler.
     */
    private String orderFile = null;
    
    /**
     * Default CrawlOrder.
     */
    private CrawlOrder crawlOrder = null;        
    
    /**
     * Job currently being crawled.
     */
    private CrawlJob currentJob = null;       
    
    /**
     * A list of pending CrawlJobs.
     */
    private Vector pendingCrawlJobs = new Vector();
    
    /**
     * A list of completed CrawlJobs
     */
    private Vector completedCrawlJobs = new Vector();
    
    private boolean shouldCrawl = false;
    
    private boolean crawling = false;
    
    /**
     * Reports the success or failure of the last action taken. 
     * 
     * TODO: Reconsider how this is used and if it should stay in at all.
     */
    private String statusMessage = "No actions taken";
    
    /** 
     * A high-priority, one-time alert to show the user 
     */
    private String alertMessage = null;
    
    private CrawlController controller =  null;
    
    private OrderTransformation orderTransform = null;
    
    
    public SimpleHandler()
    {
        // Look to see if a default order file system property has been
        // supplied. If so, use it instead.
        String aOrderFile = System.getProperty(DEFAULT_ORDER_FILE_NAME);
        if (aOrderFile != null) {
                orderFile = aOrderFile;
        }
        else {
            orderFile = SimpleHttpServer.getAdminWebappPath() + 
                DEFAULT_ORDER_FILE;
        }
            
        // Used to convert HTML forms into an XML
        orderTransform = new OrderTransformation();
        try {
            Node orderNode
                = OrderTransformation.readDocumentFromFile(orderFile);
            orderTransform.setNode(orderNode);
            crawlOrder = CrawlOrder.readFromFile(orderFile);
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace(System.out);
        }
    }

    /**
     * @return Returns the default crawlorder
     */
    public CrawlOrder getDefaultCrawlOrder()
    {
        return crawlOrder;
    }
    
    /**
     * Returns the Frontier report.
     * 
     * @return A report of the frontier's status.
     */
    public String getFrontierReport()
    {
        if(controller == null || controller.getFrontier() == null)
        {
            return "Crawler not running";
        }
        else
        {
            return controller.getFrontier().report();
        }
    }
    
    /**
     * Returns the CrawlControllers ToeThreads report
     * @return The CrawlControllers ToeThreads report
     * 
     * @see org.archive.crawler.framework.CrawlController#reportThreads()
     */
    public String getThreadsReport()
    {
        if(controller==null)
        {
            return "Crawler not running";        
        }
        else
        {
            return controller.reportThreads();    
        }
    }
    
    /**
     * Returns the CrawlControllers Processors report
     * @return The CrawlControllers Processors report
     * 
     * @see org.archive.crawler.framework.CrawlController#reportProcessors()
     */
    public String getProcessorsReport()
    {
        if(controller==null)
        {
            return "Crawler not running";        
        }
        else
        {
            return controller.reportProcessors();    
        }
    }
    
    /**
     * @param newJob Job to add.
     * 
     * @see org.archive.crawler.framework.CrawlJobHandler#addJob(org.archive.crawler.framework.CrawlJob)
     */
    public void addJob(CrawlJob newJob)
    {
        newJob.setStatus(CrawlJob.STATUS_PENDING);
        pendingCrawlJobs.add(newJob);
        if(crawling == false && shouldCrawl)
        {
            // Start crawling
            startNextJob();
        }
        
        statusMessage = "New job added " + newJob.getJobName();        
    }
    
    /**
     * @return Vector of pending jobs.
     * @see org.archive.crawler.framework.CrawlJobHandler#getPendingJobs()
     */
    public Vector getPendingJobs()
    {
        return pendingCrawlJobs;
    }
    
    /**
     * @return Vector of current jobs.
     * @see org.archive.crawler.framework.CrawlJobHandler#getCurrentJobs()
     */
    public Vector getCurrentJobs()
    {
        Vector temp = new Vector();
        temp.add(getCurrentJob());
        return temp;        
    }
    
    /**
     * Implemented to return a time stamp.
     * 
     * @return A unique job ID in the form of a timestamp.
     * 
     * @see org.archive.crawler.framework.CrawlJobHandler#getNextJobUID()
     * @see ArchiveUtils#TIMESTAMP17
     */
    public String getNextJobUID()
    {
        return ArchiveUtils.TIMESTAMP17.format(new Date());        
    }
    
    /**
     * Find a crawl job with a matching UID and return it.
     * 
     * @param jobUID to search for.
     * @return Crawljob.
     */
    public CrawlJob getJob(String jobUID) 
    {
        // First check currently running job
        if(currentJob != null && currentJob.getUID().equals(jobUID)) 
        {
            return currentJob;
        }
        else
        {
            // Then check pending jobs.
            Iterator itPend = pendingCrawlJobs.iterator();
            while(itPend.hasNext())
            {
                CrawlJob cj = (CrawlJob)itPend.next();
                if(cj.getUID().equals(jobUID))
                {
                    return cj;
                }
            }

            // Finally check completed jobs.
            Iterator itComp = completedCrawlJobs.iterator();
            while(itComp.hasNext())
            {
                CrawlJob cj = (CrawlJob)itComp.next();
                if(cj.getUID().equals(jobUID))
                {
                    return cj;
                }
            }
        }
        return null; //Nothing found, return null
    }

    /**
     * Since this implementation of the CrawlJobHandler allows for only one job
     * to be running at a time, this method is included for convenience.
     * 
     * @return The currently running job.
     */
    public CrawlJob getCurrentJob()
    {
        return currentJob;
    }

    /**
     * @return Vector of completed jobs.
     * @see org.archive.crawler.framework.CrawlJobHandler#getCompletedJobs()
     */    
    public Vector getCompletedJobs()
    {
        return completedCrawlJobs;
    }
    
    /**
     * Allow jobs to be crawled.
     */
    public void startCrawler()
    {
        shouldCrawl = true;
        if(pendingCrawlJobs.size()>0 && crawling == false)
        {
            // Ok, can just start the next job
            startNextJob(); 
        }
    }
    
    /**
     * Stop future jobs from being crawled.
     * 
     * This action will not affect the current job.
     */
    public void stopCrawler()
    {
        shouldCrawl = false;
    }
    
    /**
     * Start next crawl job.  
     * 
     * If a is job already running this method will do nothing.
     */
    protected void startNextJob()
    {
        if(pendingCrawlJobs.size()==0)
        {
            statusMessage = "No jobs in queue";
            return;
        }
        else if(isCrawling())
        {
            // Already crawling.
            statusMessage = CRAWLER_RUNNING_ERR;
            return;
        }
        
        currentJob = (CrawlJob)pendingCrawlJobs.get(0);
        
        // Create new controller.
        controller = new CrawlController();         
        // Register as listener to get job finished notice.
        controller.addCrawlStatusListener(this);

        try {
            controller.initialize(currentJob.getCrawlOrder());
        } catch (InitializationException e) {
            currentJob = null;
            controller = null;
            shouldCrawl = false;
            setAlertMessage(e.getMessage());
            return;
        } // catch (Exception e) {
        //  e.printStackTrace();
        // }
        pendingCrawlJobs.remove(0);
        controller.startCrawl();
        currentJob.setStatus(CrawlJob.STATUS_RUNNING);
        currentJob.setStatisticsTracking(getStatistics());
        crawling = true;
        statusMessage = CRAWLER_STARTED;
    }

    /**
     * Update the crawl order for the running crawl to match the given filename.
     * 
     * The new crawl order is then loaded into the CrawlController.
     * 
     * @param filename Order file to use updating.
     */    
    public void updateCrawlOrder(String filename)
    {
        currentJob.setCrawlOrder(filename);
        updateCrawlOrder();
    }
    
    /**
     * The specified crawl order for the current job is (re)loaded into the
     * CrawlController.
     * 
     * This will cause changes to it to be put into effect.
     */    
    public void updateCrawlOrder()
    {
        controller.updateOrder(currentJob.getCrawlOrder());
    }
    
    /**
     * Terminate the currently running job.
     * 
     * If no job is running this method will do nothing.  This will NOT stop the
     * crawler, it will procede with the next job unless it's stopped.  Info on
     * success or failure, in user readable form, can be accessed via
     * getStatusMessage().
     */
    public synchronized void terminateJob()
    {
        if(isCrawling()==false)
        {
            //Not crawling
            statusMessage = CRAWLER_NOT_RUNNING_ERR;
        }
        else
        {
            //This will cause crawlEnding to be invoked. It will handle the
            // clean up.
            controller.stopCrawl(); 
            statusMessage = CRAWLER_STOPPED;
            crawling = false;
        }
        try
        {
            // Take a few moments so that the controller can change states
            // before the UI updates. The CrawlEnding event will wake us if it
            // occurs sooner than this.
            wait(3000);        
        }
        catch(InterruptedException e){}
    }
    
    public void pauseJob() {
        controller.pauseCrawl();
        //We'll do this pre-emptively so that the UI can be updated.
        currentJob.setStatus(CrawlJob.STATUS_WAITING_FOR_PAUSE); 
    }
    
    public void resumeJob() {
        controller.resumeCrawl();
    }
    
    /**
     * @return A string informing the user as to the success or failure of the
     * last action performed.
     */
    public String getStatusMessage()
    {
        return statusMessage;
    }
    
    /**
     * @return Whether to crawl or not.
     */
    public boolean shouldcrawl()
    {
        return shouldCrawl;
    }
    
    /**
     * @return Whether crawling or not.
     */
    public boolean isCrawling()
    {
        return crawling;
    }
    
    /**
     * @return Return statistics.
     */
    public StatisticsTracking getStatistics()
    {
        return controller.getStatistics();
    }
    
    /**
     * Set the default crawl order.
     * 
     * @param filename The filename (with path) of the new default crawl order;
     */
    public void setDefaultCrawlOrder(String filename)
    {
        try{
            // Take the new default crawl order and use it to overwrite the old
            // one.
            orderTransform.setNode
                (OrderTransformation.readDocumentFromFile(filename));
            orderTransform.serializeToXMLFile(orderFile); 
            // And then reload the old one (now overwritten)
            crawlOrder = CrawlOrder.readFromFile(orderFile);
        } 
        catch(IOException e){
            e.printStackTrace();    
        }
        catch (InitializationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }    
    }
    
    /**
     * @param req Current request.
     */
    public void updateDefaultCrawlOrder(HttpServletRequest req) 
    {
        createCrawlOrderFile(req, orderFile);    

        setDefaultCrawlOrder(orderFile);
        statusMessage = CRAWL_ORDER_UPDATED;
    }

    /**
     * @param req Containing a form with parameters named for the relevant
     * XPaths in a crawl order and values that are appropriate for every given
     * XPath.  Any XPath not found in the request will have values equal to the
     * default crawl order.
     * 
     * @param filename Filename (including path) for where this new crawl order
     * file is to be created.
     * @see #createCrawlOrderFile(HttpServletRequest, String)
     */
    public void createCrawlOrderFile(HttpServletRequest req, String filename)
    {
        createCrawlOrderFile(req, filename, null,false);
    }
    
    /**
     * Builds a new crawl order based on a form in a HttpServletRequest
     * (Presumably posted from a webpage).
     * 
     * @param req Containing a form with parameters named for the relevant
     * XPaths in a crawl order and values that are appropriate for every given
     * XPath.  Any XPath not found in the request will have values equal to the
     * default crawl order.
     * @param filename Filename (including path) for where this new crawl order
     * file is to be created.
     * @param seedsFileName if not null, this will supercede any seedfile
     * declared in the req. Should NOT include path.  Seed file will be stored
     * in the same path as the crawl order (filename).
     * @param writeSeedsToFile if true the seeds in the req will be written to
     * the seedfile rather then the new crawl order xml.  If false and req
     * contains seeds they will be written to the crawl order xml and the
     * seedfilename (in req or otherwise) will be discarded.
     */
    public void createCrawlOrderFile(HttpServletRequest req, String filename,
        String seedsFileName, boolean writeSeedsToFile)
    {
        Enumeration it = req.getParameterNames();
        String name;
        String seeds = null;

        if(seedsFileName!=null)
        {
            // If seedsFileName provided we will ignore that in the request.
            // Write it now to get it out of the way.
            orderTransform.setNodeValue(XP_SEEDS_FILE,
                    ArchiveUtils.getFilePath(filename)+seedsFileName);
        }

        // Now go through the request and write to the new crawl order.
        while (it.hasMoreElements()) 
        {
            name = it.nextElement().toString();
            String value = req.getParameter(name);
            
            if(name.equals(XP_SEEDS)) // Is this seeds? 
            {
                if(writeSeedsToFile)
                {
                    seeds = value; //This will be written to the seeds file.
                }
                else
                {
                    // Write the seeds to the crawl order XML
                    orderTransform.setNodeValue(name,value);
                }
            }
            else if(name.equals(XP_SEEDS_FILE)) // Is this seed file?
            {
                if(seedsFileName == null)
                {
                    seedsFileName = value;
                    value = ArchiveUtils.getFilePath(filename)+ value;
                    orderTransform.setNodeValue(name, value);
                }
            }
            else if (name != null && value != null)
            {
                // Any other XPath that has a non null value just gets written
                orderTransform.setNodeValue(name, value);
            }
        }
        
        // Write the new crawl order to disk
        orderTransform.serializeToXMLFile(filename);

        // Write the seed file if needed.
        if (seeds != null && seedsFileName != null) {
            try {
                String fn = ArchiveUtils.getFilePath(filename) + seedsFileName;
                BufferedWriter writer = new BufferedWriter(new FileWriter(fn));
                if (writer != null) {
                    writer.write(seeds);
                    writer.close();
                }
            } catch (Exception e) {
                // TODO: handle exception
                e.printStackTrace();
            }
        }
    }

	/**
	 * Returns the filename (with path) of the default order file.
	 * @return the filename (with path) of the default order file.
	 */
	public String getDefaultOrderFileName(){
		return orderFile;
	}

    /**
     * @param jobUID Job to remove.
     * 
     * @return True if we found the job to remove.
     */
    public boolean removeJob(String jobUID) {
        for(int i=0 ; i<pendingCrawlJobs.size() ; i++)
        {
            CrawlJob cj = (CrawlJob)pendingCrawlJobs.get(i);
            if(cj.getUID().equals(jobUID))
            {
                // Found the one to remove.    
                pendingCrawlJobs.remove(i);
                return true;
            }
        }
        return false;
    }

    /**
     * @param statusMessage Message to display.
     * 
     * @see org.archive.crawler.event.CrawlStatusListener#crawlPausing(java.lang.String)
     */
    public void crawlPausing(String statusMessage) {
        currentJob.setStatus(statusMessage);
    }

    /**
     * @param statusMessage Message to display.
     * 
     * @see org.archive.crawler.event.CrawlStatusListener#crawlPaused(java.lang.String)
     */
    public void crawlPaused(String statusMessage) {
        currentJob.setStatus(statusMessage);
    }

    /**
     * @param statusMessage Message to display.
     * 
     * @see org.archive.crawler.event.CrawlStatusListener#crawlResuming(java.lang.String)
     */
    public void crawlResuming(String statusMessage) {
        currentJob.setStatus(statusMessage);
    }

    /**
     * @param sExitMessage Exit message to display.
     * 
     * @see org.archive.crawler.event.CrawlStatusListener#crawlEnding(java.lang.String)
     */
    public void crawlEnding(String sExitMessage)
    {
        crawling = false;
        currentJob.setStatus(sExitMessage);
        completedCrawlJobs.add(currentJob);
        currentJob.setReadOnly(); //Further changes have no meaning
        currentJob = null;
        // Remove the reference so that the old controller can be gc.
        controller = null; 
        if(shouldCrawl)
        {
            startNextJob();        
        }
        
        synchronized(this){
            //If the GUI terminated the job then it is waiting for this event.
            notify();         
        }
    }
    
    /**
     * @param sExitMessage Exit message to display.
     * 
     * @see org.archive.crawler.event.CrawlStatusListener#crawlEnded(java.lang.String)
     */
    public void crawlEnded(String sExitMessage) {
        // Not interested.  Once the Controller tells us that it is ending it's
        // crawl we will simply assume that that was indeed done.
    }
    /**
     * @return Returns the alertMessage.
     */
    public String consumeAlertMessage() {
      String retVal = alertMessage;
      alertMessage = null;
      return retVal;
    }

    /**
     * @param alertMessage The alertMessage to set.
     */
    public void setAlertMessage(String alertMessage) {
      this.alertMessage = alertMessage;
    }

}