/*
 * HostCache.java
 * Created on May 15, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

import java.util.HashMap;

/**
 * @author gojomo
 *
 */
public class HostCache {
	private static HashMap hosts = new HashMap(); // hostname -> CrawlHost
	
	public static CrawlHost getHostFor(String h) {
		CrawlHost chost = (CrawlHost) hosts.get(h);
		if (chost==null) {
			chost = new CrawlHost(h);
			hosts.put(h,chost);
		}
		return chost;
	}

	/**
	 * @param curi
	 * @return
	 */
	public CrawlHost getHostFor(CrawlURI curi) {
		return getHostFor(curi.getUURI().getUri().getAuthority());
	}
}
