/* BdbUriUniqFilter
*
* $Id$
*
* Created on September 17, 2004
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
package org.archive.crawler.util;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.settings.file.BdbModule;
import org.archive.state.Immutable;
import org.archive.state.Initializable;
import org.archive.state.Key;
import org.archive.state.KeyManager;
import org.archive.state.StateProvider;

import st.ata.util.FPGenerator;

import com.sleepycat.bind.tuple.LongBinding;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.DatabaseNotFoundException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.OperationStatus;


/**
 * A BDB implementation of an AlreadySeen list.
 * 
 * This implementation performs adequately without blowing out 
 * the heap. See
 * <a href="http://crawler.archive.org/cgi-bin/wiki.pl?AlreadySeen">AlreadySeen</a>.
 * 
 * <p>Makes keys that have URIs from same server close to each other.  Mercator
 * and 2.3.5 'Elminating Already-Visited URLs' in 'Mining the Web' by Soumen
 * Chakrabarti talk of a two-level key with the first 24 bits a hash of the
 * host plus port and with the last 40 as a hash of the path.  Testing
 * showed adoption of such a scheme halving lookup times (Tutilhis implementation
 * actually concatenates scheme + host in first 24 bits and path + query in
 * trailing 40 bits).
 * 
 * @author stack
 * @version $Date$, $Revision$
 */
public class BdbUriUniqFilter extends SetBasedUriUniqFilter 
implements Initializable, Serializable {
    private static final long serialVersionUID = -8099357538178524011L;

    private static Logger logger =
        Logger.getLogger(BdbUriUniqFilter.class.getName());

    protected boolean createdEnvironment = false;
    protected long lastCacheMiss = 0;
    protected long lastCacheMissDiff = 0;
    protected transient Database alreadySeen = null;
    protected transient DatabaseEntry value = null;
    static protected DatabaseEntry ZERO_LENGTH_ENTRY = 
        new DatabaseEntry(new byte[0]);
    private static final String DB_NAME = "alreadySeenUrl";
    protected long count = 0;
    private long aggregatedLookupTime = 0;
    
    private static final String COLON_SLASH_SLASH = "://";
    
    
    @Immutable
    public static final Key<BdbModule> BDB = 
        Key.make(BdbModule.class, null);
    
    static {
        KeyManager.addKeys(BdbUriUniqFilter.class);
    }
    
    private BdbModule bdb;
    
    public BdbUriUniqFilter() {
    }
    
    
    public void initialTasks(StateProvider provider) {
        this.bdb = provider.get(this, BDB);
        try {
            BdbModule.BdbConfig config = getDatabaseConfig();
            config.setAllowCreate(true);
            initialize(bdb.openDatabase(DB_NAME, config, false));
        } catch (DatabaseException e) {
            throw new IllegalStateException(e);
        }
    }
    
    /**
     * Constructor.
     * @param bdbEnv The directory that holds the bdb environment. Will
     * make a database under here if doesn't already exit.  Otherwise
     * reopens any existing dbs.
     * @throws IOException
     */
    public BdbUriUniqFilter(File bdbEnv)
    throws IOException {
        this(bdbEnv, -1);
    }
    
    /**
     * Constructor.
     * @param bdbEnv The directory that holds the bdb environment. Will
     * make a database under here if doesn't already exit.  Otherwise
     * reopens any existing dbs.
     * @param cacheSizePercentage Percentage of JVM bdb allocates as
     * its cache.  Pass -1 to get default cache size.
     * @throws IOException
     */
    public BdbUriUniqFilter(File bdbEnv, final int cacheSizePercentage)
    throws IOException {
        super();
        if (!bdbEnv.exists()) {
            bdbEnv.mkdirs();
        }
        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setAllowCreate(true);
        if (cacheSizePercentage > 0 && cacheSizePercentage < 100) {
            envConfig.setCachePercent(cacheSizePercentage);
        }
        try {
            createdEnvironment = true;
            Environment env = new Environment(bdbEnv, envConfig);
            BdbModule.BdbConfig config = getDatabaseConfig();
            config.setAllowCreate(true);
            try {
                env.truncateDatabase(null, DB_NAME, false);
            } catch (DatabaseNotFoundException e) {
                // ignored
            }
            Database db = env.openDatabase(null, DB_NAME, config.toDatabaseConfig());
            initialize(db);
        } catch (DatabaseException e) {
            IOException io = new IOException();
            io.initCause(e);
            throw io;
        }
    }


    /**
     * Method shared by constructors.
     * @param env Environment to use.
     * @throws DatabaseException
     */
    protected void initialize(Database db) throws DatabaseException {
        open(db);
    }

    /**
     * @return DatabaseConfig to use
     */
    protected BdbModule.BdbConfig getDatabaseConfig() {
        BdbModule.BdbConfig dbConfig = new BdbModule.BdbConfig();
        return dbConfig;
    }
    
    /**
     * Call after deserializing an instance of this class.  Will open the
     * already seen in passed environment.
     * @param env DB Environment to use.
     * @throws DatabaseException
     */
    public void reopen(Database db)
    throws DatabaseException {
        open(db);
    }
    
    protected void open(final Database db)
    throws DatabaseException {
        this.alreadySeen = db;
        this.value = new DatabaseEntry("".getBytes());
    }
    
    public synchronized void close() {
        if (logger.isLoggable(Level.INFO)) {
            logger.info("Count of alreadyseen on close "
                    + Long.toString(count));
        }
        if (createdEnvironment) {
            // Only manually close database if it were created via a
            // constructor, and not via a BdbModule. Databases created by a 
            // BdbModule will be closed by that BdbModule.
            Environment env = null;
            if (this.alreadySeen != null) {
                try {
                    env = this.alreadySeen.getEnvironment();
                    alreadySeen.sync();
                    alreadySeen.close();
                } catch (DatabaseException e) {
                    logger.severe(e.getMessage());
                }
                this.alreadySeen = null;
            }
            if (env != null) {
                try {
                    // This sync flushes whats in RAM. Its expensive operation.
                    // Without, data can be lost. Not for transactional operation.
                    env.sync();
                    env.close();
                } catch (DatabaseException e) {
                    logger.severe(e.getMessage());
                }
            }
        }
    }
    
    public synchronized long getCacheMisses() throws DatabaseException {
        long cacheMiss = this.alreadySeen.getEnvironment().
            getStats(null).getNCacheMiss();
        this.lastCacheMissDiff = cacheMiss - this.lastCacheMiss;
        this.lastCacheMiss = cacheMiss;
        return this.lastCacheMiss;
    }
    
    public long getLastCacheMissDiff() {
        return this.lastCacheMissDiff;
    }
    
    /**
     * Create fingerprint.
     * Pubic access so test code can access createKey.
     * @param uri URI to fingerprint.
     * @return Fingerprint of passed <code>url</code>.
     */
    public static long createKey(CharSequence uri) {
        String url = uri.toString();
        int index = url.indexOf(COLON_SLASH_SLASH);
        if (index > 0) {
            index = url.indexOf('/', index + COLON_SLASH_SLASH.length());
        }
        CharSequence hostPlusScheme = (index == -1)? url: url.subSequence(0, index);
        long tmp = FPGenerator.std24.fp(hostPlusScheme);
        return tmp | (FPGenerator.std40.fp(url) >>> 24);
    }



    protected boolean setAdd(CharSequence uri) {
        DatabaseEntry key = new DatabaseEntry();
        LongBinding.longToEntry(createKey(uri), key);
        long started = 0;
        
        OperationStatus status = null;
        try {
            if (logger.isLoggable(Level.INFO)) {
                started = System.currentTimeMillis();
            }
            status = alreadySeen.putNoOverwrite(null, key, ZERO_LENGTH_ENTRY);
            if (logger.isLoggable(Level.INFO)) {
                aggregatedLookupTime +=
                    (System.currentTimeMillis() - started);
            }
        } catch (DatabaseException e) {
            logger.severe(e.getMessage());
        }
        if (status == OperationStatus.SUCCESS) {
            count++;
            if (logger.isLoggable(Level.INFO)) {
                final int logAt = 10000;
                if (count > 0 && ((count % logAt) == 0)) {
                    logger.info("Average lookup " +
                        (aggregatedLookupTime / logAt) + "ms.");
                    aggregatedLookupTime = 0;
                }
            }
        }
        if(status == OperationStatus.KEYEXIST) {
            return false; // not added
        } else {
            return true;
        }
    }

    protected long setCount() {
        return count;
    }

    protected boolean setRemove(CharSequence uri) {
        DatabaseEntry key = new DatabaseEntry();
        LongBinding.longToEntry(createKey(uri), key);
            OperationStatus status = null;
        try {
            status = alreadySeen.delete(null, key);
        } catch (DatabaseException e) {
            logger.severe(e.getMessage());
        }
        if (status == OperationStatus.SUCCESS) {
            count--;
            return true; // removed
        } else {
            return false; // not present
        }
    }

    public long flush() {
    	    // We always write but this might be place to do the sync
        // when checkpointing?  TODO.
        return 0;
    }

    private void writeObject(ObjectOutputStream output) throws IOException {
        // sync deferred-write database
        try {
            alreadySeen.sync();
        } catch (DatabaseException e) {
            // TODO Auto-generated catch block
            throw new RuntimeException(e);
        }
        output.defaultWriteObject();
    }

    private void readObject(ObjectInputStream input) 
    throws IOException, ClassNotFoundException {
        input.defaultReadObject();        

        try {
            BdbModule.BdbConfig config = getDatabaseConfig();
            config.setAllowCreate(false);
            reopen(bdb.getDatabase(DB_NAME));
        } catch (DatabaseException e) {
            IOException io = new IOException();
            io.initCause(e);
            throw io;
        }
    }

}