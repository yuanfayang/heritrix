/* ReplayCharSequenceFactory
 *
 * Created on Mar 8, 2004
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
package org.archive.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.util.DevUtils;


/**
 * Factory that returns a ReplayCharSequence view on to a recording stream.
 *
 * This factory encapsulates the decision-making figuring which
 * ReplayCharSequence to return whether the single byte or multibyte handling
 * ReplayCharSequence implementations.  Get instance of this factory
 * using {@link #getInstance()} and then call
 * {@link #getReplayCharSequence(byte [], long, long, String, String)}.
 *
 * @author stack
 * @version $Revision$, $Date$
 */
public class ReplayCharSequenceFactory {

    /**
     * Logger.
     *
     * Logger used by this factory and by the ReplayCharSequence's returned.
     */
    protected static Logger logger =
        Logger.getLogger("org.archive.io.ReplayCharSequenceFactory");

    /**
     * Singleton instance of this factory.
     */
    private static final ReplayCharSequenceFactory factory =
        new ReplayCharSequenceFactory();


    /**
     * Private constructor.
     *
     * Private ensures only one singleton instance.
     */
    private ReplayCharSequenceFactory() {
        super();
    }

    /**
     * @return Instance of the singleton ReplayCharSequenceFactory.
     */
    public static ReplayCharSequenceFactory getInstance() {
        return ReplayCharSequenceFactory.factory;
    }

    /**
     * Return appropriate ReplayCharSequence switching off passed encoding.
     *
     * We look at the encoding and try to figure whether to pass back a
     * byte-orientated ReplayCharSequence or a character-orientated
     * ReplayCharStream.
     *
     * @param buffer In-memory buffer of recordings prefix.  We read from here
     * first and will only go to the backing file if <code>size</code> requested
     * is greater than <code>buffer.length</code>.
     * @param size Total size of stream to replay in bytes.  Used to find EOS.
     * This is total length of content including HTTP headers if present.
     * @param responseBodyStart Where the response body starts in bytes. Used to
     * skip over the HTTP headers if present.
     * @param backingFilename Full path to backing file with content in excess
     * of whats in <code>buffer</code>.
     * @param encoding Encoding to use reading the passed prefix buffer and
     * backing file.  For now, should be java canonical name for the encoding.
     * (If null is passed, we will default to ByteReplayCharSequence).
     *
     * @return A ReplayCharSequence.
     *
     * @throws IOException Problems accessing backing file or writing new file
     * of the decoded content.
     */
    public ReplayCharSequence getReplayCharSequence(byte[] buffer, long size,
                long responseBodyStart, String backingFilename, String encoding)
        throws IOException {

        checkParameters(buffer, size, responseBodyStart);
        ReplayCharSequence rcs = null;
        if (isMultibyteEncoding(encoding)) {
            rcs = new MultiByteReplayCharSequence(buffer, size,
                responseBodyStart, backingFilename, encoding);
        } else {
            rcs = new ByteReplayCharSequence(buffer, size, responseBodyStart,
                backingFilename);
        }

        return rcs;
    }
    
    /**
     * Make decision as to whether encoding warrants single-byte replay char
     * sequence or multi-byte.
     *
     * @param encoding Encoding to use reading the passed prefix buffer and
     * backing file.  For now, should be java canonical name for the encoding.
     * (If null is passed, we will default to ByteReplayCharSequence).
     *
     * @return True if multibyte encoding.
     */
    private boolean isMultibyteEncoding(String encoding) {
        boolean isMultibyte = false;
        final Charset cs;
        try {
            if (encoding != null && encoding.length() > 0) {
                cs = Charset.forName(encoding);
                if(cs.canEncode()) {
                    isMultibyte = cs.newEncoder().maxBytesPerChar() > 1;
                } else {
                    isMultibyte = false;
                    logger.info("Encoding not fully supported: " + encoding
                            + ".  Defaulting to single byte.");
                }
            }
        } catch (IllegalCharsetNameException e) {
            // Unsupported encoding.  Default to singlebyte.
            isMultibyte = false;

            logger.info("Illegal encoding name: " + encoding
                + ".  Defaulting to single byte.");
        } catch (UnsupportedCharsetException e) {
            // Unsupported encoding.  Default to singlebyte.
            isMultibyte = false;

            logger.info("Unsupported encoding " + encoding
                + ".  Defaulting to single byte.");
        }
        logger.fine("Encoding " + encoding + " is multibyte: "
            + ((isMultibyte) ? Boolean.TRUE : Boolean.FALSE));
        
        return isMultibyte;
    }

    /**
     * Test passed arguments.
     *
     * @param buffer In-memory buffer of recordings prefix.  We read from here
     * first and will only go to the backing file if <code>size</code> requested
     * is greater than <code>buffer.length</code>.
     * @param size Total size of stream to replay in bytes.  Used to find EOS.
     * This is total length of content including HTTP headers if present.
     * @param responseBodyStart Where the response body starts in bytes. Used to
     * skip over the HTTP headers if present.
     *
     * @throws IllegalArgumentException Thrown if passed an illegal argument.
     */
    protected void checkParameters(byte[] buffer, long size,
            long responseBodyStart)
        throws IllegalArgumentException {

        if (responseBodyStart > size) {
            throw new IllegalArgumentException("Illegal response body offset" +
                " of " + responseBodyStart + " whereas size is only " + size);
        }

        if (responseBodyStart > Integer.MAX_VALUE) {
            // A value of this size will mess up math below.
            throw new IllegalArgumentException("Response body start " +
                " of " + responseBodyStart + " > Integer.MAX_VALUE.");
        }

        if (responseBodyStart > buffer.length) {
            logger.log(Level.WARNING,
                "Unexpected response body offset " + responseBodyStart + ",\n" +
                "beyond the first buffer of length "+buffer.length+".\n" +
                "Thread: "+ Thread.currentThread().getName() + "\n");
        }

        if ((size - responseBodyStart) > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Length is bigger than we  can" +
               " handle: " + (size - responseBodyStart));
        }
    }

    /**
     * Provides a (Replay)CharSequence view on recorded stream bytes (a prefix
     * buffer and overflow backing file).
     *
     * Treats the byte stream as 8-bit.
     *
     * <p>Uses a wraparound rolling buffer of the last windowSize bytes read
     * from disk in memory; as long as the 'random access' of a CharSequence
     * user stays within this window, access should remain fairly efficient.
     * (So design any regexps pointed at these CharSequences to work within
     * that range!)
     *
     * <p>When rereading of a location is necessary, the whole window is
     * recentered around the location requested. (TODO: More research
     * into whether this is the best strategy.)
     *
     * <p>An implementation of a ReplayCharSequence done with ByteBuffers -- one
     * to wrap the passed prefix buffer and the second, a memory-mapped
     * ByteBuffer view into the backing file -- was consistently slower: ~10%.
     * My tests did the following. Made a buffer filled w/ regular content.
     * This buffer was used as the prefix buffer.  The buffer content was
     * written MULTIPLER times to a backing file.  I then did accesses w/ the
     * following pattern: Skip forward 32 bytes, then back 16 bytes, and then
     * read forward from byte 16-32.  Repeat.  Though I varied the size of the
     * buffer to the size of the backing file,from 3-10, the difference of 10%
     * or so seemed to persist.  Same if I tried to favor get() over get(index).
     * I used a profiler, JMP, to study times taken (St.Ack did above comment).
     *
     * <p>TODO determine in memory mapped files is better way to do this;
     * probably not -- they don't offer the level of control over
     * total memory used that this approach does.
     *
     * @author Gordon Mohr
     * @version $Revision$, $Date$
     */
    private class ByteReplayCharSequence implements ReplayCharSequence {

        /**
         * Buffer that holds the first bit of content.
         *
         * Once this is exhausted we go to the backing file.
         */
        private byte[] prefixBuffer;

        /**
         * Total length of character stream to replay minus the HTTP headers
         * if present.
         *
         * Used to find EOS.
         */
        protected int length;

        /**
         * Absolute length of the stream.
         *
         * Includes HTTP headers.  Needed doing calc. in the below figuring
         * how much to load into buffer.
         */
        private int absoluteLength = -1;

        /**
         * Buffer window on to backing file.
         */
        private byte[] wraparoundBuffer;

        /**
         * Absolute index into underlying bytestream where wrap starts.
         */
        private int wrapOrigin;

        /**
         * Index in wraparoundBuffer that corresponds to wrapOrigin
         */
        private int wrapOffset;

        /**
         * Name of backing file we go to when we've exhausted content from the
         * prefix buffer.
         */
        private String backingFilename;

        /**
         * Random access to the backing file.
         */
        private RandomAccessFile raFile;

        /**
         * Offset into prefix buffer at which content beings.
         */
        private int contentOffset;

        /**
         * 8-bit encoding used reading single bytes from buffer and
         * stream.
         */
        private static final String DEFAULT_SINGLE_BYTE_ENCODING =
            "ISO-8859-1";


        /**
         * Constructor.
         *
         * @param buffer In-memory buffer of recordings prefix.  We read from
         * here first and will only go to the backing file if <code>size</code>
         * requested is greater than <code>buffer.length</code>.
         * @param size Total size of stream to replay in bytes.  Used to find
         * EOS. This is total length of content including HTTP headers if
         * present.
         * @param responseBodyStart Where the response body starts in bytes.
         * Used to skip over the HTTP headers if present.
         * @param backingFilename Path to backing file with content in excess of
         * whats in <code>buffer</code>.
         *
         * @throws IOException
         */
        private ByteReplayCharSequence(byte[] buffer, long size,
                long responseBodyStart, String backingFilename)
            throws IOException {

            this.length = (int)(size - responseBodyStart);
            this.absoluteLength = (int)size;
            this.prefixBuffer = buffer;
            this.contentOffset = (int)responseBodyStart;

            // If amount to read is > than what is in our prefix buffer, then
            // open the backing file.
            if (size > buffer.length) {
                this.backingFilename = backingFilename;
                this.raFile = new RandomAccessFile(backingFilename, "r");
                this.wraparoundBuffer = new byte[this.prefixBuffer.length];
                this.wrapOrigin = this.prefixBuffer.length;
                this.wrapOffset = 0;
                loadBuffer();
            }
        }

        /**
         * @return Length of characters in stream to replay.  Starts counting
         * at the HTTP header/body boundary.
         */
        public int length() {
            return this.length;
        }

        /**
         * Get character at passed absolute position.
         *
         * Called by {@link #charAt(int)} which has a relative index into the
         * content, one that doesn't account for HTTP header if present.
         *
         * @param index Index into content adjusted to accomodate initial offset
         * to get us past the HTTP header if present (i.e.
         * {@link #contentOffset}).
         *
         * @return Characater at offset <code>index</code>.
         */
        public char charAt(int index) {
            int c = -1;
            // Add to index start-of-content offset to get us over HTTP header
            // if present.
            index += this.contentOffset;
            if (index < this.prefixBuffer.length) {
                // If index is into our prefix buffer.
                c = this.prefixBuffer[index];
            } else if (index >= this.wrapOrigin &&
                (index - this.wrapOrigin) < this.wraparoundBuffer.length) {
                // If index is into our buffer window on underlying backing file.
                c = this.wraparoundBuffer[
                        ((index - this.wrapOrigin) + this.wrapOffset) %
                            this.wraparoundBuffer.length];
            } else {
                // Index is outside of both prefix buffer and our buffer window
                // onto the underlying backing file.  Fix the buffer window
                // location.
                c = faultCharAt(index);
            }
            // Stream is treated as single byte.  Make sure characters returned
            // are not negative.
            return (char)(c & 0xff);
        }

        /**
         * Get a character that's outside the current buffers.
         *
         * will cause the wraparoundBuffer to be changed to
         * cover a region including the index
         *
         * if index is higher than the highest index in the
         * wraparound buffer, buffer is moved forward such
         * that requested char is last item in buffer
         *
         * if index is lower than lowest index in the
         * wraparound buffer, buffet is reset centered around
         * index
         *
         * @param index Index of character to fetch.
         * @return A character that's outside the current buffers
         */
        private int faultCharAt(int index) {
            if(Thread.interrupted()) {
                throw new RuntimeException("thread interrupted");
            }
            if(index >= this.wrapOrigin + this.wraparoundBuffer.length) {
                // Moving forward
                while (index >= this.wrapOrigin + this.wraparoundBuffer.length)
                {
                    // TODO optimize this
                    advanceBuffer();
                }
                return charAt(index - this.contentOffset);
            }
            // Moving backward
            recenterBuffer(index);
            return charAt(index - this.contentOffset);
        }

        /**
         * Move the buffer window on backing file back centering current access
         * position in middle of window.
         *
         * @param index Index of character to access.
         */
        private void recenterBuffer(int index) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Recentering around " + index + " in " +
                    this.backingFilename);
            }
            this.wrapOrigin = index - (this.wraparoundBuffer.length / 2);
            if(this.wrapOrigin < this.prefixBuffer.length) {
                this.wrapOrigin = this.prefixBuffer.length;
            }
            this.wrapOffset = 0;
            loadBuffer();
        }

        /**
         * Load from backing file into the wrapper buffer.
         */
        private void loadBuffer()
        {
            long len = -1;
            try {
                len = this.raFile.length();
                this.raFile.seek(this.wrapOrigin - this.prefixBuffer.length);
                this.raFile.readFully(this.wraparoundBuffer, 0,
                    Math.min(this.wraparoundBuffer.length,
                         this.absoluteLength - this.wrapOrigin));
            }

            catch (IOException e) {
                // TODO convert this to a runtime error?
                DevUtils.logger.log (
                    Level.SEVERE,
                    "raFile.seek(" +
                    (this.wrapOrigin - this.prefixBuffer.length) +
                    ")\n" +
                    "raFile.readFully(wraparoundBuffer,0," +
                    (Math.min(this.wraparoundBuffer.length,
                        this.length - this.wrapOrigin )) +
                    ")\n"+
                    "raFile.length()" + len + "\n" +
                    DevUtils.extraInfo(),
                    e);
                throw new RuntimeException(e);
            }
        }

        /**
         * Roll the wraparound buffer forward one position
         */
        private void advanceBuffer() {
            try {
                this.wraparoundBuffer[this.wrapOffset] =
                    (byte)this.raFile.read();
                this.wrapOffset++;
                this.wrapOffset %= this.wraparoundBuffer.length;
                this.wrapOrigin++;
            } catch (IOException e) {
                DevUtils.logger.log(Level.SEVERE, "advanceBuffer()" +
                    DevUtils.extraInfo(), e);
                throw new RuntimeException(e);
            }
        }

        public CharSequence subSequence(int start, int end) {
            return new CharSubSequence(this, start, end);
        }

        /**
         * Cleanup resources.
         *
         * @exception IOException Failed close of random access file.
         */
        public void close() throws IOException
        {
            this.prefixBuffer = null;
            if (this.raFile != null) {
                this.raFile.close();
                this.raFile = null;
            }
        }

        /* (non-Javadoc)
         * @see java.lang.Object#finalize()
         */
        protected void finalize() throws Throwable
        {
            super.finalize();
            close();
        }

        /* (non-Javadoc)
         * @see org.archive.io.EnhancedCharSequence#substring(int, int)
         */
        public String substring(int offset, int len) {
            StringBuffer ret = new StringBuffer(len);
            // Add to offset start-of-content offset to get us over HTTP header
            // if present.
            offset += this.contentOffset;
            if (offset < this.prefixBuffer.length) {
                // Need something from the prefix buffer.
                int from = offset;
                // To the end of the buffer
                int count = this.prefixBuffer.length - from;
                if (offset + len < this.prefixBuffer.length) {
                    count = len; // length falls within the buffer.
                } else {
                    // Will need more then is in the prefixBuffer.
                    offset = this.prefixBuffer.length + 1;
                    len = len - count;
                }
                // Since we are dealing with a byte buffer we'll have to use
                // a String and then wrap up in a StringBuffer to concat with
                // the backing file. TODO: This can probably be optimized.
                //
                // Also, force an 8-bit encoding.  Default jvm encoding is
                // usually -- us context -- 7 bit ascii.  If we don't force
                // 8-bit, characters above 127 are considered rubbish.
                try {
                    ret.append(new String(this.prefixBuffer,from,count,
                        DEFAULT_SINGLE_BYTE_ENCODING));
                }
                catch (UnsupportedEncodingException e) {
                    logger.severe("Failed encoding string: " + e.getMessage());
                }
            }
            if (offset >= this.prefixBuffer.length) {
                // TODO: Maybe better performance can be gained by reading
                // blocks from files.
                int to = offset + len;
                for(int i = offset ; i < to ; i++) {
                    ret.append(charAt(i - this.contentOffset));
                }
            }

            return ret.toString();
        }
        
        public String toString() {
            return substring(0, length());
        }
    }

    /**
     * Provides a (Replay)CharSequence view on recorded streams (a prefix
     * buffer and overflow backing file) that can handle streams of multibyte
     * characters.
     *
     * If possible, use {@link ByteReplayCharSequence}.  It performs better even
     * for the single byte case (Decoding is an expensive process).
     *
     * <p>Call close on this class when done so can clean up resources.
     *
     * <p>Implementation currently works by checking to see if content to read
     * all fits the in-memory buffer.  If so, we decode into a CharBuffer and
     * keep this around for CharSequence operations.  This CharBuffer is
     * discarded on close.
     *
     * <p>If content length is greater than in-memory buffer, we decode the
     * buffer plus backing file into a new file named for the backing file w/
     * a suffix of the encoding we write the file as. We then run w/ a
     * memory-mapped CharBuffer against this file to implement CharSequence.
     * Reasons for this implemenation are that CharSequence wants to return the
     * length of the CharSequence.
     *
     * <p>Obvious optimizations would keep around decodings whether the
     * in-memory decoded buffer or the file of decodings written to disk but the
     * general usage pattern processing URIs is that the decoding is used by one
     * processor only.  Also of note, files usually fit into the in-memory
     * buffer.
     *
     * <p>We might also be able to keep up 3 windows that moved across the file
     * decoding a window at a time trying to keep one of the buffers just in
     * front of the regex processing returning it a length that would be only
     * the length of current position to end of current block or else the length
     * could be got by multipling the backing files length by the decoders'
     * estimate of average character size.  This would save us writing out the
     * decoded file.  We'd have to do the latter for files that are
     * > Integer.MAX_VALUE.
     *
     * @author stack
     * @version $Revision$, $Date$
     */
    private class MultiByteReplayCharSequence implements ReplayCharSequence {

        /**
         * Name of the encoding we use writing out concatenated decoded prefix
         * buffer and decoded backing file.
         *
         * <p>This define is also used as suffix for the file that holds the
         * decodings.  The name of the file that holds the decoding is the name
         * of the backing file w/ this encoding for a suffix.
         *
         * <p>See <a ref="http://java.sun.com/j2se/1.4.2/docs/guide/intl/encoding.doc.html">Encoding</a>.
         */
        private static final String WRITE_ENCODING = "UTF-16BE";

        /**
         * CharBuffer of decoded content.
         *
         * Content of this buffer is unicode.
         */
        private CharBuffer content = null;

        /**
         * File that has decoded content.
         *
         * Keep it around so we can remove on close.
         */
        private File decodedFile = null;


        /**
         * Constructor.
         *
         * @param buffer In-memory buffer of recordings prefix.  We read from
         * here first and will only go to the backing file if <code>size</code>
         * requested is greater than <code>buffer.length</code>.
         * @param size Total size of stream to replay in bytes.  Used to find
         * EOS. This is total length of content including HTTP headers if
         * present.
         * @param responseBodyStart Where the response body starts in bytes.
         * Used to skip over the HTTP headers if present.
         * @param backingFilename Path to backing file with content in excess of
         * whats in <code>buffer</code>.
         * @param encoding Encoding to use reading the passed prefix buffer and
         * backing file.  For now, should be java canonical name for the
         * encoding. (If null is passed, we will default to
         * ByteReplayCharSequence).
         *
         * @throws IOException
         */
        private MultiByteReplayCharSequence(byte[] buffer, long size,
                long responseBodyStart, String backingFilename, String encoding)
            throws IOException {
            super();
            if (encoding == null) {
                throw new NullPointerException("Character encoding is null.");
            }

            this.content = decode(buffer, backingFilename, size,
                responseBodyStart, encoding);
         }

        /**
         * Decode passed buffer and backing file into a CharBuffer.
         *
         * This method writes a new file made of the decoded concatenation of
         * the in-memory prefix buffer and the backing file.  Returns a
         * charSequence view onto this new file.
         *
         * @param buffer In-memory buffer of recordings prefix.  We read from
         * here first and will only go to the backing file if <code>size</code>
         * requested is greater than <code>buffer.length</code>.
         * @param size Total size of stream to replay in bytes.  Used to find
         * EOS. This is total length of content including HTTP headers if
         * present.
         * @param responseBodyStart Where the response body starts in bytes.
         * Used to skip over the HTTP headers if present.
         * @param backingFilename Path to backing file with content in excess of
         * whats in <code>buffer</code>.
         * @param encoding Encoding to use reading the passed prefix buffer and
         * backing file.  For now, should be java canonical name for the
         * encoding. (If null is passed, we will default to
         * ByteReplayCharSequence).
         *
         * @return A CharBuffer view on decodings of the contents of passed
         * buffer.
         * @throws IOException
         */
        private CharBuffer decode(byte[] buffer, String backingFilename,
                long size, long responseBodyStart, String encoding)
            throws IOException {

            CharBuffer charBuffer = null;

            if (size <= buffer.length) {
                charBuffer =
                    decodeInMemory(buffer, size, responseBodyStart, encoding);
            } else {
                File backingFile = new File(backingFilename);
                if (!backingFile.exists()) {
                    throw new FileNotFoundException(backingFilename +
                         "doesn't exist");
                }

                // Get decoder.  Will burp if encoding string is bad.
                // Should probably keep it around.  I'm sure its not
                // cheap to construct. Tell decoder to replace bad chars with
                // default marks.
                CharsetDecoder decoder =
                    (Charset.forName(encoding)).newDecoder();
                decoder.onMalformedInput(CodingErrorAction.REPLACE);
                decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
                decoder.reset();

                // Get a memory mapped bytebuffer view onto backing file
                // and a bytebuffer view onto passed in-memory buffer.
                // Pass them around as an array of ByteBuffers.  That I can
                // do this, pass around an array of ByteBuffers rather than
                // a byte array and an input stream, is why I'm using Channels.
                ByteBuffer [] buffers = {ByteBuffer.wrap(buffer),
                    getReadOnlyMemoryMappedBuffer(backingFile)};
                try {
                    this.decodedFile = decodeToFile(buffers, decoder,
                        backingFilename + "." + WRITE_ENCODING);
                } catch (IOException e) {
                    logger.info("Adding info to " + e + ": " +
                        backingFile.toString() +
                        " " + backingFile.length() + " " + backingFile.exists() +
                        size + " " + buffer.length);
                    throw e;
                }
                charBuffer = getReadOnlyMemoryMappedBuffer(this.decodedFile).
                    asCharBuffer();
            }

            return charBuffer;
        }

        /**
         * Decode passed buffer into a CharBuffer.
         *
         * This method decodes a memory buffer returning a memory buffer.
         *
         * @param buffer In-memory buffer of recordings prefix.  We read from
         * here first and will only go to the backing file if <code>size</code>
         * requested is greater than <code>buffer.length</code>.
         * @param size Total size of stream to replay in bytes.  Used to find
         * EOS. This is total length of content including HTTP headers if
         * present.
         * @param responseBodyStart Where the response body starts in bytes.
         * Used to skip over the HTTP headers if present.
         * @param encoding Encoding to use reading the passed prefix buffer and
         * backing file.  For now, should be java canonical name for the
         * encoding. (If null is passed, we will default to
         * ByteReplayCharSequence).
         *
         * @return A CharBuffer view on decodings of the contents of passed
         * buffer.
         */
        private CharBuffer decodeInMemory(byte[] buffer, long size,
                long responseBodyStart, String encoding)
        {
            ByteBuffer bb = ByteBuffer.wrap(buffer);
            // Move past the HTTP header if present.
            bb.position((int)responseBodyStart);
            // Set the end-of-buffer to be end-of-content.
            bb.limit((int)size);
            return (Charset.forName(encoding)).decode(bb).asReadOnlyBuffer();
        }

        /**
         * Create read-only memory-mapped buffer onto passed file.
         *
         * @param file File to get memory-mapped buffer on.
         * @return Read-only memory-mapped ByteBuffer view on to passed file.
         * @throws IOException
         */
        private ByteBuffer getReadOnlyMemoryMappedBuffer(File file)
            throws IOException {

            ByteBuffer bb = null;
            FileInputStream in = null;
            FileChannel c = null;
            assert file.exists(): "No file " + file.getAbsolutePath();

            try {
                in = new FileInputStream(file);
                c = in.getChannel();
                // TODO: Confirm the READ_ONLY works.  I recall it not working.
                // The buffers seem to always say that the buffer is writeable.
                bb = c.map(FileChannel.MapMode.READ_ONLY, 0, c.size()).
                    asReadOnlyBuffer();
            }

            finally {
                if (c != null && c.isOpen()) {
                    c.close();
                }
                if (in != null) {
                    in.close();
                }
            }

            return bb;
        }

        /**
         * Decode passed array of bytebuffers into a file of passed name.
         *
         * This is the guts of the
         * {@link #decode(byte[], String, long, long, String)} method.
         *
         * @param buffers Array of byte buffers.
         * @param decoder Decoder to use decoding.
         * @param name Name of file we decoded into.
         *
         * @return File we put unicode decoding into.
         * @throws IOException
         */
        private File decodeToFile(ByteBuffer [] buffers, CharsetDecoder decoder,
                String name)
            throws IOException {

            if (buffers.length <= 0) {
                throw new IllegalArgumentException("No bytebuffers to read.");
            }
            
            // Place to catch decodings in memory. I'll then write to writer. If
            // I can't get a character array from it, then there is going to
            // be probs so throw exception here.
            CharBuffer cb = CharBuffer.allocate(1024 * 4);
            if (!cb.hasArray()) {
                throw new IOException("Can't get array from CharBuffer.");
            }

            File unicode = new File(name);
            if (unicode.exists()) {
                logger.warning(unicode.toString() + " already exists");
            }
            Writer writer = null;

            IOException exceptionThrown = null;
            try {
                // Get a writer.  Output in our WRITE_ENCODING.
                writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(unicode), WRITE_ENCODING));
                CoderResult result = null;
                ByteBuffer bb = null;
                for (int i = 0; i < buffers.length; i++) {
                    bb = manageTransition(bb, buffers[i], decoder, cb);
                    // If we fill the decoder buffer or if decoder reports
                    // underfilled and the buffer has content in it, then go
                    // and drain the buffer and recall the decoder.
                    while((result = decoder.decode(bb, cb, false))
                                == CoderResult.OVERFLOW) {
                        drainCharBuffer(cb, writer);
                    }

                    if (result != CoderResult.UNDERFLOW) {
                        throw new IOException("Unexpected result: " + result);
                    }
                }

                if ((result = decoder.decode(bb, cb, true)) ==
                        CoderResult.OVERFLOW) {
                    drainCharBuffer(cb, writer);
                }

                // Flush any remaining state from the decoder, being careful
                // to detect output buffer overflow(s)
                while (decoder.flush(cb) == CoderResult.OVERFLOW) {
                    drainCharBuffer(cb, writer);
                }

                // Drain any chars remaining in the output buffer
                drainCharBuffer(cb, writer);
            }

            catch (IOException e) {
                exceptionThrown = e;
                throw e;
            }

            finally {
                if (writer != null) {
                    try {
                        writer.flush();
                        writer.close();
                    } catch (IOException e) {
                        if (exceptionThrown != null) {
                            exceptionThrown.printStackTrace();
                        }
                        logger.log(Level.WARNING, "Failed close.  Check logs", e);
                    }
                }
                if (!unicode.exists()) {
                    logger.warning(unicode.toString() + " does not exist, " +
                        buffers.length);
                }
                if (exceptionThrown != null) {
                    deleteFile(unicode, exceptionThrown);
                }
            }

            return unicode;
        }

        /**
         * Manage buffer transition.
         *
         * Handle case of multibyte characters spanning buffers.
         * See "[ 935122 ] ToeThreads hung in ExtractorHTML after Pause":
         * http://sourceforge.net/tracker/?func=detail&aid=935122&group_id=73833&atid=539099
         *
		 * @param previous The buffer we're leaving.
		 * @param next The buffer we're going to.
         * @param decoder Decoder to use decoding.
         * @param cb Where we're putting decoded characters.
		 * @return Pointer to next buffer with postion set to just past
         * any byte read making the transition.
		 */
		private ByteBuffer manageTransition(ByteBuffer previous,
             ByteBuffer next, CharsetDecoder decoder, CharBuffer cb) {

            if (previous == null || !previous.hasRemaining()) {
                return next;
            }

            // previous has content remaining but its just a piece of a
            // multibyte character.  Need to go to next buffer to get the
            // rest of the character.  Save off tail for moment.

            ByteBuffer tail = previous.slice();
            int cbPosition = cb.position();

            ByteBuffer tbb = null;
            final int MAX_CHAR_SIZE = 6;
            for (int i = 0; i < MAX_CHAR_SIZE; i++) {
            	    tbb = ByteBuffer.allocate(tail.capacity() + i + 1);
                tbb.put(tail);
                // Copy first bytes without disturbing buffer position.
                for (int j = 0; j <= i; j++) {
                    tbb.put(next.get(j));
                }
                CoderResult result = decoder.decode(tbb, cb, false);
                if (result == CoderResult.UNDERFLOW) {
                	    // Enough bytes for us to move on.
                    next.position(i + 1);
                    break;
                }
                // Go around again.  Restore cb to where it was.
                cb.position(cbPosition);
                tail.rewind();
            }

            return next;
		}

		/**
         * Helper method to drain the char buffer and write its content to
         * the given output stream (as bytes).  Upon return, the buffer is empty
         * and ready to be refilled.
         *
         * @param cb A CharBuffer containing chars to be written.
         * @param writer An output stream to consume the bytes in cb.
		 * @throws IOException
         */
        private void drainCharBuffer(CharBuffer cb, Writer writer)
            throws IOException  {

            // Prepare buffer for draining
            cb.flip();

            // This writes the chars contained in the CharBuffer but doesn't
            // actually modify the state of the buffer. If the char buffer was
            // being drained by calls to get(), a loop might be needed here.
            if (cb.hasRemaining()) {
                writer.write (cb.array(), cb.arrayOffset(), cb.limit());
            }

            cb.clear();        // Prepare buffer to be filled again
        }

        private void deleteFile(File fileToDelete) {
            deleteFile(fileToDelete, null);        
        }

        private void deleteFile(File fileToDelete, final Exception e) {
            if (e != null) {
                // Log why the delete to help with debug of java.io.FileNotFoundException:
                // ....tt53http.ris.UTF-16BE.
                logger.severe("Deleting " + fileToDelete + " because of "
                    + e.toString());
            }
            if (fileToDelete != null && fileToDelete.exists()) {
                fileToDelete.delete();
            }
        }

        public void close()
        {
            this.content = null;
            deleteFile(this.decodedFile);
        }

        protected void finalize() throws Throwable
        {
            super.finalize();
            close();
        }

        public int length()
        {
            return this.content.limit();
        }

        public char charAt(int index)
        {
            return this.content.get(index);
        }

        public CharSequence subSequence(int start, int end) {
            return new CharSubSequence(this, start, end);
        }
        
        public String toString() {
            StringBuffer sb = new StringBuffer(length());
            // could use StringBuffer.append(CharSequence) if willing to do 1.5 & up
            for (int i = 0;i<length();i++) {
                sb.append(charAt(i)); 
            }
            return sb.toString();
        }
    }
}
