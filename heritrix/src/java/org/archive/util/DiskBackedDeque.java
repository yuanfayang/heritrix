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
 * DiskBackedDeque.java
 * Created on May 21, 2004
 *
 * $Header$
 */
package org.archive.util;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.collections.Predicate;


/**
 * @author gojomo
 */
public class DiskBackedDeque extends DiskBackedQueue implements Deque {
    protected DiskStack stack;
    /**
     * @param dir
     * @param name
     * @param reuse
     * @param headMax
     * @throws IOException
     */
    public DiskBackedDeque(File dir, String name, boolean reuse, int headMax) throws IOException {
        super(dir, name, reuse, headMax);
        stack = new DiskStack(new File(dir,name+".top"));
    }
    
    /* (non-Javadoc)
     * @see org.archive.util.Stack#push(java.lang.Object)
     */
    public void push(Object object) {
        headQ.addFirst(object);
        enforceHeadSize();
    }
    
    /**
     * Ensure that only the chosen maximum number of 
     * items are held in memory, pushing any excess
     * to the stack as necessary. 
     */
    private void enforceHeadSize() {
        while(headQ.size()>headMax) {
            stack.push(headQ.removeLast());
        }
    }

    /**
     * Set the number of items to keep in memory,
     * and adjust current head to match.
     * 
     * @param hm
     */
    public void setHeadMax(int hm) {
        super.setHeadMax(hm);
        enforceHeadSize();
    }
    
    /* (non-Javadoc)
     * @see org.archive.util.Stack#pop()
     */
    public Object pop() {
        return dequeue();
    }
    
    /* (non-Javadoc)
     * @see org.archive.util.Stack#height()
     */
    public long height() {
        return length();
    }
    
    /* (non-Javadoc)
     * @see org.archive.util.DiskBackedQueue#backingDequeue()
     */
    protected Object backingDequeue() {
        // try disk stack first
        if(!stack.isEmpty()) {
            return stack.pop();
        }
        // then disk queue
        return tailQ.dequeue();
    }
    
    /* (non-Javadoc)
     * @see org.archive.util.DiskBackedQueue#backingUpdate()
     */
    protected void backingUpdate() {
        // TODO Auto-generated method stub
        super.backingUpdate();
    }
    
    /* (non-Javadoc)
     * @see org.archive.util.Queue#deleteMatchedItems(org.apache.commons.collections.Predicate)
     */
    public long deleteMatchedItems(Predicate matcher) {
        // TODO Auto-generated method stub
        return super.deleteMatchedItems(matcher);
    }

    /* (non-Javadoc)
     * @see org.archive.util.Queue#getIterator(boolean)
     */
    public Iterator getIterator(boolean inCacheOnly) {
        // TODO Auto-generated method stub
        return super.getIterator(inCacheOnly);
    }
    
    /* (non-Javadoc)
     * @see org.archive.util.DiskBackedQueue#headTargetSize()
     */
    protected int headTargetSize() {
        // leave space for in-memory pushes
        return super.headTargetSize()/2;
    }
    
    /* (non-Javadoc)
     * @see org.archive.util.Queue#isEmpty()
     */
    public boolean isEmpty() {
        // include stack in calculation
        return super.isEmpty() && stack.isEmpty();
    }
    
    /* (non-Javadoc)
     * @see org.archive.util.Queue#length()
     */
    public long length() {
        // include stack in calculation
        return super.length() + stack.height();
    }
    
    /* (non-Javadoc)
     * @see org.archive.util.Queue#release()
     */
    public void release() {
        super.release();
        stack.release();
    }
}
