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
		synchronized (store) {
			while(true) {
				long now = System.currentTimeMillis();
				long waitMax = 0;
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
				if(store.getHeldClassQueues().isEmpty() && store.getSnoozeQueues().isEmpty()) {
					// nothing left to crawl
					logger.info("nothing left to crawl");
					return null;
				}
				waitMax = store.earliestWakeTime()-now;
				
				try {
					if(waitMax<0) {
						logger.warning("negative wait "+waitMax+" ignored");
					} else {
						store.wait(waitMax);
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * @param crawlURI
	 * @return
	 */
	private CrawlURI emitCuri(CrawlURI curi) {
		if(curi != null) {
			store.noteInProcess(curi);
			curi.setHost(controller.getHostCache().getHostFor(curi));
		}
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
