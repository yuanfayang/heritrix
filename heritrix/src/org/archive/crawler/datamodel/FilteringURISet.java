/*
 * FilteringURISet.java
 * Created on Apr 21, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

import org.archive.crawler.framework.Filter;

/**
 * A URISet that also filters, for example by requiring
 * passed-in URIs to be extensions of the contained URIs.
 * 
 * @author gojomo
 *
 */
public class FilteringURISet extends Filter {

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Filter#accepts(java.lang.Object)
	 */
	public boolean accepts(Object o) {
		// TODO Auto-generated method stub
		return false;
	}

}
