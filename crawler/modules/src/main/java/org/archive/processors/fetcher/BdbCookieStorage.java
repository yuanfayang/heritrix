/* 
 * Copyright (C) 2007 Internet Archive.
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
 * BdbCookieStorage.java
 *
 * Created on Apr 18, 2007
 *
 * $Id:$
 */

package org.archive.processors.fetcher;

import java.util.Map;
import java.util.SortedMap;
import java.util.logging.Logger;

import org.apache.commons.httpclient.Cookie;
import org.archive.settings.file.BdbModule;
import org.archive.state.Immutable;
import org.archive.state.Initializable;
import org.archive.state.Key;
import org.archive.state.KeyManager;
import org.archive.state.StateProvider;

import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.bind.tuple.StringBinding;
import com.sleepycat.collections.StoredSortedMap;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseException;

/**
 * @author pjack
 *
 */
public class BdbCookieStorage implements CookieStorage, Initializable {

    
    final private static Logger LOGGER = 
        Logger.getLogger(BdbCookieStorage.class.getName()); 
    
    @Immutable
    final public static Key<BdbModule> BDB = Key.make(BdbModule.class, null);
    
    @Immutable
    final public static Key<String> COOKIEDB_NAME = Key.make("http_cookies");
    
    
    static {
        KeyManager.addKeys(BdbCookieStorage.class);
    }

    
    private BdbModule bdb;
    private Database cookieDb;
    private StoredSortedMap cookies;

    public BdbCookieStorage() {
    }


    public void initialTasks(StateProvider provider) {
        this.bdb = provider.get(this, BDB);
        String dbName = provider.get(this, COOKIEDB_NAME);
        try {
            StoredClassCatalog classCatalog = bdb.getClassCatalog();
            DatabaseConfig dbConfig = new DatabaseConfig();
            dbConfig.setTransactional(false);
            dbConfig.setAllowCreate(true);
            dbConfig.setDeferredWrite(true);
            cookieDb = bdb.openDatabase(dbName, dbConfig, true);
            cookies = new StoredSortedMap(cookieDb,
                    new StringBinding(), new SerialBinding(classCatalog,
                            Cookie.class), true);
        } catch (DatabaseException e) {
            LOGGER.severe(e.getMessage());
            e.printStackTrace();
        }
    }


    @SuppressWarnings("unchecked")
    public SortedMap<String, Cookie> loadCookiesMap() {
        return cookies;
    }


    public void saveCookiesMap(Map<String, Cookie> map) {
    }
    
    
    
}