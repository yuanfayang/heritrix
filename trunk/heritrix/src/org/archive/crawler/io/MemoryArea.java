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
 * max permitted ( MAX_BLOCKS ) is reached. It seizes to write 
 * any more bytes, when subsequent free blocks could not be found 
 * in the manager's data pool.
 * 
 * The indices of the allocated blocks in the manager's data pool 
 * are tracked using a LinkedList of integers.
 * 
 * The read positions are not tracked by this class since there 
 * could be multiple InputStreams working on this memory area 
 * concurrently. So, the readers have to maintain the next read 
 * position themselves.
 * 
 * Whereas, write operations being done by a single caller, the 
 * write position is tracked by this class itself.
 * 
 * None of the methods in this class are blocking calls.
 * 
 * This class is package private since it is only internally
 * used by the VirtualBuffer classes and the 
 * SpreadOutputStream/SpreadInputStream classes.
*/
class MemoryArea {

  /**
   * The maximum number of blocks that any MemoryArea instance 
   * can hold.
   * ToDo : Should make this a parameter of MemPoolManager.
   */
  private static final int MAX_BLOCKS = 4;
  
  /**
   * The manager of the virtual buffer to which this memory area 
   * corresponds to.
   */
  private MemPoolManager mMgr;
  
  /**
   * Holds the indices of the blocks which are currently 
   * occupied by this memory area. It is the sequence 
   * of the allocation in the manager's data pool.
   */
  private LinkedList mAllocationSequence;
  
  /**
   * The number of valid bytes written into the memory area. This 
   * value is always in the range <tt>0</tt> through 
   * <tt>MAX_BLOCKS * mMgr.upperBlockSize</tt>; elements in the 
   * range <tt>0</tt> through <tt>length-1</tt> contain valid byte data.
   * This also tracks the current write position in the manager's data pool.
   * For example, if you have completed 2 chunks and written 20 bytes in the 
   * 3rd chunk, then the value would be (2 * 4K + 20) where 4k is 
   * assumed to be the chunk size.
   * 
   * ToDo: Problem -- mLength here is more of a position rather than a size.
   * So, if it is size, it has to one greater than the position. Take care.
   * This influences a lot of code.
   */
  private int mLength;

  /**
   * Just to signify that no more bytes could be accomodated into this
   * memory area. This could be because the MAX_BLOCKS is reached or the
   * manager's data pool isn't free for further allocation.
   */
  private boolean mExhausted;

  /**
   * Contructs a memory area for the specified manager. The first
   * free block acquired by the virtual buffer implementation is fed
   * as the second argument. It is more of a convenience since a 
   * MemeoryArea instance is created by the VirtualBuffers only if 
   * atleast one free block is found in the manager's data pool.
   */
  MemoryArea(MemPoolManager mgr, LinkedList firstBlock) {
    mMgr = mgr;
    mAllocationSequence = firstBlock;
  }
  
  /**
   * Returns the current length value.
   */
  int getLength() {
    return mLength;
  }
  
  /**
   * Writes the given data bytes into its allocated memory area.
   * It uses the mAllocationSequence and the mLength value to 
   * locate the right indices at which to write the data bytes 
   * into the manager's data pool.
   * If there isn't enough space, it would try to get more free
   * blocks until it reaches its MAX_BLOCKS quota.
   * 
   * Returns the number of bytes successfully written into the 
   * memory area. The return value could be less than the length 
   * requested to be written. This could be because the max size
   * was exceeded or there wasn't any more free space in the 
   * manager's data pool. Zero would be returned if none was
   * written.
   * 
   * This method is not thread-safe. Assumes that there will be
   * one writer only.
   */
  int write(byte[] data, int off, int len) {
    if ((off < 0) || (off > data.length) || (len < 0) ||
              ((off + len) > data.length) || ((off + len) < 0)) {
        throw new IndexOutOfBoundsException();
    } else if (len == 0) {
        return 0;
    }
    int lengthB4Write = mLength;
    int numBlocks = mAllocationSequence.size();
    int lastBlockIndex = ((Integer)mAllocationSequence.getLast()).intValue();

    while (len != 0) {
      int freeSpaceInCurrentBlock = numBlocks * mMgr.upperBlockSize - mLength;
      // take in as much as possible..
      if ( freeSpaceInCurrentBlock >= len ) {
        // do array copy..
        System.arraycopy(data, off, mMgr.mDataPool,
            (lastBlockIndex * mMgr.upperBlockSize +
            mLength % mMgr.upperBlockSize), len);
        mLength += len;
        len = 0;
      } else {
        if (freeSpaceInCurrentBlock > 0) {
          // do array copy..
          System.arraycopy(data, off, mMgr.mDataPool,
              (lastBlockIndex * mMgr.upperBlockSize +
              mLength % mMgr.upperBlockSize), freeSpaceInCurrentBlock);
          len -= freeSpaceInCurrentBlock;
          off += freeSpaceInCurrentBlock;
          mLength += freeSpaceInCurrentBlock;
        }
        if ( numBlocks < MAX_BLOCKS ) {
          // allocate new block..
          LinkedList newBlock = mMgr.allocateBlocks(1);
          if (newBlock != null) {
            mAllocationSequence.addAll(newBlock);
            numBlocks++;
            lastBlockIndex = ((Integer)newBlock.getFirst()).intValue();
            continue;
          }
        }
        mExhausted = true;
        break;
      }
    }
    return (mLength - lengthB4Write);
  }
  
  /**
   * 
   */
  boolean write(int b) {
    int numBlocks = mAllocationSequence.size();
    int lastBlockIndex = ((Integer)mAllocationSequence.getLast()).intValue();

    int freeSpaceInCurrentBlock = numBlocks * mMgr.upperBlockSize - mLength;
    if (freeSpaceInCurrentBlock > 0) {
      int index = lastBlockIndex * mMgr.upperBlockSize +
            mLength % mMgr.upperBlockSize;
      mMgr.mDataPool[index] = (byte)b;
      mLength++;
      return true;
    }
    else {
      if ( numBlocks < MAX_BLOCKS ) {
        // allocate new block..
        LinkedList newBlock = mMgr.allocateBlocks(1);
        if (newBlock != null) {
          mAllocationSequence.addAll(newBlock);
          numBlocks++;
          lastBlockIndex = ((Integer)newBlock.getFirst()).intValue();
          int index = lastBlockIndex * mMgr.upperBlockSize +
            mLength % mMgr.upperBlockSize;
          mMgr.mDataPool[index] = (byte)b;
          mLength++;
          return true;
        }
      }
      mExhausted = true;
      return false;
    }
  }
  
  /**
   * Reads the byte of data at the given position.
   * Returns -1 if there are no more bytes to read
   * or if the given position is out of the memory 
   * area's boundry.
   */
  int read(int pos) {
    if (pos >= mLength) return -1;
    int blockNum = pos / mMgr.upperBlockSize;
    if (blockNum >= mAllocationSequence.size()) {
      System.err.println("Ooops !! Bug in Memory Area detected in read(pos).");
      return -1;
    }
    int blockIndex = ((Integer)mAllocationSequence.get(blockNum)).intValue();
    return ( mMgr.mDataPool[blockIndex * mMgr.upperBlockSize + 
        (pos % mMgr.upperBlockSize)] & 0xff );
  }
  
  /**
   * Similar to read(int, byte[], int, int).
   */
  int read(int pos, byte b[]) {
    return read(pos, b, 0, b.length);
  }
  
  /**
   * Tries to fill len number of bytes from the given position
   * in the memory area into the given byte array starting at 
   * the given offset.
   * 
   * Returns the actual number of bytes filled into the given 
   * array. Will be -1 if there are no more bytes to read.
   */
  int read(int pos, byte[] b, int off, int len) {
    if (b == null) {
        throw new NullPointerException();
    } else if ((off < 0) || (off > b.length) || (len < 0) ||
       ((off + len) > b.length) || ((off + len) < 0)) {
      throw new IndexOutOfBoundsException();
    }
    if (pos >= mLength) return -1;
    if (pos + len > mLength) {
      len = mLength - pos;
    }
    if (len <= 0) return 0;
    
    int blockNum = pos / mMgr.upperBlockSize;
    if (blockNum >= mAllocationSequence.size()) {
      System.err.println("Ooops !! Bug in Memory Area detected in read(args).");
      return -1;
    }
    int blockIndex = ((Integer)mAllocationSequence.get(blockNum)).intValue();
    System.arraycopy(mMgr.mDataPool, blockIndex * mMgr.upperBlockSize + 
        (pos % mMgr.upperBlockSize), b, off, len);
    return len;
  }
  
 
  /**
   * Returns true if either the max quota is reached or if it had hit 
   * the case where the manager's data pool didn't have any free blocks to
   * give. Used by the virtual buffers to check before trying to make 
   * a write call.
   */
  boolean isExhausted() {
    return mExhausted;
  }
  
  /**
   * Releases all the occupied memory blocks to the manager.
   */
  void releaseMemory() {
    mMgr.releaseBlocks(mAllocationSequence);
  }
 
}


