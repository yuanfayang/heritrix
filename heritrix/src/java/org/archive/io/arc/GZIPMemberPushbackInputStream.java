/* IAGZIPMemberPushBackInputStream
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
package org.archive.io.arc;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
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
 * file.   {@link #next()} returns -1 if we're at EOF.
 * 
 * <p>We subclass PushbackInputStream so we can unread bytes as soon as we 
 * notice we've stepped into the next GZIP member.  BufferedInputStream won't
 * work because I'd have to mark every byte read just so I could read back.
 * 
 * <p>{@link java.util.zip.ZipFile} doesn't work going against GZIP files
 * composed of multiple GZIP members.
 * 
 * <p>TODO: All reads go via the read() method.  There may be performance
 * improvements to be won making the buffer read go direct to the underlying
 * streams buffer read scanning what comes back for GZIP member begin.
 * 
 * @author stack
 */
public class GZIPMemberPushbackInputStream extends PushbackInputStream
{
    /**
     * Length of minimual 'default GZIP header.
     * 
     * See RFC1952 for explaination of value of 10.
     */
    private static final int DEFAULT_GZIP_HEADER_LENGTH = 10;
    
    /**
     * Flag is set when we are to read the GZIP member that sitting in 
     * the stream.  Flag is false when we come across the end of a GZIP member.
     * Reading of next GZIP member in the stream is blocked till this flag is
     * set to true again. 
     */
    private boolean readGZIPMember = true;
    
    
    public GZIPMemberPushbackInputStream(InputStream in)
    {
        super(in, DEFAULT_GZIP_HEADER_LENGTH);
    }
    
    /**
     * @return True if more GZIP members in the stream.
     * @throws IOException
     */
    public boolean hasNext()
        throws IOException
    {
        testIsOpen();
        return super.available() > 0;
    }
    
    /**
     * Moves us on to the next GZIP member.
     * 
     * Call {@link #hasNext()} before calling this method else you'll get a
     * IOException at the end of the stream.
     * 
     * @throws IOException
     */
    public void next()
        throws IOException
    {
        testIsOpen();
        if (!hasNext())
        {
            throw new IOException("No more GZIP members.");
        }
        this.readGZIPMember = true;
    }
    
    
    public int read()
        throws IOException
    {
        testIsOpen();
        int c = super.read();
        if (c == (byte)GZIPInputStream.GZIP_MAGIC)
        {
            // Put byte we just read back on the stream so we can test for 
            // GZIP member header.   Not doing this can put the underlying
            // pushback buffer's knickers into a twist.
            super.unread(c);
            if (testGZIPHeader())
            {
                if(!this.readGZIPMember)
                {
                    // Its a GZIP header and we're not supposed to let it out.
                    // Return a -1 signifying end of GZIP member.
                    c = -1;
                }
                else
                {
                    // It is a GZIP header but we are supposed to let it out.
                    // Go get the character again.  Take this opportunity to 
                    // clear the readGZIPMember flag so we stop at end of this
                    // GZIP member.
                    c = super.read();
                    this.readGZIPMember = false;
                }
            }
            else
            {
                // Its not a GZIP header.  Get the character we unread and 
                // let it out.
                c = super.read();
            }
        }
        return c;
    }
    
    public int read(byte [] buf, int off, int len)
        throws IOException
    {
        testIsOpen();
        int c = -1;
        int i = 0;
        for (; (i < len) && ((c = read()) != -1); i++)
        {
            buf[off + i] = (byte)c;
        }
        return (i == 0 && len > 0)? -1 : i;
    }
    
    /**
     * Test for GZIP header.
     * 
     * This methods assumes that we've just read the first byte of 
     * GZIP_MAGIC.  This method will look ahead in the buffer to see if we're
     * upon a GZIP header.
     * 
     * @return True if buffer begins w/ a .
     */
    protected boolean testGZIPHeader()
        throws IOException
    {   
        boolean gzipHeader = false;
        
        // Read the next DEFAULT_GZIP_HEADER_LENGTH - 1 candidate header
        // bytes.
        byte [] header = new byte[DEFAULT_GZIP_HEADER_LENGTH];
        int read = super.read(header, 0, DEFAULT_GZIP_HEADER_LENGTH);
        if (read == DEFAULT_GZIP_HEADER_LENGTH)
        {
            if (header[0] == (byte) GZIPInputStream.GZIP_MAGIC
                && header[1] == (byte)(GZIPInputStream.GZIP_MAGIC >> 8)
                && header[2] == Deflater.DEFLATED)
            {
                // The top bits of the FLG byte are reserved.  Assume they 
                // are always zero.  This may not be correct.
                if (header[3] < 0x20)
                {
                    // The XFL field when using the default (CM = 8) method
                    // can only be 2 or 4 -- nothing else according to RFC1952.
                    // So if anything but these two flags are set, then reject
                    // header.
                    if ((header[8] | 0xf6) != 0)
                    {
                        gzipHeader = true;
                    }
                }
            }
        }
        
        // Put back into the buffer all read beyond current byte.
        if (read != -1)
        {
            super.unread(header, 0, read);
        }
        
        return gzipHeader;
    }
    
    /**
     * Test stream is open.
     * 
     * @exception IOException If stream is null.
     */
    private void testIsOpen()
        throws IOException
    {
        if (this.in == null)
        {    
            throw new IOException("Stream is null (closed?).");
        }
    }
}
