/*
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

}
