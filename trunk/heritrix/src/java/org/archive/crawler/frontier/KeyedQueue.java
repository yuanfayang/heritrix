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
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.util.ArchiveUtils;
import org.archive.util.CompositeIterator;
import org.archive.util.DiskBackedDeque;
import org.archive.util.Inverter;
import org.archive.util.Queue;

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
    
    DiskBackedDeque innerQ; // rest of eligible items
    LinkedList unqueued; // held batch of items to be queued
    int maxMemoryLoad; // total number of items to hold in RAM
    
    /**
     * Put-to-side items; not returned from normal accessors.
     */
    Queue frozenQ = null;

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
    public KeyedQueue(String key, File scratchDir, int maxMemLoad)
            throws IOException {
        super();
        this.classKey = key;
        String tmpName = key;
        this.maxMemoryLoad = maxMemLoad;
        this.innerQ = new DiskBackedDeque(scratchDir,tmpName,false,maxMemLoad);    
        this.unqueued = new LinkedList();
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
    public void noteInProcess(CrawlURI o) {
        assert this.state == READY || this.state == EMPTY;
        //assert this.inProcessItem == null;
        inProcessItems.add(o);
        inProcessLoad += loadFor(o);
        if(inProcessLoad>=valence) {
            this.state = BUSY;
        }
    }

    /**
     * Note that the given item's processing
     * has completed; forget the in-process item
     * and move queue from BUSY to READY or
     * EMPTY state
     *
     * @param o
     */
    public void noteProcessDone(CrawlURI o) {
        // assert this.state == BUSY;
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
     * @param curi
     * @see org.archive.util.Queue#enqueue(java.lang.Object)
     */
    public void enqueue(CrawlURI curi) {
        
        if(curi.needsImmediateScheduling()) {
            enqueueHigh(curi);
        } else if (curi.needsSoonScheduling()) {
            enqueueMedium(curi);
        } else {
            if(state!=INACTIVE) {
                this.innerQ.enqueue(curi);
            } else {
                // hold until connected
                this.unqueued.addLast(curi);
            } 
        }
        lastQueued = curi.getURIString();
        enforceMemoryLoad();
    }

    /**
     * Ensure the total 
     */
    private void enforceMemoryLoad() {
        if(memoryLoad()>maxMemoryLoad) {
            // empty unqueued
            enqueueUnqueued(); 
            if(state==INACTIVE) {
                // release any filehandles queueing may have opened
                innerQ.disconnect();
            }
        }
    }

    /**
     * @return
     */
    public int memoryLoad() {
        return unqueued.size()+innerQ.memoryLoad();
    }

    /**
     * enqueue at a middle location (ahead of 'most'
     * items, but behind any recent 'enqueueHigh's
     *
     * @param curi
     */
    private void enqueueMedium(CrawlURI curi) {
        this.unqueued.addLast(curi);
    }

    /**
     * enqueue ahead of everything else
     *
     * @param curi
     */
    private void enqueueHigh(CrawlURI curi) {
        this.unqueued.addFirst(curi);
    }

    /**
     * Is this KeyedQueue empty of ready-to-try URIs. (NOTE: may
     * still have 'frozen' off-to-side URIs.)
     *
     * @see org.archive.util.Queue#isEmpty()
     * @return
     */
    public boolean isEmpty() {
        return this.unqueued.isEmpty() && this.innerQ.isEmpty();
        // return innerStack.isEmpty() && innerQ.isEmpty() && frozenQ.isEmpty();
    }

    /**
     * Remove an item in the default manner
     *
     * @see org.archive.util.Queue#dequeue()
     * @return
     */
    public CrawlURI dequeue() {
        CrawlURI candidate = null;
        // first try 'unqueued' buffer
        if(!this.unqueued.isEmpty()) {
            candidate = dequeueFromUnqueued();
        }
        if (candidate == null) {
            // otherwise consult innerQ
            candidate = (CrawlURI) this.innerQ.dequeue();
        }
        if (candidate != null) {
            lastDequeued = candidate.getURIString();
        }
        return candidate;
    }

    /**
     * Consult the unqueued buffer for any CrawlURIs to 
     * dequeue. Buffer is consulted front-to-back, ensuring
     * high priority items are released first. Any normal
     * items discovered in the buffer are simply put at the
     * back of the main queue.
     * 
     * @return an eligible CrawlURI
     */
    private CrawlURI dequeueFromUnqueued() {
        CrawlURI candidate;
        while(!unqueued.isEmpty()) {
            candidate = (CrawlURI) unqueued.removeFirst();
            if(candidate.needsImmediateScheduling()|| candidate.needsSoonScheduling()) {
                return candidate; 
            } else {
                // was a normal item buffered up; just put at end of queue
                innerQ.enqueue(candidate);
            }
        }
        return null;
    }
    
    private void enqueueUnqueued() {
        CrawlURI candidate;
        while(!unqueued.isEmpty()) {
            candidate = (CrawlURI) unqueued.removeLast();
            if(candidate.needsImmediateScheduling()|| candidate.needsSoonScheduling()) {
                // push to top
                innerQ.push(candidate);
            } else {
                // enqueue to back
                innerQ.enqueue(candidate);
            }
        }
    }

    
    /** 
     * Total number of available items. (Does not include
     * any 'frozen' items.)
     *
     * @see org.archive.util.Queue#length()
     * @return
     */
    public long length() {
        return this.innerQ.length() + this.unqueued.size();
    }

    /**
     * @return Total number of 'frozen' items.
     */
    public long frozenLength() {
        return this.innerQ.length() + this.unqueued.size();
    }
    
    /** 
     * Iterate over all available (non-frozen) items. 
     * 
     * @param inCacheOnly
     * @see org.archive.util.Queue#getIterator(boolean)
     * @return
     */
    public Iterator getIterator(boolean inCacheOnly) {
        // TODO: consider pushing all unqueued to deque to simplify
        return new CompositeIterator(this.unqueued.iterator(),
            this.innerQ.getIterator(inCacheOnly));
    }

    /**
     * Delete items matching the supplied criterion.
     *
     * @param matcher
     * @see org.archive.util.Queue#deleteMatchedItems(org.apache.commons.collections.Predicate)
     * @return
     */
    public long deleteMatchedItems(Predicate matcher) {
        // Delete from inner queue
        long numberOfDeletes = this.innerQ.deleteMatchedItems(matcher);
        // TODO: consider pushing all unqueued to deque to simplify
        // Then delete from inner stack
        int presize = unqueued.size();
        CollectionUtils.filter(unqueued,new Inverter(matcher));
        numberOfDeletes += (presize - unqueued.size());
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

    // custom serialization
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        // ensure object identities of state value match
        this.state = ((String) this.state).intern();
    }

    /**
     * @param v
     */
    public void setValence(int v) {
        valence = v;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.frontier.URIWorkQueue#setMaximumMemoryLoad(int)
     */
    public void setMaximumMemoryLoad(int load) {
        maxMemoryLoad = load;
        if(state==INACTIVE) {
            innerQ.setHeadMax(0);
        } else {
            innerQ.setHeadMax(load);
        }
    }

    /**
     * Return the last enqueued URI; useful for
     * assessing queue state.
     * 
     * @return
     */
    public String getLastQueued() {
        return lastQueued;
    }

    /**
     * Return the last dequeued URI; useful
     * for assessing queue state.
     * @return
     */
    public String getLastDequeued() {
        // TODO Auto-generated method stub
        return lastDequeued;
    }
}
