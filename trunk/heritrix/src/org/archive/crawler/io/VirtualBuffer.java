/*
 * VirtualBuffer.java
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
 * Actually read/written by requesting the relevant stream. Such
 * streams will have inside knowledge of the VirtualBuffer's
 * internal representation. 
 * 
 * Subclasses could also provide wrappers (perhaps read-only) of 
 * other datatypes which might be available (Strings, NIO Buffers, etc.)
 * 
 * Subclasses might also include knowledge of the buffers format
 * and convenience methods for accessing its subranges. For
 * example, HTTPResponseBuffer.getContentBodyCharSequence() 
 * would return a properly character-decoded CharSequence for
 * just the Content-Body portion of the response.
 * 
 * 
 * @author gojomo
 */
public abstract class VirtualBuffer {
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
	 * Returns a CharSequence interface on this VirtualBuffer
	 * (for use by Regular Expressions, for example).
	 * 
	 * May require choice of a character encoding in subclasses,
	 * by previous initialization, or in alternate forms of
	 * this method.
	 * @return
	 */
	
	public abstract CharSequence getCharSequence();
	
	/**
	 * Returns a CharSequence interface on a subrange of
	 * this VirtualBuffer (for use by Regular Expressions, for example).
	 * 
	 * May require choice of a character encoding in subclasses,
	 * by previous initialization, or in alternate forms of
	 * this method.
	 * @return
	 */
	public abstract CharSequence getCharSequence(long start, long end);

	
	/**
	 * Returns whether buffer can only be read. Even if
	 * a buffer is initially writable, subsequently closing an
	 * OutputStream returned from getOutputStream() will
	 * render it read-only. 
	 * @return
	 */
	public abstract boolean isReadOnly();
	
	/**
	 * Returns total size of the buffered data. May increase
	 * as long as the buffer remains writable. 
	 * @return
	 */
	public abstract int getSize();
	
	/**
	 * Returns the checksum of all written data. May change
	 * as long as the buffer remains writable. 
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
