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
package org.archive.crawler.frontier;

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
 * Ordered collection of work items with the same "classKey". The
 * collection itself has a state, which may reflect where it
 * is stored or what can be done with the contained items.
 * 
 * For easy access to several locations in the main collection,
 * it is held between 2 data structures: a top stack and a 
 * bottom queue. (These in turn may be disk-backed.)
 * 
 * Also maintains a collection 'off to the side' of 'frozen'
 * items. 
 *
 * About KeyedQueue states:
 * 
 * All KeyedQueues begin INACTIVE. A call to activate() will 
 * render them READY (if not empty of eligible URIs) or EMPTY
 * otherwise. 
 * 
 * A noteInProcess() puts the KeyedQueue into IN_PROCESS state. 
 * A matching noteProcessDone() puts the KeyedQueue bank into 
 * READY or EMPTY. 
 * 
 * A freeze() may be issued to any READY or EMPTY queue to 
 * put it into FROZEN state. Only an unfreeze() will move 
 * the queue to INACTIVE state. 
 * 
 * A deactivate() may be issued to any READY or EMPTY queue
 * to put it into INACTIVE state. 
 * 
 * A snooze() may be issued to any READY or EMPTY queue to 
 * put it into SNOOZED state.
 * 
 * A discard() may be issued to any EMPTY queue to put it into
 * the DISCARDED state. A queue never leaves the discarded state;
 * if a queue of its hostname is needed again, a new one is created.
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
    public static final Object DISCARDED = "FINISHED".intern();

    /** ms time to wake, if snoozed */
    long wakeTime;
    /** common string 'key' of included items (typically hostname)  */
    String classKey;
    /** current state; see above values */
    Object state;

    /** if state is IN_PROCESS, item in progress */
    Object inProcessItem;
    
    LinkedList innerStack; // topmost eligible items
    Queue innerQ; // rest of eligible items
    
    Queue frozenQ; // put-to-side items; not returned from normal accessors

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
        frozenQ = new DiskBackedQueue(scratchDir,tmpName+".frozen",headMax);
        state = INACTIVE;
    }

    /**
     * The 'classKey' identifier common to items in this queue
     * @return Object
     */
    public String getClassKey() {
        return classKey;
    }

    /** 
     * @return The state of this queue.
     */
    public Object getState() {
        return state;
    }

//
// STATE TRANSITIONS
//
    /**
     * Move queue from INACTIVE to ACTIVE state
     */
    public void activate() {
        assert state == INACTIVE;
        state = isEmpty() ? EMPTY : READY;
    }
    /**
     * Move queue from READY or EMPTY state to INACTIVE
     */
    public void deactivate() {
        assert state == READY || state == EMPTY;
        state = INACTIVE;
    }
    /**
     * Move queue from READY or EMPTY state to FROZEN
     */
    public void freeze() {
        assert state == READY || state == EMPTY;
        state = FROZEN;
    }
    /**
     * Move queue from FROZEN state to INACTIVE
     */
    public void unfreeze() {
        assert state == FROZEN;
        state = INACTIVE;
    }
    /**
     * Move queue from READY or EMPTY state to SNOOZED
     */
    public void snooze() {
        assert state == READY || state == EMPTY;
        state = SNOOZED;
    }
    /**
     * Move queue from SNOOZED state to READY or EMPTY
     */
    public void wake() {
        assert state == SNOOZED;
        state = isEmpty() ? EMPTY : READY;
    }
    /**
     * Move queue from READY or EMPTY to DISCARDED
     */
    public void discard() {
        assert state == READY || state == EMPTY;
        state = DISCARDED;
    }
    /**
     * Note that the given item is 'in process';
     * move queue from READY or EMPTY to IN_PROCESS
     * and remember in-process item.
     * 
     * @param o
     */
    public void noteInProcess(Object o) {
        assert state == READY || state == EMPTY;
        assert inProcessItem == null;
        inProcessItem = o;
        state = IN_PROCESS;
    }
    /**
     * Note that the given item's processing
     * has completed; forget the in-process item
     * and move queue from IN_PROCESS to READY or 
     * EMPTY state
     * 
     * @param o
     */
    public void noteProcessDone(Object o) {
        assert state == IN_PROCESS;
        assert inProcessItem == o;
        inProcessItem = null;
        state = isEmpty() ? EMPTY : READY;
    }
    /**
     * Update READY/EMPTY state after preceding
     * queue edit operations.
     * 
     * @return true if state changed, false otherwise
     */
    public boolean checkEmpty() {
        // update READY|EMPTY state after recent relevant changes
        if (! (state == READY || state == EMPTY) ) {
            // only relevant for active states
            return false;
        }
        Object previous = state;
        state = isEmpty() ? EMPTY : READY;
        return state != previous;
    }
    
//
// SCHEDULING SUPPORT
//
    
    /** 
     * @return Ttime to wake, when snoozed
     */
    public long getWakeTime() {
        return wakeTime;
    }

    /**
     * @param w time to wake, when snoozed
     */
    public void setWakeTime(long w) {
        wakeTime = w;
    }

    /** 
     * To ensure total and consistent ordering when 
     * in scheduled order, a fallback sort criterion
     * @return Fallback sort.
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

    /** 
     * Add an item in the default manner
     * 
     * @see org.archive.util.Queue#enqueue(java.lang.Object)
     */
    public void enqueue(Object o) {
        innerQ.enqueue(o);
    }

    /** 
     * Is this KeyedQueue empty of ready-to-try URIs. (NOTE: may
     * still have 'frozen' off-to-side URIs.)
     * 
     * @see org.archive.util.Queue#isEmpty()
     */
    public boolean isEmpty() {
        return innerStack.isEmpty() && innerQ.isEmpty();
        // return innerStack.isEmpty() && innerQ.isEmpty() && frozenQ.isEmpty();
    }

    /** 
     * Remove an item in the default manner
     * 
     * @see org.archive.util.Queue#dequeue()
     */
    public Object dequeue() {
        if (!innerStack.isEmpty()) {
            return innerStack.removeFirst();
        }
        return innerQ.dequeue();
    }

    /** 
     * Total number of available items. (Does not include
     * any 'frozen' items.)
     * 
     * @see org.archive.util.Queue#length()
     */
    public long length() {
        return innerQ.length()+innerStack.size();
    }

    /** 
     * @return Total number of 'frozen' items. 
     */
    public long frozenLength() {
        return innerQ.length()+innerStack.size();
    }
    
    /**
     * Release any external resources (eg open files) which
     * may be held.
     */
    public void release() {
        innerQ.release();
        frozenQ.release();
    }

    /** 
     * Iterate over all available (non-frozen) items. 
     * 
     * @see org.archive.util.Queue#getIterator(boolean)
     */
    public Iterator getIterator(boolean inCacheOnly) {
        return new CompositeIterator(innerStack.iterator(),innerQ.getIterator(inCacheOnly));
    }

    /** 
     * Delete items matching the supplied criterion. 
     * 
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
     * @return The remembered item in process (set with noteInProgress()).
     */
    public Object getInProcessItem() {
       return inProcessItem;
    }

    /**
     * enqueue at a middle location (ahead of 'most'
     * items, but behind any recent 'enqueueHigh's
     * 
     * @param curi
     */
    public void enqueueMedium(CrawlURI curi) {
        innerStack.addLast(curi);
    }
    /**
     * enqueue ahead of everything else
     * 
     * @param curi
     */
    public void enqueueHigh(CrawlURI curi) {
        innerStack.addFirst(curi);
    }

    /**
     * enqueue to the 'frozen' set-aside queue,
     * which holds items indefinitely (only 
     * operator action returns them to availability)
     * 
     * @param curi
     */
    public void enqueueFrozen(CrawlURI curi) {
        frozenQ.enqueue(curi);
    }
    
    /**
     * Return, without removing, the top available item.
     * 
     * @return The top available item.
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

    /**
     * May this KeyedQueue be completely discarded. 
     *
     * It may be discarded only if empty of available and frozen items, and
     * not SNOOZED or FROZEN (which implies state info which would be lost if
     * discarded).
     * 
     * @return True if discardable.
     */
    public boolean isDiscardable() {
        return isEmpty() && frozenQ.isEmpty() && state == EMPTY;
    }
}
