/*  $Id: ExperimentalWARCWriter.java 4604 2006-09-06 05:38:18Z stack-sf $
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.archive.io.UTF8Bytes;
import org.archive.io.WriterPoolMember;
import org.archive.uid.GeneratorFactory;
import org.archive.util.ArchiveUtils;
import org.archive.util.anvl.ANVLRecord;



/**
 * WARC implementation.
 *
 * <p>Assumption is that the caller is managing access to this
 * WARCWriter ensuring only one thread accessing this WARC instance
 * at any one time.
 * 
 * <p>While being written, WARCs have a '.open' suffix appended.
 *
 * @author stack
 * @version $Revision: 4604 $ $Date: 2006-09-05 22:38:18 -0700 (Tue, 05 Sep 2006) $
 */
public class WARCWriter extends WriterPoolMember
implements WARCConstants {
    
    WARCRecord warcInfoRecord;
    
    /**
     * NEWLINE as bytes.
     */
    public static byte [] CRLF_BYTES;
    static {
        try {
            CRLF_BYTES = CRLF.getBytes(DEFAULT_ENCODING);
        } catch(Exception e) {
            e.printStackTrace();
        }
    };
    
    /**
     * Metadata.
     * TODO: Exploit writing warcinfo record.  Currently unused.
     */
    private final List<Object> fileMetadata;
    
    
    /**
     * Shutdown Constructor
     * Has default access so can make instance to test utility methods.
     */
    WARCWriter() {
        this(null, null, "", "", true, -1, null);
    }
    
    /**
     * Constructor.
     * Takes a stream. Use with caution. There is no upperbound check on size.
     * Will just keep writing.  Only pass Streams that are bounded. 
     * @param serialNo  used to generate unique file name sequences
     * @param out Where to write.
     * @param f File the <code>out</code> is connected to.
     * @param cmprs Compress the content written.
     * @param a14DigitDate If null, we'll write current time.
     * @throws IOException
     */
    public WARCWriter (
            final AtomicInteger serialNo,
            final OutputStream out, 
            final File f,
            final boolean cmprs, 
            final String a14DigitDate,
            final List<Object> warcinfoData)
    throws IOException {
        super(serialNo, out, f, cmprs, a14DigitDate);
        this.fileMetadata = warcinfoData;
    }
            
    /**
     * Constructor.
     *
     * @param dirs Where to drop files.
     * @param prefix File prefix to use.
     * @param cmprs Compress the records written. 
     * @param maxSize Maximum size for ARC files written.
     * @param suffix File tail to use.  If null, unused.
     * @param warcinfoData File metadata for warcinfo record.
     */
    public WARCWriter(
            final AtomicInteger serialNo,
            final List<File> dirs, 
            final String prefix, 
            final String suffix, 
            final boolean cmprs,
            final long maxSize, 
            final List<Object> warcinfoData) {
        super(serialNo, dirs, prefix, suffix, cmprs, maxSize, WARC_FILE_EXTENSION);
        this.fileMetadata = warcinfoData;
    }
    
     @Override
    protected String createFile(File file) throws IOException {
    	String filename = super.createFile(file);
    	writeWarcInfoRecord(filename,null);
        return filename;
    }
    
    protected void baseCharacterCheck(final char c, final String parameter)
    throws IOException {
        // TODO: Too strict?  UNICODE control characters?
        if (Character.isISOControl(c) || !Character.isValidCodePoint(c)) {
            throw new IOException("Contains illegal character 0x" +
                Integer.toHexString(c) + ": " + parameter);
        }
    }
    
    protected String checkHeaderValue(final String value)
    throws IOException {
        for (int i = 0; i < value.length(); i++) {
        	final char c = value.charAt(i);
        	baseCharacterCheck(c, value);
        	if (Character.isWhitespace(c)) {
                throw new IOException("Contains disallowed white space 0x" +
                    Integer.toHexString(c) + ": " + value);
        	}
        }
        return value;
    }
    
    protected String checkHeaderLineMimetypeParameter(final String parameter)
    throws IOException {
    	StringBuilder sb = new StringBuilder(parameter.length());
    	boolean wasWhitespace = false;
        for (int i = 0; i < parameter.length(); i++) {
        	char c = parameter.charAt(i);
        	if (Character.isWhitespace(c)) {
        		// Map all to ' ' and collapse multiples into one.
        		// TODO: Make sure white space occurs in legal location --
        		// before parameter or inside quoted-string.
        		if (wasWhitespace) {
        			continue;
        		}
        		wasWhitespace = true;
        		c = ' ';
        	} else {
        		wasWhitespace = false;
        		baseCharacterCheck(c, parameter);
        	}
        	sb.append(c);
        }
        
        return sb.toString();
    }

    
    /**
     * Create WARC header string from WARCRecord object 
     * @param r WARCRecord object
     * @return WARC record header String
     * @throws IOException
     */
    
    protected String createRecordHeader(WARCRecord r) 
    throws IOException {
        
        // TODO: 2048 = A SWAG: Do analysis.
    	final StringBuilder sb = new StringBuilder(2048);

    	// WARC version
    	sb.append(WARC_ID).append(CRLF);
    	
    	// WARC-Type
    	sb.append(HEADER_KEY_TYPE).append(COLON_SPACE).append(r.type).append(CRLF);

    	// WARC-Target-URI
    	// Do not write a subject-uri if not one present.
    	if (r.url != null && r.url.length() > 0) {
    	    sb.append(HEADER_KEY_URI).append(COLON_SPACE)
    	        .append(checkHeaderValue(r.url)).append(CRLF);
    	}

    	// WARC-Date
    	sb.append(HEADER_KEY_DATE).append(COLON_SPACE).append(r.date).append(CRLF);
    	if (r.namedFields != null) {
    	    for (final Iterator<?> i = r.namedFields.iterator(); i.hasNext();) {
    	        sb.append(i.next()).append(CRLF);
    	    }
    	}

    	// WARC-Record-ID
        sb.append(HEADER_KEY_ID).append(COLON_SPACE).append('<')
            .append(r.id.toString()).append('>').append(CRLF);

        // Content-type
        if (r.length > 0) {
            sb.append(CONTENT_TYPE).append(COLON_SPACE)
                .append(checkHeaderLineMimetypeParameter(r.mimeType))
                    .append(CRLF);
        }

        // Content-length
        sb.append(CONTENT_LENGTH).append(COLON_SPACE)
            .append(Long.toString(r.length)).append(CRLF);
    	
    	return sb.toString();
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

    public void writeRecord(WARCRecord record)
    throws IOException {

        // check record type
    	if (!TYPES_LIST.contains(record.type)) {
    		throw new IllegalArgumentException("Unknown record type: " 
    		        + record.type);
    	}

    	// check record length
    	if (record.length == 0 
    	        && (record.namedFields == null 
    	                || record.namedFields.size() <= 0)) {
    		throw new IllegalArgumentException("Cannot write record " +
    		    "of content-length zero and base headers only.");
    	}

        preWriteRecordTasks();
        
        try {
            final String header = createRecordHeader(record);

            // TODO: Revisit encoding of header.
            write(header.getBytes(WARC_HEADER_ENCODING));
            
            if (record.getIn() != null && record.length > 0) {
                // Write out the header/body separator.
                // TODO: should this be written even for zero-length?
                write(CRLF_BYTES); 
                copyFrom(record.getIn(), record.length, 
                        record.enforceLengthFlag);
            }
            
            // Write out the two blank lines at end of all records.
            write(CRLF_BYTES);
            write(CRLF_BYTES);
        } finally {
            postWriteRecordTasks();
        }
    }
    
    /** write WARCINFO record 
     * TODO: this method really belongs in a package which invokes WARCWriter
     * @param filename WARC-Filename
     * @param description Content-Description
     * @return URI WARC-Record-ID
     * @throws IOException
     */
    public URI writeWarcInfoRecord(String filename,final String description)
    throws IOException {
        
        // Strip .open suffix if present.
        if (filename.endsWith(WriterPoolMember.OCCUPIED_SUFFIX)) {
        	filename = filename.substring(0,
        		filename.length() - WriterPoolMember.OCCUPIED_SUFFIX.length());
        }

        ANVLRecord namedFields = new ANVLRecord(2);
        namedFields.addLabelValue(HEADER_KEY_FILENAME, filename);
        if (description != null && description.length() > 0) {
        	namedFields.addLabelValue(CONTENT_DESCRIPTION, description);
        }

        // Add warcinfo body.
        byte [] warcinfoBody = null;
        if (this.fileMetadata == null) {
            // TODO: What to write into a warcinfo?  What to associate?
            warcinfoBody = "TODO: Unimplemented".getBytes();
        } else {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            for (final Iterator<Object> i = this.fileMetadata.iterator();
                    i.hasNext();) {
                baos.write(i.next().toString().getBytes(UTF8Bytes.UTF8));
            }
            warcinfoBody = baos.toByteArray();
        }
             
        ByteArrayInputStream in = new ByteArrayInputStream(warcinfoBody);         
        URI id = generateRecordId(TYPE, WARCINFO);
        long len = warcinfoBody.length;
        String date = ArchiveUtils.getLog14Date();

        WARCRecord record = new WARCRecord(in,id,len,date,WARCINFO);
        record.setMimeType("application/warc-fields");
        record.setNamedFields(namedFields);

        // TODO: If at start of file, and we're writing compressed,
        // write out our distinctive GZIP extensions.
        
        writeRecord(record);

        return id;

    }
    

    /**
     * Write a WARCINFO record to current file.
     * TODO: Write crawl metadata or pointers to crawl description.
     * @param mimeType WARCINFO Content-Type string
     * @param namedFields WARCINFO named-fields <code>ANVLRecord</code>
     * @param fileMetadata WARCINFO metadata (RDF, ANVL, etc.) <code>InputStream</code>
     * @param fileMetadataLength Length of <code>fileMetadata</code>.
     * @throws IOException
     * @return Generated record-id made with
     * <a href="http://en.wikipedia.org/wiki/Data:_URL">data: scheme</a> and
     * the current filename.
     */
    public URI writeWarcInfoRecord(
            final String mimetype,
            final ANVLRecord namedFields, 
            final InputStream fileMetadata,
            final long fileMetadataLength)
    throws IOException {
        final String date = ArchiveUtils.getLog14Date();
        final URI recordid = generateRecordId(TYPE, WARCINFO);
        WARCRecord record = new WARCRecord(fileMetadata,recordid,
                fileMetadataLength,date,WARCINFO);
        record.setMimeType(mimetype);
        record.setNamedFields(namedFields);
        writeRecord(record);
        return recordid;
    }
    
        
    /**
     * Convenience method for getting Record-Ids.
     * @return A record ID.
     * @throws IOException
     */
    public static URI getRecordID() throws IOException {
        URI result;
        try {
            result = GeneratorFactory.getFactory().getRecordID();
        } catch (URISyntaxException e) {
            throw new IOException(e.toString());
        }
        return result;
    }
    
}