/*
 * SimplePostselector.java
 * Created on Oct 2, 2003
 *
 * $Header$
 */
package org.archive.crawler.basic;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.framework.Processor;

/**
 * Determine which links etc get fed back into Frontier,
 * if/when failures get retried, etc.
 * 
 * 
 * @author gojomo
 *
 */
public class Postselector extends Processor implements CoreAttributeConstants, FetchStatusCodes {
	private static Logger logger = Logger.getLogger("org.archive.crawler.basic.Postselector");

	// limits on retries TODO: separate into retryPolicy? 
	private int maxDeferrals = 10; // should be at least max-retries plus 3 or so

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Processor#innerProcess(org.archive.crawler.datamodel.CrawlURI)
	 */
	protected void innerProcess(CrawlURI curi) {
		logger.finest(getName()+" processing "+curi);
		
		// handle any prerequisites
		if (curi.getAList().containsKey(A_PREREQUISITE_URI)) {
			handlePrerequisites(curi);
			return;
		}
		
		URI baseUri = getBaseURI(curi);			
		// handle http headers 
		if (curi.getAList().containsKey(A_HTTP_HEADER_URIS)) {
			handleHttpHeaders(curi, baseUri);
		}
		// handle embeds 
		if (curi.getAList().containsKey(A_HTML_EMBEDS)) {
			handleEmbeds(curi, baseUri);
		}
		// handle links
		if (curi.getAList().containsKey(A_HTML_LINKS)) {
			handleLinks(curi, baseUri);
		}
	}

	/**
	 * @param curi
	 */
	private URI getBaseURI(CrawlURI curi) {
		if (!curi.getAList().containsKey(A_HTML_BASE)) {
			return curi.getUURI().getUri();
		}
		String base = curi.getAList().getString(A_HTML_BASE);
		try {
			return UURI.createUURI(base).getUri();
		} catch (URISyntaxException e) {
			Object[] array = { this, base };
			controller.uriErrors.log(Level.INFO,e.getMessage(), array );
			// next best thing: use self
			return curi.getUURI().getUri();
		}
	}

	protected void handlePrerequisites(CrawlURI curi) {
		try {
			UURI prereq = UURI.createUURI(curi.getPrerequisiteUri(),curi.getUURI().getUri());
			CandidateURI caUri = new CandidateURI(prereq);
			caUri.setVia(curi);
			caUri.setPathFromSeed(curi.getPathFromSeed()+"P");
			
			if ( curi.getDeferrals() > maxDeferrals ) {
				// too many deferrals, equals failure
				curi.setFetchStatus(S_PREREQUISITE_FAILURE);
				//failureDisposition(curi);
				return;
			}		
			if (!scheduleHigh(caUri)) {
				// prerequisite cannot be scheduled (perhaps excluded by scope)
				// must give up on 
				curi.setFetchStatus(S_PREREQUISITE_FAILURE);
				//failureDisposition(curi);
				return;
			}
			// TODO possibly offload prerq-chaining to frontier (resuscitate 'held' facility)
			curi.getAList().remove(A_PREREQUISITE_URI);
		} catch (URISyntaxException ex) {
			Object[] array = { curi, curi.getPrerequisiteUri() };
			controller.uriErrors.log(Level.INFO,ex.getMessage(), array );
		}
	}

	/**
	 * @param prereq
	 * @return
	 */
	private boolean scheduleHigh(CandidateURI caUri) {
		if(controller.getScope().accepts(caUri)) {
			logger.finer("URI accepted: "+caUri);
			controller.getFrontier().scheduleHigh(caUri);
			return true;
		}
		logger.finer("URI rejected: "+caUri);
		//boolean test = ((Scope)controller.getScope()).getFocusFilter().accepts(caUri);
		//test = ((Scope)controller.getScope()).getTransitiveFilter().accepts(caUri);
		//test = ((Scope)controller.getScope()).getExcludeFilter().accepts(caUri);
		return false;
	}

	/**
	 * @param prereq
	 * @return
	 */
	private boolean schedule(CandidateURI caUri) {
		if(controller.getScope().accepts(caUri)) {
			logger.finer("URI accepted: "+caUri);
			controller.getFrontier().schedule(caUri);
			return true;
		}
		logger.finer("URI rejected: "+caUri);
		//controller.getScope().accepts(caUri);
		return false;
	}

	/**
	 * @param curi
	 */
	private void handleHttpHeaders(CrawlURI curi, URI baseUri) {
		// treat roughly the same as embeds, with same distance-from-seed
		Collection uris = (Collection)curi.getAList().getObject(A_HTTP_HEADER_URIS);
		Iterator iter = uris.iterator();
		while(iter.hasNext()) {
			String r = (String)iter.next();
			try {
				UURI u = UURI.createUURI(r,baseUri);
				CandidateURI caUri = new CandidateURI(u);
				caUri.setVia(curi);
				caUri.setPathFromSeed(curi.getPathFromSeed()+"R");
				logger.finest("inserting header at head "+u);
				scheduleHigh(caUri);
			} catch (URISyntaxException ex) {
				Object[] array = { curi, r };
				controller.uriErrors.log(Level.INFO,ex.getMessage(), array );
			}
		}
	}
	
	protected void handleLinks(CrawlURI curi, URI baseUri) {
		if (curi.getFetchStatus() >= 400) {
			// do not follow links of error pages
			return;
		}
		Collection links = (Collection)curi.getAList().getObject(A_HTML_LINKS);
		Iterator iter = links.iterator();
		while(iter.hasNext()) {
			String l = (String)iter.next();
			try {
				UURI link = UURI.createUURI(l,baseUri);
				CandidateURI caUri = new CandidateURI(link);
				caUri.setVia(curi);
				caUri.setPathFromSeed(curi.getPathFromSeed()+"L");
				logger.finest("inserting link at head "+link);
				schedule(caUri);
			} catch (URISyntaxException ex) {
				Object[] array = { curi, l };
				controller.uriErrors.log(Level.INFO,ex.getMessage(), array );
			}
		}
	}


	protected void handleEmbeds(CrawlURI curi, URI baseUri) {
		if (curi.getFetchStatus() >= 400) {
			// do not follow links of error pages
			return;
		}
		Collection embeds = (Collection)curi.getAList().getObject(A_HTML_EMBEDS);
		Iterator iter = embeds.iterator();
		while(iter.hasNext()) {
			String e = (String)iter.next();
			try {
				UURI embed = UURI.createUURI(e,baseUri);
				CandidateURI caUri = new CandidateURI(embed);
				caUri.setVia(curi);
				caUri.setPathFromSeed(curi.getPathFromSeed()+"E");
				logger.finest("inserting embed at head "+embed);
				schedule(caUri);
			} catch (URISyntaxException ex) {
				Object[] array = { curi, e };
				controller.uriErrors.log(Level.INFO,ex.getMessage(), array );
			}
		}
	}




}
