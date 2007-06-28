/*
 * HTTPRecorder.java
 * Created on Sep 22, 2003
 *
 * $Header$
 */
package org.archive.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;

import org.archive.io.RecordingInputStream;
import org.archive.io.RecordingOutputStream;

/**
 * Pairs together a RecordingInputStream and RecordingOutputStream
 * to capture exactly a single HTTP transaction.
 * 
 * Initially only supports HTTP/1.0 (one request, one response per stream)
 * 
 * @author gojomo
 *
 */
public class HttpRecorder {
	private static final int DEFAULT_OUTPUT_BUFFER_SIZE = 4096;
	private static final int DEFAULT_INPUT_BUFFER_SIZE = 65536;
	protected RecordingInputStream ris;
	protected RecordingOutputStream ros;
	
	/**
	 * 
	 */
	public HttpRecorder(File tempDir, String backingFilenamePrefix) {
//		super();
		tempDir.mkdirs();
		String tempDirPath = tempDir.getPath()+File.separatorChar;
		ris = new RecordingInputStream(DEFAULT_INPUT_BUFFER_SIZE,tempDirPath+backingFilenamePrefix+".ris",2^20);
		ros = new RecordingOutputStream(DEFAULT_OUTPUT_BUFFER_SIZE,tempDirPath+backingFilenamePrefix+".ros",2^12);
	}

	/**
	 * Wrap the provided stream with the internal RecordingInputStream
	 * 
	 * @param is
	 * @return
	 */
	public InputStream inputWrap(InputStream is) throws IOException {
		ris.open(is);
		return ris;
	}

	/**
	 * Wrap the provided stream with the internal RecordingOutputStream
	 * @param outputStream
	 * @return
	 */
	public OutputStream outputWrap(OutputStream os) throws IOException {
		ros.open(os);
		return ros;
	}

	/**
	 * Close all streams.
	 */
	public void close() {
		try {
			ris.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			DevUtils.logger.log(Level.SEVERE,"close() ris"+DevUtils.extraInfo(),e);

		}
		try {
			ros.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			DevUtils.logger.log(Level.SEVERE,"close() ros"+DevUtils.extraInfo(),e);
		}
	}

	/**
	 * Return the internal RecordingInputStream
	 */
	public RecordingInputStream getRecordedInput() {
		return ris;
	}

	/**
	 * Mark the point where the HTTP headers end. 
	 * 
	 */
	public void markResponseBodyStart() {
		ris.markResponseBodyStart();
	}

	/**
	 * @return
	 */
	public long getResponseContentLength() {
		return ris.getResponseContentLength();
	}

	/**
	 * 
	 */
	public void closeRecorders() {
		try {
			ris.closeRecorder();
			ros.closeRecorder();
		} catch (IOException e) {
			DevUtils.warnHandle(e,"convert to runtime exception?");
		}
	}
	
}