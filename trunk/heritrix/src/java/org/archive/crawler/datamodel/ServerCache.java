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

import org.archive.crawler.settings.SettingsHandler;

/**
 * @author stack
 * @version $Date$, $Revision$
 */
public interface ServerCache {
    /**
     * Initialization called after initial instantiation.
     * @param settingsHandler Settings handler to use.
     */
    public abstract void initialize(SettingsHandler settingsHandler);
    
    /**
     * Get the {@link CrawlServer} associated with <code>name</code>.
     * @param serverKey Server name we're to return server for.
     * @return CrawlServer instance that matches the passed server name.
     */
    public abstract CrawlServer getServerFor(String serverKey);

    /**
     * Get the {@link CrawlServer} associated with <code>curi</code>.
     * @param curi CrawlURI we're to get server from.
     * @return CrawlServer instance that matches the passed CrawlURI.
     */
    public abstract CrawlServer getServerFor(CrawlURI curi);
    
    /**
     * Get the {@link CrawlHost} associated with <code>name</code>.
     * @param hostname Host name we're to return Host for.
     * @return CrawlHost instance that matches the passed Host name.
     */
    public abstract CrawlHost getHostFor(String hostname);
    
    /**
     * Get the {@link CrawlHost} associated with <code>curi</code>.
     * @param curi CrawlURI we're to return Host for.
     * @return CrawlHost instance that matches the passed Host name.
     */
    public abstract CrawlHost getHostFor(CrawlURI curi);

    /**
     * @param serverKey Key to use doing lookup.
     * @return True if a server instance exists.
     */
    public abstract boolean containsServer(String serverKey);

    /**
     * @param hostKey Key to use doing lookup.
     * @return True if a host instance exists.
     */
    public abstract boolean containsHost(String hostKey);
}