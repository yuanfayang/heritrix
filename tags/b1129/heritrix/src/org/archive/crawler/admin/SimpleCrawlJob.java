package org.archive.crawler.admin;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.framework.CrawlJob;
import org.archive.crawler.framework.exceptions.InitializationException;
import org.archive.util.ArchiveUtils;

import org.w3c.dom.Node;

/**
 * @author Kristinn Sigurdsson
 *
 * This class encapsulates a crawl job.  This includes, a crawl order filename,
 * a name for the crawl, it's status etc.
 */

public class SimpleCrawlJob implements CrawlJob
{
	protected int UID;
	protected String sName;
	protected String sFilename; //An XML file describing a crawl order (including path)
	protected String sStatus;
	protected StatisticsTracker stats;
	protected boolean isReadOnly = false;
	protected int priority;
	
	protected OrderTransformation orderTransform;
	protected Node orderNode;


	
	public SimpleCrawlJob(int UID, String name, String crawlorderfile, int priority)
	{
		this.UID = UID;
		this.priority = priority;
		sName = name;
		sFilename = crawlorderfile;
		sStatus = STATUS_CREATED;

	}
	
	/**
	 * Implemented to return [UID] 'crawl name'
	 * 
	 * @return
	 */
	public String getJobName() {
		return "["+UID+"] " + sName;
	}

	/**
	 * @return
	 */
	public String getStatus() {
		return sStatus;
	}

	/**
	 * @param string
	 */
	public void setStatus(String string) {
		sStatus = string;
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.CrawlJob#setJobName(java.lang.String)
	 */
	public void setJobName(String jobname) {
		sName = jobname;		
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.CrawlJob#getCrawlOrder()
	 */
	public CrawlOrder getCrawlOrder() {
		CrawlOrder co = null;
		try {
			co = CrawlOrder.readFromFile(getCrawlOrderFile());
		} 
		catch (InitializationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 

		return co;
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.CrawlJob#setCrawlOrder(java.lang.String)
	 */
	public boolean setCrawlOrder(String crawlOrderFile) {
		if(isReadOnly())
		{
			// Can't modify this attribute if read-only
			return false;
		}
		else
		{
			sFilename = crawlOrderFile;
			return true;
		}
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.CrawlJob#getCrawlOrderFile()
	 */
	public String getCrawlOrderFile() {
		// TODO Auto-generated method stub
		return sFilename;
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.CrawlJob#setJobPriority(int)
	 */
	public void setJobPriority(int priority) {
		this.priority = priority;		
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.CrawlJob#getJobPriority()
	 */
	public int getJobPriority() {
		return priority;
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.CrawlJob#setReadOnly()
	 */
	public void setReadOnly() {
		isReadOnly = true;		
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.CrawlJob#isReadOnly()
	 */
	public boolean isReadOnly() {
		return isReadOnly;
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.CrawlJob#setStatisticsTracker(org.archive.crawler.admin.StatisticsTracker)
	 */
	public void setStatisticsTracker(StatisticsTracker tracker) {
		stats = tracker;	
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.CrawlJob#getStatisticsTracker()
	 */
	public StatisticsTracker getStatisticsTracker() {
		return stats;
	}

	public void createCrawlOrderFile(HttpServletRequest req, String filename)
	{
		Enumeration it = req.getParameterNames();
		String name;
		String seedsFileName = null;
		String seeds = null;
	
		while (it.hasMoreElements()) 
		{
			name = it.nextElement().toString();
			String value = req.getParameter(name);
			if (name.equals("//seeds")) 
			{
				seeds = value;
			} 
			else 
			{
				if (name != null && value != null) 
				{
					if (name.equals("//seeds/@src")) 
					{
						seedsFileName = value;
					}
					orderTransform.setNodeValue(name, value);
				}
			}
		}
		if (seeds != null && seedsFileName != null) 
		{
			try 
			{
				BufferedWriter writer =
					new BufferedWriter(
						new FileWriter(ArchiveUtils.getFilePath(filename) + seedsFileName));
				if (writer != null) 
				{
					writer.write(seeds);
					writer.close();
				}
			}
			catch (Exception e) 
			{
				// TODO: handle exception
				e.printStackTrace();
			}
		}
		orderTransform.serializeToXMLFile(AdminConstants.WEB_APP_PATH + filename);
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.CrawlJob#getUID()
	 */
	public int getUID() {
		return UID;
	}	
}