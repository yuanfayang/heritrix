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
}
