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

import org.archive.io.RecordingInputStream;
import org.archive.io.RecordingOutputStream;

/**
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
	 * @param is
	 * @return
	 */
	public InputStream inputWrap(InputStream is) throws IOException {
		ris.open(is);
		return ris;
	}

	/**
	 * @param outputStream
	 * @return
	 */
	public OutputStream outputWrap(OutputStream os) throws IOException {
		ros.open(os);
		return ros;
	}

	/**
	 * 
	 */
	public void close() {
		try {
			ris.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			ros.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	/**
	 * 
	 */
	public RecordingInputStream getRecordedInput() {
		return ris;
	}

	/**
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
