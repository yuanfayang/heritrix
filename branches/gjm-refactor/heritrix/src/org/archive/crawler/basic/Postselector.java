/*
 * SimplePostselector.java
 * Created on Oct 2, 2003
 *
 * $Header$
 */
package org.archive.crawler.basic;

import java.net.URISyntaxException;
import java.util.logging.Level;

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
	// limits on retries TODO: separate into retryPolicy? 
	private int maxDeferrals = 10; // should be at least max-retries plus 3 or so

	

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Processor#innerProcess(org.archive.crawler.datamodel.CrawlURI)
	 */
	protected void innerProcess(CrawlURI curi) {
		// handle any prerequisites
		if (curi.getAList().containsKey(A_PREREQUISITE_URI)) {
			handlePrerequisites(curi);
			return;
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
			controller.getFrontier().scheduleHigh(caUri);
			return true;
		}
		return false;
	}




}
