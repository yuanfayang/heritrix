/*
 * ReplayInputStream.java
 * Created on Sep 24, 2003
 *
 * $Header$
 */
package org.archive.crawler.io;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author gojomo
 *
 */
public class ReplayInputStream extends InputStream {
	private BufferedInputStream diskStream;
	byte[] buffer;
	long size;
	long position;
	String backingFilename;
	
	/**
	 * @param buffer
	 * @param size
	 * @param backingFilename
	 */
	public ReplayInputStream(byte[] buffer, long size, String backingFilename) throws IOException {
		this.buffer = buffer;
		this.size = size;
		if (size>buffer.length) {
			this.backingFilename = backingFilename;
			diskStream = new BufferedInputStream(new FileInputStream(backingFilename),4096);
		}
	}

	/* (non-Javadoc)
	 * @see java.io.InputStream#read()
	 */
	public int read() throws IOException {
		if (position<buffer.length) {
			return buffer[(int)position++];
		} else {
			position++;
			return diskStream.read();
		}
	}
	
	// TODO: implement other read()s for efficiency

}
