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
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.URISelector;
import org.archive.crawler.io.FetchFormatter;

/**
 * @author gojomo
 *
 */
public class SimpleSelector implements URISelector, CoreAttributeConstants {
	private static Logger logger = Logger.getLogger("org.archive.crawler.basic.SimpleSelector");
	private static Logger fetchLogger = Logger.getLogger("fetchLogger");

	CrawlController controller;
	SimpleStore store;
	private int maxLinkDepth = -1;
	int completionCount = 0;

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
			// handle links, if not too deep
			if ((maxLinkDepth>=0) && (curi.getAList().getInt("distance-from-seed") < maxLinkDepth) ) {
				if (curi.getAList().containsKey("html-links")) {
					Collection links = (Collection)curi.getAList().getObject("html-links");
					Iterator iter = links.iterator();
					while(iter.hasNext()) {
						String l = (String)iter.next();
						UURI embed = UURI.createUURI(l,curi.getBaseUri());
						store.insert(embed,curi.getAList().getInt("distance-from-seed")+1);
					}
				}
			}
			
			// snooze as necessary
			long duration = 0;
			if (curi.getAList().containsKey(A_DELAY_FACTOR)
				&& curi.getAList().containsKey(A_FETCH_BEGAN_TIME)
				&& curi.getAList().containsKey(A_FETCH_COMPLETED_TIME)) {
				int delayFactor = curi.getAList().getInt(A_DELAY_FACTOR);
				long completeTime =
					curi.getAList().getLong(A_FETCH_COMPLETED_TIME);
				duration =
					delayFactor
						* (completeTime
							- curi.getAList().getLong(A_FETCH_BEGAN_TIME));

				if (curi.getAList().containsKey(A_MINIMUM_DELAY)) {
					// ensure minimum delay is enforced
					int min = curi.getAList().getInt(A_MINIMUM_DELAY);
					if (min > duration) {
						duration = min;
					}
				}
				// TODO: maximum delay? 
				store.snoozeQueueUntil(
					curi.getClassKey(),
					completeTime + duration);
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
		maxLinkDepth = controller.getOrder().getBehavior().getIntAt("//limits/max-link-depth/@value");
		
		ConsoleHandler ch = new ConsoleHandler();
		ch.setFormatter(new FetchFormatter());
		fetchLogger.addHandler(ch);
		fetchLogger.setUseParentHandlers(false);
	}

}
