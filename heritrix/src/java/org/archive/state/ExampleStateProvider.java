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

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;


/**
 * Example of a StateProvider.  This implementation just stores keys and
 * values in an ordinary HashMap.
 *
 * <p>This class really only exists for unit testing purposes.  FIXME:
 * We really need a separate test source directory for this sort of thing.
 *
 * @author pjack
 */
public class ExampleStateProvider implements StateProvider {


    /**
     * The keys and their values.
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
        if (module.getClass().isAssignableFrom(key.getOwner())) {
            throw new IllegalArgumentException("Module and key incompatible.");
        }
        Map<Key,Object> map = values.get(module);
        Object o = null;
        if (map != null) {
            o = map.get(key);
        }
        if (o == null) {
            return key.getDefaultValue();
        } else {
            return key.getType().cast(o);
        }
    }

    
    /**
     * Sets the value for the given key.
     * 
     * @param <T>  the type of value to set
     * @param key   the key whose value to set
     * @param value  the value to set
     */
    public <T> void set(Object module, Key<T> key, T value) {
        if (module == null) {
            throw new IllegalArgumentException("Null module.");
        }
        if (module.getClass().isAssignableFrom(key.getOwner())) {
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
