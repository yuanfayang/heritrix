/*
 * KeyedQueue.java
 * Created on May 29, 2003
 *
 * $Header$
 */
package org.archive.crawler.basic;

import java.util.LinkedList;
import java.util.logging.Logger;

/**
 * Ordered collection of items with the same "classKey". The 
 * collection itself has a state, which may reflect where it
 * is stored or what can be done with the contained items.
 * 
 * @author gojomo
 *
 */
public class KeyedQueue extends LinkedList implements URIStoreable {
	private static Logger logger = Logger.getLogger("org.archive.crawler.basic.KeyedQueue");

	long wakeTime;
	Object classKey;
	Object state;
	
	/**
	 * 
	 */
	public KeyedQueue(Object key) {
		super();
		classKey = key;
	}
	
	/**
	 * @return
	 */
	public boolean isReady() {
		return System.currentTimeMillis() > wakeTime;
	}

	/**
	 * 
	 */
	public Object getClassKey() {
		return classKey;
	}


	/* (non-Javadoc)
	 * @see org.archive.crawler.basic.URIStoreable#getStoreState()
	 */
	public Object getStoreState() {
		return state;
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.basic.URIStoreable#setStoreState(java.lang.Object)
	 */
	public void setStoreState(Object s) {
		state=s;
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.basic.URIStoreable#getWakeTime()
	 */
	public long getWakeTime() {
		return wakeTime;
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.basic.URIStoreable#setWakeTime(long)
	 */
	public void setWakeTime(long w) {
		wakeTime = w;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "KeyedQueue[classKey="+getClassKey()+"]";
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.basic.URIStoreable#getSortFallback()
	 */
	public String getSortFallback() {
		return classKey.toString();
	}

}
