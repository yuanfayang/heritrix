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
 * MemQueue.java
 * Created on Oct 14, 2003
 *
 * $Header$
 */
package org.archive.util;

import java.util.LinkedList;

/** An im-memory implementation of a {@link Queue}.
 *
 * @author gojomo
 *
 */
public class MemQueue extends LinkedList implements Queue {
    /** Create a new, empty MemQueue
     */
    public MemQueue() {
        super();
    }
    /* (non-javadoc)
     * @see org.archive.util.Queue#enqueue()

     */
    public void enqueue(Object o) {
        add(o);
    }

    /* (non-Javadoc)
     * @see org.archive.util.Queue#dequeue()
     */
    public Object dequeue() {
        return removeFirst();
    }

    /* (non-Javadoc)
     * @see org.archive.util.Queue#length()
     */
    public long length() {
        return (long)size();
    }

    /* (non-Javadoc)
     * @see org.archive.util.Queue#release()
     */
    public void release() {
        // nothing to release
    }

}
