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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.util.Properties;

import org.archive.crawler.framework.Savable;
import org.archive.util.ArchiveUtils;

/**
 * FIFO byte queue, using disk space as needed.
 *
 * TODO: add maximum size?
 *
 * Flips between two backing files: as soon as reading head
 * reaches beginning of one, writing tail flips to other.
 *
 * @author Gordon Mohr
 *
 */
public class DiskBackedByteQueue implements Savable {
    File tempDir;
    String backingFilenamePrefix;
    FlipFileInputStream headStream;   // read stream
    FlipFileOutputStream tailStream;  // write stream

    public DiskBackedByteQueue(File tempDir, String backingFilenamePrefix) throws FileNotFoundException {
        super();
        this.tempDir = tempDir;
        this.backingFilenamePrefix=backingFilenamePrefix;
     }

    public void initializeStreams() throws FileNotFoundException {
        tailStream = new FlipFileOutputStream(tempDir,backingFilenamePrefix);
        headStream = new FlipFileInputStream(tailStream);
    }
    public InputStream getHeadStream() {
        return headStream;
    }

    public OutputStream getTailStream() {
        return tailStream;
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
        tailStream.flush();
        // Get the head file, move the input stream for it to the current
        // position and wrap in a bufferedInputStream
        BufferedInputStream inStream1;
        if(headStream.currentFile==null){
            // No reads have been performed.
            inStream1 = null;
        } else {
            FileInputStream tmpFileStream1 = new FileInputStream(new File(headStream.currentFile.getAbsolutePath()));
            tmpFileStream1.getChannel().position(
                    headStream.getReadPosition());
            inStream1 = new BufferedInputStream(tmpFileStream1, 4096);
        }
        
        // Get the tail file, alright to read it from start. Wrap it up.
        tailStream.flush(); // Let's make sure nothing is stuck in buffers.
        FileInputStream tmpFileStream2 = new FileInputStream(
                tailStream.currentFile);
        BufferedInputStream inStream2 = new BufferedInputStream(tmpFileStream2,
                4096);

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
        tailStream.close();
    }

    public void discard() {
        tailStream.discard();
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Checkpointable#prepare(int, java.io.File)
     */
    public void save(File directory, String key) throws IOException {
        // virtually copy the two flip files
        ArchiveUtils.hardLinkOrCopy(tailStream.getFile0(),new File(directory,key+".0"));
        ArchiveUtils.hardLinkOrCopy(tailStream.getFile1(),new File(directory,key+".1"));
        // remember all read state
        Properties props = new Properties();
        props.setProperty("currentFile",""+tailStream.getCurrentFileIndex());
        props.setProperty("headPosition",""+headStream.getReadPosition());
        props.setProperty("fileExtent0",""+tailStream.getFile0().length());
        props.setProperty("fileExtent1",""+tailStream.getFile1().length());
        OutputStream storeStream = new FileOutputStream(new File(directory,key+".p"));
        props.store(storeStream,"DiskBackedByteQueue");
        storeStream.close();
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Savable#restore(java.io.File, java.lang.String)
     */
    public void restore(File directory, String key) throws IOException {

    }

}
