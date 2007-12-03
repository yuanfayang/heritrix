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
 * UnmodifiableTypedMap.java
 *
 * Created on Jun 4, 2007
 *
 * $Id:$
 */

package org.archive.settings;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author pjack
 *
 */
class UnmodifiableTypedMap<T> extends AbstractMap<String,T> 
implements TypedMap<T> {

    
    private Map<String,T> delegate;
    private Class<T> elementType;
    private Sheet sheet;
    
    public UnmodifiableTypedMap(TypedMap<T> delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException();
        }
        this.delegate = delegate;
        this.elementType = delegate.getElementType();
    }
    
    
    public UnmodifiableTypedMap(Sheet sheet, Map<String,T> map, Class<T> elementType) {
        this.delegate = map;
        this.sheet = sheet;
        this.elementType = elementType;
    }
    
    
    public boolean isEmpty() {
        return delegate.isEmpty();
    }


    public int size() {
        return delegate.size();
    }


    public T get(Object key) {
        return delegate.get(key);
    }


    public Set<String> keySet() {
        return Collections.unmodifiableSet(delegate.keySet());
    }
    
    
    public Collection<T> values() {
        return Collections.unmodifiableCollection(delegate.values());
    }
    
    
    public Set<Map.Entry<String,T>> entrySet() {
        return Collections.unmodifiableSet(delegate.entrySet());
    }
    
    
    public Class<T> getElementType() {
        return elementType;
    }
    
    
    public List<Sheet> getSheets(String key) {
        if (delegate instanceof TypedMap) {
            TypedMap<T> tm = (TypedMap<T>)delegate;
            return tm.getSheets(key);
        }
        return Collections.singletonList(sheet);
    }

}
