/* BdbFrontier
 * 
 * $Id$
* 
 * Created on Sep 24, 2004
 *
 *  Copyright (C) 2004 Internet Archive.
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
  */
package org.archive.crawler.frontier;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.FrontierMarker;
import org.archive.modules.util.RobotsHonoringPolicy;
import org.archive.queue.StoredQueue;
import org.archive.settings.RecoverAction;
import org.archive.settings.file.BdbModule;
import org.archive.settings.file.Checkpointable;
import org.archive.state.Immutable;
import org.archive.state.Key;
import org.archive.state.KeyManager;
import org.archive.state.StateProvider;
import org.archive.util.ArchiveUtils;

import com.sleepycat.collections.StoredIterator;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseException;

/**
 * A Frontier using several BerkeleyDB JE Databases to hold its record of
 * known hosts (queues), and pending URIs. 
 *
 * @author Gordon Mohr
 */
public class BdbFrontier extends WorkQueueFrontier 
implements Serializable, Checkpointable {
    // be robust against trivial implementation changes
    private static final long serialVersionUID = ArchiveUtils
        .classnameBasedUID(BdbFrontier.class, 1);

    private static final Logger logger =
        Logger.getLogger(BdbFrontier.class.getName());

    /** all URIs scheduled to be crawled */
    protected transient BdbMultipleWorkQueues pendingUris;

    @Immutable
    final public static Key<BdbModule> BDB = Key.make(BdbModule.class, null);

    static {
        KeyManager.addKeys(BdbFrontier.class);
    }

    
    private BdbModule bdb;
    
    
    /**
     * Create the single object (within which is one BDB database)
     * inside which all the other queues live. 
     * 
     * @return the created BdbMultipleWorkQueues
     * @throws DatabaseException
     */
    private BdbMultipleWorkQueues createMultipleWorkQueues(boolean recycle)
    throws DatabaseException {
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setAllowCreate(!recycle);
        // Make database deferred write: URLs that are added then removed 
        // before a page-out is required need never cause disk IO.
        dbConfig.setDeferredWrite(true);
        Database db = bdb.openDatabase("pending", dbConfig, recycle);
        
        return new BdbMultipleWorkQueues(db, bdb.getClassCatalog());
    }


    /**
     * Return the work queue for the given CrawlURI's classKey. URIs
     * are ordered and politeness-delayed within their 'class'.
     * 
     * @param curi CrawlURI to base queue on
     * @return the found or created BdbWorkQueue
     */
    protected WorkQueue getQueueFor(CrawlURI curi) {
        WorkQueue wq;
        String classKey = curi.getClassKey();
        synchronized (allQueues) {
            wq = (WorkQueue)allQueues.get(classKey);
            if (wq == null) {
                wq = new BdbWorkQueue(classKey, this);
                wq.setTotalBudget(curi.get(this, QUEUE_TOTAL_BUDGET));
                allQueues.put(classKey, wq);
            }
        }
        return wq;
    }
    
    /**
     * Return the work queue for the given classKey, or null
     * if no such queue exists.
     * 
     * @param classKey key to look for
     * @return the found WorkQueue
     */
    protected WorkQueue getQueueFor(String classKey) {
        WorkQueue wq; 
        synchronized (allQueues) {
            wq = (WorkQueue)allQueues.get(classKey);
        }
        return wq;
    }

    public FrontierMarker getInitialMarker(String regexpr,
            boolean inCacheOnly) {
        return pendingUris.getInitialMarker(regexpr);
    }

    /**
     * Return list of urls.
     * @param marker
     * @param numberOfMatches
     * @param verbose 
     * @return List of URIs (strings).
     */
    public ArrayList<String> getURIsList(FrontierMarker marker, 
            int numberOfMatches, final boolean verbose) {
        List curis;
        try {
            curis = pendingUris.getFrom(marker, numberOfMatches);
        } catch (DatabaseException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        ArrayList<String> results = new ArrayList<String>(curis.size());
        Iterator iter = curis.iterator();
        while(iter.hasNext()) {
            CrawlURI curi = (CrawlURI) iter.next();
            results.add("["+curi.getClassKey()+"] "+curi.singleLineReport());
        }
        return results;
    }
    
    @Override
    protected void initQueue(boolean recycle) throws IOException {
        try {
            this.pendingUris = createMultipleWorkQueues(recycle);
        } catch(DatabaseException e) {
            throw (IOException)new IOException(e.getMessage()).initCause(e);
        }
    }
    
    protected void closeQueue() {
        if (this.pendingUris != null) {
            this.pendingUris.close();
            this.pendingUris = null;
        }
    }
        
    protected BdbMultipleWorkQueues getWorkQueues() {
        return pendingUris;
    }

    protected boolean workQueueDataOnDisk() {
        return true;
    }

    
    /**
     * Constructor.
     */
    public BdbFrontier() {
        super();
    }

    
    public void initialTasks(StateProvider p) {
        this.bdb = p.get(this, BDB);
        super.initialTasks(p);
    }
    
    public void checkpoint(File checkpointDir, List<RecoverAction> actions) 
    throws IOException {
        logger.fine("Started syncing already seen as part "
            + "of checkpoint. Can take some time.");
        // An explicit sync on the any deferred write dbs is needed to make the
        // db recoverable. Sync'ing the environment doesn't work.
        if (this.pendingUris != null) {
        	this.pendingUris.sync();
        }
        logger.fine("Finished syncing already seen as part of checkpoint.");
    }

    
    @Override
    protected void initAllQueues() throws DatabaseException {
        this.allQueues = bdb.getBigMap("allqueues",
                String.class, WorkQueue.class);
        if (logger.isLoggable(Level.FINE)) {
            Iterator i = this.allQueues.keySet().iterator();
            try {
                for (; i.hasNext();) {
                    logger.fine((String) i.next());
                }
            } finally {
                StoredIterator.close(i);
            }
        }
        
        // TODO: handle checkpoint-recovery
        
        // small risk of OutOfMemoryError: if 'hold-queues' is false,
        // readyClassQueues may grow in size without bound
        readyClassQueues = new LinkedBlockingQueue<String>();

        Database retiredQueuesDb;
        try {
            Database inactiveQueuesDb = bdb.openDatabase("inactiveQueues",
                    StoredQueue.databaseConfig(), false);
            inactiveQueues = new StoredQueue<String>(inactiveQueuesDb,
                    String.class, null);
            retiredQueuesDb = bdb.openDatabase("retiredQueues", 
                    StoredQueue.databaseConfig(), false);
            retiredQueues = new StoredQueue<String>(retiredQueuesDb,
                    String.class, null);
        } catch (DatabaseException e) {
            throw new RuntimeException(e);
        }

        // small risk of OutOfMemoryError: in large crawls with many 
        // unresponsive queues, an unbounded number of snoozed queues 
        // may exist
        snoozedClassQueues = Collections
                .synchronizedSortedSet(new TreeSet<WorkQueue>());
    }
    
    // good to keep at end of source: must run after all per-Key 
    // initialization values are set.
    static {
        KeyManager.addKeys(BdbFrontier.class);
    }
}
