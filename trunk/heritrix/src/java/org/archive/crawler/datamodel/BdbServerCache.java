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

import java.io.File;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.settings.SettingsHandler;
import org.archive.util.CachedBdbMap;

import com.sleepycat.je.DatabaseException;

/**
 * All CrawlServer instances created by a crawl are held here, to enforce the
 * one instance to one host:port relationship.
 * 
 * @author gojomo
 *  
 */
public class BdbServerCache implements ServerCache {

    private static final Logger logger = Logger.getLogger(BdbServerCache.class
            .getName());

    private SettingsHandler settingsHandler;

    private Map servers;

    private Map hosts;

    BdbServerCache() {
        super();
        logger.info("Instantiating BdbServerCache");
    }

    public CrawlServer getServerFor(String h) {
        CrawlServer cserver = null;
        cserver = (CrawlServer) servers.get(h);
        if (cserver == null) {
            cserver = new CrawlServer(h);
            cserver.setSettingsHandler(settingsHandler);
            servers.put(h, cserver);
        }
        return cserver;
    }

    public CrawlServer getServerFor(CrawlURI curi) {
        CrawlServer hostOrAuthority = null;
        try {
            String key = CrawlServer.getServerKey(curi);
            // TODOSOMEDAY: make this robust against those rare cases
            // where authority is not a hostname.
            if (key != null) {
                hostOrAuthority = getServerFor(key);
            }
        } catch (URIException e) {
            e.printStackTrace();
        } catch (NullPointerException npe) {
            logger.severe("NullPointerException with " + curi);
            npe.printStackTrace();
        }
        return hostOrAuthority;
    }
    
    public CrawlHost getHostFor(String hostname) {
        CrawlHost host = (CrawlHost)this.hosts.get(hostname);
        if (host == null) {
            String hkey = new String(hostname); 
            host = new CrawlHost(hkey);
            hosts.put(hkey, host);
        }
        return host;
    }
    
    public CrawlHost getHostFor(CrawlURI curi) {
        CrawlHost h = null;
        try {
            h = getHostFor(curi.getUURI().getHost());
        } catch (URIException e) {
            e.printStackTrace();
        }
        return h;
    }

    public void initialize(SettingsHandler settings) {
        this.settingsHandler = settings;
        CrawlController controller =
            settingsHandler.getOrder().getController();

        File fileName = new File(controller.getScratchDisk(), "serverCache");
        fileName.mkdirs();
        try {
            servers = new CachedBdbMap(fileName, "servers", String.class,
                    CrawlServer.class);
            hosts = new CachedBdbMap(fileName, "hosts", String.class,
                    CrawlHost.class);
        } catch (DatabaseException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean containsServer(String serverKey) {
        CrawlServer cserver = (CrawlServer) servers.get(serverKey);
        return cserver != null;
    }

    public boolean containsHost(String hostKey) {
        CrawlHost chost = (CrawlHost) hosts.get(hostKey);
        return chost != null;
    }
}