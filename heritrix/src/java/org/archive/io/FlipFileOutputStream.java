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
 * FlipFileOutputStream.java
 * Created on Oct 14, 2003
 *
 * $Header$
 */
package org.archive.io;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * An output stream that uses a pair of files on disk. One file is used for  
 * reading and one for writing. Once the file being read (by a 
 * <code>FlipFileOutputStream</code>) from is exhausted it switches
 * to the other causing this output stream to switch to the other file 
 * (overwriting it).
 * <p>
 * The files are named xxx.ff0 and xxx.ff1 (with xxx being user configureable).
 * <p>
 * This structure is ideally suited for implementing disk based queues.
 * @author Gordon Mohr.
 * @see org.archive.io.FlipFileInputStream
 * @see org.archive.io.DiskBackedByteQueue
 */
public class FlipFileOutputStream extends OutputStream {
    BufferedOutputStream outStream;
    FileOutputStream fileStream;
    String pathPrefix;
    File file0;
    File file1;
    File currentFile;

    /**
     * Constructor
     * @param tempDir The directory where the files are to be stored.
     * @param backingFilenamePrefix name of the files, the class will append
     *                              to this name to make them each unique.
     * @throws FileNotFoundException if unable to create FileOutStream.
     */
    public FlipFileOutputStream(File tempDir, String backingFilenamePrefix) 
                            throws FileNotFoundException  {
        tempDir.mkdirs();
        pathPrefix = tempDir.getPath()+File.separatorChar+backingFilenamePrefix;
        file0 = new File(pathPrefix + ".ff0");
        file1 = new File(pathPrefix + ".ff1");
        currentFile = file0;
        fileStream = new FileOutputStream(currentFile);
        outStream = new BufferedOutputStream(fileStream, 4096);
    }

    /* (non-Javadoc)
     * @see java.io.OutputStream#write(int)
     */
    public void write(int b) throws IOException {
        outStream.write(b);
    }

    /* (non-Javadoc)
     * @see java.io.OutputStream#write(byte[], int, int)
     */
    public void write(byte[] b, int off, int len) throws IOException {
        outStream.write(b, off, len);
    }

    /* (non-Javadoc)
     * @see java.io.OutputStream#write(byte[])
     */
    public void write(byte[] b) throws IOException {
        outStream.write(b);
    }


    // TODO other write()s for efficiency
    /**
     * Called by <code>FlipFileInputStream</code> when it has exhausted
     * it's current input file. Returns the current output file and switches
     * writing to the old input file.
     */
    protected File getInputFile() throws IOException {
        File lastFile = currentFile;
        flip();
        return lastFile;
    }

    /**
     * Flush the current output stream and switch it to the other file.
     */
    private void flip() throws IOException {
        flush();
        currentFile = (currentFile == file0) ? file1 : file0;
        outStream = new BufferedOutputStream(new FileOutputStream(currentFile), 4096);
    }
    
    /**
     * Returns the current output file.
     * @return the current output file
     */
    public File getOutputFile(){
        return currentFile;
    }

    /**
     * Deletes both files associated with this class and it's partner.
     */
    public void discard() {
        file0.delete();
        file1.delete();
    }

    /* (non-Javadoc)
     * @see java.io.OutputStream#close()
     */
    public void close() throws IOException {
        super.close();
        outStream.close();
    }

    public File getFile0() {
        return file0;
    }

    public File getFile1() {
        return file1;
    }

    public int getCurrentFileIndex() {
        return (currentFile == file0) ? 0 : 1;
    }
    
    /* (non-Javadoc)
     * @see java.io.OutputStream#flush()
     */
    public void flush() throws IOException{
        outStream.flush();
    }
}
