/* ReplayableOutputStream
 * 
 * $Id$
 * 
 * Created on Sep 23, 2003
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

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


/**
 * An output stream that records all writes to wrapped output
 * stream.
 * 
 * A RecordingOutputStream can be wrapped around any other
 * OutputStream to record all bytes written to it.  You can
 * then request a ReplayInputStream to read those bytes.
 * 
 * <p>The RecordingOutputStream uses an in-memory buffer and 
 * backing disk file to allow it to record streams of 
 * arbitrary length limited only by available disk space. 
 * 
 * <p>As long as the stream recorded is smaller than the 
 * in-memory buffer, no disk access will occur. 
 * 
 * <p>Recorded content can be recovered as a ReplayInputStream
 * (via getReplayInputStream() or, for only the content after
 * the content-begin-mark is set, getContentReplayInputStream() )
 * or as a ReplayCharSequence (via getReplayCharSequence()). 
 * 
 * <p>This class is also used as a straight output stream 
 * by {@link RecordingInputStream} to which it records all reads. 
 * {@link RecordingInputStream} is exploiting the file backed buffer
 * facility of this class passing <code>null</code> for the stream
 * to wrap.  TODO: Make a FileBackedOutputStream class that is
 * subclassed by RecordingInputStream.
 * 
 * @author gojomo
 *
 */
public class RecordingOutputStream extends OutputStream {
    
    /**
     * Size of recording.
     * 
     * Later passed to ReplayInputStream on creation.  It uses it to know when
     * EOS.
     */
	private long size = 0;
    
    private String backingFilename;
    private BufferedOutputStream diskStream;
    private byte[] buffer;
    private long position;
    private boolean shouldDigest = false;
    private MessageDigest digest; 
	
    /**
     * When recording HTTP, where the content-body starts.
     */
    private long contentBeginMark;
    
    /**
     * Stream to record.
     */
    private OutputStream out = null;
    
	
    /**
	 * Create a new RecordingOutputStream.
	 * 
	 * @param bufferSize Buffer size to use.
	 * @param backingFilename Name of backing file to use.
	 */
	public RecordingOutputStream(int bufferSize, String backingFilename)
    {
		this.buffer = new byte[bufferSize];
		this.backingFilename = backingFilename;
	}

    /**
     * Wrap the given stream, both recording and passing along any data written
     * to this RecordingOutputStream.
     * 
     * @throws IOException If failed creation of backing file.
     */
    public void open()
        throws IOException
    {
        this.open(null);
    }
    
	/**
     * Wrap the given stream, both recording and passing along any data written
     * to this RecordingOutputStream.
     * 
	 * @param wrappedStream Stream to wrap.  May be null for case where we 
     * want to write to a file backed stream only.
     * 
	 * @throws IOException If failed creation of backing file.
	 */
	public void open(OutputStream wrappedStream)
        throws IOException
   {
        assert this.out == null : "RecordingOutputStream still has old 'out' stream";
		this.out = wrappedStream;
		this.position = 0;
		this.size = 0;
		// Always begins false; must use startDigest() to begin
        this.shouldDigest = false;
        this.diskStream = null; 
		lateOpen();
	}


	private void lateOpen()
        throws FileNotFoundException
    {
        // TODO: Fix so we only make file when its actually needed.
		if (diskStream == null) {
			FileOutputStream fis = new FileOutputStream(backingFilename);
			this.diskStream = new BufferedOutputStream(fis, 4096);
		}
	}
	
	/* (non-Javadoc)
	 * @see java.io.OutputStream#write(int)
	 */
	public void write(int b) throws IOException {
		record(b);
        if (this.out != null)
        {
            this.out.write(b);
        }
	}
	
	/* (non-Javadoc)
	 * @see java.io.OutputStream#write(byte[])
	 */
	public void write(byte[] b) throws IOException {
		record(b, 0, b.length);
        if (this.out != null)
        {
            this.out.write(b);
        }
	}
	
	/* (non-Javadoc)
	 * @see java.io.OutputStream#write(byte[], int, int)
	 */
	public void write(byte[] b, int off, int len) throws IOException {
		record(b, off, len);
        if (this.out != null)
        {
            this.out.write(b, off, len);
        }
	}
	
	/** 
     * Record the given byte for later recovery
     * 
     * @param b Int to record.
     * 
     * @exception IOException Failed write to backing file.
     */
    private void record(int b) throws IOException {
        if (shouldDigest) {
            digest.update((byte)b);
        }
        if (position >= buffer.length) {
            // lateOpen()
            // TODO: Its possible to call write w/o having first opened a
            // stream.  Protect ourselves against this.
            diskStream.write(b);
        } else {
            buffer[(int) position] = (byte) b;
        }
        position++;
    }
	
	/** 
     * Record the given byte-array range for recovery later
     * 
	 * @param b Buffer to record.
	 * @param off Offset into buffer at which to start recording.
	 * @param len Length of buffer to record.
     * 
     * @exception IOException Failed write to backing file.
	 */
	private void record(byte[] b, int off, int len) throws IOException {
        if(shouldDigest) {
            digest.update(b, off, len);
        }
		if(position >= buffer.length){
            // lateOpen()
            // TODO: Its possible to call write w/o having first opened a
            // stream.  Lets protect ourselves against this.
			diskStream.write(b, off, len);
			position += len;
		} else {
			int toCopy = (int)Math.min(buffer.length - position, len);
			System.arraycopy(b, off, buffer, (int)position, toCopy);
			position += toCopy;
			// TODO verify these are +1 -1 right
			if (toCopy < len) {
				record(b, off + toCopy, len - toCopy);
			}
		}
	}

	/* (non-Javadoc)
	 * @see java.io.OutputStream#close()
	 */
	public void close()
        throws IOException
    {
		if (this.out != null)
        {
		    this.out.close();
            this.out = null;
        }
        closeRecorder();
	}
    
    public void closeRecorder()
        throws IOException
    {
        if (diskStream != null) {
            diskStream.close();
            diskStream = null;
        }
        
        // This setting of size is important.  Its passed to ReplayInputStream
        // on creation.  It uses it to know EOS.
        if (size == 0)
        {
            size = position;
        }
    }

	/* (non-Javadoc)
	 * @see java.io.OutputStream#flush()
	 */
	public void flush() throws IOException {
        if (this.out != null)
        {
            this.out.flush();
        }
		if (diskStream != null) {
			diskStream.flush();
		}
	}
	
	public ReplayInputStream getReplayInputStream() throws IOException {
        // If this method is being called, then assumption must be that the 
        // stream is closed (If it ain't, then the stream gotten won't work
        // -- the size will zero so any attempt at a read will get back EOF.
        assert this.out == null: "Stream is still open.";
		return new ReplayInputStream(buffer, size, contentBeginMark,
            backingFilename);
	}

	/**
	 * Return a replay stream, cued up to beginning of content
     * 
	 * @throws IOException
	 * @return An RIS.
	 */
	public ReplayInputStream getContentReplayInputStream() throws IOException {
		ReplayInputStream replay = getReplayInputStream();
		replay.skip(contentBeginMark);
		return replay;
	}

	public long getSize() {
		return size;
	}

	/**
	 * Remember the current position as the start of the "response
     * body". Useful when recording HTTP traffic as a way to start
     * replays after the headers. 
	 */
	public void markContentBegin() {
		contentBeginMark = position;
	}
    
    /**
     * Starts digesting recorded data, if a MessageDigest has been
     * set. 
     */
    public void startDigest() {
        if (digest != null) {
            digest.reset();
            shouldDigest = true;
        }
    }
    
    /**
     * Convenience method for setting SHA1 digest. 
     */
    public void setSha1Digest() {
        try {
            setDigest(MessageDigest.getInstance("SHA1"));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Sets a digest function which may be applied to recorded data.
     * 
     * As usually only a subset of the recorded data should
     * be fed to the digest, you must also call startDigest()
     * to begin digesting. 
     * 
     * @param md Message digest function to use.
     */
    public void setDigest(MessageDigest md) {
        digest = md;
    }

    /**
     * Return the digest value for any recorded, digested data. Call
     * only after all data has been recorded; otherwise, the running
     * digest state is ruined.  
     * 
     * @return the digest final value
     */
    public byte[] getDigestValue() {
        if(digest == null) {
            return null;
        }
        return digest.digest();
    }
    
	public CharSequence getReplayCharSequence() {
		try {
			return new ReplayCharSequence(buffer, size, contentBeginMark,
                backingFilename);
		} catch (IOException e) {
			// TODO convert to runtime exception?
			e.printStackTrace();
		}
		return null;
	}

	public long getResponseContentLength() {
		return size-contentBeginMark;
	}
}
