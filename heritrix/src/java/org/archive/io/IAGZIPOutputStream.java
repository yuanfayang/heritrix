/* Copyright (C) 2003 Internet Archive.
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
 *
 * Created on Jun 20, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.archive.io;

import java.util.zip.GZIPOutputStream;
import java.util.zip.Deflater;
import java.io.OutputStream;
import java.io.IOException;

import org.archive.io.NullOutputStream;

/**
 * @author Parker Thompson
 *
 */
public class IAGZIPOutputStream extends GZIPOutputStream {

	// begin with the header block in progress
	private boolean blockWriteInProgress = true;


	/**
	 * Create a custom gzip output stream, writing IA header information.
	 * Normal gzip output streams are then used to write records to the file.
	 * @param o
	 * @throws IOException
	 */
	public IAGZIPOutputStream(OutputStream o) throws IOException {
		this(o, 512);
	}
	
	public IAGZIPOutputStream(OutputStream o, int size) throws IOException{
		// call super to satisfy java, let it do its thang with dev null
		super(new NullOutputStream());
		
		out = o;
		
		writeIAHeader();
		crc.reset();
		
		// end the IA Header Block
		//blockWriteInProgress = true;
		//endCompressionBlock();
	}
	
	/*
	 * GZIP header magic number.
	 */
	private final static int GZIP_MAGIC = 0x8b1f;
	
	// IA custom gzip header
	private byte[] iaHeader = { 31, (byte)139, 8,  4, 0, 0, 0, 0, 0, 3, 8, 0, 'L', 'X', 4, 0, 0, 0, 0, 0 };
	
	// standard GZIP header
	private final static byte[] header = {
		(byte) GZIP_MAGIC,                // Magic number (short)
		(byte)(GZIP_MAGIC >> 8),          // Magic number (short)
		Deflater.DEFLATED,                // Compression method (CM)
		0,                                // Flags (FLG)
		0,                                // Modification time MTIME (int)
		0,                                // Modification time MTIME (int)
		0,                                // Modification time MTIME (int)
		0,                                // Modification time MTIME (int)
		0,                                // Extra flags (XFLG)
		0                                 // Operating system (OS)
	};
	
	/**
	 * Writes IA Gzip header, rather than standard Gzip header
	 * @throws IOException
	 */
	protected void writeIAHeader() throws IOException {
		out.write(iaHeader);
	}
	
	protected void writeStandardHeader() throws IOException{
		out.write(header);
		crc.reset();
	}
	
	/**
	 * Call to begin a record.  This will flush anything 
	 * left in the buffer, end the previous record, and set
	 * us up to start writing anew.
	 * @throws IOException
	 */
	public void startCompressionBlock() throws IOException{
		if(blockWriteInProgress){
			endCompressionBlock();	  
		}
		
		writeStandardHeader();
		blockWriteInProgress = true;
	}
  	
	/**
	 * Flushes output buffer and gets the output stream and deflator
	 * ready to write a new record.
	 * @throws IOException
	 */
	public void endCompressionBlock() throws IOException{
		if(!blockWriteInProgress){
			return;
		}
		
		finish();
		def.reset();
		blockWriteInProgress = false;
	}
	
	// finish the compression block, but keep the deflater around
	// and don't close the steam.
	public void finish() throws IOException {
		if (!def.finished()) {
			def.finish();
			while (!def.finished()) {
				int len = def.deflate(buf, 0, buf.length);
				if (def.finished() && len <= buf.length - TRAILER_SIZE) {
					// last deflater buffer. Fit trailer at the end
					writeTrailer(buf, len);
					len = len + TRAILER_SIZE;
					out.write(buf, 0, len);
					return;
				}
				if (len > 0)
					out.write(buf, 0, len);
			}
			// if we can't fit the trailer at the end of the last
			// deflater buffer, we write it separately
			byte[] trailer = new byte[TRAILER_SIZE];
			writeTrailer(trailer, 0);
			out.write(trailer);
		}
	}	
	
	/** Tidy up and close the underlying stream
	 * @throws IOException
	 */
	public void close() throws IOException {
		endCompressionBlock();
		out.close();
	}
	
	// ibm stuff
	
	/*
	  * Writes GZIP member trailer to a byte array, starting at a given
	  * offset.
	  */
	 private void writeTrailer(byte[] buf, int offset) throws IOException {
		 writeInt((int)crc.getValue(), buf, offset); // CRC-32 of uncompr. data
		 writeInt(def.getTotalIn(), buf, offset + 4); // Number of uncompr. bytes
	 }

	 /*
	  * Writes integer in Intel byte order to a byte array, starting at a
	  * given offset.
	  */
	 private void writeInt(int i, byte[] buf, int offset) throws IOException {
		 writeShort(i & 0xffff, buf, offset);
		 writeShort((i >> 16) & 0xffff, buf, offset + 2);
	 }

	 /*
	  * Writes short integer in Intel byte order to a byte array, starting
	  * at a given offset
	  */
	 private void writeShort(int s, byte[] buf, int offset) throws IOException {
		 buf[offset] = (byte)(s & 0xff);
		 buf[offset + 1] = (byte)((s >> 8) & 0xff);
	 }
	 
	/*
	 * Trailer size in bytes.
	 *
	 */
	private final static int TRAILER_SIZE = 8;
}
