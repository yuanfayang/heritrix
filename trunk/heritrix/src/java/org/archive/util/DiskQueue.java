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
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.logging.Level;

import org.archive.crawler.framework.Savable;
import org.archive.io.DiskBackedByteQueue;
import org.archive.io.DevNull;

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
public class DiskQueue implements Queue, Savable {

    /** the directory used to create the temporary files */
    private File scratchDir;

    /** the prefix for the files created in the scratchDir */
    String prefix;

    /** the number of elements currently in the queue */
    long length;

    /**
     * The object which deals with serializing the actual bytes to/from disk.
     */
    DiskBackedByteQueue bytes;

    ObjectOutputStream testStream; // to verify that object is serializable
    ObjectOutputStream tailStream;
    ObjectInputStream headStream;

    /**
     * A flag which marks when the lazy initialization is finished, and the
     * object is ready for use
     */
    private boolean isInitialized = false;


    /** Create a new {@link DiskQueue} which creates its temporary files in a
     * given directory, with a given prefix.
     *
     * @param dir the directory in which to create the data files
     * @param prefix
     * @throws FileNotFoundException if we cannot create an appropriate file
     */
    public DiskQueue(File dir, String prefix) throws FileNotFoundException {
        if(dir == null || prefix == null) {
            throw new FileNotFoundException("null arguments not accepted");
        }

        length = 0;
        this.prefix = prefix;
        this.scratchDir = dir;
        bytes = new DiskBackedByteQueue(scratchDir, this.prefix);
        bytes.initializeStreams();
        // TODO someday: enable queue to already be filled
    }

    private void lateInitialize() throws FileNotFoundException, IOException {
        testStream = new ObjectOutputStream(new DevNull());        
        tailStream = new ObjectOutputStream(bytes.getTailStream());
        headStream = new ObjectInputStream(bytes.getHeadStream());
        tailStream.flush();
        isInitialized = true;
    }

    /* (non-Javadoc)
     * @see org.archive.util.Queue#enqueue(java.lang.Object)
     */
    public void enqueue(Object o){
        //logger.finest(name+"("+length+"): "+o);
        try {
            if(!isInitialized) {
                lateInitialize();
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

    /* (non-Javadoc)
     * @see org.archive.util.Queue#isEmpty()
     */
    public boolean isEmpty() {
        return length==0;
    }

    /* (non-Javadoc)
     * @see org.archive.util.Queue#dequeue()
     */
    public Object dequeue() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        Object o;
        try {
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

    /* (non-Javadoc)
     * @see org.archive.util.Queue#length()
     */
    public long length() {
        return length;
    }

    /* (non-Javadoc)
     * @see org.archive.util.Queue#release()
     */
    public void release() {
        if (bytes != null) {
            try {
                if(headStream != null) headStream.close();
                if(tailStream != null) tailStream.close();
                bytes.discard();
            } catch (IOException e) {
                // TODO: convert to runtime?
                e.printStackTrace();
            }
        }
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Savable#save(java.io.File, java.lang.String)
     */
    public void save(File directory, String key) throws IOException {
        bytes.save(directory, key);
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Savable#restore(java.io.File, java.lang.String)
     */
    public void restore(File directory, String key) throws IOException {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.archive.util.Queue#peek()
     */
    public Object peek() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see org.archive.util.Queue#getIterator(boolean)
     */
    public Iterator getIterator(boolean inCacheOnly) {
        // There are no levels of storage so we will return all items.
        Iterator it = null;
        
        try {
            it = new DiskQueueIterator(bytes.getReadAllInputStream(),length);
        } catch (IOException e) {
            e.printStackTrace();
            it = new Iterator(){
                public void remove() { ; }
                public boolean hasNext() { return false; }
                public Object next() { throw new NoSuchElementException(); }
            };        
        }
        
        return it;
    }

    /* (non-Javadoc)
     * @see org.archive.util.Queue#deleteMatchedItems(org.archive.util.QueueItemMatcher)
     */
    public long deleteMatchedItems(QueueItemMatcher matcher) {
        // While not processed as many items as there currently are in the queue
        //  dequeue item
        //  if it does not match, requeue it.
        //  else discard it
        // end loop
        long itemsInQueue = length();
        long numberOfDeletes = 0;
        for ( int i = 0 ; i < itemsInQueue ; i++ ){
            Object o = dequeue();
            if(matcher.match(o)==false){
                // Not supposed to delete this one, put it back
                enqueue(o);
            } else {
                numberOfDeletes++;
            }
        }
        return numberOfDeletes;
    }

}
