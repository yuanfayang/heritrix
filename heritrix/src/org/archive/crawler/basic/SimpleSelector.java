/*
 * SimpleSelector.java
 * Created on Jun 5, 2003
 *
 * $Header$
 */
package org.archive.crawler.basic;

import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.URISelector;

/**
 * @author gojomo
 *
 */
public class SimpleSelector implements URISelector {
	private static Logger logger = Logger.getLogger("org.archive.crawler.basic.SimpleScheduler");

	CrawlController controller;
	SimpleStore store;

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.URISelector#inter(org.archive.crawler.datamodel.CrawlURI)
	 */
	public void inter(CrawlURI curi) {
		store.noteProcessingDone(curi);
		// handle prerequisites
		if (curi.getAList().containsKey("prerequisite-uri")) {
			UURI prereq = UURI.createUURI(curi.getPrerequisiteUri(),curi.getUURI().getUri());
			curi.getAList().remove("prerequisite-uri");
			store.reinsert(curi);
			store.insertAtHead(prereq,curi.getAList().getInt("distance-from-seed"));
			return;
		}
		// handle embeds 
		if (curi.getAList().containsKey("html-embeds")) {
			Collection embeds = (Collection)curi.getAList().getObject("html-embeds");
			Iterator iter = embeds.iterator();
			while(iter.hasNext()) {
				String e = (String)iter.next();
				UURI embed = UURI.createUURI(e,curi.getBaseUri());
				store.insertAtHead(embed,curi.getAList().getInt("distance-from-seed"));
			}
		}
		// handle links 
		if (curi.getAList().containsKey("html-links")) {
			Collection links = (Collection)curi.getAList().getObject("html-links");
			Iterator iter = links.iterator();
			while(iter.hasNext()) {
				String l = (String)iter.next();
				UURI embed = UURI.createUURI(l,curi.getBaseUri());
				store.insert(embed,curi.getAList().getInt("distance-from-seed")+1);
			}
		}
		
		// snooze as necessary
		if(curi.getAList().containsKey("delay-factor")&&
		curi.getAList().containsKey("http-begin-time")&&
		curi.getAList().containsKey("http-complete-time")) {
			int delayFactor = curi.getAList().getInt("delay-factor");
			long completeTime = curi.getAList().getLong("http-complete-time");
			long duration = delayFactor * (completeTime-curi.getAList().getLong("http-begin-time"));
			store.snoozeQueueUntil(curi.getClassKey(),completeTime+duration);
		}
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.URISelector#initialize(org.archive.crawler.framework.CrawlController)
	 */
	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.URIScheduler#initialize()
	 */
	public void initialize(CrawlController c) {
		controller = c;
		store = (SimpleStore)c.getStore();
	}

}
