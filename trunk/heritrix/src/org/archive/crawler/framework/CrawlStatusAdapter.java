package org.archive.crawler.framework;

/**
 * An Adapter class for the CrawlStatusListener interface. 
 * 
 * @author Kristinn Sigurdsson
 */
public class CrawlStatusAdapter implements CrawlStatusListener
{

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.CrawlStatusListener#crawlEnding(java.lang.String)
	 */
	public void crawlEnding(String sExitMessage) {
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.CrawlStatusListener#crawlPausing(java.lang.String)
	 */
	public void crawlPausing(String statusMessage) {
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.CrawlStatusListener#crawlPaused(java.lang.String)
	 */
	public void crawlPaused(String statusMessage) {
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.CrawlStatusListener#crawlResuming(java.lang.String)
	 */
	public void crawlResuming(String statusMessage) {
	}
	
}