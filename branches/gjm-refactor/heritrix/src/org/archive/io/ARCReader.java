/*
 * ARCReader.java
 * Created on Sep 26, 2003
 *
 * $Header$
 */
package org.archive.io;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/**
 * Utility class for reading ARC files, including .arc.gz
 * files. 
 * 
 * IN PROGRESS / THINKING OUT LOUD CODE
 * 
 * @author gojomo
 *
 */
public class ARCReader {
	protected InputStream inStream;
	protected FileInputStream arcStream;
	protected ARCResource lastResource;
	protected int resourcePosition;
	protected long filePosition;
	
	/**
	 * 
	 */
	public ARCReader() {
		super();
	}

	public void open(String filename) throws IOException {
		String flattenedFilename = filename.toLowerCase();
		assert flattenedFilename.endsWith(".arc") || flattenedFilename.endsWith(".arc.gz") : "non-arc filename extension";
		arcStream = new FileInputStream(filename);
		inStream = new BufferedInputStream(arcStream,4096);
	    if (flattenedFilename.endsWith(".gz")) {
	    	inStream = new GZIPInputStream(inStream);
	    }
	}
	
	public ARCResource getNextResource() {
		return null;
	}
}
