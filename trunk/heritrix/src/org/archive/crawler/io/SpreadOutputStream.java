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
 * in the global data pool, a local augmented memory is created and used.
 * 
 * The user should invoke the close method once he is done with writing.
 * This would enable the stream to callback its owner virtual buffer and
 * submit the relevent info for it to allow read operations.
 */

public class SpreadOutputStream extends OutputStream {

  /**
   * The manager to which the associated virtual buffer corresponds to.
   */
  private MemPoolManager mgr;
  
  /**
   * The virtual buffer to which this stream is working for.
   */
  private DiskedVirtualBuffer dBuff;

  /**
   * The memory area used by this stream. This could be null
   * if the global data pool was full.
   */
  private MemoryArea memArea;
  
  /**
   * The local memory used in case the global data pool is full.
   */
  private byte[] augmentedDataArray;
  
  /**
   * Used to track the current position in the augmented array.
   */
  private int augmentLength;

  /**
   * The temporary file used to write the excess data.
   */  
  private File file;

  /**
   * The file stream associated with the temporary file.
   */
  private FileOutputStream fileStream;
  
  
  /**
   * Constructs an output stream for the given virtual buffer.
   * Tries to get free memory area from the data pool of the manager
   * else uses the local augmented memory.
   */
  SpreadOutputStream(MemPoolManager mgr, DiskedVirtualBuffer dBuff) {
    this.mgr = mgr;
    this.dBuff = dBuff;
    LinkedList allocBlocks = mgr.allocateBlocks(1);
    if (allocBlocks != null) {
      memArea = new MemoryArea(mgr, allocBlocks);
    }
    else {
      /* ToDo: This local memory allocation could also fail.
      So, we might have to switch to disk IO straight-away. */
      augmentedDataArray = new byte[dBuff.MAX_AUGMENT_SIZE];
    }
  }
 
  
  /**
   * Writes into the allocated memory area or the local augmented memory.
   * If the memory area or the augmented memory is exhausted, it would start
   * writing into the file stream.
   * Uses the MemoryArea's write calls to write into the manager's data pool.
   * Augmented memory writes and its positions are tracked locally.
   */
  public synchronized void write(byte b[], int off, int len) throws IOException {
  
  }

  public synchronized void write(int b) throws IOException {
    
  }

  /**
   * Flushes the underlying file output stream. This has no 
   * effect if the data is in the global data pool only. But the user 
   * has to invoke this method since he is unaware of whether a temporary
   * file is used or not.
   */
  public void flush() throws IOException {
    if (fileStream != null)
      fileStream.flush();
  }

  /**
   * Closes the temporary file stream if it had been created.
   * This method does not release the memory area so that it 
   * allows it to be used by the input stream.
   */
  public void close() throws IOException {
    flush();
    long length;
    if (memArea != null) 
      length = memArea.getLength();
    else if (augmentedDataArray != null)
      length = augmentLength;
    if (fileStream != null) {
      fileStream.close();
      length += file.length();
    }
    dBuff.writeFinishCallBack(memArea, file, 
        augmentedDataArray, length);
  }

}

