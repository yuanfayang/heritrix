/* Copyright (C) 2003 Internet Archive.
 *
 * This file is part of the Heritrix web crawler (crawler.archive.org).
 *
 * Heritrix is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * any later version.
 *
 * Heritrix is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with Heritrix; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
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
 * Determine which extracted links etc get fed back into Frontier.
 *
 * Could in the future also control whether current URI is retried. 
 * 
 * @author gojomo
 *
 */
public class Postselector extends Processor implements CoreAttributeConstants, FetchStatusCodes {
    private static Logger logger = Logger.getLogger("org.archive.crawler.basic.Postselector");

	// limits on retries TODO: separate into retryPolicy? 
	private int maxDeferrals = 10; // should be at least max-retries plus 3 or so

    /**
     * @param name
     * @param description
     */
    public Postselector(String name) {
        super(name, "Post selector");
    }

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
		// handle speculative embeds 
		if (curi.getAList().containsKey(A_HTML_SPECULATIVE_EMBEDS)) {
			handleSpeculativeEmbeds(curi, baseUri);
		}
		// handle links
		if (curi.getAList().containsKey(A_HTML_LINKS)) {
			handleLinks(curi, baseUri);
		}
		// handle css links
		if (curi.getAList().containsKey(A_CSS_LINKS)) {
			handleCSSLinks(curi, baseUri);
		}

	}

	/**
	 * @param curi
	 * @param baseUri
	 */
	private void handleSpeculativeEmbeds(CrawlURI curi, URI baseUri) {
		if (curi.getFetchStatus() >= 400) {
			// do not follow links of error pages
			return;
		}
		Collection embeds = (Collection)curi.getAList().getObject(A_HTML_SPECULATIVE_EMBEDS);
		Iterator iter = embeds.iterator();
		while(iter.hasNext()) {
			String e = (String)iter.next();
			try {
				UURI embed = UURI.createUURI(e,baseUri);
				CandidateURI caUri = new CandidateURI(embed);
				caUri.setVia(curi);
				char pathSuffix = caUri.sameDomainAs(curi) ? 'D' : 'X'; 
				caUri.setPathFromSeed(curi.getPathFromSeed()+pathSuffix);
				logger.finest("inserting speculative embed at head "+embed);
				schedule(caUri);
			} catch (URISyntaxException ex) {
				Object[] array = { curi, e };
				controller.uriErrors.log(Level.INFO,ex.getMessage(), array );
			}
		}
	}

	/**
	 * @param curi
	 */
	private URI getBaseURI(CrawlURI curi) {
		if (!curi.getAList().containsKey(A_HTML_BASE)) {
			return curi.getUURI().getRawUri();
		}
		String base = curi.getAList().getString(A_HTML_BASE);
		try {
			return UURI.createUURI(base).getRawUri();
		} catch (URISyntaxException e) {
			Object[] array = { curi, base };
			controller.uriErrors.log(Level.INFO,e.getMessage(), array );
			// next best thing: use self
			return curi.getUURI().getRawUri();
		}
	}

	protected void handlePrerequisites(CrawlURI curi) {
		try {
			if ( curi.getDeferrals() > maxDeferrals ) {
				// too many deferrals, equals failure
				curi.setFetchStatus(S_PREREQUISITE_FAILURE);
				//failureDisposition(curi);
				return;
			}	
			
			UURI prereq = UURI.createUURI((String) curi.getPrerequisiteUri(),getBaseURI(curi));
			curi.setPrerequisiteUri(prereq); // convert to UURI for convenience of Frontier
			CandidateURI caUri = new CandidateURI(prereq);
			caUri.setVia(curi);
			caUri.setPathFromSeed(curi.getPathFromSeed()+"P");

			if(curi.hasForcedPrerequisiteUri()) {
				// This URI should be fetched even though it is in the
				// alreadyIncluded map.
				caUri.setForceFetch(true);
			}
			
			if (!scheduleHigh(caUri)) {
				// prerequisite cannot be scheduled (perhaps excluded by scope)
				// must give up on 
				curi.setFetchStatus(S_PREREQUISITE_FAILURE);
				//failureDisposition(curi);
				return;
			}
			// leave PREREQ in place so frontier can properly defer this curi
		} catch (URISyntaxException ex) {
			Object[] array = { curi, curi.getPrerequisiteUri() };
			controller.uriErrors.log(Level.INFO,ex.getMessage(), array );
		}
	}

	/**
	 * Schedule the given {@link CandidateURI CandidateURI} with the Frontier as a 
	 * "high" priority item (such as a prerequisite or embedded resource which should 
	 * be fetched in an expedited manner). 
	 * 
	 * @param caUri The CandidateURI to be scheduled
	 * 
	 * @return true if CandidateURI was accepted by crawl scope, false otherwise
	 */
	private boolean scheduleHigh(CandidateURI caUri) {
		if(controller.getScope().accepts(caUri)) {
			logger.finer("URI accepted: "+caUri);
			controller.getFrontier().batchScheduleHigh(caUri);
			return true;
		}
		logger.finer("URI rejected: "+caUri);
		//boolean test = ((Scope)controller.getScope()).getFocusFilter().accepts(caUri);
		//test = ((Scope)controller.getScope()).getTransitiveFilter().accepts(caUri);
		//test = ((Scope)controller.getScope()).getExcludeFilter().accepts(caUri);
		return false;
	}

	/**
	 * Schedule the given {@link CandidateURI CandidateURI} with the Frontier.
	 * 
	 * @param caUri The CandidateURI to be scheduled
	 * 
	 * @return true if CandidateURI was accepted by crawl scope, false otherwise
	 */
	private boolean schedule(CandidateURI caUri) {
		if(controller.getScope().accepts(caUri)) {
			logger.finer("URI accepted: "+caUri);
			controller.getFrontier().batchSchedule(caUri);
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

	protected void handleCSSLinks(CrawlURI curi, URI baseUri) {
		// treat same as embedded links
		if (curi.getFetchStatus() >= 400) {
			// do not follow links of error pages
			return;
		}
		Collection links = (Collection) curi.getAList().getObject(A_CSS_LINKS);
		if (links == null) {
			return;
		}
		Iterator iter = links.iterator();
		while (iter.hasNext()) {
			String e = (String) iter.next();
			try {
				UURI embed = UURI.createUURI(e, baseUri);
				CandidateURI caUri = new CandidateURI(embed);
				caUri.setVia(curi);
				char pathSuffix = caUri.sameDomainAs(curi) ? 'D' : 'E';
				caUri.setPathFromSeed(curi.getPathFromSeed() + pathSuffix);
				logger.finest("inserting embed at head " + embed);
				schedule(caUri);
			} catch (URISyntaxException ex) {
				Object[] array = { curi, e };
				controller.uriErrors.log(Level.INFO, ex.getMessage(), array);
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
				char pathSuffix = caUri.sameDomainAs(curi) ? 'D' : 'E'; 
				caUri.setPathFromSeed(curi.getPathFromSeed()+pathSuffix);
				logger.finest("inserting embed at head "+embed);
				schedule(caUri);
			} catch (URISyntaxException ex) {
				Object[] array = { curi, e };
				controller.uriErrors.log(Level.INFO,ex.getMessage(), array );
			}
		}
	}




}
