/* GzipHeader
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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;

/**
 * Read in the GZIP header.
 * 
 * See RFC1952 for specification on what the header looks like.
 * Assumption is that stream is cued-up with the gzip header as the
 * next thing to be read.
 * 
 * <p>Of <a href="http://jguru.com/faq/view.jsp?EID=13647">Java
 * and unsigned bytes</a>. That is, its always a signed int in
 * java no matter what the qualifier whether byte, char, etc.
 * 
 * <p>Add accessors for optional filename, comment and MTIME.
 * 
 * @author stack
 */
public class GzipHeader {

    /**
     * The GZIP header FLG byte.
     */
    protected int flg;
    
    /**
     * GZIP header XFL byte.
     */
	private int xfl;
    
    /**
     * GZIP header OS byte.
     */
	private int os;
    
    /**
     * Extra header field content.
     */
    private byte [] fextra = null;
    
    /**
     * GZIP header MTIME field.
     */
    private int mtime;
    
    
	/**
	 * Shutdown constructor.
     * 
     * Must pass an input stream.
	 */
	protected GzipHeader() {
		super();
	}
    
    /**
     * Constructor.
     * 
     * This constructor advances the stream past any gzip header found
     * else, throws IOException if problems getting the gzip header.
     * 
     * @param in InputStream to read from.
     */
    protected GzipHeader(InputStream in) throws IOException {
        super();
        
        CRC32 crc = new CRC32();
        crc.reset();
   
        if (readShort(in, crc) != GZIPInputStream.GZIP_MAGIC) {
            throw new IOException("Failed to find GZIP MAGIC");
        }   
   
        if (readByte(in, crc) != Deflater.DEFLATED) {
            throw new IOException("Unknown compression");
        }   
       
        // Get gzip header flag.
        this.flg = readByte(in, crc);
        
        // Get MTIME.
        this.mtime = readInt(in, crc);
        
        // Read XFL and OS.
        this.xfl = readByte(in, crc);
        this.os = readByte(in, crc);
        
        // Skip optional extra field -- stuff w/ alexa stuff in it.
        final int FLG_FEXTRA = 4;
        if ((this.flg & FLG_FEXTRA) == FLG_FEXTRA) {
            int count = readShort(in, crc);
            this.fextra = new byte[count];
            readByte(in, crc, this.fextra, 0, count);
        }   
        
        // Skip file name.  It ends in null.
        final int FLG_FNAME  = 8;
        if ((this.flg & FLG_FNAME) == FLG_FNAME) {
            while (readByte(in, crc) != 0) {
                continue;
            }
        }   
        
        // Skip file comment.  It ends in null.
        final int FLG_FCOMMENT = 16;   // File comment
        if ((this.flg & FLG_FCOMMENT) == FLG_FCOMMENT) {
            while (readByte(in, crc) != 0) {
                continue;
            }
        }
        
        // Check optional CRC.
        final int FLG_FHCRC  = 2;
        if ((this.flg & FLG_FHCRC) == FLG_FHCRC) {
            int calcCrc = (int)(crc.getValue() & 0xffff);
            if (readShort(in, crc) != calcCrc) {
                throw new IOException("Bad header CRC");
            }
        }
    }
    
    /**
     * Read an int. 
     * 
     * We do not expect to get a -1 reading.  If we do, we throw exception.
     * Update the crc as we go.
     * 
     * @param in InputStream to read.
     * @param crc CRC to update.
     * @return int read.
     * 
     * @throws IOException
     */
    private int readInt(InputStream in, CRC32 crc) throws IOException {
        int s = readShort(in, crc);
        return ((readShort(in, crc) << 16) & 0xffff0000) | s;
    }
    
    /**
     * Read a short. 
     * 
     * We do not expect to get a -1 reading.  If we do, we throw exception.
     * Update the crc as we go.
     * 
     * @param in InputStream to read.
     * @param crc CRC to update.
     * @return Short read.
     * 
     * @throws IOException
     */
    private int readShort(InputStream in, CRC32 crc) throws IOException {
        int b = readByte(in, crc);
        return ((readByte(in, crc) << 8) & 0x00ff00) | b;
    }
    
    /**
     * Read a byte. 
     * 
     * We do not expect to get a -1 reading.  If we do, we throw exception.
     * Update the crc as we go.
     * 
     * @param in InputStream to read.
     * @param crc CRC to update.
     * @return Byte read.
     * 
     * @throws IOException
     */
    protected int readByte(InputStream in, CRC32 crc) throws IOException {
        int b = in.read();
        if (b == -1) {
            throw new EOFException();
        }
        crc.update(b);
        return b & 0xff;
    }
    
    
    /**
     * Read a byte. 
     * 
     * We do not expect to get a -1 reading.  If we do, we throw exception.
     * Update the crc as we go.
     * 
     * @param in InputStream to read.
     * @param crc CRC to update.
     * @param buffer Buffer to read into.
     * @param offset Offset to start filling buffer at.
     * @param length How much to read.
     * @return Bytes read.
     * 
     * @throws IOException
     */
    protected int readByte(InputStream in, CRC32 crc, byte [] buffer,
                int offset, int length)
            throws IOException {
        for (int i = offset; i < length; i++) {
            buffer[offset + i] = (byte)readByte(in, crc);   
        }
        return length;
    }
    
	/**
	 * @return Returns the fextra.
	 */
	public byte[] getFextra() {
		return this.fextra;
	}
    
	/**
	 * @return Returns the flg.
	 */
	public int getFlg() {
		return this.flg;
	}
    
	/**
	 * @return Returns the os.
	 */
	public int getOs() {
		return this.os;
	}
    
	/**
	 * @return Returns the xfl.
	 */
	public int getXfl() {
		return this.xfl;
	}
    
	/**
	 * @return Returns the mtime.
	 */
	public int getMtime() {
		return this.mtime;
	}
}
