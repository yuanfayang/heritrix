/*
 * BreadthFirstScheduler.java
 * Created on May 27, 2003
 *
 * $Header$
 */
package org.archive.crawler.basic;

import java.util.Iterator;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.ToeThread;
import org.archive.crawler.framework.URIScheduler;

/**
 * @author gojomo
 *
 */
public class BreadthFirstScheduler implements URIScheduler {
	CrawlController controller = null;
	SimpleStore store;
	
	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.URIScheduler#curiFor(org.archive.crawler.framework.ToeThread)
	 */
	public CrawlURI curiFor(ToeThread thread) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.URIScheduler#initialize()
	 */
	public void initialize(CrawlController c) {
		controller = c;
		store = (SimpleStore)c.getStore();
		// load seeds
		Iterator iter = c.getOrder().getScope().getSeeds().iterator();
		while (iter.hasNext()) {
			insertAsSeed((UURI)iter.next());
		}
	}

	/**
	 * @param uuri
	 */
	private void insertAsSeed(UURI uuri) {
		// TODO implement
		// must be harmless to redundantly add same UURI
		store.insertAsSeed(uuri);
	}

}
