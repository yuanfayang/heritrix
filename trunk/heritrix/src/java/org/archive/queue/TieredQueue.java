/* TieredQueue
*
* $Id$
*
* Created on Aug 16, 2004
*
* Copyright (C) 2004 Internet Archive.
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
package org.archive.queue;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.commons.collections.Predicate;

/**
 * A queue with multiple internal queues, numbered 0 to n.
 * 
 * Dequeues come from lower-numbered queues before higher-numbered queues.
 * 
 * The standard enqueue() places an item at the back of the highest-numbered
 * queue. Other variants of enqueue place items on specific queues.
 * 
 * A peek() guarantees that the item returned by peek() will be returned by the
 * next dequeue(), even if other items are enqueued to lower-numbered internal
 * queues in the meantime. (The unpeek() may be used to release the TieredQueue
 * from this guarantee; the next peek()/dequeue() will then return the item
 * available from the lowest-numbered queue.)
 * 
 * @author gojomo
 */
public class TieredQueue implements Queue {
    Object headObject = null; // offered/set by peek()

    int headSource = -1; // index of queue which provided headObject

    Queue[] innerQueues; // internal tiers

    int lastQueue = 0; // index of highest innerQueue

    long length = 0; // count of items in all innerQueues

    /**
     * Create a TieredQueue with the given number of
     * internal queue slots. 
     */
    public TieredQueue(int tiers) {
        super();
        innerQueues = new Queue[tiers];
        lastQueue = tiers - 1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.archive.queue.Queue#enqueue(java.lang.Object)
     */
    public void enqueue(Object obj) {
        enqueue(obj, lastQueue);
    }

    /**
     * Enqueue the object to the given tier.
     */
    public void enqueue(Object obj, int tier) {
        innerQueues[tier].enqueue(obj);
        length++;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.archive.queue.Queue#isEmpty()
     */
    public boolean isEmpty() {
        return length == 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.archive.queue.Queue#dequeue()
     */
    public Object dequeue() throws NoSuchElementException {
        Object o = peek();
        assert (o == innerQueues[headSource].peek()) : 
            "TieredQueue innerQueue[headSource] != headObject";
        innerQueues[headSource].dequeue();
        headObject = null;
        length--;
        return o;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.archive.queue.Queue#length()
     */
    public long length() {
        return length;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.archive.queue.Queue#release()
     */
    public void release() {
        for (int i = 0; i <= lastQueue; i++) {
            innerQueues[i].release();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.archive.queue.Queue#peek()
     */
    public Object peek() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        if (headObject == null) {
            loadHead();
        }
        return headObject;
    }

    private void loadHead() {
        assert headObject == null : "TieredQueue redundant loadHead called";
        headSource = 0;
        while (innerQueues[headSource].isEmpty()) {
            headSource++;
        }
        headObject = innerQueues[headSource].peek();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.archive.queue.Queue#unpeek()
     */
    public void unpeek() {
        headObject = null;
        headSource = -1;
    }

    
    /**
     * Set the internal queue tier to be the 
     * supplied Queue instance. 
     *  
     * @param tier
     * @param q
     */
    public void setQueue(int tier, Queue q) {
        innerQueues[tier] = q;
        recalculateLength();
    }

    private void recalculateLength() {
        length = 0;
        for (int i = 0; i <= lastQueue; i++) {
            length += innerQueues[i].length();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.archive.queue.Queue#getIterator(boolean)
     */
    public Iterator getIterator(boolean inCacheOnly) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.archive.queue.Queue#deleteMatchedItems(org.apache.commons.collections.Predicate)
     */
    public long deleteMatchedItems(Predicate matcher) {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Initialize all innerQueues to be DiskBackedQueues in the given scratch
     * directory, using the tmpName prefix.
     * 
     * @param scratchDir
     * @param tmpName
     * @throws IOException
     */
    public void initializeDiskBackedQueues(File scratchDir, String tmpName,
            int inMemCap) throws IOException {
        for (int i = 0; i <= lastQueue; i++) {
            innerQueues[i] = new DiskBackedQueue(scratchDir, tmpName + i,
                    false, inMemCap / (lastQueue + 1));
        }
    }

    /**
     * Set the total number of in-memory items, assuming the underlying
     * subqueues are DiskBackedQueue instances.
     * 
     * @param inMemCap
     */
    public void setMemoryResidentQueueCap(int inMemCap) {
        for (int i = 0; i <= lastQueue; i++) {
            ((DiskBackedQueue) innerQueues[i]).setHeadMax(inMemCap
                    / (lastQueue + 1));
        }
    }

}
