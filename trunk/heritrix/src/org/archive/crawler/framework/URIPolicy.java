/*
 * URIPolicy.java
 * Created on Apr 17, 2003
 *
 * $Header$
 */
package org.archive.crawler.framework;

/**
 * Decides how incoming CrawlURIs and associated URI strings
 * should be handled by the URIManager.
 * 
 * All knowledge about URI seen/visited/etc histories should 
 * be taken from URIManager.
 * 
 * @author gojomo
 *
 */
public interface URIPolicy {
	
	public void setManager(URIManager m);
	
	public void consider(CrawlURI curi);
	
	public void consider(String uristr);
	
	public void init(Config cfg);
}
