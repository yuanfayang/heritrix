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
 * ServerCache.java
 * Created on May 15, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

import java.util.HashMap;

import org.archive.crawler.datamodel.settings.SettingsHandler;
import org.xbill.DNS.FindServer;

/**
 * All CrawlServer instances created by a crawl are held here,
 * to enforce the one instance to one host:port relationship.
 * 
 * @author gojomo
 *
 */
public class ServerCache {
    private final SettingsHandler settingsHandler;
	private HashMap servers = new HashMap(); // hostname[:port] -> CrawlServer
	private HashMap hosts = new HashMap(); // hostname -> CrawlHost
	
    public ServerCache(SettingsHandler settingsHandler) {
        this.settingsHandler = settingsHandler;
    }
    
	public CrawlServer getServerFor(String h) {
		CrawlServer cserver = (CrawlServer) servers.get(h);
		if (cserver==null) {
			cserver = new CrawlServer(h);
            cserver.setSettings(
                settingsHandler.getSettings(cserver.getHostname()));
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
	 * @return CrawlServer
	 */
	public CrawlServer getServerFor(CrawlURI curi) {
		String scheme = curi.getUURI().getUri().getScheme();
		if (scheme.equals("dns")) {
			// set crawlhost to default nameserver
			String primaryDns = FindServer.server();
			if (primaryDns == null) {
                settingsHandler.getOrder().getController()
                    .runtimeErrors.warning("Could not get primary DNS server.");
				return null;
			} else {
				return getServerFor(primaryDns);
			}
		}

		String hostOrAuthority = curi.getUURI().getUri().getAuthority();
		if (hostOrAuthority != null) {
			return getServerFor(hostOrAuthority);
			// TODOSOMEDAY: make this robust against those rare cases
			// where authority is not a hostname
		} else {
			return null;
		}
	}
}
