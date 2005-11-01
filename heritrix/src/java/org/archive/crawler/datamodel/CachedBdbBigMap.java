/* CachedBdbBigMap
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

import java.io.Serializable;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.settings.SettingsHandler;
import org.archive.util.CachedBdbMap;

import com.sleepycat.collections.StoredIterator;
import com.sleepycat.je.DatabaseException;

/**
 * Implementation of BigMap based on CachedBdbMap
 * @author stack
 * @version $Date$, $Revision$
 */
public class CachedBdbBigMap extends CachedBdbMap
implements BigMap, Serializable {
    private static final long serialVersionUID = -2230838823763943383L;
    private static final Logger LOGGER =
        Logger.getLogger(CachedBdbBigMap.class.getName());
    
    /**
     * Constructor.
     */
    public CachedBdbBigMap() {
        super();
    }
    
    /**
     * Setup the big map.
     * @param settings Context.
     * @param dbName Name of db to use.
     * @param keyClass Type for key.
     * @param valueClass Type for value.
     * @throws DatabaseException 
     */
    public void initialize(SettingsHandler settings, String dbName,
            Class keyClass, Class valueClass)
    throws DatabaseException {
        CrawlController c = settings.getOrder().getController();
        this.db = openDatabase(c.getBdbEnvironment(), dbName);
        this.diskMap = createDiskMap(this.db, c.getClassCatalog(),
            keyClass, valueClass);
        if (LOGGER.isLoggable(Level.FINE)) {
            Iterator i = this.diskMap.keySet().iterator();
            try {
                for (; i.hasNext();) {
                    LOGGER.severe(dbName + " " + (String) i.next());
                }
            } finally {
                StoredIterator.close(i);
            }
        }
        // The below code was added to deal with case where we were not
        // managing diskMapSize across checkpoint/recover. This has since been
        // fixed.  The code below should not be triggered subsequent to
        // recover of bnf crawl (10/2005). Can be removed thereafter. See
        // '[ 1324989 ] Queue counts wrong after checkpointing'.
        if (this.diskMapSize == 0) {
            Iterator iter = this.diskMap.keySet().iterator();
            while(iter.hasNext()) {
                iter.next();
                diskMapSize++;
            }
            StoredIterator.close(iter);
        }
    }

    public void clear() {
        super.clear();
        // Close out my bdb db.
        if (this.db != null) {
            try {
                this.db.close();
            } catch (DatabaseException e) {
                e.printStackTrace();
            }
            this.db = null;
        }
    }
}
