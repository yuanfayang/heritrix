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

import it.unimi.dsi.mg4j.io.RepositionableStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;


/**
 * Subclass of GZIPInputStream that can handle a stream made of multiple
 * concatenated GZIP members/records.
 * 
 * This class is needed because GZIPInputStream only finds the first GZIP
 * member in the file even if the file is made up of multiple GZIP members.
 * 
 * <p>Takes an InputStream streams that implements
 * {@link RepositionableStream} interface so it can backup overreads done
 * by the zlib Inflater class.
 * 
 * <p>Use the {@link #iterator()} method to get a gzip member iterator.
 * Calls to {@link Iterator#next()} returns the next gzip member in the
 * stream.  Cast return from {@link Iterator#next()} to InputStream.
 * 
 * <p>Use {@link #gzipMemberSeek(long)} to position stream before reading
 * a gzip member if doing random accessing of gzip members.  Pass it offset
 * at which gzip member starts.
 * 
 * <p>If you need to know position at which a gzip member starts, call
 * {@link #position()} just after a call to {@link Iterator#hasNext()}
 * and before you call {@link Iterator#next()}.
 * 
 * @author stack
 */
public class GzippedInputStream
extends GZIPInputStream
implements RepositionableStream {
    
    /**
     * Length of minimal GZIP header.
     *
     * See RFC1952 for explaination of value of 10.
     */
    public static final int MINIMAL_GZIP_HEADER_LENGTH = 10;
    
    /**
     * Tail on gzip members (The CRC).
     */
    private static final int GZIP_TRAILER_LENGTH = 8;
    
    /**
     * Utility class used probing for gzip members in stream.
     * We need this instance to get at the readByte method.
     */
    private GzipHeader gzipHeader = new GzipHeader();
    
    /**
     * Buffer size used skipping over gzip members.
     */
    private static final int LINUX_PAGE_SIZE = 4 * 1024;

    /**
     * Buffer used skipping over gzip members.
     */
    private static final byte [] SKIP_BUFFER = new byte [LINUX_PAGE_SIZE];
    
    
    public GzippedInputStream(InputStream is) throws IOException {
        // Have buffer match linux page size.
        this(is, LINUX_PAGE_SIZE);
    }
    
    public GzippedInputStream(InputStream is, int size)
    throws IOException {
        super(is, size);
        if (!(this.in instanceof RepositionableStream)) {
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
        // If at start of file, we ain't setup to read.
        if (position() == 0) {
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
    
    public Iterator iterator() {
        try {
            // Reset.
            position(0);
        } catch (IOException e) {
            throw new RuntimeException("Failed seeking to 0: " +
                e.getMessage());
        }
        // Need pointer to compressed stream to pass the Iterator.
        final GzippedInputStream gis = this;
        
        // Return an anonymous iterator instance.
        return new Iterator() {
            private GzippedInputStream compressedStream = gis;
            public boolean hasNext() {
                try {
                    gotoEOR();
                } catch (IOException e) {
                    throw new RuntimeException(e.getMessage());
                }
                return moveToNextGzipMember();
            }
            
            /**
             * @return An InputStream.
             */
            public Object next() {
                try {
                    gzipMemberSeek();
                } catch (IOException e) {
                    throw new RuntimeException("Failed move to EOR or " +
                        "failed header read: " + e.getMessage());
                }
                return this.compressedStream;
            }
            
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };   
    }
    
    /**
     * @return True if we found another record in the stream.
     */
    protected boolean moveToNextGzipMember() {
        boolean result = false;
        // Move to the next gzip member, if there is one, positioning
        // ourselves by backing up the stream so we reread any inflater
        // remaining bytes. Then add 8 bytes to get us past the GZIP
        // CRC trailer block that ends all gzip members.
        try {
            RepositionableStream ps =
                (RepositionableStream)getInputStream();
            // 8 is sizeof gzip CRC block thats on tail of gzipped
            // record. If remaining is < 8 then experience indicates
            // we're seeking past the gzip header -- don't backup the
            // stream.
            if (getInflater().getRemaining() > GZIP_TRAILER_LENGTH) {
                ps.position(position() - getInflater().getRemaining() +
                    GZIP_TRAILER_LENGTH);
            }
            // If at least MINIMAL_GZIP_HEADER_LENGTH available assume
            // possible other gzip member. Move the stream on checking
            // as we go to be sure of another record. We do this slow
            // poke ahead because the calculation using
            // this.inf.getRemaining can be off by a couple if the
            // remaining is zero.  There seems to be nothing in
            // gzipinputstream nor in the inflater that can be relied
            // upon:  this.eos = false, this.inf.finished = false,
            // this.inf.readEOF is true. I don't know why its messed up
            // like this.  Study the core zlib and see if can figure
            // where inflater is going wrong.
            int read = -1;
            int headerRead = 0;
            while (getInputStream().available() >
                    MINIMAL_GZIP_HEADER_LENGTH) {
                read = getGzipHeader().readByte(getInputStream());
                if ((byte)read == (byte)GZIPInputStream.GZIP_MAGIC) {
                    headerRead++;
                    read = getGzipHeader().readByte(getInputStream());
                    if((byte)read ==
                            (byte)(GZIPInputStream.GZIP_MAGIC >> 8)) {
                        headerRead++;
                        read =
                            getGzipHeader().readByte(getInputStream());
                        if ((byte)read == Deflater.DEFLATED) {
                            headerRead++;
                            // Found gzip header. Backup the stream the
                            // two bytes we just found and set result
                            // true.
                            ps.position(position() - headerRead);
                            result = true;
                            break;
                        }
                    }
                    // Didn't find gzip header.  Back up stream one
                    // byte because the byte just read might be the
                    // actual start of the gzip header. Needs testing.
                    ps.position(position() - headerRead);
                    headerRead = 0;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed i/o: " + e.getMessage());
        }
        return result;
    }
  
    protected Inflater getInflater() {
        return this.inf;
    }
    
    protected InputStream getInputStream() {
        return this.in;
    }
    
    protected GzipHeader getGzipHeader() {
        return this.gzipHeader;
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
     * Assumption is that public use of this method is only
     * to position stream at start of a gzip member.
     * 
     * @param position Absolute position of a gzip member start.
     * @throws IOException
     */
    public void position(long position) throws IOException {
        ((RepositionableStream)this.in).position(position);
        resetInflater();
    }

    public long position() throws IOException {
       return  ((RepositionableStream)this.in).position();
    }
    
    /**
     * Seek to a gzip member.
     * 
     * Moves stream to new position, resets inflater and reads in the gzip
     * header ready for subsequent calls to read.
     * 
     * @param position Absolute position of a gzip member start.
     * @throws IOException
     */
    public void gzipMemberSeek(long position) throws IOException {
        position(position);
        readHeader();
    }
    
    public void gzipMemberSeek() throws IOException {
        gzipMemberSeek(position());
    }
    
    /**
     * Gzip passed bytes.
     * Use only when bytes is small.
     * @param bytes What to gzip.
     * @return A gzip member of bytes.
     * @throws IOException
     */
    public static byte [] gzip(byte [] bytes) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPOutputStream gzipOS = new GZIPOutputStream(baos);
        gzipOS.write(bytes, 0, bytes.length);
        gzipOS.close();
        return baos.toByteArray();
    }
}
