/* BigMap
 * 
 * Created on Jan 13, 2005
 *
 * Copyright (C) 2005 Internet Archive.
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
import java.io.Serializable;

import org.archive.crawler.settings.SettingsHandler;

/**
 * A map that knows how to manage ever-growing sets of key/value pairs.
 * Implementations must implement {@link Serializable}.
 * @author stack
 * @version $Date$, $Revision$
 */
public interface BigMap extends Map, Serializable {
    /**
     * Initialize this big map.
     * Pass in the crawl settings so map instance can use current
     * configuration.
     * @param settings Current crawler settings.
     * @param dbName Name to give any associated database.
     * @param keyClass Class of keys we'll be using.
     * @param valueClass Class of values we'll be using.
     * @throws Exception
     */
    public void initialize(SettingsHandler settings, String dbName,
        Class keyClass, Class valueClass) throws Exception;
    
    /**
     * Sync in-memory structures to disk in preperation for persisting.
     */
    public void sync();
}
