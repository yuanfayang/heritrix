/* GZIPMemberInputStream
 *
 * $Id$
 *
 * Created on Jan 16, 2004
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
import java.nio.BufferOverflowException;
import java.nio.MappedByteBuffer;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;

/**
 * Read a stream of GZIP members.
 *
 * This stream is to be put under a GZIPInputStream.  GZIPInputStream reads to
 * the EOF finding only the first GZIP member in the file even if the file is
 * made up of multiple GZIP members.  This stream is for spoon feeding
 * GZIPInputStream GZIP members.  It returns -1 on reads done at a GZIP member
 * boundary.  Use {@link #next()} to move reading on to the next GZIP in the
 * file.
 *
 * <p>We subclass MappedByteBufferInputStream so we can unread bytes as soon as
 * we notice we've stepped into the next GZIP member.  BufferedInputStream
 * won't work because I'd have to mark every byte read just so I could read
 * back (PushbackBufferInputStream works though with a buffer of the size of
 * the gzip header).
 *
 * <p>{@link java.util.zip.ZipFile} doesn't work going against GZIP files
 * composed of multiple GZIP members so had choice but to do the below.
 *
 * <p>TODO: All reads go via the read() method.  There may be performance
 * improvements to be won making the buffer read go direct to the underlying
 * streams buffer read scanning what comes back for GZIP member begin.
 *
 * @author stack
 */
public class GZIPMemberInputStream
    extends MappedByteBufferInputStream
        implements Iterator {
    /**
     * Length of minimual 'default GZIP header.
     *
     * See RFC1952 for explaination of value of 10.
     */
    public static final int DEFAULT_GZIP_HEADER_LENGTH = 10;

    /**
     * Flag is set when we are to read the GZIP member that is sitting in
     * the stream.  Flag is false when we come across the end of a GZIP member.
     * Reading of next GZIP member in the stream is blocked till this flag is
     * set to true again.
     */
    private boolean readGZIPMember = true;


    public GZIPMemberInputStream(MappedByteBuffer mbb) {
        super(mbb);
    }

    /**
     * @return True if more GZIP members in the stream.
     * @throws RuntimeException We convert IOException to RuntimeException
     * so can adhere to Iterator interface.
     */
    public boolean hasNext() {
        boolean result = false;
        try {
			checkClosed();
            result = (available() > 0);
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage());
		}
        return result;
    }

    /**
     * Moves us on to the next GZIP member.
     *
     * Call {@link #hasNext()} before calling this method else you'll get a
     * IOException at the end of the stream.
     * 
     * @return Returns null (Just so we align with the Iterator interface).
     */
    public Object next() {
        try {
			checkClosed();
		} catch (IOException e) {
			throw new RuntimeException(e.getClass() + ": " + e.getMessage());
		}
        if (!hasNext()) {
            throw new NoSuchElementException("No more GZIP members.");
        }
        this.readGZIPMember = true;
        return null;
    }

    /* (non-Javadoc)
     * @see java.util.Iterator#remove()
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }

    public int read()
            throws IOException {
    	    checkClosed();
    	    int c = -1;
    	    if (available() > 0) {
    	    	    // See this on java and unsigned bytes:
    	    	    // http://jguru.com/faq/view.jsp?EID=13647
    	    	    // That is, its always a signed int in java no matter what
            // the qualifier whether byte, char, etc.
    	    	    c = super.read() & 0xff;
    	    	    if ((byte)c == (byte)GZIPInputStream.GZIP_MAGIC) {
    	    	    	    // Put byte we just read back on the stream so can
                // test for GZIP member header. Makes the processing
                // below easier to grok.
    	    	    	    setPosition(getPosition() - 1);
    	    	    	    if (testGZIPHeader()) {
    	    	    	    	    if(!this.readGZIPMember) {
    	    	    	    	    	    // Its a GZIP header and we're not supposed to
                        // let it out. Return a -1 signifying end of GZIP
                        // member.
    	    	    	    	    	    c = -1;
    	    	    	    	    } else {
    	    	    	    	    	    // It is a GZIP header but we are supposed to let
                        // it out. Go get the character again.  Take this
                        // opportunity to clear the readGZIPMember flag
                        // so we stop at end of this GZIP member.
    	    	    	    	    	    c = super.read() & 0xff;
    	    	    	    	    	    this.readGZIPMember = false;
    	    	    	    	    }
    	    	    	    } else {
    	    	    	    	    // Its not a GZIP header.  Get the character we unread
                    // and let it out.
    	    	    	    	    c = super.read() & 0xff;
    	    	    	    }
    	    	    }
    	    }
    	    return c;
    }

    /*
    public int read(byte [] buf, int off, int len)
            throws IOException {
        checkClosed();
        int c = -1;
        int i = 0;
        for (; (i < len) && ((c = read()) != (byte)-1); i++) {
            buf[off + i] = (byte)c;
        }
        return (i == 0 && len > 0)? -1 : i;
    }*/

    /**
     * Test for GZIP header.
     *
     * Look ahead in the buffer to see if we're upon a GZIP header.
     *
     * @return True if buffer begins w/ gzip header.
     */
    protected boolean testGZIPHeader()
            throws IOException {
        boolean gzipHeader = false;
        if (available() < DEFAULT_GZIP_HEADER_LENGTH) {
            // Quick return.
            return gzipHeader;
        }
        int position = getPosition();

        // Read the next DEFAULT_GZIP_HEADER_LENGTH candidate header bytes.
        byte [] header = new byte[DEFAULT_GZIP_HEADER_LENGTH];
        int read = DEFAULT_GZIP_HEADER_LENGTH;
        try {
            getMappedByteBuffer().get(header);
        } catch (BufferOverflowException e) {
        	    read = -1;
        }
        if (read == DEFAULT_GZIP_HEADER_LENGTH) {
            if (header[0] == (byte) GZIPInputStream.GZIP_MAGIC
                && header[1] == (byte)(GZIPInputStream.GZIP_MAGIC >> 8)
                && header[2] == Deflater.DEFLATED) {
                // The top bits of the FLG byte are reserved.  Assume they
                // are always < 0x20 for our case.
                if (header[3] < 0x20) {
                    // The XFL field when using the default (CM = 8) method
                    // can only be 2 or 4 -- nothing else according to RFC1952.
                    // So if anything but these two flags are set, then reject
                    // header.
                    if ((header[8] | 0xf6) != 0) {
                        gzipHeader = true;
                    }
                }
            }
        }

        // Put back into the buffer all read.
        setPosition(position);

        return gzipHeader;
    }
}
