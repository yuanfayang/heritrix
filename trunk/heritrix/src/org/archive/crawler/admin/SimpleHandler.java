package org.archive.crawler.admin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.archive.crawler.basic.Frontier;
import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.CrawlJob;
import org.archive.crawler.framework.CrawlJobHandler;
import org.archive.crawler.framework.CrawlListener;
import org.archive.crawler.framework.exceptions.InitializationException;
import org.archive.util.ArchiveUtils;
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
	
	/**
	 * Returns the Frontier report..
	 * 
	 * @return A report of the frontiers status.
	 */
	public String getFrontierReport()
	{
		if(controller == null || controller.getFrontier() == null)
		{
			return "Crawler not running";
		}
		else
		{
			return ((Frontier)controller.getFrontier()).report();
		}
	}
	
	/**
	 * Returns the CrawlControllers ToeThreads report.
	 * 
	 * @return
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
	
	public String getNextJobUID()
	{
		return ArchiveUtils.TIMESTAMP17.format(new Date());		
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
		createCrawlOrderFile(req, WEB_APP_PATH+orderFile);	

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
}