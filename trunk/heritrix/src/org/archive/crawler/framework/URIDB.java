/*
 * URIDB.java
 * Created on May 14, 2003
 *
 * $Header$
 */
package org.archive.crawler.framework;

/**
 * @author gojomo
 *
 */
public interface URIDB {
	public void enqueueTo(AnnotatedURI auri, Object key);
	public void dequeueFrom(AnnotatedURI auri, Object key);
	public void pushTo(AnnotatedURI auri, Object key);
	public void popFrom(AnnotatedURI auri, Object key);
	public void peekFrom(AnnotatedURI auri, Object key);
	public long count(Object key);
	public long countFrom(Object key);
	
}
