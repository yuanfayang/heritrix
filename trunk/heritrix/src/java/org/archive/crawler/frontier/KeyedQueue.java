/* KeyedQueue
 * 
 * $Id$
 * 
 * Created on May 29, 2003
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
package org.archive.crawler.frontier;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.util.CompositeIterator;
import org.archive.util.DiskBackedQueue;
import org.archive.util.Queue;
import org.archive.util.QueueItemMatcher;

/**
 * Ordered collection of work items with the same "classKey". 
 * 
 * The collection itself has a state, which may reflect where it
 * is stored or what can be done with the contained items.
 * 
 * <p>For easy access to several locations in the main collection,
 * it is held between 2 data structures: a top stack and a 
 * bottom queue. (These in turn may be disk-backed.)
 * 
 * <p>Also maintains a collection 'off to the side' of 'frozen'
 * items. 
 *
 * <p>About KeyedQueue states:
 * 
 * <p>All KeyedQueues begin INACTIVE. A call to activate() will 
 * render them READY (if not empty of eligible URIs) or EMPTY
 * otherwise. 
 * 
 * <p>A noteInProcess() puts the KeyedQueue into IN_PROCESS state. 
 * A matching noteProcessDone() puts the KeyedQueue bank into 
 * READY or EMPTY. 
 * 
 * <p>A freeze() may be issued to any READY or EMPTY queue to 
 * put it into FROZEN state. Only an unfreeze() will move 
 * the queue to INACTIVE state. 
 * 
 * <p>A deactivate() may be issued to any READY or EMPTY queue
 * to put it into INACTIVE state. 
 * 
 * <p>A snooze() may be issued to any READY or EMPTY queue to 
 * put it into SNOOZED state.
 * 
 * <p>A discard() may be issued to any EMPTY queue to put it into
 * the DISCARDED state. A queue never leaves the discarded state;
 * if a queue of its hostname is needed again, a new one is created.
 * 
 * @author gojomo
 * @version $Date$ $Revision$
 */
public class KeyedQueue implements Queue {
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
    
    /**
     * Put-to-side items; not returned from normal accessors.
     */
    Queue frozenQ = null;

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
    public KeyedQueue(String key, File scratchDir, int headMax)
            throws IOException {
        super();
        this.classKey = key;
        String tmpName = key;
        this.innerStack = new LinkedList();
        this.innerQ = new DiskBackedQueue(scratchDir,tmpName,headMax);
        // TODO: Currently unimplemented.  Commenting out for now because its
        // presence means extra two file descriptors per processed URI.
        // See https://sourceforge.net/tracker/?func=detail&aid=943768&group_id=73833&atid=539099
        // this.frozenQ =
        //  new DiskBackedQueue(scratchDir,tmpName+".frozen",headMax);
        this.state = INACTIVE;
    }

    /**
     * The 'classKey' identifier common to items in this queue
     * @return Object
     */
    public String getClassKey() {
        return this.classKey;
    }

    /** 
     * @return The state of this queue.
     */
    public Object getState() {
        return this.state;
    }

//
// STATE TRANSITIONS
//
    /**
     * Move queue from INACTIVE to ACTIVE state
     */
    public void activate() {
        assert this.state == INACTIVE;
        this.state = isEmpty() ? EMPTY : READY;
    }
    /**
     * Move queue from READY or EMPTY state to INACTIVE
     */
    public void deactivate() {
        assert this.state == READY || this.state == EMPTY;
        this.state = INACTIVE;
    }
    /**
     * Move queue from READY or EMPTY state to FROZEN
     */
    public void freeze() {
        assert this.state == READY || this.state == EMPTY;
        this.state = FROZEN;
    }
    /**
     * Move queue from FROZEN state to INACTIVE
     */
    public void unfreeze() {
        assert this.state == FROZEN;
        this.state = INACTIVE;
    }
    /**
     * Move queue from READY or EMPTY state to SNOOZED
     */
    public void snooze() {
        assert this.state == READY || this.state == EMPTY;
        this.state = SNOOZED;
    }
    /**
     * Move queue from SNOOZED state to READY or EMPTY
     */
    public void wake() {
        assert this.state == SNOOZED;
        this.state = isEmpty() ? EMPTY : READY;
    }
    /**
     * Move queue from READY or EMPTY to DISCARDED
     */
    public void discard() {
        assert this.state == READY || this.state == EMPTY;
        this.state = DISCARDED;
    }
    /**
     * Note that the given item is 'in process';
     * move queue from READY or EMPTY to IN_PROCESS
     * and remember in-process item.
     * 
     * @param o
     */
    public void noteInProcess(Object o) {
        assert this.state == READY || this.state == EMPTY;
        assert this.inProcessItem == null;
        this.inProcessItem = o;
        this.state = IN_PROCESS;
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
        assert this.state == IN_PROCESS;
        assert this.inProcessItem == o;
        this.inProcessItem = null;
        this.state = isEmpty() ? EMPTY : READY;
    }
    /**
     * Update READY/EMPTY state after preceding
     * queue edit operations.
     * 
     * @return true if state changed, false otherwise
     */
    public boolean checkEmpty() {
        // update READY|EMPTY state after recent relevant changes
        if (! (this.state == READY || this.state == EMPTY) ) {
            // only relevant for active states
            return false;
        }
        Object previous = this.state;
        this.state = isEmpty() ? EMPTY : READY;
        return this.state != previous;
    }
    
//
// SCHEDULING SUPPORT
//
    /** 
     * @return Time to wake, when snoozed
     */
    public long getWakeTime() {
        return this.wakeTime;
    }

    /**
     * @param w time to wake, when snoozed
     */
    public void setWakeTime(long w) {
        this.wakeTime = w;
    }

    /** 
     * To ensure total and consistent ordering when 
     * in scheduled order, a fallback sort criterion
     * @return Fallback sort.
     */
    public String getSortFallback() {
        return this.classKey.toString();
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
        this.innerQ.enqueue(o);
    }

    /** 
     * Is this KeyedQueue empty of ready-to-try URIs. (NOTE: may
     * still have 'frozen' off-to-side URIs.)
     * 
     * @see org.archive.util.Queue#isEmpty()
     */
    public boolean isEmpty() {
        return this.innerStack.isEmpty() && this.innerQ.isEmpty();
        // return innerStack.isEmpty() && innerQ.isEmpty() && frozenQ.isEmpty();
    }

    /** 
     * Remove an item in the default manner
     * 
     * @see org.archive.util.Queue#dequeue()
     */
    public Object dequeue() {
        if (!this.innerStack.isEmpty()) {
            return this.innerStack.removeFirst();
        }
        return this.innerQ.dequeue();
    }

    /** 
     * Total number of available items. (Does not include
     * any 'frozen' items.)
     * 
     * @see org.archive.util.Queue#length()
     */
    public long length() {
        return this.innerQ.length() + this.innerStack.size();
    }

    /** 
     * @return Total number of 'frozen' items. 
     */
    public long frozenLength() {
        return this.innerQ.length() + this.innerStack.size();
    }
    
    /**
     * Release any external resources (eg open files) which
     * may be held.
     */
    public void release() {
        this.innerQ.release();
        if (this.frozenQ != null) {
            this.frozenQ.release();
        }
    }

    /** 
     * Iterate over all available (non-frozen) items. 
     * 
     * @see org.archive.util.Queue#getIterator(boolean)
     */
    public Iterator getIterator(boolean inCacheOnly) {
        return new CompositeIterator(this.innerStack.iterator(),
            this.innerQ.getIterator(inCacheOnly));
    }

    /** 
     * Delete items matching the supplied criterion. 
     * 
     * @see org.archive.util.Queue#deleteMatchedItems(org.archive.util.QueueItemMatcher)
     */
    public long deleteMatchedItems(QueueItemMatcher matcher) {
        // Delete from inner queue
        long numberOfDeletes = this.innerQ.deleteMatchedItems(matcher);
        // Then delete from inner stack
        Iterator it = this.innerStack.iterator();
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
       return this.inProcessItem;
    }

    /**
     * enqueue at a middle location (ahead of 'most'
     * items, but behind any recent 'enqueueHigh's
     * 
     * @param curi
     */
    public void enqueueMedium(CrawlURI curi) {
        this.innerStack.addLast(curi);
    }
    /**
     * enqueue ahead of everything else
     * 
     * @param curi
     */
    public void enqueueHigh(CrawlURI curi) {
        this.innerStack.addFirst(curi);
    }

    /**
     * enqueue to the 'frozen' set-aside queue,
     * which holds items indefinitely (only 
     * operator action returns them to availability)
     * 
     * @param curi
     */
    public void enqueueFrozen(CrawlURI curi) {
        if (this.frozenQ != null) {
            this.frozenQ.enqueue(curi);
        }
    }
    
    /**
     * Return, without removing, the top available item.
     * 
     * @return The top available item.
     */
    public Object peek() {
        if(!this.innerStack.isEmpty()) {
            return this.innerStack.getFirst();
        }
        if(!this.innerQ.isEmpty()) {
            return this.innerQ.peek();
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
        return isEmpty() &&
            ((this.frozenQ != null)? this.frozenQ.isEmpty(): true) &&
                this.state == EMPTY;
    }
}
