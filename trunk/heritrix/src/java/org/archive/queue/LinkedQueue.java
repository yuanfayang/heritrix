/* LinkedQueue
 *
 * $Id$
 *
 * Created on Oct 1, 2004
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
package org.archive.queue;

import java.util.Iterator;
import java.util.NoSuchElementException;

import EDU.oswego.cs.dl.util.concurrent.LinkedNode;

/**
 * Version of LinkedQueue (from util.concurrent) which gives more options for
 * understanding its contents: a running count of entries and an iterator. 
 * 
 * @author gojomo
 */
public class LinkedQueue extends EDU.oswego.cs.dl.util.concurrent.LinkedQueue {
    /** count of items in queue*/
    protected int count = 0;

    /* (non-Javadoc)
     * @see EDU.oswego.cs.dl.util.concurrent.LinkedQueue#extract()
     */
    protected synchronized Object extract() {
        Object obj = super.extract();
        if (obj != null) {
            count--;
        }
        return obj;
    }

    /* (non-Javadoc)
     * @see EDU.oswego.cs.dl.util.concurrent.LinkedQueue#insert(java.lang.Object)
     */
    protected void insert(Object x) {
        super.insert(x);
        count++;
    }

    /**
     * Return a count of entries in the queue.
     * 
     * Note that this counter's maintenance and access is generally
     * unsynchronized, and so may not reflect all inserts/extracts in 
     * progress.
     * 
     * @return count of items in queue
     */
    public int getCount() {
        return count;
    }

    public Iterator iterator() {
        return new LinkedQueueIterator();
    }

    /**
     * Iterator over contents of a org.archive.queue.LinkedQueue.
     * 
     * Behavior is undefined in presence of concurrent
     * takes from underlying queue. 
     * 
     * @author gojomo
     */
    public class LinkedQueueIterator implements Iterator {
        LinkedNode current;

        /**
         * 
         */
        public LinkedQueueIterator() {
            super();
            current = head_;
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#hasNext()
         */
        public boolean hasNext() {
            return current.next != null;
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#next()
         */
        public Object next() {
            if (current.next == null) {
                throw new NoSuchElementException();
            }
            Object obj = current.next.value;
            current = current.next;
            return obj;
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#remove()
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}