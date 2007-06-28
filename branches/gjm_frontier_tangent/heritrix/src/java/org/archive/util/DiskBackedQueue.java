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
import java.util.Iterator;
import java.util.logging.Logger;


/**
 * Queue which uses a DiskQueue ('tailQ') for spillover entries once a
 * MemQueue ('headQ') reaches a maximum size.
 *
 *
 * @author Gordon Mohr
 */
public class DiskBackedQueue implements Queue {
    private static Logger logger = Logger.getLogger("org.archive.util.DiskBackedQueue");

    int headMax;
    MemQueue headQ;
    DiskQueue tailQ;
    String name;

    /**
     * @param dir
     * @param name
     * @param headMax
     * @throws IOException
     *
     */
    public DiskBackedQueue(File dir, String name, int headMax) throws IOException {
        this.headMax = headMax;
        this.name = name;
        headQ = new MemQueue();
        tailQ = new DiskQueue(dir, name);
    }

    /* (non-Javadoc)
     * @see org.archive.util.Queue#enqueue(java.lang.Object)
     */
    public void enqueue(Object o) {
        logger.finest(name+"("+length()+"): "+o);
        if (length()<headMax) {
            fillHeadQ();
            headQ.enqueue(o);
        } else {
            tailQ.enqueue(o);
        }
    }

    /* (non-Javadoc)
     * @see org.archive.util.Queue#isEmpty()
     */
    public boolean isEmpty() {
        return length()==0;
    }

    /* (non-Javadoc)
     * @see org.archive.util.Queue#dequeue()
     */
    public Object dequeue() {
        if (headQ.isEmpty()) {
            fillHeadQ();
        }
        Object o = headQ.dequeue();
        logger.finest(name+"("+length()+"): "+o);
        return o;
    }

    /**
     *
     */
    private void fillHeadQ() {
        while (headQ.length()<headMax && tailQ.length()>0) {
            headQ.enqueue(tailQ.dequeue());
        }
    }

    /* (non-Javadoc)
     * @see org.archive.util.Queue#length()
     */
    public long length() {
        return headQ.length()+tailQ.length();
    }

    /* (non-Javadoc)
     * @see org.archive.util.Queue#release()
     */
    public void release() {
        tailQ.release();
    }

    /* (non-Javadoc)
     * @see org.archive.util.Queue#peek()
     */
    public Object peek() {
         return headQ.peek();
    }

    /* (non-Javadoc)
     * @see org.archive.util.Queue#getIterator(boolean)
     */
    public Iterator getIterator(boolean inCacheOnly) {
        if(inCacheOnly){
            // The headQ is a memory based structure and
            // that the tailQ is a disk based structure
            return headQ.getIterator(true);
        } else {
            final Iterator it1 = headQ.getIterator(false);
            final Iterator it2 = tailQ.getIterator(false);
            
            // Create and return a compound iterator over the two queues.
            Iterator it = new Iterator(){
                
                private boolean lastNextWasIt1 = true;
                
                public void remove() {
                    if( lastNextWasIt1 ){
                        it1.remove();
                    } else {
                        it2.remove();
                    }
                }

                public boolean hasNext() {
                    return it1.hasNext() || it2.hasNext();
                }

                public Object next() {
                    if(it1.hasNext()){
                        // Still have something left in it1
                        return it1.next();
                    } else {
                        // Donw with it1, working on it2
                        lastNextWasIt1 = false;
                        return it2.next();
                    }
                }
            };
            
            return it;
        }
    }

    /* (non-Javadoc)
     * @see org.archive.util.Queue#deleteMatchedItems(org.archive.util.QueueItemMatcher)
     */
    public long deleteMatchedItems(QueueItemMatcher matcher) {
        long numberOfDeletes = 0;
        numberOfDeletes += headQ.deleteMatchedItems(matcher);
        numberOfDeletes += tailQ.deleteMatchedItems(matcher);
        return numberOfDeletes;
    }

}