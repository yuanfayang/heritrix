/* ReplayInputStream
 * 
 * $Id$
 * 
 * Created on Sep 24, 2003
 * 
 * Copyright (C) 2003 Internet Archive.
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

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * Replays the bytes recorded from a RecordingInputStream or
 * RecordingOutputStream. 
 * 
 * @author gojomo
 */
public class ReplayInputStream extends InputStream
{    
	protected BufferedInputStream diskStream;
	protected byte[] buffer;
	protected long size;
	protected long position;
    
    /**
     * Where the response body starts, if marked
     */
    protected long responseBodyStart;
	
    
	/**
     * Constructor.
     * 
	 * @param buffer Buffer to read from.
	 * @param size Size of data to replay.
	 * @param responseBodyStart Start of the response body.
     * @param backingFilename Backing file that sits behind the buffer.  If
     * <code>size<code> > than buffer then we go to backing file to read 
     * data that is beyond buffer.length.
     * 
	 * @throws IOException If we fail to open an input stream on 
     * backing file.
	 */
	public ReplayInputStream(byte[] buffer, long size, long responseBodyStart,
            String backingFilename)
        throws IOException
    {
		this(buffer,size,backingFilename);
		this.responseBodyStart = responseBodyStart;
	}

	/**
     * Constructor.
     * 
     * @param buffer Buffer to read from.
     * @param size Size of data to replay.
     * @param backingFilename Backing file that sits behind the buffer.  If
     * <code>size<code> > than buffer then we go to backing file to read 
     * data that is beyond buffer.length.
     * @throws IOException If we fail to open an input stream on 
     * backing file.
	 */
	public ReplayInputStream(byte[] buffer, long size, String backingFilename)
        throws IOException
    {
		this.buffer = buffer;
		this.size = size;
		if (size > buffer.length) {
            FileInputStream fis = new FileInputStream(backingFilename);
			diskStream = new BufferedInputStream(fis, 4096);
		}
	}

	public long setToResponseBodyStart() {
		position = responseBodyStart;
		return position;
	}
	
	/* (non-Javadoc)
	 * @see java.io.InputStream#read()
	 */
	public int read() throws IOException {
		if (position == size) {
			return -1; // EOF
		}
		if (position < buffer.length) {
		    // Convert to unsigned int.
			int c = (int)buffer[(int)position] & 0xFF;
			position++;
			return c; 
		} else {
			int c = diskStream.read();
			if(c >= 0) {
				position++;
			}
			return c;
		}
	}
	
	/* (non-Javadoc)
	 * @see java.io.InputStream#read(byte[], int, int)
	 */
	public int read(byte[] b, int off, int len) throws IOException {
		if (position == size) {
			return -1; // EOF
		}
		if (position < buffer.length) {
			int toCopy = (int)Math.min(size - position,
                Math.min(len, buffer.length - position));
			System.arraycopy(buffer, (int)position, b, off, toCopy);
			if (toCopy > 0) {
				position += toCopy;
			} 
			return toCopy;
		}
		// into disk zone
		int read = diskStream.read(b,off,len);
		if(read>0) {
			position += read;
		}
		return read;
	}

	public void readFullyTo(OutputStream os) throws IOException {
		byte[] buf = new byte[4096];
		int c = read(buf);
		while (c != -1) {
			os.write(buf,0,c);
			c = read(buf);
		}
	}

	/* (non-Javadoc)
	 * @see java.io.InputStream#close()
	 */
	public void close() throws IOException {
		super.close();
		if(diskStream != null) {
			diskStream.close();
		} 
	}
	
	/**
	 * @return Amount THEORETICALLY remaining.
	 */
	public long remaining() {
		return size - position;
	}
}
