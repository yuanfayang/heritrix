/* ServerCache
 * 
 * Created on Nov 19, 2004
 *
 * Copyright (C) 2004 Internet Archive.
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
 */
package org.archive.crawler.datamodel;

import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.settings.SettingsHandler;

/**
 * @author stack
 * @version $Date$, $Revision$
 */
public abstract class ServerCache {
    private static Logger logger =
        Logger.getLogger(ServerCache.class.getName());
    
    protected SettingsHandler settingsHandler = null;
    
    /**
     * hostname[:port] -> CrawlServer.
     * Set in the initialization.
     */
    protected Map servers = null;
    
    /**
     * hostname -> CrawlHost.
     * Set in the initialization.
     */
    protected Map hosts = null;
    
    /**
     * Initialization called after initial instantiation.
     * @param handler Settings handler to use.
     */
    public abstract void initialize(SettingsHandler handler);
    
    /**
     * Get the {@link CrawlServer} associated with <code>name</code>.
     * @param serverKey Server name we're to return server for.
     * @return CrawlServer instance that matches the passed server name.
     */
    public CrawlServer getServerFor(String serverKey) {
        CrawlServer cserver = (CrawlServer)this.servers.get(serverKey);
        return (cserver != null)? cserver: createServerFor(serverKey);
    }
    
    private synchronized CrawlServer createServerFor(String s) {
        CrawlServer cserver = (CrawlServer)this.servers.get(s);
        if (cserver != null) {
            return cserver;
        }
        // Ensure key is private object
        String skey = new String(s);
        cserver = new CrawlServer(skey);
        cserver.setSettingsHandler(settingsHandler);
        servers.put(skey,cserver);
        return cserver;
    }

    /**
     * Get the {@link CrawlServer} associated with <code>curi</code>.
     * @param curi CrawlURI we're to get server from.
     * @return CrawlServer instance that matches the passed CrawlURI.
     */
    public CrawlServer getServerFor(CrawlURI curi) {
        CrawlServer cs = null;
        try {
            String key = CrawlServer.getServerKey(curi);
            // TODOSOMEDAY: make this robust against those rare cases
            // where authority is not a hostname.
            if (key != null) {
                cs = getServerFor(key);
            }
        } catch (URIException e) {
            logger.severe(e.getMessage() + ": " + curi);
            e.printStackTrace();
        } catch (NullPointerException npe) {
            logger.severe(npe.getMessage() + ": " + curi);
            npe.printStackTrace();
        }
        return cs;
    }
    
    /**
     * Get the {@link CrawlHost} associated with <code>name</code>.
     * @param hostname Host name we're to return Host for.
     * @return CrawlHost instance that matches the passed Host name.
     */
    public CrawlHost getHostFor(String hostname) {
        if (hostname == null || hostname.length() == 0) {
            return null;
        }
        CrawlHost host = (CrawlHost)this.hosts.get(hostname);
        return (host != null)? host: createHostFor(hostname);
    }
    
    public synchronized CrawlHost createHostFor(String hostname) {
        if (hostname == null || hostname.length() == 0) {
            return null;
        }
        CrawlHost host = (CrawlHost)this.hosts.get(hostname);
        if (host != null) {
            return host;
        }
        String hkey = new String(hostname); 
        host = new CrawlHost(hkey);
        this.hosts.put(hkey, host);
        return host;
    }
    
    /**
     * Get the {@link CrawlHost} associated with <code>curi</code>.
     * @param curi CrawlURI we're to return Host for.
     * @return CrawlHost instance that matches the passed Host name.
     */
    public CrawlHost getHostFor(CrawlURI curi) {
        CrawlHost h = null;
        try {
            h = getHostFor(curi.getUURI().getReferencedHost());
        } catch (URIException e) {
            e.printStackTrace();
        }
        return h;
    }

    /**
     * @param serverKey Key to use doing lookup.
     * @return True if a server instance exists.
     */
    public boolean containsServer(String serverKey) {
        return (CrawlServer) servers.get(serverKey) != null; 
    }

    /**
     * @param hostKey Key to use doing lookup.
     * @return True if a host instance exists.
     */
    public boolean containsHost(String hostKey) {
        return (CrawlHost) hosts.get(hostKey) != null; 
    }
}