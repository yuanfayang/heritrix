// Copyright (c) 2002 G.B.Reddy (reddy@isofttech.com)
/**
 * $Id$
*/

package org.archive.crawler.io;

import java.util.*;
import java.io.*;

/**
 * This is a stream which has two types of data stores, viz, in-memory 
 * data store and a disk based data store. The disk based data store comes
 * into picture if the in-memory data store max size is reached.
 * 
 * When the in-memory data store gets filled completely, the whole in-memory 
 * data is written to disk and a large portion of the in-memory store is 
 * released to the manager's data pool. A small portion (8k) of it is 
 * retained and would be used as a buffer for the file IO. Issue needs to be
 * clarified on whether this 8K be contiguous.
 * 
 * The in-memory data store is a MemoryArea instance. If the MemoryArea 
 * instance could not be created because of non-availability of free space 
 * in the manager's data pool, a local augmented memory is created and used.
 * 
 * The user should invoke the close method once he is done with writing.
 * This would enable the stream to callback its owner virtual buffer and
 * submit the relevent info for it to allow read operations.
 */

public class SpreadOutputStream extends OutputStream {

  /**
   * The manager to which the associated virtual buffer corresponds to.
   */
  private MemPoolManager mMgr;
  
  /**
   * The virtual buffer to which this stream is working for.
   */
  private DiskedVirtualBuffer mDBuff;

  /**
   * The memory area used by this stream. This could be null
   * if the manager's data pool was full.
   */
  private MemoryArea mMemArea;
  
  /**
   * The local memory used in case the manager's data pool is full.
   * This will be null if mMemArea is non-null which means that the
   * manager's data pool had room for creation of MemoryArea.
   */
  private byte[] mAugmentedDataArray;
  
  /**
   * Used to track the number of bytes written into the augmented array.
   */
  private int mAugmentLength;

  /**
   * The temporary file used to write the excess data.
   */
  private File mFile;

  /**
   * The file stream associated with the temporary file.
   */
  private FileOutputStream mFileStream;
  
  /**
   * Used as a name generator for the spilled files.
   */
  private static int spillFileNameGen;
  
  /**
   * Constructs an output stream for the given virtual buffer.
   * Tries to get free memory area from the data pool of the manager
   * else uses the local augmented memory.
   */
  SpreadOutputStream(MemPoolManager mgr, DiskedVirtualBuffer dBuff) {
    mMgr = mgr;
    mDBuff = dBuff;
    LinkedList allocBlocks = mMgr.allocateBlocks(1);
    if (allocBlocks != null) {
      mMemArea = new MemoryArea(mMgr, allocBlocks);
    }
    else {
      /* ToDo: This local memory allocation could also fail.
      So, we might have to switch to disk IO straight-away. */
      mAugmentedDataArray = new byte[DiskedVirtualBuffer.MAX_AUGMENT_SIZE];
    }
  }
 
  private boolean isAugmented() {
    return ( mAugmentedDataArray != null );
  }
  
  /**
   * Writes into the allocated memory area or the local augmented memory.
   * If the memory area or the augmented memory is exhausted, it would start
   * writing into the file stream.
   * Uses the MemoryArea's write calls to write into the manager's data pool.
   * Augmented memory writes and its positions are tracked locally.
   */
  public synchronized void write(byte b[], int off, int len) throws IOException {
    if ((off < 0) || (off > b.length) || (len < 0) ||
              ((off + len) > b.length) || ((off + len) < 0)) {
        throw new IndexOutOfBoundsException();
    } else if (len == 0) {
        return;
    }
    if ( ! isAugmented() ) {
      if ( ! mMemArea.isExhausted()) {
        // write into mem area.
        int numWritten = mMemArea.write(b, off, len);
        off += numWritten;
        len -= numWritten;
      }
    } else {
      // write into augmented data array.
      int augWriteLength = 
        Math.min(mAugmentedDataArray.length - mAugmentLength, len);
      if (augWriteLength > 0) {
        System.arraycopy(b, off, mAugmentedDataArray,
            mAugmentLength, augWriteLength);
        mAugmentLength += augWriteLength;
        off += augWriteLength;
        len -= augWriteLength;
      }
    }
    if (len > 0) { // spill to file.
      if (mFile == null) {
        createSpillFile();
      }
      mFileStream.write(b, off, len);
    }
  }

  public synchronized void write(int b) throws IOException {
    if ( ! isAugmented() ) {
      if ( ! mMemArea.isExhausted()) {
        // write into mem area.
        if (mMemArea.write(b)) return;
      }
    } else {
      // write into augmented data array.
      if (mAugmentLength < mAugmentedDataArray.length) {
        mAugmentedDataArray[mAugmentLength++] = (byte)b;
        return;
      }
    }
    if (mFile == null) {
      createSpillFile();
    }
    mFileStream.write(b);
  }
  
  private void createSpillFile() throws IOException {
    spillFileNameGen++;
    mFile = File.createTempFile("Spill " + spillFileNameGen, "tmp");
    mFileStream = new FileOutputStream(mFile);
  }

  /**
   * Flushes the underlying file output stream. This has no 
   * effect if the data is in the manager's data pool alone. But the user 
   * has to invoke this method since he is unaware of whether a temporary
   * file is used or not.
   */
  public void flush() throws IOException {
    if (mFileStream != null)
      mFileStream.flush();
  }

  /**
   * Closes the temporary file stream if it had been created.
   * This method does not release the memory area so that it 
   * allows it to be used by the input stream.
   */
  public void close() throws IOException {
    flush();
    long length = 0;
    if (mMemArea != null)
      length = mMemArea.getLength();
    else if (mAugmentedDataArray != null)
      length = mAugmentLength;
    if (mFileStream != null) {
      mFileStream.close();
      length += mFile.length();
    }
    mDBuff.writeFinishCallBack(mMemArea, mFile,
        mAugmentedDataArray, length);
  }

}

