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
 * MapServerCache.java
 * Created on May 15, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

import java.util.HashMap;
import java.util.logging.Logger;
//import java.util.logging.Level;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.settings.SettingsHandler;

/**
 * All CrawlServer and CrawlHost instances are created and held by this
 * class to enforce the one instance to one host:port relationship.
 *
 * @author gojomo
 */
public class MapServerCache extends ServerCache {
    /**
     * Constructor with default access.
     * Has default access so you have to go via the ServerCacheFactory
     * to get an instance of this class.
     */
    MapServerCache() {
        super();
    }
    
    public void initialize(SettingsHandler handler) {
        this.settingsHandler = handler;
        this.servers = new HashMap();
        this.hosts = new HashMap();
    }
}
