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
 * Stack.java
 * Created on May 21, 2004
 *
 * $Header$
 */
 
package org.archive.util;


/**
 * Simple Stack: supports add and remove at top.
 * 
 * @author gojomo
 */
public interface Stack {
    /**
     * Add object to top of Stack
     * 
     * @param object
     */
    public void push(Object object);
    
    /**
     * Remove and return item from top of Stack
     * 
     * @return
     */
    public Object pop();
    
    /**
     * Return item from top of Stack without removing it.
     * 
     * @return
     */
    public Object peek();
    
    /**
     * Number of items in the Stack.
     */
    public long height();

    /**
     * Release any OS resources, if necessary.
     */
    public void release();

    /**
     * @return
     */
    public boolean isEmpty();
}
