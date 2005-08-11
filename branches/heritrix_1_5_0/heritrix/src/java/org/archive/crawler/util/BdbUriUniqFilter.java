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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.UriUniqFilter;
import org.archive.util.JeUtils;

import st.ata.util.FPGenerator;

import com.sleepycat.bind.tuple.LongBinding;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.DatabaseNotFoundException;
import com.sleepycat.je.DatabaseStats;
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
 * showed adoption of such a scheme halving lookup times (This implementation
 * actually concatenates scheme + host in first 24 bits and path + query in
 * trailing 40 bits).
 * 
 * @author stack
 * @version $Date$, $Revision$
 */
public class BdbUriUniqFilter implements UriUniqFilter {
    private static Logger logger =
        Logger.getLogger(BdbUriUniqFilter.class.getName());
    // protected transient FPGenerator fpgen64 = FPGenerator.std64;
    protected boolean createdEnvironment = false;
    protected long lastCacheMiss = 0;
    protected long lastCacheMissDiff = 0;
    protected Database alreadySeen = null;
    protected DatabaseEntry value = null;
	private HasUriReceiver receiver = null;
    private static final String DB_NAME = "alreadySeenUrl";
    protected long count = 0;
    private long aggregatedLookupTime = 0;
    
    private static final String COLON_SLASH_SLASH = "://";
    
    /**
     * Shutdown default constructor.
     */
	protected BdbUriUniqFilter() {
		super();
	}
    
    /**
     * Constructor.
     * @param environment A bdb environment ready-configured.
     * @param recycle True if we are to reuse db content if any.
     * @throws IOException
     */
    public BdbUriUniqFilter(Environment environment, boolean recycle)
    throws IOException {
        super();
        try {
            initialize(environment, recycle);
        } catch (DatabaseException e) {
            throw new IOException(e.getMessage());
        }
    }
    
    /**
     * Constructor.
     * @param bdbEnv The directory that holds the bdb environment. Will
     * make a database under here if doesn't already exit.  Otherwise
     * reopens any existing dbs.
     * @param recycle True if we are to reuse db content if any.
     * @throws IOException
     */
    public BdbUriUniqFilter(File bdbEnv, boolean recycle)
    throws IOException {
        this(bdbEnv, -1, recycle);
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
        this(bdbEnv, cacheSizePercentage, false);
    }
    
    /**
     * Constructor.
     * @param bdbEnv The directory that holds the bdb environment. Will
     * make a database under here if doesn't already exit.  Otherwise
     * reopens any existing dbs.
     * @param cacheSizePercentage Percentage of JVM bdb allocates as
     * its cache.  Pass -1 to get default cache size.
     * @param recycle True if we are to reuse db content if any.
     * @throws IOException
     */
    public BdbUriUniqFilter(File bdbEnv, final int cacheSizePercentage,
            final boolean recycle)
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
            initialize(new Environment(bdbEnv, envConfig), recycle);
        } catch (DatabaseException e) {
            throw new IOException(e.getMessage());
        }
    }
    
    /**
     * Method shared by constructors.
     * @param env Environment to use.
     * @param recycle Reuse db content if any.
     * @throws DatabaseException
     */
    protected void initialize(Environment env, boolean recycle)
    throws DatabaseException {
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setAllowCreate(true);
        if (!recycle) {
            try {
                env.truncateDatabase(null, DB_NAME, false);
            } catch (DatabaseNotFoundException e) {
                // Ignored
            } 
        }
        this.alreadySeen = env.openDatabase(null, DB_NAME, dbConfig);
        if (recycle) {
            this.count = JeUtils.getCount(this.alreadySeen);
            if (logger.isLoggable(Level.INFO)) {
                if (logger.isLoggable(Level.INFO)) {
                    logger.info("Count of alreadyseen on open "
                            + Long.toString(count));
                }
            }
        }
        this.value = new DatabaseEntry("".getBytes());
    }
    
    public synchronized void close() {
        Environment env = null;
        if (this.alreadySeen != null) {
        	try {
                env = this.alreadySeen.getEnvironment();
                if (logger.isLoggable(Level.INFO)) {
                    logger.info("Count of alreadyseen on close " +
                        Long.toString(count));
                }
				this.alreadySeen.close();
			} catch (DatabaseException e) {
				logger.severe(e.getMessage());
			}
            this.alreadySeen = null;
        }
        if (env != null && createdEnvironment) {
            try {
				// This sync flushes whats in RAM.  Its expensive operation.
				// Without, data can be lost.  Not for transactional operation.
				env.sync();
				env.close();
			} catch (DatabaseException e) {
				logger.severe(e.getMessage());
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
     * @param url Url to fingerprint.
     * @return Fingerprint of passed <code>url</code>.
     */
    public static long createKey(String url) {
        int index = url.indexOf(COLON_SLASH_SLASH);
        if (index > 0) {
            index = url.indexOf('/', index + COLON_SLASH_SLASH.length());
        }
        CharSequence hostPlusScheme = (index == -1)? url: url.subSequence(0, index);
        long tmp = FPGenerator.std24.fp(hostPlusScheme);
        return tmp | (FPGenerator.std40.fp(url) >>> 24);
    }
    
    public long count() {
        return count;
    }

    public long pending() {
        return 0;
    }

    public void setDestination(HasUriReceiver receiver) {
    	    this.receiver = receiver;
    }

    public void add(String canonical, CandidateURI item) {
    	    add(canonical, item, true, false);
    }

    public void addNow(String canonical, CandidateURI item) {
    	    add(canonical, item);
    }

    public void addForce(String canonical, CandidateURI item) {
    	    add(canonical, item, true, true);
    }

    public void note(String canonical) {
        add(canonical, null, false, false);
    }
    
    /**
     * Add implementation.
     * @param item Item to add to already seen and to pass through to the
     * receiver.
     * @param canonical Canonical representation of <code>item</code>.
     * @param passItOn True if we're to pass on the item IF it has not
     * been seen already.
     * @param force Override of <code>passItOn</code> forcing passing on
     * of <code>item</code> even if already seen.
     */
    protected void add(String canonical, CandidateURI item, boolean passItOn,
            boolean force) {        DatabaseEntry key = new DatabaseEntry();
        LongBinding.longToEntry(createKey(canonical), key);
        long started = 0;
        
        OperationStatus status = null;
        try {
            if (logger.isLoggable(Level.INFO)) {
                started = System.currentTimeMillis();
            }
            status = this.alreadySeen.putNoOverwrite(null, key, this.value);
            if (logger.isLoggable(Level.INFO)) {
                this.aggregatedLookupTime +=
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
                        (this.aggregatedLookupTime / logAt) + "ms.");
                    this.aggregatedLookupTime = 0;
                }
            }
        }
        if ((status != OperationStatus.KEYEXIST && passItOn) || force) {
            this.receiver.receive(item);
        }
    }

    public void forget(String canonical, CandidateURI item) {
        DatabaseEntry key = new DatabaseEntry();
        LongBinding.longToEntry(createKey(canonical), key);
    	    OperationStatus status = null;
        try {
			status = this.alreadySeen.delete(null, key);
		} catch (DatabaseException e) {
			logger.severe(e.getMessage());
		}
        if (status == OperationStatus.SUCCESS) {
            count--;
        }
    }

    public long flush() {
    	    // We always write but this might be place to do the sync
        // when checkpointing?  TODO.
        return 0;
    }
}
