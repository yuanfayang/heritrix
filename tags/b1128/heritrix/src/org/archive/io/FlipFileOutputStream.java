/*
 * FlipFileOutputStream.java
 * Created on Oct 14, 2003
 *
 * $Header$
 */
package org.archive.io;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author gojomo
 *
 */
public class FlipFileOutputStream extends OutputStream {
	BufferedOutputStream outStream;
	String pathPrefix;
	String currentFile;
	boolean next1;
	
	/**
	 * @param tempDir
	 * @param backingFilenamePrefix
	 */
	public FlipFileOutputStream(File tempDir, String backingFilenamePrefix) throws FileNotFoundException {
		tempDir.mkdirs();
	    pathPrefix = tempDir.getPath()+File.separatorChar+backingFilenamePrefix;
		currentFile = pathPrefix + ".ff0";
		outStream = new BufferedOutputStream(new FileOutputStream(currentFile), 4096);
		next1 = true;
	}

	/* (non-Javadoc)
	 * @see java.io.OutputStream#write(int)
	 */
	public void write(int b) throws IOException {
		outStream.write(b);
	}
	
	// TODO other write()s for efficiency
	
	public String getInputFile() throws IOException {
		String lastFile = currentFile;
		flip();
		return lastFile;
	}

	/**
	 * 
	 */
	private void flip() throws IOException {
		outStream.flush();
		currentFile = pathPrefix + (next1 ? ".ff1" : ".ff0");
		next1 = !next1;
		outStream = new BufferedOutputStream(new FileOutputStream(currentFile), 4096);
	}

}
