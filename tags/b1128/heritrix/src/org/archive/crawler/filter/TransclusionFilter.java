/* 
 * TransclusionFilter.java
 * Created on Oct 3, 2003
 *
 * $Header$
 */
package org.archive.crawler.filter;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.Filter;

/**
 * Filter which accepts CandidateURI/CrawlURI instances which contain more
 * than zero but fewer than max-trans-hops entries at the end of their 
 * discovery path. 
 * 
 * @author Gordon Mohr
 */
public class TransclusionFilter extends Filter {
	int maxTransHops = 0;
	
	// 1-3 trailing P(recondition)/R(eferral)/E(mbed)/X(speculative-embed) hops
	private static final String TRANSCLUSION_PATH = ".*[PREX][PREX]?[PREX]?$";
	
	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Filter#innerAccepts(java.lang.Object)
	 */
	protected boolean innerAccepts(Object o) {
//		if(o instanceof CandidateURI) {
//			return ((CandidateURI)o).getPathFromSeed().matches(TRANSCLUSION_PATH);
//		}
//		return false;
		if(! (o instanceof CandidateURI)) {
			return false;
		}
		String path = ((CandidateURI)o).getPathFromSeed();
		int transCount = 0;
		for(int i=path.length()-1;i>=0;i--) {
			// everything except 'L' is consider transitive
			if(path.charAt(i)=='L') {
				break;
			} else {
				transCount++;
			}
		}
		return (transCount > 0) && (transCount <= maxTransHops);
	}

	public void initialize(CrawlController c) {
		super.initialize(c);
		maxTransHops = getIntAt("@max-trans-hops",maxTransHops);
	}

}
