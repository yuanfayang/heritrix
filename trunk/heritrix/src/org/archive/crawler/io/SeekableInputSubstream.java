/* 
 * SeekableInputSubstream.java
 * Created on Apr 22, 2003
 *
 * $Header$
 */
package org.archive.crawler.io;

import java.io.IOException;

/**
 * A view on a SeekableInputStream constrained to a specific range.
 * 
 * @author Gordon Mohr
 */
public class SeekableInputSubstream extends SeekableInputStream {
  SeekableInputStream innerStream;
  long start;
  long end;
  
  /**
   * Create a "view" on the given stream, limited to the given range.
   * 
   * @param inner
   * @param s
   * @param e
   */
  public SeekableInputSubstream(SeekableInputStream inner, long s, long e) 
      throws IOException {
    innerStream = inner;
    start = s;
    end = e;
    inner.seek(s);
  }
  
  /* (non-Javadoc)
   * @see org.archive.crawler.framework.SeekableInputStream#seek(long)
   */
  public void seek(long loc) throws IOException {
    innerStream.seek(start+loc);
  }

  /* (non-Javadoc)
   * @see java.io.InputStream#read()
   */
  public int read() throws IOException {
    if ( innerStream.getPosition()>=end ) return -1;
    return innerStream.read();
  }

  /* (non-Javadoc)
   * @see org.archive.crawler.framework.SeekableInputStream#getPosition()
   */
  public long getPosition() {
    return innerStream.getPosition()-start;
  }

}
