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
	 * @param tempDir
	 * @param backingFilenamePrefix
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
	 * @return An inputstream.
	 * @throws IOException
	 */
	public InputStream inputWrap(InputStream is) throws IOException {
		ris.open(is);
		return ris;
	}

	/**
	 * Wrap the provided stream with the internal RecordingOutputStream
	 * @param os
	 * @return An output stream.
	 * @throws IOException
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
	 * @return A RIS.
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
