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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
 * @author gojomo
 *
 */
public class DiskBackedByteQueue implements Savable {
    File tempDir;
    String backingFilenamePrefix;
    FlipFileInputStream headStream;
    FlipFileOutputStream tailStream;

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
