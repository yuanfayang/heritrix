/* 
 * ReplayCharSequence.java
 * Created on Sep 30, 2003
 *
 * $Header$
 */
package org.archive.crawler.io;

import java.io.BufferedInputStream;

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
 * recentered around the location requested. (??? Is this the best
 * strategy?)
 * 
 * TODO determine in memory mapped files is better way to do this
 * 
 * @author Gordon Mohr
 */
public class ReplayCharSequence implements CharSequence {
	protected BufferedInputStream diskStream;
	protected byte[] prefixBuffer;
	protected long size;
	protected long responseBodyStart; // where the response body starts, if marked
	
	protected byte[] wraparoundBuffer;
	protected long position;
	protected String backingFilename;

	/* (non-Javadoc)
	 * @see java.lang.CharSequence#length()
	 */
	public int length() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see java.lang.CharSequence#charAt(int)
	 */
	public char charAt(int index) {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see java.lang.CharSequence#subSequence(int, int)
	 */
	public CharSequence subSequence(int start, int end) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
