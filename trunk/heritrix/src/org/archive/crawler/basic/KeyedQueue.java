/*
 * KeyedQueue.java
 * Created on May 29, 2003
 *
 * $Header$
 */
package org.archive.crawler.basic;

import java.util.LinkedList;

/**
 * @author gojomo
 *
 */
public class KeyedQueue extends LinkedList implements URIStoreable {
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
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.basic.URIStoreable#setStoreState(java.lang.Object)
	 */
	public void setStoreState(Object s) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.basic.URIStoreable#getWakeTime()
	 */
	public long getWakeTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.basic.URIStoreable#setWakeTime(long)
	 */
	public void setWakeTime(long w) {
		// TODO Auto-generated method stub
		
	}
	
	

}
