/* GzippedInputStream
*
* $Id$
*
* Created on July 5, 2004
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
import java.util.Iterator;
import java.util.zip.GZIPInputStream;

import org.archive.io.PositionableStream;


/**
 * Subclass of GZIPInputStream that can handle a stream made of multiple
 * concatenatd GZIP members/records.
 * 
 * This class is needed because GZIPInputStream only finds the first GZIP
 * member in the file even if the file is made up of multiple GZIP members.
 * 
 * @author stack
 */
public class GzippedInputStream extends GZIPInputStream implements Iterator {
    
    public GzippedInputStream(InputStream in) throws IOException {
        this(in, 512);
    }
    
	public GzippedInputStream(InputStream in, int size) throws IOException {
		super(in, size);
	}
    
	public boolean hasNext() {
        boolean result = false;
		if (!this.inf.finished()) {
            // Inflater is asking for more.  We're probably on the first gzip
            // member in the stream.  Return 'true'.
            result = true;
        } else {
            // We've finished current inflation.  Is there more in the stream?
            // Only if the underlying stream implements SeekableStream can we
            // support moving on to next gzip member, if any.
            if (this.inf.getRemaining() >= 0 &&
                    this.in instanceof PositionableStream) {
                // Move to next gzip member. Positioning ourselves means
                // backing up the stream so we reread any inflater remaining
                // bytes.  We then add 8 bytes to get us past the GZIP CRC
                // trailer block that ends all gzip members.
                try {
                    PositionableStream ss = (PositionableStream)this.in;
					ss.seek(ss.getFilePointer() - this.inf.getRemaining() +
                        8 /*Sizeof gzip CRC block*/);
                    // If available bytes, assume another gzip member.
                    if (ss.available() > 0) {
                        nextGzipMember();
                    	    result = true;
                    }
				} catch (IOException e) {
					throw new RuntimeException("Failed i/o: " +
                        e.getMessage());
				}
            }
        }
        
        return result;
	}
	
    /**
     * @return An InputStream.
     */
	public Object next() {
		return this;
	}
	
	public void remove() {
		throw new UnsupportedOperationException();
	}
	
	public Iterator iterator() {
		return this;   
	}
	
    /**
     * Move to next gzip member in the file.
     */
	protected synchronized void nextGzipMember() throws IOException {
		this.eos = false;
		this.inf.reset();
		new GzipHeader(this.in);
		this.crc.reset();
	}
}
