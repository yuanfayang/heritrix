/*
 * ReadWriteBuffer.java
 * Created on Apr 17, 2003
 *
 * $Header$
 */
package org.archive.crawler.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.Checksum;


/**
 * A virtual buffer of arbitrary size, likely using some mixture of
 * RAM and backing disk when necessary. 
 * 
 * @author gojomo
 */
public abstract class ReadWriteVirtualBuffer  {
	/**
	 * Returns an OutputStream to write into the buffer. Only
	 * one such stream should be requested. Once this stream
	 * is closed, no further writing is possible. 
	 * @return
	 */
	public abstract OutputStream getOutputStream();
	
	/**
	 * Returns a SeekableInputStream for reading from the 
	 * buffer. Each call returns an independent stream,
	 * initially pointing at the beginning of the buffer.
	 * @return
	 */
	public abstract SeekableInputStream getInputStream();
	
	/**
	 * Returns whether additional data can be written 
	 * to the buffer. (If an OutputStream from getOutpuStream()
	 * has been closed, returns false, otherwise true.)
	 * @return
	 */
	public abstract boolean isOpen();
	
	/**
	 * Returns total size of the buffered data. May increase
	 * if the buffer remains open. 
	 * @return
	 */
	public abstract int getSize();
	
	/**
	 * Returns the checksum of all written data. May change
	 * if the buffer remains open.
	 * @return
	 */
	public abstract Checksum getChecksum();
	
	/**
	 * Utility method to write all buffer data to another
	 * OutputStream.
	 * @param os
	 * @throws IOException
	 */
	public void writeAllTo(OutputStream os) throws IOException {
		InputStream is = getInputStream();
		int b;
		while( (b=is.read())>-1 ) os.write(b);
	}
	
	/**
	 * Utility method to read all buffer data from another
	 * InputStream.
	 * @param is
	 * @throws IOException
	 */
	public void readAllFrom(InputStream is) throws IOException {
		OutputStream os = getOutputStream();
		int b;
		while( (b=is.read())>-1 ) os.write(b);
	}

	
}
