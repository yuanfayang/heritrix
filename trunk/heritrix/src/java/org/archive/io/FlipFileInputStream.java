/* Copyright (C) 2003 Internet Archive.
 *
 * This file is part of the Heritrix web crawler (crawler.archive.org).
 *
 * Heritrix is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * any later version.
 *
 * Heritrix is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with Heritrix; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
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

	/* (non-Javadoc)
	 * @see java.io.InputStream#read(byte[])
	 */
	public int read(byte[] b) throws IOException {
		int count;
		if (inStream==null || (count = inStream.read(b)) == -1 ) {
			getNewInStream();
			if((count = inStream.read(b)) == -1) {
				// if both old and new stream were exhausted, return EOF
				return -1;
			}
		}
		return count;
	}


	/* (non-Javadoc)
	 * @see java.io.InputStream#read(byte[], int, int)
	 */
	public int read(byte[] b, int off, int len) throws IOException {
		int count;
		if (inStream==null || (count = inStream.read(b,off,len)) == -1 ) {
			getNewInStream();
			if((count = inStream.read(b,off,len)) == -1) {
				// if both old and new stream were exhausted, return EOF
				return -1;
			}
		}
		return count;
	}


	/**
	 * 
	 */
	private void getNewInStream() throws FileNotFoundException, IOException {
		if(inStream!=null) {
			inStream.close();
		} 
		inStream = new BufferedInputStream(new FileInputStream(source.getInputFile()),4096);
	}

	/* (non-Javadoc)
	 * @see java.io.InputStream#close()
	 */
	public void close() throws IOException {
		super.close();
		inStream.close();
	}
}
