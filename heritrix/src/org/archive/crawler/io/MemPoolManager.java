// Copyright (c) 2002 G.B.Reddy (reddy@isofttech.com)
/**
 * $Id$
*/

package org.archive.crawler.io;

import java.util.*;
import java.io.*;

/**
 * The MemPoolManager manages a pool of memory blocks that would be
 * shared by multiple processes in the form of VirtualBuffers.
 * 
 * The data pool maintained is package private so as to allow easy 
 * access for the VirtualBuffers and its i/o streams.
 * 
 * The upper bound 4k chunks are allocated from top and the lower
 * bound 8k or above chunks are allocated from bottom. (Not yet implemented.)
 */
public class MemPoolManager {

  /**
   * This is the data store initialized to the size given in the
   * constructor. This store is divided into blocks each of size 
   * upperBlockSize. Discrete chunks would be owned by individual 
   * MemoryArea instances of the VirtualBuffers.
   * Mostly manipulated by the MemoryArea instances.
   */
  byte[] dataPool;

  /**
   * This is the free list which maintains the indices
   * of unused blocks in the dataPool.
   */
  private LinkedList freeBlocks;

  /**
   * Creates a memory pool manager with the specified buffer size and the
   * upper block size and lower block size combination. The lower block size
   * should be a multiple of upper block size.
   *
   * @param memorySize in MBs.
   * @param upperBlockSize This would be 4 KB in our case.
   * @param lowerBlockSize This would be 8 KB in our case.
   *
  */
  public MemPoolManager(int memorySize, int upperBlockSize,
          int lowerBlockSize) {
    // Initialize the data pool ...
    int memSizeInBytes = memorySize * 1024 * 1024;
    dataPool = new byte[memSizeInBytes];
    // ToDo: Should trim off the last reminder chunk.
    int numBlocks = memSizeInBytes / upperBlockSize;
    for (int i = 0; i < numBlocks; i++) {
        freeBlocks.add(new Integer(i));
    }
  }


  /**
   * Tries to allocate the given number of blocks from the data pool. 
   * May return lesser than requested number of blocks. 
   * Returns null if no free blocks were found.
   * 
   * ToDo : Should have to give variants of this method for contiguous 
   * allocations.
   */
  LinkedList allocateBlocks(int numBlocks) {
    synchronized (this) {
      if (freeBlocks.size() == 0)
        return null;
      LinkedList allocatedBlocks = new LinkedList();
      Iterator iter = freeBlocks.iterator();
      int numAdded = 0;
      while (iter.hasNext()) {
        allocatedBlocks.add(iter.next());
        iter.remove();
        if ( ++numAdded == numBlocks) break;
      }
      return allocatedBlocks;
    }
  }
  
  /**
   * Reclaims the specified blocks into the free list of the
   * global data pool. Used by the VirtualBuffers when they 
   * are closed.
   */
  void releaseBlocks(LinkedList indices) {
    synchronized (this) {
      freeBlocks.addAll(indices);
      /* ToDo : Adding the freed indices at last makes the 
      free list disordered. Should have to maintain it in order,
      so that it makes searching contiguous blocks easier. */
    }
  }
  
}

