/* BigMapFactory
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.archive.crawler.Heritrix;
import org.archive.crawler.checkpoint.Checkpoint;
import org.archive.crawler.settings.SettingsHandler;

/**
 * Factory for Maps that know how to hold ever-growing set of key/value pairs.
 * Implementations know how to persist part of the map to disk rather
 * than try and hold the total Map in memory.
 * @author stack
 * @version $Date$, $Revision$
 */
public class BigMapFactory {
    private static final String KEY_BASE = BigMapFactory.class.getName();
    private static final String KEY =  KEY_BASE + ".class";
    private static final Logger LOGGER = Logger.getLogger(KEY_BASE);
    
    /**
     * Keep a list of all instances made -- shouldn't be many -- so that
     * we can checkpoint.
     */
    transient private static List instances =
        Collections.synchronizedList(new ArrayList());
   
    private BigMapFactory() {
        super();
    }
    
    /**
     * Call this method to get instance of the crawler BigMap implementation.
     * If none available, returns a {@link java.util.HashMap}.
     * @param settings Crawl settings.
     * @param dbName Name to give any associated database.
     * @param keyClass Class of keys we'll be using.
     * @param valueClass Class of values we'll be using.
     * @throws Exception
     * @return Map that knows how to carry large sets of key/value pairs or
     * if none available, returns instance of HashMap.
     */
    public static Map getBigMap(SettingsHandler settings, String dbName,
            Class keyClass, Class valueClass)
    throws Exception {
        Map result = null;
        // Try and get an instance of BigMap, if there is one configured into
        // Heritrix. If not, we'll just go with default HashMap.
        String className = System.getProperty(BigMapFactory.KEY);
        if (className != null) {
            Class classDefinition = Class.forName(className);
            result = (Map)classDefinition.newInstance();
            ((BigMap)result).initialize(settings, dbName, keyClass,
                valueClass);
        } else {
            result = new HashMap();
        }
        BigMapFactory.instances.add(result);
        return result;
    }

    public static void checkpoint()
    throws Exception {
        for (Iterator i = BigMapFactory.instances.iterator(); i.hasNext();) {
            Object obj = i.next();
            if (obj instanceof BigMap) {
                ((BigMap)obj).sync();
            } else {
                LOGGER.info("Checkpointing unsupported for type " + obj);
            }
        }
    }
}
