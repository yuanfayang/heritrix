/* URIWorkQueue
 *
 * $Id$
 *
 * Created on May 24, 2004
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
package org.archive.crawler.frontier;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.Predicate;
import org.archive.crawler.datamodel.CrawlURI;

/**
 * A single queue of related URIs to visit. Typically grouped
 * by hostname:port. 
 * 
 * @author gojomo
 */
public interface URIWorkQueue {

    // states
    /** INACTIVE: not considered as URI source until activated by policy */
    public static final Object INACTIVE = "INACTIVE".intern();

    /** READY: eligible and able to supply a new work URI on demand */
    public static final Object READY = "READY".intern();

    /** FROZEN: not considered as URI source until operator intervention */
    public static final Object FROZEN = "FROZEN".intern();

    /** BUSY: on hold until one or more URIs in progress are finished */
    public static final Object BUSY = "BUSY".intern();

    /** SNOOZED: on hold until a specific time interval has passed */
    public static final Object SNOOZED = "SNOOZED".intern();

    /** EMPTY: eligible to supply URIs, but without any to supply */
    public static final Object EMPTY = "EMPTY".intern();

    /** DISCARDED: discarded because empty (not irreversible) */
    public static final Object DISCARDED = "DISCARDED".intern();

//
// basic state & size info 
//
    /**
     * The 'classKey' identifier common to items in this queue
     * @return Object
     */
    public abstract String getClassKey();

    /** 
     * @return The state of this queue.
     */
    public abstract Object getState();

    /** 
     * @return Is this KeyedQueue empty of ready-to-try URIs. (NOTE: may
     * still have 'frozen' off-to-side URIs.)
     */
    public abstract boolean isEmpty();

    /** 
     * @see org.archive.util.Queue#length()
     * @return Total number of available items. (Does not include
     * any 'frozen' items.)
     */
    public abstract long length();

//
//  STATE TRANSITIONS
//
    /**
     * Move queue from INACTIVE to ACTIVE state
     */
    public abstract void activate();

    /**
     * Move queue from READY or EMPTY state to INACTIVE
     */
    public abstract void deactivate();

    /**
     * Move queue from READY or EMPTY state to FROZEN
     */
    public abstract void freeze();

    /**
     * Move queue from FROZEN state to INACTIVE
     */
    public abstract void unfreeze();

    /**
     * Move queue from READY or EMPTY state to SNOOZED
     */
    public abstract void snooze();

    /**
     * Move queue from SNOOZED state to READY or EMPTY
     */
    public abstract void wake();

    /**
     * Move queue from READY or EMPTY to DISCARDED
     */
    public abstract void discard();

    /**
     * Update READY/EMPTY state after preceding
     * queue edit operations.
     * 
     * @return true if state changed, false otherwise
     */
    public abstract boolean checkEmpty();

//
// in-process item tracking 
//
    /**
     * Note that the given item is 'in process';
     * move queue from READY or EMPTY to BUSY
     * if appropriate and remember in-process item.
     * 
     * @param o
     */
    public abstract void noteInProcess(CrawlURI o);

    /**
     * Note that the given item's processing
     * has completed; forget the in-process item
     * and move queue from BUSY to READY or 
     * EMPTY state if appropriate
     * 
     * @param o
     */
    public abstract void noteProcessDone(CrawlURI o);

    /**
     * @return The remembered items in process (set with noteInProgress()).
     */
    public abstract List getInProcessItems();

    /**
     * Set 'valence', the number of simultaneous items to
     * allow in process before becoming BUSY 
     * @param v
     */
    public abstract void setValence(int v);
 
//
// scheduling support
//
    /**
     * @return time when queue should wake
     */
    public abstract long getWakeTime();

    /**
     * @param w time to wake, when snoozed
     */
    public abstract void setWakeTime(long w);

    /** 
     * To ensure total and consistent ordering when 
     * in scheduled order, a fallback sort criterion
     * @return Fallback sort.
     */
    public abstract String getSortFallback();

//
// queueing operations
//
    /** 
     * Add an item in the default manner
     * 
     * @param curi
     */
    public abstract void enqueue(CrawlURI curi);

    /** 
     * Remove an item in the default manner
     * 
     * @return Item removed.
     */
    public abstract CrawlURI dequeue();

    /**
     * @return the last enqueued URI; useful for
     * assessing queue state.
     */
    public String getLastQueued();

    /**
     * @return the last dequeued URI; useful
     * for assessing queue state.
     */
    public String getLastDequeued();

// 
// resource management
//
    /**
     * Set maximum number of items to hold in memory.
     * @param load
     */
    public abstract void setMaximumMemoryLoad(int load);
    
    /**
     * @return current memory load (items in memory rather
     * than on disk).
     */
    public int memoryLoad();
    
    /**
     * May this KeyedQueue be completely discarded. 
     *
     * It may be discarded only if empty of available and frozen items, and
     * not SNOOZED or FROZEN (which implies state info which would be lost if
     * discarded).
     * 
     * @return True if discardable.
     */
    public abstract boolean isDiscardable();

//
// queue editting
//
    /** 
     * Iterate over all available (non-frozen) items. 
     * 
     * @param inCacheOnly
     * @see org.archive.util.Queue#getIterator(boolean)
     * @return An iterator.
     */
    public abstract Iterator getIterator(boolean inCacheOnly);

    /** 
     * Delete items matching the supplied criterion. 
     * 
     * @param matcher
     * @see org.archive.util.Queue#deleteMatchedItems(org.apache.commons.collections.Predicate)
     * @return Count of items deleted.
     */
    public abstract long deleteMatchedItems(Predicate matcher);
}
