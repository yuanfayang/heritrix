/* Copyright (C) 2003 Internet Archive.
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
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;

import org.archive.crawler.basic.Frontier;
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
 * This class provides control access to the crawler to the web pages or other control interfaces.
 * It allows them to submit jobs, changed configurations and get status information. 
 * <p>
 * Jobs are queued and processed in the order they are submitted in while the crawler is
 * set to run.  
 * <p>
 * If used with web pages an instance of it should reside in "application scope" to be equally 
 * accessible to all page instances.
 * 
 * @author Kristinn Sigurdsson
 */

public class SimpleHandler implements AdminConstants, CrawlJobHandler, CrawlStatusListener
{
	private String orderFile;			// Default order file (order.xml by default)
	private CrawlOrder crawlOrder;		// Default CrawlOrder. (matches order.xml) 
	
	//Crawl Jobs	
	private CrawlJob currentJob;		// Job currently being crawled
	private Vector pendingCrawlJobs;	// A list of pending CrawlJobs
	private Vector completedCrawlJobs;	// A list of completed CrawlJobs
	
	private boolean shouldcrawl;
	
	private boolean crawling = false;
	private String statusMessage = "No actions taken"; //Reports the success or failure of the last action taken. TODO: Reconsider how this is used and if it should stay in at all.

	private CrawlController controller;
	private OrderTransformation orderTransform;
	
	public SimpleHandler()
	{
		shouldcrawl = false;
		pendingCrawlJobs = new Vector();
		completedCrawlJobs = new Vector();
		
		// Default order file;
		orderFile = DEFAULT_ORDER_FILE;

		String aOrderFile = System.getProperty("OrderFile");
		if (aOrderFile != null) {
			File f = new File(aOrderFile);
			if (!f.isAbsolute()) {
				orderFile =
					System.getProperty("user.dir")
						+ File.separator
						+ aOrderFile;
			} else {
				orderFile = aOrderFile;
			}
		}

		orderTransform = new OrderTransformation(); // Used to convert HTML forms into an XML

		try {
			Node orderNode = OrderTransformation.readDocumentFromFile(WEBAPP_PATH+orderFile);
			orderTransform.setNode(orderNode);

			loadCrawlOrder(); // Finally load the crawlorder to memory 
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace(System.out);
		}
	}
	
	public CrawlOrder getDefaultCrawlOrder()
	{
		return crawlOrder;
	}
	
	/** 
	 * Loads the selected order file (as specified by the orderFile attribute)
	 * as a crawlOrder. 
	 */
	private void loadCrawlOrder() throws InitializationException
	{ 
		crawlOrder = CrawlOrder.readFromFile(WEBAPP_PATH+orderFile);
	}
	
	/**
	 * Returns the Frontier report..
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
			// TODO: Consider making Frontier.report() method a part of the URIFrontier interface.
			return ((Frontier)controller.getFrontier()).report();
		}
	}
	
	/**
	 * Returns the CrawlControllers ToeThreads report.
	 * 
	 * @return The CrawlControllers ToeThreads report
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
	
	/*
	 *  (non-Javadoc)
	 * @see org.archive.crawler.framework.CrawlJobHandler#addJob(org.archive.crawler.framework.CrawlJob)
	 */
	public void addJob(CrawlJob newJob)
	{
		newJob.setStatus(CrawlJob.STATUS_PENDING);
		pendingCrawlJobs.add(newJob);
		
		if(crawling == false && shouldcrawl)
		{
			// Start crawling
			startNextJob();
		}
		
		statusMessage = "New job added " + newJob.getJobName();		
	}
	
	/*
	 *  (non-Javadoc)
	 * @see org.archive.crawler.framework.CrawlJobHandler#getPendingJobs()
	 */
	public Vector getPendingJobs()
	{
		return pendingCrawlJobs;
	}
	
	/*
	 *  (non-Javadoc)
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
	
	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.CrawlJobHandler#getJob(int)
	 */
	public CrawlJob getJob(String jobUID) 
	{
		// Find a crawl job with a matching UID and return it.
		
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

	/*
	 *  (non-Javadoc)
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
		shouldcrawl = true;
		if(pendingCrawlJobs.size()>0 && crawling == false)
		{
			// Ok, can just start the next job
			startNextJob(); 
		}
	}
	
	/**
	 * Stop future jobs from being crawled.
	 * This action will not affect the current job.
	 */
	public void stopCrawler()
	{
		shouldcrawl = false;
	}
	
	/**
	 * Start next crawl job.  
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
		pendingCrawlJobs.remove(0);
		
		controller = new CrawlController(); 		//Create new controller.
		controller.addCrawlStatusListener(this);	//Register as listener to get job finished notice.

		
		try {
			controller.initialize(currentJob.getCrawlOrder());
		} catch (InitializationException e) {
			//TODO Report Error
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		controller.startCrawl();
		currentJob.setStatus(CrawlJob.STATUS_RUNNING);
		currentJob.setStatisticsTracking(getStatistics());
		crawling = true;
		statusMessage = CRAWLER_STARTED;
	}

	/**
	 * Update the crawl order for the running crawl to match the given filename.
	 * The new crawl order is then loaded into the CrawlController.
	 * @param filename
	 */	
	public void updateCrawlOrder(String filename)
	{
		currentJob.setCrawlOrder(filename);
		updateCrawlOrder();
	}
	
	/**
	 * The specified crawl order for the current job is (re)loaded into the CrawlController
	 * This will cause changes to it to be put into effect.
	 */	
	public void updateCrawlOrder()
	{
		controller.updateOrder(currentJob.getCrawlOrder());
	}
	
	/**
	 * Terminate the currently running job
	 * If no job is running this method will do nothing.
	 * This will NOT stop the crawler, it will procede with the next job unless it's stopped
	 * 
	 * Info on success or failure (in user readable form can be accessed via getStatusMessage()
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
			controller.stopCrawl(); //This will cause crawlEnding to be invoked. It will handle the clean up.
			statusMessage = CRAWLER_STOPPED;
			crawling = false;
		}
		try
		{
			wait(3000); // Take a few moments so that the controller can change states before the UI updates. The CrawlEnding event will wake us if it occurs sooner than this.
		}
		catch(InterruptedException e){}
	}
	
	public void pauseJob() {
		controller.pauseCrawl();
		currentJob.setStatus(CrawlJob.STATUS_WAITING_FOR_PAUSE); //We'll do this pre-emptively so that the UI can be updated.
	}
	
	public void resumeJob() {
		controller.resumeCrawl();
	}
	
	/**
	 * 
	 * @return A string informing the user as to the success or failure of the last action performed.
	 */
	public String getStatusMessage()
	{
		return statusMessage;
	}
	
	public boolean shouldcrawl()
	{
		return shouldcrawl;
	}
	
	public boolean isCrawling()
	{
		return crawling;
	}
	
	public StatisticsTracking getStatistics()
	{
		return controller.getStatistics();
	}
	
	/**
	 * Set the default crawl order.
	 * @param filename The filename (with path) of the new default crawl order;
	 */
	public void setDefaultCrawlOrder(String filename)
	{
		try{
			// Take the new default crawl order and use it to overwrite the old one.
			orderTransform.setNode(OrderTransformation.readDocumentFromFile(filename));
			orderTransform.serializeToXMLFile(WEBAPP_PATH+orderFile); 
			// And then reload the old one (now overwritten)
			loadCrawlOrder();
		} 
		catch(IOException e){
			e.printStackTrace();	
		}
		catch (InitializationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	
	public void updateDefaultCrawlOrder(HttpServletRequest req) 
	{
		createCrawlOrderFile(req, WEBAPP_PATH+orderFile);	

		try {
			loadCrawlOrder();
			statusMessage = CRAWL_ORDER_UPDATED;
		} catch (InitializationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}

	/**
	 * Same as createCrawlOrderFile(req, filename, null, false)
	 * 
	 * @param req Containing a form with parameters named for the relevant XPaths in a crawl order and values that are appropriate for every given XPath.  Any XPath not found in the request will have values equal to the default crawl order.
	 * @param filename Filename (including path) for where this new crawl order file is to be created.
	 */
	public void createCrawlOrderFile(HttpServletRequest req, String filename)
	{
		createCrawlOrderFile(req, filename, null,false);
	}
	
	/**
	 * Builds a new crawl order based on a form in a HttpServletRequest. (Presumably posted from a webpage)
	 * 
	 * @param req Containing a form with parameters named for the relevant XPaths in a crawl order and values that are appropriate for every given XPath.  Any XPath not found in the request will have values equal to the default crawl order.
	 * @param filename Filename (including path) for where this new crawl order file is to be created.
	 * @param seedsFileName if not null, this will supercede any seedfile declared in the req. Should NOT include path.  Seed file will be stored in the same path as the crawl order (filename)
	 * @param writeSeedsToFile if true the seeds in the req will be written to the seedfile rather then the new crawl order xml.  If false and req contains seeds they will be written to the crawl order xml and the seedfilename (in req or otherwise) will be discarded.
	 */
	public void createCrawlOrderFile(HttpServletRequest req, String filename, String seedsFileName, boolean writeSeedsToFile)
	{
		Enumeration it = req.getParameterNames();
		String name;
		String seeds = null;

		if(seedsFileName!=null)
		{
			// If seedsFileName provided we will ignore that in the request.
			// Write it now to get it out of the way.
			orderTransform.setNodeValue(XP_SEEDS_FILE,ArchiveUtils.getFilePath(filename)+seedsFileName);
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
				BufferedWriter writer =
					new BufferedWriter(
						new FileWriter(ArchiveUtils.getFilePath(filename) + seedsFileName));
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


	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.CrawlJobHandler#removeJob(org.archive.crawler.framework.CrawlJob)
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

	/* (non-Javadoc)
	 * @see org.archive.crawler.event.CrawlStatusListener#crawlPausing(java.lang.String)
	 */
	public void crawlPausing(String statusMessage) {
		currentJob.setStatus(statusMessage);
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.event.CrawlStatusListener#crawlPaused(java.lang.String)
	 */
	public void crawlPaused(String statusMessage) {
		currentJob.setStatus(statusMessage);
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.event.CrawlStatusListener#crawlResuming(java.lang.String)
	 */
	public void crawlResuming(String statusMessage) {
		currentJob.setStatus(statusMessage);
	}

	/* 
	 * @see org.archive.crawler.event.CrawlStatusListener#crawlEnding(java.lang.String)
	 */
	public void crawlEnding(String sExitMessage)
	{
		crawling = false;
		currentJob.setStatus(sExitMessage);
		completedCrawlJobs.add(currentJob);
		currentJob.setReadOnly(); //Further changes have no meaning
		currentJob = null;
		controller = null; // Remove the reference so that the old controller can be gc.
		if(shouldcrawl)
		{
			startNextJob();		
		}
		
		synchronized(this){
			notify(); 		//If the GUI terminated the job then it is waiting for this event.
		}
	}
	
	/*
	 * @see org.archive.crawler.event.CrawlStatusListener#crawlEnded(java.lang.String)
	 */
	public void crawlEnded(String sExitMessage) {
		// Not interested.  Once the Controller tells us that it is ending it's crawl we will simply assume that that was indeed done.
	}

}
