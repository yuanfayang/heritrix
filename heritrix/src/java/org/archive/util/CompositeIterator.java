/* CompositeIterator
*
* $Id$
*
* Created on Mar 3, 2004
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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * @author gojomo
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class CompositeIterator implements Iterator {
    LinkedList iterators = new LinkedList();
    Iterator innerIterator; // of Iterators;
    Iterator currentIterator;
    
    
    /**
     * Sets up internal interators; must be called before using
     * CompositeIterator instance
     */
    public void begin() {
        innerIterator = iterators.iterator();
        cueIterator();
    }
    
    private void cueIterator() {
        if (innerIterator.hasNext()) {
            currentIterator = (Iterator) innerIterator.next();
        } else {
            currentIterator = null;
        }
    }

    /* (non-Javadoc)
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext() {
        if(currentIterator!=null) {
            if(currentIterator.hasNext()) {
                return true;
            } else {
                cueIterator();
                return hasNext();
            }
        } else {
            return false;
        }
    }
    
    /* (non-Javadoc)
     * @see java.util.Iterator#next()
     */
    public Object next() {
        if(currentIterator!=null) {
            return currentIterator.next();
        } else {
            throw new NoSuchElementException();
        }
    }
    
    /* (non-Javadoc)
     * @see java.util.Iterator#remove()
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Create an empty CompositeIterator. Internal
     * iterators may be added later.
     */
    public CompositeIterator() {
        super();
    }
    
    /**
     * Convenience method for concatenating together
     * two iterators. 
     */
    public CompositeIterator(Iterator i1, Iterator i2) {
        this();
        add(i1);
        add(i2);
    }

    /**
     * Add an iterator to the internal chain. 
     * 
     * @param i
     */
    private void add(Iterator i) {
        iterators.add(i);
    }

}
