/*
 * BreadthFirstScheduler.java
 * Created on May 27, 2003
 *
 * $Header$
 */
package org.archive.crawler.basic;

import java.util.Iterator;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.ToeThread;
import org.archive.crawler.framework.URIScheduler;

/**
 * @author gojomo
 *
 */
public class SimpleScheduler implements URIScheduler {
	private static Logger logger = Logger.getLogger("org.archive.crawler.basic.SimpleScheduler");

	CrawlController controller = null;
	SimpleStore store;
	
	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.URIScheduler#curiFor(org.archive.crawler.framework.ToeThread)
	 */
	public CrawlURI curiFor(ToeThread thread) {
		long now = System.currentTimeMillis();
		long waitMax = 0;
		synchronized (store) {
			store.wakeReadyQueues(now);
			CrawlURI curi = null;
			if (!store.getReadyClassQueues().isEmpty()) {
				curi = store.dequeueFromReady();
				return emitCuri(curi);
			}
			while ((curi = store.dequeueFromPending()) != null) {
				if (!store.enqueueIfNecessary(curi)) {
					// OK to emit
					return emitCuri(curi);
				}
			}
			waitMax = store.earliestWakeTime()-now;
		}
		try {
			store.ReadyChangeSemaphore.wait(waitMax);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @param crawlURI
	 * @return
	 */
	private CrawlURI emitCuri(CrawlURI curi) {
		store.noteInProcess(curi);
		return curi;
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
		logger.info("Scheduler inserting seed "+uuri);
		store.insertAsSeed(uuri);
	}

}
