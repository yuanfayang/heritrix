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
	 * @throws FileNotFoundException
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

    /**
     * 
     */
    public File getFile0() {
        return file0;
    }

    /**
     * 
     */
    public File getFile1() {
        return file1;
    }

    /**
     * @return
     */
    public int getCurrentFileIndex() {
        return (currentFile == file0) ? 0 : 1;
    }


}
