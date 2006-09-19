/* BufferedSeekInputStream
*
* Created on September 14, 2006
*
* Copyright (C) 2006 Internet Archive.
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

import org.archive.util.IoUtils;


/**
 * Buffers data from some other SeekInputStream.
 * 
 * @author pjack
 */
public class BufferedSeekInputStream extends SeekInputStream {


    /**
     * The underlying input stream.
     */
    final private SeekInputStream input;


    /**
     * The buffered data.
     */
    final private byte[] buffer;


    /**
     * The offset of within the buffer of the next byte to read.
     */
    private int offset;


    /**
     * Constructor.
     * 
     * @param input  the underlying input stream
     * @param capacity   the size of the buffer
     * @throws IOException   if an IO occurs filling the first buffer
     */
    public BufferedSeekInputStream(SeekInputStream input, int capacity) 
    throws IOException {
        this.input = input;
        this.buffer = new byte[capacity];
        buffer();
    }


    /**
     * Fills the buffer.
     * 
     * @throws IOException  if an IO error occurs
     */
    private void buffer() throws IOException {
        IoUtils.readFully(input, buffer);
        offset = 0;
    }


    /**
     * Ensures that the buffer is valid.
     * 
     * @throws IOException  if an IO error occurs
     */
    private void ensureBuffer() throws IOException {
        if (offset >= buffer.length) {
            buffer();
        }
    }


    /**
     * Returns the number of unread bytes in the current buffer.
     * 
     * @return  the remaining bytes
     */
    private int remaining() {
        return buffer.length - offset;
    }


    @Override
    public int read() throws IOException {
        ensureBuffer();
        int ch = buffer[offset] & 0xFF;
        offset++;
        return ch;
    }


    @Override
    public int read(byte[] buf, int ofs, int len) throws IOException {
        ensureBuffer();
        len = Math.min(len, remaining());
        System.arraycopy(buffer, offset, buf, ofs, len);
        offset += len;
        return len;
    }


    @Override
    public int read(byte[] buf) throws IOException {
        return read(buf, 0, buf.length);
    }


    @Override
    public long skip(long c) throws IOException {
        ensureBuffer();
        int count = (c > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int)c;
        int skip = Math.min(count, remaining());
        offset += skip;
        return skip;
    }


    /**
     * Returns the stream's current position.
     * 
     * @return  the current position
     */
    public long position() throws IOException {
        return input.position() - buffer.length + offset;
    }


    /**
     * Seeks to the given position.  This method avoids re-filling the buffer
     * if at all possible.
     * 
     * @param p  the position to set
     * @throws IOException if an IO error occurs
     */
    public void position(long p) throws IOException {
        ensureBuffer();
        long blockStart = input.position() / buffer.length * buffer.length;
        long blockEnd = blockStart + buffer.length;
        if ((p >= blockStart) && (p < blockEnd)) {
            long adj = p - blockStart;
            offset += (int)adj;
        }
        positionDirect(p);
    }


    /**
     * Positions the underlying stream at the given position, then refills
     * the buffer.
     * 
     * @param p  the position to set
     * @throws IOException  if an IO error occurs
     */
    private void positionDirect(long p) throws IOException {
        long newBlockStart = p / buffer.length * buffer.length;
        input.position(newBlockStart);
        IoUtils.readFully(input, buffer);
        offset = (int)(p % buffer.length);        
    }


}
