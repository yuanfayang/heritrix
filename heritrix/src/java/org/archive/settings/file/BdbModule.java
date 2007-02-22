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

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;

import org.archive.state.Dependency;
import org.archive.state.Key;
import org.archive.state.KeyManager;
import org.archive.state.Module;
import org.archive.state.StateProvider;
import org.archive.util.CachedBdbMap;

import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;

public class BdbModule implements Module, Serializable {

    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Dependency
    final public static Key<BdbConfig> CONFIG = 
        Key.make(BdbConfig.class, null);
    
    @Dependency
    final public static Key<StateProvider> PROVIDER =
        Key.make(StateProvider.class, null);
    
    static {
        KeyManager.addKeys(BdbModule.class);
    }
    
    final private BdbConfig config;

    private transient Environment bdbEnvironment;
    
    private transient Database classCatalogDB;
    
    private transient StoredClassCatalog classCatalog;
    
    public BdbModule(StateProvider provider, BdbConfig bdbConfig) 
    throws DatabaseException {
        if (bdbConfig == null) {
            throw new IllegalArgumentException("config may not be null");
        }
        this.config = bdbConfig;
        int cachePercent = provider.get(bdbConfig, BdbConfig.BDB_CACHE_PERCENT);
        String path = provider.get(bdbConfig, BdbConfig.DIR);
        setUp(path, cachePercent, true);
    }
    
    
    private void setUp(String path, int cachePercent, boolean create) 
    throws DatabaseException {
        EnvironmentConfig config = new EnvironmentConfig();
        config.setAllowCreate(create);
        config.setLockTimeout(5000000);        
        config.setCachePercent(cachePercent);
        
        new File(path).mkdirs();
        this.bdbEnvironment = new Environment(new File(path), config);
        
        // Open the class catalog database. Create it if it does not
        // already exist. 
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setAllowCreate(create);
        this.classCatalogDB = this.bdbEnvironment.
            openDatabase(null, "classes", dbConfig);
        this.classCatalog = new StoredClassCatalog(classCatalogDB);
    }

    
    public Environment getEnvironment() {
        return bdbEnvironment;
    }
    
    
    public Database getClassCatalogDB() {
        return classCatalogDB;
    }
    
    
    public StoredClassCatalog getClassCatalog() {
        return classCatalog;
    }


    public BdbConfig getBdbConfig() {
        return config;
    }


    public <K,V> Map<K,V> getBigMap(String dbName, Class<K> key, Class<V> value) 
    throws DatabaseException {
        CachedBdbMap<K,V> r = new CachedBdbMap<K,V>(dbName);
        r.initialize(bdbEnvironment, key, value, classCatalog);
        return r;
    }

    
    

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        try {
            out.writeInt(bdbEnvironment.getConfig().getCachePercent());
            out.writeUTF(bdbEnvironment.getHome().getAbsolutePath());
        } catch (DatabaseException e) {
            IOException io = new IOException();
            io.initCause(e);
            throw io;
        }
    }
    
    
    private void readObject(ObjectInputStream in) 
    throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        int cachePercent = in.readInt();
        String path = in.readUTF();
        try {
            setUp(path, cachePercent, false);
        } catch (DatabaseException e) {
            IOException io = new IOException();
            io.initCause(e);
            throw io;            
        }
    }

}
