/* Copyright (C) 2006 Internet Archive.
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
 * Resolved.java
 * Created on October 24, 2006
 *
 * $Header$
 */
package org.archive.settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.archive.state.Key;

public class Resolved<T> {

    private Key<T> key;
    private Object module;
    private Object value;
    private List<Sheet> sheets;
    private Object elementSheets;


    private Resolved(
            Object module, 
            Key<T> key, 
            Object value,
            List<Sheet> sheets,
            Object elementSheets) {
        this.key = key;
        this.module = module;
        this.value = value;
        this.sheets = sheets;
        this.elementSheets = elementSheets;
    }
    
    
            
    
    
    public Offline getOfflineProxy() {
        if (value == null) {
            return null;
        }
        if (!(value instanceof Offline)) {
            throw new IllegalStateException("Online or leaf sheet.");
        }
        return (Offline)value;
    }


    public Key<T> getKey() {
        return key;
    }


    public Object getModule() {
        return module;
    }


    @SuppressWarnings("unchecked")
    public List<Sheet> getSheets() {
        return sheets;
    }
    
    
    @SuppressWarnings("unchecked")
    public Map<String,List<Sheet>> getMapSheets() {
        return (Map)elementSheets;
    }
    
    
    @SuppressWarnings("unchecked")
    public List<List<Sheet>> getListSheets() {
        return (List)elementSheets;
    }

    
    
    public Sheet getLastSheet() {
        List<Sheet> sheets = getSheets();
        return sheets.get(sheets.size() - 1);
    }

    public T getOnlineValue() {
        if (value instanceof Offline) {
            throw new IllegalStateException("Not an online/leaf sheet.");
        }
        @SuppressWarnings("unchecked")
        T r = (T)value;
        return r;
    }
    
    
    public Object getValue() {
        if (getLastSheet().isOnline(key)) {
            return getOnlineValue();
        } else {
            return getOfflineProxy();
        }
    }


    // for both online AND Leaf 
    public static <T> Resolved<T> makeOnline(
            Object module,
            Key<T> key, 
            T value,
            List<Sheet> sheets) {
        sheets = new ArrayList<Sheet>(sheets);
        sheets = Collections.unmodifiableList(sheets);
        return new Resolved<T>(module, key, value, sheets, null);
    }
    
    
    static <T> Resolved<T> make(
            Object module,
            Key<T> key,
            Object value,
            List<Sheet> sheets,
            Object elementSheets
            ) {
        return new Resolved<T>(module, key, value, sheets, elementSheets);
    }
    
    
    public static <T> Resolved<T> makeOnline(
            Object module,
            Key<T> key,
            T value,
            Sheet sheet
            ) {
        List<Sheet> l = Collections.singletonList(sheet);
        return make(module, key, value, l, null);
    }


    public static <T> Resolved<T> makeOffline(
            Object module,
            Key<T> key,
            Offline offline,
            List<Sheet> sheets) {
        List<Sheet> l = new ArrayList<Sheet>(sheets);
        l = Collections.unmodifiableList(l);
        return make(module, key, offline, sheets, null);
    }

    
    public static <T> Resolved<T> makeOffline(
            Object module,
            Key<T> key,
            Offline offline,
            Sheet sheet) {
        List<Sheet> l = Collections.singletonList(sheet);
        return makeOffline(module, key, offline, l);
    }
    

    
    public static <T> Resolved<T> makeMap(
            Object module, 
            Key<T> key, 
            Map<String,Object> value, 
            List<Sheet> sheets,
            Map<String,List<Sheet>> elementSheets) {
        value = Collections.unmodifiableMap(value);
        elementSheets = Collections.unmodifiableMap(elementSheets);
        return make(module, key, value, sheets, elementSheets);
    }
    
    
    public static <T> Resolved<T> makeList(
            Object module, 
            Key<T> key,
            List<Object> value, 
            List<Sheet> sheets,
            List<List<Sheet>> elementSheets) {
        return make(module, key, value, sheets, elementSheets);
    }


    public static <T> Resolved<T> makeList(
            Object module, 
            Key<T> key,
            T value, 
            List<Sheet> sheets,
            List<List<Sheet>> elementSheets) {
        return make(module, key, value, sheets, elementSheets);
    }


}
