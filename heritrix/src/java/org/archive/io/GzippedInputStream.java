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
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;


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
 * <p>If you need to know start of a gzip header, call {@link #getFilePointer()}
 * just after a call to {@link #hasNext()} and before you call {@link #next()}.
 * 
 * @author stack
 */
public class GzippedInputStream extends GZIPInputStream
    implements Iterator, PositionableStream {
    
    /**
     * Length of minimual 'default GZIP header.
     *
     * See RFC1952 for explaination of value of 10.
     */
    public static final int MINIMAL_GZIP_HEADER_LENGTH = 10;
    
    /**
     * Used to find next gzip member.
     */
    private GzipHeader gzipHeader = new GzipHeader();

    private static final byte [] SKIP_BUFFER = new byte [4096];
    
    
    public GzippedInputStream(InputStream in) throws IOException {
        this(in, 512);
    }
    
    public GzippedInputStream(InputStream in, int size) throws IOException {
        super(in, size);
        if (!(this.in instanceof PositionableStream)) {
            throw new IOException("Passed stream does not" +
                    " implement PositionableStream");
        }
    }
    
    /**
     * Exhaust current GZIP member content.
     * Call this method when you think you're on the end of the
     * GZIP member.  It will clean out any dross.
     * @param ignore Character to ignore counting characters (Usually
     * trailing new lines).
     * @return Count of characters skipped over.
     * @throws IOException
     */
    public long gotoEOR(int ignore) throws IOException {
        long bytesSkipped = 0;
        if (this.inf.getTotalIn() <= 0) {
            return bytesSkipped;
        }
        if (!this.inf.finished()) {
            int read = 0;
            while ((read = read()) != -1) {
                if ((byte)read == (byte)ignore) {
                    continue;
                }
                bytesSkipped = gotoEOR() + 1;
                break;
            }
        }
        return bytesSkipped;
    }
    
    /**
     * Exhaust current GZIP member content.
     * Call this method when you think you're on the end of the
     * GZIP member.  It will clean out any dross.
     * @return Count of characters skipped over.
     * @throws IOException
     */
    public long gotoEOR() throws IOException {
        long bytesSkipped = 0;
        if (this.inf.getTotalIn() <= 0) {
            return bytesSkipped;
        }
        if (!this.inf.finished()) {
            long read = 0;
            while ((read = read(SKIP_BUFFER)) != -1) {
                bytesSkipped += read;
            }
        }
        return bytesSkipped;
    }
    
    public boolean hasNext() {
        if (this.inf.getTotalIn() == 0) {
            // We haven't read anything yet.  Must be at start of file.
            return true;
        }
        
        boolean result = false;
        // Move to the next gzip member, if there is one, positioning
        // ourselves by backing up the stream so we reread any inflater
        // remaining bytes.  Then add 8 bytes to get us past the GZIP
        // CRC trailer block that ends all gzip members.
        try {
            PositionableStream ps = (PositionableStream)this.in;
            // 8 is sizeof gzip CRC block thats on tail of gzipped record.
            // If remaining is < 8 then experience indicates we're seeking past
            // the gzip header -- don't backup the stream.
            if (this.inf.getRemaining() > 8) {
                ps.seek(getFilePointer() - this.inf.getRemaining() + 8);
            }
            // If at least MINIMAL_GZIP_HEADER_LENGTH available
            // assume possible other gzip member. Move the
            // stream on checking as we go to be sure of another record.
            // We do this slow poke ahead because the calculation using
            // this.inf.getRemaining can be off by a couple if the
            // remaining is zero.  There seems to be
            // nothing in gzipinputstream nor in the inflater that can be
            // relied upon:  this.eos = false, this.inf.finished = false,
            // this.inf.readEOF is true. I don't know why its messed up
            // like this.  Study the core zlib and see if can figure where
            // inflater is going wrong.
            int read = -1;
            int headerRead = 0;
            while (this.in.available() > MINIMAL_GZIP_HEADER_LENGTH) {
                read = this.gzipHeader.readByte(this.in);
                if ((byte)read == (byte)GZIPInputStream.GZIP_MAGIC) {
                    headerRead++;
                    read = this.gzipHeader.readByte(this.in);
                    if((byte)read == (byte)(GZIPInputStream.GZIP_MAGIC >> 8)) {
                        headerRead++;
                        read = this.gzipHeader.readByte(this.in);
                        if ((byte)read == Deflater.DEFLATED) {
                            headerRead++;
                            // Found gzip header.  Backup the stream the
                            // two bytes we just found and set result true.
                            ps.seek(getFilePointer() - headerRead);
                            result = true;
                            break;
                        }
                    }
                    // Didn't find gzip header.  Back up stream one
                    // byte because the byte just read might be the
                    // actual start of the gzip header. Needs testing.
                    ps.seek(getFilePointer() - headerRead);
                    headerRead = 0;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed i/o: " +
                    e.getMessage());
        }
        
        return result;
    }
    
    /**
     * @return An InputStream.
     */
    public Object next() {
        // Assume inflater has been reset.  Read in header.
        try {
            readHeader();
        } catch (IOException e) {
            throw new RuntimeException("Failed header read: " +
                    e.getMessage());
        }
        return this;
    }
    
    public void remove() {
        throw new UnsupportedOperationException();
    }
    
    public Iterator iterator() {
        try {
            seek(0);
        } catch (IOException e) {
            throw new RuntimeException("Failed seeking to 0: " +
                e.getMessage());
        }
        return this;   
    }
    
    /**
     * Move to next gzip member in the file.
     */
    protected void resetInflater() {
        this.eos = false;
        this.inf.reset();
    }
    
    /**
     * Read in the gzip header.
     * @throws IOException
     */
    protected void readHeader() throws IOException {
        new GzipHeader(this.in);
        // Reset the crc for subsequent reads.
        this.crc.reset();
    }

    /**
     * Seek to passed offset.
     * 
     * After positioning the stream, it resets the inflater.
     * 
     * @param position Absolute position of a gzip member start.
     */
    public void seek(long position) throws IOException {
        ((PositionableStream)this.in).seek(position);
        resetInflater();
    }

    public long getFilePointer() throws IOException {
       return  ((PositionableStream)this.in).getFilePointer();
    }
    
    /**
     * Seek to a gzip member.
     * 
     * Moves stream to new position, resets inflater and reads in the gzip
     * header ready for subsequent calls to read.
     * 
     * @param position Absolute position of a gzip member start.
     */
    public void gzipMemberSeek(long position) throws IOException {
        seek(position);
        readHeader();
    }
}
