/*
 * FlipFileInputStream.java
 * Created on Oct 14, 2003
 *
 * $Header$
 */
package org.archive.io;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author gojomo
 *
 */
public class FlipFileInputStream extends InputStream {
	FlipFileOutputStream source;
	InputStream inStream;
	
	/**
	 * @param tailStream
	 */
	public FlipFileInputStream(FlipFileOutputStream tailStream) {
		source = tailStream;
	}

	/* (non-Javadoc)
	 * @see java.io.InputStream#read()
	 */
	public int read() throws IOException {
		int c;
		if(inStream==null || (c = inStream.read()) == -1) {
			getNewInStream();
			if((c = inStream.read()) == -1) {
				// if both old and new streams were exhausted, return EOF
				return -1;
			}
		}
		return c;
	}

	/**
	 * 
	 */
	private void getNewInStream() throws FileNotFoundException, IOException {
		inStream = new BufferedInputStream(new FileInputStream(source.getInputFile()),4096);
	}

}
