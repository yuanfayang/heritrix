/* BdbFrontier
 * 
 * $Header$
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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlServer;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.datamodel.UriUniqFilter;
import org.archive.crawler.datamodel.UriUniqFilter.HasUriReceiver;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.Frontier;
import org.archive.crawler.framework.FrontierMarker;
import org.archive.crawler.framework.exceptions.EndedException;
import org.archive.crawler.framework.exceptions.FatalConfigurationException;
import org.archive.crawler.framework.exceptions.InvalidFrontierMarkerException;
import org.archive.crawler.util.FPUriUniqFilter;
import org.archive.util.ArchiveUtils;
import org.archive.util.MemLongFPSet;

import st.ata.util.FPGenerator;
import EDU.oswego.cs.dl.util.concurrent.ClockDaemon;
import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;

/**
 * A Frontier using several BerkeleyDB JE Databases to hold its record of
 * known hosts (queues), and pending URIs. 
 * 
 * EXPERIMENTAL 
 * CURRENT STATE: uses in-memory map of all known 'queues' inside a 
 * single BDB database. Round-robins between all queues. Encounters
 * BDB lock timeout exceptions if more than a tiny crawl. 
 *
 * @author Gordon Mohr
 */
public class BdbFrontier extends AbstractFrontier implements Frontier,
        FetchStatusCodes, CoreAttributeConstants, HasUriReceiver {

    // be robust against trivial implementation changes
    private static final long serialVersionUID = ArchiveUtils
            .classnameBasedUID(BdbFrontier.class, 1);

    private static final Logger logger = Logger.getLogger(BdbFrontier.class
            .getName());

    /** all URIs scheduled to be crawled */
    protected BdbMultiQueue pendingUris;

    /** those UURIs which are already in-process (or processed), and
     thus should not be rescheduled */
    protected UriUniqFilter alreadyIncluded;

    // TODO: BDBify
    /** all known queues */
    protected HashMap allQueues = new HashMap(); // of classKey -> BDBWorkQueue

    /** all per-class queues whose first item may be handed out */
    protected LinkedQueue readyClassQueues = new LinkedQueue(); // of KeyedQueues

    /** daemon to wake (put in ready queue) WorkQueues at the appropriate time */
    protected ClockDaemon daemon = new ClockDaemon();

    public BdbFrontier(String name) {
        this(name, "BdbFrontier. NOT YET FUNCTIONAL. DO NOT USE.");
    }

    /**
     * Create the BdbFrontier
     * 
     * @param name
     */
    public BdbFrontier(String name, String description) {
        // The 'name' of all frontiers should be the same (URIFrontier.ATTR_NAME)
        // therefore we'll ignore the supplied parameter.
        super(Frontier.ATTR_NAME, description);
    }

    /**
     * Initializes the Frontier, given the supplied CrawlController.
     *
     * @see org.archive.crawler.framework.Frontier#initialize(org.archive.crawler.framework.CrawlController)
     */
    public void initialize(CrawlController c)
            throws FatalConfigurationException, IOException {
        this.controller = c;
        pendingUris = createMultiQueue();
        alreadyIncluded = createAlreadyIncluded();
        loadSeeds();
    }

    /**
     * Create the single object (within which is one BDB database)
     * inside which all the other queues live. 
     * 
     * @return the created BdbMultiQueue
     */
    private BdbMultiQueue createMultiQueue() {
        return new BdbMultiQueue(controller.getStateDisk());
    }

    /**
     * Create a memory-based UriUniqFilter that will serve as record 
     * of already seen URIs.
     *
     * @param dir Directory where the set's files should be written
     * @param filePrefix Prefix to names of the set's files
     * @return A UURISet that will serve as a record of already seen URIs
     * @throws IOException If problems occur creating files on disk
     */
    protected UriUniqFilter createAlreadyIncluded() throws IOException,
            FatalConfigurationException {
        // TODO: adapt to use stack's Bdb already-seen facility
        UriUniqFilter uuf = new FPUriUniqFilter(new MemLongFPSet(23, 0.75f));
        uuf.setDestination(this);
        return uuf;
    }

    /**
     * Arrange for the given CandidateURI to be visited, if it is not
     * already scheduled/completed.
     *
     * @see org.archive.crawler.framework.Frontier#schedule(org.archive.crawler.datamodel.CandidateURI)
     */
    public void schedule(CandidateURI caUri) {
        if (caUri.forceFetch()) {
            alreadyIncluded.addForce(caUri);
        } else {
            alreadyIncluded.add(caUri);
        }
    }

    /**
     * Accept the given CandidateURI for scheduling, as it has
     * passed the alreadyIncluded filter. 
     * 
     * Choose a per-classKey queue and enqueue it. If this
     * item has made an unready queue ready, place that 
     * queue on the readyClassQueues queue. 
     * 
     * @param huri
     */
    public void receive(UriUniqFilter.HasUri huri) {
        CandidateURI caUri = (CandidateURI) huri;
        CrawlURI curi = asCrawlUri(caUri);

        applySpecialHandling(curi);

        incrementQueuedCount();
        BdbWorkQueue wq = getQueueFor(curi.getClassKey());
        synchronized (wq) {
            wq.enqueue(curi);
            if (wq.hasBecomeReady()) {
                try {
                    readyClassQueues.put(wq);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Return the work queue for the given classKey. URIs
     * are ordered and politeness-delayed within their 'class'.
     * 
     * @param classKey
     * @return the found or created BdbWorkQueue
     */
    private BdbWorkQueue getQueueFor(String classKey) {
        BdbWorkQueue wq;
        synchronized (allQueues) {
            wq = (BdbWorkQueue) allQueues.get(classKey);
            if (wq == null) {
                wq = new BdbWorkQueue(classKey, pendingUris);
                allQueues.put(classKey, wq);
            }
        }
        return wq;
    }

    /**
     * Increment the running count of queued URIs. Synchronized
     * because operations on longs are not atomic. 
     */
    private synchronized void incrementQueuedCount() {
        queuedCount++;
    }

    /**
     * Return the next CrawlURI to be processed (and presumably
     * visited/fetched) by a a worker thread.
     *
     * Relies on the readyClassQueues having been loaded with
     * any work queues that are eligible to provide a URI. 
     *
     * @return next CrawlURI to be processed. Or null if none is available.
     *
     * @see org.archive.crawler.framework.Frontier#next(int)
     */
    public CrawlURI next() throws InterruptedException, EndedException {
        while (true) {
            long now = System.currentTimeMillis();

            // do common checks for pause, terminate, bandwidth-hold
            preNext(now);

            BdbWorkQueue readyQ = (BdbWorkQueue) readyClassQueues.poll(5000);
            if (readyQ != null) {
                CrawlURI curi = readyQ.peek();
                if (curi != null) {
                    noteAboutToEmit(curi, readyQ);
                    return curi;
                }
            }

            // nothing was ready; ensure any piled-up scheduled URIs are considered
            alreadyIncluded.flush();
        }
    }

    /**
     * Perform fixups on a CrawlURI about to be returned via next().
     * 
     * @param curi
     */
    protected void noteAboutToEmit(CrawlURI curi, BdbWorkQueue q) {
        curi.setHolder(q);
        CrawlServer cs = this.controller.getServerCache().getServerFor(curi);
        if (cs != null) {
            curi.setServer(cs);
        }
        this.controller.recover.emitted(curi);
    }

    /**
     * Note that the previously emitted CrawlURI has completed
     * its processing (for now).
     *
     * The CrawlURI may be scheduled to retry, if appropriate,
     * and other related URIs may become eligible for release
     * via the next next() call, as a result of finished().
     *
     *  (non-Javadoc)
     * @throws InterruptedException
     * @see org.archive.crawler.framework.Frontier#finished(org.archive.crawler.datamodel.CrawlURI)
     */
    public void finished(CrawlURI curi) {
        long now = System.currentTimeMillis();

        curi.incrementFetchAttempts();
        logLocalizedErrors(curi);
        BdbWorkQueue wq = (BdbWorkQueue) curi.getHolder();
        assert (wq.peek() == curi) : "unexpected peek " + wq;

        if (needsRetrying(curi)) {
            // Consider errors which can be retried, leaving uri atop queue
            long delay_sec = retryDelayFor(curi);
            wq.unpeek();
            if (delay_sec > 0) {
                long delay = delay_sec * 1000;
                wq.snooze();
                daemon.executeAfterDelay(delay, new WakeTask(wq));
            } else {
                try {
                    readyClassQueues.put(wq);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    System.err.println("unable to ready queue "+wq);
                    e.printStackTrace();
                }
            }
            // Let everyone interested know that it will be retried.
            controller.fireCrawledURINeedRetryEvent(curi);
            controller.recover.rescheduled(curi);
            return;
        }

        // curi will definitely be disposed of without retry, so remove from q
        wq.dequeue();
        log(curi);

        if (curi.isSuccess()) {
            totalProcessedBytes += curi.getContentSize();
            successCount++;
            // Let everyone know in case they want to do something before we strip the curi.
            controller.fireCrawledURISuccessfulEvent(curi);
            controller.recover.finishedSuccess(curi);
        } else if (isDisregarded(curi)) {
            // Check for codes that mean that while we the crawler did
            // manage to try it, it must be disregarded for some reason.
            disregardedCount++;
            //Let interested listeners know of disregard disposition.
            controller.fireCrawledURIDisregardEvent(curi);
            // if exception, also send to crawlErrors
            if (curi.getFetchStatus() == S_RUNTIME_EXCEPTION) {
                Object[] array = { curi };
                controller.runtimeErrors.log(Level.WARNING, curi.getUURI()
                        .toString(), array);
            }
            // TODO: consider reinstating forget-uri
        } else {
            // In that case FAILURE, note & log
            //Let interested listeners know of failed disposition.
            this.controller.fireCrawledURIFailureEvent(curi);
            // if exception, also send to crawlErrors
            if (curi.getFetchStatus() == S_RUNTIME_EXCEPTION) {
                Object[] array = { curi };
                this.controller.runtimeErrors.log(Level.WARNING, curi.getUURI()
                        .toString(), array);
            }

            this.failedCount++;
            this.controller.recover.finishedFailure(curi);
        }

        long delay_ms = politenessDelayFor(curi);
        if (delay_ms > 0) {
            wq.snooze();
            daemon.executeAfterDelay(delay_ms, new WakeTask(wq));
        } else {
            try {
                readyClassQueues.put(wq);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                System.err.println("unable to ready queue "+wq);
                e.printStackTrace();
            }
        }

        curi.stripToMinimal();
        curi.processingCleanup();

    }

    /**
     * Forget the given CrawlURI. This allows a new instance
     * to be created in the future, if it is reencountered under
     * different circumstances.
     *
     * @param curi The CrawlURI to forget
     */
    protected void forget(CrawlURI curi) {
        logger.finer("Forgetting " + curi);
        alreadyIncluded.forget(curi.getUURI());
    }

    /**  (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#discoveredUriCount()
     */
    public long discoveredUriCount() {
        return alreadyIncluded.count();
    }

    /** (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#getInitialMarker(java.lang.String, boolean)
     */
    public FrontierMarker getInitialMarker(String regexpr, boolean inCacheOnly) {
        // TODO: implement again based on new backing quqes
        return null;
    }

    /** (non-Javadoc)
     *
     * @param marker
     * @param numberOfMatches
     * @param verbose
     * @return List of URIS.
     * @throws InvalidFrontierMarkerException
     */
    public ArrayList getURIsList(FrontierMarker marker, int numberOfMatches,
            boolean verbose) throws InvalidFrontierMarkerException {
        // TODO: implement again based on new underlying queues
        return null;
    }

    /**
     * @param match String to  match.
     * @return Number of items deleted.
     */
    public long deleteURIs(String match) {
        // TODO: implement again
        return 0;
    }

    /**
     * @return One-line summary report, useful for display when full report
     * may be unwieldy. 
     */
    public String oneLineReport() {
        StringBuffer rep = new StringBuffer();
        rep.append(allQueues.size() + " queues: ");
        // TODO: improve based on new backing queues
        return rep.toString();
    }

    /**
     * This method compiles a human readable report on the status of the frontier
     * at the time of the call.
     *
     * @return A report on the current status of the frontier.
     */
    public synchronized String report() {
        long now = System.currentTimeMillis();
        StringBuffer rep = new StringBuffer();

        rep.append("Frontier report - "
                + ArchiveUtils.TIMESTAMP12.format(new Date()) + "\n");
        rep.append(" Job being crawled: "
                + controller.getOrder().getCrawlOrderName() + "\n");
        rep.append("\n -----===== STATS =====-----\n");
        rep.append(" Discovered:    " + discoveredUriCount() + "\n");
        rep.append(" Queued:        " + queuedCount + "\n");
        rep.append(" Finished:      " + finishedUriCount() + "\n");
        rep.append("  Successfully: " + successCount + "\n");
        rep.append("  Failed:       " + failedCount + "\n");
        rep.append("  Disregarded:  " + disregardedCount + "\n");
        rep.append("\n -----===== QUEUES =====-----\n");
        rep.append(" Already included size:     " + alreadyIncluded.count()
                + "\n");
        rep.append("\n All class queues map size: " + allQueues.size() + "\n");
        // TODO: improve/update for new backing queues
        return rep.toString();
    }

    /**
     * Force logging, etc. of operator- deleted CrawlURIs
     * 
     * @see org.archive.crawler.framework.Frontier#deleted(org.archive.crawler.datamodel.CrawlURI)
     */
    public synchronized void deleted(CrawlURI curi) {
        //treat as disregarded
        controller.fireCrawledURIDisregardEvent(curi);
        log(curi);
        disregardedCount++;
        curi.stripToMinimal();
        curi.processingCleanup();
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.URIFrontier#considerIncluded(org.archive.crawler.datamodel.UURI)
     */
    public void considerIncluded(UURI u) {
        this.alreadyIncluded.note(u);
    }

    /**
     * Perform any special handling of the CrawlURI, such as promoting
     * its URI to seed-status, or preferencing it because it is an 
     * embed. 
     *  
     * @param curi
     */
    protected void applySpecialHandling(CrawlURI curi) {
        if (curi.isSeed() && curi.getVia() != null
                && curi.flattenVia().length() > 0) {
            // The only way a seed can have a non-empty via is if it is the
            // result of a seed redirect.  Add it to the seeds list.
            //
            // This is a feature.  This is handling for case where a seed
            // gets immediately redirected to another page.  What we're doing is
            // treating the immediate redirect target as a seed.
            List seeds = this.controller.getScope().getSeedlist();
            synchronized (seeds) {
                seeds.add(curi.getUURI());
            }
            // And it needs rapid scheduling.
            curi.setSchedulingDirective(CandidateURI.MEDIUM);
        }

        // optionally preferencing embeds up to MEDIUM
        int prefHops = 1; // TODO: restore as configurable setting
        if (prefHops > 0) {
            int embedHops = curi.getTransHops();
            if (embedHops > 0 && embedHops <= prefHops
                    && curi.getSchedulingDirective() == CandidateURI.NORMAL) {
                // number of embed hops falls within the preferenced range, and
                // uri is not already MEDIUM -- so promote it
                curi.setSchedulingDirective(CandidateURI.MEDIUM);
            }
        }
    }

    /**
     * A BerkeleyDB-database-backed structure for holding ordered
     * groupings of CrawlURIs. Reading the groupings from specific
     * per-grouping (per-classKey/per-Host) starting points allows
     * this to act as a collection of independent queues. 
     * 
     * TODO: cleanup/close handles, refactor, improve naming
     * @author gojomo
     */
    public class BdbMultiQueue {
        protected Environment myDbEnvironment = null;

        protected Database pendingUrisDB = null;

        protected Database classCatalogDB = null;

        protected StoredClassCatalog classCatalog = null;

        protected SerialBinding crawlUriBinding;

        ThreadLocal cursor = new ThreadLocal();

        /**
         * @param stateDisk
         */
        public BdbMultiQueue(File stateDisk) {
            // Open the environment. Allow it to be created if it does not already exist. 
            try {
                EnvironmentConfig envConfig = new EnvironmentConfig();
                envConfig.setAllowCreate(true);
                myDbEnvironment = new Environment(stateDisk, envConfig);
                // Open the database. Create it if it does not already exist. 
                DatabaseConfig dbConfig = new DatabaseConfig();
                dbConfig.setAllowCreate(true);
                pendingUrisDB = myDbEnvironment.openDatabase(null, "pending",
                        dbConfig);
                pendingUrisDB.truncate(null, false);
                classCatalogDB = myDbEnvironment.openDatabase(null, "classes",
                        dbConfig);
                classCatalog = new StoredClassCatalog(classCatalogDB);
                crawlUriBinding = new SerialBinding(classCatalog,
                        CrawlURI.class);
                // cursor = pendingUrisDB.openCursor(null,null);
            } catch (DatabaseException dbe) {
                // TODO: handle
                dbe.printStackTrace();
            }
        }

        /**
         * Get the next nearest item after the given key. Relies on 
         * external discipline to avoid asking for something from an
         * origin where there are no associated items -- otherwise
         * could get first item of next 'queue' by mistake. 
         * 
         * TODO: hold within a queue's range
         * 
         * @param headKey
         * @return
         * @throws DatabaseException
         */
        public CrawlURI get(DatabaseEntry headKey) throws DatabaseException {
            DatabaseEntry result = new DatabaseEntry();
            getCursor().getSearchKeyRange(headKey, result, null);
            CrawlURI retVal = (CrawlURI) crawlUriBinding.entryToObject(result);
            retVal.setHolderKey(headKey);
            return retVal;
        }

        /**
         * Get a thread-specific, lazy-initialized Cursor. 
         * 
         * @return
         */
        private Cursor getCursor() {
            Cursor c = (Cursor) cursor.get();
            if (c == null) {
                try {
                    c = pendingUrisDB.openCursor(null, null);
                } catch (DatabaseException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                cursor.set(c);
            }
            return c;
        }

        /**
         * Put the given CrawlURI in at the appropriate place. 
         * 
         * @param curi
         * @throws DatabaseException
         */
        public void put(CrawlURI curi) throws DatabaseException {
            DatabaseEntry insertKey = (DatabaseEntry) curi.getHolderKey();
            if (insertKey == null) {
                insertKey = calculateInsertKey(curi);
                curi.setHolderKey(insertKey);
            }
            DatabaseEntry value = new DatabaseEntry();
            crawlUriBinding.objectToEntry(curi, value);
            pendingUrisDB.put(null, insertKey, value);
        }

        /**
         * Calculate the insertKey that places a CrawlURI in the
         * desired spot. First 60 bits are always host (classKey)
         * based -- ensuring grouping by host. Next 4 bits are
         * priority -- allowing 'immediate' and 'soon' items to 
         * sort above regular. Last 64 bits are ordinal serial number,
         * ensuring earlier-discovered URIs sort before later. 
         * 
         * @param curi
         * @return
         */
        private DatabaseEntry calculateInsertKey(CrawlURI curi) {
            byte[] keyData = new byte[16];
            long fp = FPGenerator.std64.fp(curi.getClassKey()) & 0xFFFFFFFFFFFFFFF0L;
            if (curi.needsImmediateScheduling()) {
                // leave last 4 bits 0
            } else if (curi.needsSoonScheduling()) {
                // prio = 1
                fp = fp | 0x0000000000000001L;
            } else {
                // prio = 2
                fp = fp | 0x0000000000000002L;
            }
            ArchiveUtils.longIntoByteArray(fp, keyData, 0);
            ArchiveUtils.longIntoByteArray(curi.getOrdinal(), keyData, 8);
            return new DatabaseEntry(keyData);
        }

        /**
         * Delete the given CrawlURI from persistent store. Requires
         * the key under which it was stored be available. 
         * 
         * @param peekItem
         * @throws DatabaseException
         */
        public void delete(CrawlURI item) throws DatabaseException {
            pendingUrisDB.delete(null, (DatabaseEntry) item.getHolderKey());
        }

        /**
         * clean up 
         *
         */
        public void close() {
            try {
                //cursor.close();
                pendingUrisDB.close();
                classCatalogDB.close();
                myDbEnvironment.close();
            } catch (DatabaseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

    /**
     * One independent queue of items with the same 'classKey' (eg host). 
     * 
     * @author gojomo
     */
    public class BdbWorkQueue {
        String classKey;
        
        /** where items are really stored, for now */
        BdbMultiQueue masterQueue; 
        
        /** total number of stored items */
        long count = 0;

        /** whether the queue has recently become ready */
        boolean hasBecomeReady = false;

        /** whether the queue is snoozed (held unready even if items
         *  available */
        boolean snoozed = false;

        /** the next item to be returned */ 
        CrawlURI peekItem = null;

        /** key coordinate to begin seeks, to find queue head */
        byte[] origin; 

        /**
         * Create a virtual queue inside the given BdbMultiQueue 
         * 
         * @param classKey
         * @param pendingUris
         */
        public BdbWorkQueue(String classKey, BdbMultiQueue pendingUris) {
            this.classKey = classKey;
            masterQueue = pendingUris;
            origin = new byte[16];
            long fp = FPGenerator.std64.fp(classKey) & 0xFFFFFFFFFFFFFFF0l;
            ArchiveUtils.longIntoByteArray(fp, origin, 0);
        }

        /**
         * Note that the queue is snoozed, so that a zero-item queue
         * that gets an add isn't prematurely or redundantly made ready. 
         * 
         */
        public void snooze() {
            // TODO: revisit sync issues around snooze ops
            snoozed = true;
        }

        /**
         * Remove the peekItem from the queue. 
         * 
         */
        public void dequeue() {
            try {
                masterQueue.delete(peekItem);
            } catch (DatabaseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            unpeek();
            count--;
        }

        /**
         * Forgive the peek, allowing a subsequent peek to 
         * return a different item. 
         * 
         */
        public void unpeek() {
            peekItem = null;
        }

        /**
         * Return the topmost queue item -- and remember it,
         * such that even later higher-priority inserts don't
         * change it. 
         * 
         * TODO: evaluate if this is really necessary
         * 
         * @return topmost queue item
         */
        public CrawlURI peek() {
            if (peekItem == null && count > 0) {
                try {
                    peekItem = masterQueue.get(new DatabaseEntry(origin));
                } catch (DatabaseException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    return null;
                }
            }
            return peekItem;
        }

        /**
         * Add the given CrawlURI
         * 
         * @param curi
         */
        public void enqueue(CrawlURI curi) {
            try {
                masterQueue.put(curi);
            } catch (DatabaseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            count++;
            if (count == 1 && snoozed == false) {
                hasBecomeReady = true;
            }
        }

        /**
         * check-and-clear whether queue has just become ready
         * 
         * @return whether the queue had just become ready
         */
        public boolean hasBecomeReady() {
            boolean retVal = hasBecomeReady;
            hasBecomeReady = false;
            return retVal;
        }

        /**
         * clear the snoozed indicator upon wake
         */
        public void unsnooze() {
            snoozed = false;
        }

    }

    /**
     * Utility task to put a queue whose politeness timeout
     * has expired onto the readyQueues queue. 
     * 
     * TODO: consider reverting to previous style, with 
     * sorted list of wakers checked occasionally, so that
     * there's a clear list with a count of waiting items.
     * 
     * @author gojomo
     */
    protected class WakeTask implements Runnable {
        BdbWorkQueue waker;

        /**
         * Create the WakeTask for the given queue
         */
        public WakeTask(BdbWorkQueue o) {
            super();
            waker = o;
        }

        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        public void run() {
            try {
                waker.unsnooze();
                readyClassQueues.put(waker);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                System.err.println("Queue not woken: " + waker);
            }
        }

    }
}

