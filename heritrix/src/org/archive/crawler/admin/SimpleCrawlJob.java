package org.archive.crawler.admin;

import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.framework.CrawlJob;
import org.archive.crawler.framework.exceptions.InitializationException;

/**
 * @author Kristinn Sigurdsson
 *
 * This class encapsulates a crawl job.  This includes, a crawl order filename,
 * a name for the crawl, it's status etc.
 */

public class SimpleCrawlJob implements CrawlJob
{
	protected String UID;		//A UID issued by the CrawlJobHandler.
	protected String sName;
	protected String sFilename; //An XML file describing a crawl order (including path)
	protected String sStatus;
	protected boolean isReadOnly = false;
	protected StatisticsTracker stats;
	protected int priority;
	protected int orderVersion;
	
	public SimpleCrawlJob(String UID, String name, String crawlorderfile, int priority){
		this.UID = UID;
		this.priority = priority;
		sName = name;
		sFilename = crawlorderfile;
		sStatus = STATUS_CREATED;
		orderVersion = 1; 

	}
	
	/**
	 * Implemented to return 'crawl name' [UID]
	 * 
	 * @return
	 */
	public String getJobName() {
		return sName + " ["+UID+"]";
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
			orderVersion++;
			sFilename = crawlOrderFile;
			return true;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.CrawlJob#getCrawlOrderFile()
	 */
	public String getCrawlOrderFile() {
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

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.CrawlJob#getUID()
	 */
	public String getUID() {
		return UID;
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.CrawlJob#getOrderVersion()
	 */
	public int getOrderVersion() {
		return orderVersion;
	}	
}