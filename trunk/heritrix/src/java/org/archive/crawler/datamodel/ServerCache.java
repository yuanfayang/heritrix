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

import java.lang.ref.SoftReference;
import java.util.WeakHashMap;
import java.util.logging.Logger;
//import java.util.logging.Level;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.settings.SettingsHandler;

/**
 * All CrawlServer instances created by a crawl are held here,
 * to enforce the one instance to one host:port relationship.
 *
 * @author gojomo
 *
 */
public class ServerCache {
    private static Logger logger =
        Logger.getLogger(ServerCache.class.getName());
    
    private final SettingsHandler settingsHandler;
    // hostname[:port] -> SoftReference(CrawlServer)
    private WeakHashMap servers = new WeakHashMap();
    // hostname -> SoftReference(CrawlHost)
    private WeakHashMap hosts = new WeakHashMap();

    public ServerCache(SettingsHandler settingsHandler) {
        this.settingsHandler = settingsHandler;
    }

    public CrawlServer getServerFor(String h) {
        SoftReference serverRef = (SoftReference)servers.get(h);
        CrawlServer cserver = (CrawlServer) ((serverRef==null) ?
            null : serverRef.get());
        if (cserver == null) {
            String skey = new String(h); // ensure key is private object
            cserver = new CrawlServer(skey);
            cserver.setSettingsHandler(settingsHandler);
            
            String hostname = cserver.getHostname();
            SoftReference hostRef = (SoftReference) hosts.get(hostname);
            CrawlHost host = (CrawlHost)((hostRef == null) ?
                null: hostRef.get());
            if (host == null) {
                String hkey = new String(hostname); 
                host = new CrawlHost(hkey);
                hosts.put(hkey,new SoftReference(host));
            }
            cserver.setHost(host);
            
            servers.put(skey,new SoftReference(cserver));
        }
        return cserver;
    }

    /**
     * @param curi CrawlURI we're to get server from.
     * @return CrawlServer instance that matches the passed CrawlURI.
     */
    public CrawlServer getServerFor(CrawlURI curi) {
        CrawlServer hostOrAuthority = null;
        try {
            // TODO: evaluate if this is really necessary -- why not 
            // make the server of a dns CrawlURI the looked-up domain,
            // also simplifying FetchDNS?
            String hostOrAuthorityStr =
                curi.getUURI().getAuthorityMinusUserinfo();
            if (hostOrAuthorityStr == null) {
                // Fallback for cases where getAuthority() fails (eg dns:)
                hostOrAuthorityStr = curi.getUURI().getCurrentHierPath();
                if(hostOrAuthorityStr != null &&
                    !hostOrAuthorityStr.matches("[-_\\w\\.:]+")) {
                    // Not just word chars and dots and colons and dashes and 
                    // underscores; throw away
                    hostOrAuthorityStr = null;
                }
            }
            if (hostOrAuthorityStr != null &&
                    curi.getUURI().getScheme().equals(UURIFactory.HTTPS)) {
                // If https and no port specified, add default https port to
                // distinuish https from http server without a port.
                if (!hostOrAuthorityStr.matches(".+:[0-9]+")) {
                    hostOrAuthorityStr += ":" + UURIFactory.HTTPS_PORT;
                }
            }
            // TODOSOMEDAY: make this robust against those rare cases
            // where authority is not a hostname.
            if (hostOrAuthorityStr != null) {
            	hostOrAuthority = getServerFor(hostOrAuthorityStr);
            }
        } catch (URIException e) {
            e.printStackTrace();
        } catch (NullPointerException npe) {
            logger.severe("NullPointerException with " + curi);
            npe.printStackTrace();
        }
        return hostOrAuthority;
    }
    
    public boolean containsServer(String serverKey) {
        SoftReference serverRef = (SoftReference) servers.get(serverKey);
        CrawlServer cserver = (CrawlServer) ((serverRef==null) ? null : serverRef.get());
        return cserver != null; 
    }
    
    public boolean containsHost(String hostKey) {
        SoftReference hostRef = (SoftReference) hosts.get(hostKey);
        CrawlHost chost = (CrawlHost) ((hostRef==null) ? null : hostRef.get());
        return chost != null; 
    }
}
