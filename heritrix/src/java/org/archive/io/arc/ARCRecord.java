/* ARCRecord
 *
 * $Id$
 *
 * Created on Jan 7, 2004
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpParser;
import org.apache.commons.httpclient.StatusLine;
import org.apache.commons.httpclient.util.EncodingUtil;
import org.archive.util.Base32;


/**
 * An ARC file record.
 *
 * @author stack
 */
public class ARCRecord extends InputStream implements ARCConstants {
    /**
     * Map of record header fields.
     *
     * We store all in a hashmap.  This way it doesn't matter whether we're
     * parsing version 1 or version 2 records.
     *
     * <p>Keys are lowercased.
     */
    private ARCRecordMetaData metaData = null;

    /**
     * Stream to read this record from.
     *
     * Stream can only be read sequentially.  Will only return this records
     * content returning a -1 if you try to read beyond the end of the current
     * record.
     *
     * <p>Streams can be markable or not.  If they are, we'll be able to roll
     * back when we've read too far.  If not markable, assumption is that
     * the underlying stream is managing our not reading too much (This pertains
     * to the skipping over the end of the ARCRecord.  See {@link #skip()}.
     */
    private InputStream in = null;

    /**
     * Position w/i the ARCRecord content, within <code>in</code>.
     *
     * This position is relative within this ARCRecord.  Its not
     * same as the arcfile position.
     */
    private long position = 0;

    /**
     * Set flag when we've reached the end-of-record.
     */
    private boolean eor = false;
    
    /**
     * Whether to read all available bytes from source stream as
     * part of this record. Only appropriate if source stream has
     * its own enveloping which delineates record with an EOF -- as with 
     * GZIPInputStream.
     */
    private boolean readAllAvailable = false;
    
    /**
     * Compute digest on what we read and add to metadata when done.
     * 
     * Currently hardcoded as sha-1. TODO: Remove when arcs record
     * digest or else, add a facility that allows the arc reader to
     * compare the calculated digest to that which is recorded in
     * the arc.
     */
    private MessageDigest digest = null;

    /**
     * Http status line object.
     * 
     * May be null if record is not http.
     */
    private StatusLine httpStatus = null;

    /**
     * Http header bytes.
     * 
     * If non-null and bytes available, give out its contents before we
     * go back to the underlying stream.
     */
    private InputStream httpHeaderStream = null;
    
    /**
     * Http headers.
     * 
     * Only populated after reading of headers.
     */
    private Header [] httpHeaders = null;

    
    /**
     * Minimal http header length.
     * 
     * I've seen in arcs content length of 1 with no 
     * header.
     */
    private static final long MIN_HTTP_HEADER_LENGTH =
        "HTTP/1.1 200 OK\r\n".length();
    
    /**
     * Offset at which the body begins.
     */
    private int bodyOffset = -1;
    

    /**
     * Constructor.
     *
     * @param in Stream cue'd up to be at the start of the record this instance
     * is to represent.
     * @param metaData Meta data.
     * @throws IOException
     */
    public ARCRecord(InputStream in, ARCRecordMetaData metaData)
    		throws IOException {
        this(in, metaData, 0);
    }

    /**
     * Constructor.
     *
     * @param in Stream cue'd up to be at the start of the record this instance
     * is to represent.
     * @param metaData Meta data.
     * @param bodyOffset Offset into the body.  Usually 0.
     * @throws IOException
     */
    public ARCRecord(InputStream in, ARCRecordMetaData metaData,
                int bodyOffset) 
    		throws IOException {
        this.in = in;
        this.metaData = metaData;
        this.position = bodyOffset;
        try {
            this.digest = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            // Convert to IOE because thats more amenable to callers
            // -- they are dealing with it anyways.
            throw new IOException(e.getMessage());
        }
        
        this.httpHeaderStream = readHttpHeader();
    }
    
    public void setReadAllAvailable(boolean flag) {
        this.readAllAvailable = flag;
    }
    
    /**
     * Skip over the the http header if one present.
     * 
     * Subsequent reads will get the body.
     * 
     * <p>Calling this method in the midst of reading the header
     * will make for strange results.  Otherwise, safe to call
     * at any time though before reading any of the arc record
     * content is only time that it makes sense.
     * 
     * <p>After calling this method, you can call
     * {@link #getHttpHeaders()} to get the read http header.
     * 
     * @throws IOException
     */
    public void skipHttpHeader() throws IOException {
        if (this.httpHeaderStream != null) {
            // Empty the httpHeaderStream
            for (int available = this.httpHeaderStream.available();
            		this.httpHeaderStream != null &&
            			(available = this.httpHeaderStream.available()) > 0;) {
                // We should be in this loop once only we should only do this
                // buffer allocation once.
                byte [] buffer = new byte[available];
                // The read nulls out httpHeaderStream when done with it so
                // need check for null in the loop control line.
                read(buffer, 0, available);
            }
        }
    }
    
    /**
     * Read http header if present.
     * Technique borrowed from HttpClient HttpParse class.
     * @return ByteArrayInputStream with the http header in it or null if no
     * http header.
     * @throws IOException
     */
    private InputStream readHttpHeader() throws IOException {
        // If judged a record that doesn't have an http header, return
        // immediately.
        if(!this.metaData.getUrl().startsWith("http") ||
            this.metaData.getLength() <= MIN_HTTP_HEADER_LENGTH) {
            return null;
        }
        byte [] statusBytes = HttpParser.readRawLine(this.in);
        int eolCharCount = getEolCharsCount(statusBytes);
        if (eolCharCount <= 0) {
            throw new IOException("Failed to read http status where one " +
                " was expected: " + new String(statusBytes));
        }
        String statusLine = EncodingUtil.getString(statusBytes, 0,
            statusBytes.length - eolCharCount, ARCConstants.DEFAULT_ENCODING);
        if ((statusLine == null) ||
                !StatusLine.startsWithHTTP(statusLine)) {
            throw new IOException("Failed parse of http status line.");
        }
        this.httpStatus = new StatusLine(statusLine);
        
        // Save off all bytes read.  Keep them as bytes rather than
        // convert to strings so we don't have to worry about encodings
        // though this should never be a problem doing http headers since
        // its all supposed to be ascii.
        ByteArrayOutputStream baos =
            new ByteArrayOutputStream(statusBytes.length + 4 * 1024);
        baos.write(statusBytes);
        
        // Now read rest of the header lines looking for the separation
        // between header and body.
        for (byte [] lineBytes = null; true;) {
            lineBytes = HttpParser.readRawLine(this.in);
            eolCharCount = getEolCharsCount(lineBytes);
            if (eolCharCount <= 0) {
                throw new IOException("Failed reading http headers: " +
                    new String(lineBytes));
            }
            // Save the bytes read.
            baos.write(lineBytes);
            if ((lineBytes.length - eolCharCount) <= 0) {
                // We've finished reading the http header.
                break;
            }
        }
        
        byte [] headerBytes = baos.toByteArray();
        // Save off where body starts.
        this.bodyOffset = headerBytes.length;
        ByteArrayInputStream bais =
            new ByteArrayInputStream(headerBytes);
        if (!bais.markSupported()) {
            throw new IOException("ByteArrayInputStream does not support mark");
        }
        bais.mark(headerBytes.length);
        // Read the status line.  Don't let it into the parseHeaders function.
        // It doesn't know what to do with it.
        bais.read(statusBytes, 0, statusBytes.length);
        this.httpHeaders = HttpParser.parseHeaders(bais,
            ARCConstants.DEFAULT_ENCODING);
        bais.reset();
        return bais;
    }
    
    /**
     * @return Offset at which the body begins (Only known after
     * header has been read) or -1 if none or we haven't read
     * headers yet.
     */
    public int getBodyOffset() {
        return this.bodyOffset;
    }
    
    /**
     * Return status code for this record.
     * 
     * This method will return -1 until the http header has been read.
     * @return Status code.
     */
    public int getStatusCode() {
        return (this.httpStatus == null)? -1: this.httpStatus.getStatusCode();
    }
    
    /**
     * @param bytes Array of bytes to examine for an EOL.
     * @return Count of end-of-line characters or zero if none.
     */
    private int getEolCharsCount(byte [] bytes) {
        int count = 0;
        if (bytes != null && bytes.length >=1 &&
                bytes[bytes.length - 1] == '\n') {
            count++;
            if (bytes.length >=2 && bytes[bytes.length -2] == '\r') {
                count++;
            }
        }
        return count;
    }
    
    public boolean markSupported() {
        return false;
    }

    /**
     * @return Meta data for this record.
     */
    public ARCRecordMetaData getMetaData() {
        return this.metaData;
    }
    
    /**
     * @return http headers (Only available after header has been read).
     */
    public Header [] getHttpHeaders() {
        return this.httpHeaders;
    }

    /**
     * Calling close on a record skips us past this record to the next record
     * in the stream.
     *
     * It does not actually close the stream.  The underlying steam is probably
     * being used by the next arc record.
     *
     * @throws IOException
     */
    public void close() throws IOException {
        if (this.in != null) {
            skip();
            this.in = null;
        }
    }

    /**
     * @return Next character in this ARCRecord's content else -1 if at end of
     * this record.
     * @throws IOException
     */
    public int read() throws IOException {
        int c = -1;
        if (this.httpHeaderStream != null &&
                (this.httpHeaderStream.available() > 0)) {
            // If http header, return bytes from it before we go to underlying
            // stream.
            c = this.httpHeaderStream.read();
            // If done with the header stream, null it out.
            if (this.httpHeaderStream.available() <= 0) {
                this.httpHeaderStream = null;
            }
        } else {
            if (available() > 0) {
                c = this.in.read();
                if (c == -1) {
                    throw new IOException("Premature EOF before end-of-record.");
                }
                this.digest.update((byte)c);
            }
        }
        this.position++;
        return c;
    }

    public int read(byte [] b, int offset, int length) throws IOException {
        int read = -1;
        if (this.httpHeaderStream != null &&
                (this.httpHeaderStream.available() > 0)) {
            // If http header, return bytes from it before we go to underlying
            // stream.
            read = Math.min(length - offset,
                this.httpHeaderStream.available());
            if (read == 0) {
                read = -1;
            } else {
                read = this.httpHeaderStream.read(b, offset, read);
            }
            // If done with the header stream, null it out.
            if (this.httpHeaderStream.available() <= 0) {
                this.httpHeaderStream = null;
            }
        } else {
            read = Math.min(length - offset, available());
            if (read == 0) {
                read = -1;
            } else {
                read = this.in.read(b, offset, read);
                if (read == -1) {
                    throw new IOException("Premature EOF before end-of-record.");
                }
                this.digest.update(b, offset, read);
            }
        }
        this.position += read;
        return read;
    }

    /**
     * This available is not the stream's available.  Its an available
     * based on what the stated ARC record length is minus what we've
     * read to date.
     * 
     * @return True if bytes remaining in record content.
     */
    public int available() {
        return (int)(this.metaData.getLength() - this.position);
    }

    public long skip(long n) throws IOException {
        final int SKIP_BUFFERSIZE = 1024 * 4;
        byte [] b = new byte[SKIP_BUFFERSIZE];
        long total = 0;
        for (int read = 0; (total < n) && (read != -1);) {
            read = Math.min(SKIP_BUFFERSIZE, (int)(n - total));
            // TODO: Interesting is that reading from compressed stream, we only
            // read about 500 characters at a time though we ask for 4k.
            // Look at this sometime.
            read = read(b, 0, read);
            if (read <= 0) {
                read = -1;
            } else {
                total += read;
            }
        }
        return total;
    }

    /**
     * Skip over this records content.
     *
     * @throws IOException
     */
    private void skip() throws IOException {
        if (!this.eor) {
            // Read to the end of the body of the record.  Exhaust the stream.
            // Can't skip to end because underlying stream may be compressed
            // and we're calculating the digest for the record.
            if (available() > 0) {
                skip(available());
            }
            // The available here is different from the above available.
            // The one here is the stream's available.  We're looking to see
            // if anything in stream after the arc content.... and we're
            // trying to move past it.  Important is that we not count
            // bytes read below here as part of the arc content.
            if (this.in.available() > 0) {
                int c = -1;
                c = this.in.read();
                if(c != LINE_SEPARATOR) {
                    System.err.println("ERROR("+metaData.getDate()
                            +" "+metaData.getUrl()
                            +" "+metaData.getArcFile()+"):"
                            +" expected newline, received char: "+c);
                }
                if(this.readAllAvailable) {
                    int excess = 0;
                    while(this.in.read()!=-1) {
                        excess++;
                    }
                    if(excess > 0) {
                        System.err.println("ERROR("+metaData.getDate()
                                +" "+metaData.getUrl()
                                +" "+metaData.getArcFile()+"):"
                                +" excess record material:"+excess);
                    }
                }
            } else {
                System.err.println("ERROR("+metaData.getDate()
                        +" "+metaData.getUrl()
                        +" "+metaData.getArcFile()+"):"
                        +" expected record-terminator-newline missing");
            }
//                // If there's still stuff on the line, its the LINE_SEPARATOR
//                // that lies between records.  Lets read it so we're cue'd up
//                // aligned ready to read the next record.
//                //
//                // But there is a problem.  If the file is compressed, there
//                // will only be LINE_SEPARATOR's in the stream -- we need to
//                // move on to the next GZIP member in the stream before we can
//                // get more characters.  But if the file is uncompressed, then
//                // we need to NOT read characters from the next record in the
//                // stream.
//                //
//                // If the stream supports mark, then its not the GZIP stream.
//                // Use the mark to go back if we read anything but
//                // LINE_SEPARATOR characters.
//                int c = -1;
//                while (this.in.available() > 0) {
//                    if (this.in.markSupported()) {
//                        this.in.mark(1);
//                    }
//                    c = this.in.read();
//                    if (c != -1) {
//                        if (c == LINE_SEPARATOR) {
//                            continue;
//                        }
//                        if (this.in.markSupported()) {
//                            // We've overread.  We're in next record. Backup
//                            // break.
//                            this.in.reset();
//                            break;
//                        }
//                        throw new IOException("Read " + (char)c +
//                            " when only" + LINE_SEPARATOR + " expected.");
//                    }
//                }
//            }

            this.eor = true;
            // Set the metadata digest as base32 string.
            this.metaData.
            	setDigest(Base32.encode(this.digest.digest()));
            if (this.httpStatus != null) {
                int statusCode = this.httpStatus.getStatusCode();
                this.metaData.setStatusCode(Integer.toString(statusCode));
            }
        }
    }
}
