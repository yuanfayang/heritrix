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
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.management.openmbean.CompositeData;

import org.apache.commons.collections.Closure;
import org.archive.crawler.datamodel.CrawlURI;
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

    /** 
     * All 'inactive' queues, not yet in active rotation.
     * Linked-list of keys for the queues.
     */
    protected SortedMap<Integer,Queue<String>> inactiveQueuesByPrecedence;

    /**
     * 'retired' queues, no longer considered for activation.
     * Linked-list of keys for queues.
     */
    protected StoredQueue<String> retiredQueues;
    
    /** all URIs scheduled to be crawled */
    protected transient BdbMultipleWorkQueues pendingUris;

    @Immutable
    final public static Key<BdbModule> BDB = Key.makeAuto(BdbModule.class);

    @Immutable
    final public static Key<Boolean> DUMP_PENDING_AT_CLOSE = Key.make(false);

    private BdbModule bdb;
    
    /* (non-Javadoc)
     * @see org.archive.crawler.frontier.WorkQueueFrontier#getInactiveQueuesByPrecedence()
     */
    @Override
    SortedMap<Integer, Queue<String>> getInactiveQueuesByPrecedence() {
        return inactiveQueuesByPrecedence;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.frontier.WorkQueueFrontier#getRetiredQueues()
     */
    @Override
    Queue<String> getRetiredQueues() {
        return retiredQueues;
    }
    
    /**
     * Create the single object (within which is one BDB database)
     * inside which all the other queues live. 
     * 
     * @return the created BdbMultipleWorkQueues
     * @throws DatabaseException
     */
    private BdbMultipleWorkQueues createMultipleWorkQueues(boolean recycle)
    throws DatabaseException {
        Database db;
        if (recycle) {
            db = bdb.getDatabase("pending");
        } else {
            BdbModule.BdbConfig dbConfig = new BdbModule.BdbConfig();
            dbConfig.setAllowCreate(!recycle);
            // Make database deferred write: URLs that are added then removed 
            // before a page-out is required need never cause disk IO.
            db = bdb.openDatabase("pending", dbConfig, recycle);
        }
        
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
                wq.setStateProvider(manager);
                wq.get(this, QUEUE_PRECEDENCE_POLICY).queueCreated(wq);
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
        assert Thread.currentThread() == managerThread; 
        
        WorkQueue wq = (WorkQueue)allQueues.get(classKey);
        if(wq!=null) {
            wq.setStateProvider(manager);
        }
        return wq;
    }


    /**
     * Return list of urls.
     * @param marker
     * @param numberOfMatches
     * @param verbose 
     * @return List of URIs (strings).
     */
    public CompositeData getURIsList(String marker, 
            int numberOfMatches, String pattern, final boolean verbose) {
        try {
            Pattern p = Pattern.compile(pattern);
            return pendingUris.getFrom(marker, numberOfMatches, p, verbose);
        } catch (DatabaseException e) {
            throw new IllegalStateException(e);
        }
    }
    
    protected void closeQueue() {
        if (manager.get(this, DUMP_PENDING_AT_CLOSE)) {
            try {
                dumpAllPendingToLog();
            } catch (DatabaseException e) {
                logger.log(Level.WARNING, "dump pending problem", e);
            }
        }
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
        this.allQueues = bdb.getBigMap("allqueues", false,
                String.class, WorkQueue.class);
        if (logger.isLoggable(Level.FINE)) {
            Iterator<String> i = this.allQueues.keySet().iterator();
            try {
                for (; i.hasNext();) {
                    logger.fine((String) i.next());
                }
            } finally {
                StoredIterator.close(i);
            }
        }
    }
    
    @Override
    protected void initOtherQueues(boolean recycle) throws DatabaseException {
        // small risk of OutOfMemoryError: if 'hold-queues' is false,
        // readyClassQueues may grow in size without bound
        readyClassQueues = new LinkedBlockingQueue<String>();

        inactiveQueuesByPrecedence = new TreeMap<Integer,Queue<String>>();
        
        Database retiredQueuesDb;
        retiredQueuesDb = bdb.openDatabase("retiredQueues", 
                StoredQueue.databaseConfig(), false);
        retiredQueues = new StoredQueue<String>(retiredQueuesDb,
                String.class, null);

        // small risk of OutOfMemoryError: in large crawls with many 
        // unresponsive queues, an unbounded number of snoozed queues 
        // may exist
        snoozedClassQueues = new DelayQueue<DelayedWorkQueue>();
        
        // initialize master map in which other queues live
        this.pendingUris = createMultipleWorkQueues(recycle);
    }


    /* (non-Javadoc)
     * @see org.archive.crawler.frontier.WorkQueueFrontier#createInactiveQueueForPrecedence(int)
     */
    @Override
    Queue<String> createInactiveQueueForPrecedence(int precedence) {
        Database inactiveQueuesDb;
        try {
            inactiveQueuesDb = bdb.openDatabase("inactiveQueues-"+precedence,
                    StoredQueue.databaseConfig(), false);
        } catch (DatabaseException e) {
            throw new RuntimeException(e);
        }
        return new StoredQueue<String>(inactiveQueuesDb,
                String.class, null);
    }
    
    private void readObject(ObjectInputStream in) 
    throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        // rehook StoredQueues to their databases
        for(int precedenceKey : inactiveQueuesByPrecedence.keySet()) {
            Database inactiveQueuesDb = 
                bdb.getDatabase("inactiveQueues-"+precedenceKey);
            ((StoredQueue)inactiveQueuesByPrecedence.get(precedenceKey))
                .hookupDatabase(inactiveQueuesDb, String.class, null);
        }
        
        // rehook retiredQueues to its database
        Database retiredQueuesDb = bdb.getDatabase("retiredQueues");
        retiredQueues.hookupDatabase(retiredQueuesDb, String.class, null);

        try {
            this.pendingUris = new BdbMultipleWorkQueues(bdb.getDatabase("pending"), 
                    bdb.getClassCatalog());
        } catch (DatabaseException e) {
            IOException io = new IOException();
            io.initCause(e);
            throw io;
        }
        startManagerThread();
    }
    
    /**
     * Dump all still-enqueued URIs to the crawl.log -- without actually
     * dequeuing. Useful for understanding what was remaining in a crawl that
     * was ended early, for example at a time limit.
     * 
     * @throws DatabaseException
     */
    public void dumpAllPendingToLog() throws DatabaseException {
        Closure tolog = new Closure() {
            public void execute(Object curi) {
                log((CrawlURI) curi);
            }
        };
        pendingUris.forAllPendingDo(tolog);
    }
    

    // good to keep at end of source: must run after all per-Key
    // initialization values are set.
    static {
        KeyManager.addKeys(BdbFrontier.class);
    }
}
