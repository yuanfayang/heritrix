/*
 * NullFilter.java
 * Created on Oct 2, 2003
 *
 * $Header$
 */
package org.archive.crawler.util;

import org.archive.crawler.framework.Filter;

/**
 * @author gojomo
 *
 */
public class NullFilter extends Filter {

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Filter#innerAccepts(java.lang.Object)
	 */
	protected boolean innerAccepts(Object o) {
		return true;
	}

}
