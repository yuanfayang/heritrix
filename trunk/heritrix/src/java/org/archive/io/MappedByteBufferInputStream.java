/* MappedByteBufferInputStream
 *
 * $Id$
 *
 * Created on May 1, 2004
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.MappedByteBuffer;

/**
 * An inputstream perspective on a MappedByteBuffer.
 *
 * This class is effectively a random access input stream.  Use
 * {@link #getFilePointer()} to get current location and then mark and reset to
 * move about in the stream.
 * 
 * <p>This class was made because I wanted to use java.nio memory-mapped
 * files rather than old-school java.io reading arcs because:
 *
 * "Accessing a file through the memory-mapping mechanism can be far more
 * efficient than reading or writing data by conventional means, even when
 * using channels. No explicit system calls need to be made, which can be
 * time-consuming. More importantly, the virtual memory system of the operating
 * system automatically caches memory pages. These pages will be cached using
 * system memory and will not consume space from the JVM's memory heap.
 * Once a memory page has been made valid (brought in from disk), it can be
 * accessed again at full hardware speed without the need to make another
 * system call to get the data. Large, structured files that contain indexes or
 * other sections that are referenced or updated frequently can benefit
 * tremendously from memory mapping....", from the OReilly Java NIO By Ron
 * Hitchens.
 *
 * <p>Using a ByteBuffer that
 * holds the whole ARC file for sure makes the code simpler and the nice thing
 * about using memory-mapped buffers for reading is that the memory used is
 * allocated in the OS, not in the JVM.  I played around w/ this on a machine
 * w/ 512M of physical memory and a swap of 1G (/sbin/swapon -s).  I made a
 * dumb program to use file channel memory-mapped buffers to read a file.  I
 * was able to read a file of 1.5G using default JVM heap (64M on linux IIRC):
 * i.e. I was able to allocate a buffer of 1.5G inside inside in my
 * small-heap program.  Anything bigger and I got complaints back
 * about  unable to allocate the memory.  So, a channel based reader would be
 * limited only by memory characteristics of the machine its running on (swap
 * and physical memory -- not JVM heap size) ONLY, I discovered the following.
 * Note, a spin on the 'unable to allocate the memory' was that I was 
 * unable to keep open tens of ARC instances concurrently because each was
 * using 100meg plus of RAM.
 *
 * <p>Really big files generated complaint out of FileChannel.map saying the
 * size parameter was > Integer.MAX_VALUE which is also odd considering the
 * type is long.  This must be an nio bug.  Means there is an upperbound of
 * Integer.MAX_VALUE (about 2.1G or so).  This is unfortunate -- particularly
 * as the c-code tools for ARC manipulations, see alexa/common/a_arcio.c,
 * support > 2.1G -- but its good enough for now (ARC files are usually
 * 100M).
 *
 * <p>The committee seems to still be out regards general nio
 * performance.  See <a
 * href="http://forum.java.sun.com/thread.jsp?forum=4&thread=227539&message=806443">NIO
 * ByteBuffer slower than BufferedInputStream</a>.  It can be 4 times slower
 * than java.io or 40% faster.  For sure its 3x to 4x slower than reading from
 * a buffer: http://jroller.com/page/cpurdy/20040405#raw_nio_performance.
 * Tests done reading arcs show the difference to be little in the scheme of 
 * things.
 *
 * @author stack
 */
public class MappedByteBufferInputStream extends InputStream
        implements PositionableStream {
	
    /**
     * The mapped byte buffer we're feeding this stream from.
     */
	private final MappedByteBuffer mbb;

    /**
     * Position to return to on call to reset.
     */
	private int mark = 0;

    /**
     * True if close has been called on this stream.
     */
    private boolean closed = true;
	
	/**
	 * Constructor.
	 * @param mbb MappedByteBuffer to use.
	 */
	public MappedByteBufferInputStream(MappedByteBuffer mbb) {
		super();
		this.mbb = mbb;
        this.closed = false;
	}

	public int read() throws IOException {
        checkClosed();
		return (available() <= 0)? -1: this.mbb.get() & 0xff;
	}

    public int read(byte[] b, int off, int len) throws IOException {
        checkClosed();
        int read = (available() <= 0)? -1: Math.min(available(), len);
        if (read > 0) {
            this.mbb.get(b, off, read);
        }
        return read;
    }

	public void close() throws IOException {
		super.close();
        this.closed = true;
	}

    protected void checkClosed() throws IOException {
    	    if (this.closed) {
            throw new IOException("Stream has been closed");
        }
    }

	public boolean markSupported() {
		return true;
	}
	
	public synchronized void mark(int markAmount) {
        // markAmount is unused.  Just get current postion
        // so can reset to it.
		this.mark = this.mbb.position();
	}
	
	public synchronized void reset() throws IOException {
        checkClosed();
		this.mbb.position(this.mark);
	}
	
	public int available() throws IOException {
        checkClosed();
		return this.mbb.remaining();
	}
	
	public long getFilePointer() throws IOException {
		return this.mbb.position();
	}
	
	public void seek(long position) throws IOException {
        assert position < Integer.MAX_VALUE: "Position too big for int.";
		this.mbb.position((int)position);
	}
}