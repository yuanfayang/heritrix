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
import java.net.SocketException;
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
     * Reusable buffer to avoid reallocation on each readFullyUntil
     */
    protected byte[] drainBuffer = new byte[16*1024];

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
        if(isOpen()) {
            // error; should not be opening/wrapping in an unclosed 
            // stream remains open
            throw new IOException("RIS already open for "
                    +Thread.currentThread().getName());
        }
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
        while(read(drainBuffer) != -1) {
            // Empty out stream.
            continue;
        }
        return this.recordingOutputStream.getSize();
    }

    /**
     * Read all of a stream (Or read until we timeout or have read to the max).
     * @param softMaxLength Maximum length to read; if zero or < 0, then no 
     * limit. If met, return normally. 
     * @param hardMaxLength Maximum length to read; if zero or < 0, then no 
     * limit. If exceeded, throw RecorderLengthExceededException
     * @param timeout Timeout in milliseconds for total read; if zero or
     * negative, timeout is <code>Long.MAX_VALUE</code>. If exceeded, throw
     * RecorderTimeoutException
     * @param maxBytesPerMs How many bytes per millisecond.
     * @throws IOException failed read.
     * @throws RecorderLengthExceededException
     * @throws RecorderTimeoutException
     * @throws InterruptedException
     */
    public void readFullyOrUntil(long softMaxLength, long hardMaxLength,
	long timeout, int maxBytesPerMs)
        throws IOException, RecorderLengthExceededException,
            RecorderTimeoutException, InterruptedException {
        // Check we're open before proceeding.
        if (!isOpen()) {
            // TODO: should this be a noisier exception-raising error? 
            return;
        }

        // We need the rate limit in ms per byte, so turn it over
        double minMsPerByte = 0.0;
        if (maxBytesPerMs != 0) {
            minMsPerByte = ((double)1.0)/(double)maxBytesPerMs;
        }

        long maxLength;
        if ( softMaxLength>0 && hardMaxLength>0) {
            // both maxes set; use lower of softMax or hardMax+1
            maxLength = Math.min(softMaxLength,hardMaxLength+1);
        } else if(softMaxLength>0 && hardMaxLength<=0) {
            // softMax only; set max to softMax
            maxLength = softMaxLength;
        } else if(softMaxLength<=0 && hardMaxLength>0) {
            // hardMax only; set max to read as 1 past max
            maxLength = hardMaxLength+1;
        } else { // => (softMaxLength<=0 && hardMaxLength<=0) 
            // no maxes
            maxLength = -1;
        }

        long timeoutTime;
        long startTime = System.currentTimeMillis();

        if(timeout > 0) {
            timeoutTime = startTime + timeout;
        } else {
            timeoutTime = Long.MAX_VALUE;
        }

        long totalReadingTime = 0L;
        long totalBytes = 0L;
        long bytesRead = -1L;
        int maxToRead = -1; 
        while (true) {
            try {
                maxToRead = (maxLength <= 0) 
                    ? drainBuffer.length 
                    : (int) Math.min(drainBuffer.length, maxLength - totalBytes);
                // Now, decide if (and for how long) to sleep, prior to reading,
                // in order to yield the average fetch rate desired.
                if (minMsPerByte != 0.0 && totalBytes > 0L) {
                    // We've already fetched something, so we can estimate the
                    // actual bandwidth we've been getting. This number is used
                    // to figure out how many ms we need to wait before
                    // starting the next read.  We want the stats at the end of
                    // that read to be within the specified bounds.
                    double actualMsPerByte =
                        ((double)totalReadingTime)/(double)totalBytes;
                    // Calculate the minimum time
                    long minTime =
                        (long)((totalBytes + maxToRead) * minMsPerByte);
                    // Calculate the actual time spent, plus the estimated time
                    // for the bytes to read
                    long estTime = System.currentTimeMillis() - startTime +
                        (long)(maxToRead * actualMsPerByte);
                    // If estTime <  minTime, sleep for the difference
                    if (estTime < minTime) {
                        Thread.sleep(minTime - estTime);
                    }
                }

                long readStartTime = System.currentTimeMillis();
                bytesRead = read(drainBuffer,0,maxToRead);
                if (bytesRead == -1) {
                    break;
                }
                totalBytes += bytesRead;
                totalReadingTime += System.currentTimeMillis() - readStartTime;

                if (Thread.interrupted()) {
                    throw new InterruptedException("Interrupted during IO");
                }
            } catch (SocketTimeoutException e) {
                // A socket timeout is just a transient problem, meaning
                // nothing was available in the configured  timeout period,
                // but something else might become available later.
                // Take this opportunity to check the overall 
                // timeout (below).  One reason for this timeout is 
                // servers that keep up the connection, 'keep-alive', even
                // though we asked them to not keep the connection open.
                if (System.currentTimeMillis() < timeoutTime) {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("Socket timed out after " +
                            (timeoutTime - startTime) + "ms: " +
                            e.getMessage());
                    }
                }
            } catch (SocketException se) {
                throw se;
            } catch (NullPointerException e) {
                // [ 896757 ] NPEs in Andy's Th-Fri Crawl.
                // A crawl was showing NPE's in this part of the code but can
                // not reproduce.  Adding this rethrowing catch block w/
                // diagnostics to help should we come across the problem in the
                // future.
                throw new NullPointerException("Stream " + this.in + ", " +
                    e.getMessage() + " " + Thread.currentThread().getName());
            }
            if (System.currentTimeMillis() >= timeoutTime) {
                throw new RecorderTimeoutException("Timedout after " +
                    (timeoutTime - startTime) + "ms.");
            }
            if (hardMaxLength > 0 && totalBytes >= hardMaxLength) {
                throw new RecorderLengthExceededException();
            }
            if (maxLength > 0 && totalBytes >= maxLength) {
                break; // return
            }
        }
    }

    public long getSize() {
        return this.recordingOutputStream.getSize();
    }

    public void markContentBegin() {
        this.recordingOutputStream.markContentBegin();
    }
    
    public long getContentBegin() {
        return this.recordingOutputStream.getContentBegin();
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
     * Sets a digest algorithm which may be applied to recorded data.
     * As usually only a subset of the recorded data should
     * be fed to the digest, you must also call startDigest()
     * to begin digesting.
     *
     * @param algorithm
     */
    public void setDigest(String algorithm) {
        this.recordingOutputStream.setDigest(algorithm);
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
