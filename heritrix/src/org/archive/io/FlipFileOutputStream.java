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
	File file0;
	File file1;
	File currentFile;
	
	/**
	 * @param tempDir
	 * @param backingFilenamePrefix
	 */
	public FlipFileOutputStream(File tempDir, String backingFilenamePrefix) throws FileNotFoundException {
		tempDir.mkdirs();
	    pathPrefix = tempDir.getPath()+File.separatorChar+backingFilenamePrefix;
		file0 = new File(pathPrefix + ".ff0");
		file1 = new File(pathPrefix + ".ff1");
		currentFile = file0;
		outStream = new BufferedOutputStream(new FileOutputStream(currentFile), 4096);
	}

	/* (non-Javadoc)
	 * @see java.io.OutputStream#write(int)
	 */
	public void write(int b) throws IOException {
		outStream.write(b);
	}
	
	/* (non-Javadoc)
	 * @see java.io.OutputStream#write(byte[], int, int)
	 */
	public void write(byte[] b, int off, int len) throws IOException {
		outStream.write(b, off, len);
	}

	/* (non-Javadoc)
	 * @see java.io.OutputStream#write(byte[])
	 */
	public void write(byte[] b) throws IOException {
		outStream.write(b);
	}

	
	// TODO other write()s for efficiency
	
	public File getInputFile() throws IOException {
		File lastFile = currentFile;
		flip();
		return lastFile;
	}

	/**
	 * 
	 */
	private void flip() throws IOException {
		outStream.flush();
		currentFile = (currentFile == file0) ? file1 : file0;
		outStream = new BufferedOutputStream(new FileOutputStream(currentFile), 4096);
	}

	/**
	 * 
	 */
	public void discard() {
		file0.delete();
		file1.delete();
	}

	/* (non-Javadoc)
	 * @see java.io.OutputStream#close()
	 */
	public void close() throws IOException {
		super.close();
		outStream.close();
	}


}
