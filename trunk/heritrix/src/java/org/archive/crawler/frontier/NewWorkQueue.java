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
import java.util.Iterator;

import org.apache.commons.collections.Predicate;
import org.archive.crawler.datamodel.CrawlServer;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.queue.TieredQueue;
import org.archive.util.ArchiveUtils;

/**
 * Ordered collection of work items with the same "classKey".
 *
 * @author gojomo
 * @version $Date$ $Revision$
 */
public class NewWorkQueue implements Serializable  {
    // be robust against trivial implementation changes
    private static final long serialVersionUID = ArchiveUtils.classnameBasedUID(NewWorkQueue.class,1);
    
    /** Associated CrawlServer instance, held to keep CrawlServer from being cache-flushed */
    CrawlServer crawlServer;
    
    /** ms time to wake, if snoozed */
    long wakeTime;
    /** common string 'key' of included items (typically hostname)  */
    String classKey;

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
    public NewWorkQueue(String key, CrawlServer server, File scratchDir, int maxMemLoad)
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
        this.innerQ.initializeDiskBackedQueues(scratchDir,tmpName);
    }

    /**
     * The 'classKey' identifier common to items in this queue
     * @return Object
     */
    public String getClassKey() {
        return this.classKey;
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
            innerQ.enqueue(curi);
        }
        lastQueued = curi.getURIString();
    }

    /**
     * @see org.archive.queue.Queue#isEmpty()
     * @return Is this KeyedQueue empty of ready-to-try URIs. (NOTE: may
     * still have 'frozen' off-to-side URIs.)
     */
    public boolean isEmpty() {
        return innerQ.isEmpty();
    }

    /**
     * Remove an item in the default manner
     *
     * @see org.archive.queue.Queue#dequeue()
     * @return A crawl uri.
     */
    public CrawlURI dequeue() {
        return (CrawlURI) innerQ.dequeue();
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
        return innerQ.getIterator(inCacheOnly);
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
        return numberOfDeletes;
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
}
