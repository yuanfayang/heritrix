/*
 * CrawlScope.java
 * Created on Oct 1, 2003
 *
 * $Header$
 */
package org.archive.crawler.framework;

/**
 * Filter which determines, looking at the totality of 
 * information available about a CandidateURI/CrawlURI,
 * instamce, if that URI should be scheduled for crawling.
 * 
 * Dynamic information inherent in the discovery of the
 * URI -- such as the path by which it was discovered --
 * may be considered.
 * 
 * Dynamic information which requires the consultation 
 * of external and potentially volatile information --
 * such as current robots.txt requests and the history
 * of attempts to crawl the same URI -- should NOT be
 * considered. Those potentially high-latency decisions
 * should be made at another step. . 
 * 
 * @author gojomo
 *
 */
public class CrawlScope extends Filter {
	int version;
	
	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Filter#innerAccepts(java.lang.Object)
	 */
	protected boolean innerAccepts(Object o) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * @return
	 */
	public int getVersion() {
		return version;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "CrawlScope<"+name+">";
	}
}
