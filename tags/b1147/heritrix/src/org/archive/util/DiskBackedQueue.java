/* 
 * DiskBackedQueue.java
 * Created on Oct 16, 2003
 *
 * $Header$
 */
package org.archive.util;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;


/**
 * Queue which uses a DiskQueue ('tailQ') for spillover entries once a 
 * MemQueue ('headQ') reaches a maximum size. 
 * 
 * 
 * @author Gordon Mohr
 */
public class DiskBackedQueue implements Queue {
	private static Logger logger = Logger.getLogger("org.archive.util.DiskBackedQueue");

	int headMax;
	MemQueue headQ;
	DiskQueue tailQ;
	String name;
	
	/**
	 * 
	 */
	public DiskBackedQueue(File dir, String name, int headMax) throws IOException {
		this.headMax = headMax;
		this.name = name;
		headQ = new MemQueue();
		tailQ = new DiskQueue(dir, name);
	}

	/* (non-Javadoc)
	 * @see org.archive.util.Queue#enqueue(java.lang.Object)
	 */
	public void enqueue(Object o) {
		logger.finest(name+"("+length()+"): "+o);
		if (length()<headMax) {
			fillHeadQ();
			headQ.enqueue(o);
		} else {
			tailQ.enqueue(o);
		}
	}

	/* (non-Javadoc)
	 * @see org.archive.util.Queue#isEmpty()
	 */
	public boolean isEmpty() {
		return length()==0;
	}

	/* (non-Javadoc)
	 * @see org.archive.util.Queue#dequeue()
	 */
	public Object dequeue() {
		if (headQ.isEmpty()) {
			fillHeadQ();
		}
		Object o = headQ.dequeue();
		logger.finest(name+"("+length()+"): "+o);
		return o;
	}

	/**
	 * 
	 */
	private void fillHeadQ() {
		while (headQ.length()<headMax && tailQ.length()>0) {
			headQ.enqueue(tailQ.dequeue());
		}
	}

	/* (non-Javadoc)
	 * @see org.archive.util.Queue#length()
	 */
	public long length() {
		return headQ.length()+tailQ.length();
	}

	/* (non-Javadoc)
	 * @see org.archive.util.Queue#release()
	 */
	public void release() {
		tailQ.release();
	}

}
