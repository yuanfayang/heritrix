/*
 * URIDB.java
 * Created on May 14, 2003
 *
 * $Header$
 */
package org.archive.crawler.framework;

import org.archive.crawler.datamodel.*;

/**
 * Handles all persistence for Scheduler and Selector, allowing
 * them to be stateless (and somewhat indifferent to the strategies
 * used for giant URI queues/sets).
 * 
 * @author gojomo
 *
 */
public interface URIStore {
	public void enqueueTo(AnnotatedURI auri, Object key);
	public void dequeueFrom(AnnotatedURI auri, Object key);
	public void pushTo(AnnotatedURI auri, Object key);
	public void popFrom(AnnotatedURI auri, Object key);
	public void peekFrom(AnnotatedURI auri, Object key);
	public long count(Object key);
	public long countFrom(Object key);
	
}
