/* RandomAccessInputStream
*
* Created on May 21, 2004
*
* Copyright (C) 2004 Internet Archive.
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
*/
package org.archive.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;


/**
 * Wraps a RandomAccessFile with an InputStream interface.
 *
 * @author gojomo
 */
public class RandomAccessInputStream extends InputStream  implements PositionableStream {
    
    /**
     * Reference to the random access file this stream is reading from.
     */
    private RandomAccessFile raf = null;

    /**
     * @param raf RandomAccessFile to wrap.
     */
    public RandomAccessInputStream(RandomAccessFile raf) {
        super();
        this.raf = raf;
    }
    
    /**
     * @param file File to wrap.
     */
    public RandomAccessInputStream(File file) throws FileNotFoundException {
        super();
        this.raf = new RandomAccessFile(file, "r");
    }

    /* (non-Javadoc)
     * @see java.io.InputStream#read()
     */
    public int read() throws IOException {
        return this.raf.read();
    }

    /* (non-Javadoc)
     * @see java.io.InputStream#read(byte[], int, int)
     */
    public int read(byte[] b, int off, int len) throws IOException {
        return this.raf.read(b, off, len);
    }
    
    /* (non-Javadoc)
     * @see java.io.InputStream#read(byte[])
     */
    public int read(byte[] b) throws IOException {
        return this.raf.read(b);
    }

    /* (non-Javadoc)
     * @see java.io.InputStream#skip(long)
     */
    public long skip(long n) throws IOException {
        this.raf.seek(this.raf.getFilePointer() + n);
        return n;
    }

	public long getFilePointer() throws IOException {
		return this.raf.getFilePointer();
	}

	public void seek(long position) throws IOException {
		this.raf.seek(position);
	}
    
	public int available() throws IOException {
        long amount = this.raf.length() - this.getFilePointer();
        return (amount >= Integer.MAX_VALUE)? Integer.MAX_VALUE: (int)amount;
	}
}
