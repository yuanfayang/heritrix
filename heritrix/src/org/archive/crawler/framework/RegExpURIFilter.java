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
public class RegExpURIFilter extends Filter {
	String name;

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

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Filter#accepts(java.lang.Object)
	 */
	public boolean accepts(Object o) {
		// TODO Auto-generated method stub
		return false;
	}


}
