/*
 * RecordingInputStream.java
 * Created on Sep 24, 2003
 *
 * $Header$
 */
package org.archive.crawler.io;

import java.io.IOException;
import java.io.InputStream;

import javax.swing.text.Position;

import org.archive.util.NullOutputStream;

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
		recordingOutputStream.write(b);
		return b;
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
		while(read()!=-1) {
		}
		return recordingOutputStream.getSize();
		
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

}
