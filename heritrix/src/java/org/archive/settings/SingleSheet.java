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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.archive.crawler.util.Transform;
import org.archive.state.Constraint;
import org.archive.state.Key;


/**
 * A single sheet of settings.
 * 
 * @author pjack
 */
public class SingleSheet extends Sheet {
    

    /**
     * Maps processor/Key combo to value for that combo.
     */
    private Map<Identity,Map<Key,Object>> settings;


    /**
     * Constructor.
     * 
     * @param manager   the manager who created this sheet
     */
    SingleSheet(SheetManager manager, String name) {
        super(manager, name);
        this.settings = new HashMap<Identity,Map<Key,Object>>();
    }
        
    
    @Override
    public <T> T check(Object target, Key<T> key) {
        Identity id = new Identity(target);
        Map<Key,Object> keys = settings.get(id);
        if (keys == null) {
            return null;
        }
        Object value = keys.get(key);
        if (value == null) {
            return null;
        }
        return key.getType().cast(value);
    }

    
    public <T> Resolved<T> resolve(Object processor, Key<T> key) {
        T result = check(processor, key);
        if (result == null) {
            return resolveDefault(processor, key);
        }
        return new Resolved<T>(this, processor, key, result);
    }
    
    /**
     * Sets a property.
     * 
     * @param <T>         the type of the property to set
     * @param processor   the processor to set the property on
     * @param key         the property to set
     * @param value       the new value for that property, or null to remove
     *     the property from this sheet
     */
    public <T> void set(Object processor, Key<T> key, T value) {
        if (!key.getOwner().isInstance(processor)) {
            throw new IllegalArgumentException("Illegal module type.");
        }
        if ((value != null) && !key.getType().isInstance(value)) {
            throw new IllegalArgumentException("Illegal value type for " +
                    key.getFieldName() + ". Expected " + 
                    key.getType().getName() + " but got " + 
                    value.getClass().getName());
        }
        T v = key.getType().cast(value);
        for (Constraint<T> c: key.getConstraints()) {
            if (!c.allowed(v)) {
                throw new IllegalArgumentException("IllegalValue");
            }
        }
        
        // Module and value are of the correct type, and value is valid.
        Identity id = new Identity(processor);
        Map<Key,Object> map = settings.get(id);
        if (map == null) {
            map = new HashMap<Key,Object>();
            settings.put(id, map);
        }
        if (value == null) {
            map.remove(key);
        } else {
            map.put(key, value);
        }
    }


    public void removeAll(Object processor) {
        Identity id = new Identity(processor);
        settings.remove(id);
    }


    public Map<Key,Object> getAll(Object processor) {
        Identity id = new Identity(processor);
        return settings.get(id);
    }
    
    
    public Collection<Object> getProcessors() {
        Set<Identity> s = settings.keySet();
        return new Transform<Identity,Object>(s, Identity.TO_OBJECT);
    }
    
    
    void swapRoot(Object oldRoot, Object newRoot, Set<Key<Object>> dead) {
        
    }

}
