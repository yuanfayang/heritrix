/*
 * FencedInputStream.java
 * Created on Sep 26, 2003
 *
 * $Header$
 */
package org.archive.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author gojomo
 *
 */
public class FencedInputStream extends FilterInputStream {
	long maxToRead;
	long position = 0;
	
	/**
	 * @param in
	 */
	protected FencedInputStream(InputStream in, long maxToRead) {
		super(in);
		this.maxToRead = maxToRead;
	}

	/* (non-Javadoc)
	 * @see java.io.InputStream#read()
	 */
	public int read() throws IOException {
		if (position < maxToRead) {
			int b = super.read();
			if (b>=0) {
				position++;
			}
			return b;
		} else {
			return -1; // virtual EOF
		}
	}

	/* (non-Javadoc)
	 * @see java.io.InputStream#read(byte[], int, int)
	 */
	public int read(byte[] b, int off, int len) throws IOException {
		// TODO Auto-generated method stub
		return super.read(b, off, len);
	}

	/* (non-Javadoc)
	 * @see java.io.InputStream#read(byte[])
	 */
	public int read(byte[] b) throws IOException {
		// TODO Auto-generated method stub
		return super.read(b);
	}

}
