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
 * 
 * ToDo : Support mark and reset.
 *
 */
public class SpreadInputStream extends SeekableInputStream {

  /**
   * The manager to which the associated virtual buffer corresponds to.
   */
  private MemPoolManager mMgr;
  
  /**
   * The virtual buffer for which this stream is working for.
   */
  private DiskedVirtualBuffer mDBuff;
  
  /**
   * The index of the next byte to read from the virtual buffer.
   * This value would not be larger than the size of the 
   * virtual buffer.
   */
  private long mPosition;
  
  /**
   * The random access file opened on the temporary file to provide
   * the seek/rewind functionalities.
   */
  private RandomAccessFile mRAccessFile;
  

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
    if (mPosition >= mDBuff.getSize()) return -1;
    
    if ( ! mDBuff.isAugmented() ) {
      if (mPosition < mDBuff.mMemArea.getLength()) {
        return mDBuff.mMemArea.read((int)mPosition++);
      }
    } else {
      if (mPosition < mDBuff.mAugmentedDataArray.length) {
        return mDBuff.mAugmentedDataArray[(int)mPosition++] & 0xff;
      }
    }
    if (mDBuff.mSpillFile != null) {
      int byteRead = readFromSpillFile();
      if (byteRead != -1) mPosition++;
      return byteRead;
    }
    return -1;
  }

  /**
   * Reads from the memory area/augmented memory or the temporary file
   * depending on the current position.
   */
  public synchronized int read(byte b[], int off, int len) throws IOException {
    if (b == null) {
        throw new NullPointerException();
    } else if ((off < 0) || (off > b.length) || (len < 0) ||
       ((off + len) > b.length) || ((off + len) < 0)) {
      throw new IndexOutOfBoundsException();
    }
    if (mPosition >= mDBuff.getSize()) return -1;
    int lengthRead = 0;
    if ( ! mDBuff.isAugmented() ) {
      if (mPosition < mDBuff.mMemArea.getLength()) {
        lengthRead = mDBuff.mMemArea.read((int)mPosition, b, off, len);
        off += lengthRead;
        len -= lengthRead;
      }
    } else {
      if (mPosition < mDBuff.mAugmentedDataArray.length) {
        int augReadLength = 
          Math.min(mDBuff.mAugmentedDataArray.length - (int)mPosition, len);
        if (augReadLength > 0) {
          System.arraycopy(mDBuff.mAugmentedDataArray,
              (int)mPosition, b, off, augReadLength);
          off += augReadLength;
          len -= augReadLength;
          lengthRead = augReadLength;
        }
      }
    }
    mPosition += lengthRead;
    if (len > 0) { // more to read ..
      if (mDBuff.mSpillFile != null) {
        int bytesRead = readFromSpillFile(b, off, len);
        lengthRead += bytesRead;
        mPosition += bytesRead;
      }
    }
    return lengthRead;
  }
  
  private int getMemLength() {
    return (int)( mDBuff.isAugmented() ?
      mDBuff.mAugmentedDataArray.length :
      mDBuff.mMemArea.getLength() );
  }
  
  /**
   * Reads len bytes from the spilled file into the given 
   * byte array at the given offset.
   * Assumes that it is called from a synchronized method.
   */
  private int readFromSpillFile(byte[] b, int off, int len) 
        throws IOException {
    if (mDBuff.mSpillFile == null) // assert fail..
      throw new RuntimeException("Invalid call. No spill file exists.");
    if (mPosition >= mDBuff.getSize()) return -1;
    long filePosition = mPosition - getMemLength();
    ensureSpillFileOpen();
    mRAccessFile.seek(filePosition);
    return mRAccessFile.read(b, off, len);
  }

  private int readFromSpillFile() throws IOException {
    if (mDBuff.mSpillFile == null) // assert fail..
      throw new RuntimeException("Invalid call. No spill file exists.");
    if (mPosition >= mDBuff.getSize()) return -1;
    long filePosition = mPosition - getMemLength();
    ensureSpillFileOpen();
    mRAccessFile.seek(filePosition);
    return mRAccessFile.read();
  }

  private void ensureSpillFileOpen() throws IOException {
    if (mRAccessFile == null) {
      mRAccessFile = new RandomAccessFile(mDBuff.mSpillFile, "r");
    }
  }

  public synchronized void seek(long loc) throws IOException {
    mPosition = loc;
  }
  
  public long getPosition() {
    return mPosition;
  }
  
  public int available() throws IOException {
    return (int)(mDBuff.getSize() - mPosition);
  }
  
  /**
   * Closes the temporary file if it had been created.
   */
  public void close() throws IOException {
    if (mRAccessFile != null)
      mRAccessFile.close();
  }

}
