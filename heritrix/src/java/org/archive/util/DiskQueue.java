/* DiskQueue
 *
 * $Id$
 *
 * Created on Oct 16, 2003
 *
 * Copyright (C) 2003 Internet Archive.
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
 */
package org.archive.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.logging.Level;

import org.apache.commons.collections.Predicate;
import org.archive.io.*;
import org.archive.io.DevNull;
import org.archive.io.DiskByteQueue;

/**
 * Queue which stores all its objects to disk using object
 * serialization, on top of a DiskBackedByteQueue.
 *
 * The serialization state is reset after each enqueue().
 * Care should be taken not to enqueue() items which will
 * pull out excessive referenced objects, or objects which
 * will be redundantly reinstantiated upon dequeue() from
 * disk.
 *
 * This class is not synchronized internally.
 *
 * @author Gordon Mohr
 */
public class DiskQueue implements Queue, Serializable {
    /** the directory used to create the temporary files */
    private File scratchDir;

    /** the prefix for the files created in the scratchDir */
    String prefix;

    /** the number of elements currently in the queue */
    long length;

    /**
     * The object which deals with serializing the actual bytes to/from disk.
     */
    DiskByteQueue bytes;

    transient ObjectOutputStream testStream; // to verify that object is serializable
    transient ObjectOutputStream tailStream;
    transient ObjectInputStream headStream;

    /**
     * A flag which marks when the lazy initialization is finished, and the
     * object is ready for use
     */
    private boolean isInitialized = false;

    private boolean reuse;


    /** Create a new {@link DiskQueue} which creates its temporary files in a
     * given directory, with a given prefix, and reuse any prexisting backing
     * files as directed.
     *
     * @param dir the directory in which to create the data files
     * @param prefix
     * @param reuse whether to reuse any existing backing files
     * @throws IOException if we cannot create an appropriate file
     */
    public DiskQueue(File dir, String prefix, boolean reuse) throws IOException {
        if(dir == null || prefix == null) {
            throw new FileNotFoundException("null arguments not accepted");
        }

        length = 0;
        this.prefix = prefix;
        this.scratchDir = dir;
        this.reuse = reuse;
        // test minimally if supplied disk paths are sensible
        if(dir.exists()==false) {
            if(dir.mkdirs()==false) {
                throw new FileNotFoundException("unable to create scratch directory");
            }
        }
        // TODO: test more extensively, given lazy disk use, if queue
        // will be be viable?
    }

    /** Create a new {@link DiskQueue} which creates its temporary files in a
     * given directory, with a given prefix.
     * @param file
     * @param file_prefix
     * @throws IOException
     */
    public DiskQueue(File file, String file_prefix) throws IOException {
        this(file,file_prefix,false);
    }

    private void lazyInitialize() throws FileNotFoundException, IOException {
        if(bytes==null) {
            bytes = new DiskByteQueue(scratchDir, this.prefix, reuse);
            bytes.initializeStreams(0);
        }
        testStream = new ObjectOutputStream(new DevNull());
        tailStream = new HeaderlessObjectOutputStream(bytes.getTailStream());
        headStream = new HeaderlessObjectInputStream(bytes.getHeadStream());
        // tailStream.flush(); // ??
        isInitialized = true;
    }

    protected boolean isInitialized(){
    	return isInitialized;
    }

    /**
     * @see org.archive.util.Queue#enqueue(java.lang.Object)
     */
    public void enqueue(Object o){
        //logger.finest(name+"("+length+"): "+o);
        try {
            if(!isInitialized) {
                lazyInitialize();
            }
            // TODO: optimize this, for example by serializing to buffer, then
            // writing to disk on success
            testStream.writeObject(o);
            testStream.reset();
            tailStream.writeObject(o);
            tailStream.reset(); // forget state with each enqueue
            length++;
        } catch (IOException e) {
            // TODO convert to runtime exception?
            DevUtils.logger.log(Level.SEVERE,"enqueue("+o+")" +
                DevUtils.extraInfo(),e);
        }
    }

    /**
     * @see org.archive.util.Queue#isEmpty()
     */
    public boolean isEmpty() {
        return length==0;
    }

    /**
     * @see org.archive.util.Queue#dequeue()
     */
    public Object dequeue() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        Object o;
        try {
             if(!isInitialized) {
                lazyInitialize();
            }
            o = headStream.readObject();
        } catch (IOException e) {
            e.printStackTrace();
            throw new NoSuchElementException();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new NoSuchElementException();
        }
        // logger.finest(name+"("+length+"): "+o);
        length--;
        return o;
    }

    /**
     * @see org.archive.util.Queue#length()
     */
    public long length() {
        return length;
    }

    /**
     * @see org.archive.util.Queue#release()
     */
    public void release() {
        if (bytes != null) {
            try {
                releaseStreams();
                //bytes.close();
                bytes.discard();
            } catch (IOException e) {
                // TODO: convert to runtime?
                e.printStackTrace();
            }
            bytes = null;
        }
        isInitialized = false;
        // If this object is used again after this method is invoked, the
        // late initialization will be reinvoked aquiring these resources
        // again.
    }

    private void releaseStreams() throws IOException {
        if(testStream != null) {
            testStream.close();
            testStream = null;
        }
        if(headStream != null) {
            headStream.close();
            headStream = null;
        }
        if(tailStream != null) {
            tailStream.close();
            tailStream = null;
        }
    }

    /**
     * Disconnect from any backing files, without deleting those
     * files, allowing reattachment later.
     *
     */
    public void disconnect() {
        try {
            releaseStreams();
            bytes.disconnect();
            isInitialized = false;
        } catch (IOException e) {
            // TODO convert to runtime exception?
            DevUtils.logger.log(Level.SEVERE,"disconnect()" +
                DevUtils.extraInfo(),e);
        }
    }

    /**
     * Reconnect to disk-based backing
     */
    public void connect() {
        // do nothing -- allow lazy initialization to connect when necessary
    }

    /**
     * @see org.archive.util.Queue#peek()
     */
    public Object peek() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    /**
     * @see org.archive.util.Queue#getIterator(boolean)
     * @return iterator
     */
    public Iterator getIterator(boolean inCacheOnly) {
        // There are no levels of storage so we will return all items.
        Iterator it = null;

        if( isInitialized ){
            try {
                it = new DiskQueueIterator(bytes.getReadAllInputStream(),length);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if( it == null ){
            it = new Iterator(){
                public void remove() { throw new UnsupportedOperationException(); }
                public boolean hasNext() { return false; }
                public Object next() { throw new NoSuchElementException(); }
            };
        }

        return it;
    }

    /**
     * @see org.archive.util.Queue#deleteMatchedItems(org.apache.commons.collections.Predicate)
     */
    public long deleteMatchedItems(Predicate matcher) {
        // While not processed as many items as there currently are in the queue
        //  dequeue item
        //  if it does not match, requeue it.
        //  else discard it
        // end loop
        long itemsInQueue = length();
        long numberOfDeletes = 0;
        for ( int i = 0 ; i < itemsInQueue ; i++ ){
            Object o = dequeue();
            if(matcher.evaluate(o)==false){
                // Not supposed to delete this one, put it back
                enqueue(o);
            } else {
                numberOfDeletes++;
            }
        }
        return numberOfDeletes;
    }

    // custom serialization
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        // after deserialization, must reinitialize object streams from underlying bytestreams
        isInitialized = false;
    }
}
