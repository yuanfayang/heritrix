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

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.crawler.checkpoint.ObjectPlusFilesInputStream;
import org.archive.crawler.checkpoint.ObjectPlusFilesOutputStream;
import org.archive.io.*;
import org.archive.io.RandomAccessInputStream;
import org.archive.io.RandomAccessOutputStream;


/**
 * A Stack which serializes objects to disk.
 *
 * @author gojomo
 *
 */
public class DiskStack implements Stack, Serializable {
    private static final Logger logger =
        Logger.getLogger(DiskStack.class.getName());

    /** the backing file */
    protected File storage;

    /** total items in stack */
    protected long height = 0;

    /** pointer to top prevIndex + topItem in backing file */
    protected long topItemPointer = -1;

    /** random-access file into which bojects are serialized */
    transient RandomAccessFile raf;
    /** stream offering non-serialization writing into raf */
    transient DataOutputStream rawStream;
    /** stream offering serialization writing into raf */
    transient HeaderlessObjectOutputStream pushStream;
    /** stream offering serialization reading from raf */ 
    transient HeaderlessObjectInputStream popStream;


    /**
     * @param storage
     * @throws FileNotFoundException
     * @throws IOException
     */
    public DiskStack(File storage) throws IOException {
        super();
        this.storage = storage;
        // test minimally if supplied file is sensible
        if(storage.exists()==false) {
            storage.getParentFile().mkdirs();
            storage.createNewFile();
        }
    }

    /**
     * Initialization tasks are put off until backing is
     * needed, and may be repeated if backing is discarded
     * during object lifetime.
     *
     * @throws FileNotFoundException
     * @throws IOException
     */
    private void lazyInitialize() throws FileNotFoundException, IOException {
        raf = new RandomAccessFile(storage, "rws");
        if (raf.length()>0) {
            restore();
        } else {
            start();
        }
        // buffer all writing to raf for efficiency
        BufferedOutputStream outStream = new BufferedOutputStream(new RandomAccessOutputStream(raf),4096);
        
        rawStream = new DataOutputStream(outStream);
        pushStream = new HeaderlessObjectOutputStream(outStream);
        
        popStream = new HeaderlessObjectInputStream(new RandomAccessInputStream(raf));
    }

    /**
     * @param raf2
     * @return
     */
    private Object RandomAccessOutputStream(RandomAccessFile raf2) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Add date to front of fresh backing file.
     * @throws IOException
     */
    private void start() throws IOException {
        raf.writeLong(0);
        raf.writeLong(0);
    }

    /**
     * Read height and index of top item from end of file.
     * @throws IOException
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
            if(raf==null) {
                lazyInitialize();
            }
            long itemStartPointer = raf.getFilePointer();
            // The reset writes a byte onto the stream, some kind of delimiter.
            // Put it onto the stream before serializing of object.
            pushStream.reset();
            pushStream.writeObject(object);
            height++;
            rawStream.writeLong(height);
            rawStream.writeLong(itemStartPointer);
            pushStream.flush(); // flushes underlying bufferedstream
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
            if(raf == null) {
                lazyInitialize();
            }
            raf.seek(topItemPointer-8);
            long nextPointer = raf.readLong();
            retObj = popStream.readObject();
            height--;
            raf.seek(topItemPointer);
            raf.setLength(topItemPointer); // truncate to current size
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
        if(pushStream!=null) {
            pushStream.close();
        }
        if(popStream!=null) {
            popStream.close();
        }
        if(raf!=null) {
            raf.close();
            raf = null;
        }
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

    /**
     *
     */
    public void disconnect() {
        try {
            close();
        } catch (IOException e) {
            DevUtils.logger.log(Level.SEVERE,"disconnect()" +
                    DevUtils.extraInfo(),e);
        }
    }

    /* (non-Javadoc)
     * @see org.archive.util.Stack#release()
     */
    public void release() {
        try {
            discard();
        } catch (IOException e) {
            DevUtils.logger.log(Level.SEVERE,"release()" +
                    DevUtils.extraInfo(),e);
        }
    }

    /* (non-Javadoc)
     * @see org.archive.util.Stack#isEmpty()
     */
    public boolean isEmpty() {
        return height == 0;
    }

    // custom serialization
    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        // now, must snapshot constituent files and their current extents/positions
        // to allow equivalent restoral
        ObjectPlusFilesOutputStream coostream = (ObjectPlusFilesOutputStream)stream;
        // save storage file
        // TODO: ensure copy, since file is editted in place
        coostream.snapshotAppendOnlyFile(storage );
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        // now, must restore constituent files to their checkpoint-time
        // extents and read positions
        ObjectPlusFilesInputStream coistream = (ObjectPlusFilesInputStream)stream;
        // restore storage file
        coistream.restoreFile(storage);
    }


}
