/*
 * HopsFilter.java
 * Created on Oct 3, 2003
 *
 * $Header$
 */
package org.archive.crawler.filter;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.Filter;

/**
 * Accepts (returns  for)) for all CandidateURIs passed in
 * with a link-hop-count greater than the max-link-hops
 * value. 
 * 
 * @author gojomo
 *
 */
public class HopsFilter extends Filter {
	int maxLinkHops = Integer.MAX_VALUE;
	int maxTransHops = Integer.MAX_VALUE;
	
	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Filter#innerAccepts(java.lang.Object)
	 */
	protected boolean innerAccepts(Object o) {
		if(! (o instanceof CandidateURI)) {
			return false;
		}
		String path = ((CandidateURI)o).getPathFromSeed();
		int linkCount = 0;
		int transCount = 0;
		for(int i=path.length()-1;i>=0;i--) {
			if(path.charAt(i)=='L') {
				linkCount++;
			} else if (linkCount==0) {
				transCount++;
			}
		}
		return (linkCount > maxLinkHops)|| (transCount>maxTransHops);
	}

	public void initialize(CrawlController c) {
		super.initialize(c);
		maxLinkHops = getIntAt("@max-link-hops",maxLinkHops);
		maxTransHops = getIntAt("@max-trans-hops",maxTransHops);
	}

}
