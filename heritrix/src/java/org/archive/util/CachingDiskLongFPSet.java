/* Copyright (C) 2003 Internet Archive.
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
 * CachingDiskLongFPSet.java
 * Created on Oct 30, 2003
 *
 * $Header$
 */
package org.archive.util;

import java.io.File;
import java.io.IOException;

/**
 * A disk-based long fingerprint set, but with an in-memory
 * cache which can give positive contains() answers for
 * recently added or contains()-queries values from the
 * fixed-size in-memory cache.
 *
 * @author gojomo
 *
 */
public class CachingDiskLongFPSet extends DiskLongFPSet {
    protected LongFPSetCache cache;

    /**
     * @param dir
     * @param name
     * @param capacityPowerOfTwo
     * @param loadFactor
     * @param cacheCapacityPowerOfTwo
     * @param cacheLoadFactor
     * @throws IOException
     */
    public CachingDiskLongFPSet(
        File dir,
        String name,
        int capacityPowerOfTwo,
        float loadFactor,
        int cacheCapacityPowerOfTwo,
        float cacheLoadFactor)
        throws IOException {
        super(dir, name, capacityPowerOfTwo, loadFactor);
        cache = new LongFPSetCache(cacheCapacityPowerOfTwo, cacheLoadFactor);
    }

    /**
     * @param dir
     * @param name
     * @throws IOException
     */
    public CachingDiskLongFPSet(File dir, String name) throws IOException {
        super(dir, name);
        cache = new LongFPSetCache();
    }

    /* (non-Javadoc)
     * @see org.archive.util.AbstractLongFPSet#add(long)
     */
    public boolean add(long val) {
        cache.add(val);
        return super.add(val);
    }

    /* (non-Javadoc)
     * @see org.archive.util.AbstractLongFPSet#contains(long)
     */
    public boolean contains(long val) {
        if(cache.contains(val)) {
            return true;
        }
        if(super.contains(val)) {
            cache.add(val);
            return true;
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.archive.util.AbstractLongFPSet#remove(long)
     */
    public boolean remove(long val) {
        cache.remove(val);
        return super.remove(val);
    }

    /* (non-Javadoc)
     * @see org.archive.util.LongFPSet#quickContains(long)
     */
    public boolean quickContains(long fp) {
        if(cache.contains(fp)) {
            return true;
        }
        return false;
    }

}
