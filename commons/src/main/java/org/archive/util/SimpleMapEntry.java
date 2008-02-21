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
 * SimpleMapEntry.java
 *
 * Created on Jun 26, 2007
 *
 * $Id:$
 */

package org.archive.util;

import java.util.Map;

/**
 * @author pjack
 *
 */
public class SimpleMapEntry<K,V> implements Map.Entry<K,V> {

    
    private K key;
    private V value;
    
    
    public SimpleMapEntry(K key, V value) {
        this.key = key;
        this.value = value;
    }
    
    
    public K getKey() {
        return key;
    }
    
    
    public V getValue() {
        return value;
    }
    
    
    public V setValue(V value) {
        V result = this.value;
        this.value = value;
        return result;
    }


    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof Map.Entry)) {
            return false;
        }
        Map.Entry sme = (Map.Entry)other;
        return key.equals(sme.getKey()) && value.equals(sme.getValue());
    }
    
    
    public int hashCode() {
        return (key == null ? 0 : key.hashCode()) 
            ^ (value == null ? 0 : value.hashCode());
    }

    
    public String toString() {
        return key + "=" + value;
    }
}
