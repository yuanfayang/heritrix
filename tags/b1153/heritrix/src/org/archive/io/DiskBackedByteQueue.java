/*
 * DiskBackedByteQueue.java
 * Created on Oct 14, 2003
 *
 * $Header$
 */
package org.archive.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * FIFO byte queue, using disk space as needed, up to provided
 * maximum queue size. 
 * 
 * Flips between two backing files: as soon as reading head 
 * reached beginning of one, writing tail flips to other. SO,
 * avoid using for small amounts of data that are immediately
 * read -- a lot of 
 * 
 * @author gojomo
 *
 */
public class DiskBackedByteQueue {
	long maxSize;
	FlipFileInputStream headStream;
	FlipFileOutputStream tailStream;

	public DiskBackedByteQueue(File tempDir, String backingFilenamePrefix) throws FileNotFoundException {
		super();
		tailStream = new FlipFileOutputStream(tempDir,backingFilenamePrefix);
		headStream = new FlipFileInputStream(tailStream);
	}

	/**
	 * @return
	 */
	public InputStream getHeadStream() {
		return headStream;
	}

	/**
	 * @return
	 */
	public OutputStream getTailStream() {
		return tailStream;
	}

	/**
	 * 
	 */
	public void close() throws IOException {
		headStream.close();
		tailStream.close();
	}

	public void discard() {
		tailStream.discard();
	}

}
