/*
 * ServerCache.java
 * Created on May 15, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

import java.util.HashMap;
import org.xbill.DNS.FindServer;

/**
 * All CrawlServer instances created by a crawl are held here,
 * to enforce the one instance to one host:port relationship.
 * 
 * @author gojomo
 *
 */
public class ServerCache {
	private HashMap servers = new HashMap(); // hostname[:port] -> CrawlServer
	private HashMap hosts = new HashMap(); // hostname -> CrawlHost
	
	public CrawlServer getServerFor(String h) {
		CrawlServer cserver = (CrawlServer) servers.get(h);
		if (cserver==null) {
			cserver = new CrawlServer(h);
			servers.put(h,cserver);
		}
		String hostname = cserver.getHostname();
		CrawlHost host = (CrawlHost) hosts.get(hostname);
		if (host==null) {
			host = new CrawlHost(hostname);
			hosts.put(hostname,host);
		}
		cserver.setHost(host);
		return cserver;
	}

	/**
	 * @param curi
	 * @return
	 */
	public CrawlServer getServerFor(CrawlURI curi) {
		String scheme = curi.getUURI().getUri().getScheme();
		if (scheme.equals("dns")) {
			// set crawlhost to default nameserver
			String primaryDns = FindServer.server();
			if (primaryDns == null) {
				return null;
			} else {
				return getServerFor(primaryDns);
			}
		}

		String hostOrAuthority = curi.getUURI().getUri().getHost();
		if (hostOrAuthority != null) {
			return getServerFor(hostOrAuthority);
			// TODOSOMEDAY: make this robust against those rare cases
			// where authority is not a hostname
		} else {
			return null;
		}
	}
}
