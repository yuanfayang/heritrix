/* ServerCacheFactory
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

import org.archive.crawler.Heritrix;
import org.archive.crawler.settings.SettingsHandler;

/**
 * Factory for ServerCache.
 * @author stack
 * @version $Date$, $Revision$
 */
public class ServerCacheFactory {
    private static final ServerCacheFactory factory =
        new ServerCacheFactory();
    
    /**
     * Key to use getting class to instantiate from heritrix.properties.
     */
    private static final String KEY = ServerCacheFactory.class.getName() +
        ".class";
    
    private static String DEFAULT_SERVERCACHE =
        "org.archive.crawler.datamodel.MapServerCache";
    
    /**
     * Constructor.
     */
    private ServerCacheFactory() {
        super();
    }
    
    /**
     * @param handler Settings handler to pass the ServerCache on
     * instantiation.
     * @return Returns ServerCache instance.
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public static ServerCache getServerCache(SettingsHandler handler)
    throws ClassNotFoundException, InstantiationException,
            IllegalAccessException {
        // The ServerCache instance has not been created yet.
        // Go instantiate it.
        String className = Heritrix.getProperty(KEY, DEFAULT_SERVERCACHE);
        Class classDefinition = Class.forName(className);
        ServerCache cache = (ServerCache)classDefinition.newInstance();
        cache.initialize(handler);
        return cache;
    }
}
