/*
 * URI.java
 * Created on Apr 18, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

/**
 * Usable URI: a legal URI for our purposes.
 * 
 * These instances will always have been normalized 
 * (massaged in ways that by spec and in practice,
 * do not change the URI's meaning or function) and
 * rehabilitated (patched in riskless ways to be
 * legal, eg escaping spaces). 
 * 
 * @author gojomo
 *
 */
public class UURI {
	java.net.URI uri;
	
	public static UURI createURI(String u) {
		return null;
	}
	
	
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
	 static String normalize(String u) {
		// TODO implement
		return null;
	}
}
