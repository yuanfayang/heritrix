/* 
 * RegExpFilter.java
 * Created on Apr 16, 2003
 *
 * $Header$
 */
package org.archive.crawler.framework;

/**
 * 
 * @author Gordon Mohr
 */
public class RegExpFilter implements Filter {


	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Filter#accepts(org.archive.crawler.framework.NormalizedURIString)
	 */
	public boolean accepts(NormalizedURIString curi) {
		// TODO Auto-generated method stub
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Filter#accepts(org.archive.crawler.framework.CrawlURI)
	 */
	public boolean accepts(CrawlURI curi) {
		return accepts(curi.getNormalizedURIString());
	}


}
