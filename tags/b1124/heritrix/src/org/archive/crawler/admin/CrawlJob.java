package org.archive.crawler.admin;

/**
 * @author Kristinn Sigurðsson
 *
 * This class encapsulates a crawl job.  This includes, a crawl order filename,
 * a name for the crawl, it's status etc.
 */

public class CrawlJob
{
	protected String sName;
	protected String sFilename; //An XML file describing a crawl order
	protected String sFilepath;
	protected String sStatus;
	protected StatisticsTracker stats;
	
	public CrawlJob(String name, String filename, String path)
	{
		sName = name;
		sFilename = filename;
		sFilepath = path; 
		sStatus = "Pending";
	}
	
	/**
	 * @return
	 */
	public String getOrderFile() {
		return sFilepath+sFilename;
	}

	/**
	 * @return
	 */
	public String getJobName() {
		return sName;
	}

	/**
	 * @return
	 */
	public String getStatus() {
		return sStatus;
	}

	/**
	 * @return
	 */
	public StatisticsTracker getStats() {
		return stats;
	}

	/**
	 * @param string
	 */
	public void setStatus(String string) {
		sStatus = string;
	}

	/**
	 * @param tracker
	 */
	public void setStats(StatisticsTracker tracker) {
		stats = tracker;
	}

}