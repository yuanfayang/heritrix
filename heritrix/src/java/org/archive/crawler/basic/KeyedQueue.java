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
public class KeyedQueue implements Queue {
    private static Logger logger = Logger.getLogger("org.archive.crawler.basic.KeyedQueue");

    /** INACTIVE: not considered as URI source until activated by policy */
    public static final Object INACTIVE = "INACTIVE".intern();
    /** READY: eligible and able to supply a new work URI on demand */
    public static final Object READY = "READY".intern();
    /** FROZEN: not considered as URI source until operator intervention */
    public static final Object FROZEN = "FROZEN".intern();
    /** IN_PROCESS: on hold until a URI in progress is finished */
    public static final Object IN_PROCESS = "IN_PROCESS".intern();
    /** SNOOZED: on hold until a specific time interval has passed */
    public static final Object SNOOZED = "SNOOZED".intern();
    /** EMPTY: eligible to supply URIs, but without any to supply */
    public static final Object EMPTY = "EMPTY".intern();
    /** FINISHED: discarded because empty (not irreversible) */
    public static final Object FINISHED = "FINISHED".intern();

    long wakeTime;
    String classKey;
    Object state;

    Object inProcessItem;
    
    LinkedList innerStack; // topmost items
    Queue innerQ;
    
    Queue frozenQ; // put-to-side items

    /**
     * @param key A unique identifier used to distingush files related to this
     *           objects disk based data structures (will be a part of their
     *           file name, must therefor be a legal filename).
     * @param scratchDir Directory where disk based data structures will be
     *           created.
     * @param headMax Maximum number of items to keep in memory (excluding
     *           those that have been enqueuedMedium or enqueuedHigh).
     * @throws IOException When it fails to create disk based data structures.
     */
    public KeyedQueue(String key, File scratchDir, int headMax) throws IOException {
        super();
        classKey = key;
        String tmpName = null;
        if (key instanceof String) {
            tmpName = (String) key;
        }
        innerStack = new LinkedList();
        innerQ = new DiskBackedQueue(scratchDir,tmpName,headMax);
        // frozenQ = new DiskBackedQueue(scratchDir,tmpName+".frozen",headMax);
    }

    /**
     * @return
     */
    public boolean isReady() {
        return System.currentTimeMillis() > wakeTime;
    }

    /**
     * @return Object
     */
    public String getClassKey() {
        return classKey;
    }


    /** (non-Javadoc)
     * @return
     */
    public Object getStoreState() {
        return state;
    }

    /** (non-Javadoc)
     * @param s
     */
    public void setStoreState(Object s) {
        state=s;
    }

    /** (non-Javadoc)
     * @return
     */
    public long getWakeTime() {
        return wakeTime;
    }

    /** (non-Javadoc)
     * @param w
     */
    public void setWakeTime(long w) {
        wakeTime = w;
    }

    /** (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "KeyedQueue[classKey="+getClassKey()+"]";
    }

    /** (non-Javadoc)
     * @return
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

    /** (non-Javadoc)
     * @see org.archive.util.Queue#enqueue(java.lang.Object)
     */
    public void enqueue(Object o) {
        innerQ.enqueue(o);
    }

    /** (non-Javadoc)
     * @see org.archive.util.Queue#isEmpty()
     */
    public boolean isEmpty() {
        return innerStack.isEmpty() && innerQ.isEmpty();
        // return innerStack.isEmpty() && innerQ.isEmpty() && frozenQ.isEmpty();
    }

    /** (non-Javadoc)
     * @see org.archive.util.Queue#dequeue()
     */
    public Object dequeue() {
        if (!innerStack.isEmpty()) {
            return innerStack.removeFirst();
        }
        return innerQ.dequeue();
    }

    /** (non-Javadoc)
     * @see org.archive.util.Queue#length()
     */
    public long length() {
        return innerQ.length()+innerStack.size();
    }

    /**
     *
     */
    public void release() {
        innerQ.release();
    }

    /** (non-Javadoc)
     * @see org.archive.util.Queue#getIterator(boolean)
     */
    public Iterator getIterator(boolean inCacheOnly) {
        return new CompositeIterator(innerStack.iterator(),innerQ.getIterator(inCacheOnly));
    }

    /** (non-Javadoc)
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
     * @param curi
     */
    public void enqueueFrozen(CrawlURI curi) {
        // frozenQ.enqueue(curi);
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
