/*
 * HTTPRecorder.java
 * Created on Sep 22, 2003
 *
 * $Header$
 */
package org.archive.util;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Initially only supports HTTP/1.0 (one request, one response per stream)
 * 
 * @author gojomo
 *
 */
public class HttpRecorder {

	/**
	 * @param is
	 * @return
	 */
	public InputStream inputWrap(InputStream is) {
		return is;
	}

	/**
	 * @param outputStream
	 * @return
	 */
	public OutputStream outputWrap(OutputStream outputStream) {
		return outputStream;
	}
	
}
