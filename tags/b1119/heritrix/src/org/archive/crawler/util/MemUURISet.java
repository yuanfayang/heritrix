/*
 * MemUURISet.java
 * Created on Sep 15, 2003
 *
 * $Header$
 */
package org.archive.crawler.util;

import java.util.HashSet;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.datamodel.UURISet;

/**
 * @author gojomo
 *
 */
public class MemUURISet extends HashSet implements UURISet {

	/* (non-Javadoc)
	 * @see org.archive.crawler.datamodel.UURISet#size()
	 */
	public long count() {
		return size();
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.datamodel.UURISet#contains(org.archive.crawler.datamodel.UURI)
	 */
	public boolean contains(UURI u) {
		return contains((Object)u);
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.datamodel.UURISet#contains(org.archive.crawler.datamodel.CrawlURI)
	 */
	public boolean contains(CandidateURI curi) {
		return contains((Object)curi.getUURI());
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.datamodel.UURISet#add(org.archive.crawler.datamodel.UURI)
	 */
	public void add(UURI u) {
		add((Object)u);
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.datamodel.UURISet#remove(org.archive.crawler.datamodel.UURI)
	 */
	public void remove(UURI u) {
		remove((Object)u);
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.datamodel.UURISet#add(org.archive.crawler.datamodel.CrawlURI)
	 */
	public void add(CandidateURI curi) {
		add(curi.getUURI());
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.datamodel.UURISet#remove(org.archive.crawler.datamodel.CrawlURI)
	 */
	public void remove(CandidateURI curi) {
		remove(curi.getUURI());
	}

}
