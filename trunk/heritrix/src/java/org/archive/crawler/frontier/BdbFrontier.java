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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.apache.commons.collections.Bag;
import org.apache.commons.collections.BagUtils;
import org.apache.commons.collections.HashBag;
import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CoreAttributeConstants;
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
import org.archive.crawler.util.BdbUriUniqFilter;
import org.archive.queue.LinkedQueue;
import org.archive.util.ArchiveUtils;

import st.ata.util.FPGenerator;
import EDU.oswego.cs.dl.util.concurrent.ClockDaemon;

import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.OperationStatus;

/**
 * A Frontier using several BerkeleyDB JE Databases to hold its record of
 * known hosts (queues), and pending URIs. 
 * 
 * EXPERIMENTAL 
 * CURRENT STATE: uses in-memory map of all known 'queues' inside a 
 * single BDB database. Round-robins between all queues. Encounters
 * BDB lock timeout exceptions if more than a tiny crawl; these seem
 * to be harmless. 
 *
 * @author Gordon Mohr
 */
public class BdbFrontier extends AbstractFrontier
implements Frontier,
        FetchStatusCodes, CoreAttributeConstants, HasUriReceiver {
    // be robust against trivial implementation changes
    private static final int REPORT_MAX_QUEUES = 100;
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
    protected LinkedQueue readyClassQueues = new LinkedQueue(); // of BDBWorkQueue

    /** all per-class queues from whom a URI is outstanding */
    protected Bag inProcessQueues = BagUtils.synchronizedBag(new HashBag()); // of BDBWorkQueue

    /** all per-class queues held in snoozed state, sorted by wake time */
    protected SortedSet snoozedClassQueues = Collections.synchronizedSortedSet(new TreeSet());

    /** moves waking items from snoozedClassQueues to readyClassQueues */
    protected ClockDaemon wakeDaemon = new ClockDaemon();
    
    /** runnable task to wake snoozed queues */
    protected Runnable waker = new Runnable() {
        public void run() {
            BdbFrontier.this.wakeQueues();
        }
    };
    
    /** shared bdb Environment for Frontier subcomponents */
    // TODO: investigate using multiple environments to split disk accesses
    // across separate physical disks
    protected Environment dbEnvironment;

    /** how long to wait for a ready queue when there's nothing snoozed */
    private static final long DEFAULT_WAIT = 1000; // 1 seconds
    
    /**
     * Create the BdbFrontier
     * 
     * @param name
     */
    public BdbFrontier(String name) {
        this(name, "BdbFrontier. EXPERIMENTAL - MAY NOT BE RELIABLE. " +
                "A Frontier using BerkeleyDB Java Edition Databases for " +
                "persistence to disk. Not yet fully tested or " +
                "feature-equivalent to classic HostQueuesFrontier.");
    }

    /**
     * Create the BdbFrontier
     * 
     * @param name
     * @param description
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
        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setAllowCreate(true);
        try {
            dbEnvironment = new Environment(c.getStateDisk(), envConfig);
            pendingUris = createMultiQueue();
            alreadyIncluded = createAlreadyIncluded();
        } catch (DatabaseException e) {
            e.printStackTrace();
            throw new FatalConfigurationException(e.getMessage());
        }
        loadSeeds();
    }

    /**
     * Create the single object (within which is one BDB database)
     * inside which all the other queues live. 
     * 
     * @return the created BdbMultiQueue
     */
    private BdbMultiQueue createMultiQueue() throws DatabaseException {
        return new BdbMultiQueue(this.dbEnvironment);
    }

    /**
     * Create a memory-based UriUniqFilter that will serve as record 
     * of already seen URIs.
     *
     * @return A UURISet that will serve as a record of already seen URIs
     * @throws DatabaseException
     */
    protected UriUniqFilter createAlreadyIncluded() throws DatabaseException {
        UriUniqFilter uuf = new BdbUriUniqFilter(this.dbEnvironment);
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
            alreadyIncluded.addForce(canonicalize(caUri.getUURI()), caUri);
        } else {
            alreadyIncluded.add(canonicalize(caUri.getUURI()), caUri);
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
    public void receive(CandidateURI caUri) {
        CrawlURI curi = asCrawlUri(caUri);
        applySpecialHandling(curi);
        incrementQueuedUriCount();
        sendToQueue(curi);
        // Update recovery log.
        this.controller.recover.added(curi);
    }

    /**
     * Send a CrawlURI to the appropriate subqueue.
     * 
     * @param curi
     */
    private void sendToQueue(CrawlURI curi) {
        BdbWorkQueue wq = getQueueFor(curi.getClassKey());
        synchronized (wq) {
            wq.enqueue(curi);
            if(!wq.isHeld()) {
                wq.setHeld();
                readyQueue(wq);
            }
        }
    }

    /**
     * Put the given queue on the readyClassQueues queue
     * @param wq
     */
    private void readyQueue(BdbWorkQueue wq) {
        try {
            readyClassQueues.put(wq);
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.err.println("unable to ready queue "+wq);
            // propagate interrupt up 
            throw new RuntimeException(e);
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
     * Return the next CrawlURI to be processed (and presumably
     * visited/fetched) by a a worker thread.
     *
     * Relies on the readyClassQueues having been loaded with
     * any work queues that are eligible to provide a URI. 
     *
     * @return next CrawlURI to be processed. Or null if none is available.
     *
     * @see org.archive.crawler.framework.Frontier#next()
     */
    public CrawlURI next() throws InterruptedException, EndedException {
        while (true) { // loop left only by explicit return or exception
            long now = System.currentTimeMillis();

            // do common checks for pause, terminate, bandwidth-hold
            preNext(now);
            
            // don't wait if there are items buffered in alreadyIncluded
            long wait = alreadyIncluded.pending() > 0 ? 0 : DEFAULT_WAIT;
            
            BdbWorkQueue readyQ = (BdbWorkQueue) readyClassQueues.poll(wait);
            if (readyQ != null) {
                while(true) { // loop left by explicit return or break on empty
                    CrawlURI curi = null;
                    synchronized(readyQ) {
                        curi = readyQ.peek();
                        curi.setServer(getServer(curi));
                        if (curi != null) {
                            // check if curi belongs in different queue
                            String currentQueueKey = getClassKey(curi);
                            if (currentQueueKey.equals(curi.getClassKey())) {
                                noteAboutToEmit(curi, readyQ);
                                inProcessQueues.add(readyQ);
                                return curi;
                            } else {
                                // URI's assigned queue has changed since it
                                // was queued (eg because its IP has become
                                // known). Requeue to new queue.
                                curi.setClassKey(currentQueueKey);
                                readyQ.dequeue();
                                curi.setHolderKey(null);
                            }
                        } else {
                            // readyQ is empty and ready: release held, allowing
                            // subsequent enqueues to ready
                            readyQ.clearHeld();
                            break;
                        }
                    }
                    // if curi non-null, it's a requeue
                    if(curi!=null) {
                        sendToQueue(curi);
                    }
                }
            }

            // nothing was ready; ensure any piled-up scheduled URIs are considered
            alreadyIncluded.flush();
        }
    }


    /**
     * Wake any queues sitting in the snoozed queue whose time has come
     */
    private void wakeQueues() {
        long now = System.currentTimeMillis();
        synchronized (snoozedClassQueues) {
            while (true) {
                if (snoozedClassQueues.isEmpty()) {
                    return;
                }
                BdbWorkQueue peek = (BdbWorkQueue) snoozedClassQueues.first();
                if (peek.getWakeTime() < now) {
                    snoozedClassQueues.remove(peek);
                    try {
                        peek.setWakeTime(0);
                        readyClassQueues.put(peek);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    return;
                }
            }
        }
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
     * @see org.archive.crawler.framework.Frontier#finished(org.archive.crawler.datamodel.CrawlURI)
     */
    public void finished(CrawlURI curi) {
        long now = System.currentTimeMillis();

        curi.incrementFetchAttempts();
        logLocalizedErrors(curi);
        BdbWorkQueue wq = (BdbWorkQueue) curi.getHolder();
        assert (wq.peek() == curi) : "unexpected peek " + wq;
        inProcessQueues.remove(wq,1);

        if (needsRetrying(curi)) {
            // Consider errors which can be retried, leaving uri atop queue
            long delay_sec = retryDelayFor(curi);
            wq.unpeek();
            synchronized(wq) {
                if (delay_sec > 0) {
                    long delay_ms = delay_sec * 1000;
                    snoozeQueue(wq, now, delay_ms);
                } else {
                    readyQueue(wq);
                }
            }
            // Let everyone interested know that it will be retried.
            controller.fireCrawledURINeedRetryEvent(curi);
            controller.recover.rescheduled(curi);
            return;
        }

        // curi will definitely be disposed of without retry, so remove from q
        wq.dequeue();
        decrementQueuedCount(1);
        log(curi);

        if (curi.isSuccess()) {
            totalProcessedBytes += curi.getContentSize();
            incrementSucceededFetchCount();
            // Let everyone know in case they want to do something before we strip the curi.
            controller.fireCrawledURISuccessfulEvent(curi);
            controller.recover.finishedSuccess(curi);
        } else if (isDisregarded(curi)) {
            // Check for codes that mean that while we the crawler did
            // manage to schedule it, it must be disregarded for some reason.
            incrementDisregardedUriCount();
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

            incrementFailedFetchCount();
            this.controller.recover.finishedFailure(curi);
        }

        long delay_ms = politenessDelayFor(curi);
        synchronized(wq) {
            if (delay_ms > 0) {
                snoozeQueue(wq,now,delay_ms);
            } else {
                readyQueue(wq);
            }
        }

        curi.stripToMinimal();
        curi.processingCleanup();

    }

    /**
     * @param wq
     * @param now
     * @param delay_ms
     */
    private void snoozeQueue(BdbWorkQueue wq, long now, long delay_ms) {
        wq.setWakeTime(now+delay_ms);
        snoozedClassQueues.add(wq);
        wakeDaemon.executeAfterDelay(delay_ms,waker);
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
        alreadyIncluded.forget(canonicalize(curi.getUURI()));
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
        return pendingUris.getInitialMarker(regexpr);
    }

    /** (non-Javadoc)
     *
     * @param marker
     * @param numberOfMatches
     * @param verbose
     * @return List of URIs (strings).
     * @throws InvalidFrontierMarkerException
     */
    public ArrayList getURIsList(FrontierMarker marker, int numberOfMatches,
            boolean verbose) throws InvalidFrontierMarkerException {
        List curis; 
        try {
            curis = pendingUris.getFrom(marker,numberOfMatches);
        } catch (DatabaseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        ArrayList results = new ArrayList(curis.size());
        Iterator iter = curis.iterator();
        while(iter.hasNext()) {
            CrawlURI curi = (CrawlURI) iter.next();
            results.add("["+curi.getClassKey()+"] "+curi.getLine());
        }
        return results;
    }

    /**
     * @param match String to  match.
     * @return Number of items deleted.
     */
    public long deleteURIs(String match) {
        long count;
        try {
            count = pendingUris.deleteMatching(match);
        } catch (DatabaseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        Iterator iter = allQueues.values().iterator();
        while(iter.hasNext()) {
            ((BdbWorkQueue)iter.next()).unpeek();
        }
        // TODO: update per-queue counts!
        decrementQueuedCount(count);
        return count;
    }

    /**
     * @return One-line summary report, useful for display when full report
     * may be unwieldy. 
     */
    public String oneLineReport() {
        StringBuffer rep = new StringBuffer();
        rep.append(allQueues.size() + " queues: " + 
                inProcessQueues.uniqueSet().size() + " in-process; " +
                readyClassQueues.getCount() + " ready; " + 
                snoozedClassQueues.size() + " snoozed");
        // TODO: include active, snoozed counts if/when possible
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
        rep.append(" Queued:        " + queuedUriCount() + "\n");
        rep.append(" Finished:      " + finishedUriCount() + "\n");
        rep.append("  Successfully: " + succeededFetchCount() + "\n");
        rep.append("  Failed:       " + failedFetchCount() + "\n");
        rep.append("  Disregarded:  " + disregardedUriCount() + "\n");
        rep.append("\n -----===== QUEUES =====-----\n");
        rep.append(" Already included size:     " + alreadyIncluded.count()
                + "\n");
        rep.append("\n All class queues map size: " + allQueues.size() + "\n");
        rep.append(  "         In-process queues: " + 
                inProcessQueues.uniqueSet().size()  + "\n");
        rep.append(  "              Ready queues: " + 
                readyClassQueues.getCount() + "\n");
        rep.append(  "            Snoozed queues: " + 
                snoozedClassQueues.size() + "\n");
        rep.append("\n -----===== READY QUEUES =====-----\n");
        Iterator iter = readyClassQueues.iterator();
        int count = 0; 
        while(iter.hasNext() && count < REPORT_MAX_QUEUES ) {
            BdbWorkQueue q = (BdbWorkQueue) iter.next();
            count++;
            rep.append(q.report());
        }
        if(readyClassQueues.getCount()>REPORT_MAX_QUEUES) {
            rep.append("...and "+(readyClassQueues.getCount()-REPORT_MAX_QUEUES)+" more.\n");
        }
        rep.append("\n -----===== SNOOZED QUEUES =====-----\n");
        count = 0;
        iter = snoozedClassQueues.iterator();
        while(iter.hasNext() && count < REPORT_MAX_QUEUES ) {
            BdbWorkQueue q = (BdbWorkQueue) iter.next();
            count++;
            rep.append(q.report());
        }
        if(snoozedClassQueues.size()>REPORT_MAX_QUEUES) {
            rep.append("...and "+(snoozedClassQueues.size()-REPORT_MAX_QUEUES)+" more.\n");
        }    
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
        incrementDisregardedUriCount();
        curi.stripToMinimal();
        curi.processingCleanup();
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.URIFrontier#considerIncluded(org.archive.crawler.datamodel.UURI)
     */
    public void considerIncluded(UURI u) {
        this.alreadyIncluded.note(canonicalize(u));
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
        /** database holding all pending URIs, grouped in virtual queues */
        protected Database pendingUrisDB = null;
        /** database supporting bdb serialization */
        protected Database classCatalogDB = null;
        /** supporting bdb serialization */ 
        protected StoredClassCatalog classCatalog = null;
        /**  supporting bdb serialization of CrawlURIs*/
        protected SerialBinding crawlUriBinding;

        /**
         * Create the multi queue in the given environment. 
         * 
         * @param env bdb environment to use
         * @throws DatabaseException
         */
        public BdbMultiQueue(Environment env) throws DatabaseException {
            // Open the database. Create it if it does not already exist. 
            DatabaseConfig dbConfig = new DatabaseConfig();
            dbConfig.setAllowCreate(true);
            pendingUrisDB = env.openDatabase(null, "pending", dbConfig);
            pendingUrisDB.truncate(null, false);
            classCatalogDB = env.openDatabase(null, "classes", dbConfig);
            classCatalog = new StoredClassCatalog(classCatalogDB);
            crawlUriBinding = new SerialBinding(classCatalog, CrawlURI.class);
        }

        /**
         * Delete all CrawlURIs matching the given expression.
         * 
         * @param match
         * @return count of deleted items
         * @throws DatabaseException
         */
        public long deleteMatching(String match) throws DatabaseException {
            long deletedCount = 0;
            Pattern pattern = Pattern.compile(match);
            DatabaseEntry key = new DatabaseEntry();
            DatabaseEntry value = new DatabaseEntry();

            Cursor cursor = null;
            try {
                cursor = pendingUrisDB.openCursor(null, null);
                OperationStatus result = cursor.getNext(key, value, null);

                while (result == OperationStatus.SUCCESS) {
                    CrawlURI curi = (CrawlURI) crawlUriBinding
                            .entryToObject(value);
                    if (pattern.matcher(curi.getURIString()).matches()) {
                        cursor.delete();
                        deletedCount++;
                    }
                    result = cursor.getNext(key, value, null);
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            return deletedCount;
        }

        /**
         * @param m marker
         * @param maxMatches
         * @return list of matches starting from marker position
         * @throws DatabaseException
         */
        public List getFrom(FrontierMarker m, int maxMatches) throws DatabaseException {
            int matches = 0;
            int tries = 0;
            ArrayList results = new ArrayList(maxMatches);
            BdbFrontierMarker marker = (BdbFrontierMarker) m;
            
            DatabaseEntry key = marker.getStartKey();
            DatabaseEntry value = new DatabaseEntry();
            
            Cursor cursor = null;
            OperationStatus result = null;
            try {
                cursor = pendingUrisDB.openCursor(null,null);
                result = cursor.getSearchKey(key, value, null);
                
                while(matches<maxMatches && result == OperationStatus.SUCCESS) {
                    CrawlURI curi = (CrawlURI) crawlUriBinding.entryToObject(value);
                    if(marker.accepts(curi)) {
                        results.add(curi);
                        matches++;
                    }
                    tries++;
                    result = cursor.getNext(key,value,null);
                }
            } finally {
                if (cursor !=null) {
                    cursor.close();
                }
            }
            
            if(result != OperationStatus.SUCCESS) {
                // end of scan
                marker.setStartKey(null);
            }
            return results;
        }

        /**
         * Get a marker for beginning a scan over all contents
         * 
         * @param regexpr
         * @return a marker pointing to the first item
         */
        public FrontierMarker getInitialMarker(String regexpr) {
            try {
                return new BdbFrontierMarker(getFirstKey(), regexpr);
            } catch (DatabaseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return null; 
            }
        }

        /**
         * @return the key to the first item in the database
         * @throws DatabaseException
         */
        protected DatabaseEntry getFirstKey() throws DatabaseException {
            DatabaseEntry key = new DatabaseEntry();
            DatabaseEntry value = new DatabaseEntry();
            Cursor cursor = pendingUrisDB.openCursor(null,null);
            cursor.getNext(key,value,null);
            cursor.close();
            return key;
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
            Cursor cursor = pendingUrisDB.openCursor(null,null);
            cursor.getSearchKeyRange(headKey, result, null);
            cursor.close();
            CrawlURI retVal = (CrawlURI) crawlUriBinding.entryToObject(result);
            retVal.setHolderKey(headKey);
            return retVal;
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
            fp = fp | curi.getSchedulingDirective();
            ArchiveUtils.longIntoByteArray(fp, keyData, 0);
            ArchiveUtils.longIntoByteArray(curi.getOrdinal(), keyData, 8);
            return new DatabaseEntry(keyData);
        }

        /**
         * Delete the given CrawlURI from persistent store. Requires
         * the key under which it was stored be available. 
         * 
         * @param item
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
                pendingUrisDB.close();
                classCatalogDB.close();
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
    public class BdbWorkQueue implements Comparable {
        String classKey;
        
        /** where items are really stored, for now */
        BdbMultiQueue masterQueue; 
        
        /** total number of stored items */
        long count = 0;
        
        /** the next item to be returned */ 
        CrawlURI peekItem = null;

        /** key coordinate to begin seeks, to find queue head */
        byte[] origin; 

        // useful for reporting
        /** last URI enqueued */
        private String lastQueued; 
        /** last URI peeked */
        private String lastPeeked;

        /** whether queue is already in lifecycle stage */
        boolean isHeld = false; 
        
        /** time to wake, if snoozed */
        long wakeTime = 0;
        
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
         * @return a string describing this queue
         */
        public String report() {
            return "Queue " + classKey + "\n" + 
                   "  "+ count + " items" + "\n" +
                   "    last enqueued: " + lastQueued + "\n" +
                   "      last peeked: " + lastPeeked + "\n" +
                   ((wakeTime == 0) ? "" :
                   "         wakes in: " + 
                           (wakeTime-System.currentTimeMillis()) + "ms\n");
        }

        /**
         * @param l
         */
        public void setWakeTime(long l) {
            wakeTime = l;
        }

        /**
         * @return wakeTime
         */
        public long getWakeTime() {
            return wakeTime;
        }
        
        /**
         * @return classKey
         */
        private String getClassKey() {
            return classKey;
        }
        
        /**
         * Clear isHeld to false
         */
        public void clearHeld() {
            isHeld = false;
        }

        /**
         * Whether the queue is already in a lifecycle stage --
         * such as ready, in-progress, snoozed -- and thus should
         * not be redundantly inserted to readyClassQueues
         * 
         * @return isHeld
         */
        public boolean isHeld() {
            return isHeld;
        }

        /**
         * Set isHeld to true
         */
        public void setHeld() {
            isHeld = true; 
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
                    lastPeeked = peekItem.getURIString();
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
                lastQueued = curi.getURIString();
            } catch (DatabaseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            count++;
        }

        /* (non-Javadoc)
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        public int compareTo(Object obj) {
            if(this==obj) {
                return 0; // for exact identity only
            }
            BdbWorkQueue other = (BdbWorkQueue)obj;
            if (getWakeTime() > other.getWakeTime()) {
                return 1;
            }
            if (getWakeTime() < other.getWakeTime()) {
                return -1;
            }
            // at this point, the ordering is arbitrary, but still
            // must be consistent/stable over time
            return this.classKey.compareTo(other.getClassKey());
        }
    }

    /**
     * Marker for remembering a position within the BdbMultiQueue.
     * 
     * @author gojomo
     */
    public class BdbFrontierMarker implements FrontierMarker {
        DatabaseEntry startKey;
        Pattern pattern; 
        int nextItemNumber;
        
        /**
         * Create a marker pointed at the given start location.
         * 
         * @param startKey
         * @param regexpr
         */
        public BdbFrontierMarker(DatabaseEntry startKey, String regexpr) {
            this.startKey = startKey;
            pattern = Pattern.compile(regexpr);
            nextItemNumber = 1;
        }

        /**
         * @param curi
         * @return whether the marker accepts the given CrawlURI
         */
        public boolean accepts(CrawlURI curi) {
            boolean retVal = pattern.matcher(curi.getURIString()).matches();
            if(retVal==true) {
                nextItemNumber++;
            }
            return retVal;
        }

        /**
         * @param key position for marker
         */
        public void setStartKey(DatabaseEntry key) {
            startKey = key;
        }

        /**
         * @return startKey
         */
        public DatabaseEntry getStartKey() {
            return startKey;
        }

        /* (non-Javadoc)
         * @see org.archive.crawler.framework.FrontierMarker#getMatchExpression()
         */
        public String getMatchExpression() {
            return pattern.pattern();
        }

        /* (non-Javadoc)
         * @see org.archive.crawler.framework.FrontierMarker#getNextItemNumber()
         */
        public long getNextItemNumber() {
            return nextItemNumber;
        }

        /* (non-Javadoc)
         * @see org.archive.crawler.framework.FrontierMarker#hasNext()
         */
        public boolean hasNext() {
            // as long as any startKey is stated, consider as having next
            return startKey != null;
        }
    }

}

