/* CachedBdbMap
 * 
 * $Id$
 * 
 * Created on Mar 24, 2004
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
package org.archive.util;

import java.io.File;
import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.collections.StoredMap;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;

/**
 * A BDB JE backed hashmap. It extends the normal BDB JE map implementation by
 * holding a cache of soft referenced objects. That is objects are not written
 * to disk until they are not refrenced by any other object and therefore can be
 * Garbage Collected.
 * 
 * @author John Erik Halse
 *  
 */
public class CachedBdbMap extends AbstractMap implements Map {

    private static final Logger logger = Logger.getLogger(CachedBdbMap.class
            .getName());

    /** The database name of the class definition catalog */
    private static final String CLASS_CATALOG = "java_class_catalog";

    /**
     * A map of BDB JE Environments so that we reuse the Environment for
     * databases in the same directory.
     */
    private static final Map dbEnvironmentMap = new HashMap();

    /** The BDB JE environment used for this instance. */
    private DbEnvironmentEntry dbEnvironment;

    /** The BDB JE database used for this instance. */
    private Database db;

    /** The Collection view of the BDB JE database used for this instance. */
    private StoredMap diskMap;

    /** The softreferenced cache */
    private Map cache;

    protected ReferenceQueue refQueue = new ReferenceQueue();

    private EntrySet entrySet = new EntrySet(this);

    /** The number of objects stored in the BDB JE database. */
    private int diskMapSize = 0;

    /**
     * The number of objects only in memory. The cache might be bigger cause to
     * GCed objects reloaded from the BDB JE database, thus being both on disk
     * and in memory.
     */
    private long cacheOnlySize = 0;

    private long cacheHit = 0;

    private long getCount = 0;

    private long diskHit = 0;

    private static Field referent;

    /**
     * Simple structure to keep needed information about a DB Environment.
     */
    protected class DbEnvironmentEntry {

        Environment dbEnvironment;

        StoredClassCatalog classCatalog;

        int openDbCount = 0;

        File dbDir;
    }

    /**
     * A constructor for creating a new CachedBdbMap.
     * <p>
     * Even though the put and get methods conforms to the Collections interface
     * taking any object as key or value, you have to submit the class of the
     * allowed key and value objects here and will get an exception if you try
     * to put anything else in the map.
     * 
     * @param dbDir The directory where the database will be created.
     * @param dbName The name of the database to back this map by.
     * @param keyClass The class of the objects allowed as keys.
     * @param valueClass The class of the objects allowed as values.
     * 
     * @throws DatabaseException is thrown if the underlying BDB JE database
     *             throws an exception.
     */
    public CachedBdbMap(File dbDir, String dbName, Class keyClass,
            Class valueClass) throws DatabaseException {
        super();

        // We need access to the referent field in the PhantomReference
        try {
            referent = Reference.class.getDeclaredField("referent");
            referent.setAccessible(true);
        } catch (SecurityException e) {
            throw new RuntimeException(e);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }

        // setup memory cache
        cache = Collections.synchronizedMap(new HashMap());

        dbEnvironment = getDbEnvironment(dbDir);
        openDatabase(dbEnvironment, dbName);
        EntryBinding keyBinding = new SerialBinding(dbEnvironment.classCatalog,
                keyClass);
        EntryBinding valueBinding = new SerialBinding(
                dbEnvironment.classCatalog, valueClass);
        diskMap = new StoredMap(this.db, keyBinding, valueBinding, true);
    }

    public CachedBdbMap(File dbDir, String dbName, Map map, Class keyClass,
            Class valueClass) throws DatabaseException {
        this(dbDir, dbName, keyClass, valueClass);
        putAll(map);
    }

    /**
     * Get the database environment for a physical directory where data will be
     * stored.
     * <p>
     * If the environment already exist it will be reused, else a new one will
     * be created.
     * 
     * @param dbDir The directory where BDB JE data will be stored.
     * @return a datastructure containing the environment and a default database
     *         for storing class definitions.
     */
    private synchronized DbEnvironmentEntry getDbEnvironment(File dbDir) {
        if (dbEnvironmentMap.containsKey(dbDir.getAbsolutePath())) {
            return (DbEnvironmentEntry) dbEnvironmentMap.get(dbDir
                    .getAbsolutePath());
        } else {
            EnvironmentConfig envConfig = new EnvironmentConfig();
            envConfig.setAllowCreate(true);
            envConfig.setTransactional(false);

            //We're doing the caching ourselves so setting these at the lowest
            //possible level.
            envConfig.setCacheSize(1024);
            envConfig.setCachePercent(1);

            DbEnvironmentEntry env = new DbEnvironmentEntry();
            try {
                env.dbEnvironment = new Environment(dbDir, envConfig);
                env.dbDir = dbDir;
                dbEnvironmentMap.put(dbDir.getAbsolutePath(), env);

                DatabaseConfig dbConfig = new DatabaseConfig();
                dbConfig.setTransactional(false);
                dbConfig.setAllowCreate(true);

                Database catalogDb = env.dbEnvironment.openDatabase(null,
                        CLASS_CATALOG, dbConfig);

                env.classCatalog = new StoredClassCatalog(catalogDb);

                if (logger.isLoggable(Level.INFO)) {
                    // Write out the bdb configuration.
                    envConfig = env.dbEnvironment.getConfig();
                    logger.info("BdbConfiguration: Cache percentage "
                            + envConfig.getCachePercent() + ", cache size "
                            + envConfig.getCacheSize() + ", Map size: "
                            + size());
                }
            } catch (DatabaseException e) {
                e.printStackTrace();
                //throw new FatalConfigurationException(e.getMessage());
            }
            return env;
        }
    }

    private synchronized void openDatabase(DbEnvironmentEntry envEntry,
            String dbName) throws DatabaseException {
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setTransactional(false);
        dbConfig.setAllowCreate(true);
        this.db = envEntry.dbEnvironment.openDatabase(null, dbName, dbConfig);
        envEntry.openDbCount++;
    }

    public synchronized void close() throws DatabaseException {
        db.close();
        dbEnvironment.openDbCount--;
        if (dbEnvironment.openDbCount <= 0) {
            dbEnvironment.classCatalog.close();
            dbEnvironment.dbEnvironment.close();
            dbEnvironmentMap.remove(dbEnvironment.dbDir.getAbsolutePath());
            dbEnvironment = null;
        }
    }

    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Map#entrySet()
     */
    public Set entrySet() {
        expungeStaleEntries();
        return entrySet;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Map#get(java.lang.Object)
     */
    public Object get(Object key) {
        getCount++;
        expungeStaleEntries();

        if (logger.isLoggable(Level.FINE) && getCount % 1000 == 0) {
            try {
                long notInMapCount = (getCount - (cacheHit + diskHit));
                long cacheHitPercent = (cacheHit * 100) / (cacheHit + diskHit);
                logger.fine("DB name: " + db.getDatabaseName()
                                + ", Cache Hit: " + cacheHitPercent
                                + "%, Not in map: " + notInMapCount
                                + ", Total number of gets: " + getCount);
            } catch (DatabaseException e) {
                // This is just for logging so ignore DB Exceptions
            }
        }

        SoftEntry tmp = (SoftEntry) cache.get(key);
        if (tmp != null && tmp.get() != null) {
            cacheHit++;
            return tmp.get();
        } else {
            Object o = diskMap.get(key);
            if (o != null) {
                diskHit++;
                tmp = new SoftEntry(key, o, refQueue);
                tmp.phantom.onDisk = true;
                cache.put(key, tmp);
            }
            return o;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */
    public synchronized Object put(Object key, Object value) {
        expungeStaleEntries();
        Object returnValue = null;

        SoftEntry prevValue = (SoftEntry) cache.get(key);

        if (prevValue != null && prevValue.get() != value) {
            SoftEntry newEntry = new SoftEntry(key, value, refQueue);
            newEntry.phantom.onDisk = true;
            cache.put(key, newEntry);
            returnValue = prevValue.get();
        } else if (prevValue == null) {
            cache.put(key, new SoftEntry(key, value, refQueue));
            cacheOnlySize++;
        } else {
            returnValue = prevValue.get();
        }

        return returnValue;
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public Object remove(Object key) {
        expungeStaleEntries();
        Object returnValue;
        SoftEntry entry = (SoftEntry) cache.get(key);
        if (entry != null) {
            if (entry.phantom.onDisk) {
                diskMap.remove(key);
                diskMapSize--;
            } else {
                cacheOnlySize--;
            }
            entry.phantom = null;
            cache.remove(key);
            returnValue = entry.get();
        } else {
            returnValue = diskMap.get(key);
            if (returnValue != null) {
                diskMap.remove(key);
                diskMapSize--;
            }
        }

        return returnValue;
    }

    public boolean containsKey(Object key) {
        if (quickContainsKey(key)) {
            return true;
        } else {
            return diskMap.containsKey(key);
        }
    }

    public boolean quickContainsKey(Object key) {
        expungeStaleEntries();
        return cache.containsKey(key);
    }

    public boolean containsValue(Object value) {
        if (quickContainsValue(value)) {
            return true;
        } else {
            return diskMap.containsValue(value);
        }
    }

    public boolean quickContainsValue(Object value) {
        expungeStaleEntries();
        return cache.containsValue(value);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Map#size()
     */
    public int size() {
        return (int) (diskMapSize + cacheOnlySize);
    }

    private void expungeStaleEntries() {
        int c = 0;
        SoftEntry entry;
        while ((entry = (SoftEntry) refQueue.poll()) != null) {
            cache.remove(entry.phantom.key);
            entry.phantom.clear();
            entry.phantom = null;
            c++;
        }
        if (c > 0 && logger.isLoggable(Level.FINER)) {
            try {
                logger.finer("DB: " + db.getDatabaseName() + ",  Expunged: "
                        + c + ", Diskmap size: " + diskMapSize
                        + ", Cache size: " + cache.size()
                        + ", Objects only in cache: " + cacheOnlySize);
            } catch (DatabaseException e) {
                // Just for logging so ignore Exceptions
            }
        }
    }

    // Internal structures
    private class EntrySet extends AbstractSet implements Set {

        private final CachedBdbMap map;

        public EntrySet(CachedBdbMap map) {
            this.map = map;
        }

        public Iterator iterator() {
            return new EntrySetIterator(map);
        }

        public int size() {
            return map.size();
        }

    }

    private class EntrySetIterator implements Iterator {

        private final CachedBdbMap map;

        public EntrySetIterator(CachedBdbMap map) {
            this.map = map;
        }

        public boolean hasNext() {
            // TODO Auto-generated method stub
            return false;
        }

        public Object next() {
            // TODO Auto-generated method stub
            return null;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

    private class PhantomEntry extends PhantomReference {

        private final Object key;

        private boolean onDisk = false;

        /**
         * @param referent
         * @param q
         */
        public PhantomEntry(Object key, Object referent) {
            super(referent, null);
            this.key = key;
        }

        public void clear() {
            try {
                Object o = referent.get(this);
                diskMap.put(key, o);
                if (!onDisk) {
                    diskMapSize++;
                    cacheOnlySize--;
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            super.clear();
        }
    }

    private class SoftEntry extends SoftReference {

        private PhantomEntry phantom;

        /**
         * @param referent
         * @param q
         */
        public SoftEntry(Object key, Object referent, ReferenceQueue q) {
            super(referent, q);
            this.phantom = new PhantomEntry(key, referent);
        }

    }

}