/* DiskQueueIterator
 *
 * $Id$
 *
 * Created on Feb 26, 2004
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
package org.archive.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;


/**
 * An iterator for DiskQueue. It iterates through items queued up in a file.
 * Does this without changing the file. As a result the <code>remove</code>
 * method does nothing.
 *
 * @author Kristinn Sigurdsson
 */
public class DiskQueueIterator implements Iterator {

    InputStream inStream;
    ObjectInputStream objectInStream;
    long length;

    /**
     * Constructor
     * @param inStream InputStream containing serialized objects
     * @param length The number of items in the stream. If this number is too
     *               large, the <code>hasNext</code> will falsely return true
     *               when we reach the end of the queue and the next call to
     *               <code>next</code> will result in an exception being thrown.
     *               If the number is too small <code>hasNext</code> will return
     *               false while there are still items in the queue.
     */
    public DiskQueueIterator(InputStream inStream, long length) throws IOException{
        objectInStream = new ObjectInputStream(inStream);
        this.length = length;
    }

    /**
     * Not supported
     * @throws UnsupportedOperationException if invoked
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext() {
        return length > 0;
    }

    /* (non-Javadoc)
     * @see java.util.Iterator#next()
     */
    public Object next() {
        if (length==0) {
            throw new NoSuchElementException();
        }
        Object o;
        try {
            o = objectInStream.readObject();
        } catch (IOException e) {
            e.printStackTrace();
            throw new NoSuchElementException();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new NoSuchElementException();
        }
        length--;
        return o;
    }

}
