/*
 * WARCWriter
 *
 * $Id$
 *
 * Created on Jun 5, 2003
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
package org.archive.io.warc;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.archive.io.GzippedInputStream;
import org.archive.io.ReplayInputStream;
import org.archive.io.WriterPoolMemberImpl;
import org.archive.util.ArchiveUtils;
import org.archive.util.DevUtils;
import org.archive.util.MimetypeUtils;
import org.archive.util.TimestampSerialno;
import org.mortbay.jetty.servlet.WebApplicationContext;


/**
 * Experimental WARC implementation.
 * Based on as unreleased version of <a 
 * href="http://archive-access.sourceforge.net//warc/warc_file_format.html">WARC
 * File Format</a> document.
 *
 * Assumption is that the caller is managing access to this WARCWritexr ensuring
 * only one thread accessing this WARC instance at any one time.
 * 
 * <p>While being written, WARCs have a '.open' suffix appended.
 *
 * @author stack
 */
public class WARCWriter extends WriterPoolMemberImpl implements WARCConstants {
    private final Logger logger = Logger.getLogger(this.getClass().getName());
    
    /**
     * Buffer to reuse writing streams.
     */
    private final byte [] readbuffer = new byte[16 * 1024];
    
    private final List metadata;
    
    public static final byte [] NEWLINE_BYTES = null;
    static {
        try {
            NEWLINE.getBytes(DEFAULT_ENCODING);
        } catch(Exception e) {
            e.printStackTrace();
        }
    };
    
    /**
     * Shutdown Constructor
     * For unit tests only.
     */
    WARCWriter() {
        this(null, "", true, -1);
    }
    
    /**
     * Constructor.
     * Takes a stream.
     * @param out Where to write.
     * @param f File the <code>out</code> is connected to.
     * @param cmprs Compress the content written.
     * @param metadata File meta data.  Can be null.  Is list of File and/or
     * String objects.
     * @param a14DigitDate If null, we'll write current time.
     * @throws IOException
     */
    public WARCWriter(final PrintStream out, final File f,
            final boolean cmprs, String a14DigitDate, final List metadata)
    throws IOException {
        super(out, f, cmprs, a14DigitDate);
        this.metadata = metadata;
    }
    
    /**
     * Constructor.
     *
     * @param dirs Where to drop files.
     * @param prefix File prefix to use.
     * @param cmprs Compress the records written. 
     * @param maxSize Maximum size for ARC files written.
     */
    public WARCWriter(final List dirs, final String prefix, 
            final boolean cmprs, final int maxSize) {
        this(dirs, prefix, "", cmprs, maxSize, null);
    }
            
    /**
     * Constructor.
     *
     * @param dirs Where to drop files.
     * @param prefix File prefix to use.
     * @param cmprs Compress the records written. 
     * @param maxSize Maximum size for ARC files written.
     * @param suffix File tail to use.  If null, unused.
     * @param meta File meta data.  Can be null.  Is list of File and/or
     * String objects.
     */
    public WARCWriter(final List dirs, final String prefix, 
            final String suffix, final boolean cmprs,
            final int maxSize, final List meta) {
        super(dirs, prefix, suffix, cmprs, maxSize, WARC_FILE_EXTENSION);
        this.metadata = meta;
    }

    /**
     * Create an WARC file.
     *
     * @throws IOException
     */
    protected TimestampSerialno createFile()
    throws IOException {
        TimestampSerialno tsn = super.createFile();
        writeWarcinfoRecord(tsn);
        return tsn;
    }
    
    protected String checkHeaderLineValue(final String value)
    throws IOException {
        // TODO: A version that will replace at least space and tab.
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) <= ' ') {
                throw new IOException("Contains illegal character: " + value);
            }
        }
        return value;
    }
    
    protected String getUid() {
        // TODO:
        return "unique-id-todo";
    }
    
    protected String createRecordHeaderline(final String type, final String url,
            final String mimetype, final long contentLength)
    throws IOException {
        final String timestamp = ArchiveUtils.get14DigitDate();
        final String id = getUid();
        
        // Calculate total length.
        long length = WARC_ID.length();
        length += SEPARATORS_PLUS_LENGTH_WIDTH_PLUS_NEWLINE;
        length += checkHeaderLineValue(type).length();
        length += checkHeaderLineValue(url).length();
        length += timestamp.length();
        length += checkHeaderLineValue(type).length();
        length += id.length();
        length += contentLength;
        
        final String lenStr = Long.toString(length);
       
        StringBuffer sb = new StringBuffer();
        sb.append(WARC_ID);
        sb.append(HEADER_FIELD_SEPARATOR);
        sb.append(lenStr);
        for (int i = 0; i < (LENGTH_WIDTH - lenStr.length()); i++) {
            sb.append(HEADER_FIELD_SEPARATOR);
        }
        sb.append(type);
        sb.append(HEADER_FIELD_SEPARATOR);
        sb.append(url);
        sb.append(HEADER_FIELD_SEPARATOR);
        sb.append(timestamp);
        sb.append(HEADER_FIELD_SEPARATOR);
        sb.append(mimetype);
        sb.append(HEADER_FIELD_SEPARATOR);
        sb.append(id);
        
        return sb.toString();
    }
    
    protected void writeRecord(final String type, final String url,
            final String mimetype, final InputStream contentStream,
            final long contentLength)
    throws IOException {
        preWriteRecordTasks();
        try {
            String header = createRecordHeaderline(type, url, mimetype,
                contentLength);
            getOutputStream().write(header.getBytes(DEFAULT_ENCODING));
            // Write out the two blank lines we put at end of all records.
            // TODO: Why? Not in grammar.
            getOutputStream().write(NEWLINE_BYTES);
            getOutputStream().write(NEWLINE_BYTES);
        } finally {
            postWriteRecordTasks();
        }
    }
    
    private void writeWarcinfoRecord(final TimestampSerialno tsn)
    throws IOException {
        // TODO: getOutputStream().write(generateARCFileMetaData(tsn.getNow()));
    }
        
	/**
     * Write out the WARCMetaData.
     *
     * @param date Date to put into the ARC metadata.
     * @return Byte array filled w/ the arc header.
	 * @throws IOException
     *//*
    private byte [] generateFileMetaData(String date)
    throws IOException {
        int metadataBodyLength = getMetadataLength();
        // If metadata body, then the minor part of the version is '1' rather
        // than '0'.
        String metadataHeaderLinesTwoAndThree =
            getMetadataHeaderLinesTwoAndThree("1 " +
                ((metadataBodyLength > 0)? "1": "0"));
        int recordLength = metadataBodyLength +
            metadataHeaderLinesTwoAndThree.getBytes(DEFAULT_ENCODING).length;
        String metadataHeaderStr = ARC_MAGIC_NUMBER + generateName() +
            " 0.0.0.0 " + date + " text/plain " + recordLength +
            metadataHeaderLinesTwoAndThree;
        ByteArrayOutputStream metabaos =
            new ByteArrayOutputStream(recordLength);
        // Write the metadata header.
        metabaos.write(metadataHeaderStr.getBytes(DEFAULT_ENCODING));
        // Write the metadata body, if anything to write.
        if (metadataBodyLength > 0) {
            writeMetaData(metabaos);
        }
        
        // Write out a LINE_SEPARATORs to end this record.
        metabaos.write(LINE_SEPARATOR);
        
        // Now get bytes of all just written and compress if flag set.
        byte [] bytes = metabaos.toByteArray();
        
        if(isCompressed()) {
            // GZIP the header but catch the gzipping into a byte array so we
            // can add the special IA GZIP header to the product.  After
            // manipulations, write to the output stream (The JAVA GZIP
            // implementation does not give access to GZIP header. It
            // produces a 'default' header only).  We can get away w/ these
            // maniupulations because the GZIP 'default' header doesn't
            // do the 'optional' CRC'ing of the header.
            byte [] gzippedMetaData = GzippedInputStream.gzip(bytes);
            if (gzippedMetaData[3] != 0) {
                throw new IOException("The GZIP FLG header is unexpectedly " +
                    " non-zero.  Need to add smarter code that can deal " +
                    " when already extant extra GZIP header fields.");
            }
            // Set the GZIP FLG header to '4' which says that the GZIP header
            // has extra fields.  Then insert the alex {'L', 'X', '0', '0', '0,
            // '0'} 'extra' field.  The IA GZIP header will also set byte
            // 9 (zero-based), the OS byte, to 3 (Unix).  We'll do the same.
            gzippedMetaData[3] = 4;
            gzippedMetaData[9] = 3;
            byte [] assemblyBuffer = new byte[gzippedMetaData.length +
                ARC_GZIP_EXTRA_FIELD.length];
            // '10' in the below is a pointer past the following bytes of the
            // GZIP header: ID1 ID2 CM FLG + MTIME(4-bytes) XFL OS.  See
            // RFC1952 for explaination of the abbreviations just used.
            System.arraycopy(gzippedMetaData, 0, assemblyBuffer, 0, 10);
            System.arraycopy(ARC_GZIP_EXTRA_FIELD, 0, assemblyBuffer, 10,
                ARC_GZIP_EXTRA_FIELD.length);
            System.arraycopy(gzippedMetaData, 10, assemblyBuffer,
                10 + ARC_GZIP_EXTRA_FIELD.length, gzippedMetaData.length - 10);
            bytes = assemblyBuffer;
        }
        return bytes;
    }
    */

    /**
     * Write all metadata to passed <code>baos</code>.
     *
     * @param baos Byte array to write to.
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    /*
    private void writeMetaData(ByteArrayOutputStream baos)
            throws UnsupportedEncodingException, IOException {
        if (this.metadata == null) {
            return;
        }

        for (Iterator i = this.metadata.iterator();
                i.hasNext();) {
            Object obj = i.next();
            if (obj instanceof String) {
                baos.write(((String)obj).getBytes(DEFAULT_ENCODING));
            } else if (obj instanceof File) {
                InputStream is = null;
                try {
                    is = new BufferedInputStream(
                        new FileInputStream((File)obj));
                    byte [] buffer = new byte[4096];
                    for (int read = -1; (read = is.read(buffer)) != -1;) {
                        baos.write(buffer, 0, read);
                    }
                } finally {
                    if (is != null) {
                        is.close();
                    }
                }
            } else if (obj != null) {
                logger.severe("Unsupported metadata type: " + obj);
            }
        }
        return;
    }
    */
    
    /**
     * @return Total length of metadata.
     * @throws UnsupportedEncodingException
     */
    /*
    private int getMetadataLength()
    throws UnsupportedEncodingException {
        int result = -1;
        if (this.metadata == null) {
            result = 0;
        } else {
            for (Iterator i = this.metadata.iterator();
                    i.hasNext();) {
                Object obj = i.next();
                if (obj instanceof String) {
                    result += ((String)obj).getBytes(DEFAULT_ENCODING).length;
                } else if (obj instanceof File) {
                    result += ((File)obj).length();
                } else {
                    logger.severe("Unsupported metadata type: " + obj);
                }
            }
        }
        return result;
    }
    */
}
