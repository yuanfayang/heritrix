/*
 * SimpleSelector.java
 * Created on Jun 5, 2003
 *
 * $Header$
 */
package org.archive.crawler.basic;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.httpclient.methods.GetMethod;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.URISelector;

/**
 * @author gojomo
 *
 */
public class SimpleSelector implements URISelector, CoreAttributeConstants, FetchStatusCodes {
	private static Logger logger = Logger.getLogger("org.archive.crawler.basic.SimpleSelector");

	CrawlController controller;
	SimpleStore store;
	private int maxLinkDepth = -1;
	int completionCount = 0;
	
	public static final int MAX_FETCH_ATTEMPTS = 3;

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.URISelector#inter(org.archive.crawler.datamodel.CrawlURI)
	 */
	public void inter(CrawlURI curi) {
				
		synchronized(store) {		
			
			store.noteProcessingDone(curi);
			
			// check the status code of the uri
			switch(curi.getFetchStatus()){
				// this uri is virgin, let it carry on
				case S_UNATTEMPTED:					
					break;

				// fail cases
				case S_CONNECT_FAILED:					
				case S_CONNECT_LOST:
				case S_DOMAIN_UNRESOLVABLE:
					logger.info("Removing URI " + curi.toString() + ". ");
					// don't let the madness continue
					return;

				// they don't want us to have it
				case S_ROBOTS_PRECLUDED:
					return;
						
				// something bad happened
				case S_INTERNAL_ERROR:
					RuntimeException e = (RuntimeException)curi.getAList().getObject(A_RUNTIME_EXCEPTION);
					StringWriter sw = new StringWriter();
					e.printStackTrace(new PrintWriter(sw));
					controller.failureLogger.info(
						e+" on "+curi+"\n"+
							   sw.toString());
			}
			
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
			    store.notify();
			}
			
			// SUCCESS: note & log
 			completionCount++;
			if ( (completionCount % 50) == 0) {
				logger.info("==========> " +
					completionCount+" <========== HTTP URIs completed");
			}

			String length = "n/a";
			if ( curi.getAList().containsKey("http-transaction")) {
				GetMethod get = (GetMethod) curi.getAList().getObject("http-transaction");
				// allow get to be GC'd
				curi.getAList().remove("http-transaction");
					
				if (get.getResponseHeader("Content-Length")!=null) {
					length = get.getResponseHeader("Content-Length").getValue();
				}
			}
			Object array[] = { new Integer(curi.getThreadNumber()), new Integer(curi.getFetchStatus()), length, curi.getUURI().getUri() };
			controller.successLogger.log(Level.INFO,curi.getUURI().getUri().toString(),array);
				
		} 
			
		// note that CURI has passed out of scheduling
		curi.setStoreState(URIStoreable.FINISHED);
		
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
	}

}
