/*
 * SimpleSelector.java
 * Created on Jun 5, 2003
 *
 * $Header$
 */
package org.archive.crawler.basic;

import java.util.Collection;
import java.util.Iterator;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.httpclient.methods.GetMethod;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.URISelector;
import org.archive.crawler.io.FetchFormatter;

/**
 * @author gojomo
 *
 */
public class SimpleSelector implements URISelector {
	private static Logger logger = Logger.getLogger("org.archive.crawler.basic.SimpleSelector");
	private static Logger fetchLogger = Logger.getLogger("fetchLogger");

	CrawlController controller;
	SimpleStore store;
	int completionCount;

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.URISelector#inter(org.archive.crawler.datamodel.CrawlURI)
	 */
	public void inter(CrawlURI curi) {
		synchronized(store) {

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
			
			// note completions
			// TODO: dont' count/retry certain errors
			if(curi.getAList().containsKey("http-transaction")) {
				GetMethod get = (GetMethod) curi.getAList().getObject("http-transaction");
				// allow get to be GC'd
				curi.getAList().remove("http-transaction");
				//
				completionCount++;
				if ( (completionCount % 25) == 0) {
					logger.info("==========> " +
						completionCount+" <========== HTTP URIs completed");
				}
				
				int statusCode = -1;
				String length = "n/a";
				try {
					statusCode = get.getStatusCode();
					length = get.getResponseHeader("Content-Length").getValue();
				} catch (NullPointerException npe ) {
				}
				Object array[] = { new Integer(statusCode), length, curi.getUURI().getUri() };
				fetchLogger.log(Level.INFO,curi.getUURI().getUri().toString(),array);
			}
			// note that CURI has passed out of scheduling
			curi.setStoreState(URIStoreable.FINISHED);
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
		ConsoleHandler ch = new ConsoleHandler();
		ch.setFormatter(new FetchFormatter());
		fetchLogger.addHandler(ch);
		fetchLogger.setUseParentHandlers(false);
	}

}
