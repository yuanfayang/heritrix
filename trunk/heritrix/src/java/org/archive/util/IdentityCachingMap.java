/* BackedMap
*
* $Id$
*
* Created on Nov 3, 2004
*
* Copyright (C) 2004 Internet Archive.
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
*/ 
package org.archive.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.map.ReferenceMap;

/**
 * Map that is wraps another Map (typically a StoredMap), 
 * but uses a SoftReferenceCache to avoid double-getting (and 
 * thus double-creating) instances for specific keys, when the
 * object returned previously still exists. 
 * 
 * (Value objects might choose to remember their source map,
 * and implement a finalize() that forces their current state 
 * back into the underlying map.)
 * 
 * @author gojomo
 */
public class IdentityCachingMap implements Map {
    Map innerMap;
    ReferenceMap identityCache;
    
    /**
     * Wrap the given map with an identity-preserving memory-sensitive cache. 
     * 
     */
    public IdentityCachingMap(Map inner) {
        super();
        this.innerMap = inner;
        // use default ReferenceMap: hard keys, soft values
        this.identityCache = new ReferenceMap();
    }
    
    /* (non-Javadoc)
     * @see java.util.Map#size()
     */
    public int size() {
        return innerMap.size(); // may be unsupported operation
    }

    /* (non-Javadoc)
     * @see java.util.Map#isEmpty()
     */
    public boolean isEmpty() {
        return innerMap.isEmpty();
    }

    /* (non-Javadoc)
     * @see java.util.Map#containsKey(java.lang.Object)
     */
    public boolean containsKey(Object key) {
        return identityCache.containsKey(key) || innerMap.containsKey(key);
    }

    /** for testing */
    boolean cacheContainsKey(Object key) {
        return identityCache.containsKey(key);
    }
    
    /* (non-Javadoc)
     * @see java.util.Map#containsValue(java.lang.Object)
     */
    public boolean containsValue(Object value) {
        return innerMap.containsValue(value);
    }

    /* (non-Javadoc)
     * @see java.util.Map#get(java.lang.Object)
     */
    public Object get(Object key) {
        Object result = identityCache.get(key);
        if (result == null) {
            result = innerMap.get(key);
            if (result != null) {
                identityCache.put(key, result);
            }
        }
        return result;
    }

    /* (non-Javadoc)
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */
    public Object put(Object key, Object value) {
        Object candidate = identityCache.put(key, value);
        Object alternate = innerMap.put(key, value);
        return candidate != null ? candidate : alternate;
    }
    
    /**
     * Put to the underlying inner map, without caching. 
     * 
     * @param key key under which to insert the value
     * @param value value to put in underlying map
     */
    public void putNoCache(Object key, Object value) {
        innerMap.put(key,value);
    }

    /* (non-Javadoc)
     * @see java.util.Map#remove(java.lang.Object)
     */
    public Object remove(Object key) {
        Object candidate = identityCache.remove(key);
        Object alternate = innerMap.remove(key);
        return candidate != null ? candidate : alternate;
    }

    /* (non-Javadoc)
     * @see java.util.Map#putAll(java.util.Map)
     */
    public void putAll(Map t) {
        Iterator entries = null;
        entries = t.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry entry = (Map.Entry) entries.next();
            put(entry.getKey(), entry.getValue());
        }
    }

    /* (non-Javadoc)
     * @see java.util.Map#clear()
     */
    public void clear() {
        identityCache.clear();
        innerMap.clear();
    }

    /* (non-Javadoc)
     * @see java.util.Map#keySet()
     */
    public Set keySet() {
        return innerMap.keySet();
    }

    /* (non-Javadoc)
     * @see java.util.Map#values()
     */
    public Collection values() {
        // would require complicated implementation to 
        // maintain identity guarantees, so skipping
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see java.util.Map#entrySet()
     */
    public Set entrySet() {
        // would require complicated implementation to 
        // maintain identity guarantees, so skipping
        throw new UnsupportedOperationException();
    }
}
