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
 * Queue.java
 * Created on Oct 14, 2003
 *
 * $Header$
 */
package org.archive.util;

import java.util.NoSuchElementException;


/** An Abstract queue.  It should implement FIFO semantics.
 * @author gojomo
 *
 */
public interface Queue {

    /** Add an entry to the end of queue
     * @param o the entry to queue
     */
    void enqueue(Object o);

    /** is the queue empty?
     *
     * @return <code>true</code> if the queue has no elements
     */
    boolean isEmpty();

    /** remove an entry from the start of the  queue
     *
     * @return the object
     * @throws java.util.NoSuchElementException
     */
    Object dequeue() throws NoSuchElementException;

    /** get the number of elements in the queue
     *
     * @return the number of elements in the queue
     */
    long length();

    /**
     * release any OS/IO resources associated with Queue
     */
    void release();
}
