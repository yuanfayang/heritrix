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
 * DiskStack.java
 * Created on May 21, 2004
 *
 * $Header$
 */
package org.archive.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.NoSuchElementException;
import java.util.logging.Level;

import org.archive.io.*;
import org.archive.io.RandomAccessInputStream;
import org.archive.io.RandomAccessOutputStream;


/**
 * A Stack which serializes objects to disk.
 * 
 * @author gojomo
 *
 */
public class DiskStack implements Stack {
    /** the backing file */
    protected File storage;

    /** total items in stack */
    protected long height = 0; 

    /** pointer to top prevIndex + topItem in backing file */
    protected long topItemPointer = -1;
    
    RandomAccessFile raf;
    HeaderlessObjectOutputStream pushStream;
    HeaderlessObjectInputStream popStream;

    /**
     * 
     */
    public DiskStack(File storage) throws IOException {
        super();
        this.storage = storage;
        raf = new RandomAccessFile(storage, "rws");
        if (raf.length()>0) {
            restore();
        } else {
            start();
        }
        pushStream = new HeaderlessObjectOutputStream(new RandomAccessOutputStream(raf));
        popStream = new HeaderlessObjectInputStream(new RandomAccessInputStream(raf));
    }
    
    /**
     * 
     */
    private void start() throws IOException {
        raf.writeLong(0);
        raf.writeLong(0);
    }

    /**
     * Read height and index of top item from end of file.
     * 
     */
    private void restore() throws IOException {
        raf.seek(raf.length()-16);
        height = raf.readLong();
        topItemPointer = raf.readLong();
    }

    /* (non-Javadoc)
     * @see org.archive.util.Stack#push(java.lang.Object)
     */
    public void push(Object object) {
        try {
            long itemStartPointer = raf.getFilePointer();
            pushStream.writeObject(object);
            pushStream.reset();
            height++;
            raf.writeLong(height);
            raf.writeLong(itemStartPointer);
            topItemPointer = itemStartPointer;
        } catch (IOException e) {
            DevUtils.logger.log(Level.SEVERE,"push("+object+")" +
                    DevUtils.extraInfo(),e);
        }
    }

    /* (non-Javadoc)
     * @see org.archive.util.Stack#pop()
     */
    public Object pop() {
        if(height==0) {
            throw new NoSuchElementException();
        }
        Object retObj = null;
        try {
            raf.seek(topItemPointer-8);
            long nextPointer = raf.readLong();
            retObj = popStream.readObject();
            height--;
            raf.seek(topItemPointer);
            topItemPointer = nextPointer;
        } catch (IOException e) {
            DevUtils.logger.log(Level.SEVERE,"pop()" +
                    DevUtils.extraInfo(),e);
        } catch (ClassNotFoundException e) {
            DevUtils.logger.log(Level.SEVERE,"pop()" +
                    DevUtils.extraInfo(),e);
        }
        return retObj;
    }

    /* (non-Javadoc)
     * @see org.archive.util.Stack#peek()
     */
    public Object peek() {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see org.archive.util.Stack#height()
     */
    public long height() {
        return height;
    }

    /**
     * Close, releasing held resources.
     * 
     * @throws IOException
     */
    public void close() throws IOException {
        pushStream.close();
        popStream.close();
        raf.close();
    }
    
    /**
     * Close and delete any disk-based storage
     * 
     * @throws IOException
     */
    public void discard() throws IOException {
        close();
        storage.delete();
    }

    /* (non-Javadoc)
     * @see org.archive.util.Stack#release()
     */
    public void release() {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see org.archive.util.Stack#isEmpty()
     */
    public boolean isEmpty() {
        return height == 0;
    }

}
