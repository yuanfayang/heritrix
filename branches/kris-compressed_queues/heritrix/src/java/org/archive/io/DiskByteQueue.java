/* Copyright (C) 2003 Internet Archive.
 *
 * This file is part of the Heritrix web crawler (crawler.archive.org).
 *
 * Heritrix is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * any later version.
 *
 * Heritrix is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with Heritrix; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * DiskBackedByteQueue.java
 * Created on Oct 14, 2003
 *
 * $Header$
 */
package org.archive.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.io.Serializable;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.archive.crawler.checkpoint.ObjectPlusFilesInputStream;
import org.archive.crawler.checkpoint.ObjectPlusFilesOutputStream;

/**
 * FIFO byte queue, using disk space as needed.
 *
 * TODO: add maximum size?
 *
 * Flips between two backing files: as soon as reading head
 * reaches end of one, and is about to start at the front
 * of the other, the writing tail flips to a new file.
 *
 * The current write-target file (tail) has a file extension
 * ".qout", the current read file (head) has a file extension
 * ".qin". 
 * 
 * @author Gordon Mohr
 */
public class DiskByteQueue implements Serializable {
    private static final String IN_FILE_EXTENSION = ".qin";
    private static final String OUT_FILE_EXTENSION = ".qout";
    
    File tempDir; // enclosing directory for support files
    String backingFilenamePrefix; // filename prefix for both support files
    File inFile; // file from which bytes are read
    File outFile; // file to which bytes are written
    transient FlipFileInputStream headStream;   // read stream
    long rememberedPosition = -1;
    transient FlipFileOutputStream tailStream;  // write stream
    transient long readSinceLastFlip = 0;
    
    boolean compress;
    
    /**
     * Create a new BiskBackedByteQueue in the given directory with given 
     * filename prefix
     * 
     * @param tempDir
     * @param backingFilenamePrefix
     * @param reuse whether to reuse any prexisting backing files
     * @param compress whether to compress data being written to disk
     */
    public DiskByteQueue(File tempDir, 
                         String backingFilenamePrefix, 
                         boolean reuse,
                         boolean compress) {
        super();
        this.tempDir = tempDir;
        this.backingFilenamePrefix=backingFilenamePrefix;
        tempDir.mkdirs();
        String pathPrefix = tempDir.getPath()+File.separatorChar+backingFilenamePrefix;
        inFile = new File(pathPrefix + IN_FILE_EXTENSION);
        outFile = new File(pathPrefix + OUT_FILE_EXTENSION);
        if(reuse==false) {
            if(inFile.exists()) {
                inFile.delete();
            }
            if(outFile.exists()) {
                outFile.delete();
            }
        }
        
        this.compress = compress;
    }
    
    /**
     * Create the support streams
     * 
     * @param readPosition
     * @throws IOException
     */
    public void initializeStreams(long readPosition) throws IOException {
        tailStream = new FlipFileOutputStream();
        headStream = new FlipFileInputStream(readPosition);
    }
    
    /**
     * The stream to read from this byte queue
     * 
     * @return
     */
    public InputStream getHeadStream() {
        return headStream;
    }
    
    /**
     * The stream to write to this byte queue
     * 
     * @return
     */
    public OutputStream getTailStream() {
        return tailStream;
    }
    
    /**
     * flip the current outFile to the inFile role,
     * make new outFile
     * @throws IOException
     */
    void flip() throws IOException {
        inFile.delete();
        tailStream.flush();
        tailStream.close();
        outFile.renameTo(inFile);
        tailStream.setupStreams();
    }
    
    /**
     * Returns an input stream that covers the entire queue. It only allows read
     * access. Reading from it will not affect the queue in any way. It is not 
     * safe to add or remove items to the queue while using this stream.
     * 
     * @return an input stream that covers the entire queue
     * @throws IOException
     */
    public InputStream getReadAllInputStream() throws IOException {
        // Get the head file, move the input stream for it to the current
        // position and wrap in a bufferedInputStream
        BufferedInputStream inStream1;
        if(inFile==null){
            // No reads have been performed.
            inStream1 = null;
        } else {
            FileInputStream tmpFileStream1 = new FileInputStream(inFile);
            if(compress){
                GZIPInputStream tmpZipStream1 = new GZIPInputStream(tmpFileStream1);
                inStream1 = new BufferedInputStream(tmpZipStream1, 4096);
                // Need to skip ahead to right position or else we'll 
                // get item's that have been removed from the queue first
                inStream1.skip(headStream.position);
            } else {
                tmpFileStream1.getChannel().position(
                        headStream.getReadPosition());
                inStream1 = new BufferedInputStream(tmpFileStream1, 4096);
            }
        }
        
        // Get the tail file, alright to read it from start. Wrap it up.
        tailStream.flush(); // Let's make sure nothing is stuck in buffers.
        FileInputStream tmpFileStream2 = new FileInputStream(
                outFile);
        
        BufferedInputStream inStream2;
        if(compress){
            GZIPInputStream tmpZipStream2 = new GZIPInputStream(tmpFileStream2);
            inStream2 = new BufferedInputStream(tmpZipStream2, 4096);
        } else {
        	inStream2 = new BufferedInputStream(tmpFileStream2, 4096);
        }
        
        // Create input stream with object stream header and then wrap it up
        // along with the two input streams in a SequenceInputStream and return it.
        ByteArrayOutputStream baOutStream = new ByteArrayOutputStream();
        new ObjectOutputStream(baOutStream); // This triggers the header to be written to baOutStream.
        ByteArrayInputStream baInStream = new ByteArrayInputStream(baOutStream.toByteArray());
        
        return new SequenceInputStream(
                (inStream1 == null ? (InputStream) baInStream
                        : (InputStream) new SequenceInputStream(baInStream,
                                inStream1)), inStream2);
    }
    
    /**
     * @throws IOException
     */
    public void close() throws IOException {
        headStream.close();
        rememberedPosition = headStream.position;
        headStream = null;
        tailStream.close();
        tailStream = null;
    }
    
    /**
     * frees all streams/files associated with this object
     * @throws IOException
     */
    public void discard() throws IOException {
        close();
        inFile.delete();
        outFile.delete();
    }
    
    /**
     * @throws IOException
     */
    public void disconnect() throws IOException {
        close();
    }
    
    /**
     * @throws IOException
     */
    public void connect() throws IOException {
        initializeStreams(rememberedPosition);
    }
    
    // custom serialization
    private void writeObject(ObjectOutputStream stream) throws IOException {
        tailStream.flush();
        stream.defaultWriteObject();
        // now, must snapshot constituent files and their current extents/positions
        // to allow equivalent restoral
        ObjectPlusFilesOutputStream coostream = (ObjectPlusFilesOutputStream)stream;
        // save tail (write) stream file & extent
        coostream.snapshotAppendOnlyFile(outFile);
        // save head (read) stream file & extent
        coostream.snapshotAppendOnlyFile(inFile);
        // take note of current read position in read file
        coostream.writeLong(headStream.getReadPosition()); 
    }
    
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        // now, must restore constituent files to their checkpoint-time
        // extents and read positions
        ObjectPlusFilesInputStream coistream = (ObjectPlusFilesInputStream)stream;       
        // restore tail (write) stream file & extent
        coistream.restoreFile(outFile);
        // restore head (read) stream file & extent
        coistream.restoreFile(inFile);
        // read position
        long readPosition = coistream.readLong();
        initializeStreams(readPosition);
    }
    
    
    
    /**
     * An output stream that supports the DiskBackedByteQueue, by
     * always appending to the current outFile.
     * 
     * @author Gordon Mohr.
     */
    class FlipFileOutputStream extends OutputStream {        
        BufferedOutputStream outStream;
        FileOutputStream fileStream;
        GZIPOutputStream compressedStream;
        
        /**
         * Constructor
         * @throws FileNotFoundException if unable to create FileOutStream.
         */
        public FlipFileOutputStream() throws IOException  {
            setupStreams();
        }
        
        protected void setupStreams() throws IOException {
            fileStream = new FileOutputStream(outFile,true);
            if(compress){
            	compressedStream = new GZIPOutputStream(fileStream);
                outStream = new BufferedOutputStream(compressedStream, 4096);
            } else {
                outStream = new BufferedOutputStream(fileStream, 4096);
            }
        }
        
        /** (non-Javadoc)
         * @see java.io.OutputStream#write(int)
         */
        public void write(int b) throws IOException {
            outStream.write(b);
        }
        
        /** (non-Javadoc)
         * @see java.io.OutputStream#write(byte[], int, int)
         */
        public void write(byte[] b, int off, int len) throws IOException {
            outStream.write(b, off, len);
        }
        
        /** (non-Javadoc)
         * @see java.io.OutputStream#write(byte[])
         */
        public void write(byte[] b) throws IOException {
            outStream.write(b);
        }
        
        
        /** (non-Javadoc)
         * @see java.io.OutputStream#close()
         */
        public void close() throws IOException {
            super.close();
            outStream.close();
        }
        
        /** (non-Javadoc)
         * @see java.io.OutputStream#flush()
         */
        public void flush() throws IOException{
            outStream.flush();
        }
    }
    
    
    /**
     * An input stream that supports the DiskBackedByteQueue,
     * by always reading from the current inFile, and triggering
     * a "flip" when one inFile is exhausted.
     * 
     * @author Gordon Mohr.
     */
    public class FlipFileInputStream extends InputStream {
        FileInputStream fileStream;
        InputStream inStream;
        GZIPInputStream compressedStream;
        long position;
        
        /**
         * Constructor.
         * @param readPosition
         * @throws IOException
         */
        public FlipFileInputStream(long readPosition) throws IOException {
            setupStreams(readPosition);
        }
        
        /** (non-Javadoc)
         * @see java.io.InputStream#read()
         */
        public int read() throws IOException {
            int c;
            if(inStream==null || (c = inStream.read()) == -1) {
                getNewInStream();
                if((c = inStream.read()) == -1) {
                    // if both old and new streams were exhausted, return EOF
                    return -1;
                }
            }
            if(c!=-1){
                position++;
            }
            return c;
        }
        
        /** (non-Javadoc)
         * @see java.io.InputStream#read(byte[])
         */
        public int read(byte[] b) throws IOException {
            return read(b,0,b.length);
        }
        
        /** (non-Javadoc)
         * @see java.io.InputStream#read(byte[], int, int)
         */
        public int read(byte[] b, int off, int len) throws IOException {
            int count;
            if (inStream==null || (count = inStream.read(b,off,len)) == -1 ) {
                getNewInStream();
                if((count = inStream.read(b,off,len)) == -1) {
                    // if both old and new stream were exhausted, return EOF
                    return -1;
                }
            }
            if( count != -1 ){
                position += count; //just read that many bytes.
            }
            return count;
        }
        
        
        /**
         * Once the current file is exhausted, this method is called to flip files
         * for both input and output streams.
         * @throws FileNotFoundException
         * @throws IOException
         */
        private void getNewInStream() throws FileNotFoundException, IOException {
            if(inStream!=null) {
                inStream.close();
            }
            DiskByteQueue.this.flip();
            setupStreams(0);
        }
        
        private void setupStreams(long readPosition) throws IOException {
            if(inFile.createNewFile() && compress){
                 // Creating a new file, need to write GZIPHeader into it.
                FileOutputStream tmp1 = new FileOutputStream(inFile);
                GZIPOutputStream tmp2 = new GZIPOutputStream(tmp1);
                tmp2.finish();
                tmp2.flush();
                tmp2.close();
                tmp1.flush();
            }
            fileStream = new FileInputStream(inFile);
            if(compress){
                compressedStream = new GZIPInputStream(fileStream);
                inStream = new BufferedInputStream(compressedStream, 4096);
            } else {
                inStream = new BufferedInputStream(fileStream, 4096);
            }
            inStream.skip(readPosition);
            position = readPosition;
        }
        
        /** (non-Javadoc)
         * @see java.io.InputStream#close()
         */
        public void close() throws IOException {
            super.close();
            if(inStream!=null) {
                inStream.close();
            }
        }
        
        /**
         * Returns the current position of the input stream in the current file
         * (since last flip).
         * @return number of bytes that have been read from the current file.
         */
        public long getReadPosition() {
            return position;
        }
        
    }
}
