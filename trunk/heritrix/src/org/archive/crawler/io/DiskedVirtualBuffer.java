/*
 * DiskedVirtualBuffer.java
 * Created on Apr 17, 2003
 *
 * $Id$
 */
package org.archive.crawler.io;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.Checksum;


/**
 * A virtual buffer of arbitrary size, using a mixture of RAM and 
 * backing disk when necessary. The RAM blocks are acquired from 
 * the MemPoolManager instance to which this virtual buffer is 
 * associated to. 
 * 
 * This buffer is actually read/written by requesting the 
 * relevant stream. Write operations are performed using the
 * SpreadOutputStream. The SpreadOutputStream creates a local
 * MemoryArea instance with which it manages to do the job of
 * cccupying data blocks from the manager's data pool and other
 * read/write operations over them. 
 * If the manager fails to provide free blocks, a local augmented 
 * memory of fixed size is created and used by the SpreadOutputStream.
 * 
 * Once the output stream is closed by the user, the SpreadOutputStream
 * calls back the DiskedVirtualBuffer to submit the data received, so that
 * it could be read by the user using the input streams. 
 *
 * @author reddy
 */
public class DiskedVirtualBuffer extends VirtualBuffer {

  /**
   * The manager of this virtual buffer. Set through the
   * constructor.
   */
  private MemPoolManager mMgr;
  
  /**
   * The memory area which abstracts the regions occupied in the
   * manager's data pool. 
   * May be null if the first attempt to get a free block from the 
   * manager failed.
   * Set by the output stream's callback.
   * Package-private to allow easy access to the InputStreams.
   */
  MemoryArea mMemArea;

  /**
   * The max size of the local augmented memory.
   */
  static final int MAX_AUGMENT_SIZE = 8 * 1024 * 1024;

  /**
   * The local augmented data buffer that is created and used when 
   * the first attempt to get a free block from the manager's data 
   * pool fails. 
   * Set by the output stream's callback.
   * Package-private to allow easy access to the InputStreams.
   */
  byte[] mAugmentedDataArray;

  /**
   * This would be a file in the system's temp directory used by 
   * the OutputStream to spill the overflowing data into disk.
   * Set by the output stream's callback.
   */
  File mSpillFile;
  
  /**
   * A flag to indicate that the single output stream was 
   * created by the user.
   */
  private boolean mOutStreamCreated;

  /**
   * The total length or size of this virtual buffer.
   * Set by the output stream's callback.
   */
  long mLength;

  /**
   * Creates a disked virtual buffer for the given manager.
   * Read/writes into this buffer could be performed using
   * the i/o streams provided by this class.
   */
  public DiskedVirtualBuffer(MemPoolManager mgr) {
    this.mMgr = mgr;
  }
  
  /**
  * Returns an OutputStream to write into the buffer. Only
  * one such stream can be requested. Any subsequent requests
  * will throw a RuntimeException. Once this stream
  * is closed, no further writing is possible.
  * @return
  */
  public OutputStream getOutputStream() {
    if ( mOutStreamCreated )
      throw new RuntimeException("OutputStream already created.");
    return new SpreadOutputStream(mMgr, this);
  }
  
  /**
  * Returns a SeekableInputStream for reading from the 
  * buffer. Each call returns an independent stream,
  * initially pointing at the beginning of the buffer.
  * @return
  */
  public SeekableInputStream getInputStream() {
    return new SpreadInputStream(mMgr, this);
  }

  /**
  * Returns a CharSequence interface on this VirtualBuffer
  * (for use by Regular Expressions, for example).
  * 
  * ToDo: May require choice of a character encoding in subclasses,
  * by previous initialization, or in alternate forms of
  * this method.
  * @return
  */
  public CharSequence getCharSequence() {
    try {
      return new CharSequenceImpl(getInputStream());
    } catch (Exception ex) {
      // ToDo: We may have to have some generic exception handling framework.
      throw new RuntimeException("Unable to create char sequence.", ex);
    }
  }

  /**
  * Returns a CharSequence interface on a subrange of
  * this VirtualBuffer (for use by Regular Expressions, for example).
  * 
  * May require choice of a character encoding in subclasses,
  * by previous initialization, or in alternate forms of
  * this method.
  * @return
  */
  public CharSequence getCharSequence(long start, long end) {
    try {
      return new CharSequenceImpl(getInputStream(), start, end);
    } catch (Exception ex) {
      // ToDo: We may have to have some generic exception handling framework.
      throw new RuntimeException("Unable to create char sequence.", ex);
    }
  }
  
  
  /**
  * Returns whether buffer can only be read. Even if
  * a buffer is initially writable, subsequently closing an
  * OutputStream returned from getOutputStream() will
  * render it read-only. 
  * @return
  */
  public boolean isReadOnly() {
    // ToDo:
    return true;
  }
  
  /**
  * Returns total size of the buffered data. May increase
  * as long as the buffer remains writable. 
  * @return
  */
  public long getSize() {
    // TODO : This needs to be dynamic. If write is in progress,
    // then the length should be got from the output stream.
    return mLength;
  }
  
  /**
  * Returns the checksum of all written data. May change
  * as long as the buffer remains writable. 
  * @return
  */
  public Checksum getChecksum() {
    // ToDo:
    return null;
  }

  /**
   * Releases resources held by this virtual buffer. The space
   * in the RAM and the disk are released. Should be called
   * when no more reads on the buffer is needed. After this method
   * is executed, invocation of any of the other methods would not
   * yield the desired result.
   */  
  public void close() {
    mMemArea.releaseMemory();
    if (mSpillFile != null)
      mSpillFile.delete();
  }

  /**
   * Called by the OutputStream when the user finishes writing. The
   * OutputStream's close method calls this method.
   */
  void writeFinishCallBack(MemoryArea memArea, File file, 
      byte[] augmentedDataArray, long length) {
    this.mMemArea = memArea;
    this.mSpillFile = file;
    this.mAugmentedDataArray = augmentedDataArray;
    this.mLength = length;
  }
  
  /**
  * Can be used to find if this buffer had used the augmented memory
  * and not the memory from the manager's data pool.
  */
  boolean isAugmented() {
    return (mAugmentedDataArray != null);
  }


  class CharSequenceImpl implements CharSequence {
    private SeekableInputStream src;
    private long start, end;
    
    CharSequenceImpl(SeekableInputStream src)
    {
      this.src = src;
      start = 0;
      end = getSize();
    }
    
    CharSequenceImpl(SeekableInputStream src, long start, long end)
            throws IOException
    {
      // ToDo: Need to validate start and end..
      this.start = start;
      this.end = end;
      this.src = new SeekableInputSubstream(src, start, end);
    }
    
    public int length() {
      return (int)(end - start);
    }
    
    public char charAt(int index) {
      if ((index < 0) || (index >= length())) {
          throw new StringIndexOutOfBoundsException(index);
      }
      try {
        src.seek(index);
        // ToDo: The following would work only for us-ascii encoding.
        // Will have to support other encoding.
        return (char)src.read();
      } catch (IOException ex) {
        throw new RuntimeException("Unable to read from virtual buffer.", ex);
      }
    }

    /*
    private char getChar(byte b1, byte b2) {
      if (ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN)) {
        return (char)((b1 << 8) | (b2 & 0xff));
      }
      else {
        return (char)((b2 << 8) | (b1 & 0xff));
      }
    }*/
    
    public CharSequence subSequence(int start, int end) {
      try {
        return new CharSequenceImpl(src, start, end);
      } catch (Exception ex) {
        // ToDo: We may have to have some generic exception handling framework.
        throw new RuntimeException("Unable to create char subsequence.", ex);
      }
    }
    
    // ToDo: Doing a toString will be very costly.. may be nobody would
    // want to do it.
    public String toString() {
      return null;
    }

  }

 
}
