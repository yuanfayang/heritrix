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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.Predicate;
import org.archive.crawler.datamodel.CrawlServer;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.queue.TieredQueue;
import org.archive.util.ArchiveUtils;

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
public class KeyedQueue implements Serializable, URIWorkQueue  {
    // be robust against trivial implementation changes
    private static final long serialVersionUID = ArchiveUtils.classnameBasedUID(KeyedQueue.class,1);
    
    /** Associated CrawlServer instance, held to keep CrawlServer from being cache-flushed */
    CrawlServer crawlServer;
    
    /** ms time to wake, if snoozed */
    long wakeTime;
    /** common string 'key' of included items (typically hostname)  */
    String classKey;
    /** current state; see above values */
    Object state;
    /** maximum simultaneous plain URIs to allow in-process at a time */
    int valence = 1;

    /** items in progress */
    ArrayList inProcessItems = new ArrayList();
    int inProcessLoad = 0;
    
    TieredQueue innerQ;

    // useful for reporting
    private String lastQueued; // last URI enqueued
    private String lastDequeued; // last URI dequeued

    /**
     * @param key A unique identifier used to distingush files related to this
     *           objects disk based data structures (will be a part of their
     *           file name, must therefor be a legal filename).
     * @param scratchDir Directory where disk based data structures will be
     *           created.
     * @param maxMemLoad Maximum number of items to keep in memory
     * @throws IOException When it fails to create disk based data structures.
     */
    public KeyedQueue(String key, CrawlServer server, File scratchDir, int maxMemLoad)
            throws IOException {
        super();
        this.classKey = key;
        if(server!=null && !server.getName().startsWith(key)) {
            // temp debugging output
            System.err.println("KeyedQueue server<->key mismatch noted: "+server.getName()+"<->"+key);
            // assert server.getHostname().startsWith(key) : "KeyedQueue server - key mismatch";
        }
        this.crawlServer = server;
        String tmpName = key;
        this.innerQ = new TieredQueue(3);
        this.innerQ.initializeDiskBackedQueues(scratchDir,tmpName,300);
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
        assert this.state == READY || this.state == EMPTY : "bad state for queue about to be snoozed: "+ this.state;
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
        assert this.state == READY || this.state == EMPTY : "discarding queue in bad state";
        assert inProcessItems.isEmpty() : "discarding busy queue";
        this.state = DISCARDED;
    }
    /**
     * Note that the given item is 'in process';
     * move queue from READY or EMPTY to IN_PROCESS
     * and remember in-process item.
     *
     * @param o
     */
    public void noteInProcess(CrawlURI o) {
        assert this.state == READY || this.state == EMPTY : 
            "unexpected state " + this.state;
        inProcessItems.add(o);
        inProcessLoad += loadFor(o);
        if (inProcessLoad >= valence) {
            this.state = BUSY;
        } 
    }

    /**
     * Note that the given item's processing
     * has completed; forget the in-process item
     * and move queue from BUSY or READY to 
     * READY or EMPTY state if necessary
     *
     * @param o
     */
    public void noteProcessDone(CrawlURI o) {
        // assert this.state == BUSY : "unexpected state "+ this.state;
        assert inProcessItems.contains(o);
        inProcessItems.remove(o);
        inProcessLoad -= loadFor(o);
        if(inProcessLoad<valence) {
            this.state = isEmpty() ? EMPTY : READY;
        }
    }

    /**
     * Give the 'load' associated with the given CrawlURI.
     * Usually 1.
     * 
     * @param o
     * @return
     */
    private int loadFor(CrawlURI o) {
        if (o.needsImmediateScheduling()) {
            // treat anything high-prio as
            // blocking all others
            return valence;
        } else {
            // otherwise, it's just normal
            return 1;
        }
    }

    /**
     * Update READY/EMPTY state after preceding
     * queue edit operations.
     *
     * @return true if state changed, false otherwise
     */
    public boolean checkEmpty() {
        // update READY|EMPTY state after recent relevant changes
        if (! (this.state == READY || this.state == EMPTY ) ) {
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
     * Should take care not to mutate this value while
     * queue is inside a sorted queue.
     * 
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
     * @param curi
     * @see org.archive.queue.Queue#enqueue(java.lang.Object)
     */
    public void enqueue(CrawlURI curi) {
     
        if(curi.needsImmediateScheduling()) {
            innerQ.enqueue(curi,0);
        } else if (curi.needsSoonScheduling()) {
            innerQ.enqueue(curi,1);
        } else {
            innerQ.enqueue(curi,2);
        }
        lastQueued = curi.getURIString();
    }

    /**
     * @see org.archive.queue.Queue#isEmpty()
     * @return Is this KeyedQueue empty of ready-to-try URIs. (NOTE: may
     * still have 'frozen' off-to-side URIs.)
     */
    public boolean isEmpty() {
        return this.innerQ.isEmpty();
   }

    /**
     * Remove an item in the default manner
     *
     * @see org.archive.queue.Queue#dequeue()
     * @return A crawl uri.
     */
    public CrawlURI dequeue() {
        CrawlURI candidate = (CrawlURI) this.innerQ.dequeue();
        lastDequeued = candidate.getURIString();
        return candidate;
    }

    /** 
     * @see org.archive.queue.Queue#length()
     * @return Total number of available items. (Does not include
     * any 'frozen' items.)
     */
    public long length() {
        return this.innerQ.length();
    }
    
    /** 
     * Iterate over all available (non-frozen) items. 
     * 
     * @param inCacheOnly
     * @see org.archive.queue.Queue#getIterator(boolean)
     * @return Iterator.
     */
    public Iterator getIterator(boolean inCacheOnly) {
        // TODO: consider pushing all unqueued to deque to simplify
        return this.innerQ.getIterator(inCacheOnly);
    }

    /**
     * Delete items matching the supplied criterion.
     *
     * @param matcher
     * @see org.archive.queue.Queue#deleteMatchedItems(org.apache.commons.collections.Predicate)
     * @return Number of deletes.
     */
    public long deleteMatchedItems(Predicate matcher) {
        // Delete from inner queue
        long numberOfDeletes = this.innerQ.deleteMatchedItems(matcher);
        // return total deleted
        return numberOfDeletes;
    }

    /**
     * @return The remembered item in process (set with noteInProgress()).
     */
    public List getInProcessItems() {
       return inProcessItems;
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
        return isEmpty() && this.state == EMPTY && inProcessItems.isEmpty();
    }

    /**
     * @param v
     */
    public void setValence(int v) {
        valence = v;
    }

    /**
     * @return Return the last enqueued URI; useful for
     * assessing queue state.
     */
    public String getLastQueued() {
        return lastQueued;
    }

    /**
     * @return Return the last dequeued URI; useful
     * for assessing queue state.
     */
    public String getLastDequeued() {
        return lastDequeued;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.frontier.URIWorkQueue#peek()
     */
    public CrawlURI peek() {
        return (CrawlURI) innerQ.peek();
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.frontier.URIWorkQueue#unpeek()
     */
    public void unpeek() {
        innerQ.unpeek();
    }

    /**
     * @param i
     */
    public void setMaximumMemoryLoad(int i) {
        innerQ.setMemoryResidentQueueCap(i);
    }
}
