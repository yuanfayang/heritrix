/*
 * RecordingInputStream.java
 * Created on Sep 24, 2003
 *
 * $Header$
 */
package org.archive.io;

import java.io.IOException;
import java.io.InputStream;


/**
 * @author gojomo
 *
 */
public class RecordingInputStream extends InputStream {
	protected InputStream wrappedStream;
	protected RecordingOutputStream recordingOutputStream;


	/**
	 * Create a new RecordingInputStream with the specified parameters.
	 * 
	 * @param bufferSize
	 * @param backingFile
	 * @param maxSize
	 */
	public RecordingInputStream(int bufferSize, String backingFilename, int maxSize) {
		recordingOutputStream = new RecordingOutputStream(bufferSize, backingFilename, maxSize);
	}

	public void open(InputStream wrappedStream) throws IOException {
		this.wrappedStream = wrappedStream;
		recordingOutputStream.open(new NullOutputStream());
	}

	/* (non-Javadoc)
	 * @see java.io.InputStream#read()
	 */
	public int read() throws IOException {
		int b = wrappedStream.read();
		if (b != -1) {
			recordingOutputStream.write(b);
		} 
		return b;
	}
	
	/* (non-Javadoc)
	 * @see java.io.InputStream#read(byte[], int, int)
	 */
	public int read(byte[] b, int off, int len) throws IOException {
		int count = wrappedStream.read(b,off,len);
		if (count>0) {
			recordingOutputStream.write(b,off,count);
		} 
		return count;
	}

	/* (non-Javadoc)
	 * @see java.io.InputStream#read(byte[])
	 */
	public int read(byte[] b) throws IOException {
		int count = wrappedStream.read(b);
		if (count>0) {
			recordingOutputStream.write(b,0,count);
		} 
		return count;
	}
	
	/* (non-Javadoc)
	 * @see java.io.OutputStream#close()
	 */
	public void close() throws IOException {
		super.close();
		wrappedStream.close();
		recordingOutputStream.close();
	}
	
	public ReplayInputStream getReplayInputStream() throws IOException {
		return recordingOutputStream.getReplayInputStream();
	}

	public long readFully() throws IOException {
		byte[] buf = new byte[4096];
		while(read(buf)!=-1) {
		}
		return recordingOutputStream.getSize();
	}


	/**
	 * @param maxLength
	 * @param timeout
	 */
	public boolean readFullyOrUntil(long maxLength, long timeout) throws IOException {
		long startTime;
		if(timeout>0) {
			startTime = System.currentTimeMillis();
		}
		byte[] buf = new byte[4096];
		while(read(buf)!=-1) {
			// TODO check length and time limits
		}
		return false; // did not timeout or overflow
	}


	/**
	 * @return
	 */
	public long getSize() {
		// TODO Auto-generated method stub
		return recordingOutputStream.getSize();
	}

	/**
	 * 
	 */
	public void markResponseBodyStart() {
		recordingOutputStream.markResponseBodyStart();
	}

	/**
	 * @return
	 */
	public CharSequence getCharSequence() {
		return recordingOutputStream.getCharSequence();
	}

	/**
	 * @return
	 */
	public long getResponseContentLength() {
		return recordingOutputStream.getResponseContentLength();
	}

}
