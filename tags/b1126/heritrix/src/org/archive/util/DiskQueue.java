/* 
 * DiskQueue.java
 * Created on Oct 16, 2003
 *
 * $Header$
 */
package org.archive.util;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

import org.archive.io.DiskBackedByteQueue;

/**
 * 
 * @author Gordon Mohr
 */
public class DiskQueue implements Queue {
	private static Logger logger = Logger.getLogger("org.archive.util.DiskQueue");
	String name;
	long length;
	DiskBackedByteQueue bytes;
	ObjectOutputStream tailStream;
	ObjectInputStream headStream;
	
	/**
	 * 
	 */
	public DiskQueue(File dir, String name) throws IOException {
		bytes = new DiskBackedByteQueue(dir,name);
		tailStream = new ObjectOutputStream(bytes.getTailStream());

		headStream = new ObjectInputStream(bytes.getHeadStream());
		length = 0;
		this.name = name;
		// TODO someday: enable queue to already be filled
	}

	/* (non-Javadoc)
	 * @see org.archive.util.Queue#enqueue(java.lang.Object)
	 */
	public void enqueue(Object o){
		logger.finest(name+"("+length+"): "+o);
		try {
			tailStream.writeObject(o);
			tailStream.reset(); // forget state with each enqueue
		} catch (IOException e) {
			// TODO convert to runtime exception
			e.printStackTrace();
		}
		length++;
	}

	/* (non-Javadoc)
	 * @see org.archive.util.Queue#isEmpty()
	 */
	public boolean isEmpty() {
		return length==0;
	}

	/* (non-Javadoc)
	 * @see org.archive.util.Queue#dequeue()
	 */
	public Object dequeue() {
		Object o;
		try {
			o = headStream.readObject();
		} catch (IOException e) {
			e.printStackTrace();
			throw new NoSuchElementException();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new NoSuchElementException();
		}
		logger.finest(name+"("+length+"): "+o);
		length--;
		return o;
	}

	/* (non-Javadoc)
	 * @see org.archive.util.Queue#length()
	 */
	public long length() {
		return length;
	}

}
