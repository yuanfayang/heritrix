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
     * The stream we're recording.
     */
	protected InputStream wrappedStream;
    
    /**
     * Where we are recording to.
     */
	protected RecordingOutputStream recordingOutputStream;


	/**
	 * Create a new RecordingInputStream.
	 * 
	 * @param bufferSize Size of buffer to use.
	 * @param backingFilename Name of backing file.
	 * @param maxSize Maximum amount to record.
	 */
	public RecordingInputStream(int bufferSize, String backingFilename,
            int maxSize)
    {
		recordingOutputStream = new RecordingOutputStream(bufferSize,
            backingFilename, maxSize);
	}

	public void open(InputStream wrappedStream) throws IOException {
		this.wrappedStream = wrappedStream;
		recordingOutputStream.open(); 
	}

	/* (non-Javadoc)
	 * @see java.io.InputStream#read()
	 */
	public int read() throws IOException {
		int b = wrappedStream.read();
		if (b != -1) {
			recordingOutputStream.write(b);
		}
//		checkLimits();
		return b;
	}
	
	/* (non-Javadoc)
	 * @see java.io.InputStream#read(byte[], int, int)
	 */
	public int read(byte[] b, int off, int len) throws IOException {
		int count = wrappedStream.read(b,off,len);
		if (count>0) {
			recordingOutputStream.write(b,off,count);
		} 
		return count;
	}

	/* (non-Javadoc)
	 * @see java.io.InputStream#read(byte[])
	 */
	public int read(byte[] b) throws IOException {
		int count = wrappedStream.read(b);
		if (count>0) {
			recordingOutputStream.write(b,0,count);
		} 
		return count;
	}
	
	/* (non-Javadoc)
	 * @see java.io.OutputStream#close()
	 */
	public void close() throws IOException {
		super.close();
		wrappedStream.close();
		recordingOutputStream.close();
	}
	
	public ReplayInputStream getReplayInputStream() throws IOException {
		return recordingOutputStream.getReplayInputStream();
	}

	public ReplayInputStream getContentReplayInputStream() throws IOException {
		return recordingOutputStream.getContentReplayInputStream();
	}

	public long readFully() throws IOException {
		byte[] buf = new byte[4096];
		while(read(buf)!=-1) {
		}
		return recordingOutputStream.getSize();
	}

//	/**
//	 * @param maxLength
//	 * @param timeout
//	 */
//	public void setLimits(long maxLength, long timeout) {
//		this.maxLength = maxLength;
//		this.timeout = timeout;
//	}
	
	/**
	 * @param maxLength
	 * @param timeout
	 * @throws IOException
	 */
	public void readFullyOrUntil(long maxLength, long timeout) throws IOException {
		long timeoutTime;
		long totalBytes = 0;
		if(timeout>0) {
			timeoutTime = System.currentTimeMillis() + timeout;
		} else {
			timeoutTime = Long.MAX_VALUE;
		}
		byte[] buf = new byte[4096];
		long bytesRead;
		while(true) {
			try {
				bytesRead = read(buf);
				if (bytesRead==-1) {
					break;
				}
				totalBytes += bytesRead;
			} catch (SocketTimeoutException e) {
				// DO NOTHING; just want to check overall timeout
			}
			if (totalBytes>maxLength) {
				throw new RecorderLengthExceededException();
			}
			if (System.currentTimeMillis() > timeoutTime) {
				throw new RecorderTimeoutException();
			}
		}
	}

	public long getSize() {
		// TODO Auto-generated method stub
		return recordingOutputStream.getSize();
	}

	public void markContentBegin() {
		recordingOutputStream.markContentBegin();
	}
    
    public void startDigest() {
        recordingOutputStream.startDigest();
    }

    /**
     * Convenience method for setting SHA1 digest. 
     */
    public void setSha1Digest() {
        recordingOutputStream.setSha1Digest();
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
        recordingOutputStream.setDigest(md);
    }

    
    /**
     * Return the digest value for any recorded, digested data. Call
     * only after all data has been recorded; otherwise, the running
     * digest state is ruined.  
     * 
     * @return the digest final value
     */
    public byte[] getDigestValue() {
        return recordingOutputStream.getDigestValue();
    }
    
	public CharSequence getCharSequence() {
		return recordingOutputStream.getReplayCharSequence();
	}

	public long getResponseContentLength() {
		return recordingOutputStream.getResponseContentLength();
	}

	public void closeRecorder() throws IOException {
		recordingOutputStream.closeRecorder();
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
	 * 
	 */
	public void verify() {
		recordingOutputStream.verify();
	}


}
