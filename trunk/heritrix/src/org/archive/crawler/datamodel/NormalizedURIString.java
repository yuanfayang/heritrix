/*
 * NormalizedURIString.java
 * Created on Apr 17, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;

/**
 * Wrapper to indicate a URI string has been normalized,
 * and cache
 * @author gojomo
 *
 */
public class NormalizedURIString {
	String normalizedURI;
	SoftReference cachedURI; 		// maybe
	WeakReference cachedCrawlUri;	// maybe
 
	/**
	 * Return a NormalizedURIString for the given String.
	 * 
	 * Normalization cleans a URI to the maximum extent
	 * possible without regard to what it returns or 
	 * special-casing based on past observed behavior.
	 * 
	 * For example, the URI scheme is case-flattened, 
	 * hostnames are case-flattened, default ports are
	 * removed, and path-info is regularized. 
	 * 
	 * @param u
	 * @return
	 */
	public static NormalizedURIString normalize(String u) {
		// TODO implement
		return null;
	}
}
