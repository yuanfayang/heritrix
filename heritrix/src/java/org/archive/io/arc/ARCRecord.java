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

import java.io.IOException;
import java.io.InputStream;


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
     * Constructor.
     *
     * @param in Stream cue'd up to be at the start of the record this instance
     * is to represent.
     * @param metaData Meta data.
     */
    public ARCRecord(InputStream in, ARCRecordMetaData metaData) {
        this(in, metaData, false);
    }
    
    /**
     * Constructor.
     *
     * @param in Stream cue'd up to be at the start of the record this instance
     * is to represent.
     * @param metaData Meta data.
     * @param contentRead True if all content has been read already before the
     * making of this arc record.  Update the body position so it points to end
     * of the record.  This flag is only needed creating the arcfile meta data
     * record.  It has not content.  Its all meta info.
     */
    public ARCRecord(InputStream in, ARCRecordMetaData metaData,
                boolean contentRead) {
        this.in = in;
        this.metaData = metaData;
        if (contentRead) {
            this.position = metaData.getLength();
        }
    }
    
    /* (non-Javadoc)
     * @see java.io.InputStream#markSupported()
     */
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
        int c = -1;
        if (available() > 0) {
            c = this.in.read();
            if (c == -1) {
                throw new IOException("Premature EOF before end-of-record.");
            }
            this.position++;
        }

        return c;
    }
    
    public int read(byte [] b, int offset, int length) throws IOException {
        int read = Math.min(length - offset, available());
        if (read == 0) {
            read = -1;
        } else {
            read = this.in.read(b, offset, read);
            if (read == -1) {
                throw new IOException("Premature EOF before end-of-record.");
            }
            this.position += read;
        }
        return read;
    }

    /**
     * @return True if bytes remaining in record content.
     */
    public int available() {
        return (int)(this.metaData.getLength() - this.position);
    }
    
    /* (non-Javadoc)
     * @see java.io.InputStream#skip(long)
     */
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
            // Read to the end of the body of the record.  Exhauste the stream.
            // Can't skip to end because underlying stream may be compressed.
            if (available() > 0) {
                skip(available());
            }
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
        }
    }
}
