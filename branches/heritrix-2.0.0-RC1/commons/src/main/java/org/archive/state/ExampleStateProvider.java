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
 * ExampleStateProvider.java
 * Created on October 5, 2006
 *
 * $Header$
 */
package org.archive.state;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Default implementations of StateProvider that keeps all settings in memory.
 *
 * @author pjack
 */
public class ExampleStateProvider implements StateProvider {


    /**
     * Maps a module to the key/value settings for that module.
     */
    private Map<Object,Map<Key,Object>> values 
    = new IdentityHashMap<Object,Map<Key,Object>>();


    /**
     * Returns the value for the given key.
     * 
     * @return  the value for the given key
     */
    public <T> T get(Object module, Key<T> key) {
        if (module == null) {
            throw new IllegalArgumentException("Null module.");
        }
        Class<?> owner = key.getOwner();
        if (!owner.isInstance(module)) {
            throw new IllegalArgumentException("Module and key incompatible.");
        }
        Map<Key,Object> map = values.get(module);
        Object o = null;
        if (map != null) {
            o = map.get(key);
        }
        if (o != null) {
            return key.getType().cast(o);
        }
        
        // Wasn't specified; use the default value.
        Class<T> type = key.getType();
        if (KeyTypes.isSimple(type)) {
            return key.getDefaultValue();
        } else if (type == List.class) {
            if (KeyTypes.isSimple(key.getElementType())) {
                return key.getDefaultValue();
            }
            List<Class<?>> clist = key.getDefaultListElementImplementations();
            List<Object> list = new ArrayList<Object>();
            for (Class<?> c: clist) {
                list.add(create(c));
            }
            return type.cast(list);
        } else if (type == Map.class) {
            if (KeyTypes.isSimple(key.getElementType())) {
                return key.getDefaultValue();
            }
            Map<String,Class<?>> cm = key.getDefaultMapElementImplementations();
            Map<String,Object> m = new LinkedHashMap<String,Object>();
            for (Map.Entry<String,Class<?>> me: cm.entrySet()) {
                m.put(me.getKey(), create(me.getValue()));
            }
            return type.cast(m);
        } else {
            return type.cast(create(key.getDefaultImplementation()));
        }
    }

    
    private Object create(Class<?> c) {
        if (c == null) {
            return null;
        }
        try {
            Object o = c.newInstance();
            if (o instanceof Initializable) {
                ((Initializable)o).initialTasks(this);
            }
            return o;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
    
    
    /**
     * Sets the value for the given key.
     * 
     * @param <T>  the type of value to set
     * @param module  the module whose value to set
     * @param key     one of that module's keys
     * @param value  the value to set
     */
    public <T> void set(Object module, Key<T> key, T value) {
        if (module == null) {
            throw new IllegalArgumentException("Null module.");
        }
        Class<?> owner = key.getOwner();
        if (!owner.isInstance(module)) {
            throw new IllegalArgumentException("Module and key incompatible.");
        }
        for (Constraint<T> c: key.getConstraints()) {
            if (!c.allowed(value)) {
                throw new IllegalArgumentException();
            }
        }
        Map<Key,Object> map = values.get(module);
        if (map == null) {
            map = new HashMap<Key,Object>();
            values.put(module, map);
        }
        map.put(key, value);
    }


}
