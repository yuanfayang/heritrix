/*
 * MemQueue.java
 * Created on Oct 14, 2003
 *
 * $Header$
 */
package org.archive.util;

import java.util.LinkedList;

/**
 * @author gojomo
 *
 */
public class MemQueue extends LinkedList implements Queue {

	public void enqueue(Object o) {
		add(o);
	}

	/* (non-Javadoc)
	 * @see org.archive.util.Queue#dequeue()
	 */
	public Object dequeue() {
		return removeFirst();
	}

}
