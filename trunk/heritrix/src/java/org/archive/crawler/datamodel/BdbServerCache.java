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

import java.util.logging.Logger;

import org.archive.crawler.settings.SettingsHandler;

import com.sleepycat.je.DatabaseException;

/**
 * All CrawlServer instances created by a crawl are held here, to enforce the
 * one instance to one host:port relationship.
 * 
 * @author gojomo
 *  
 */
public class BdbServerCache extends ServerCache {

    private static final Logger logger =
        Logger.getLogger(BdbServerCache.class.getName());

    BdbServerCache() {
        super();
        logger.info("Instantiating BdbServerCache");
    }

    public void initialize(SettingsHandler settings) {
        this.settingsHandler = settings;

        try {
            this.servers = BigMapFactory.getBigMap(settings, "servers",
                String.class, CrawlServer.class);
            this.hosts = BigMapFactory.getBigMap(settings, "hosts",
                String.class, CrawlHost.class);
        } catch (DatabaseException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}