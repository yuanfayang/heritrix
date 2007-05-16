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
 * BdbEnvironment.java
 *
 * Created on Feb 15, 2007
 *
 * $Id:$
 */
package org.archive.settings.file;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.settings.CheckpointRecovery;
import org.archive.settings.RecoverAction;
import org.archive.state.Immutable;
import org.archive.state.Initializable;
import org.archive.state.Key;
import org.archive.state.KeyManager;
import org.archive.state.Module;
import org.archive.state.StateProvider;
import org.archive.util.CachedBdbMap;
import org.archive.util.FileUtils;

import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.je.CheckpointConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.DatabaseNotFoundException;
import com.sleepycat.je.DbInternal;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.SecondaryConfig;
import com.sleepycat.je.SecondaryDatabase;
import com.sleepycat.je.dbi.EnvironmentImpl;
import com.sleepycat.je.utilint.DbLsn;

public class BdbModule implements Module, Initializable, Checkpointable, 
Serializable, Closeable {

    final private static Logger LOGGER = 
        Logger.getLogger(BdbModule.class.getName()); 

    
    private static class DatabasePlusConfig {
        public Database database;
        public DatabaseConfig config;
    }
    
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Immutable
    final public static Key<String> DIR = Key.make("state");
    
    @Immutable
    final public static Key<Integer> BDB_CACHE_PERCENT = Key.make(60); 
    
    @Immutable
    final public static Key<Boolean> CHECKPOINT_COPY_BDBJE_LOGS = 
        Key.make(true);
    
    static {
        KeyManager.addKeys(BdbModule.class);
    }
    
    private boolean checkpointCopy;
    
    private String path;
    
    private int cachePercent;
    
    private transient Environment bdbEnvironment;
    
    private transient Database classCatalogDB;
    
    private transient StoredClassCatalog classCatalog;
    
    private Map<String,CachedBdbMap> bigMaps = 
        new ConcurrentHashMap<String,CachedBdbMap>();
    
    private Map<String,DatabasePlusConfig> databases =
        new ConcurrentHashMap<String,DatabasePlusConfig>();

    
    public BdbModule() {
    }

    
    public void initialTasks(StateProvider provider) {
        checkpointCopy = provider.get(this, CHECKPOINT_COPY_BDBJE_LOGS);
        cachePercent = provider.get(this, BDB_CACHE_PERCENT);
        path = provider.get(this, DIR);
        try {
            setUp(path, cachePercent, true);
        } catch (DatabaseException e) {
            throw new IllegalStateException(e);
        }
    }
    
    private void setUp(String path, int cachePercent, boolean create) 
    throws DatabaseException {
        EnvironmentConfig config = new EnvironmentConfig();
        config.setAllowCreate(create);
        config.setLockTimeout(5000000);        
        config.setCachePercent(cachePercent);
        
        File f = new File(path);
        f.mkdirs();
        this.bdbEnvironment = new Environment(f, config);
        
        // Open the class catalog database. Create it if it does not
        // already exist. 
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setAllowCreate(create);
        this.classCatalogDB = this.bdbEnvironment.
            openDatabase(null, "classes", dbConfig);
        this.classCatalog = new StoredClassCatalog(classCatalogDB);
    }

    /*
    public Environment getEnvironment() {
        return bdbEnvironment;
    }
    
    
    public Database getClassCatalogDB() {
        return classCatalogDB;
    }
    */

    
    public Database openDatabase(String name, boolean recycle) 
    throws DatabaseException {
        if (databases.containsKey(name)) {
            throw new IllegalStateException("Database already exists: " +name);
        }
        DatabaseConfig config = new DatabaseConfig();
        config.setAllowCreate(!recycle);
        return openDatabase(name, config, recycle);
    }
    
    
    public void closeDatabase(Database db) {
        try {
            closeDatabase(db.getDatabaseName());
        } catch (DatabaseException e) {
            LOGGER.log(Level.SEVERE, "Error getting db name", e);            
        }
    }
    
    public void closeDatabase(String name) {
        DatabasePlusConfig dpc = databases.remove(name);
        if (dpc == null) {
            throw new IllegalStateException("No such database: " + name);
        }
        Database db = dpc.database;
        try {
            if (dpc.config.getDeferredWrite()) {
                db.sync();
            }
            db.close();
        } catch (DatabaseException e) {
            LOGGER.log(Level.SEVERE, "Error closing db " + name, e);
        }
    }
    
    
    public Database openDatabase(String name, DatabaseConfig config, 
            boolean recycle) 
    throws DatabaseException {        
        if (databases.containsKey(name)) {
            throw new IllegalStateException("Database already exists: " +name);
        }
        if (!recycle) {
            try {
                bdbEnvironment.truncateDatabase(null, name, false);
            } catch (DatabaseNotFoundException e) {
                // Ignored
            }
        }
        DatabasePlusConfig dpc = new DatabasePlusConfig();
        dpc.database = bdbEnvironment.openDatabase(null, name, config);
        dpc.config = config;
        databases.put(name, dpc);
        return dpc.database;
    }
    
    
    public SecondaryDatabase openSecondaryDatabase(String name, Database db, 
            SecondaryConfig config) throws DatabaseException {
        if (databases.containsKey(name)) {
            throw new IllegalStateException("Database already exists: " +name);
        }
        SecondaryDatabase result = bdbEnvironment.openSecondaryDatabase(null, 
                name, db, config);
        DatabasePlusConfig dpc = new DatabasePlusConfig();
        dpc.database = result;
        dpc.config = config;
        databases.put(name, dpc);
        return result;
    }

    public StoredClassCatalog getClassCatalog() {
        return classCatalog;
    }


    public <K,V> Map<K,V> getBigMap(String dbName, 
            Class<? super K> key, Class<? super V> value) 
    throws DatabaseException {
        @SuppressWarnings("unchecked")
        CachedBdbMap<K,V> r = bigMaps.get(dbName);
        if (r != null) {
            return r;
        }
        r = new CachedBdbMap<K,V>(dbName);
        
        r.initialize(bdbEnvironment, key, value, classCatalog);
        bigMaps.put(dbName, r);
        return r;
    }
    

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }
    
    
    private void readObject(ObjectInputStream in) 
    throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (in instanceof CheckpointRecovery) {
            CheckpointRecovery cr = (CheckpointRecovery)in;
            path = cr.translatePath(path);
            cr.setState(this, DIR, path);
        }
        try {
            setUp(path, this.cachePercent, false);
            for (CachedBdbMap map: bigMaps.values()) {
                map.initialize(
                        this.bdbEnvironment, 
                        map.getKeyClass(), 
                        map.getValueClass(), 
                        this.classCatalog);
            }
        } catch (DatabaseException e) {
            IOException io = new IOException();
            io.initCause(e);
            throw io;            
        }
    }


    public void checkpoint(File dir, List<RecoverAction> actions) 
    throws IOException {
        if (checkpointCopy) {
            actions.add(new BdbRecover(path));
        }
        // First sync bigMaps
        for (Map.Entry<String,CachedBdbMap> me: bigMaps.entrySet()) {
            me.getValue().sync();
        }

        EnvironmentConfig envConfig;
        try {
            envConfig = bdbEnvironment.getConfig();
        } catch (DatabaseException e) {
            IOException io = new IOException();
            io.initCause(e);
            throw io;
        }
        
        final List bkgrdThreads = Arrays.asList(new String []
            {"je.env.runCheckpointer", "je.env.runCleaner",
                "je.env.runINCompressor"});
        try {
            // Disable background threads
            setBdbjeBkgrdThreads(envConfig, bkgrdThreads, "false");
            // Do a force checkpoint.  Thats what a sync does (i.e. doSync).
            CheckpointConfig chkptConfig = new CheckpointConfig();
            chkptConfig.setForce(true);
            
            // Mark Hayes of sleepycat says:
            // "The default for this property is false, which gives the current
            // behavior (allow deltas).  If this property is true, deltas are
            // prohibited -- full versions of internal nodes are always logged
            // during the checkpoint. When a full version of an internal node
            // is logged during a checkpoint, recovery does not need to process
            // it at all.  It is only fetched if needed by the application,
            // during normal DB operations after recovery. When a delta of an
            // internal node is logged during a checkpoint, recovery must
            // process it by fetching the full version of the node from earlier
            // in the log, and then applying the delta to it.  This can be
            // pretty slow, since it is potentially a large amount of
            // random I/O."
            chkptConfig.setMinimizeRecoveryTime(true);
            bdbEnvironment.checkpoint(chkptConfig);
            LOGGER.fine("Finished bdb checkpoint.");
            
            // From the sleepycat folks: A trick for flipping db logs.
            EnvironmentImpl envImpl = 
                DbInternal.envGetEnvironmentImpl(bdbEnvironment);
            long firstFileInNextSet =
                DbLsn.getFileNumber(envImpl.forceLogFileFlip());
            // So the last file in the checkpoint is firstFileInNextSet - 1.
            // Write manifest of all log files into the bdb directory.
            final String lastBdbCheckpointLog =
                getBdbLogFileName(firstFileInNextSet - 1);
            processBdbLogs(dir, lastBdbCheckpointLog);
            LOGGER.fine("Finished processing bdb log files.");
        } catch (DatabaseException e) {
            IOException io = new IOException();
            io.initCause(e);
            throw io;
        } finally {
            // Restore background threads.
            setBdbjeBkgrdThreads(envConfig, bkgrdThreads, "true");
        }
    }


    private void processBdbLogs(final File checkpointDir,
            final String lastBdbCheckpointLog) throws IOException {
        File bdbDir = getBdbSubDirectory(checkpointDir);
        if (!bdbDir.exists()) {
            bdbDir.mkdir();
        }
        PrintWriter pw = new PrintWriter(new FileOutputStream(new File(
             checkpointDir, "bdbje-logs-manifest.txt")));
        try {
            // Don't copy any beyond the last bdb log file (bdbje can keep
            // writing logs after checkpoint).
            boolean pastLastLogFile = false;
            Set<String> srcFilenames = null;
            do {
                FilenameFilter filter = new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name != null 
                            && name.toLowerCase().endsWith(".jdb");
                    }
                };

                srcFilenames =
                    new HashSet<String>(Arrays.asList(new File(path).list(filter)));
                List tgtFilenames = Arrays.asList(bdbDir.list(filter));
                if (tgtFilenames != null && tgtFilenames.size() > 0) {
                    srcFilenames.removeAll(tgtFilenames);
                }
                if (srcFilenames.size() > 0) {
                    // Sort files.
                    srcFilenames = new TreeSet<String>(srcFilenames);
                    int count = 0;
                    for (final Iterator i = srcFilenames.iterator();
                            i.hasNext() && !pastLastLogFile;) {
                        String name = (String) i.next();
                        if (this.checkpointCopy) {
                            FileUtils.copyFiles(new File(path, name),
                                new File(bdbDir, name));
                        }
                        pw.println(name);
                        if (name.equals(lastBdbCheckpointLog)) {
                            // We're done.
                            pastLastLogFile = true;
                        }
                        count++;
                    }
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine("Copied " + count);
                    }
                }
            } while (!pastLastLogFile && srcFilenames != null &&
                srcFilenames.size() > 0);
        } finally {
            pw.close();
        }
    }


    
    private void setBdbjeBkgrdThreads(final EnvironmentConfig config,
            final List threads, final String setting) {
        for (final Iterator i = threads.iterator(); i.hasNext();) {
            config.setConfigParam((String)i.next(), setting);
        }
    }

    
    private String getBdbLogFileName(final long index) {
        String lastBdbLogFileHex = Long.toHexString(index);
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < (8 - lastBdbLogFileHex.length()); i++) {
            buffer.append('0');
        }
        buffer.append(lastBdbLogFileHex);
        buffer.append(".jdb");
        return buffer.toString();
    }

    
    public void close() {        
        for (Map.Entry<String,CachedBdbMap> me: bigMaps.entrySet()) try {
            me.getValue().sync();
            me.getValue().close();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error closing bigMap " + me.getKey(), e);
        }

        List<String> dbNames = new ArrayList<String>(databases.keySet());
        for (String dbName: dbNames) try {
            closeDatabase(dbName);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error closing db " + dbName, e);
        }

        try {
            this.classCatalog.close();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Exception closing StoredClassCatalog", e);
        }

        try {
            this.bdbEnvironment.sync();
            this.bdbEnvironment.close();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error closing environment.", e);
        }
    }


    private static File getBdbSubDirectory(File checkpointDir) {
        return new File(checkpointDir, "bdbje-logs");
    }


    private static class BdbRecover implements RecoverAction {

        private static final long serialVersionUID = 1L;

        private String path;

        public BdbRecover(String path) {
            this.path = path;
        }
        
        public void recoverFrom(File checkpointDir, 
            CheckpointRecovery recovery) throws Exception {
            File bdbDir = getBdbSubDirectory(checkpointDir);
            path = recovery.translatePath(path);
            FileUtils.copyFiles(bdbDir, new File(path));
        }
        
    }

}
