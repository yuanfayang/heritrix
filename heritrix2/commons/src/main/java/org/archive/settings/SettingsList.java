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


/**
 * A list whose elements are configured by a SheetManager.  Modifications
 * to this list trigger ModuleListener events.
 * 
 * @author pjack
 *
 */
public class SettingsList<T> extends AbstractList<T> 
implements TypedList<T>, Serializable {


    private static final long serialVersionUID = 1L;

    
    final private List<Object> delegate;
    final private SingleSheet sheet;
    final private List<Sheet> sheets;
    final private SheetManager manager;
    final private Class<T> elementType;
    
    
    public SettingsList(SingleSheet sheet, Class<T> c) {
        this(sheet, 
                new ArrayList<T>(),
//                Collections.checkedList(new ArrayList<T>(), c),
                c);
    }
    
    
    public SettingsList(SingleSheet sheet, List<T> list, Class<T> c) {
        List l = list;
        this.delegate = l;
        this.sheet = sheet;
        this.sheets = Collections.singletonList((Sheet)sheet);
        this.manager = sheet.getSheetManager();
        this.elementType = c;
    }
    
    
//    // Used by duplicate
//    private SettingsList(List<T> delegate, 
//            List<Sheet> sheets, 
//            SheetManager manager, 
//            Class<T> elementType) {
//        this.delegate = delegate;
//        this.sheets = sheets;
//        this.manager = manager;
//        this.elementType = elementType;
//    }
    
    
    @SuppressWarnings("unchecked")
    public Object duplicate(Duplicator d) {        
        List newElements = new ArrayList<T>();
        for (Object e: delegate) {
            newElements.add(d.duplicate(e));
        }
        return new SettingsList(d.getNewSheet(), newElements, elementType);
    }
    
    public List<Sheet> getSheets(int index) {
        return sheets;
    }
     
    
    public Class<T> getElementType() {
        return elementType;
    }


    private void validate(T element) {
        if (element instanceof String) {
            String s = (String)element;
            if ((s.indexOf('\n') >= 0) || (s.indexOf('\r') >= 0)) {
                throw new IllegalArgumentException("List elements cannot contain a newline.");
            }
        }
    }
    

    public void add(int index, T element) {
        validate(element);
        if (!SingleSheet.isModuleType(elementType)) {
            delegate.add(index, element);
        } else {
            setModuleValue(index, element);
        }
    }


    public boolean add(T o) {
        validate(o);
        if (!SingleSheet.isModuleType(elementType)) {
            delegate.add(o);
        } else {
            setModuleValue(size(), o);
        }
        return true;
    }

    
    public boolean addAll(Collection<? extends T> c) {
        for (T t: c) {
            add(t);
        }
        return true;
    }

    
    public boolean addAll(int index, Collection<? extends T> c) {
        for (T t: c) {
            add(index, t);
            index++;
        }
        return true;
    }


    public void clear() {
        for (Object t: delegate) {
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
        if (o == this) {
            return true;
        }
        if (!(o instanceof List)) {
            return false;
        }
        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>)o;
        if (list.size() != size()) {
            return false;
        }
        for (int i = 0; i < size(); i++) {
            Object o1 = get(i);
            Object o2 = list.get(i);
            if (o1 == null && o2 != null) {
                return false;
            }
            if (!o1.equals(o2)) {
                return false;
            }
        }
        return true;
    }

    
    private T toActualObject(Object in) {
        if (in instanceof ModuleInfo) {
            in = ((ModuleInfo)in).holder.module;
        }
        return (T)in;
    }
    
    public T get(int index) {
        return toActualObject(delegate.get(index));
    }

    
    public int hashCode() {
        int hashCode = 1;
        for (Object element: this) {
            hashCode = 31 * hashCode + (element == null ? 0 : element.hashCode());
        }
        return hashCode;
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
        T result = toActualObject(delegate.remove(index));
        manager.fireModuleChanged(result, null);
        return result;
    }


    public boolean remove(Object o) {
        for (int i = 0; i < size(); i++) {
            Object element = get(i);
            if (element.equals(o)) {
                remove(i);
                manager.fireModuleChanged(o, null);
                return true;
            }
        }
        return false;
    }


    public boolean removeAll(Collection<?> c) {
        Iterator<Object> iter = delegate.iterator();
        boolean r = false;
        while (iter.hasNext()) {
            Object o = iter.next();
            if (c.contains(toActualObject(o))) {
                iter.remove();
                manager.fireModuleChanged(o, null);
                r = true;
            }
        }
        return r;
    }

    
    public boolean retainAll(Collection<?> c) {
        Iterator<Object> iter = delegate.iterator();
        boolean r = false;
        while (iter.hasNext()) {
            Object o = iter.next();
            if (!c.contains(toActualObject(o))) {
                iter.remove();
                manager.fireModuleChanged(o, null);
                r = true;
            }
        }
        return r;
    }

    
    public T set(int index, T element) {
        validate(element);
        Object old;
        if (!SingleSheet.isModuleType(elementType)) {
            old = delegate.set(index, element);
        } else {
            old = setModuleValue(index, element);
        }
        return (T)old;
    }
    
    
    private Object setModuleValue(int i, Object value) {
        Container c = new ListContainer(delegate);
        Object old = sheet.setModuleValue(c, i, value);
        manager.fireModuleChanged(old, value);
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
