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

import org.apache.commons.httpclient.HttpConstants;
import org.apache.commons.httpclient.HttpParser;
import org.apache.commons.httpclient.StatusLine;
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
    private InputStream httpHeaderStream = null;;

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
    
    /**
     * Read http header if present.
     * @return ByteArrayInputStream with the http header in it or null if no
     * http header.
     * @throws IOException
     */
    private InputStream readHttpHeader() throws IOException {
        // If judged a record that doesn't have an http header, return
        // immediately.
        if(!metaData.getUrl().startsWith("http")) {
            return null;
        }
        byte [] statusBytes = HttpParser.readRawLine(this.in);
        if (statusBytes == null || statusBytes.length <= 0 ||
                !isCrNl(statusBytes)) {
            throw new IOException("Failed to read http status where one " +
                " was expected.");
        }
        String statusLine = HttpConstants.getString(statusBytes, 0,
                statusBytes.length - 2 /*crnl*/);
        if ((statusLine == null) ||
                !StatusLine.startsWithHTTP(statusLine)) {
            throw new IOException("Failed parse of http status line.");
        }
        this.httpStatus = new StatusLine(statusLine);
        
        // Save off the bytes read.
        ByteArrayOutputStream baos = new ByteArrayOutputStream(4 * 1024);
        baos.write(statusBytes);
        for (byte [] lineBytes = null; true;) {
            lineBytes = HttpParser.readRawLine(this.in);
            if (lineBytes == null || lineBytes.length <= 0 ||
                    !isCrNl(lineBytes)) {
                throw new IOException("Failed reading http headers.");
            }
            String line = HttpConstants.getString(lineBytes, 0, lineBytes.length - 2);
            // Save the bytes read.
            baos.write(lineBytes);
            if ((lineBytes.length - 2) <= 0) {
                // We've finished reading the http header.
                break;
            }
        }
        
        return new ByteArrayInputStream(baos.toByteArray());
    }
    
    /**
     * @return True if line has '\r\n' on the end.
     */
    private boolean isCrNl (byte [] bytes) {
        return (bytes != null &&
            bytes.length >= 2 &&
            bytes[bytes.length - 1] == '\n' &&
            bytes[bytes.length -2] == '\r');
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
        // If http header, return bytes from it before we go to underlying
        // stream.
        if (this.httpHeaderStream != null &&
                this.httpHeaderStream.available() > 0) {
            return this.httpHeaderStream.read();
        }
        
        int c = -1;
        if (available() > 0) {
            c = this.in.read();
            if (c == -1) {
                throw new IOException("Premature EOF before end-of-record.");
            }
            this.digest.update((byte)c);
            this.position++;
        }

        return c;
    }

    public int read(byte [] b, int offset, int length) throws IOException {
        // If http header, return bytes from it before we go to underlying
        // stream.
        if (this.httpHeaderStream != null &&
                this.httpHeaderStream.available() > 0) {
            int read = Math.min(length - offset, available());
            return this.httpHeaderStream.read(b, offset, read);
        }
        
        int read = Math.min(length - offset, available());
        if (read == 0) {
            read = -1;
        } else {
            read = this.in.read(b, offset, read);
            if (read == -1) {
                throw new IOException("Premature EOF before end-of-record.");
            }
            this.digest.update(b, offset, read);
            this.position += read;
        }
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
                // If there's still stuff on the line, its the LINE_SEPARATOR
                // that lies between records.  Lets read it so we're cue'd up
                // aligned ready to read the next record.
                //
                // But there is a problem.  If the file is compressed, there
                // will only be LINE_SEPARATOR's in the stream -- we need to
                // move on to the next GZIP member in the stream before we can
                // get more characters.  But if the file is uncompressed, then
                // we need to NOT read characters from the next record in the
                // stream.
                //
                // If the stream supports mark, then its not the GZIP stream.
                // Use the mark to go back if we read anything but
                // LINE_SEPARATOR characters.
                int c = -1;
                while (this.in.available() > 0) {
                    if (this.in.markSupported()) {
                        this.in.mark(1);
                    }
                    c = this.in.read();
                    if (c != -1) {
                        if (c == LINE_SEPARATOR) {
                            continue;
                        }
                        if (this.in.markSupported()) {
                            // We've overread.  We're in next record. Backup
                            // break.
                            this.in.reset();
                            break;
                        } else {
                            throw new IOException("Read " + (char)c +
                                " when only" + LINE_SEPARATOR + " expected.");
                        }
                    }
                }
            }

            this.eor = true;
            // Set the metadata digest as base32 string.
            this.metaData.
            	setDigest(Base32.encode((byte[])this.digest.digest()));
            if (this.httpStatus != null) {
                int statusCode = this.httpStatus.getStatusCode();
                this.metaData.setStatusCode(Integer.toString(statusCode));
            }
        }
    }
}
