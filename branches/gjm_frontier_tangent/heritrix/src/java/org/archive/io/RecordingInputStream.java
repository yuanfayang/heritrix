/* RecordingInputStream
 *
 * $Id$
 *
 * Created on Sep 24, 2003
 *
 * Copyright (C) 2003 Internet Archive.
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.security.MessageDigest;


/**
 * Stream which records all data read from it, which it acquires from a wrapped
 * input stream.
 *
 * Makes use of a RecordingOutputStream for recording because of its being
 * file backed so we can write massive amounts of data w/o worrying about
 * overflowing memory.
 *
 * @author gojomo
 *
 */
public class RecordingInputStream
    extends InputStream
{
    /**
     * Where we are recording to.
     */
    private RecordingOutputStream recordingOutputStream;

    /**
     * Stream to record.
     */
    private InputStream in = null;


    /**
     * Create a new RecordingInputStream.
     *
     * @param bufferSize Size of buffer to use.
     * @param backingFilename Name of backing file.
     */
    public RecordingInputStream(int bufferSize, String backingFilename)
    {
        this.recordingOutputStream = new RecordingOutputStream(bufferSize,
            backingFilename);
    }

    public void open(InputStream wrappedStream) throws IOException {
        assert this.in == null;
        this.in = wrappedStream;
        this.recordingOutputStream.open();
    }

    /* (non-Javadoc)
     * @see java.io.InputStream#read()
     */
    public int read() throws IOException {
        assert this.in != null: "Inputstream is null.";
        int b = this.in.read();
        if (b != -1) {
            this.recordingOutputStream.write(b);
        }
        return b;
    }

    /* (non-Javadoc)
     * @see java.io.InputStream#read(byte[], int, int)
     */
    public int read(byte[] b, int off, int len) throws IOException {
        assert this.in != null: "Inputstream is null.";
        int count = this.in.read(b,off,len);
        if (count > 0) {
            this.recordingOutputStream.write(b,off,count);
        }
        return count;
    }

    /* (non-Javadoc)
     * @see java.io.InputStream#read(byte[])
     */
    public int read(byte[] b) throws IOException {
        assert this.in != null: "Inputstream is null.";
        int count = this.in.read(b);
        if (count > 0) {
            this.recordingOutputStream.write(b,0,count);
        }
        return count;
    }

    /* (non-Javadoc)
     * @see java.io.OutputStream#close()
     */
    public void close() throws IOException {
        if (this.in != null)
        {
            this.in.close();
            this.in = null;
        }
        this.recordingOutputStream.close();
    }

    public ReplayInputStream getReplayInputStream() throws IOException {
        return this.recordingOutputStream.getReplayInputStream();
    }

    public ReplayInputStream getContentReplayInputStream() throws IOException {
        return this.recordingOutputStream.getContentReplayInputStream();
    }

    public long readFully() throws IOException {
        byte[] buf = new byte[4096];
        while(read(buf) != -1) {
            // Empty out the stream.
        }
        return this.recordingOutputStream.getSize();
    }

    /**
     * Read all of a stream (Or read until we timeout or have read to the max).
     * @param maxLength Maximum length to read.
     * @param timeout Timeout in milliseconds.
     * @throws IOException failed read.
     * @throws RecorderLengthExceededException
     * @throws RecorderTimeoutException
     */
    public void readFullyOrUntil(long maxLength, long timeout)
        throws IOException, RecorderLengthExceededException,
            RecorderTimeoutException
    {
        long timeoutTime;
        long totalBytes = 0;
        
        if(timeout > 0) {
            timeoutTime = System.currentTimeMillis() + timeout;
        } else {
            timeoutTime = Long.MAX_VALUE;
        }
        
        byte[] buf = new byte[4096];
        long bytesRead = -1;
        while (true) {
            try {
                bytesRead = read(buf);
                if (bytesRead == -1) {
                    break;
                }
                totalBytes += bytesRead;
            } catch (SocketTimeoutException e) {
                // Socket timed out. If we  haven't exceeded the timeout, throw
                // an IOException.  If we have, it'll be picked up on by test
                // done below.
                if (System.currentTimeMillis() < timeoutTime) {
                    throw new IOException("Socket timedout: " + e.getMessage());
                }
            } catch (NullPointerException e) {
                // [ 896757 ] NPEs in Andy's Th-Fri Crawl.
                // A crawl was showing NPE's in this part of the code but can
                // not reproduce.  Adding this rethrowing catch block w/
                // diagnostics to help should we come across the problem in the
                // future.
                throw new NullPointerException("Stream " + this.in + ", " +
                    e.getMessage());
            }
            
            if (totalBytes > maxLength) {
                throw new RecorderLengthExceededException();
            }
            if (System.currentTimeMillis() > timeoutTime) {
                throw new RecorderTimeoutException();
            }
        }
    }

    public long getSize() {
        return this.recordingOutputStream.getSize();
    }

    public void markContentBegin() {
        this.recordingOutputStream.markContentBegin();
    }

    public void startDigest() {
        this.recordingOutputStream.startDigest();
    }

    /**
     * Convenience method for setting SHA1 digest.
     */
    public void setSha1Digest() {
        this.recordingOutputStream.setSha1Digest();
    }

    /**
     * Sets a digest function which may be applied to recorded data.
     * As usually only a subset of the recorded data should
     * be fed to the digest, you must also call startDigest()
     * to begin digesting.
     *
     * @param md
     */
    public void setDigest(MessageDigest md) {
        this.recordingOutputStream.setDigest(md);
    }

    /**
     * Return the digest value for any recorded, digested data. Call
     * only after all data has been recorded; otherwise, the running
     * digest state is ruined.
     *
     * @return the digest final value
     */
    public byte[] getDigestValue() {
        return this.recordingOutputStream.getDigestValue();
    }

    public CharSequence getCharSequence() {
        return this.recordingOutputStream.getReplayCharSequence();
    }

    public long getResponseContentLength() {
        return this.recordingOutputStream.getResponseContentLength();
    }

    public void closeRecorder() throws IOException {
        this.recordingOutputStream.closeRecorder();
    }

    /**
     * @param tempFile
     * @throws IOException
     */
    public void copyContentBodyTo(File tempFile) throws IOException {
        FileOutputStream fos = new FileOutputStream(tempFile);
        ReplayInputStream ris = getContentReplayInputStream();
        ris.readFullyTo(fos);
        fos.close();
        ris.close();
    }
    
    /**
     * @return True if we've been opened.
     */
    public boolean isOpen()
    {
        return this.in != null;
    }
}
