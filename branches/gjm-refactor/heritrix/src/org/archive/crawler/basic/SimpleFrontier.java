/*
 * SimpleFrontier.java
 * Created on Oct 1, 2003
 *
 * $Header$
 */
package org.archive.crawler.basic;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.*;
import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FatalConfigurationException;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.datamodel.UURISet;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.URIFrontier;

/**
 * A basic in-memory mostly breadth-first frontier, which 
 * refrains from emitting more than one CrawlURI of the same 
 * 'key' (host) at once, and respects minimum-delay and 
 * delay-factor specifications for politeness
 * 
 * @author gojomo
 *
 */
public class SimpleFrontier implements URIFrontier {
	private static Logger logger = Logger.getLogger("org.archive.crawler.basic.SimpleFrontier");
		
	HashMap allCuris = new HashMap(); // of UURI -> CrawlURI 
	
	UURISet alreadyIncluded = new MemFPUURISet();
	
	// every CandidateURI not yet in process or another queue; 
	// all seeds start here; may contain duplicates
	LinkedList pendingQueue = new LinkedList(); // of CandidateURIs 

	



	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.URIFrontier#initialize(org.archive.crawler.framework.CrawlController)
	 */
	public void initialize(CrawlController c)
		throws FatalConfigurationException {

		Iterator iter = c.getOrder().getBehavior().getSeeds().iterator();
		while (iter.hasNext()) {
			UURI u = (UURI) iter.next();
			CandidateURI caUri = new CandidateURI(u);
			caUri.setSeed(true);
			schedule(caUri);
		}

	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.URIFrontier#schedule(org.archive.crawler.datamodel.CandidateURI)
	 */
	public void schedule(CandidateURI caUri) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.URIFrontier#next(int)
	 */
	public CrawlURI next(int timeout) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.URIFrontier#finished(org.archive.crawler.datamodel.CrawlURI)
	 */
	public void finished(CrawlURI curi) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.URIFrontier#isEmpty()
	 */
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.URIFrontier#size()
	 */
	public long size() {
		// TODO Auto-generated method stub
		return 0;
	}

}
