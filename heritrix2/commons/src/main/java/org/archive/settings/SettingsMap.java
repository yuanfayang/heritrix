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

import org.archive.util.TypeSubstitution;


/**
 * A map whose elements are configured by a SheetManager.  Modifications
 * to this map trigger ModuleListener events.
 *
 * @author pjack
 *
 */
public class SettingsMap<T> implements TypedMap<T>, 
Serializable, TypeSubstitution {


    private static final long serialVersionUID = 1L;

    
    final private Map<String,T> delegate;
    final private SheetManager manager;
    final private List<Sheet> sheets;
    final private Class<T> elementType;
    
    
    public SettingsMap(SingleSheet sheet, Class<T> c) {
        this(
                sheet, 
//                Collections.checkedMap(
                        new LinkedHashMap<String,T>(), 
//                        String.class, 
//                        c),
                c);
    }
    
    
    public SettingsMap(Sheet sheet, Map<String,T> map, Class<T> c) {
        this.delegate = map;
        this.sheets = Collections.singletonList((Sheet)sheet);
        this.manager = sheet.getSheetManager();
        this.elementType = c;        
    }

    private SettingsMap(List<Sheet> sheets, Map<String,T> map, Class<T> c, 
            SheetManager manager) {
        this.delegate = map;
        this.sheets = sheets;
        this.manager = manager;
        this.elementType = c;        
    }


    @SuppressWarnings("unchecked")
    public SettingsMap<T> duplicate(Duplicator d) {
        List<Sheet> newSheets = d.duplicateSheets(sheets);

        Map newDelegate = new LinkedHashMap<String,T>();
        for (Map.Entry<String,T> me: delegate.entrySet()) {
            newDelegate.put(me.getKey(), d.duplicate(me.getValue()));
        }
        
        return new SettingsMap(newSheets, newDelegate, elementType, manager);
    }
    
    
    public List<Sheet> getSheets(String key) {
        return sheets;
    }
    
    public Class<T> getElementType() {
        return elementType;
    }


    public void clear() {
        Iterator<T> iter = delegate.values().iterator();
        while (iter.hasNext()) {
            T old = iter.next();
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
        return new EntrySet<T>(delegate.entrySet());
    }


    public boolean equals(Object o) {
        return delegate.equals(o);
    }


    public T get(Object key) {
        return delegate.get(key);
    }


    public int hashCode() {
        return delegate.hashCode();
    }


    public boolean isEmpty() {
        return delegate.isEmpty();
    }


    public Set<String> keySet() {
        return new KeySet<T>(delegate.entrySet());
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
        T old = delegate.put(key, value);
        manager.fireModuleChanged(old, value);
        return old;
    }


    public void putAll(Map<? extends String, ? extends T> t) {
        for (Map.Entry<? extends String, ? extends T> me: t.entrySet()) {
            String key = me.getKey();
            T value = me.getValue();
            validate(key, value);
            T old = delegate.put(key, value);
            manager.fireModuleChanged(old, value);
        }
    }


    public T remove(Object key) {
        T old = delegate.remove(key);
        manager.fireModuleChanged(old, null);
        return old;
    }


    public int size() {
        return delegate.size();
    }


    public Collection<T> values() {
        return new Values<T>(delegate.values());
    }

    
    public Class getActualClass() {
        return delegate.getClass();
    }
    
    
    public Map<String,T> getDelegate() {
        return delegate;
    }

    
    public void moveElement(String key, boolean up) {
        ArrayList<Map.Entry<String,T>> arr = 
            new ArrayList<Map.Entry<String,T>>(delegate.entrySet());
        int index = -1;
        for (int i = 0; i < arr.size(); i++) {
            Map.Entry<String,T> me = arr.get(i);
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
        for (Map.Entry<String,T> me: arr) {
            delegate.put(me.getKey(), me.getValue());
        }
    }


    private class KeySet<X> extends AbstractSet<String> {
        
        private Set<Map.Entry<String,X>> set;
        
        public KeySet(Set<Map.Entry<String,X>> set) {
            this.set = set;
        }
        
        public int size() {
            return set.size();
        }
        
        public Iterator<String> iterator() {
            return new KeyIterator<X>(set.iterator());
        }
        
    }

    
    private class EntrySet<X> extends AbstractSet<Map.Entry<String,X>> {
        
        private Set<Map.Entry<String,X>> set;
        
        public EntrySet(Set<Map.Entry<String,X>> set) {
            this.set = set;
        }
        
        public int size() {
            return set.size();
        }
        
        public Iterator<Map.Entry<String,X>> iterator() {
            return new EntryIterator<X>(set.iterator());
        }
        
    }

    
    private class Values<X> extends AbstractCollection<X> {
        
        private Collection<X> set;
        
        public Values(Collection<X> set) {
            this.set = set;
        }
        
        public int size() {
            return set.size();
        }
        
        public Iterator<X> iterator() {
            return new ValueIterator<X>(set.iterator());
        }
        
    }

    
    
    
    private class KeyIterator<X> implements Iterator<String> {
        
        final Iterator<Map.Entry<String,X>> iter;
        X last;
        
        public KeyIterator(Iterator<Map.Entry<String,X>> iter) {
            this.iter = iter;
        }
        
        public boolean hasNext() {
            return iter.hasNext();
        }

        public String next() {
            Map.Entry<String,X> me = iter.next();
            last = me.getValue();
            return me.getKey();
        }        
        
        public void remove() {
            manager.fireModuleChanged(last, null);
            iter.remove();
        }
    }


    private class EntryIterator<X> implements Iterator<Map.Entry<String,X>> {
        
        Iterator<Map.Entry<String,X>> iter;
        X last;
        
        public EntryIterator(Iterator<Map.Entry<String,X>> iter) {
            this.iter = iter;
        }

        public boolean hasNext() {
            return iter.hasNext();
        }

        public Map.Entry<String,X> next() {
            Map.Entry<String,X> me = iter.next();
            last = me.getValue();
            return me;
        }        
        
        public void remove() {
            manager.fireModuleChanged(last, null);
            iter.remove();
        }
    }

    
    private class ValueIterator<X> implements Iterator<X> {
        
        final Iterator<X> iter;
        X last;
        
        public ValueIterator(Iterator<X> iter) {
            this.iter = iter;
        }

        
        public boolean hasNext() {
            return iter.hasNext();
        }

        public X next() {
            last = iter.next();
            return last;
        }        
        
        public void remove() {
            manager.fireModuleChanged(last, null);
            iter.remove();
        }
    }

}
