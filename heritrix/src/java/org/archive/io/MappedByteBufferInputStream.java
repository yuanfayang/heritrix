/* NuArcReaderFactory
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
 * @author stack
 */
public class MappedByteBufferInputStream extends InputStream {
	
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
	 * @param buffer MappedByteBuffer to use.
	 */
	public MappedByteBufferInputStream(MappedByteBuffer mbb) {
		super();
		this.mbb = mbb;
        this.closed = false;
	}
	
    protected MappedByteBuffer getMappedByteBuffer() {
        // TODO: Shut this down.
    	    return this.mbb;
    }
    
	public int read() throws IOException {
        checkClosed();
		return (available() <= 0)? -1: this.mbb.get();
	}
    
    // TODO: Add buffer read.
    
    /* (non-Javadoc)
	 * @see java.io.InputStream#close()
	 */
	public void close() throws IOException {
		super.close();
        this.closed = true;
	}
    
    /**
     * @return True if this stream has been closed.
     */
    protected void checkClosed() throws IOException {
    	    if (this.closed) {
            throw new IOException("Stream has been closed");
        }
    }
    
    /* (non-Javadoc)
	 * @see java.io.InputStream#markSupported()
	 */
	public boolean markSupported() {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see java.io.InputStream#mark(int)
	 */
	public synchronized void mark(int markAmount) {
        // markAmount is unused.  Just get current postion
        // so can reset to it.
		this.mark = this.mbb.position();
	}
	
	/* (non-Javadoc)
	 * @see java.io.InputStream#reset()
	 */
	public synchronized void reset() throws IOException {
        checkClosed();
		this.mbb.position(this.mark);
	}
	
    /* (non-Javadoc)
	 * @see java.io.InputStream#available()
	 */
	public int available() throws IOException {
        checkClosed();
		return this.mbb.remaining();
	}
	
	public int getPosition() {
		return this.mbb.position();
	}
	
	public void setPosition(int position) {
		this.mbb.position(position);
	}
}
