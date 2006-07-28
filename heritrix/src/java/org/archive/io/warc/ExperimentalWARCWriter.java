/*  $Id$
 *
 * Created on July 27th, 2006
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
package org.archive.io.warc;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URI;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.archive.io.WriterPoolMember;
import org.archive.util.ArchiveUtils;
import org.archive.util.TimestampSerialno;


/**
 * <b>Experimental</b> WARC implementation.
 * 
 * Based on unreleased version of <a 
 * href="http://archive-access.sourceforge.net//warc/warc_file_format.html">WARC
 * File Format</a> document.  Specification and implementation subject to
 * change.
 *
 * <p>Assumption is that the caller is managing access to this
 * ExperimentalWARCWriter ensuring only one thread accessing this WARC instance
 * at any one time.
 * 
 * <p>While being written, WARCs have a '.open' suffix appended.
 *
 * @author stack
 * @version $Revision$ $Date$
 */
public class ExperimentalWARCWriter
extends WriterPoolMember implements WARCConstants {
    private final Logger logger = Logger.getLogger(this.getClass().getName());
    
    /**
     * Buffer to reuse writing streams.
     */
    private final byte [] readbuffer = new byte[16 * 1024];
    
    public static byte [] NEWLINE_BYTES;
    static {
        try {
        	NEWLINE_BYTES = NEWLINE.getBytes(DEFAULT_ENCODING);
        } catch(Exception e) {
            e.printStackTrace();
        }
    };
    
    private static NumberFormat RECORD_LENGTH_FORMATTER =
        new DecimalFormat(PLACEHOLDER_RECORD_LENGTH_STRING);
    
    /**
     * Shutdown Constructor
     * For unit testing utility methods.
     */
    ExperimentalWARCWriter() {
        this(null, "", true, -1);
    }
    
    /**
     * Constructor.
     * Takes a stream. Use with caution. There is no upperbound check on size.
     * Will just keep writing.
     * @param out Where to write.
     * @param f File the <code>out</code> is connected to.
     * @param cmprs Compress the content written.
     * @param metadata File meta data.  Can be null.  Is list of File and/or
     * String objects.
     * @param a14DigitDate If null, we'll write current time.
     * @throws IOException
     */
    ExperimentalWARCWriter(final PrintStream out, final File f,
    		final boolean cmprs, final String a14DigitDate, final Map metadata)
    throws IOException {
        super(out, f, cmprs, a14DigitDate);
        // TODO: If passed file metadata, write it out.
    }
    
    /**
     * Constructor.
     *
     * @param dirs Where to drop files.
     * @param prefix File prefix to use.
     * @param cmprs Compress the records written. 
     * @param maxSize Maximum size for ARC files written.
     */
    public ExperimentalWARCWriter(final List dirs, final String prefix, 
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
    public ExperimentalWARCWriter(final List dirs, final String prefix, 
            final String suffix, final boolean cmprs,
            final int maxSize, final Map meta) {
        super(dirs, prefix, suffix, cmprs, maxSize, WARC_FILE_EXTENSION);
        // TODO: this.metadata = meta;
    }

    /**
     * Create an WARC file.
     * @return Instance of datastructure with serial number and timestamp used
     * making this file.
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
        // TODO: A version that will collapse space and tab for mimetypes.
        // TODO: Below check may be too strict?
        for (int i = 0; i < value.length(); i++) {
        	char c = value.charAt(i);
            if (Character.isISOControl(c) || Character.isWhitespace(c)) {
                throw new IOException("Contains illegal character 0x" +
                    Integer.toHexString(c) + ": " + value);
            }
        }
        return value;
    }
    
    protected String getRecordId() {
        // TODO: Needs to be pluggable.  Factory.
        return "unique-id-todo";
    }
    
    protected byte [] serializeNamedFields(final Map namedFields) {
    	return new byte [0];
    }
    
    protected byte [] createRecordHeaderline(final String type,
    		final String url, final String mimetype, final URI recordId,
    		final int namedFieldsLength, final long contentLength)
    throws IOException {
    	final StringBuilder sb =
    		new StringBuilder(2048/*A SWAG: TODO: Do analysis.*/);
    	sb.append(WARC_ID);
    	sb.append(HEADER_FIELD_SEPARATOR);
    	sb.append(PLACEHOLDER_RECORD_LENGTH_STRING);
    	sb.append(HEADER_FIELD_SEPARATOR);
    	sb.append(type);
    	sb.append(HEADER_FIELD_SEPARATOR);
    	sb.append(checkHeaderLineValue(url));
    	sb.append(HEADER_FIELD_SEPARATOR);
    	sb.append(ArchiveUtils.get14DigitDate());
    	sb.append(HEADER_FIELD_SEPARATOR);
    	sb.append(checkHeaderLineValue(mimetype));
    	sb.append(HEADER_FIELD_SEPARATOR);
    	sb.append(checkHeaderLineValue(recordId.toString()));
    	
    	long length = sb.length() + namedFieldsLength + contentLength;
    	
    	// Insert length and pad out to fixed width with zero prefix to
        // highlight 'fixed-widthness' of length.
    	int start = WARC_ID.length() + 1 /*HEADER_FIELD_SEPARATOR */;
        int end = start + PLACEHOLDER_RECORD_LENGTH_STRING.length();
    	String lenStr = RECORD_LENGTH_FORMATTER.format(length);
    	sb.replace(start, end, lenStr);
    	
    	// TODO: Ensure all characters within a particular charset.
        return sb.toString().getBytes(HEADER_LINE_ENCODING);
    }
    
    protected void writeRecord(final String type, final String url,
            final String mimetype, final URI recordId, final Map namedFields,
            final InputStream contentStream, final long contentLength)
    throws IOException {
    	if (!TYPES_LIST.contains(type)) {
    		throw new IllegalArgumentException("Unknown record type: " + type);
    	}
    	if (contentLength == 0 &&
                (namedFields == null || namedFields.size() <= 0)) {
    		throw new IllegalArgumentException("Cannot have a record made " +
    		    "of a Header line only");
    	}
    	
        preWriteRecordTasks();
        try {
        	// Serialize metadata first so we have metadata length.
        	final byte [] namedFieldsBlock = serializeNamedFields(namedFields);
        	// Now serialize the Header line.
            final byte [] header = createRecordHeaderline(type, url,
            	mimetype, recordId, namedFieldsBlock.length, contentLength);
            write(header);
            if (namedFieldsBlock != null && namedFieldsBlock.length > 0) {
            	write(NEWLINE_BYTES);
            	write(namedFieldsBlock);
            }
            if (contentStream != null && contentLength > 0) {
            	write(NEWLINE_BYTES);
            	readFullyFrom(contentStream, contentLength, this.readbuffer);
            }
            
            // Write out the two blank lines at end of all records.
            // TODO: Why? Messes up skipping through file. Also not in grammar.
            write(NEWLINE_BYTES);
            write(NEWLINE_BYTES);
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
