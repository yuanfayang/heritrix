/* Copyright (C) 2003 Internet Archive.
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
 *
 * ReplayCharSequence.java
 * Created on Sep 30, 2003
 *
 * $Header$
 */
package org.archive.io;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.crawler.io.CharSubSequence;
import org.archive.util.DevUtils;


/**
 * Provides a CharSequence view on recorded stream bytes (a prefix buffer
 * and overflow backing file).
 *
 * Uses a wraparound rolling buffer of the last windowSize bytes read
 * from disk in memory; as long as the 'random access' of a CharSequence
 * user stays within this window, access should remain fairly efficient.
 * (So design any regexps pointed at these CharSequences to work within
 * that range!)
 *
 * <p>Clients should call {@link #close()} when done so we can clean up our
 * mess.
 *
 * <p>When rereading of a location is necessary, the whole window is
 * recentered around the location requested. (TODO: More research
 * into whether this is the best strategy.)
 * 
 * <p>An implementation of a ReplayCharSequence done with ByteBuffers -- one to
 * wrap the passed prefix buffer and the second, a memory-mapped ByteBuffer
 * view into the backing file -- was consistently slower: ~10%.  My tests did
 * the following. Made a buffer filled w/ regular content.  This buffer was used
 * as the prefix buffer.  The buffer content was written MULTIPLER times to 
 * a backing file.  I then did accesses w/ the following pattern: Skip forward
 * 32 bytes, then back 16 bytes, and then read forward from byte 16-32.  Repeat.
 * Though I varied the size of the buffer to the size of the backing file,
 * from 3-10, the difference of 10% or so seemed to persist.  Same if I tried
 * to favor get() over get(index). I used a profiler, JMP, to study times taken
 * (St.Ack did above comment).
 *
 * <p>TODO determine in memory mapped files is better way to do this;
 * probably not -- they don't offer the level of control over
 * total memory used that this approach does.
 * 
 * @author Gordon Mohr
 */
public class ReplayCharSequence implements CharSequence {
    
    /**
     * Logger.
     */
    private static Logger logger =
        Logger.getLogger("org.archive.io.ReplayCharSequence");
    
    /**
     * Buffer that holds the first bit of content.
     * 
     * Once this is exhausted we go to the backing file.
     */
    protected byte[] prefixBuffer;
    
    /**
     * Total length of stream to replay. Used to find EOS.
     */
    protected int length;

    protected byte[] wraparoundBuffer;
    
    /**
     * Index in underlying bytestream where wraparound buffer starts
     */
    protected int wrapOrigin;
    
    /**
     * Index in wraparoundBuffer that corresponds to wrapOrigin
     */
    protected int wrapOffset;

    /**
     * Name of backing file we go to when we've exhausted content from the 
     * prefix buffer.
     */
    protected String backingFilename;
    
    /**
     * Random access to the backing file.
     */
    protected RandomAccessFile raFile;


    /**
     * Constructor.
     * 
     * @param buffer In-memory buffer of recording.  We read from here first
     * and will only go to the backing file if <code>size</code> requested
     * is greater than <code>buffer.length</code>.
     * @param size Total size of stream to replay in bytes.  Used to find EOS.
     * This is total length of content including HTTP headers if present.
     * @param backingFilename Name to use for backing randomaccessfile.
     * 
     * @throws IOException
     */
    public ReplayCharSequence(byte[] buffer, long size, String backingFilename)
        throws IOException {
        this(buffer, size, 0, backingFilename);
    }
    
    /**
     * Constructor.
     * 
     * @param buffer In-memory buffer of recording.  We read from here first
     * and will only go to the backing file if <code>size</code> requested
     * is greater than <code>buffer.length</code>.
     * @param size Total size of stream to replay in bytes.  Used to find EOS.
     * This is total length of content including HTTP headers if present.
     * @param responseBodyStart Where the response body starts. Used to 
     * skip over the HTTP headers if present.
     * @param backingFilename Name to use for backing randomaccessfile.
     * 
     * @throws IOException
     */
    public ReplayCharSequence(byte[] buffer, long size, long responseBodyStart,
            String backingFilename)
        throws IOException {

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
                    " offset of " + responseBodyStart + ".  The way this class"+
                    " works, it assumes the HTTP headers are in buffer: " +
                    buffer.length);            
        }
        
        if ((size - responseBodyStart) > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Length is bigger than we  can" +
               " handle: " + (size - responseBodyStart));
        }
        
        // This is how much content we're going to read.
        this.length = (int)(size - responseBodyStart);
                
        if (responseBodyStart == 0) {
            this.prefixBuffer = buffer;
        } else {
            // This copy is painful but alternative is carrying around the 
            // responseBodyStart and doing lots of arithemetic in the below 
            // ensuring we never read from before responseBodyStart.
            int bufferSize = Math.min(this.length,
                    buffer.length - (int)responseBodyStart);
            this.prefixBuffer = new byte[bufferSize];
            System.arraycopy(buffer, (int)responseBodyStart, this.prefixBuffer,
                0, bufferSize);
        }

        // If amount to read is > than what is in our prefix buffer, then open
        // the backing file.
        if (this.length > this.prefixBuffer.length) {
            this.backingFilename = backingFilename;
            this.raFile = new RandomAccessFile(backingFilename, "r");
            this.wraparoundBuffer = new byte[this.prefixBuffer.length];
            this.wrapOrigin = this.prefixBuffer.length;
            this.wrapOffset = 0;
            loadBuffer();
        }
    }

    /**
     * @return Length of stream to replay.
     */
    public int length() {
        return this.length;
    }

    /* (non-Javadoc)
     * @see java.lang.CharSequence#charAt(int)
     */
    public char charAt(int index)
    {
        if (index < this.prefixBuffer.length) {
            // Mask to unsigned
            return (char) (this.prefixBuffer[index] & 0xFF);
        }
        
        if (index >= this.wrapOrigin &&
            index - this.wrapOrigin < this.wraparoundBuffer.length) {
            // Mask to unsigned
            return (char)(this.wraparoundBuffer[
                (index - this.wrapOrigin + this.wrapOffset) %
                    this.wraparoundBuffer.length] & 0xFF);
        }
        
        return faultCharAt(index);
    }

    /**
     * get a character that's outside the current buffers
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
            return charAt(index);
        } else {
            // moving backward
            recenterBuffer(index);
            return charAt(index);
        }
    }

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

    private void loadBuffer()
    {
        long len = -1;
        try {
            len = this.raFile.length();
            this.raFile.seek(this.wrapOrigin - this.prefixBuffer.length);
            this.raFile.readFully(this.wraparoundBuffer, 0,
                Math.min(this.wraparoundBuffer.length,
                this.length - this.wrapOrigin));
        } 
        
        catch (IOException e) {
            // TODO convert this to a runtime error?
            DevUtils.logger.log (
                Level.SEVERE,
                "raFile.seek(" + (this.wrapOrigin - this.prefixBuffer.length) +
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
     *
     */
    private void advanceBuffer() {
        try {
            this.wraparoundBuffer[this.wrapOffset] = (byte)this.raFile.read();
            this.wrapOffset++;
            this.wrapOffset %= this.wraparoundBuffer.length;
            this.wrapOrigin++;
        } catch (IOException e) {
            // TODO convert this to a runtime error?
            DevUtils.logger.log(Level.SEVERE, "advanceBuffer()" +
                DevUtils.extraInfo(), e);
        }
    }

    /* (non-Javadoc)
     * @see java.lang.CharSequence#subSequence(int, int)
     */
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
