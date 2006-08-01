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
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.archive.io.WriterPoolMember;
import org.archive.io.warc.recordid.GeneratorFactory;
import org.archive.util.ArchiveUtils;
import org.archive.util.anvl.ANVLRecord;


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
    
    /**
     * NEWLINE as bytes.
     */
    public static byte [] NEWLINE_BYTES;
    static {
        try {
        	NEWLINE_BYTES = NEWLINE.getBytes(DEFAULT_ENCODING);
        } catch(Exception e) {
            e.printStackTrace();
        }
    };
    
    /**
     * Formatter for the length.
     */
    private static NumberFormat RECORD_LENGTH_FORMATTER =
        new DecimalFormat(PLACEHOLDER_RECORD_LENGTH_STRING);
    
    /**
     * Default URL scheme prefix for WARC file warcinfo records.
     */
    private static final String RFC2397_PREFIX =
    	"data:text/plain;charset=utf-8,";
    
    /**
     * Shutdown Constructor
     * Has default access so can make instance to test utility methods.
     */
    ExperimentalWARCWriter() {
        this(null, "", "", true, -1);
    }
    
    /**
     * Constructor.
     * Takes a stream. Use with caution. There is no upperbound check on size.
     * Will just keep writing.  Only pass Streams that are bounded. Has
     * default access only because usually only used by {@link WARCReader}
     * dumping output.
     * @param out Where to write.
     * @param f File the <code>out</code> is connected to.
     * @param cmprs Compress the content written.
     * @param a14DigitDate If null, we'll write current time.
     * @throws IOException
     */
    ExperimentalWARCWriter(final PrintStream out, final File f,
    		final boolean cmprs, final String a14DigitDate)
    throws IOException {
        super(out, f, cmprs, a14DigitDate);
    }
            
    /**
     * Constructor.
     *
     * @param dirs Where to drop files.
     * @param prefix File prefix to use.
     * @param cmprs Compress the records written. 
     * @param maxSize Maximum size for ARC files written.
     * @param suffix File tail to use.  If null, unused.
     */
    public ExperimentalWARCWriter(final List dirs, final String prefix, 
            final String suffix, final boolean cmprs,
            final int maxSize) {
        super(dirs, prefix, suffix, cmprs, maxSize, WARC_FILE_EXTENSION);
        // TODO: Should there be a constructor that takes file metadata and
        // writes a warcinfo record automatically on construction?
    }

    protected String createFile()
    throws IOException {
    	// TODO: Do I need to automatically write a warcinfo record here, just
    	// after call to super.createFile()?  Yes.  But what to write in the
    	// second warcinfos?  Pointer at first?
    	// TODO: If at start of file, and we're writing compressed,
    	// write out our distinctive GZIP extensions.
        return super.createFile();
    }
    
    protected String checkHeaderLineValue(final String value)
    throws IOException {
        // TODO: A version that will collapse space and tab for mimetypes.
        // TODO: Below check may be too strict?
        for (int i = 0; i < value.length(); i++) {
        	char c = value.charAt(i);
            if (Character.isISOControl(c) || Character.isWhitespace(c) ||
            		!Character.isValidCodePoint(c)) {
                throw new IOException("Contains illegal character 0x" +
                    Integer.toHexString(c) + ": " + value);
            }
        }
        return value;
    }
    
    protected byte [] createRecordHeaderline(final String type,
    		final String url, final String create14DigitDate,
    		final String mimetype, final URI recordId,
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
    	sb.append(checkHeaderLineValue(create14DigitDate));
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

        return sb.toString().getBytes(HEADER_LINE_ENCODING);
    }

    protected void writeRecord(final String type, final String url,
    		final String create14DigitDate, final String mimetype,
    		final URI recordId, ANVLRecord namedFields,
            final InputStream contentStream, final long contentLength)
    throws IOException {
    	if (!TYPES_LIST.contains(type)) {
    		throw new IllegalArgumentException("Unknown record type: " + type);
    	}
    	if (contentLength == 0 &&
                (namedFields == null || namedFields.size() <= 0)) {
    		throw new IllegalArgumentException("Cannot have a record made " +
    		    "of a Header line only (Content and Named Fields are empty).");
    	}
    	
        preWriteRecordTasks();
        try {
        	if (namedFields == null) {
        		// Use the empty anvl record so the length of blank line on
        		// end gets counted as part of the record length.
        		namedFields = ANVLRecord.EMPTY_ANVL_RECORD;
        	}
        	
        	// Serialize metadata first so we have metadata length.
        	final byte [] namedFieldsBlock = namedFields.getUTF8Bytes();
        	// Now serialize the Header line.
            final byte [] header = createRecordHeaderline(type, url,
            	create14DigitDate, mimetype, recordId, namedFieldsBlock.length,
            	contentLength);
            write(header);
            write(NEWLINE_BYTES);
            write(namedFieldsBlock);
            if (contentStream != null && contentLength > 0) {
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
    
    /**
     * @return A RFC2397 URL made of the current filename.
     * @throws IOException 
     */
    protected String generateWarcinfoRecordURL()
    throws IOException {
    	if (getFile() == null) {
    		// Then, a current file hasn't been created yet.  Call createFile.
    		// TODO: If createFile automatically makes a warcinfo record,
    		// remove this call to createFile.
    		createFile();
    	}
    	return RFC2397_PREFIX + getBaseFilename();
    }
    
    protected URI generateRecordId(final Map<String, String> qualifiers)
    throws IOException {
    	URI rid = null;
    	try {
    		rid = GeneratorFactory.getFactory().
    			getQualifiedRecordID(qualifiers);
    	} catch (URISyntaxException e) {
    		// Convert to IOE so can let it out.
    		throw new IOException(e.getMessage());
    	}
    	return rid;
    }
    
    protected URI generateRecordId(final String key, final String value)
    throws IOException {
    	URI rid = null;
    	try {
    		rid = GeneratorFactory.getFactory().
    			getQualifiedRecordID(key, value);
    	} catch (URISyntaxException e) {
    		// Convert to IOE so can let it out.
    		throw new IOException(e.getMessage());
    	}
    	return rid;
    }
    
    /**
     * Write a warcinfo to current file.
     * @param mimetype Mimetype of the <code>fileMetadata</code> block.
     * @param namedFields Named fields. Pass <code>null</code> if none.
     * @param fileMetadata Metadata about this WARC as RDF, ANVL, etc.
     * @param fileMetadataLength Length of <code>fileMetadata</code>.
     * @throws IOException
     * @return Generated record-id made with
     * <a href="http://en.wikipedia.org/wiki/Data:_URL">data: scheme</a> and
     * the current filename.
     */
    public URI writeWarcinfoRecord(final String mimetype,
    	final ANVLRecord namedFields, final InputStream fileMetadata,
    	final long fileMetadataLength)
    throws IOException {
    	final URI recordid = generateRecordId(TYPE, WARCINFO);
    	writeWarcinfoRecord(generateWarcinfoRecordURL(),
    		ArchiveUtils.get14DigitDate(), mimetype, recordid, namedFields,
    		fileMetadata, fileMetadataLength);
    	return recordid;
    }
    
    /**
     * Write a warcinfo to current file.
     * @param url URL to use for this warcinfo.
     * @param create14DigitDate Record creation date as 14 digit date.
     * @param mimetype Mimetype of the <code>fileMetadata</code>.
     * @param namedFields Named fields.
     * @param fileMetadata Metadata about this WARC as RDF, ANVL, etc.
     * @param fileMetadataLength Length of <code>fileMetadata</code>.
     * @throws IOException
     */
    public void writeWarcinfoRecord(final String url,
    	final String create14DigitDate, final String mimetype,
    	final URI recordId, final ANVLRecord namedFields,
    	final InputStream fileMetadata, final long fileMetadataLength)
    throws IOException {
    	writeRecord(WARCINFO, url, create14DigitDate, mimetype,
        		recordId, namedFields, fileMetadata, fileMetadataLength);
    }
}
