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
 * FlipFileInputStream.java
 * Created on Oct 14, 2003
 *
 * $Header$
 */
package org.archive.io;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * An input stream that uses a pair of files on disk. One file is used for reading 
 * and one for writing. Once the file being read from is exhausted it switches
 * to the other causing the writer (a <code>FlipFileOutputStream</code>) to
 * switch to the other file (overwriting it).
 * <p>
 * The files are named xxx.ff0 and xxx.ff1 (with xxx being user configureable).
 * <p>
 * This structure is ideally suited for implementing disk based queues.
 * @author Gordon Mohr.
 * @see org.archive.io.FlipFileOutputStream
 * @see org.archive.io.DiskBackedByteQueue
 */
public class FlipFileInputStream extends InputStream {
    FlipFileOutputStream source;
    FileInputStream fileStream;
    InputStream inStream;
    File currentFile;
    long position;
    
    /**
     * Constructor.
     * @param tailStream The partner output stream.
     */
    public FlipFileInputStream(FlipFileOutputStream tailStream) {
        source = tailStream;
        position = 0;
    }

    /* (non-Javadoc)
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

    /* (non-Javadoc)
     * @see java.io.InputStream#read(byte[])
     */
    public int read(byte[] b) throws IOException {
        int count;
        if (inStream==null || (count = inStream.read(b)) == -1 ) {
            getNewInStream();
            if((count = inStream.read(b)) == -1) {
                // if both old and new stream were exhausted, return EOF
                return -1;
            }
        }
        if( count != -1 ){
            position += count; //just read that many bytes.
        }
        return count;
    }


    /* (non-Javadoc)
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
     */
    private void getNewInStream() throws FileNotFoundException, IOException {
        if(inStream!=null) {
            inStream.close();
        }
        currentFile = source.getInputFile();
        fileStream = new FileInputStream(currentFile);
        inStream = new BufferedInputStream(fileStream,4096);
        position = 0;
    }

    /* (non-Javadoc)
     * @see java.io.InputStream#close()
     */
    public void close() throws IOException {
        super.close();
        inStream.close();
    }

    /**
     * Returns the current position of the input stream in the current file
     * (since last flip).
     * @return number of bytes that have been read from the current file.
     * @throws IOException
     */
    public long getReadPosition() throws IOException {
        return position;
    }
}
