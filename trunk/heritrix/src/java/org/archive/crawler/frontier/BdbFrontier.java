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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;

import org.archive.crawler.checkpoint.CheckpointContext;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.UriUniqFilter;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.FrontierMarker;
import org.archive.crawler.framework.exceptions.FatalConfigurationException;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.Type;
import org.archive.crawler.util.BdbUriUniqFilter;
import org.archive.crawler.util.BloomUriUniqFilter;
import org.archive.util.ArchiveUtils;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.DatabaseNotFoundException;
import com.sleepycat.je.OperationStatus;

/**
 * A Frontier using several BerkeleyDB JE Databases to hold its record of
 * known hosts (queues), and pending URIs. 
 *
 * @author Gordon Mohr
 */
public class BdbFrontier extends WorkQueueFrontier implements Serializable {
    // be robust against trivial implementation changes
    private static final long serialVersionUID = ArchiveUtils
        .classnameBasedUID(BdbFrontier.class, 1);

    private static final Logger logger =
        Logger.getLogger(BdbFrontier.class.getName());

    /** all URIs scheduled to be crawled */
    protected transient BdbMultipleWorkQueues pendingUris;

    /** all URI-already-included options available to be chosen */
    private String[] AVAILABLE_INCLUDED_OPTIONS = new String[] {
            BdbUriUniqFilter.class.getName(),
            BloomUriUniqFilter.class.getName() };
    
    /** URI-already-included to use (by class name) */
    public final static String ATTR_INCLUDED = "uri-included-structure";
    
    private final static String DEFAULT_INCLUDED =
        BdbUriUniqFilter.class.getName();
    
    private static final String READY_QUEUES_DBNAME = "readyQueues";
    private static final String INACTIVE_QUEUES_DBNAME = "inactiveQueues";
    private static final String RETIRED_QUEUES_DBNAME = "retiredQueues";
    private static final String SNOOZED_QUEUES_DBNAME = "snoozedQueues";
    private static final String [] SAVED_QUEUES_DBNAMES = {
        READY_QUEUES_DBNAME, INACTIVE_QUEUES_DBNAME, RETIRED_QUEUES_DBNAME,
        SNOOZED_QUEUES_DBNAME
    };
    
    private static final DatabaseEntry EMPTY_DB_ENTRY =
        new DatabaseEntry(new byte [0]);
    
    /**
     * Constructor.
     * @param name Name for of this Frontier.
     */
    public BdbFrontier(String name) {
        this(name, "BdbFrontier. "
            + "A Frontier using BerkeleyDB Java Edition databases for "
            + "persistence to disk.");
        Type t = addElementToDefinition(new SimpleType(ATTR_INCLUDED,
                "Structure to use for tracking already-seen URIs. Non-default " +
                "options may require additional configuration via system " +
                "properties.", DEFAULT_INCLUDED, AVAILABLE_INCLUDED_OPTIONS));
        t.setExpertSetting(true);
    }

    /**
     * Create the BdbFrontier
     * 
     * @param name
     * @param description
     */
    public BdbFrontier(String name, String description) {
        super(name, description);
    }
    
    /**
     * Create the single object (within which is one BDB database)
     * inside which all the other queues live. 
     * 
     * @return the created BdbMultipleWorkQueues
     * @throws DatabaseException
     */
    private BdbMultipleWorkQueues createMultipleWorkQueues()
    throws DatabaseException {
        return new BdbMultipleWorkQueues(this.controller.getBdbEnvironment(),
            this.controller.getClassCatalog(),
            this.controller.isCheckpointRecover());
    }

    /**
     * Create a UriUniqFilter that will serve as record 
     * of already seen URIs.
     *
     * @return A UURISet that will serve as a record of already seen URIs
     * @throws IOException
     */
    protected UriUniqFilter createAlreadyIncluded() throws IOException {
        UriUniqFilter uuf;
        String c = null;
        try {
            c = (String)getAttribute(null, ATTR_INCLUDED);
        } catch (AttributeNotFoundException e) {
            // Do default action if attribute not in order.
        }
        if (c != null && c.equals(BloomUriUniqFilter.class.getName())) {
            uuf = this.controller.isCheckpointRecover()?
                    deserializeAlreadySeen(BloomUriUniqFilter.class,
                        this.controller.getCheckpointRecover().getDirectory()):
                    new BloomUriUniqFilter();
        } else {
            // Assume its BdbUriUniqFilter.
            uuf = this.controller.isCheckpointRecover()?
                deserializeAlreadySeen(BdbUriUniqFilter.class,
                    this.controller.getCheckpointRecover().getDirectory()):
                new BdbUriUniqFilter(this.controller.getBdbEnvironment());
            if (this.controller.isCheckpointRecover()) {
                // If recover, need to call reopen of the db.
                try {
                    ((BdbUriUniqFilter)uuf).
                        reopen(this.controller.getBdbEnvironment());
                } catch (DatabaseException e) {
                    throw new IOException(e.getMessage());
                }
            }   
        }
        uuf.setDestination(this);
        return uuf;
    }
    
    protected UriUniqFilter deserializeAlreadySeen(final Class cls,
            final File dir)
    throws FileNotFoundException, IOException {
        UriUniqFilter uuf = null;
        try {
            logger.info("Started deserializing " + cls.getName() +
                " of checkpoint recover.");
            uuf = (UriUniqFilter)CheckpointContext.
                readObjectFromFile(cls, dir);
            logger.info("Finished deserializing bdbje as part " +
                "of checkpoint recover.");
        } catch (ClassNotFoundException e) {
            throw new IOException("Failed to deserialize "  +
                cls.getName() + ": " + e.getMessage());
        }
        return uuf;
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
                wq = new BdbWorkQueue(classKey);
                wq.setTotalBudget(((Long)getUncheckedAttribute(
                    curi,ATTR_QUEUE_TOTAL_BUDGET)).longValue());
                allQueues.put(classKey, wq);
            }
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
    public ArrayList getURIsList(FrontierMarker marker, int numberOfMatches,
            final boolean verbose) {
        List curis;
        try {
            curis = pendingUris.getFrom(marker, numberOfMatches);
        } catch (DatabaseException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        ArrayList results = new ArrayList(curis.size());
        Iterator iter = curis.iterator();
        while(iter.hasNext()) {
            CrawlURI curi = (CrawlURI) iter.next();
            results.add("["+curi.getClassKey()+"] "+curi.singleLineReport());
        }
        return results;
    }
    
    /**
     * Private interface used in the below initQueue so I can treat TreeMap,
     * LinkedQueue, and Set all the same when it comes to adding objects.
     * @author stack
     */
    private interface Putter {
        /**
         * @param obj Object to add.
         * @throws InterruptedException LinkedQueue can throw this on put.
         */
        public void put(Object obj) throws InterruptedException;
    }
    
    protected void initQueue() throws IOException {
        try {
            pendingUris = createMultipleWorkQueues();
        } catch(DatabaseException e) {
            throw (IOException)new IOException(e.getMessage()).initCause(e);
        }
        // If checkpoint recover.
        if (this.controller.isCheckpointRecover()) {
            try {
                resurrectQueueState();
            } catch (DatabaseException e) {
                IOException ioe = new IOException("Converted bdb exception: " +
                    e.getMessage());
                ioe.setStackTrace(e.getStackTrace());
            }
        }
    }
    
    protected void resurrectQueueState()
    throws DatabaseException {
        incrementQueuedUriCount(this.pendingUris.getCount());
        
        // TODO: Resurrect allqueues. Is it done above when we reopen bigmap?
        if (logger.isLoggable(Level.FINER)) {
            logger.finer("Pending URIs count: " + queuedUriCount());
            for (Iterator i = this.allQueues.keySet().iterator();
                    i.hasNext();) {
                logger.finer("allqueues " +
                    ((WorkQueue)this.allQueues.get(i.next())).getClassKey());
            }
        }
        
        // Are there other dbs around with which to populate the
        // readyQueues, inactiveQueues and retiredQueues?
        List queueDbBaseNames = Arrays.asList(SAVED_QUEUES_DBNAMES);
        List dbNames = this.controller.getBdbEnvironment().getDatabaseNames();
        if (!dbNames.containsAll(queueDbBaseNames)) {
            logger.severe("All expected dbs not present -- skipping "
                    + "resurrection of " + SAVED_QUEUES_DBNAMES);
        } else {
            try {
                Putter p = new Putter() {
                    public void put(Object obj)
                    throws InterruptedException {
                        readyClassQueues.put(obj);
                    }
                };
                resurrectOneQueueState(READY_QUEUES_DBNAME, p, false);
                
                p = new Putter() {
                    public void put(Object obj) throws InterruptedException {
                        inactiveQueues.put(obj);
                    }
                };
                resurrectOneQueueState(INACTIVE_QUEUES_DBNAME, p, true);
                
                p = new Putter() {
                    public void put(Object obj) throws InterruptedException {
                        retiredQueues.put(obj);
                    }
                };
                resurrectOneQueueState(RETIRED_QUEUES_DBNAME, p, true);
                
                p = new Putter() {
                    public void put(Object obj) {
                        snoozedClassQueues.add(obj);
                    }
                };
                resurrectOneQueueState(SNOOZED_QUEUES_DBNAME, p, true);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    protected void resurrectOneQueueState(final String dbName,
            final Putter putter, final boolean isWq)
    throws DatabaseException, InterruptedException {
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setAllowCreate(false);
        dbConfig.setReadOnly(true);
        Database db = this.controller.getBdbEnvironment().
            openDatabase(null, dbName, dbConfig);
        OperationStatus status;
        Cursor c = null;
        try {
            DatabaseEntry key = new DatabaseEntry();
            DatabaseEntry noValue = new DatabaseEntry();
            c = db.openCursor(null, null);
            status = c.getFirst(key, noValue, null);
            long count = 0;
            if (status == OperationStatus.SUCCESS) {
                count++;
                put(putter, new String(key.getData()), isWq);
            }
            while ((status = c.getNext(key, noValue, null)) ==
                    OperationStatus.SUCCESS) {
                count++;
                put(putter, new String(key.getData()), isWq);
            }
        } finally {
            c.close();
            db.close();
        }
    }
    
    protected void put(final Putter putter, final String key,
            final boolean isWq)
    throws InterruptedException {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Restoring: " + key);
        }
        putter.put((isWq)? this.allQueues.get(key): key);
    }
    
    protected void saveStringKeysToBdb(final String dbName,
        final Iterator iterator) 
    throws DatabaseException {
        saveStringKeysToBdb(dbName, new Iterator [] {iterator});
    }
    
    /**
     * Save to bdb the passed <code>arrayOfIterators</code>.
     * @param dbName Name of db to save to.
     * @param arrayOfIterators Array of iterators over strings or over
     * WorkQueue instances.
     * @throws DatabaseException
     */
    protected void saveStringKeysToBdb(String dbName,
            Iterator [] arrayOfIterators)
    throws DatabaseException {
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setAllowCreate(true);
        // Overwrite any extant db, if there is one.
        try {
            this.controller.getBdbEnvironment().
                truncateDatabase(null, dbName, false);
        } catch (DatabaseNotFoundException e) {
            // Ignore.
        }
        Database db = this.controller.getBdbEnvironment().
            openDatabase(null, dbName, dbConfig);
        OperationStatus status = null;
        try {
            for (int i = 0; i < arrayOfIterators.length; i++) {
                for (Iterator it = arrayOfIterators[i]; it.hasNext();) {
                    Object obj = it.next();
                    String key = (obj instanceof WorkQueue)?
                        ((WorkQueue)obj).getClassKey(): (String)obj;
                    status = db.put(null, new DatabaseEntry(key.getBytes()),
                        EMPTY_DB_ENTRY);
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine(key + " " + status);
                    }
                }
            }
        } finally {
            db.close();
        }
    }

    protected void closeQueue() {
        if (this.pendingUris != null) {
            this.pendingUris.close();
            this.pendingUris = null;
        }
    }
    
    protected void persistQueueState() throws Exception {
        if (logger.isLoggable(Level.FINER)) {
            logger.finer("Pending URIs count: " + this.pendingUris.getCount());
            for (Iterator i = this.allQueues.keySet().iterator();
                    i.hasNext();) {
                logger.finer("allqueues " +
                    ((WorkQueue)this.allQueues.get(i.next())).getClassKey());
            }
        }
        
        // Write out readyQueues. Append inProcessQueues and snoozedQueues.
        // Let restart refigure whats snoozed and in process.
        saveStringKeysToBdb(READY_QUEUES_DBNAME, new Iterator[] {
                this.readyClassQueues.iterator(),
                this.inProcessQueues.iterator() });
        saveStringKeysToBdb(SNOOZED_QUEUES_DBNAME, this.snoozedClassQueues
                .iterator());
        saveStringKeysToBdb(INACTIVE_QUEUES_DBNAME, this.inactiveQueues
                .iterator());
        saveStringKeysToBdb(RETIRED_QUEUES_DBNAME, this.retiredQueues
                .iterator());
    }
        
    protected BdbMultipleWorkQueues getWorkQueues() {
        return pendingUris;
    }

    protected boolean workQueueDataOnDisk() {
        return true;
    }
    
    public void initialize(CrawlController c)
    throws FatalConfigurationException, IOException {
        super.initialize(c);
        if (c.isCheckpointRecover()) {
            // If a checkpoint recover, copy old values from serialized
            // instance into this Frontier instance. Do it this way because 
            // though its possible to serialize BdbFrontier, its difficult
            // plugging the deserialized object back into the settings system.
            // The below copying over is error-prone because its easy
            // to miss a value.  Perhaps there's a better way?  Introspection?
            BdbFrontier f = null;
            try {
                f = (BdbFrontier)CheckpointContext.
                    readObjectFromFile(this.getClass(),
                        this.controller.getCheckpointRecover().getDirectory());
            } catch (FileNotFoundException e) {
                throw new FatalConfigurationException("Failed checkpoint " +
                    "recover: " + e.getMessage());
            } catch (IOException e) {
                throw new FatalConfigurationException("Failed checkpoint " +
                    "recover: " + e.getMessage());
            } catch (ClassNotFoundException e) {
                throw new FatalConfigurationException("Failed checkpoint " +
                    "recover: " + e.getMessage());
            }
            this.nextOrdinal = f.nextOrdinal;
            this.totalProcessedBytes = f.totalProcessedBytes;
            this.disregardedUriCount = f.disregardedUriCount;
            this.failedFetchCount = f.failedFetchCount;
            this.processedBytesAfterLastEmittedURI =
                f.processedBytesAfterLastEmittedURI;
            this.queuedUriCount = f.queuedUriCount;
            this.succeededFetchCount = f.succeededFetchCount;
            this.lastMaxBandwidthKB = f.lastMaxBandwidthKB;
        }
    }

    public void crawlCheckpoint(File checkpointDir) throws Exception {
        super.crawlCheckpoint(checkpointDir);
        persistQueueState();
        logger.info("Started serializing already seen as part "
            + "of checkpoint. Can take some time.");
        CheckpointContext
            .writeObjectToFile(this.alreadyIncluded, checkpointDir);
        logger.info("Finished serializing already seen as part "
            + "of checkpoint.");
        // Serialize ourselves.
        CheckpointContext.writeObjectToFile(this, checkpointDir);
    }
}
