/*
 * Created on Jun 20, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.archive.util;

import java.util.zip.GZIPOutputStream;
import java.util.zip.Deflater;
import java.io.OutputStream;
import java.io.IOException;
import org.archive.util.NullOutputStream;

/**
 * @author Parker Thompson
 *
 */
public class IAGZIPOutputStream extends GZIPOutputStream {

	/**
	 * Create a custom gzip output stream, writing IA header information.
	 * Normal gzip output streams are then used to write records to the file.
	 * @param outputstream
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
	 */
	public void startCompressionBlock() throws IOException{
		endCompressionBlock();	  
		writeStandardHeader();
	}
  	
	/**
	 * Flushes output buffer and gets the output stream and deflator
	 * ready to write a new record.
	 */
	protected void endCompressionBlock() throws IOException{
		finish();
		def.reset();
	}
}
