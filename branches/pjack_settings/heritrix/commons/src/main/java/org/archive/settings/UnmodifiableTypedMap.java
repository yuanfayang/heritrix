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

    
    private TypedMap<T> delegate;
    
    
    public UnmodifiableTypedMap(TypedMap<T> delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException();
        }
        this.delegate = delegate;
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
        return delegate.getElementType();
    }
    
    
    public List<Sheet> getSheets(String key) {
        return delegate.getSheets(key);
    }

}
