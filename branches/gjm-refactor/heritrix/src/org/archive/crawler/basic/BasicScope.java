/*
 * BasicScope.java
 * Created on Oct 1, 2003
 *
 * $Header$
 */
package org.archive.crawler.basic;

import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.CrawlScope;
import org.archive.crawler.framework.Filter;
import org.archive.crawler.util.NullFilter;

/**
 * A core CrawlScope suitable for the most common
 * crawl needs.
 * 
 * Roughly, its logic is that a URI is included if:
 * 
 *    (( isSeed(uri) || focusFilter.accepts(uri) ) 
 *      || transitiveFilter.accepts(uri) )
 *     && ! excludeFilter.accepts(uri)
 * 
 * @author gojomo
 *
 */
public class BasicScope extends CrawlScope {
	private static final Filter NULL_FILTER = new NullFilter();
	Filter focusFilter; 
	Filter transitiveFilter; 
	Filter excludeFilter;
	
	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Filter#initialize(org.archive.crawler.framework.CrawlController)
	 */
	public void initialize(CrawlController controller) {
		super.initialize(controller);
		focusFilter = (Filter) instantiate("/focus");
		if (focusFilter == null) {
			focusFilter = NULL_FILTER;
		}
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Filter#innerAccepts(java.lang.Object)
	 */
	protected boolean innerAccepts(Object o) {
		// TODO Auto-generated method stub
		return false;
	}

} 
