/* DiskBackedQueue
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
import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Logger;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;


/**
 * Queue which uses a DiskQueue ('tailQ') for spillover entries once a
 * in-memory LinkedList ('headQ') reaches a maximum size.
 *
 * @author Gordon Mohr
 */
public class DiskBackedQueue implements Queue, Serializable {
    private static Logger logger = 
        Logger.getLogger(DiskBackedQueue.class.getName());

    protected int headMax;
    protected LinkedList headQ;
    protected DiskQueue tailQ;
    protected String name;

    /**
     * @param dir
     * @param name
     * @param reuse whether to reuse any existing backing files
     * @param headMax
     * @throws IOException
     *
     */
    public DiskBackedQueue(File dir, String name, boolean reuse, int headMax)
            throws IOException {
        this.headMax = headMax;
        this.name = name;
        this.headQ = new LinkedList();
        this.tailQ = new DiskQueue(dir, name, reuse);
    }

    /**
     * @see org.archive.util.Queue#enqueue(java.lang.Object)
     */
    public void enqueue(Object o) {
        logger.finest(name+"("+length()+"): "+o);
        if (length()<headMax) {
            fillHeadQ();
            headQ.addLast(o);
        } else {
            tailQ.enqueue(o);
        }
    }

    /**
     * @see org.archive.util.Queue#isEmpty()
     */
    public boolean isEmpty() {
        return length()==0;
    }

    /**
     * @see org.archive.util.Queue#dequeue()
     */
    public Object dequeue() {
        Object retObj = null;
        if (headQ.isEmpty()) {
            // batch fill head if possible
            fillHeadQ();
        }
        if (headQ.isEmpty()) {
            // if still no memory head, get from backing
            retObj = backingDequeue();
        } else {
            // get from memory head where possible
            retObj = headQ.removeFirst();
        }
        logger.finest(name+"("+length()+"): "+retObj);
        backingUpdate();
        return retObj;
    }

    protected void backingUpdate() {
        if(length()<=headMax/4 && tailQ.isInitialized()){
      		// Currently less then a quarter of what can fit in the memory cache
            // is left in the queue. Flush out the items on disk and close the
            // files to free up file handles.
        	if(tailQ.isEmpty() == false){
      			fillHeadQ();
            }
            if(tailQ.isEmpty()){
                tailQ.release();
            }
        }
    }

    protected void fillHeadQ() {
        while (headQ.size()<headTargetSize() && headQ.size()<length()) {
            headQ.addLast(backingDequeue());
        }
     }

    /**
     * @return
     */
    protected Object backingDequeue() {
        return tailQ.dequeue();
    }

    /**
     * @return
     */
    protected int headTargetSize() {
        return headMax;
    }

    /**
     * @see org.archive.util.Queue#length()
     */
    public long length() {
        return headQ.size()+tailQ.length();
    }

    /**
     * @see org.archive.util.Queue#release()
     */
    public void release() {
        tailQ.release();
    }

    /**
     * @see org.archive.util.Queue#peek()
     */
    public Object peek() {
    	if(headQ.isEmpty()){
    		fillHeadQ();
    	} 
    	return headQ.getFirst();
    }

    /**
     * @see org.archive.util.Queue#getIterator(boolean)
     */
    public Iterator getIterator(boolean inCacheOnly) {
        if(inCacheOnly){
            // The headQ is a memory based structure and
            // that the tailQ is a disk based structure
            return headQ.iterator();
        } else {
            // Create and return a composite iterator over the two queues.
            Iterator it = new CompositeIterator(
                    headQ.iterator(),
                    tailQ.getIterator(false));
            
            return it;
        }
    }

    /**
     * @see org.archive.util.Queue#deleteMatchedItems(org.apache.commons.collections.Predicate)
     */
    public long deleteMatchedItems(Predicate matcher) {
        long numberOfDeletes = 0;
        long oldSize = headQ.size();
        CollectionUtils.filter(headQ,new Inverter(matcher));
        numberOfDeletes += oldSize-headQ.size();
        numberOfDeletes += tailQ.deleteMatchedItems(matcher);
        return numberOfDeletes;
    }

    /**
     * Set the maximum number of items to keep in memory
     * at the structure's top. If more than that number are
     * already in memory, they will remain in memory until 
     * dequeued, and thereafter the max will not be exceeded.
     * 
     * @param hm
     */
    public void setHeadMax(int hm) {
        headMax = hm;
    }

}
