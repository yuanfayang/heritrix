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
 * SettingsMap.java
 *
 * Created on Mar 9, 2007
 *
 * $Id:$
 */
package org.archive.settings;


import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.keyvalue.DefaultMapEntry;


/**
 * A map whose elements are configured by a SheetManager.  Modifications
 * to this map trigger ModuleListener events.
 *
 * @author pjack
 *
 */
public class SettingsMap<T> implements TypedMap<T>, Serializable {


    private static final long serialVersionUID = 1L;

    
    final private Map<String,Object> delegate;
    final private SheetManager manager;
    final private List<Sheet> sheets;
    final private SingleSheet sheet;
    final private Class<T> elementType;
    
    
    public SettingsMap(SingleSheet sheet, Class<T> c) {
        this(sheet, new LinkedHashMap<String,T>(), c);
    }
    
    
    public SettingsMap(SingleSheet sheet, Map<String,T> map, Class<T> c) {
        this.delegate = cast(map);
        this.sheet = sheet;
        this.sheets = Collections.singletonList((Sheet)sheet);
        this.manager = sheet.getSheetManager();
        this.elementType = c;        
    }




    @SuppressWarnings("unchecked")
    public SettingsMap<T> duplicate(Duplicator d) {
        Map newDelegate = new LinkedHashMap<String,T>();
        for (Map.Entry<String,Object> me: delegate.entrySet()) {
            newDelegate.put(me.getKey(), d.duplicate(me.getValue()));
        }
        return new SettingsMap(d.getNewSheet(), newDelegate, elementType);
    }

    
    private T toActualObject(Object x) {
        if (x instanceof ModuleInfo) {
            x = ((ModuleInfo)x).holder.module;
        }
        return (T)x;
    }

    
    public List<Sheet> getSheets(String key) {
        return sheets;
    }
    
    public Class<T> getElementType() {
        return elementType;
    }


    public void clear() {
        Iterator<Object> iter = delegate.values().iterator();
        while (iter.hasNext()) {
            T old = toActualObject(iter.next());
            iter.remove();
            manager.fireModuleChanged(old, null);
        }
        delegate.clear();
    }


    public boolean containsKey(Object key) {
        return delegate.containsKey(key);
    }


    public boolean containsValue(Object value) {
        return delegate.containsValue(value);
    }


    public Set<Entry<String, T>> entrySet() {
        return new EntrySet(delegate.entrySet());
    }


    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Map)) {
            return false;
        }
        @SuppressWarnings("unchecked")
        Map<?,?> map = (Map)o;
        return entrySet().equals(map.entrySet());
    }


    public T get(Object key) {
        return toActualObject(delegate.get(key));
    }


    public int hashCode() {
        return entrySet().hashCode();
    }


    public boolean isEmpty() {
        return delegate.isEmpty();
    }


    public Set<String> keySet() {
        Set<Map.Entry<String,Object>> set = delegate.entrySet();
        return new KeySet(set);
    }

    
    private void validate(String key, Object value) {
        if (key.indexOf(':') >= 0) {
            throw new IllegalArgumentException(
               "Keys cannot contain a colon.");
        }
        if ((key.indexOf('\n') >= 0) || (key.indexOf('\r') >= 0)) {
            throw new IllegalArgumentException("Keys cannot contain a newline.");
        }
        if (value instanceof String) {
            String s = (String)value;
            if ((s.indexOf('\n') >= 0) || (s.indexOf('\r') >= 0)) {
                throw new IllegalArgumentException(
                        "Values cannot contain a newline.");
            }
        }
    }
    
    public T put(String key, T value) {
        validate(key, value);
        
        if (!SingleSheet.isModuleType(elementType)) {
            Object oldObj = delegate.put(key, value);
            T old = elementType.cast(oldObj);
            manager.fireModuleChanged(old, value);
            return old;
        }
        
        Container container = new MapContainer(delegate);
        Object old = sheet.setModuleValue(container, key, value);
        manager.fireModuleChanged(old, value);
        return (T)old;
    }


    public void putAll(Map<? extends String, ? extends T> t) {
        for (Map.Entry<? extends String, ? extends T> me: t.entrySet()) {
            put(me.getKey(), me.getValue());
        }
    }


    public T remove(Object key) {
        T old = toActualObject(delegate.remove(key));
        manager.fireModuleChanged(old, null);
        return old;
    }


    public int size() {
        return delegate.size();
    }



    public Collection<T> values() {
        Collection<Object> values = delegate.values();
        return new Values(values);
    }


    public void moveElement(String key, boolean up) {
        ArrayList<Map.Entry<String,Object>> arr = 
            new ArrayList<Map.Entry<String,Object>>(delegate.entrySet());
        int index = -1;
        for (int i = 0; i < arr.size(); i++) {
            Map.Entry<String,Object> me = arr.get(i);
            if (me.getKey().equals(key)) {
                index = i;
                break;
            }
        }
        if (index == -1) {
            throw new IllegalArgumentException("No such key: " + key);
        }
        
        int index2 = up ? index - 1 : index + 1;
        
        Collections.swap(arr, index, index2);
        this.delegate.clear();
        for (Map.Entry<String,Object> me: arr) {
            delegate.put(me.getKey(), me.getValue());
        }
    }


    private class KeySet extends AbstractSet<String> {
        
        private Set<Map.Entry<String,Object>> set;
        
        public KeySet(Set<Map.Entry<String,Object>> set) {
            this.set = set;
        }
        
        public int size() {
            return set.size();
        }
        
        public Iterator<String> iterator() {
            return new KeyIterator(set.iterator());
        }
        
    }

    
    private class EntrySet extends AbstractSet<Map.Entry<String,T>> {
        
        private Set<Map.Entry<String,Object>> set;
        
        public EntrySet(Set<Map.Entry<String,Object>> set) {
            this.set = set;
        }
        
        public int size() {
            return set.size();
        }
        
        public Iterator<Map.Entry<String,T>> iterator() {
            return new EntryIterator(set.iterator());
        }
        
    }

    
    private class Values extends AbstractCollection<T> {
        
        private Collection<Object> set;
        
        public Values(Collection<Object> set) {
            this.set = set;
        }
        
        public int size() {
            return set.size();
        }
        
        public Iterator<T> iterator() {
            return new ValueIterator(set.iterator());
        }
        
    }

    
    
    
    private class KeyIterator implements Iterator<String> {
        
        final Iterator<Map.Entry<String,Object>> iter;
        T last;
        
        public KeyIterator(Iterator<Map.Entry<String,Object>> iter) {
            this.iter = iter;
        }
        
        public boolean hasNext() {
            return iter.hasNext();
        }

        public String next() {
            Map.Entry<String,Object> me = iter.next();
            last = toActualObject(me.getValue());
            return me.getKey();
        }
        
        public void remove() {
            manager.fireModuleChanged(last, null);
            iter.remove();
        }
    }


    private class EntryIterator implements Iterator<Map.Entry<String,T>> {
        
        Iterator<Map.Entry<String,Object>> iter;
        T last;
        
        public EntryIterator(Iterator<Map.Entry<String,Object>> iter) {
            this.iter = iter;
        }

        public boolean hasNext() {
            return iter.hasNext();
        }

        public Map.Entry<String,T> next() {
            Map.Entry<String,Object> me = iter.next();
            last = toActualObject(me.getValue());
            @SuppressWarnings("unchecked")
            Map.Entry<String,T> result = new DefaultMapEntry(me.getKey(), last);
            return result;
        }
        
        public void remove() {
            manager.fireModuleChanged(last, null);
            iter.remove();
        }
    }

    
    private class ValueIterator implements Iterator<T> {
        
        final Iterator<Object> iter;
        T last;
        
        public ValueIterator(Iterator<Object> iter) {
            this.iter = iter;
        }

        
        public boolean hasNext() {
            return iter.hasNext();
        }

        public T next() {
            last = toActualObject(iter.next());
            return last;
        }        
        
        public void remove() {
            manager.fireModuleChanged(last, null);
            iter.remove();
        }
    }

    
    @SuppressWarnings("unchecked")
    private static Map<String,Object> cast(Map<String,?> map) {
        Map m = map;
        return m;
    }
}
