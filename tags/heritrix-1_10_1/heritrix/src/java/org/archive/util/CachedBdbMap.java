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
import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.collections.StoredSortedMap;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;

/**
 * A BDB JE backed hashmap. It extends the normal BDB JE map implementation by
 * holding a cache of soft referenced objects. That is objects are not written
 * to disk until they are not referenced by any other object and therefore can be
 * Garbage Collected.
 * 
 * @author John Erik Halse
 * @author stack
 * @author gojomo
 *  
 */
public class CachedBdbMap extends AbstractMap implements Map, Serializable {
    private static final long serialVersionUID = -8655539411367047332L;

    private static final Logger logger =
        Logger.getLogger(CachedBdbMap.class.getName());

    /** The database name of the class definition catalog.*/
    private static final String CLASS_CATALOG = "java_class_catalog";

    /**
     * A map of BDB JE Environments so that we reuse the Environment for
     * databases in the same directory.
     */
    private static final Map dbEnvironmentMap = new HashMap();

    /** The BDB JE environment used for this instance.
     */
    private transient DbEnvironmentEntry dbEnvironment;

    /** The BDB JE database used for this instance. */
    protected transient Database db;

    /** The Collection view of the BDB JE database used for this instance. */
    protected transient StoredSortedMap diskMap;

    /** The softreferenced cache */
    private transient Map memMap;

    protected transient ReferenceQueue refQueue;

    /** The number of objects in the diskMap StoredMap. 
     *  (Package access for unit testing.) */
    protected int diskMapSize = 0;

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
    
    /**
     * Name of bdbje db.
     */
    private String dbName = null;

    /**
     * Reference to the Reference#referent Field.
     */
    protected static Field referentField;
    static {
        // We need access to the referent field in the PhantomReference.
        // For more on this trick, see
        // http://www.javaspecialists.co.za/archive/Issue098.html and for
        // discussion:
        // http://www.theserverside.com/tss?service=direct/0/NewsThread/threadViewer.markNoisy.link&sp=l29865&sp=l146901
        try {
            referentField = Reference.class.getDeclaredField("referent");
            referentField.setAccessible(true);
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
     * Shudown default constructor.
     */
    private CachedBdbMap() {
        super();
    }
    
    /**
     * Constructor.
     * 
     * You must call
     * {@link #initialize(Environment, Class, Class, StoredClassCatalog)}
     * to finish construction. Construction is two-stepped to support
     * reconnecting a deserialized CachedBdbMap with its backing bdbje
     * database.
     * 
     * @param dbName Name of the backing db this instance should use.
     */
    public CachedBdbMap(final String dbName) {
        this();
        this.dbName = dbName;
    }

    /**
     * A constructor for creating a new CachedBdbMap.
     * 
     * Even though the put and get methods conforms to the Collections interface
     * taking any object as key or value, you have to submit the class of the
     * allowed key and value objects here and will get an exception if you try
     * to put anything else in the map.
     * 
     * <p>This constructor internally calls
     * {@link #initialize(Environment, Class, Class, StoredClassCatalog)}.
     * Do not call initialize if you use this constructor.
     * 
     * @param dbDir The directory where the database will be created.
     * @param dbName The name of the database to back this map by.
     * @param keyClass The class of the objects allowed as keys.
     * @param valueClass The class of the objects allowed as values.
     * 
     * @throws DatabaseException is thrown if the underlying BDB JE database
     *             throws an exception.
     */
    public CachedBdbMap(final File dbDir, final String dbName,
            final Class keyClass, final Class valueClass)
    throws DatabaseException {
        this(dbName);
        this.dbEnvironment = getDbEnvironment(dbDir);
        this.dbEnvironment.openDbCount++;
        initialize(dbEnvironment.environment, keyClass, valueClass,
            dbEnvironment.classCatalog);
        if (logger.isLoggable(Level.INFO)) {
            // Write out the bdb configuration.
            EnvironmentConfig cfg = this.dbEnvironment.environment.getConfig();
            logger.info("BdbConfiguration: Cache percentage "  +
                cfg.getCachePercent() + ", cache size " + cfg.getCacheSize() +
                ", Map size: " + size());
        }
    }
    
    /**
     * Call this method when you have an instance when you used the
     * default constructor or when you have a deserialized instance that you
     * want to reconnect with an extant bdbje environment.  Do not
     * call this method if you used the
     * {@link #CachedBdbMap(File, String, Class, Class)} constructor.
     * @param env
     * @param keyClass
     * @param valueClass
     * @param classCatalog
     * @throws DatabaseException
     */
    public synchronized void initialize(final Environment env, final Class keyClass,
            final Class valueClass, final StoredClassCatalog classCatalog)
    throws DatabaseException {
        initializeInstance();
        this.db = openDatabase(env, this.dbName);
        this.diskMap = createDiskMap(this.db, classCatalog, keyClass,
            valueClass);
    }
    
    /**
     * Do any instance setup.
     * This method is used by constructors and when deserializing an instance.
     */
    protected void initializeInstance() {
        this.memMap = new HashMap();
        this.refQueue = new ReferenceQueue();
    }
    
    protected StoredSortedMap createDiskMap(Database database,
            StoredClassCatalog classCatalog, Class keyClass, Class valueClass) {
        EntryBinding keyBinding = TupleBinding.getPrimitiveBinding(keyClass);
        if(keyBinding == null) {
            keyBinding = new SerialBinding(classCatalog, keyClass);
        }
        EntryBinding valueBinding = TupleBinding.getPrimitiveBinding(valueClass);
        if(valueBinding == null) {
            valueBinding = new SerialBinding(classCatalog, valueClass);
        }
        return new StoredSortedMap(database, keyBinding, valueBinding, true);
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
    private DbEnvironmentEntry getDbEnvironment(File dbDir) {
        if (dbEnvironmentMap.containsKey(dbDir.getAbsolutePath())) {
            return (DbEnvironmentEntry) dbEnvironmentMap.get(dbDir
                    .getAbsolutePath());
        }
        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setAllowCreate(true);
        envConfig.setTransactional(false);
        
        // We're doing the caching ourselves so setting these at the lowest
        // possible level.
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
        } catch (DatabaseException e) {
            e.printStackTrace();
            //throw new FatalConfigurationException(e.getMessage());
        }
        return env;
    }

    protected Database openDatabase(final Environment environment,
            final String dbName) throws DatabaseException {
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setTransactional(false);
        dbConfig.setAllowCreate(true);
        return environment.openDatabase(null, dbName, dbConfig);
    }

    public synchronized void close() throws DatabaseException {
        // Close out my bdb db.
        if (this.db != null) {
            try {
                this.db.close();
            } catch (DatabaseException e) {
                e.printStackTrace();
            } finally {
                this.db = null;
            }
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
     * The keySet of the diskMap is all relevant keys. 
     * 
     * @see java.util.Map#keySet()
     */
    public Set keySet() {
        return diskMap.keySet();
    }
    
    public Set entrySet() {
        // Would require complicated implementation to 
        // maintain identity guarantees, so skipping
        throw new UnsupportedOperationException();
    }

    public synchronized Object get(final Object key) {
        countOfGets++;
        expungeStaleEntries();
        if (countOfGets % 10000 == 0) {
            logCacheSummary();
        }
        SoftEntry entry = (SoftEntry)memMap.get(key);
        if (entry != null) {
            Object val = entry.get(); // get & hold, so not cleared pre-return
            if (val != null) {
                cacheHit++;
                return val;
            }
            // Explicitly clear this entry from referencequeue since its
            // value is null.
            expungeStaleEntry(entry);
        }

        // check backing diskMap
        Object o = this.diskMap.get(key);
        if (o != null) {
            diskHit++;
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
            logger.fine("DB name: " + this.db.getDatabaseName()
                + ", Cache Hit: " + cacheHitPercent
                + "%, Not in map: " + (countOfGets - (cacheHit + diskHit))
                + ", Total number of gets: " + countOfGets);
        } catch (DatabaseException e) {
            // This is just for logging so ignore DB Exceptions
        }
    }
    
    public synchronized Object put(Object key, Object value) {
        Object prevVal = get(key);
        memMap.put(key, new SoftEntry(key, value, refQueue));
        diskMap.put(key,value); // dummy
        if(prevVal==null) {
            diskMapSize++;
        }
        return prevVal;
    }

    /**
     * Note that a call to this method CLOSEs the underlying bdbje.
     * This instance is no longer of any use.  It must be re-initialized.
     * We close the db here because if this BigMap is being treated as a plain
     * Map, this is only opportunity for cleanup.
     */
    public synchronized void clear() {
        this.memMap.clear();
        this.diskMap.clear();
        this.diskMapSize = 0;
        try {
            close();
        } catch (DatabaseException e) {
            e.printStackTrace();
        }
    }

    public synchronized Object remove(final Object key) {
        Object prevValue = get(key);
        memMap.remove(key);
        expungeStaleEntries();
        diskMap.remove(key);
        diskMapSize--;
        return prevValue;
    }

    public synchronized boolean containsKey(Object key) {
        if (quickContainsKey(key)) {
            return true;
        }
        return diskMap.containsKey(key);
    }

    public synchronized boolean quickContainsKey(Object key) {
        expungeStaleEntries();
        return memMap.containsKey(key);
    }

    public synchronized boolean containsValue(Object value) {
        if (quickContainsValue(value)) {
            return true;
        }
        return diskMap.containsValue(value);
    }

    public synchronized boolean quickContainsValue(Object value) {
        expungeStaleEntries();
        // FIXME this isn't really right, as memMap is of SoftEntries
        return memMap.containsValue(value);
    }

    public int size() {
        return diskMapSize;
    }
    
    protected String getDatabaseName() {
        String name = "DbName-Lookup-Failed";
        try {
            if (this.db != null) {
                name = this.db.getDatabaseName();
            }
        } catch (DatabaseException e) {
            // Ignore.
        }
        return name;
    }
    
    /**
     * Sync in-memory map entries to backing disk store.
     * When done, the memory map will be cleared and all entries stored
     * on disk.
     */
    public synchronized void sync() {
        String dbName = null;
        // Sync. memory and disk.
        long startTime = 0;
        if (logger.isLoggable(Level.INFO)) {
            dbName = getDatabaseName();
            startTime = System.currentTimeMillis();
            logger.info(dbName + " start sizes: disk " + this.diskMapSize +
                ", mem " + this.memMap.size());
        }
        expungeStaleEntries();
        LinkedList<SoftEntry> stale = new LinkedList<SoftEntry>(); 
        for (Iterator i = this.memMap.keySet().iterator(); i.hasNext();) {
            Object key = i.next();
            SoftEntry entry = (SoftEntry) memMap.get(key);
            if (entry != null) {
                // Get & hold so not cleared pre-return.
                Object value = entry.get();
                if (value != null) {
                    this.diskMap.put(key, value);
                } else {
                    stale.add(entry);
                }
            }
        }
        // for any entries above that had been cleared, ensure expunged
        for (SoftEntry entry : stale) {
            expungeStaleEntry(entry);
        }   
        
        if (logger.isLoggable(Level.INFO)) {
            logger.info(dbName + " sync took " +
                (System.currentTimeMillis() - startTime) + "ms. " +
                "Finish sizes: disk " +
                this.diskMapSize + ", mem " + this.memMap.size());
        }
    }

    private void expungeStaleEntries() {
        int c = 0;
        for(SoftEntry entry; (entry = (SoftEntry)refQueue.poll()) != null;) {
            expungeStaleEntry(entry);
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
    
    private void expungeStaleEntry(SoftEntry entry) {
        // If phantom already null, its already expunged -- probably
        // because it was purged directly first from inside in
        // {@link #get(String)} and then it went on the poll queue and
        // when it came off inside in expungeStaleEntries, this method
        // was called again.
        if (entry.getPhantom() == null) {
            return;
        }
        // If the object that is in memMap is not the one passed here, then
        // memMap has been changed -- probably by a put on top of this entry.
        if (memMap.get(entry.getPhantom().getKey()) == entry) {
            memMap.remove(entry.getPhantom().getKey());
            diskMap.put(entry.getPhantom().getKey(),
                entry.getPhantom().doctoredGet());
        }
        entry.clearPhantom();
    }
    
    private class PhantomEntry extends PhantomReference {
        private final Object key;

        public PhantomEntry(Object key, Object referent) {
            super(referent, null);
            this.key = key;
        }

        /**
         * @return Return the referent. The contract for {@link #get()}
         * always returns a null referent.  We've cheated and doctored
         * PhantomReference to return the actual referent value.  See notes
         * at {@link #referentField};
         */
        public Object doctoredGet() {
            try {
                // Here we use the referentField saved off on static
                // initialization of this class to get at this References'
                // private referent field.
                return referentField.get(this);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * @return Returns the key.
         */
        public Object getKey() {
            return this.key;
        }
    }

    private class SoftEntry extends SoftReference {
        private PhantomEntry phantom;

        public SoftEntry(Object key, Object referent, ReferenceQueue q) {
            super(referent, q);
            this.phantom = new PhantomEntry(key, referent);
        }

        /**
         * @return Returns the phantom reference.
         */
        public PhantomEntry getPhantom() {
            return this.phantom;
        }
        
        public void clearPhantom() {
            this.phantom.clear();
            this.phantom = null;
            super.clear();
        }
    }
    
    private void readObject(java.io.ObjectInputStream stream)
    throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        initializeInstance();
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(getDatabaseName() + " diskMapSize: " + diskMapSize);
        }
    }
}
