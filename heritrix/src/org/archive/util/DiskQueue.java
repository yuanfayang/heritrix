/* 
 * DiskQueue.java
 * Created on Oct 16, 2003
 *
 * $Header$
 */
package org.archive.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.io.DiskBackedByteQueue;
import org.archive.io.NullOutputStream;

/**
 * Queue which stores all its objects to disk using object
 * serialization, on top of a DiskBackedByteQueue. 
 *
 * The serialization state is reset after each enqueue(). 
 * Care should be taken not to enqueue() items which will
 * pull out excessive referenced objects, or objects which
 * will be redundantly reinstantiated upon dequeue() from 
 * disk. 
 * 
 * @author Gordon Mohr
 */
public class DiskQueue implements Queue {
	private static Logger logger = Logger.getLogger("org.archive.util.DiskQueue");

	private File scratchDir;
	String name;
	long length;
	DiskBackedByteQueue bytes;
	ObjectOutputStream testStream; // to verify that object is serializable
	ObjectOutputStream tailStream;
	ObjectInputStream headStream;
	
	/**
	 * 
	 */
	public DiskQueue(File dir, String name) {
		length = 0;
		this.name = name;
		this.scratchDir = dir;
		// TODO someday: enable queue to already be filled
	}
	
	private void lateInitialize() throws FileNotFoundException, IOException {
		bytes = new DiskBackedByteQueue(scratchDir,this.name);
		testStream = new ObjectOutputStream(new NullOutputStream());
		tailStream = new ObjectOutputStream(bytes.getTailStream());
		headStream = new ObjectInputStream(bytes.getHeadStream());
	}

	/* (non-Javadoc)
	 * @see org.archive.util.Queue#enqueue(java.lang.Object)
	 */
	public void enqueue(Object o){
		//logger.finest(name+"("+length+"): "+o);
		try {
			if(bytes==null) {
				lateInitialize();
			}
			// TODO: optimize this, for example by serializing to buffer, then writing to disk on success
			testStream.writeObject(o);
			testStream.reset();
			tailStream.writeObject(o);
			tailStream.reset(); // forget state with each enqueue
			length++;
		} catch (IOException e) {
			// TODO convert to runtime exception?
			DevUtils.logger.log(Level.SEVERE,"enqueue("+o+")"+DevUtils.extraInfo(),e);
		}
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
		if (isEmpty()) {
			throw new NoSuchElementException();
		}
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
		// logger.finest(name+"("+length+"): "+o);
		length--;
		return o;
	}

	/* (non-Javadoc)
	 * @see org.archive.util.Queue#length()
	 */
	public long length() {
		return length;
	}

	/* (non-Javadoc)
	 * @see org.archive.util.Queue#release()
	 */
	public void release() {
		if (bytes != null) {
			try {
				headStream.close();
				tailStream.close();
				bytes.discard();

			} catch (IOException e) {
				// TODO: convert to runtime? 
				e.printStackTrace();
			}
		}
	}

}
