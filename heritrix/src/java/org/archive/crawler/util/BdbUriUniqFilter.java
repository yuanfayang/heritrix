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
import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.UriUniqFilter;

import st.ata.util.FPGenerator;

import com.sleepycat.bind.tuple.LongBinding;
import com.sleepycat.je.BtreeStats;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.OperationStatus;


/**
 * A BDB implementation of an AlreadySeen list.
 * 
 * This implementation performs adequately without blowing out 
 * the heap. See
 * <a href="http://crawler.archive.org/cgi-bin/wiki.pl?AlreadySeen">AlreadySeen</a>.
 * Its hardwired to grow to use 25% of JVM heap as cache (This can be changed).
 * @author stack
 * @version $Date$, $Revision$
 */
public class BdbUriUniqFilter implements UriUniqFilter {
    private static Logger logger =
        Logger.getLogger(BdbUriUniqFilter.class.getName());
    protected transient FPGenerator fpgen = FPGenerator.std64;
    protected Environment environment = null;
    protected long lastCacheMiss = 0;
    protected long lastCacheMissDiff = 0;
    protected Database alreadySeen = null;
    protected DatabaseEntry value = null;
	private HasUriReceiver receiver = null;
    private static final String DB_NAME = "alreadySeenUrl";
    
    /**
     * Shutdown default constructor.
     */
	protected BdbUriUniqFilter() {
		super();
	}
    
    /**
     * Constructor.
     * @param environment A bdb environment ready-configured.
     * @throws DatabaseException
     * @throws UnsupportedEncodingException
     * @throws DatabaseException
     * @throws UnsupportedEncodingException
     */
    protected BdbUriUniqFilter(Environment environment)
    throws UnsupportedEncodingException, DatabaseException {
        super();
        initialize(environment);
    }
    
    /**
     * Constructor.
     * @param bdbEnv The directory that holds the bdb environment. Will
     * make a database under here if doesn't already exit.  Otherwise
     * reopens any existing dbs.
     * @param cacheSizePercentage Percentage of JVM bdb allocates as
     * its cache.
     * @throws DatabaseException
     * @throws UnsupportedEncodingException
     */
    protected BdbUriUniqFilter(File bdbEnv,  int cacheSizePercentage)
    throws DatabaseException, UnsupportedEncodingException {
        super();
        if (!bdbEnv.exists()) {
            bdbEnv.mkdirs();
        }
        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setAllowCreate(true);
        envConfig.setCachePercent(cacheSizePercentage);
        initialize(new Environment(bdbEnv, envConfig));
    }
    
    /**
     * Method shared by constructors.
     * @throws DatabaseException
     * @throws UnsupportedEncodingException
     */
    protected void initialize(Environment environment)
    throws DatabaseException, UnsupportedEncodingException {
        this.environment = environment;
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setAllowCreate(true);
        this.alreadySeen = this.environment.openDatabase(null, 
            DB_NAME, dbConfig);
        this.value = new DatabaseEntry("".getBytes("UTF-8"));
    }
    
    public synchronized void close() {
        if (this.alreadySeen != null) {
        	try {
				this.alreadySeen.close();
			} catch (DatabaseException e) {
				logger.severe(e.getMessage());
			}
            this.alreadySeen = null;
        }
    	if (this.environment != null) {
            try {
				// This sync flushes whats in RAM.  Its expensive operation.
				// Without, data can be lost.  Not for transactional operation.
				// TODO: This close needs to be inside a finally so that if 
				// we pop out, already seen gets flushed.
				this.environment.sync();
                this.environment.close();
			} catch (DatabaseException e) {
				logger.severe(e.getMessage());
			}
            this.environment = null;
        }
    }
    
    public synchronized long getCacheMisses() throws DatabaseException {
        long cacheMiss = this.environment.getStats(null).getNCacheMiss();
        this.lastCacheMissDiff = cacheMiss - this.lastCacheMiss;
        this.lastCacheMiss = cacheMiss;
        return this.lastCacheMiss;
    }
    
    public long getLastCacheMissDiff() throws DatabaseException {
        return this.lastCacheMissDiff;
    }
    
    protected long createKey(String url) {
    	return this.fpgen.fp(url);
    }
    
    public long count() {
        long count = -1;
        try {
			count = ((BtreeStats)this.alreadySeen.getStats(null)).
                getLeafNodeCount();
		} catch (DatabaseException e) {
			logger.severe(e.getMessage());
		}
        return count;
    }

    public long pending() {
        return 0;
    }

    public void setDestination(HasUriReceiver receiver) {
    	this.receiver = receiver;
    }

    public void add(HasUri item) {
    	add(item, true, false);
    }

    public void addNow(HasUri item) {
    	add(item);
    }

    public void addForce(HasUri item) {
    	add(item, true, true);
    }

    public void note(HasUri item) {
        add(item, false, false);
    }
    
    /**
     * Add implementation.
     * @param item Item to add to already seen and to pass through to the
     * receiver.
     * @param passItOn True if we're to pass on the item IF it has not
     * been seen already.
     * @param force Override of <code>passItOn</code> forcing passing on
     * of <code>item</code> even if already seen.
     */
    protected void add(HasUri item, boolean passItOn, boolean force) {
        DatabaseEntry key = new DatabaseEntry();
        LongBinding.longToEntry(createKey(item.getUri()), key);
        
        OperationStatus status = null;
        try {
            status = this.alreadySeen.putNoOverwrite(null, key, this.value);
        } catch (DatabaseException e) {
            logger.severe(e.getMessage());
        }

        if ((status != OperationStatus.KEYEXIST && passItOn) || force) {
        	this.receiver.receive(item);
        }
    }

    public void forget(HasUri item) {
        DatabaseEntry key = new DatabaseEntry();
        LongBinding.longToEntry(createKey(item.getUri()), key);
    	try {
			this.alreadySeen.delete(null, key);
		} catch (DatabaseException e) {
			logger.severe(e.getMessage());
		}
    }

    public long flush() {
    	// We always write but this might be place to do the sync
        // when checkpointing?  TODO.
        return 0;
    }
}
