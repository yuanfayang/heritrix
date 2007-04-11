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
 * ObjectAndKey.java
 * Created on October 24, 2006
 *
 * $Header$
 */
package org.archive.settings;


import org.archive.state.Key;


/**
 * A processor and a key; used for hashtable lookups.
 * 
 * @author pjack
 *
 * @param <T>
 */
class ObjectAndKey<T> {


    /** The identity hash code of the processor. */
    private int identity;


    /** The key. */
    private Key<T> key;


    /**
     * Constructor.
     * 
     * @param object  the object
     * @param key     the key
     */
    public ObjectAndKey(Object object, Key<T> key) {
        super();
        this.identity = System.identityHashCode(object);
        this.key = key;
    }
    
    
    @Override
    public int hashCode() {
        return identity ^ key.hashCode();
    }
    
    
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ObjectAndKey)) {
            return false;
        }
        ObjectAndKey nk = (ObjectAndKey)o;
        return (nk.identity == identity) && nk.key.equals(key);
    }
    
    
    public int getIdentity() {
        return identity;
    }
}
