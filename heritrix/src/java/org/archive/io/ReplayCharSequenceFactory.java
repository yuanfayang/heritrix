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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.logging.Level;
import java.util.logging.Logger;



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
}
