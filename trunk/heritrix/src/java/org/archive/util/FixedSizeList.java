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
 * Created on Jul 17, 2003
 *
 */
package org.archive.util;

import java.util.LinkedList;

/** Provide a simple fixed-size queue that handles ordering and
 *  makes sure it's a first-in first-out kind of thing.
 *
 * @author Parker Thompson
 */
public class FixedSizeList extends LinkedList {

    public FixedSizeList() {
    }

    protected int maxSize = 10;

    public FixedSizeList(int size){
    	maxSize = size;
    }

    public boolean add(Object item){
    	makeSpace();
    	return super.add(item);
    }

    public void add(int index, Object element) {
    	makeSpace();
    	super.add(index, element);
    }

    public void addLast(Object o) {
    	makeSpace();
    	super.addLast(o);
    }

    private void makeSpace() {
    	if(size() >= maxSize){
    		removeFirst();
    	}
    }
}
