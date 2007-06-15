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
 * SheetMap.java
 *
 * Created on Feb 9, 2007
 *
 * $Id:$
 */
package org.archive.settings;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantLock;


/**
 * A hashtable that uses identity-based semantics, has weak keys and is highly
 * concurrent.
 * 
 * <p>
 * <b>Identity-Based Semantics:</b>
 * 
 * <p>
 * This class behaves similarly to {@link java.util.IdentityHashMap}, in that
 * the == operator, and not the {@link Object#equals(Object)} method, is used to
 * determine equality of keys. We want every module to map to its own distinct
 * settings, even if two modules are otherwise identical.
 * 
 * <p>
 * However, unlike IdentityHashMap, this class does <i>not</i> use
 * {@link java.lang.System#identityHashCode(Object)} to determine the hash codes
 * for objects. Instead, {@link Object#hashCode()} is used. My performance
 * testing showed that <code>System.identityHashCode(Object)</code> doesn't
 * scale well under concurrent threads; it seems to acquire some sort of
 * JVM-wide lock in order to return its results. Weirdly, the default
 * implemenation of <code>Object.hashCode()</code> doesn't share that
 * performance bottlenock, even though it always returns the same result as
 * <code>System.identityHashCode(Object)</code>. Sun has an open bug on this
 * issue: {@link http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6378256}.
 * 
 * <p>
 * So instead of using <code>System.identityHashCode</code>, I'm using
 * standard <code>Object.hashCode()</code> to create hashes for objects. For
 * all our module implementations (Processors, DecideRules and so on), this will
 * return the same result as <code>System.identityHashCode</code> anyway,
 * since we don't override the default <code>hashCode()</code>.
 * 
 * <p>
 * It wouldn't be a problem if we did override <code>hashCode()</code>,
 * assuming the overridden method randomly distributed the hashes like it's
 * supposed to.
 * 
 * <p>
 * <b>Weak Keys:</b>
 * 
 * <p>
 * This class behaves similarly to {@link java.util.WeakHashMap}, in that the
 * existence of a key/value mapping will not prevent the key from being garbage
 * collected. When the key is garbage collected, its mapping is essentially
 * removed from the dictionary.
 * 
 * <p>
 * Rather than using a ReferenceQueue and having a thread scan for removed keys,
 * this class instead checks for stale mappings during other operations. If any
 * stale keys are found, then <i>all</i> stale keys are removed from the
 * dictionary. This pruning operation is expensive as it requires iterating over
 * the entire dictionary under the write lock (see below). However, for
 * SingleSheet's purposes, this should be fine: We'll only lose keys as the
 * result of some configuration change, and we expect those to be rare during
 * the lifetime of a crawl.
 * 
 * <p>
 * <b>High Concurrency:</b>
 * 
 * <p>
 * This class behaves similarly to
 * {@link java.util.concurrent.ConcurrentHashMap}, in that it is safe for use
 * by multiple concurrent threads, yet read operations normally do not incur any
 * locking costs.  A read operation might incur a locking cost if a stale
 * mapping is discovered while traversing a particular bucket; that will 
 * trigger an expensive pruning operation.
 * 
 * <p>
 * (Actually, read operations <i>do</i> incur the cost of reading volatile
 * fields, but that's unavoidable and not as bad as full synchronization.)
 * 
 * <p>
 * Insertions and deletions are performed under a global write lock.  This is
 * unlike ConcurrentHashMap, which partitions the hash table into multiple
 * segments, each with its own write lock.  That approach makes growing the
 * hash table problematic, though, especially since we have mappings that
 * might be stale.
 * 
 * <p>
 * Read operations can occur without locking even during write operations.
 * 
 * <p>
 * <b>Other Notes</b>
 * 
 * <p>
 * This class does not implement the full Map interface, since doing so is
 * probably impossible, and I only need get, put and remove operations. As such,
 * this class does not provide collection views or bulk operations on the
 * dictionary, or even a size() operation.
 * 
 * <p>
 * This class is fully serializable.  Serialization does not involve locking,
 * and can occur while other read or write operations are taking place.
 * 
 * @author pjack
 * 
 * @param <K>  the type of keys in the map
 * @param <V>  the type of values in the map
 */
class SheetMap<K,V> implements Serializable {


    /**
     * First version of this class.
     */
    private static final long serialVersionUID = 1L;


    static class Node<K,V> {
        public final WeakReference<K> key;
        public final int hash;
        public final Node<K,V> next;

        public volatile V value;

        
        public Node(int hash, WeakReference<K> key, V value, Node<K,V> next) {
            this.key = key;
            this.value = value;
            this.next = next;
            this.hash = hash;
        }
        
        
        public Node(int hash, K key, V value, Node<K,V> next) {
            this.key = new WeakReference<K>(key);
            this.value = value;
            this.next = next;
            this.hash = hash;
        }

        
        public boolean equals(Object other) {
            if (!(other instanceof Node)) {
                return false;
            }
            
            Node n = (Node)other;
            
            Object k1 = key.get();
            Object k2 = n.key.get();
            if (!eq(k1, k2)) {
                return false;
            }
            
            Object v1 = value;
            Object v2 = n.value;
            return eq(v1, v2);
        }
        
        
        private boolean eq(Object o1, Object o2) {
            if ((o1 == null) && (o2 == null)) {
                return true;
            }
            if ((o1 == null) || (o2 == null)) {
                return false;
            }
            return o1.equals(o2);
        }
        
        
        public int hashCode() {
            return (value == null) ? 0 : value.hashCode();
        }
    }


    private transient ReentrantLock lock;
    transient volatile AtomicReferenceArray<Node<K,V>> buckets;
    transient int size;
    transient int threshold;
    
    
    public SheetMap() {
        @SuppressWarnings("unchecked")
        Node<K,V>[] arr = new Node[16];
        this.buckets = new AtomicReferenceArray<Node<K,V>>(arr);
        calculateThreshold();
        lock = new ReentrantLock();
    }

    
    List<Node<K,V>> rawBuckets() {
        AtomicReferenceArray<Node<K,V>> localBuckets = buckets;
        List<Node<K,V>> result = new ArrayList<Node<K,V>>(localBuckets.length());
        for (int i = 0; i < localBuckets.length(); i++) {
            result.add(localBuckets.get(i));
        }
        return result;
    }
    
    
    
    private void calculateThreshold() {
        // TODO: Make tunable
        threshold = buckets.length() * 75 / 100;
    }


    public V get(K key) {
        if (key == null) {
            throw new IllegalArgumentException("null key");
        }
        int hash = hashFor(key);
        AtomicReferenceArray<Node<K,V>> localBuckets = buckets;
        int bucketNo = hash & (localBuckets.length() - 1);
        Node<K,V> head = localBuckets.get(bucketNo);

        boolean stale = false;
        try {
            for (Node<K,V> n = head; n != null; n = n.next) {
                K nodeKey = n.key.get();
                if (nodeKey == key) {
                    return n.value;
                } else if (nodeKey == null) {
                    stale = true;
                }
            }
            return null;
        } finally {
            if (stale) {
                removeStale();
            }
        }
    }


    public V putIfAbsent(K key, V value) {
        int hash = hashFor(key);
        boolean stale = false;

        lock.lock();
        try {            
            int x = hash & (buckets.length() - 1);
            Node<K,V> head = buckets.get(x);
            for (Node<K,V> n = head; n != null; n = n.next) {
                K test = n.key.get();
                if (test == null) {
                    stale = true;
                } else {
                    if (test == key) {
                        return n.value;
                    }
                }
            }
                        
            buckets.set(x, new Node<K,V>(hash, key, value, head)); 
            size++;
            if (size > threshold) {
                rehash();
            }
            return null;
        } finally {
            lock.unlock();
            if (stale) {
                removeStale();
            }
        }
    }
    
    
    public V remove(K key) {
        int hash = hashFor(key);

        lock.lock();
        try {
            int bucket = hash & (buckets.length() - 1);
            Node<K,V> head = buckets.get(bucket);
            V result = null;
            for (Node<K,V> n = head; n != null; n = n.next) {
                 if (n.key.get() == key) {
                     result = n.value;
                     n.value = null;
                 }
            }
            
            if (result == null) {
                return null;
            }
            
            size--;
            
            Node<K,V> newHead = null;
            for (Node<K,V> n = head; n != null; n = n.next) {
                if ((n.key.get() != null) && (n.value != null)) {
                    newHead = new Node<K,V>(n.hash, n.key, n.value, newHead);
                }
            }
            buckets.set(bucket, newHead);
            
            return result;
        } finally {
            lock.unlock();
        }
    }

    
    private void rehash() {
        // Assuming lock is already obtained!
        @SuppressWarnings("unchecked")
        Node<K,V>[] arr = new Node[buckets.length() * 2];
        AtomicReferenceArray<Node<K,V>> mod = 
            new AtomicReferenceArray<Node<K,V>>(arr);
        int mask = mod.length() - 1;
        
        // TODO: Use something more elegant than brute force here.
        for (int i = 0; i < buckets.length(); i++) {
            for (Node<K,V> n = buckets.get(i); n != null; n = n.next) {
                if (n.key.get() == null) {
                    // key was garbage collected, don't copy node
                    size--;
                } else {
                    // calculate index for modified bucket array
                    int x = n.hash & mask;
                    Node<K,V> head = mod.get(x);
                    mod.set(x, new Node<K,V>(n.hash, n.key, n.value, head));
                }
            }
        }
        
        buckets = mod;
        calculateThreshold();
    }
    
    private void removeStale() {
        lock.lock();
        try {
            for (int i = 0; i < buckets.length(); i++) {
                Node<K,V> newHead = null;
                for (Node<K,V> n = buckets.get(i); n != null; n = n.next) {
                    if (n.key.get() != null) {
                        newHead = new Node<K,V>(n.hash, n.key, n.value, newHead);
                    } else {
                        size--;
                    }
                }
                buckets.set(i, newHead);
            }
        } finally {
            lock.unlock();
        }
    }


    /**
     * Used to provide a hopefully random distribution of identityHashCodes.
     * 
     * <p>I haven't tested this directly. It's taken from Thomas Wang's
     * 32-bit mix function, which Apache Jakarta Commons uses in its Map 
     * implementations.
     * 
     * @param key   an identity hash code
     * @return      a randomized hash code
     * @see http://www.concentric.net/~Ttwang/tech/inthash.htm
     */
    private static int hash32shift(int key) {
        key = ~key + (key << 15); // key = (key << 15) - key - 1;
        key = key ^ (key >>> 12);
        key = key + (key << 2);
        key = key ^ (key >>> 4);
        key = key * 2057; // key = (key + (key << 3)) + (key << 11);
        key = key ^ (key >>> 16);
        return key;
    }

    
    private static int hashFor(Object o) {
        int hash = o.hashCode();
        hash = hash32shift(hash);
        return hash;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        
        // Keep a local copy of the buckets, in case they change.
        AtomicReferenceArray<Node<K,V>> localBuckets = buckets;
        out.writeInt(localBuckets.length());
        for (int i = 0; i < localBuckets.length(); i++) {
            Node<K,V> head = localBuckets.get(i);
            for (Node<K,V> n = head; n != null; n = n.next) {
                K key = n.key.get();
                V value = n.value;
                if ((key != null) && (value != null)) {
                    out.writeObject(key);
                    out.writeObject(value);
                }
            }
        }
        out.writeObject(null);
    }

    
    public Map<K,V> snapshot() {
        Map<K,V> result = new HashMap<K,V>();
        AtomicReferenceArray<Node<K,V>> localBuckets = buckets;
        for (int i = 0; i < localBuckets.length(); i++) {
            Node<K,V> head = localBuckets.get(i);
            for (Node<K,V> n = head; n != null; n = n.next) {
                K key = n.key.get();
                V value = n.value;
                if ((key != null) && (value != null)) {
                    result.put(key, value);
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) 
    throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        
        this.lock = new ReentrantLock();
        this.size = 0;
        
        this.buckets = new AtomicReferenceArray<Node<K,V>>(in.readInt());
        this.calculateThreshold();
        
        K key = (K)in.readObject();
        while (key != null) {
            V value = (V)in.readObject();
            int hash = hashFor(key);
            int bucket = hash & (buckets.length() - 1);
            
            Node<K,V> old = buckets.get(bucket);
            buckets.set(bucket, new Node<K,V>(hash, key, value, old));
            size++;
            
            key = (K)in.readObject();
        }
    }

    

    // For unit testing only.
    Set<Object> makeNodeSet() {
        Set<Object> result = new HashSet<Object>();
        AtomicReferenceArray<Node<K,V>> local = buckets;
        for (int i = 0; i < local.length(); i++) {
            for (Node<K,V> n = local.get(i); n != null; n = n.next) {
                if ((n.key != null) && (n.value != null)) {
                    result.add(n);
                }
            }
        }
        return result;
    }
}
