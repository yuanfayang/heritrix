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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

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
     * Pattern to use stripping underscores and hyphens from encoding names.
     */
    private Pattern HYPHENS_UNDERSCORES = Pattern.compile("[-_]?");
    
    
    /**
     * Private constructor.
     * 
     * Private ensures only one singleton instance.
     */
    private ReplayCharSequenceFactory()
    {
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
        if (encoding != null)
        {
            // TODO: Add a set here that we populate from a properties
            // file that has in it a list of charsets that we'd like to
            // be handled by the multibyte ReplayCharSequence.  Members of this
            // set would include names of charsets other than the canonical 
            // java names.
            
            try
            {
                "".getBytes(encoding);
                // Lower case and replace all hyphens and underscores to make
                // it easier to compare (Sometimes an encoding can have the 
                // underscore and other times not as in ISO-8859-1 or ISO8859-1.
                // As an aside, java seems fine w/ iso-8859-1 and ISO-8859-1:
                // i.e. its case insenstive.
                String encodingLowerCase = encoding.toLowerCase();
                encodingLowerCase = this.HYPHENS_UNDERSCORES.
                    matcher(encodingLowerCase).replaceAll("");
                if (encodingLowerCase.startsWith("utf")         ||
                    encodingLowerCase.startsWith("euc")         ||
                    encodingLowerCase.startsWith("xeuc")        ||
                    encodingLowerCase.startsWith("shift")       ||
                    encodingLowerCase.startsWith("sjis")        ||
                    encodingLowerCase.startsWith("iso2022")     ||
                    encodingLowerCase.startsWith("windows31")   ||
                    encodingLowerCase.startsWith("big5")        ||
                    encodingLowerCase.startsWith("ms9")         ||
                    encodingLowerCase.startsWith("xms9")        ||
                    encodingLowerCase.startsWith("xmswin9")     ||
                    encodingLowerCase.startsWith("xwindows9")   ||
                    encodingLowerCase.startsWith("cp9")         ||
                    encodingLowerCase.startsWith("cp10")        ||
                    encodingLowerCase.startsWith("gb180")       ||
                    encodingLowerCase.startsWith("gbk")         ||
                    encodingLowerCase.startsWith("iscii") ) {
                    
                    isMultibyte = true;
                    logger.info("Encoding " + encoding +
                        " considered multibyte.");
                }
            }
            
            catch (UnsupportedEncodingException e)
            {
                // Unsupported encoding.  Default to singlebyte.
                logger.info("Unsupported encoding " + encoding +
                    ".  Defaulting to single byte.");
            }
        }
        logger.fine("Encoding " + encoding + " is multibyte: " +
            ((isMultibyte)? Boolean.TRUE: Boolean.FALSE));
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
            throw new IllegalArgumentException("Unexpected response body" +
                " offset of " + responseBodyStart + ".  The way this" +
                " class works, it assumes the HTTP headers are in buffer: " +
                buffer.length);            
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
     * Uses a wraparound rolling buffer of the last windowSize bytes read
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

        public char charAt(int index) {
            // Add to index start-of-content offset to get us over HTTP header
            // if present.
            return charAtAbsolute(index + this.contentOffset);
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
        private char charAtAbsolute(int index)
        {
            char c;
            if (index < this.prefixBuffer.length) {
            
                // If index is into our prefix buffer.
                c = (char) this.prefixBuffer[index];
                
            } else if (index >= this.wrapOrigin &&
                (index - this.wrapOrigin) < this.wraparoundBuffer.length) {
                
                // If index is into our buffer window on underlying backing file.
                c = (char)this.wraparoundBuffer[
                        ((index - this.wrapOrigin) + this.wrapOffset) %
                            this.wraparoundBuffer.length];
            } else {
                
                // Index is outside of both prefix buffer and our buffer window
                // onto the underlying backing file.  Fix the buffer window
                // location.
                c = faultCharAt(index);
            }
            
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
        private char faultCharAt(int index) {
            if(index >= this.wrapOrigin + this.wraparoundBuffer.length) {
                // Moving forward
                while (index >= this.wrapOrigin + this.wraparoundBuffer.length)
                {
                    // TODO optimize this
                    advanceBuffer();
                }
                return charAtAbsolute(index);
            } else {
                // Moving backward
                recenterBuffer(index);
                return charAtAbsolute(index);
            }
        }

        /**
         * Move the buffer window on backing file back centering current access
         * position in middle of window.
         * 
         * @param index Index of character to access.
         */
        private void recenterBuffer(int index) {
            logger.info("Recentering around " + index + " in " +
                this.backingFilename);
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
                // TODO convert this to a runtime error?
                DevUtils.logger.log(Level.SEVERE, "advanceBuffer()" +
                    DevUtils.extraInfo(), e);
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
         * This define is also used as suffix for the file that holds the
         * decodings.  The name of the file that holds the decoding is the name
         * of the backing file w/ this encoding for a suffix.
         * 
         * @see http://java.sun.com/j2se/1.4.2/docs/guide/intl/encoding.doc.html
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
                this.decodedFile = decodeToFile(buffers, decoder,
                    backingFilename + "." + WRITE_ENCODING);
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
         */
        private File decodeToFile(ByteBuffer [] buffers, CharsetDecoder decoder,
                String name)
            throws IOException {
            
            if (buffers.length <= 0) {
                throw new IllegalArgumentException("No bytebuffers to read.");
            }
            
            File unicode = new File(name);
            Writer writer = null;

            // Place to catch decodings in memory. I'll then write to writer. If 
            // I can't get a character array from it, then there is going to 
            // be probs so throw exception here.
            CharBuffer cb = CharBuffer.allocate(1024 * 4);
            if (!cb.hasArray()) {
                throw new IOException("Can't get array from CharBuffer.");
            }
            
            boolean isException = false;
            try {
                // Get a writer.  Output in our WRITE_ENCODING.
                writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(unicode), WRITE_ENCODING));
                assert writer != null: "Writer is null: " + name;
                CoderResult result = null;
                ByteBuffer bb = null;
                for (int i = 0; i < buffers.length; i++) {
                    bb = buffers[i];
                    assert bb.hasRemaining(): "Buffer has nought in it: " + i;
                    while((result = decoder.decode(bb, cb, false))
                            == CoderResult.UNDERFLOW && bb.hasRemaining()) {
                        if (drainCharBuffer(cb, writer)) {
                            assert unicode.exists(): "No file: " +
                                unicode.getAbsolutePath();
                        }
                    }
                    
                    if (result != CoderResult.UNDERFLOW) {
                        throw new IOException("Unexpected result: " + result);
                    }
                }
                
                if ((result = decoder.decode(bb, cb, true)) ==
                        CoderResult.OVERFLOW) {
                    if(drainCharBuffer(cb, writer)) {
                        assert unicode.exists(): "No file: " +
                            unicode.getAbsolutePath();
                    }
                }
               
                // REMOVE
                // assert unicode.exists(): "No file: " + unicode.getAbsolutePath();
            
                // Flush any remaining state from the decoder, being careful
                // to detect output buffer overflow(s)
                while (decoder.flush(cb) == CoderResult.OVERFLOW) {
                    if (drainCharBuffer(cb, writer)) {
                        assert unicode.exists(): "No file: " +
                            unicode.getAbsolutePath();
                    }
                }
            
                // REMOVE
                assert unicode.exists(): "No file: " + unicode.getAbsolutePath();
                // Drain any chars remaining in the output buffer
                if (drainCharBuffer(cb, writer)) {
                    assert unicode.exists(): "No file: " +
                        unicode.getAbsolutePath();
                }
            }
            
            catch (IOException e) {
                isException = true;
                throw e;
            }
            
            finally {
                // REMOVE
                //assert unicode.exists(): "No file: " + unicode.getAbsolutePath();
                if (writer != null) {
                    writer.close();
                }
                // REMOVE
                //assert unicode.exists(): "No file: " + unicode.getAbsolutePath();
                //assert isException: "IsException is true";
                //if (isException) {
                //    deleteFile(unicode);
                //}
            }

            //assert unicode.exists(): "No file: " + unicode.getAbsolutePath();
            return unicode;
        }
            
        /**
         * Helper method to drain the char buffer and write its content to
         * the given output stream (as bytes).  Upon return, the buffer is empty
         * and ready to be refilled.
         * 
         * @param cb A CharBuffer containing chars to be written.
         * @param out An output stream to consume the bytes in cb.
         */
        private boolean drainCharBuffer(CharBuffer cb, Writer writer)
            throws IOException  {
            
            boolean wrote = false;
            
            // Prepare buffer for draining
            cb.flip();
        
            // This writes the chars contained in the CharBuffer but doesn't
            // actually modify the state of the buffer. If the char buffer was
            // being drained by calls to get(), a loop might be needed here.
            if (cb.hasRemaining()) {
                // REMOVE
                logger.info("Writing " + cb.arrayOffset() + ", " +
                    cb.limit());
                assert cb.limit() > cb.arrayOffset(): cb.limit() + ", " +
                    cb.arrayOffset();
                wrote = cb.limit() > cb.arrayOffset();
                writer.write (cb.array(), cb.arrayOffset(), cb.limit());
            }
        
            cb.clear();        // Prepare buffer to be filled again
            
            return wrote;
        }
        
        private void deleteFile(File fileToDelete) {
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
    }
}
