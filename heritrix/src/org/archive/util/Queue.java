/*
 * Queue.java
 * Created on Oct 14, 2003
 *
 * $Header$
 */
package org.archive.util;


/**
 * @author gojomo
 *
 */
public interface Queue {

	/**
	 * @param caUri
	 */
	void enqueue(Object o);

	/**
	 * @return
	 */
	boolean isEmpty();

	/**
	 * @return
	 */
	Object dequeue();

	long length();

	/**
	 * release any OS/IO resources associated with Queue
	 */
	void release();
}
