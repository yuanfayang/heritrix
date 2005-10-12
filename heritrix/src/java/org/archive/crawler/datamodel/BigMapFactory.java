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

import java.io.File;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import org.archive.crawler.checkpoint.CheckpointContext;
import org.archive.crawler.framework.CrawlController;
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
     * Name of the subdir in the checkpointing dir in which we keep our
     * serialized bigmaps.
     */
    private static final String CHECKPOINT_SUBDIR = KEY_BASE;
    
    /**
     * Keep a list of all instances made -- shouldn't be many -- so that
     * we can checkpoint.
     */
    transient private static Map instances = new Hashtable();
   
    private BigMapFactory() {
        super();
    }
    
    /**
     * Call this method to get instance of the crawler BigMap implementation.
     * If none available, returns a {@link java.util.HashMap}.  If we're in
     * a checkpoint recovery, will manage reinstantiation of checkpointed
     * bigmaps.
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
            // If checkpointing, check to see the bigmapfactory subdir
            // exists. If it does, deserialize and return the deserialized
            // instance.  If it does not, then its the special case where
            // checkpointing of big maps was broken; we were not persisting
            // out big map count.  A workaround was put into the initialize to
            // handle case where we weren't persisting.  Can remove this check
            // after first recover of october BNF crawl.  See
            // '[ 1324989 ] Queue counts wrong after checkpointing'.
            CrawlController c = null;
            if (settings != null) {
                c = settings.getOrder().getController();
            }
            if (c != null && c.isCheckpointRecover()) {
                File baseDir =
                    getCheckpointDir(c.getCheckpointRecover().getDirectory());
                if (baseDir.exists()) {
                    // Do a getClass on result.  Will ensure we're deserializing
                    // correct class.  Will fail if config. for BigMap has
                    // been changed since checkpoint.
                    result = (Map)CheckpointContext.
                        readObjectFromFile(result.getClass(), dbName, baseDir);
                }
            }
            ((BigMap)result).initialize(settings, dbName, keyClass, valueClass);
        } else {
            // Default to use hashmap.
            result = new HashMap();
        }
        // Save reference to all big maps made so can manage checkpointing.
        BigMapFactory.instances.put(dbName, result);
        return result;
    }

    public static void checkpoint(final File baseCheckpointDir)
    throws Exception {
        for (Iterator i = BigMapFactory.instances.keySet().iterator();
                i.hasNext();) {
            Object key = i.next();
            Object obj = BigMapFactory.instances.get(key);
            if (obj instanceof BigMap) {
                // Note: I tried adding sync to custom serialization of BigMap
                // implementation but data member counts of the BigMap
                // implementation were not being persisted properly.  Do sync
                // in advance of serialization for now.
                ((BigMap)obj).sync();
                CheckpointContext.writeObjectToFile(obj,
                     (String)key, getCheckpointDir(baseCheckpointDir));
            } else {
                LOGGER.info("Checkpointing unsupported for type " + obj);
            }
        }
    }
    
    protected static File getCheckpointDir(final File baseCheckpointDir) {
        return new File(baseCheckpointDir, CHECKPOINT_SUBDIR);
    }
}
