/*
 * KeyedQueue.java
 * Created on May 29, 2003
 *
 * $Header$
 */
package org.archive.crawler.basic;

import java.util.LinkedList;

/**
 * Ordered collection of items with the same "classKey". The 
 * collection itself has a state, which may reflect where it
 * is stored or what can be done with the contained items.
 * 
 * @author gojomo
 *
 */
public class KeyedQueue extends LinkedList implements URIStoreable, Comparable {
	public static Object READY = new Object();
	public static Object HOLDING = new Object();
	public static Object SLEEPING = new Object();
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
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Object other) {
		if(this==other) {
			return 0; // for exact identity only
		}
		if (((KeyedQueue)other).getWakeTime()> wakeTime) {
			return -1;
		} 
		if (((KeyedQueue)other).getWakeTime()< wakeTime) {
			return 1;
		} 
		if (other.hashCode() > hashCode()) {
			return -1;
		} else {
			return 1;
		}
	}
	
	

}
