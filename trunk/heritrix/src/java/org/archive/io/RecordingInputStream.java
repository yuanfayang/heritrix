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
import java.util.logging.Level;
import java.util.logging.Logger;


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
    extends InputStream {

    protected static Logger logger =
        Logger.getLogger("org.archive.io.RecordingInputStream");

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
        logger.fine(Thread.currentThread().getName() + " opening " +
            wrappedStream + ", " + Thread.currentThread().getName());
        assert this.in == null: "Inputstream is not null: " +
            Thread.currentThread().getName();
        this.in = wrappedStream;
        this.recordingOutputStream.open();
    }

    public int read() throws IOException {
        if (!isOpen()) {
            throw new IOException("Stream closed " +
                Thread.currentThread().getName());
        }
        int b = this.in.read();
        if (b != -1) {
            assert this.recordingOutputStream != null: "ROS is null " +
                Thread.currentThread().getName();
            this.recordingOutputStream.write(b);
        }
        return b;
    }

    public int read(byte[] b, int off, int len) throws IOException {
        if (!isOpen()) {
            throw new IOException("Stream closed " +
                Thread.currentThread().getName());
        }
        int count = this.in.read(b,off,len);
        if (count > 0) {
            assert this.recordingOutputStream != null: "ROS is null " +
                Thread.currentThread().getName();
            this.recordingOutputStream.write(b,off,count);
        }
        return count;
    }

    public int read(byte[] b) throws IOException {
    	    if (!isOpen()) {
    	    	    throw new IOException("Stream closed " +
    			    Thread.currentThread().getName());
    	    }
    	    int count = this.in.read(b);
        if (count > 0) {
            assert this.recordingOutputStream != null: "ROS is null " +
                Thread.currentThread().getName();
            this.recordingOutputStream.write(b,0,count);
        }
        return count;
    }

    public void close() throws IOException {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(Thread.currentThread().getName() + " closing " +
                    this.in + ", " + Thread.currentThread().getName());
        }
        if (this.in != null) {
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
            // Empty out stream.
            continue;
        }
        return this.recordingOutputStream.getSize();
    }

    /**
     * Read all of a stream (Or read until we timeout or have read to the max).
     * @param maxLength Maximum length to read. If zero or < 0, then no limit.
     * @param timeout Timeout in milliseconds for total read.  If zero or
     * negative, timeout is <code>Long.MAX_VALUE</code>.
     * @throws IOException failed read.
     * @throws RecorderLengthExceededException
     * @throws RecorderTimeoutException
     * @throws InterruptedException
     */
    public void readFullyOrUntil(long maxLength, long timeout)
        throws IOException, RecorderLengthExceededException,
            RecorderTimeoutException, InterruptedException
    {
        // Check we're open before proceeding.
        if (!isOpen()) {
            return;
        }

        long timeoutTime;
        long totalBytes = 0;
        long startTime = System.currentTimeMillis();

        if(timeout > 0) {
            timeoutTime = startTime + timeout;
        } else {
            timeoutTime = Long.MAX_VALUE;
        }

        final int BUFFER_SIZE = 4096;
        int buffersize = (maxLength > 0)?
            Math.min(BUFFER_SIZE, (int)maxLength): BUFFER_SIZE;
        byte[] buf = new byte[buffersize];
        long bytesRead = -1;
        while (true) {
            try {
                bytesRead = read(buf);
                if (bytesRead == -1) {
                    break;
                }
                totalBytes += bytesRead;
                if(Thread.interrupted()) {
                    throw new InterruptedException("interrupted during IO");
                }
            } catch (SocketTimeoutException e) {
                // FIXME: I don't think an IOException should be thrown here;
                // it will cause the entire fetch to be aborted, as a connection
                // lost (up in FetchHTTP). A socket timeout is just a transient
                // problem, meaning nothing was available in the configured 
                // timeout period, but something else might become available
                // later. Unless we are constantly resetting the socket timeout
                // to reflect the 'time remaining', the socket timeout should 
                // be some constant interval at which we check the overall 
                // timeout (below). Any given socket timeout is not a fatal
                // problem, just a reminder to check the overall timeout and
                // continue. (The below was added in rev 1.7, 20040218, perhaps
                // to assist in debugging another problem.)
                
                // Socket timed out. If we  haven't exceeded the timeout, throw
                // an IOException.  If we have, it'll be picked up on by test
                // done below.
                if (System.currentTimeMillis() < timeoutTime) {
                    throw new IOException("Socket timed out after " +
                        (timeoutTime - startTime) + "ms: " + e.getMessage());
                }
            } catch (NullPointerException e) {
                // [ 896757 ] NPEs in Andy's Th-Fri Crawl.
                // A crawl was showing NPE's in this part of the code but can
                // not reproduce.  Adding this rethrowing catch block w/
                // diagnostics to help should we come across the problem in the
                // future.
                throw new NullPointerException("Stream " + this.in + ", " +
                    e.getMessage() + " " + Thread.currentThread().getName());
            }

            if (maxLength > 0 && totalBytes >= maxLength) {
                throw new RecorderLengthExceededException();
            }
            if (System.currentTimeMillis() >= timeoutTime) {
                throw new RecorderTimeoutException("Timedout after " +
                    (timeoutTime - startTime) + "ms.");
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

    public ReplayCharSequence getReplayCharSequence() throws IOException {
        return getReplayCharSequence(null);
    }

    /**
     * @param characterEncoding Encoding of recorded stream.
     * @return A ReplayCharSequence  Will return null if an IOException.  Call
     * close on returned RCS when done.
     * @throws IOException
     */
    public ReplayCharSequence getReplayCharSequence(String characterEncoding)
    		throws IOException {
        return this.recordingOutputStream.
            getReplayCharSequence(characterEncoding);
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
