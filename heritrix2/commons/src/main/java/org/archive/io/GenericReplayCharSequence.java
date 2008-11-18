/* GenericReplayCharSequence
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.text.NumberFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.util.DevUtils;

/**
 * (Replay)CharSequence view on recorded streams.
 *
 * For small streams, use {@link InMemoryReplayCharSequence}.
 *
 * <p>Call {@link close()} on this class when done to clean up resources.
 *
 * @author stack
 * @author nlevitt
 * @version $Revision: 5554 $, $Date: 2007-11-14 16:53:00 -0800 (Wed, 14 Nov 2007) $
 */
public class GenericReplayCharSequence implements ReplayCharSequence {

    protected static Logger logger = Logger
            .getLogger(GenericReplayCharSequence.class.getName());

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

    private static final long MAP_MAX_BYTES = 64 * 1024 * 1024; // 64M
	
	/**
	 * When the memory map moves away from the beginning of the file 
	 * (to the "right") in order to reach a certain index, it will
	 * map up to this many bytes preceding (to the left of) the target character. 
	 * Consequently it will map up to 
	 * <code>MAP_MAX_BYTES - MAP_TARGET_LEFT_PADDING</code>
	 * bytes to the right of the target.
	 */
	private static final long MAP_TARGET_LEFT_PADDING_BYTES = (long) (MAP_MAX_BYTES * 0.2);

    /**
     * Total length of character stream to replay minus the HTTP headers
     * if present. 
     * 
     * If the backing file is larger than <code>Integer.MAX_VALUE</code> (i.e. 2gb),
     * only the first <code>Integer.MAX_VALUE</code> characters are available through this API. 
     * We're overriding <code>java.lang.CharSequence</code> so that we can use 
     * <code>java.util.regex</code> directly on the data, and the <code>CharSequence</code> 
     * API uses <code>int</code> for the length and index.
     */
    protected int length;
    
	/**
     * Byte offset into the file where the memory mapped portion begins.
     */
    private long mapByteOffset;

    // XXX do we need to keep the input stream around?
    private FileInputStream backingFileIn = null;

    private FileChannel backingFileChannel = null;

    private int bytesPerChar;

    private ByteBuffer mappedBuffer = null;

    private CharsetDecoder decoder = null;

    private ByteBuffer tempBuf = null;

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
     * @param contentReplayInputStream inputStream of content
     * @param charsetName Encoding to use reading the passed prefix
     * buffer and backing file.  For now, should be java canonical name for the
     * encoding. Must not be null.
     * @param backingFilename Path to backing file with content in excess of
     * whats in <code>buffer</code>.
     *
     * @throws IOException
     */
	public GenericReplayCharSequence(
            ReplayInputStream contentReplayInputStream, String backingFilename,
            String charsetName) throws IOException {
        super();
        logger.info("new GenericReplayCharSequence() characterEncoding="
                + charsetName + " backingFilename=" + backingFilename);

        Charset charset = Charset.forName(charsetName);
        if (charset.name().equals("UTF-16BE")
                || charset.name().equals("UTF-16LE")
                || charset.newEncoder().maxBytesPerChar() == 1.0) {
            logger.info("charset=" + charsetName
                    + ": supports random access, using backing file directly");
            this.bytesPerChar = (int) charset.newEncoder().averageBytesPerChar();
            this.backingFileIn = new FileInputStream(backingFilename);
            this.decoder = charset.newDecoder();
        } else {
            logger.info("charset=" + charsetName
                            + ": may not support random access, decoding to separate file");

            // decodes only up to Integer.MAX_VALUE characters
            decodeToFile(contentReplayInputStream, backingFilename, charsetName);

            this.bytesPerChar = 2;
            this.backingFileIn = new FileInputStream(decodedFile);
            this.decoder = Charset.forName(WRITE_ENCODING).newDecoder();
        }

        this.tempBuf = ByteBuffer.wrap(new byte[this.bytesPerChar]);
        this.backingFileChannel = backingFileIn.getChannel();

        // we only support the first Integer.MAX_VALUE characters
        if (this.backingFileChannel.size() / bytesPerChar <= Integer.MAX_VALUE) {
            this.length = (int) (this.backingFileChannel.size() / bytesPerChar);
        } else {
            this.length = Integer.MAX_VALUE;
        }

        this.mapByteOffset = 0;
        updateMemoryMappedBuffer();
    }

    private void updateMemoryMappedBuffer() {
        long mapSize = Math.min((long) this.length * (long) this.bytesPerChar
                - this.mapByteOffset, MAP_MAX_BYTES);
        logger.fine("updateMemoryMappedBuffer: mapOffset="
                + NumberFormat.getInstance().format(this.mapByteOffset)
                + " mapSize=" + NumberFormat.getInstance().format(mapSize));
        try {
            System.gc();
            System.runFinalization();
            // TODO: Confirm the READ_ONLY works. I recall it not working.
            // The buffers seem to always say that the buffer is writable.
            this.mappedBuffer = this.backingFileChannel.map(
                    FileChannel.MapMode.READ_ONLY, this.mapByteOffset, mapSize)
                    .asReadOnlyBuffer();
        } catch (IOException e) {
            // TODO convert this to a runtime error?
            DevUtils.logger.log(Level.SEVERE,
                    " backingFileChannel.map() mapByteOffset=" + mapByteOffset
                            + " mapSize=" + mapSize + "\n" + "decodedFile="
                            + decodedFile + " length=" + length + "\n"
                            + DevUtils.extraInfo(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Converts the first <code>Integer.MAX_VALUE</code> characters from the
     * file <code>backingFilename</code> from encoding <code>encoding</code> to
     * encoding <code>WRITE_ENCODING</code> and saves as
     * <code>this.decodedFile</code>, which is named <code>backingFilename
     * + "." + WRITE_ENCODING</code>.
     * 
     * @throws IOException
     */
    private void decodeToFile(ReplayInputStream inStream,
            String backingFilename, String encoding) throws IOException {

        BufferedReader reader = new BufferedReader(new InputStreamReader(
                inStream, encoding));

        this.decodedFile = new File(backingFilename + "." + WRITE_ENCODING);

        logger.info("decodeToFile: backingFilename=" + backingFilename
                + " encoding=" + encoding + " decodedFile=" + decodedFile);

        FileOutputStream fos;
        try {
            fos = new FileOutputStream(this.decodedFile);
        } catch (FileNotFoundException e) {
            // Windows workaround attempt
            System.gc();
            System.runFinalization();
            logger.info("Windows 'file with a user-mapped section open' "
                    + "workaround gc-finalization performed.");
            // try again
            fos = new FileOutputStream(this.decodedFile);
        }
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos,
                WRITE_ENCODING));

        int c;
        long count = 0;
        while ((c = reader.read()) >= 0 && count < Integer.MAX_VALUE) {
            writer.write(c);
            count++;
            if (count % 100000000 == 0) {
                logger.fine("wrote " + count + " characters so far...");
            }
        }
        writer.close();

        logger.info("decodeToFile: wrote " + count + " characters to "
                + decodedFile);
        if (c >= 0 && count == Integer.MAX_VALUE) {
            logger.warning("input stream is longer than Integer.MAX_VALUE="
                            + NumberFormat.getInstance().format(
                                    Integer.MAX_VALUE)
                            + " characters -- only first "
                            + NumberFormat.getInstance().format(
                                    Integer.MAX_VALUE)
                            + " written and accessible through this GenericReplayCharSequence");
        }
    }

    /**
     * Get character at passed absolute position.
     * @param index Index into content 
     * @return Character at offset <code>index</code>.
     */
	public char charAt(int index) {
        if (index < 0 || index >= this.length()) {
            throw new IndexOutOfBoundsException("index=" + index
                    + " - should be between 0 and length()=" + this.length());
        }

        if (index * this.bytesPerChar < this.mapByteOffset
                || index * this.bytesPerChar - this.mapByteOffset >= mappedBuffer
                        .limit()) {
            // this.mapByteOffset is bounded by 0 and file size +/- size of the
            // map, and starts as close to <code>index -
            // MAP_TARGET_LEFT_PADDING_BYTES</code> as it can while also not
            // being smaller than it needs to be.
            this.mapByteOffset = Math.max(0, (long) index
                    * (long) this.bytesPerChar
                    - (long) MAP_TARGET_LEFT_PADDING_BYTES);
            this.mapByteOffset = Math.min(this.mapByteOffset, (long) this
                    .length()
                    * (long) this.bytesPerChar - MAP_MAX_BYTES);
            updateMemoryMappedBuffer();
        }

        // CharsetDecoder always decodes up to the end of the ByteBuffer, so we
        // create a new ByteBuffer with only the bytes we're interested in.
        this.mappedBuffer.position((int) ((long) index * this.bytesPerChar - this.mapByteOffset));
        this.mappedBuffer.get(tempBuf.array());
        tempBuf.position(0); // decoder starts at this position

        try {
            CharBuffer cbuf = this.decoder.decode(tempBuf);
            return cbuf.get();
        } catch (CharacterCodingException e) {
            logger.warning("unable to get character at index " + index + ": "
                    + e);
            // U+FFFD REPLACEMENT CHARACTER --
            // "used to replace an incoming character whose value is unknown or unrepresentable in Unicode"
            return (char) 0xfffd;
        }
    }

    public CharSequence subSequence(int start, int end) {
        return new CharSubSequence(this, start, end);
    }

    private void deleteFile(File fileToDelete) {
        deleteFile(fileToDelete, null);
    }

    private void deleteFile(File fileToDelete, final Exception e) {
        if (e != null) {
            // Log why the delete to help with debug of
            // java.io.FileNotFoundException:
            // ....tt53http.ris.UTF-16BE.
            logger.severe("Deleting " + fileToDelete + " because of "
                    + e.toString());
        }
        if (fileToDelete != null && fileToDelete.exists()) {
            logger.info("deleting file: " + fileToDelete);
            fileToDelete.delete();
        }
    }

    public void close() throws IOException {
        logger.info("closing");

        if (this.backingFileChannel != null && this.backingFileChannel.isOpen()) {
            this.backingFileChannel.close();
        }
        if (backingFileIn != null) {
            backingFileIn.close();
        }

        deleteFile(this.decodedFile);

        // clear decodedFile -- so that double-close (as in finalize()) won't
        // delete a later instance with same name see bug [ 1218961 ]
        // "failed get of replay" in ExtractorHTML... usu: UTF-16BE
        this.decodedFile = null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#finalize()
     */
    protected void finalize() throws Throwable {
        super.finalize();
        logger.info("finalizing");
        close();
    }

    /**
     * Convenience method for getting a substring.
     * 
     * @deprecated please use subSequence() and then toString() directly
     */
    public String substring(int offset, int len) {
        return subSequence(offset, offset + len).toString();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(this.length());
        sb.append(this);
        return sb.toString();
    }

    public int length() {
        return this.length;
    }
}
