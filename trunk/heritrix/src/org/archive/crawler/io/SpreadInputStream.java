// Copyright (c) 2002 G.B.Reddy (reddy@isofttech.com)
/**
 * $Id$
*/

package org.archive.crawler.io;

import java.util.*;
import java.io.*;

/**
 * This is the counterpart of the SpreadOutputStream. The in-memory 
 * data store and a disk based data store used by the SpreadOutputStream
 * are passed onto this stream for use through the virtual buffer.
 * 
 * A random access file is used to provide the seek/rewind capabilities.
 */
public class SpreadInputStream extends SeekableInputStream {

  /**
   * The manager to which the associated virtual buffer corresponds to.
   */
  private MemPoolManager mMgr;
  
  /**
   * The virtual buffer to which this stream is working for.
   * 
   */
  private DiskedVirtualBuffer mDBuff;
  
  /**
   * To track the current read position.
   */
  private int mPosition;
  
  /**
   * The random access file opened on the temporary file to provide
   * the seek/rewind functionalities.
   */
  private RandomAccessFile mRAccessfile;
  

  /**
   * Constructs an input stream for the given virtual buffer.
   * The virtual buffers memory area, augmented memory and the temp
   * file would be used accordingly to provide the seekable input
   * stream capabilities.
   */
  SpreadInputStream(MemPoolManager mgr, DiskedVirtualBuffer dBuff) {
    mMgr = mgr;
    mDBuff = dBuff;
  }
  
  /**
   * Reads from the memory area/augmented memory or the temporary file
   * depending on the current position.
   */
  public synchronized int read() throws IOException {
  
  }
  
  public synchronized int read(byte b[], int off, int len) throws IOException {
  
  }

  public synchronized void seek(long loc) throws IOException {
  
  }
  
  public long getPosition() {
    return mPosition;
  }
  
  public int available() throws IOException {
  
  }
  
  /**
   * Closes the temporary file if it had been created.
   */
  public void close() throws IOException {
    if (mRAccessfile != null)
      mRAccessfile.close();
  }

}
