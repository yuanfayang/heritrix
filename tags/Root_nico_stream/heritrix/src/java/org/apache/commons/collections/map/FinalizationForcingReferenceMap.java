/* FinalizationForcingReferenceMap
*
* $Id$
*
* Created on Dec 23, 2004
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
package org.apache.commons.collections.map;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;

import org.archive.crawler.frontier.BdbFrontier;
import org.archive.util.ArchiveUtils;

/**
 * A ReferenceMap which only responds to a get() for a broken-reference
 * entry if that entry's value has been finalized. 
 * 
 * Useful inside an IdentityCachingMap, where it prevents the delay between 
 * when an entry's references are cleared, and when the entry's value is 
 * finalized, creating a timing bug. If the ReferenceMap gave the impression
 * any macthing instances were already completely gone (a null response
 * befroe the previous value is finalized), a second instance could be 
 * created from persistent store, which does not yet reflect any changes
 * the last instance's finalize() would have persisted. 
 * 
 * @author gojomo
 */
public class FinalizationForcingReferenceMap extends ReferenceMap {
    // be robust against trivial implementation changes
    private static final long serialVersionUID = ArchiveUtils
            .classnameBasedUID(BdbFrontier.class, 1);
    
    /**
     * default constructor
     */
    public FinalizationForcingReferenceMap() {
        super();
    } 
    
    /**
     * @param keyType
     * @param valueType
     */
    public FinalizationForcingReferenceMap(int keyType, int valueType) {
        super(keyType, valueType);
    }
    
    /**
     * @param keyType
     * @param valueType
     * @param purgeValues
     */
    public FinalizationForcingReferenceMap(int keyType, int valueType,
            boolean purgeValues) {
        super(keyType, valueType, purgeValues);
    }
    
    /**
     * @param keyType
     * @param valueType
     * @param capacity
     * @param loadFactor
     */
    public FinalizationForcingReferenceMap(int keyType, int valueType,
            int capacity, float loadFactor) {
        super(keyType, valueType, capacity, loadFactor);
    }
    
    /**
     * @param keyType
     * @param valueType
     * @param capacity
     * @param loadFactor
     * @param purgeValues
     */
    public FinalizationForcingReferenceMap(int keyType, int valueType,
            int capacity, float loadFactor, boolean purgeValues) {
        super(keyType, valueType, capacity, loadFactor, purgeValues);
    }
    
    /* (non-Javadoc)
     * @see java.util.Map#get(java.lang.Object)
     */
    public Object get(Object key) {
        purgeBeforeRead();
        Entry entry = getEntry(key);
        if (entry == null) {
            return null;
        }
        Object value = entry.getValue();
        if(value == null) {
            // cleared but not yet purged: waiting for finalization
            while(getEntry(key) != null) {
                System.runFinalization();
                purgeBeforeRead();
                // TODO: check if this risks busy-spin, because
                // finalization may be indefinitely delayed
            }
        }
        return value;
    }
    
    /**
     * @author gojomo
     */
    public class PhantomRef extends PhantomReference {
        /** the hashCode of the key (to find the entry at purge time) */
        private int hash;

        public PhantomRef(int hash, Object r, ReferenceQueue q) {
            super(r, q);
            this.hash = hash;
        }

        public int hashCode() {
            return hash;
        }
    }
    
    /**
     * @author gojomo
     */
    public class ReferenceEntryWithPhantom extends ReferenceEntry {
        PhantomRef phantomRef;
        
        /**
         * @param parent
         * @param next
         * @param hashCode
         * @param key
         * @param value
         */
        public ReferenceEntryWithPhantom(AbstractReferenceMap parent, HashEntry next, int hashCode, Object key, Object value) {
            super(parent, next, hashCode, key, value);
            phantomRef = new PhantomRef(hashCode,value,parent.queue);
        }

        /* (non-Javadoc)
         * @see org.apache.commons.collections.map.AbstractReferenceMap.ReferenceEntry#purge(java.lang.ref.Reference)
         */
        boolean purge(Reference ref) {
            if (ref == phantomRef) {
                // now it's OK to remove entry entirely
                phantomRef.clear();
                return true; 
            }
            boolean r = (parent.keyType > HARD) && (key == ref);
            r = r || ((parent.valueType > HARD) && (value == ref));
            if (r) {
                if (parent.keyType > HARD) { // TODO: fix this: shouldn't allow non-hard keys
                    ((Reference)key).clear();
                }
                if (parent.valueType > HARD) {
                    ((Reference)value).clear();
                } else if (parent.purgeValues) {
                    value = null;
                }
            }
            return false; // don't remove entry yet
        }
    }
    
    /* (non-Javadoc)
     * @see org.apache.commons.collections.map.AbstractHashedMap#createEntry(org.apache.commons.collections.map.AbstractHashedMap.HashEntry, int, java.lang.Object, java.lang.Object)
     */
    protected HashEntry createEntry(HashEntry next, int hashCode, Object key,
            Object value) {
        return new ReferenceEntryWithPhantom(this, next, hashCode, key, value);
    }
}
