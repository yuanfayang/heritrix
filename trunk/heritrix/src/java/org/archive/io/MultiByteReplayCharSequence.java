/* MultiByteReplayCharSequenceFactory
 *
 * (Re)Created on Dec 21, 2006
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
package org.archive.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.logging.Level;

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
class MultiByteReplayCharSequence implements ReplayCharSequence {

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
    MultiByteReplayCharSequence(byte[] buffer, long size,
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
                ReplayCharSequenceFactory.logger.info("Adding info to " + e + ": " +
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
            ReplayCharSequenceFactory.logger.warning(unicode.toString() + " already exists");
        }
        Writer writer = null;

        IOException exceptionThrown = null;
        int step = -1; 
        try {
            step = 0;
            // Get a writer.  Output in our WRITE_ENCODING.
            writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(unicode), WRITE_ENCODING));
            step = 1; 
            CoderResult result = null;
            ByteBuffer bb = null;
            for (int i = 0; i < buffers.length; i++) {
                bb = manageTransition(bb, buffers[i], decoder, cb);
                // If we fill the decoder buffer or if decoder reports
                // underfilled and the buffer has content in it, then go
                // and drain the buffer and recall the decoder.
                step = 2; 
                while((result = decoder.decode(bb, cb, false))
                            == CoderResult.OVERFLOW) {
                    drainCharBuffer(cb, writer);
                }
                step = 3; 
                if (result != CoderResult.UNDERFLOW) {
                    throw new IOException("Unexpected result: " + result);
                }
                step = 4; 
            }
            step = 5; 
            if ((result = decoder.decode(bb, cb, true)) ==
                    CoderResult.OVERFLOW) {
                step = 6; 
                drainCharBuffer(cb, writer);
                step = 7; 
            }
            step = 8; 
            // Flush any remaining state from the decoder, being careful
            // to detect output buffer overflow(s)
            while (decoder.flush(cb) == CoderResult.OVERFLOW) {
                step = 9; 
                drainCharBuffer(cb, writer);
                step = 10; 
            }
            step = 11; 
            // Drain any chars remaining in the output buffer
            drainCharBuffer(cb, writer);
            step = 12; 
        }

        catch (IOException e) {
            exceptionThrown = e;
            ReplayCharSequenceFactory.logger.log(Level.WARNING, "IOE, highest step="+step, e);
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
                    ReplayCharSequenceFactory.logger.log(Level.WARNING, "Failed close.  Check logs", e);
                }
            }
            if (!unicode.exists()) {
                ReplayCharSequenceFactory.logger.warning(unicode.toString() + 
                " does not exist, buffers.length=" +
                buffers.length + " step=" + step);
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
            ReplayCharSequenceFactory.logger.severe("Deleting " + fileToDelete + " because of "
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