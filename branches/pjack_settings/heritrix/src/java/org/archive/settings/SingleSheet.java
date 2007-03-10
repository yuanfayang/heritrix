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
 * SingleSheet.java
 * Created on October 24, 2006
 *
 * $Header$
 */
package org.archive.settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.archive.state.Constraint;
import org.archive.state.Key;
import org.archive.state.KeyTypes;


/**
 * A single sheet of settings.
 * 
 * @author pjack
 */
public class SingleSheet extends Sheet {
    

    /**
     * 
     */
    private static final long serialVersionUID = 1L;


    /**
     * Maps a module to the Key/Value settings for that module.  The map only
     * keeps weak references to the module; the existence of settings for a
     * module will not prevent that module from being garbage collected.
     * Modules are compared inside the map by reference, not using the
     * equals() method.  The map should have performance characteristics 
     * similar to ConcurrentHashMap -- in particular, the get() operation
     * usually will not block even if another thread is writing to the map.
     * See the {@link SheetMap} class for more details.
     */    
    private SheetMap<Object,Map<Key,Object>> settings;


    /**
     * Constructor.
     * 
     * @param manager   the manager who created this sheet
     */
    SingleSheet(SheetManager manager, String name) {
        super(manager, name);
        this.settings = new SheetMap<Object,Map<Key,Object>>();
    }

    @Override
    public <T> T check(Object module, Key<T> key) {
        if (module == null) {
            throw new IllegalArgumentException("Requested key on null module.");
        }
        validateModuleType(module, key);
        Map<Key,Object> keys = settings.get(module);
        if (keys == null) {
            return null;
        }
        Object value = keys.get(key);
        if (value == null) {
            return null;
        }
        return key.getType().cast(value);
    }
    
    
    @Override
    public <T> Offline checkOffline(Offline module, Key<T> key) {
        validateModuleType(module, key);
        if (isOnline(key)) {
            throw new IllegalStateException("Not an offline key.");
        }
        Map<Key,Object> keys = settings.get(module);
        if (keys == null) {
            return null;
        }
        Offline value = (Offline)keys.get(key);
        return value;
    }


    public <T> Resolved<T> resolve(Object module, Key<T> key) {
        if (module == null) {
            throw new IllegalArgumentException("Requested key on null module.");
        }
        validateModuleType(module, key);
        
        if (Map.class.isAssignableFrom(key.getType())) {
            return resolveMap(module, key);
        }
        if (List.class.isAssignableFrom(key.getType())) {
            return resolveList(module, key);
        }
        if (isOnline(key)) {
            return resolveOnline(module, key);
        } else {
            return resolveOffline((Offline)module, key);
        }
    }

    
    private <T> Resolved<T> resolveOnline(Object module, Key<T> key) {
        T result = check(module, key);
        if (result == null) {
            return resolveDefault(module, key);
        }
        return Resolved.makeOnline(module, key, result, this);
    }

    
    private <T> Resolved<T> resolveOffline(Offline module, Key<T> key) {
        Offline result = checkOffline(module, key);
        if (result == null) {
            return resolveDefault(module, key);
        }
        return Resolved.makeOffline(module, key, result, this);
    }
    
    
    private <T> Resolved<T> resolveMap(Object module, Key<T> key) {
        SingleSheet def = getSheetManager().getDefault();
        Map<String,List<Sheet>> sheetMap = new HashMap<String,List<Sheet>>();
        @SuppressWarnings("unchecked")
        Map<String,Object> defMap = (Map)def.check(module, key);
        
        // If this is the default sheet, avoid redundant double-check.
        @SuppressWarnings("unchecked")
        Map<String,Object> myMap = (def == this) ? null 
                : (Map)this.check(module, key);
        
        Map<String,Object> result;
        List<Sheet> sheets;
        if ((defMap == null) && (myMap == null)) {
            Sheet un = getSheetManager().getUnspecifiedSheet();
            return un.resolve(module, key);
        }
        if ((defMap != null) && (myMap != null)) {
            sheets = Collections.singletonList((Sheet)def);            
            result = MapResolver.makeMergeMap(defMap, sheetMap, sheets);
            List<Sheet> thisList = Collections.singletonList((Sheet)this);
            MapResolver.merge(result, sheetMap, myMap, thisList);
        } else if (defMap != null) {
            sheets = Collections.singletonList((Sheet)def);            
            result = MapResolver.makeMergeMap(defMap, sheetMap, sheets);
        } else { // myMap != null
            sheets = Collections.singletonList((Sheet)this);
            result = MapResolver.makeMergeMap(myMap, sheetMap, sheets);
        }
        return Resolved.makeMap(module, key, result, sheets, sheetMap);
    }
    
    
    private <T> Resolved<T> resolveList(Object module, Key<T> key) {
        SingleSheet def = getSheetManager().getDefault();
        @SuppressWarnings("unchecked")
        List<Object> defList = (List)def.check(module, key);
        @SuppressWarnings("unchecked")
        List<Object> myList = (def == this) ? null 
                :  (List)this.check(module, key);
        
        List<Object> result;
        List<Sheet> sheets;
        List<List<Sheet>> elementSheets;
        if ((defList == null) && (myList == null)) {
            Sheet un = getSheetManager().getUnspecifiedSheet();
            return un.resolve(module, key);
        }


        if ((defList != null) && (myList != null)) {
            sheets = Collections.singletonList((Sheet)def);
            result = new ArrayList<Object>();
            result.addAll(defList);
            result.addAll(myList);
            elementSheets = new ArrayList<List<Sheet>>();
            elementSheets.addAll(Collections.nCopies(defList.size(), sheets));
            elementSheets.addAll(Collections.nCopies(myList.size(), 
                    Collections.singletonList((Sheet)this)));
        } else if (defList != null) {
            sheets = Collections.singletonList((Sheet)def);
            result = defList;
            elementSheets = Collections.nCopies(defList.size(), sheets);
        } else {
            sheets = Collections.singletonList((Sheet)this);
            result = myList;
            elementSheets = Collections.nCopies(myList.size(), sheets);
        }
        return Resolved.makeList(module, key, result, sheets, elementSheets);
    }
    

    /**
     * Sets a property.
     * 
     * @param <T>         the type of the property to set
     * @param module   the processor to set the property on
     * @param key         the property to set
     * @param value       the new value for that property, or null to remove
     *     the property from this sheet
     */
    public <T> void set(Object module, Key<T> key, T value) {
        if (module == null) {
            throw new IllegalArgumentException(
                    "Attempt to set key value on null module.");
        }
        Class vtype = (value == null) ? null: value.getClass();
        validateTypes(module, key, vtype);
        T v = key.getType().cast(value);
        for (Constraint<T> c: key.getConstraints()) {
            if (!c.allowed(v)) {
                throw new IllegalArgumentException("IllegalValue");
            }
        }
        
        // Module and value are of the correct type, and value is valid.
        set2(module, key, value);
    }

    
    public <T> void setOffline(Offline module, Key<T> key, Object value) {
        Class vtype;
        if (value == null) {
            vtype = null;
        } else if (value instanceof Offline) {
            vtype = ((Offline)value).getType();
        } else {
            vtype = value.getClass();
        }
        validateTypes(module, key, vtype);
        set2(module, key, value);
    }

    
    /**
     * Performs the actual mutation after preconditions are enforced.
     * When this method is called, it is known that the key belongs to the
     * module, and that the value is appropriate for the key.
     * 
     * @param <T>
     * @param module   the module whose setting to change
     * @param key      the setting's key
     * @param value    the new value for that setting
     */
    private <T> void set2(Object module, Key<T> key, Object value) {
        Map<Key,Object> map = settings.get(module);
        if (map == null) {
            map = new ConcurrentHashMap<Key,Object>();
            Map<Key,Object> prev = settings.putIfAbsent(module, map);
            if (prev != null) {
                // Some other thread beat us to inserting the map; use that
                // previous map instead.
                map = prev;
            }
        }
        if (value == null) {
            map.remove(key);
        } else {
            value = transform(value);
            Object old = map.put(key, value);
            if (!KeyTypes.isSimple(Offline.getType(value))) {
                getSheetManager().fireModuleChanged(old, value);
            }
        }
    }


    private static <T> void validateTypes(Object module, Key<T> key, Class vtype) {
        validateModuleType(module, key);
        if ((vtype != null) && !key.getType().isAssignableFrom(vtype)) {
            throw new IllegalArgumentException("Illegal value type for " +
                    key.getFieldName() + ". Expected " + 
                    key.getType().getName() + " but got " + 
                    vtype.getName());
        }
    }


    @SuppressWarnings("unchecked")
    private Object transform(Object o) {
        if ((o instanceof Map) && (!(o instanceof SettingsMap))) {
            Map<String,Object> map = (Map)o;
            return new SettingsMap<Object>(getSheetManager(), map);
        }
        if ((o instanceof List) && (!(o instanceof SettingsList))) {
            List<Object> list = (List)o;
            return new SettingsList<Object>(getSheetManager(), list);
        }
        return o;
    }

}
