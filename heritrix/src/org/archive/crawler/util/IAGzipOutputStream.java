/*
 * Created on Jun 20, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.archive.crawler.util;

import java.util.zip.GZIPOutputStream;
import java.util.zip.Deflater;
import java.io.OutputStream;
import java.io.IOException;

/**
 * @author Parker Thompson
 *
 */
public class IAGzipOutputStream extends GZIPOutputStream {

	/**
	 * Create a custom gzip output stream, writing IA header information.
	 * Normal gzip output streams are then used to write records to the file.
	 * @param outputstream
	 * @throws IOException
	 */
	public IAGzipOutputStream(OutputStream o) throws IOException {
		super(o);
	}
	
	/*
	 * GZIP header magic number.
	 */
	private final static int GZIP_MAGIC = 0x8b1f;
	
	private final static byte[] header = {
		(byte)GZIP_MAGIC,                // Magic number (short)
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
	
	private byte[] iaHeader = { 31, (byte)139, 8,  4, 0, 0, 0, 0, 0, 3, 8, 0, 'L', 'X', 4, 0, 0, 0, 0, 0, 0 };

	/**
	 * Writes IA Gzip header, rather than standard Gzip header
	 * @throws IOException
	 */
	protected void writeHeader() throws IOException {
		out.write(iaHeader);
	}
	
	/**
	 * Start a record by writing a standard gzip header
	 */
	public void writeRecordHeader() throws IOException{
		out.write(header);
	}

}
