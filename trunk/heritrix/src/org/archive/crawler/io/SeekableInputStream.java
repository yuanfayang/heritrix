/* 
 * SeekableInputStream.java
 * Created on Apr 22, 2003
 *
 * $Header$
 */
package org.archive.crawler.io;

import java.io.InputStream;
import java.io.IOException;

/**
 * An InputStream that is completely RandomAccess, as might be
 * the case if it is backed by a VirtualBuffer.
 * 
 * @author Gordon Mohr
 */
public abstract class SeekableInputStream extends InputStream {
  public abstract void seek(long loc) throws IOException;
  public abstract long getPosition();
  public void rewind() throws IOException {
    seek(0);
  }
  public SeekableInputSubstream excerpt(long s, long e) throws IOException {
    return new SeekableInputSubstream(this, s, e);
  }
}
