/*
 * ReplayableOutputStream.java
 * Created on Sep 23, 2003
 *
 * $Header$
 */
package org.archive.io;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A RecordingOutputStream can be wrapped around any other
 * OutputStream to record all bytes written to it. You can
 * then request a ReplayInputStream to read those bytes.
 * 
 * The RecordingOutputStream uses an in-memory buffer and 
 * backing disk file to allow it to record streams of 
 * arbitrary length, limited only by available disk space. 
 * 
 * As long as the stream recorded is smaller than the 
 * in-memory buffer, no disk access will occur. 
 * 
 * @author gojomo
 *
 */
public class RecordingOutputStream extends OutputStream {
	protected long size;
	protected int maxSize;
	protected String backingFilename;
	protected BufferedOutputStream diskStream;
	protected FileOutputStream fileStream;
	protected OutputStream wrappedStream;
	protected byte[] buffer;
	protected long position;
	protected long responseBodyStart; // when recording HTTP, where the content-body starts

	
	/**
	 * Create a new RecordingPutputStream with the specified parameters.
	 * 
	 * @param bufferSize
	 * @param backingFile
	 * @param maxSize
	 */
	public RecordingOutputStream(int bufferSize, String backingFilename, int maxSize) {
		buffer = new byte[bufferSize];
		this.backingFilename = backingFilename;
		this.maxSize = maxSize;
	}


	public void open(OutputStream wrappedStream) throws IOException {
		this.wrappedStream = wrappedStream;
		this.position = 0;
		fileStream = new FileOutputStream(backingFilename);
		diskStream = new BufferedOutputStream(fileStream,4096);
	}
	
	/**
	 * Total reset -- discarding all 
	 */
	public void clear() {
		try {
			// diskStream.flush(); // redundant
			diskStream.close();
		} catch (IOException e) {
			// nothing
		}
		diskStream = null;
	}


	/* (non-Javadoc)
	 * @see java.io.OutputStream#write(int)
	 */
	public void write(int b) throws IOException {
		record(b);
		wrappedStream.write(b);
	}
	
	/* (non-Javadoc)
	 * @see java.io.OutputStream#write(byte[])
	 */
	public void write(byte[] b) throws IOException {
		record(b,0,b.length);
		wrappedStream.write(b);
	}
	
	/* (non-Javadoc)
	 * @see java.io.OutputStream#write(byte[], int, int)
	 */
	public void write(byte[] b, int off, int len) throws IOException {
		record(b,off,len);
		wrappedStream.write(b,off,len);
	}
	
	/**
	 * @param b
	 */
	private void record(int b) throws IOException {
		if(position>=buffer.length){
			diskStream.write(b);
		} else {
			buffer[(int)position] = (byte)b;
		}
		position++;
	}
	
	/**
	 * @param b
	 * @param off
	 * @param len
	 */
	private void record(byte[] b, int off, int len) throws IOException {
		if(position>=buffer.length){
			diskStream.write(b,off,len);
			position += len;
		} else {
			int toCopy = (int)Math.min(buffer.length-position,len);
			System.arraycopy(b,off,buffer,(int)position,toCopy);
			position += toCopy;
			// TODO verify these are +1 -1 right
			if (toCopy<len) {
				record(b,off+toCopy,len-toCopy);
			}
		}
	}

	/* (non-Javadoc)
	 * @see java.io.OutputStream#close()
	 */
	public void close() throws IOException {
		super.close();
		wrappedStream.close();
		// diskStream.flush(); // redundant
		if (diskStream != null) {
			diskStream.close();
		} 
		this.size = position;
	}

	/* (non-Javadoc)
	 * @see java.io.OutputStream#flush()
	 */
	public void flush() throws IOException {
		super.flush();
		if (diskStream != null) {
			diskStream.flush();
		}
		wrappedStream.flush();
	}
	
	public ReplayInputStream getReplayInputStream() throws IOException {
		return new ReplayInputStream(buffer,size,responseBodyStart,backingFilename);
	}


	/**
	 * @return
	 */
	public long getSize() {
		return size;
	}


	/**
	 * 
	 */
	public void markResponseBodyStart() {
		responseBodyStart = position;
	}


	/**
	 * @return
	 */
	public CharSequence getCharSequence() {
		try {
			return new ReplayCharSequence(buffer,size,responseBodyStart,backingFilename);
		} catch (IOException e) {
			// TODO convert to runtime exception?
			e.printStackTrace();
		}
		return null;
	}


	/**
	 * @return
	 */
	public long getResponseContentLength() {
		return size-responseBodyStart;
	}

	/**
	 * 
	 */
	public void closeRecorder() throws IOException {
		// diskStream.flush(); // redundant, close includes flush
		if (diskStream != null) {
			diskStream.close();
		}
		this.size = position;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#finalize()
	 */
	protected void finalize() throws Throwable {
		if (fileStream != null) {
			assert !fileStream.getFD().valid() : "valid fileStream reached finalize";
		}
		super.finalize();
	}


	/**
	 * Return a replay stream, cued up to beginning of 
	 */
	public ReplayInputStream getContentReplayInputStream() throws IOException {
		ReplayInputStream replay = getReplayInputStream();
		replay.skip(responseBodyStart);
		return replay;
	}

}
