/* 
 * TransclusionFilter.java
 * Created on Oct 3, 2003
 *
 * $Header$
 */
package org.archive.crawler.filter;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.framework.Filter;

/**
 * Filter which accepts CandidateURI/CrawlURI instances which
 * 
 * @author Gordon Mohr
 */
public class TransclusionFilter extends Filter {
	// 1-3 trailing P(recondition)/R(eferral)/E(mbed) hops
	private static final String TRANSCLUSION_PATH = ".*[PRE][PRE]?[PRE]?$";
	
	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Filter#innerAccepts(java.lang.Object)
	 */
	protected boolean innerAccepts(Object o) {
		if(o instanceof CandidateURI) {
			return ((CandidateURI)o).getPathFromSeed().matches(TRANSCLUSION_PATH);
		}
		return false;
	}

}
