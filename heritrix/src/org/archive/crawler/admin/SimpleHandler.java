package org.archive.crawler.admin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Vector;
import java.util.Enumeration;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.CrawlJob;
import org.archive.crawler.framework.CrawlJobHandler;
import org.archive.crawler.framework.CrawlListener;
import org.archive.crawler.framework.exceptions.InitializationException;
import org.archive.util.ArchiveUtils;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Node;

/**
 * 
 * @author Kristinn Sigurdsson
 *
 * This class provides control access to the crawler to the web pages.
 * It allows them to submit jobs, changed configurations and get status information. 
 * 
 * Jobs are queued and processed in the order they are submitted in while the crawler is
 * set to run.  
 * 
 * An instance of it should reside in "application scope" to be equally accessible to
 * all page instances.
 */

public class SimpleHandler implements AdminConstants, CrawlJobHandler, CrawlListener
{
	private static Logger logger = Logger.getLogger("org.archive.crawler.framework.CrawlController");

	private String orderFile;	// Default order file (order.xml by default)
	private CrawlOrder crawlOrder;			// Default CrawlOrder. (matches order.xml) 
	
	//Crawl Jobs	
	private CrawlJob currentJob;			// Job currently being crawled
	private Vector pendingCrawlJobs;		// A list of CrawlJobs
	private Vector completedCrawlJobs;	// A list of CrawlJobs
	
	private boolean shouldcrawl;
	
	private boolean crawling = false;
	private String statusMessage = "No actions taken"; //Reports the success or failure of the last action taken.
	private int crawlerAction = -1;
	// private String diskPath; - Depracated, access CrawlOrder directly.
	private String workingDirectory;
	private Node orderNode;
	private CrawlController controller;
	private OrderTransformation orderTransform;
	
	private int jobserial; //Unique ID for jobs.  

	public SimpleHandler()
	{
		shouldcrawl = false;
		jobserial = 1;
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
			orderNode = OrderTransformation.readDocumentFromFile(WEB_APP_PATH+orderFile);
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
	
	/* Loads the selected order file (as specified by the orderFile attribute)
	 * as a crawlOrder. 
	 */
	private void loadCrawlOrder() throws InitializationException
	{ 
		crawlOrder = CrawlOrder.readFromFile(WEB_APP_PATH+orderFile);
	}
	
	/**
	 * The CrawlController calls this method once it's current job is finished 
	 * or has been terminated for whatever reason (sExitMessage contains details).
	 * This method implements the CrawlListener interface.
	 * 
	 * Once a job has started crawling, it is considered to be crawling until this
	 * method is invoked.  It should only be invoked by the CrawlController in question
	 * once it is exiting.
	 */
	public void crawlEnding(String sExitMessage)
	{
		crawling = false;
		currentJob.setStatus(sExitMessage);
		completedCrawlJobs.add(currentJob);
		currentJob = null;
		controller = null; // Remove the reference so that the old controller can be gc.
		if(shouldcrawl)
		{
			startNextJob();		
		}
	}
	
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
	
	public Vector getPendingJobs()
	{
		return pendingCrawlJobs;
	}
	
	public Vector getCurrentJobs()
	{
		Vector temp = new Vector();
		temp.add(getCurrentJob());
		return temp;		
	}
	
	public int getNextJobUID()
	{
		return jobserial++;		
	}
	
	public CrawlJob getCurrentJob()
	{
		return currentJob;
	}
	
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
		
		controller = new CrawlController(); //Create new controller.
		controller.addListener(this);		//Register as listener to get job finished notice.

		currentJob.setStatus(CrawlJob.STATUS_RUNNING);
		
		try {
			controller.initialize(currentJob.getCrawlOrder());
		} catch (InitializationException e) {
			//TODO Report Error
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		controller.startCrawl();
		currentJob.setStatus("Crawling");
		currentJob.setStatisticsTracker(getStatistics());
		crawling = true;
		statusMessage = CRAWLER_STARTED;
	}
	
	/**
	 * Terminate the currently running job
	 * If no job is running this method will do nothing.
	 * This will NOT stop the crawler, it will procede with the next job unless it's stopped
	 * 
	 * Info on success or failure (in user readable form can be accessed via getStatusMessage()
	 */
	public void terminateJob()
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
	
	public StatisticsTracker getStatistics()
	{
		return controller.getStatistics();
	}
	
	public void updateDefaultCrawlOrder(HttpServletRequest req) 
	{
		createCrawlOrderFile(req, orderFile);	

		statusMessage = CRAWL_ORDER_UPDATED;
	}
	
	public void createCrawlOrderFile(HttpServletRequest req, String filename)
	{
		Enumeration it = req.getParameterNames();
		String name;
		String seedsFileName = null;
		String seeds = null;

		while (it.hasMoreElements()) {
			name = it.nextElement().toString();
			String value = req.getParameter(name);
			if (name.equals("//seeds")) {
				seeds = value;
			} else {
				if (name != null && value != null) {
					if (name.equals("//seeds/@src")) {
						seedsFileName = value;
					}
					orderTransform.setNodeValue(name, value);
				}
			}
		}
		if (seeds != null && seedsFileName != null) {
			try {
				BufferedWriter writer =
					new BufferedWriter(
						new FileWriter(ArchiveUtils.getFilePath(orderFile) + seedsFileName));
				if (writer != null) {
					writer.write(seeds);
					writer.close();
				}
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
		}
		orderTransform.serializeToXMLFile(WEB_APP_PATH + filename);
		
		try {
			loadCrawlOrder();
		} catch (InitializationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}


	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.CrawlJobHandler#removeJob(org.archive.crawler.framework.CrawlJob)
	 */
	public boolean removeJob(int jobUID) {
		for(int i=0 ; i<pendingCrawlJobs.size() ; i++)
		{
			CrawlJob cj = (CrawlJob)pendingCrawlJobs.get(i);
			if(cj.getUID()==jobUID)
			{
				// Found the one to remove.	
				pendingCrawlJobs.remove(i);
				return true;
			}
		}
		return false;
	}
}