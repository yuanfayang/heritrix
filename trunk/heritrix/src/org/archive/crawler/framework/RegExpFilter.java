/* 
 * RegExpFilter.java
 * Created on Apr 16, 2003
 *
 * $Header$
 */
package org.archive.crawler.framework;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.UURI;

/**
 * 
 * @author Gordon Mohr
 */
public class RegExpFilter implements UURIFilter {
	String name;
	

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Filter#accepts(org.archive.crawler.framework.NormalizedURIString)
	 */
	public boolean accepts(UURI uuri) {
		// TODO Auto-generated method stub
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Filter#accepts(org.archive.crawler.framework.CrawlURI)
	 */
	public boolean accepts(CrawlURI curi) {
		return accepts(curi.getUURI());
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Filter#setName(java.lang.String)
	 */
	public void setName(String n) {
		name = n;
		
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Filter#getName()
	 */
	public String getName() {
		return name;
	}


}
