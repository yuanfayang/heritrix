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
 * <p>Takes an InputStream streams that implements
 * {@link PositionableStream} interface so it can backup overreads done
 * by the zlib Inflater class.  Also implements this interface so can
 * compressed and uncompressed streams alike.
 * 
 * @author stack
 */
public class GzippedInputStream extends GZIPInputStream
	implements Iterator, PositionableStream {
    
    /**
     * Offset of current gzip member.
     * 
     * -1 flags that we're on the first member.
     */
    private long currentMemberOffset = -1;
    
    
    public GzippedInputStream(InputStream in) throws IOException {
        this(in, 512);
    }
    
	public GzippedInputStream(InputStream in, int size) throws IOException {
		super(in, size);
		if (!(this.in instanceof PositionableStream)) {
		    throw new IOException("Passed stream does not" +
		            " implement PositionableStream");
		}
		// The parent GZIPInputStream constructor has read past the gzip member
		// header.  Position stream back at start so the first record gets
		// treated just like all others in below processing.  Makes logic cleaner.
		seek(0);
	}

	/**
	 * Use this method to get position of gzip member start.
	 * @return Returns current gzip members beginning offset.
	 */
	public long getMemberOffset() {
	    return this.currentMemberOffset;
	}
	
	public boolean hasNext() {
        boolean result = false;
		if (this.currentMemberOffset == -1) {
		    // Let out the member we're currently pointing at.
		    result = true;
		} else {
		    // Move to next record if there is one. Move to the next gzip
		    // member positioning ourselves by backing up the stream
		    // so we reread any inflater remaining bytes.  Then add 8 bytes to
		    // get us past the GZIP CRC trailer block that ends all
		    // gzip members.
		    try {
		        seek(getFilePointer() - this.inf.getRemaining() +
		                8 /*Sizeof gzip CRC block*/);
		        // If available bytes, assume another gzip member.
		        // Move the stream on to the start of next gzip member.
		        // We do this because the above calculation using the
		        // this.inf.getRemaining is off by one if the remaining is
		        // zero.
		        int read = -1;
		        while (((PositionableStream)this.in).available() > 0 &&
		                	!result &&
		                	((read = this.in.read()) != -1)) {
		            	if ((read & 0xff) ==
		            	    	(0xff & GZIPInputStream.GZIP_MAGIC)) {
		            	    // We've found gzip header start.  Backup stream one byte.
		            	    seek(getFilePointer() - 1);
		            	    result = true;
		            }
		        }
		    } catch (IOException e) {
		        throw new RuntimeException("Failed i/o: " +
		                e.getMessage());
		    }
		}
		
		return result;
	}
	
    /**
     * @return An InputStream.
     */
	public Object next() {
	    // Reset inflater and read in next gzip header.
	    try {
            this.currentMemberOffset = getFilePointer();
	        nextGzipMember();
        } catch (IOException e) {
	        throw new RuntimeException("Failed i/o next member: " +
	                e.getMessage());
        }
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

    public void seek(long position) throws IOException {
        // Assume that seek puts us at a gzip member begin.
        // Reset flag that indicates get next record from
        // current location.
        this.currentMemberOffset = -1;
        ((PositionableStream)this.in).seek(position);
    }

    public long getFilePointer() throws IOException {
       return  ((PositionableStream)this.in).getFilePointer();
    }
}