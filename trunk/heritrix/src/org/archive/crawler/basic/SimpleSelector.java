/*
 * SimpleSelector.java
 * Created on Jun 5, 2003
 *
 * $Header$
 */
package org.archive.crawler.basic;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.Filter;
import org.archive.crawler.framework.URISelector;
import org.archive.crawler.framework.XMLConfig;

/**
 * @author gojomo
 *
 */
public class SimpleSelector extends XMLConfig implements URISelector, CoreAttributeConstants, FetchStatusCodes {
	/**
	 * XPath to any specified filters
	 */
	private static String XP_FILTERS = "filter";
	private static Logger logger = Logger.getLogger("org.archive.crawler.basic.SimpleSelector");

	CrawlController controller;
	SimpleStore store;
	ArrayList filters = new ArrayList();
	private int maxLinkDepth = -1;
	private int maxDeferrals = 5;
	int completionCount = 0;
	
	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.URISelector#inter(org.archive.crawler.datamodel.CrawlURI)
	 */
	public void inter(CrawlURI curi) {
				
		synchronized(store) {		
			
			store.noteProcessingDone(curi);
			// snooze queues as necessary
			updateScheduling(curi);

			// consider errors which halt further processing
			if (isDispositiveFailure(curi)) {
				failureDisposition(curi);
				return;
			}

			// handle any prerequisites
			if (curi.getAList().containsKey(A_PREREQUISITE_URI)) {
				handlePrerequisites(curi);
				return;
			}
			// handle http headers 
			if (curi.getAList().containsKey(A_HTTP_HEADER_URIS)) {
				handleHttpHeaders(curi);
			}
			// handle embeds 
			if (curi.getAList().containsKey(A_HTML_EMBEDS)) {
				handleEmbeds(curi);
			}
			// handle links, if not too deep
			if ((maxLinkDepth >= 0)
				 && (curi.getAList().getInt(A_DISTANCE_FROM_SEED) < maxLinkDepth)
				 && curi.getAList().containsKey(A_HTML_LINKS)) {
				handleLinks(curi);
			}
			
			
			// SUCCESS: note & log
			successDisposition(curi);	
		} 
			
	}


	/**
	 * @param curi
	 */
	private void handleHttpHeaders(CrawlURI curi) {
		// treat roughly the same as embeds, with same distance-from-seed
		Collection uris = (Collection)curi.getAList().getObject(A_HTTP_HEADER_URIS);
		Iterator iter = uris.iterator();
		while(iter.hasNext()) {
			String e = (String)iter.next();
			try {
				UURI u = UURI.createUURI(e,curi.getBaseUri());
				if(filtersAccept(u)) {
					store.insertAtHead(u,curi.getAList().getInt("distance-from-seed"));
				}
			} catch (URISyntaxException ex) {
				Object[] array = { curi, e };
				controller.uriErrors.log(Level.INFO,ex.getMessage(), array );
			}
		}
	}


	/**
	 * The CrawlURI has been successfully crawled, and will be
	 * attempted no more. 
	 * 
	 * @param curi
	 */
	protected void successDisposition(CrawlURI curi) {
		completionCount++;
		if ( (completionCount % 50) == 0) {
			logger.info("==========> " +
				completionCount+" <========== HTTP URIs completed");
		}
				
		Object array[] = { curi };
		controller.uriProcessing.log(
			Level.INFO,
			curi.getUURI().getUri().toString(),
			array);
		
		// note that CURI has passed out of scheduling
		curi.setStoreState(URIStoreable.FINISHED);
	}


	/**
	 * Update any scheduling structures with the new information
	 * in this CrawlURI. Chiefly means make necessary arrangements
	 * for no other URIs at the same host to be visited within the
	 * appropriate politeness window. 
	 * 
	 * @param curi
	 */
	protected void updateScheduling(CrawlURI curi) {
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
	}


	/**
	 * Has the CrawlURI suffered a failure which completes
	 * its processing?
	 * 
	 * @param curi
	 * @return
	 */
	private boolean isDispositiveFailure(CrawlURI curi) {
		switch(curi.getFetchStatus()){

			case S_CONNECT_FAILED:					
			case S_CONNECT_LOST:
			case S_DOMAIN_UNRESOLVABLE:
				// network errors; perhaps some of these 
				// should be scheduled for retries
			case S_ROBOTS_PRECLUDED:
			    // they don't want us to have it	
			case S_INTERNAL_ERROR:
				// something unexpectedly bad happened
				return true;
			
			case S_UNATTEMPTED:					
				// this uri is virgin, let it carry on
			default:
				return false;
		}
	}


	protected void handleLinks(CrawlURI curi) {
		Collection links = (Collection)curi.getAList().getObject("html-links");
		Iterator iter = links.iterator();
		while(iter.hasNext()) {
			String l = (String)iter.next();
			try {
				UURI embed = UURI.createUURI(l,curi.getBaseUri());
				if(filtersAccept(embed)) {
					store.insert(embed,curi.getAList().getInt("distance-from-seed")+1);
				} 
			} catch (URISyntaxException ex) {
				Object[] array = { curi, l };
				controller.uriErrors.log(Level.INFO,ex.getMessage(), array );
			}
		}
	}


	protected void handleEmbeds(CrawlURI curi) {
		Collection embeds = (Collection)curi.getAList().getObject("html-embeds");
		Iterator iter = embeds.iterator();
		while(iter.hasNext()) {
			String e = (String)iter.next();
			try {
				UURI embed = UURI.createUURI(e,curi.getBaseUri());
				//if(filtersAccept(embed)) {
					store.insertAtHead(embed,curi.getAList().getInt("distance-from-seed"));
				//}
			} catch (URISyntaxException ex) {
				Object[] array = { curi, e };
				controller.uriErrors.log(Level.INFO,ex.getMessage(), array );
			}
		}
	}


	protected void handlePrerequisites(CrawlURI curi) {
		try {
			UURI prereq = UURI.createUURI(curi.getPrerequisiteUri(),curi.getUURI().getUri());
			if ( curi.getDeferrals() > maxDeferrals ) {
				curi.setFetchStatus(S_PREREQUISITE_FAILURE);
				failureDisposition(curi);
				return;
			}		
			if (!canSchedule(prereq)) {
				// prerequisite cannot be fetched (it's probably already failed)
				// must give up on 
				curi.setFetchStatus(S_PREREQUISITE_FAILURE);
				failureDisposition(curi);
				return;
			}
			curi.getAList().remove("prerequisite-uri");
			store.reinsert(curi);
			store.insertAtHead(prereq,curi.getAList().getInt("distance-from-seed"));
		} catch (URISyntaxException ex) {
			Object[] array = { curi, curi.getPrerequisiteUri() };
			controller.uriErrors.log(Level.INFO,ex.getMessage(), array );
		}
	}
	
	
	/**
	 * Is it possible for the given UURI to be enqueued for
	 * processing? Or is it already precluded/failed/unretryable?
	 * 
	 * @param prereq
	 * @return
	 */
	private boolean canSchedule(UURI u) {
		CrawlURI curi = store.getExistingCrawlURI(u);
		if (curi == null) {
			// can always try scheduling a new CrawlURI
			return true;
		}
		if (curi.getStoreState()==URIStoreable.FINISHED) {
			// it's retired
			return false;
		}
		// otherwise, OK
		return true;
	}


	/**
	 * The CrawlURI has encountered a problem, and will not
	 * be retried. 
	 * 
	 * @param curi
	 */
	protected void failureDisposition(CrawlURI curi) {
		// send to basic log 
		Object array[] = { curi };
		controller.uriProcessing.log(
			Level.INFO,
			curi.getUURI().getUri().toString(),
			array);

         // if exception, also send to crawlErrors
		if(curi.getFetchStatus()==S_INTERNAL_ERROR) {
			controller.crawlErrors.log(
			    Level.INFO,
			    curi.getUURI().getUri().toString(),
				array);
		}
	}



	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.URISelector#initialize(org.archive.crawler.framework.CrawlController)
	 */
	public void initialize(CrawlController c) {
		controller = c;
		store = (SimpleStore)c.getStore();
		maxLinkDepth = controller.getOrder().getBehavior().getIntAt("//limits/max-link-depth/@value");
	
		instantiateAllInto(XP_FILTERS,filters);
		Iterator iter = filters.iterator();
		while(iter.hasNext()) {
			Object o = iter.next();
			Filter f = (Filter)o;
			f.initialize();
		}
	}
	
	/**
	 * Do all specified filters (if any) accept this CrawlURI? 
	 *  
	 * @param curi
	 * @return
	 */
	protected boolean filtersAccept(Object o) {
		if (filters.isEmpty()) {
			return true;
		}
		Iterator iter = filters.iterator();
		while(iter.hasNext()) {
			Filter f = (Filter)iter.next();
			if( !f.accepts(o) ) {
				logger.fine(f+" rejected "+o);
				return false; 
			}
		}
		return true;
	}


}
