/* 
 * ReplayCharSequence.java
 * Created on Sep 30, 2003
 *
 * $Header$
 */
package org.archive.io;

import java.io.IOException;
import java.io.RandomAccessFile;

import org.archive.crawler.io.CharSubSequence;


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
 * When rereading of a location is necessary, the whole window is 
 * recentered around the location requested. (TODO: More research
 * into whether this is the best strategy.)
 * 
 * TODO determine in memory mapped files is better way to do this;
 * probably not -- they don't offer the level of control over 
 * total memory used that this approach does. 
 * 
 * @author Gordon Mohr
 */
public class ReplayCharSequence implements CharSequence {
	protected byte[] prefixBuffer;
	protected long size;
	protected long responseBodyStart; // where the response body starts, if marked
	
	protected byte[] wraparoundBuffer;
	protected int wrapOrigin; // index in underlying bytestream where wraparound buffer starts
	protected int wrapOffset; // index in wraparoundBuffer that corresponds to wrapOrigin

	protected String backingFilename;
	protected RandomAccessFile raFile;

	/**
	 * @param buffer
	 * @param size
	 * @param responseBodyStart
	 * @param backingFilename
	 */
	public ReplayCharSequence(byte[] buffer, long size, long responseBodyStart, String backingFilename) throws IOException {
		this(buffer,size,backingFilename);
		this.responseBodyStart = responseBodyStart;
	}

	/**
	 * @param buffer
	 * @param size
	 * @param backingFilename
	 */
	public ReplayCharSequence(byte[] buffer, long size, String backingFilename) throws IOException {
		this.prefixBuffer = buffer;
		this.size = size;
		if (size>buffer.length) {
			this.backingFilename = backingFilename;
			raFile = new RandomAccessFile(backingFilename,"r");
			wraparoundBuffer = new byte[buffer.length];
			wrapOrigin = prefixBuffer.length;
			wrapOffset = 0;
			loadBuffer();
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.CharSequence#length()
	 */
	public int length() {
		return (int) size;
	}

	/* (non-Javadoc)
	 * @see java.lang.CharSequence#charAt(int)
	 */
	public char charAt(int index) {
//		if(index>size) {
//			throw new IndexOutOfBoundsException();
//		}
		if(index < prefixBuffer.length) {
			return (char) prefixBuffer[index];
		}
		if(index >= wrapOrigin && index-wrapOrigin < wraparoundBuffer.length) {
			return (char) wraparoundBuffer[(index-wrapOrigin+wrapOffset) % wraparoundBuffer.length];
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
	 * @param index
	 * @return
	 */
	private char faultCharAt(int index) {
		if(index>=wrapOrigin+wraparoundBuffer.length) {
			// moving forward 
			while (index>=wrapOrigin+wraparoundBuffer.length){
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
		System.out.println("recentering around "+index+" in "+ backingFilename);
		wrapOrigin = index - (wraparoundBuffer.length/2);
		if(wrapOrigin<prefixBuffer.length) {
			wrapOrigin = prefixBuffer.length;
		}
		wrapOffset = 0;
		loadBuffer();
	}
	
	private void loadBuffer() {
		try {
			raFile.seek(wrapOrigin-prefixBuffer.length);
			raFile.readFully(wraparoundBuffer,0,(int)Math.min(wraparoundBuffer.length, size-wrapOrigin ));
		} catch (IOException e) {
			// TODO convert this to a runtime error?
			e.printStackTrace();
		}
	}

	/**
	 * Roll the wraparound buffer forward one position
	 * 
	 */
	private void advanceBuffer() {
		try {
			wraparoundBuffer[wrapOffset] = (byte)raFile.read();
			wrapOffset++;
			wrapOffset %= wraparoundBuffer.length;
			wrapOrigin++;
		} catch (IOException e) {
			// TODO convert this to a runtime error?
			e.printStackTrace();
		}
		
	}

	/* (non-Javadoc)
	 * @see java.lang.CharSequence#subSequence(int, int)
	 */
	public CharSequence subSequence(int start, int end) {
		return new CharSubSequence(this,start,end);
	}
	
	
//	/* (non-Javadoc)
//	 * @see java.lang.Object#toString()
//	 */
//	public String toString() {
//		StringBuffer sb = new StringBuffer((int)size);
//		for(int i=0; i<size; i++) {
//			sb.append(charAt(i));
//		}
//		return sb.toString();
//	}

}
