package org.archive.crawler2.settings;


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
}
