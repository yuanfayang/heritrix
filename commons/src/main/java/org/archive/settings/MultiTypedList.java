/* 
 * Copyright (C) 2007 Internet Archive.
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
 * MultiTypedList.java
 *
 * Created on Jun 5, 2007
 *
 * $Id:$
 */

package org.archive.settings;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

/**
 * @author pjack
 *
 */
class MultiTypedList<T> extends AbstractList<T> implements TypedList<T> {

    
    final private List<TypedList<T>> lists;
    final private Sheet start;
    private int size = -1;
    
    
    public MultiTypedList(List<TypedList<T>> lists, Sheet start) {
        this.lists = lists;
        this.start = start;
    }

    
    public int size() {
        if (size != -1) {
            return size;
        }
        int sz = 0;
        for (TypedList<T> list: lists) {
            sz += list.size();
        }
        return sz;
    }
    
    
    public boolean isEmpty() {
        return size() == 0;
    }

    
    public T get(int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException(Integer.toString(index));
        }
        int lower = 0;
        for (TypedList<T> list: lists) {
            int sz = list.size();
            if (index < lower + sz) {
                return list.get(index - lower);
            }
            lower += sz;
        }
        this.size = lower;
        throw new IndexOutOfBoundsException(index + " >= " + lower);
    }

    
    public List<Sheet> getSheets(int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException(Integer.toString(index));
        }
        List<Sheet> result = new ArrayList<Sheet>();
        if (this.start != null) {
            result.add(this.start);
        }
        int lower = 0;
        for (TypedList<T> list: lists) {
            int sz = list.size();
            if (index < lower + sz) {
                result.addAll(list.getSheets(index - lower));
                return result;
            }
            lower += sz;
        }
        this.size = lower;
        throw new IndexOutOfBoundsException(index + " >= " + lower);
    }

    
    public Class<T> getElementType() {
        return lists.get(0).getElementType();
    }
}
