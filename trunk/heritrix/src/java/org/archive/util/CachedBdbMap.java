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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.collections.StoredIterator;
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
 * <p>
 * Requires external synchronization. 
 * <p>
 * An entry will exist in the memMap, or on disk, but not in both places. So,
 * a resume from disk state (for example for checkpointing/crash-recovery) is
 * not well-supported. This behavior could be revisited to enable such use. 
 * 
 * @author John Erik Halse
 *  
 */
public class CachedBdbMap extends AbstractMap implements Map {

    private static final Logger logger =
        Logger.getLogger(CachedBdbMap.class.getName());

    /** The database name of the class definition catalog.*/
    private static final String CLASS_CATALOG = "java_class_catalog";

    /**
     * A map of BDB JE Environments so that we reuse the Environment for
     * databases in the same directory.
     */
    private static final Map dbEnvironmentMap = new HashMap();

    /** The BDB JE environment used for this instance. */
    private DbEnvironmentEntry dbEnvironment;

    /** The BDB JE database used for this instance. */
    protected Database db;

    /** The Collection view of the BDB JE database used for this instance. */
    protected StoredMap diskMap;

    /** The softreferenced cache */
    private Map memMap;

    protected ReferenceQueue refQueue = new ReferenceQueue();

    /** The number of objects stored in the BDB JE database. 
     *  (Package access for unit testing.) */
    int diskMapSize = 0;

    /**
     * Count of times we got an object from in-memory cache.
     */
    private long cacheHit = 0;

    /**
     * Count of times the {@link CachedBdbMap#get(Object)} method was called.
     */
    private long countOfGets = 0;

    /**
     * Count of every time we went to the disk-based map AND we found an
     * object (Doesn't include accesses that came back null).
     */
    private long diskHit = 0;

    private static Field referent;
    
    static {
        // We need access to the referent field in the PhantomReference.
        // For more on this trick, see
        // http://www.javaspecialists.co.za/archive/Issue098.html and for
        // discussion:
        // http://www.theserverside.com/tss?service=direct/0/NewsThread/threadViewer.markNoisy.link&sp=l29865&sp=l146901
        try {
            referent = Reference.class.getDeclaredField("referent");
            referent.setAccessible(true);
        } catch (SecurityException e) {
            throw new RuntimeException(e);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Simple structure to keep needed information about a DB Environment.
     */
    protected class DbEnvironmentEntry {
        Environment environment;
        StoredClassCatalog classCatalog;
        int openDbCount = 0;
        File dbDir;
    }
    
    /**
     * Constructor for subclasses to use.
     */
    protected CachedBdbMap() {
        super();
        initialize();
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
        initialize();
        dbEnvironment = getDbEnvironment(dbDir);
        this.db = openDatabase(dbEnvironment.environment, dbName);
        dbEnvironment.openDbCount++;
        this.diskMap = createDiskMap(this.db, dbEnvironment.classCatalog,
            keyClass, valueClass);
    }
    
    /**
     * Constructor that takes ready-made environment and class catalog.
     * @param environment
     * @param classCatalog
     * @param dbName The name of the database to back this map by.
     * @param keyClass The class of the objects allowed as keys.
     * @param valueClass The class of the objects allowed as values.
     * @throws DatabaseException is thrown if the underlying BDB JE database
     *             throws an exception.
     */
    public CachedBdbMap(Environment environment, StoredClassCatalog classCatalog,
            String dbName, Class keyClass, Class valueClass)
    throws DatabaseException {
        super();
        initialize();
        this.db = openDatabase(environment, dbName);
        this.diskMap = createDiskMap(this.db, classCatalog, keyClass, valueClass);
    }
    
    private void initialize() {
        // Setup memory cache
        this.memMap = new HashMap();
    }
    
    protected StoredMap createDiskMap(Database database,
            StoredClassCatalog classCatalog, Class keyClass, Class valueClass) {
        return new StoredMap(database, new SerialBinding(classCatalog, keyClass),
                new SerialBinding(classCatalog, valueClass), true);
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
        }
        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setAllowCreate(true);
        envConfig.setTransactional(false);
        
        //We're doing the caching ourselves so setting these at the lowest
        //possible level.
        envConfig.setCacheSize(1024);
        envConfig.setCachePercent(1);
        
        DbEnvironmentEntry env = new DbEnvironmentEntry();
        try {
            env.environment = new Environment(dbDir, envConfig);
            env.dbDir = dbDir;
            dbEnvironmentMap.put(dbDir.getAbsolutePath(), env);
            
            DatabaseConfig dbConfig = new DatabaseConfig();
            dbConfig.setTransactional(false);
            dbConfig.setAllowCreate(true);
            
            Database catalogDb = env.environment.openDatabase(null,
                    CLASS_CATALOG, dbConfig);
            
            env.classCatalog = new StoredClassCatalog(catalogDb);
            
            if (logger.isLoggable(Level.INFO)) {
                // Write out the bdb configuration.
                envConfig = env.environment.getConfig();
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

    protected synchronized Database openDatabase(Environment environment,
            String dbName) throws DatabaseException {
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setTransactional(false);
        dbConfig.setAllowCreate(true);
        return environment.openDatabase(null, dbName, dbConfig);
    }

    public synchronized void close() throws DatabaseException {
        if( db != null) {
            db.close();
        }
        if (dbEnvironment != null) {
            dbEnvironment.openDbCount--;
            if (dbEnvironment.openDbCount <= 0) {
                dbEnvironment.classCatalog.close();
                dbEnvironment.environment.close();
                dbEnvironmentMap.remove(dbEnvironment.dbDir.getAbsolutePath());
                dbEnvironment = null;
            }
        }
    }

    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    
    /**
     * As this keySet is a union of the cache and diskMap KeySets,
     * it does not support the remove operations typical of keySet views
     * on underlying maps.
     * 
     * @see java.util.Map#keySet()
     */
    public Set keySet() {
        // Return unmodifiable union of cache and diskMap keySets
        HashSet allKeys = new HashSet(memMap.keySet());
        // Bdb iterators have to be closed so can't use addAll w/o
        // getting annoying complaints about cursors being left open.
        Iterator i = diskMap.keySet().iterator();
        for (; i.hasNext();) {
            allKeys.add(i.next());
        }
        ((StoredIterator)i).close();
        return Collections.unmodifiableSet(allKeys);
    }
    
    public Set entrySet() {
        // Would require complicated implementation to 
        // maintain identity guarantees, so skipping
        throw new UnsupportedOperationException();
    }

    public Object get(Object key) {
        countOfGets++;
        expungeStaleEntries();
        if (countOfGets % 10000 == 0) {
            logCacheSummary();
        }
        SoftEntry tmp = (SoftEntry) memMap.get(key);
        if (tmp != null) {
            Object val = tmp.get(); // get & hold, so not cleared pre-return
            if (val != null) {
                cacheHit++;
                return val;
            }
        }
        
        // In case ref was cleared after last expunge
        // we remove so that any key is either in memory, or on disk, but 
        // not both
        expungeStaleEntries();
        Object o = diskMap.remove(key);
        if (o != null) {
            diskHit++;
            diskMapSize--;
            memMap.put(key, new SoftEntry(key, o, refQueue));
        }
        return o;
    }

    /**
     * Info to log, if at FINE level, on every get()
     */
    private void logCacheSummary() {
        if (!logger.isLoggable((Level.FINE))) {
            return;
        }
        try {
            long cacheHitPercent = (cacheHit * 100) / (cacheHit + diskHit);
            logger.fine("DB name: " + db.getDatabaseName()
                + ", Cache Hit: " + cacheHitPercent
                + "%, Not in map: " + (countOfGets - (cacheHit + diskHit))
                + ", Total number of gets: " + countOfGets);
        } catch (DatabaseException e) {
            // This is just for logging so ignore DB Exceptions
        }
    }
    
    public synchronized Object put(Object key, Object value) {
        expungeStaleEntries();
        Object prevVal = null;
        SoftEntry prevEntry = (SoftEntry) memMap.get(key);
        if (prevEntry != null) {
            prevVal = prevEntry.get();
            if (prevVal != null) {
                memMap.put(key, new SoftEntry(key, value, refQueue));
                return prevVal;
            }
        } 
        
        // To maintain contract of Map.put(), get old value if any from disk
        // and as long as we're accessing, remove it (to maintain each-key-only-
        // in-one-place property)
        prevVal = diskMap.remove(key);
        if (prevVal != null) {
            diskMapSize--;
        }
        memMap.put(key, new SoftEntry(key, value, refQueue));
        return value;
    }

    public void clear() {
        memMap.clear();
        diskMap.clear();
        diskMapSize = 0;
    }

    public Object remove(Object key) {
        expungeStaleEntries();
        Object prevValue;
        SoftEntry entry = (SoftEntry) memMap.get(key);
        while (entry != null) {
            prevValue = entry.get();
            if (prevValue != null) {
                // key is in-mem (and nowhere else)
                memMap.remove(key);
                return prevValue;
            }
            // Entry cleared, but not yet expunged
            expungeStaleEntries();
            entry = (SoftEntry) memMap.get(key);
        }
        prevValue = diskMap.remove(key);
        if (prevValue != null) {
            diskMapSize--;
        }
        return prevValue;
    }

    public boolean containsKey(Object key) {
        if (quickContainsKey(key)) {
            return true;
        }
        return diskMap.containsKey(key);
    }

    public boolean quickContainsKey(Object key) {
        expungeStaleEntries();
        return memMap.containsKey(key);
    }

    public boolean containsValue(Object value) {
        if (quickContainsValue(value)) {
            return true;
        }
        return diskMap.containsValue(value);
    }

    public boolean quickContainsValue(Object value) {
        expungeStaleEntries();
        return memMap.containsValue(value);
    }

    public int size() {
        return diskMapSize + memMap.size();
    }

    private void expungeStaleEntries() {
        int c = 0;
        SoftEntry entry;
        while ((entry = (SoftEntry) refQueue.poll()) != null) {
            memMap.remove(entry.phantom.key);
            entry.phantom.clear();
            entry.phantom = null;
            c++;
        }
        if (c > 0 && logger.isLoggable(Level.FINER)) {
            try {
                logger.finer("DB: " + db.getDatabaseName() + ",  Expunged: "
                        + c + ", Diskmap size: " + diskMapSize
                        + ", Cache size: " + memMap.size());
            } catch (DatabaseException e) {
                // Just for logging so ignore Exceptions
            }
        }
    }
    
    private class PhantomEntry extends PhantomReference {

        private final Object key;

        public PhantomEntry(Object key, Object referent) {
            super(referent, null);
            this.key = key;
        }

        public void clear() {
            try {
                Object o = referent.get(this);
                diskMap.put(key, o);
                diskMapSize++;
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            super.clear();
        }
    }

    private class SoftEntry extends SoftReference {
        private PhantomEntry phantom;

        public SoftEntry(Object key, Object referent, ReferenceQueue q) {
            super(referent, q);
            this.phantom = new PhantomEntry(key, referent);
        }
    }
    
    /**
     * Used by unit test.
     * @return This classes logger.
     */
    Logger getLogger() {
        return logger;
    }
}
