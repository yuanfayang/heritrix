// Copyright (c) 2002 G.B.Reddy (reddy@isofttech.com)
/**
 * $Id$
*/

package org.archive.crawler.io;

import java.util.*;
import java.io.*;


/**
 * MemoryArea represents the memory area allocated to a particular
 * VirtualBuffer. This represents a collection of memory blocks
 * in the data pool of the manager of the VirtualBuffer.
 * 
 * Read/write operations into the allocated memory blocks could be 
 * performed using this class. During write operation, blocks are
 * occupied incrementally from the manager's data pool until the
 * max permitted ( MAX_MEM_SIZE ) is reached. 
 * 
 * It seizes to write any more bytes, when subsequent free blocks 
 * could not be found in the manager's data pool.
 * 
 * The indices of the allocated blocks in the manager's data pool 
 * are tracked using a LinkedList of integers.
 * 
 * The read positions are not tracked by this class since there 
 * could multiple InputStreams working on this memory area 
 * concurrently. So, the readers have to maintain the next read 
 * position themselves.
 * 
 * Whereas, write operations being done by a single caller, the 
 * write position is tracked by this class itself.
 * 
 * This class is package private since it is only internally
 * used by the VirtualBuffer classes and the 
 * SpreadOutputStream/SpreadInputStream classes.
*/
class MemoryArea {

  /**
   * The maximum memory size in KBs that any MemoryArea instance 
   * can hold.
   * ToDo : Should make this a parameter of MemPoolManager.
   */
  private static final int MAX_MEM_SIZE = 32; // in KBs.
  
  /**
   * The manager of the virtual buffer to which this memory area 
   * corresponds to.
   */
  private MemPoolManager mgr;
  
  /**
   * Holds the indices of the blocks which are currently 
   * occupied by this memory area. It is the sequence 
   * of the allocation in the global data pool.
   */
  private LinkedList allocationSequence;
  
  /**
   * The number of valid bytes written into the memory area. This 
   * value is always in the range <tt>0</tt> through 
   * <tt>MAX_MEM_SIZE</tt>; elements in the range <tt>0</tt> 
   * through <tt>length-1</tt> contain valid byte data.
   * This also tracks the current write position in the global data pool. 
   * For example, if you have completed 4 chunks and written 20 bytes in the 
   * 5th chunk, then the value would be (4 * 4K + 20) where 4k is 
   * assumed to be the chunk size.
   */
  private int length;

  /**
   * Just to signify that no more bytes could be accomodated into this
   * memory area. This could be because the max size is reached or the
   * manager's data pool is fully occupied.
   */
  private boolean exhausted;

  /**
   * Contructs a memory area for the specified manager. The first
   * free block acquired by the virtual buffer implementation is fed
   * as the second argument. It is more of a convenience since a 
   * MemeoryArea instance is created by the VirtualBuffers only if 
   * atleast one free block is found in the manager's data pool.
   */
  MemoryArea(MemPoolManager mgr, LinkedList firstBlock) {
    this.mgr = mgr;
    this.allocationSequence = firstBlock;
  }
  
  /**
   * Returns the current length value.
   */
  int getLength() {
    return length;
  }
  
  /**
   * Writes the given data bytes into its allocated memory area.
   * It uses the allocationSequence and the length value to 
   * locate the right indices at which to write the data bytes 
   * into the manager's data pool.
   * If there isn't enough space, it would try to get more free
   * blocks until it reaches its MAX_MEM_SIZE quota.
   * 
   * Returns the number of bytes successfully written into the 
   * memory area. The return value could be less than the length 
   * requested to be written. This could be because the max size
   * was exceeded or there wasn't any more free space in the 
   * manager's data pool. Zero would be returned if none was
   * written.
   */
  int write(byte[] data, int offset, int length) {

  }
  
  /**
   * Reads the byte of data at the given position.
   * Returns -1 if there are no more bytes to read.
   */
  int read(int pos) {
    // &0xff
  }
  
  /**
   * Tries to fill len number of bytes from the given position
   * in the memory area into the given byte array starting at 
   * the given offset.
   * 
   * Returns the actual number of bytes filled. Will be -1
   * if there are no more bytes to read.
   */
  int read(int pos, byte[] b, int off, int len) {
  
  }
  
 
  /**
   * Returns true if either the max quota is reached or if it had hit 
   * the case where the manager's data pool didn't have any free blocks to
   * give. Used by the virtual buffers to check before trying to make 
   * a write call.
   */
  boolean isExhausted() {
    return exhausted;
  }
  
  /**
   * Releases all the occupied memory blocks to the manager.
   */
  void releaseMemory() {
    mgr.releaseBlocks(allocationSequence);
  }
 
}


