/*
 * ReplayInputStream.java
 * Created on Sep 24, 2003
 *
 * $Header$
 */
package org.archive.io;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Replays the bytes recorded from a RecordingInputStream or
 * RecordingOutputStream. 
 * 
 * @author gojomo
 *
 */
public class ReplayInputStream extends InputStream {
	protected BufferedInputStream diskStream;
	protected byte[] buffer;
	protected long size;
	protected long responseBodyStart; // where the response body starts, if marked
	protected long position;
	protected String backingFilename;
	
	/**
	 * @param buffer
	 * @param size
	 * @param responseBodyStart
	 * @param backingFilename
	 */
	public ReplayInputStream(byte[] buffer, long size, long responseBodyStart, String backingFilename) throws IOException {
		this(buffer,size,backingFilename);
		this.responseBodyStart = responseBodyStart;
	}

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

	public long setToResponseBodyStart() {
		position = responseBodyStart;
		return position;
	}
	
	/* (non-Javadoc)
	 * @see java.io.InputStream#read()
	 */
	public int read() throws IOException {
		if (position==size) {
			return -1; // EOF
		}
		if (position<buffer.length) {
			return buffer[(int)position++];
		} else {
			position++;
			return diskStream.read();
		}
	}
	
	/* (non-Javadoc)
	 * @see java.io.InputStream#read(byte[], int, int)
	 */
	public int read(byte[] b, int off, int len) throws IOException {
		if (position==size) {
			return -1; // EOF
		}
		if (position<buffer.length) {
			int toCopy = (int) Math.min(size-position,Math.min(len,buffer.length-position));
			System.arraycopy(buffer,(int)position,b,off,toCopy);
			position += toCopy;
			return toCopy;
		}
		// into disk zone
		int read = diskStream.read(b,off,len);
		position += read;
		return read;
	}


	public void readFullyTo(OutputStream os) throws IOException {
		byte[] buf = new byte[4096];
		int c = read(buf);
		while (c != -1) {
			os.write(buf,0,c);
			c = read(buf);
		}
	}

}
