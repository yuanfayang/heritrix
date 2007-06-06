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
 * SettingsList.java
 *
 * Created on Mar 9, 2007
 *
 * $Id:$
 */

package org.archive.settings;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.archive.util.SubList;
import org.archive.util.TypeSubstitution;

/**
 * A list whose elements are configured by a SheetManager.  Modifications
 * to this list trigger ModuleListener events.
 * 
 * @author pjack
 *
 */
public class SettingsList<T> extends AbstractList<T> 
implements TypedList<T>, TypeSubstitution, Serializable {


    private static final long serialVersionUID = 1L;

    
    final private List<T> delegate;
    final private List<Sheet> sheets;
    final private SheetManager manager;
    final private Class<T> elementType;
    
    
    public SettingsList(SingleSheet sheet, Class<T> c) {
        this(sheet, 
                new ArrayList<T>(),
//                Collections.checkedList(new ArrayList<T>(), c),
                c);
    }
    
    
    public SettingsList(Sheet sheet, List<T> list, Class<T> c) {
        this.delegate = list;
        this.sheets = Collections.singletonList((Sheet)sheet);
        this.manager = sheet.getSheetManager();
        this.elementType = c;
    }
    
    
    public List<Sheet> getSheets(int index) {
        return sheets;
    }
     
    
    public Class<T> getElementType() {
        return elementType;
    }
    
    public Class getActualClass() {
        return delegate.getClass();
    }
    
    
    public List<T> getDelegate() {
        return delegate;
    }


    public void add(int index, T element) {
        delegate.add(index, element);
        manager.fireModuleChanged(null, element);
    }


    public boolean add(T o) {
        manager.fireModuleChanged(null, o);
        return delegate.add(o);
    }

    
    public boolean addAll(Collection<? extends T> c) {
        for (T t: c) {
            manager.fireModuleChanged(null, t);
        }
        return delegate.addAll(c);
    }

    
    public boolean addAll(int index, Collection<? extends T> c) {
        for (T t: c) {
            manager.fireModuleChanged(null, t);
        }
        return delegate.addAll(index, c);
    }


    public void clear() {
        for (T t: delegate) {
            manager.fireModuleChanged(t, null);
        }
        delegate.clear();
    }

    
    public boolean contains(Object o) {
        return delegate.contains(o);
    }

    
    public boolean containsAll(Collection<?> c) {
        return delegate.containsAll(c);
    }


    public boolean equals(Object o) {
        return delegate.equals(o);
    }

    
    public T get(int index) {
        return delegate.get(index);
    }

    
    public int hashCode() {
        return delegate.hashCode();
    }


    public int indexOf(Object o) {
        return delegate.indexOf(o);
    }


    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    
    public int lastIndexOf(Object o) {
        return delegate.lastIndexOf(o);
    }


    public T remove(int index) {
        T result = delegate.remove(index);
        manager.fireModuleChanged(result, null);
        return result;
    }


    public boolean remove(Object o) {
        if (delegate.remove(o)) {
            manager.fireModuleChanged(o, null);
        }
        return delegate.remove(o);
    }


    public boolean removeAll(Collection<?> c) {
        Iterator<T> iter = delegate.iterator();
        boolean r = false;
        while (iter.hasNext()) {
            Object o = iter.next();
            if (c.contains(o)) {
                iter.remove();
                manager.fireModuleChanged(o, null);
                r = true;
            }
        }
        return r;
    }

    
    public boolean retainAll(Collection<?> c) {
        Iterator<T> iter = delegate.iterator();
        boolean r = false;
        while (iter.hasNext()) {
            Object o = iter.next();
            if (!c.contains(o)) {
                iter.remove();
                manager.fireModuleChanged(o, null);
                r = true;
            }
        }
        return r;
    }

    
    public T set(int index, T element) {
        T old = delegate.set(index, element);
        manager.fireModuleChanged(old, element);
        return old;
    }


    public int size() {
        return delegate.size();
    }


    public List<T> subList(int fromIndex, int toIndex) {
        return new SubList<T>(this, fromIndex, toIndex);
    }

    
    public Object[] toArray() {
        return delegate.toArray();
    }


    public <X> X[] toArray(X[] a) {
        return delegate.toArray(a);
    }
    
    
}
