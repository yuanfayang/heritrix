/*
 * BasicScope.java
 * Created on Oct 1, 2003
 *
 * $Header$
 */
package org.archive.crawler.basic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.filter.HopsFilter;
import org.archive.crawler.filter.SeedExtensionFilter;
import org.archive.crawler.filter.TransclusionFilter;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.CrawlScope;
import org.archive.crawler.framework.Filter;

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
 * The focusFilter may be specified by either:
 *   - adding a 'mode' attribute to the 
 *     <scope> element. mode="broad" is equivalent
 *     to no focus; modes "path", "host", and "domain"
 *     imply a SeedExtensionFilter will be used, with 
 *     the <scope> element providing its configuration 
 *   - adding a <focus> subelement
 * If unspecified, the focusFilter will default to
 * an accepts-all filter.
 * 
 * The transitiveFilter may be specified by supplying
 * a <transitive> subelement. If unspecified, a 
 * TransclusionFilter will be used, with the <scope>
 * element providing its configuration.
 * 
 * The excludeFilter may be specified by supplying
 * a <exclude> subelement. If unspecified, a
 * accepts-none filter will be used -- meaning that
 * no URIs will pass the filter and thus be excluded.
 * 
 * @author gojomo
 *
 */
public class Scope extends CrawlScope {
	Filter focusFilter; 
	Filter transitiveFilter; 
	Filter excludeFilter;
	List seeds;
	
	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Filter#initialize(org.archive.crawler.framework.CrawlController)
	 */
	public void initialize(CrawlController controller) {
		super.initialize(controller);
		// setup focusFilter
		if(getStringAt("@mode")==null) {
			focusFilter = (Filter) instantiate("focus");
		} else if (getStringAt("@mode").equalsIgnoreCase("broad")){
			focusFilter = null;
		} else {
			// SeedExtensionFilter implied
			focusFilter = new SeedExtensionFilter();
			focusFilter.setNode(xNode);
		}
		if(focusFilter != null) {
			focusFilter.initialize(controller);
			// only set up transitiveFilter if focusFilter set
			transitiveFilter = (Filter) instantiate("transitive");
			if(transitiveFilter == null) {
				transitiveFilter = new TransclusionFilter();
				transitiveFilter.setNode(xNode);
			}
			transitiveFilter.initialize(controller);
		}
		// setup exclude filter
		if(getNodeAt("@max-link-hops")!=null) {
			// SeedExtensionFilter implied
			excludeFilter = new HopsFilter();
			excludeFilter.setNode(xNode);
		} else {
			excludeFilter = (Filter) instantiate("exclude");
		}
		excludeFilter.initialize(controller);
	}

	/**
	 * 
	 */
	private void cacheSeeds() {
		seeds = new ArrayList();
		Iterator iter = super.getSeedsIterator();
		while(iter.hasNext()) {
			seeds.add(iter.next());
		}
	}

	/** 
	 * @see org.archive.crawler.framework.Filter#innerAccepts(java.lang.Object)
	 */
	protected boolean innerAccepts(Object o) {
		return ((isSeed(o)||focusAccepts(o))||transitiveAccepts(o))&&!excludeAccepts(o);
	}
	
	/**
	 * @param o
	 * @return
	 */
	private boolean excludeAccepts(Object o) {
		if (excludeFilter == null) {
			return false;
		}
		return excludeFilter.accepts(o);
	}

	/**
	 * @param o
	 * @return
	 */
	private boolean transitiveAccepts(Object o) {
		if (transitiveFilter == null) {
			return true;
		}
		return transitiveFilter.accepts(o);
	}

	/**
	 * @param o
	 * @return
	 */
	private boolean focusAccepts(Object o) {
		if (focusFilter == null) {
			return true;
		}
		return focusFilter.accepts(o);
	}

	private boolean isSeed(Object o) {
		return o instanceof CandidateURI && ((CandidateURI)o).getIsSeed();
	}

	/**
	 * @return
	 */
	public Filter getExcludeFilter() {
		return excludeFilter;
	}

	/**
	 * @return
	 */
	public Filter getFocusFilter() {
		return focusFilter;
	}

	/**
	 * @return
	 */
	public Filter getTransitiveFilter() {
		return transitiveFilter;
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.CrawlScope#getSeedsIterator()
	 */
	public Iterator getSeedsIterator() {
		if (focusFilter == null) {
			// a cached seeds list isn't necessary for scope tests
			return super.getSeedsIterator();
		} 
		// seeds should be in memory for scope tests
		if (seeds==null) {
			cacheSeeds();
		}
		return seeds.iterator();
	}

} 
