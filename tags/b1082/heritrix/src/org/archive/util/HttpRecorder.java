/*
 * HTTPRecorder.java
 * Created on Sep 22, 2003
 *
 * $Header$
 */
package org.archive.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.archive.crawler.io.RecordingInputStream;
import org.archive.crawler.io.RecordingOutputStream;

/**
 * Initially only supports HTTP/1.0 (one request, one response per stream)
 * 
 * @author gojomo
 *
 */
public class HttpRecorder {
	String backingFilenamePrefix;
	RecordingInputStream ris;
	RecordingOutputStream ros;
	
	/**
	 * 
	 */
	public HttpRecorder(String backingFilenamePrefix) {
		super();
		ris = new RecordingInputStream(32768,backingFilenamePrefix+".ris",2^20);
		ros = new RecordingOutputStream(2048,backingFilenamePrefix+".ros",2^12);
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
	
}
