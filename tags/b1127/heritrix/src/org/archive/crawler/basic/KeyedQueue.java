/*
 * KeyedQueue.java
 * Created on May 29, 2003
 *
 * $Header$
 */
package org.archive.crawler.basic;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.archive.util.DiskBackedQueue;
import org.archive.util.MemQueue;
import org.archive.util.Queue;

/**
 * Ordered collection of items with the same "classKey". The 
 * collection itself has a state, which may reflect where it
 * is stored or what can be done with the contained items.
 * 
 * @author gojomo
 *
 */
public class KeyedQueue implements Queue, URIStoreable {
	private static Logger logger = Logger.getLogger("org.archive.crawler.basic.KeyedQueue");

	long wakeTime;
	Object classKey;
	Object state;
	
	Queue innerQ;
	
	/**
	 * 
	 */
	public KeyedQueue(Object key, File scratchDir, int headMax) {
		super();
		classKey = key;
		String tmpName = null;
		if (key instanceof String) {
			tmpName = (String) key;
		}
		innerQ = new MemQueue();
//		try {
//			innerQ = new DiskBackedQueue(scratchDir,tmpName,headMax);
//		} catch (IOException e) {
//			// TODO Convert to runtime exception?
//			e.printStackTrace();
//		}
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

	/**
	 * The only equals() that matters for KeyedQueues is
	 * object equivalence.
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object o) {
		return this == o;
	}

	/* (non-Javadoc)
	 * @see org.archive.util.Queue#enqueue(java.lang.Object)
	 */
	public void enqueue(Object o) {
		innerQ.enqueue(o);
	}

	/* (non-Javadoc)
	 * @see org.archive.util.Queue#isEmpty()
	 */
	public boolean isEmpty() {
		return innerQ.isEmpty();
	}

	/* (non-Javadoc)
	 * @see org.archive.util.Queue#dequeue()
	 */
	public Object dequeue() {
		return innerQ.dequeue();
	}

	/* (non-Javadoc)
	 * @see org.archive.util.Queue#length()
	 */
	public long length() {
		return innerQ.length();
	}

}
