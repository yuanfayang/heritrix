/*
 * SimpleStore.java
 * Created on May 27, 2003
 *
 * $Header$
 */
package org.archive.crawler.basic;

import java.util.HashMap;
import java.util.LinkedList;

import org.archive.crawler.datamodel.AnnotatedURI;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.URIStore;

/**
 * A minimal in-memory URIStore. Keeps "full" CrawlURI instances
 * around, because it can. 
 * 
 * @author gojomo
 *
 */
public class SimpleStore implements URIStore {
	HashMap allCuris = new HashMap(); // of UURI -> CrawlURI 
	LinkedList pendingCuris = new LinkedList(); // of CrawlURIs 
	HashMap processing = new HashMap(); // of String (queueKey) -> CrawlURI
	HashMap allQueues = new HashMap(); // of String (queueKey) -> KeyedQueue
	LinkedList readyQueues = new LinkedList(); // of KeyedQueues 
	LinkedList snoozeQueues = new LinkedList(); // of KeyedQueues (prob should be Heap)
	
	
	
	

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.URIStore#enqueueTo(org.archive.crawler.datamodel.AnnotatedURI, java.lang.Object)
	 */
	public void enqueueTo(AnnotatedURI auri, Object key) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.URIStore#dequeueFrom(org.archive.crawler.datamodel.AnnotatedURI, java.lang.Object)
	 */
	public void dequeueFrom(AnnotatedURI auri, Object key) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.URIStore#pushTo(org.archive.crawler.datamodel.AnnotatedURI, java.lang.Object)
	 */
	public void pushTo(AnnotatedURI auri, Object key) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.URIStore#popFrom(org.archive.crawler.datamodel.AnnotatedURI, java.lang.Object)
	 */
	public void popFrom(AnnotatedURI auri, Object key) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.URIStore#peekFrom(org.archive.crawler.datamodel.AnnotatedURI, java.lang.Object)
	 */
	public void peekFrom(AnnotatedURI auri, Object key) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.URIStore#count(java.lang.Object)
	 */
	public long count(Object key) {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.URIStore#countFrom(java.lang.Object)
	 */
	public long countFrom(Object key) {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.URIStore#initialize(org.archive.crawler.framework.CrawlController)
	 */
	public void initialize(CrawlController c) {
		// TODO Auto-generated method stub

	}

	/**
	 * @param uuri
	 */
	public void insertAsSeed(UURI uuri) {
		// TODO Auto-generated method stub
		
	}

}
