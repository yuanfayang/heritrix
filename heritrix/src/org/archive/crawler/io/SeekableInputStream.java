/* 
 * SeekableInputStream.java
 * Created on Apr 22, 2003
 *
 * $Header$
 */
package org.archive.crawler.io;

import java.io.InputStream;

/**
 * 
 * @author Gordon Mohr
 */
public abstract class SeekableInputStream extends InputStream {
	public abstract void seek(long loc);
	public abstract long getPosition();
	public void rewind() {
		seek(0);
	}
	public SeekableInputSubstream excerpt(long s, long e) {
		return new SeekableInputSubstream(this, s, e);
	}
}
