/*
 * HostCache.java
 * Created on May 15, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

import java.util.HashMap;
import org.xbill.DNS.FindServer;

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
		String scheme = curi.getUURI().getUri().getScheme();
		if (scheme.equals("dns")){
			// TODO: set crawlhost to default nameserver
			String primaryDns = FindServer.server();
			
			if(primaryDns == null){
				return null;
	
			}else{
				return getHostFor(primaryDns);	
			}
		}
		
		String authorityUsuallyHost = curi.getUURI().getUri().getAuthority();
		if (authorityUsuallyHost != null) {
			return getHostFor(authorityUsuallyHost);
			// TODOSOMEDAY: make this robust against those rare cases
			// where authority is not a hostname
		} else {
			return null;
		} 
	}
}
