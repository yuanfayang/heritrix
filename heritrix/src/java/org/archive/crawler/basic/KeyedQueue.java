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
 * KeyedQueue.java
 * Created on May 29, 2003
 *
 * $Header$
 */
package org.archive.crawler.basic;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.util.CompositeIterator;
import org.archive.util.DiskBackedQueue;
import org.archive.util.Queue;
import org.archive.util.QueueItemMatcher;

/**
 * Ordered collection of items with the same "classKey". The
 * collection itself has a state, which may reflect where it
 * is stored or what can be done with the contained items.
 *
 * @author gojomo
 *
 */
public class KeyedQueue implements Queue, URIStoreable {
    private static Logger logger = Logger.getLogger("org.archive.crawler.basic.KeyedQueue");

    long wakeTime;
    String classKey;
    Object state;

    Object inProcessItem;
    
    LinkedList innerStack; // topmost items
    Queue innerQ;

    /**
     * @param key
     * @param scratchDir
     * @param headMax
     */
    public KeyedQueue(String key, File scratchDir, int headMax) {
        super();
        classKey = key;
        String tmpName = null;
        if (key instanceof String) {
            tmpName = (String) key;
        }
        innerStack = new LinkedList();
//        innerQ = new MemQueue();
        try {
            innerQ = new DiskBackedQueue(scratchDir,tmpName,headMax);
        } catch (IOException e) {
            // TODO Convert to runtime exception?
            e.printStackTrace();
        }
    }

    public boolean isReady() {
        return System.currentTimeMillis() > wakeTime;
    }

    /**
     * @return Object
     */
    public String getClassKey() {
        return classKey;
    }


    /* (non-Javadoc)
     * @see org.archive.crawler.basic.URIStoreable#getStoreState()
     */
    public Object getStoreState() {
        return state;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.basic.URIStoreable#setStoreState(java.lang.Object)
     */
    public void setStoreState(Object s) {
        state=s;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.basic.URIStoreable#getWakeTime()
     */
    public long getWakeTime() {
        return wakeTime;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.basic.URIStoreable#setWakeTime(long)
     */
    public void setWakeTime(long w) {
        wakeTime = w;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "KeyedQueue[classKey="+getClassKey()+"]";
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.basic.URIStoreable#getSortFallback()
     */
    public String getSortFallback() {
        return classKey.toString();
    }

    /**
     * The only equals() that matters for KeyedQueues is
     * object equivalence.
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object o) {
        return this == o;
    }

    /* (non-Javadoc)
     * @see org.archive.util.Queue#enqueue(java.lang.Object)
     */
    public void enqueue(Object o) {
        innerQ.enqueue(o);
    }

    /* (non-Javadoc)
     * @see org.archive.util.Queue#isEmpty()
     */
    public boolean isEmpty() {
        return innerStack.isEmpty() && innerQ.isEmpty();
    }

    /* (non-Javadoc)
     * @see org.archive.util.Queue#dequeue()
     */
    public Object dequeue() {
        if (!innerStack.isEmpty()) {
            return innerStack.removeFirst();
        }
        return innerQ.dequeue();
    }

    /* (non-Javadoc)
     * @see org.archive.util.Queue#length()
     */
    public long length() {
        return innerQ.length();
    }

    /**
     *
     */
    public void release() {
        innerQ.release();
    }

    /* (non-Javadoc)
     * @see org.archive.util.Queue#getIterator(boolean)
     */
    public Iterator getIterator(boolean inCacheOnly) {
        return new CompositeIterator(innerStack.iterator(),innerQ.getIterator(inCacheOnly));
    }

    /* (non-Javadoc)
     * @see org.archive.util.Queue#deleteMatchedItems(org.archive.util.QueueItemMatcher)
     */
    public long deleteMatchedItems(QueueItemMatcher matcher) {
        // Delete from inner queue
        long numberOfDeletes = innerQ.deleteMatchedItems(matcher);
        // Then delete from inner stack
        Iterator it = innerStack.iterator();
        while(it.hasNext()){
            if(matcher.match(it.next())){
                it.remove();
                numberOfDeletes++;
            }
        }
        // return total deleted
        return numberOfDeletes;
    }

    /**
     * @return
     */
    public Object getInProcessItem() {
       return inProcessItem;
    }

    /**
     * @param curi
     */
    public void setInProcessItem(CrawlURI curi) {
        inProcessItem = curi;
    }

    /**
     * @param curi
     */
    public void enqueueMedium(CrawlURI curi) {
        innerStack.addLast(curi);
    }
    /**
     * @param curi
     */
    public void enqueueHigh(CrawlURI curi) {
        innerStack.addFirst(curi);
    }

    /**
     * @return
     */
    public Object peek() {
        if(!innerStack.isEmpty()) {
            return innerStack.getFirst();
        }
        if(!innerQ.isEmpty()) {
            return innerQ.peek();
        }
        return null;
    }

}
