/* Copyright (C) 2003 Internet Archive.
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
 * SimpleFrontier.java
 * Created on Oct 1, 2003
 *
 * $Header$
 */
package org.archive.crawler.frontier;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;

import org.apache.commons.collections.Predicate;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.URIException;
import org.archive.crawler.checkpoint.ObjectPlusFilesInputStream;
import org.archive.crawler.checkpoint.ObjectPlusFilesOutputStream;
import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlHost;
import org.archive.crawler.datamodel.CrawlServer;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.datamodel.UURISet;
import org.archive.crawler.event.CrawlStatusListener;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.URIFrontier;
import org.archive.crawler.framework.URIFrontierMarker;
import org.archive.crawler.framework.exceptions.FatalConfigurationException;
import org.archive.crawler.framework.exceptions.InvalidURIFrontierMarkerException;
import org.archive.crawler.settings.ModuleType;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.Type;
import org.archive.crawler.util.FPUURISet;
import org.archive.util.ArchiveUtils;
import org.archive.util.DiskBackedQueue;
import org.archive.util.MemLongFPSet;
import org.archive.util.MemQueue;
import org.archive.util.PaddingStringBuffer;
import org.archive.util.Queue;

/**
 * A basic mostly breadth-first frontier, which refrains from
 * emitting more than one CrawlURI of the same 'key' (host) at
 * once, and respects minimum-delay and delay-factor specifications
 * for politeness.
 * 
 * There is one generic 'pendingQueue', and then an arbitrary
 * number of other 'KeyedQueues' each representing a certain
 * 'key' class of URIs -- effectively, a single host (by hostname).
 * 
 * KeyedQueues may have an item in-process -- in which case they
 * do not provide any other items for processing. KeyedQueues may
 * also be 'snoozed' -- when they should be kept inactive for a 
 * period of time, to either enforce politeness policies or allow
 * a configurable amount of time between error retries. 
 * 
 * 
 * @author Gordon Mohr
 */
public class Frontier
    extends ModuleType
    implements URIFrontier, FetchStatusCodes, CoreAttributeConstants,
        CrawlStatusListener {
    // be robust against trivial implementation changes
    private static final long serialVersionUID = ArchiveUtils.classnameBasedUID(Frontier.class,1);

    private static final Logger logger =
        Logger.getLogger(Frontier.class.getName());

    /** how many multiples of last fetch elapsed time to wait before recontacting same server */
    public final static String ATTR_DELAY_FACTOR = "delay-factor";
    /** always wait this long after one completion before recontacting 
     * same server, regardless of multiple */
    public final static String ATTR_MIN_DELAY = "min-delay-ms";
    /** never wait more than this long, regardless of multiple */
    public final static String ATTR_MAX_DELAY = "max-delay-ms";
    /** always wait at least this long between request *starts*
     * (contrasted with min-delay: if min-interval time has already elapsed during last
     * fetch, then next fetch may occur immediately; it constrains starts not off-cycles) */
    public final static String ATTR_MIN_INTERVAL = "min-interval-ms";
    /** maximum times to emit a CrawlURI without final disposition */
    public final static String ATTR_MAX_RETRIES = "max-retries";
    /** for retryable problems, seconds to wait before a retry */
    public final static String ATTR_RETRY_DELAY = "retry-delay-seconds";
    /** maximum simultaneous requests in process to a host (queue) */
    public final static String ATTR_HOST_VALENCE = "host-valence";
    /** maximum overall bandwidth usage */
    public final static String ATTR_MAX_OVERALL_BANDWIDTH_USAGE =
        "total-bandwidth-usage-KB-sec";
    /** maximum per-host bandwidth usage */
    public final static String ATTR_MAX_HOST_BANDWIDTH_USAGE =
        "max-per-host-bandwidth-usage-KB-sec";

    /** how many items to store in memory atop of the pending queue
     * higher == more RAM used per active host; lower == more disk IO */
    public final static String ATTR_PENDING_QUEUE_MEMORY_CAPACITY =
        "pending-queue-memory-capacity";
    /** maximum how many items to store in memory atop each keyedqueue
     * higher == more RAM used per active host; lower == more disk IO */
    public final static String ATTR_HOST_QUEUES_MEMORY_CAPACITY =
        "host-queues-memory-capacity";


    protected final static Float DEFAULT_DELAY_FACTOR = new Float(5);
    protected final static Integer DEFAULT_MIN_DELAY = new Integer(500);
    protected final static Integer DEFAULT_MAX_DELAY = new Integer(5000);
    protected final static Integer DEFAULT_MIN_INTERVAL = new Integer(1000);
    protected final static Integer DEFAULT_MAX_RETRIES = new Integer(30);
    protected final static Long DEFAULT_RETRY_DELAY = new Long(900); //15 minutes
    protected final static Integer DEFAULT_HOST_VALENCE = new Integer(1); 
    protected final static Integer DEFAULT_MAX_OVERALL_BANDWIDTH_USAGE =
        new Integer(0);
    protected final static Integer DEFAULT_MAX_HOST_BANDWIDTH_USAGE =
        new Integer(0);

    protected final static Integer DEFAULT_PENDING_QUEUE_MEMORY_CAPACITY =
        new Integer(10000);
    protected final static Integer DEFAULT_HOST_QUEUES_MEMORY_CAPACITY =
        new Integer(200);

    
    protected final static float KILO_FACTOR = 1.024F;

    protected final static String F_ADD = "F+ ";
    protected final static String F_EMIT = "Fe ";
    protected final static String F_RESCHEDULE = "Fr ";
    protected final static String F_SUCCESS = "Fs ";
    protected final static String F_FAILURE = "Ff ";

    protected CrawlController controller;

    // those UURIs which are already in-process (or processed), and
    // thus should not be rescheduled
    protected UURISet alreadyIncluded;
    /** ordinal numbers to assign to created CrawlURIs */
    protected long nextOrdinal = 1;
    
    private ThreadLocalQueue threadWaiting = new ThreadLocalQueue();

    // every CandidateURI not yet in process or another queue;
    // all seeds start here; may contain duplicates
    protected Queue pendingQueue; // of CandidateURIs

    // all active per-class queues
    HashMap allClassQueuesMap = new HashMap(); // of String (classKey) -> KeyedQueue

    // all per-class queues whose first item may be handed out (that is, no CrawlURI
    // of the same class is currently in-process)
    LinkedList readyClassQueues = new LinkedList(); // of String (queueKey) -> KeyedQueue

    // all per-class queues who are on hold until a certain time
    SortedSet snoozeQueues = new TreeSet(new SchedulingComparator()); // of KeyedQueue, sorted by wakeTime    
    
    // top-level stats
    long queuedCount = 0;
    
    long successCount = 0;
    long failedCount = 0;
    long disregardedCount = 0; //URI's that are disregarded (for example because of robot.txt rules)
    
    long totalProcessedBytes = 0;


    // Used when bandwidth constraint are used
    long nextURIEmitTime = 0;
    long processedBytesAfterLastEmittedURI = 0;
    int lastMaxBandwidthKB = 0;
    
    /**
     * @param name
     */
    public Frontier(String name) {
        //The 'name' of all frontiers should be the same (URIFrontier.ATTR_NAME)
        //therefore we'll ignore the supplied parameter. 
        super(URIFrontier.ATTR_NAME, "Frontier. \nMaintains the internal" +
            " state of the crawl. It dictates the order in which URIs" +
            " will be scheduled. \nThis frontier is mostly a breadth-first" +
            " frontier, which refrains from emitting more than one" +
            " CrawlURI of the same \'key\' (host) at once, and respects" +
            " minimum-delay and delay-factor specifications for" +
            " politeness.");
        addElementToDefinition(new SimpleType(ATTR_DELAY_FACTOR,
            "How many multiples of last fetch elapsed time to wait before " +
            "recontacting same server", DEFAULT_DELAY_FACTOR));
        addElementToDefinition(new SimpleType(ATTR_MAX_DELAY,
            "Never wait more than this long, regardless of multiple",
            DEFAULT_MAX_DELAY));
        addElementToDefinition(new SimpleType(ATTR_MIN_DELAY,
            "Always wait this long after one completion before recontacting " +
            "same server, regardless of multiple", DEFAULT_MIN_DELAY));
        addElementToDefinition(new SimpleType(ATTR_MIN_INTERVAL,
            "Always wait at least this long between request *starts*. \n " +
            "Contrasted with min-delay: if min-interval time has already " +
            "elapsed during last fetch, then next fetch may occur " +
            "immediately; it constrains starts not off-cycles.",
            DEFAULT_MIN_INTERVAL));
        addElementToDefinition(new SimpleType(ATTR_MAX_RETRIES,
            "How often to retry fetching a URI that failed to be retrieved.\n" +
            "If zero, the crawler will get the robots.txt only.",
            DEFAULT_MAX_RETRIES));
        addElementToDefinition(new SimpleType(ATTR_RETRY_DELAY,
                "How long to wait by default until we retry fetching a URI that " +
                "failed to be retrieved (seconds). ",
                DEFAULT_RETRY_DELAY));
        Type t;
        t = addElementToDefinition(new SimpleType(ATTR_HOST_VALENCE,
                "Maximum number of simultaneous requests to a single" +
                "host.",
                DEFAULT_HOST_VALENCE));
        t.setExpertSetting(true);
        t = addElementToDefinition(
            new SimpleType(ATTR_MAX_OVERALL_BANDWIDTH_USAGE,
            "The maximum average bandwidth the crawler is allowed to use. \n" +
            "The actual readspeed is not affected by this setting, it only " +
            "holds back new URIs from being processed when the bandwidth " +
            "usage has been to high.\n0 means no bandwidth limitation.",
            DEFAULT_MAX_OVERALL_BANDWIDTH_USAGE));
        t.setOverrideable(false);
        t = addElementToDefinition(
            new SimpleType(ATTR_MAX_HOST_BANDWIDTH_USAGE,
            "The maximum average bandwidth the crawler is allowed to use per " +
            "host. \nThe actual readspeed is not affected by this setting, " +
            "it only holds back new URIs from being processed when the " +
            "bandwidth usage has been to high.\n0 means no bandwidth " +
            "limitation.",
            DEFAULT_MAX_HOST_BANDWIDTH_USAGE));
        t.setExpertSetting(true);
        t = addElementToDefinition(
                new SimpleType(ATTR_PENDING_QUEUE_MEMORY_CAPACITY,
                "Size of the pending queue's in memory head.\n Once it grows " +
                "beyond this size additional items will be written to a file " +
                "on disk. Default value " + 
                DEFAULT_PENDING_QUEUE_MEMORY_CAPACITY + ". Higher value " +
                "means more RAM used; lower means more disk I/O",
                DEFAULT_PENDING_QUEUE_MEMORY_CAPACITY));
        t.setOverrideable(false);
        t.setExpertSetting(true);
        t = addElementToDefinition(
                new SimpleType(ATTR_HOST_QUEUES_MEMORY_CAPACITY,
                "Size of each host queue's in memory head.\n Once each grows " +
                "beyond this size additional items will be written to a file " +
                "on disk. Default value " + DEFAULT_HOST_QUEUES_MEMORY_CAPACITY+
                ". A high value means more RAM used per host queue while a low"+
                " value will require more disk I/O.",
                DEFAULT_HOST_QUEUES_MEMORY_CAPACITY));
        t.setExpertSetting(true);
    }

    /**
     * Initializes the Frontier, given the supplied CrawlController.
     *
     * @see org.archive.crawler.framework.URIFrontier#initialize(org.archive.crawler.framework.CrawlController)
     */
    public void initialize(CrawlController c)
        throws FatalConfigurationException, IOException {
        this.controller = c;

        pendingQueue = createPendingQueue(c.getStateDisk(),"pendingQ");

        alreadyIncluded = createAlreadyIncluded(c.getStateDisk(),
                "alreadyIncluded");
        
        loadSeeds();
    }
    
    /**
     * Create a queue that will serve as the pending queue.
     * 
     * @param dir Directory where pending queue files should be written
     * @param filePrefix Prefix to names of pending queue files
     * @param capacity In memory cache capacity.
     * @return A queue that is usable as a pending queue.
     * @throws IOException If problems occur creating files on disk
     */
    protected Queue createPendingQueue(File dir, String filePrefix)
            throws IOException, FatalConfigurationException {
        try {
            return new DiskBackedQueue(
                    dir,
                    filePrefix,
                    false,
                    ((Integer) getAttribute(ATTR_PENDING_QUEUE_MEMORY_CAPACITY))
                        .intValue());
        } catch (AttributeNotFoundException e) {
            throw new FatalConfigurationException("AttributeNotFoundException " +
                    "encountered on reading " + 
                    ATTR_PENDING_QUEUE_MEMORY_CAPACITY + ". Message:\n" +
                    e.getMessage());
        } catch (MBeanException e) {
            throw new FatalConfigurationException("MBeanException " +
                    "encountered on reading " + 
                    ATTR_PENDING_QUEUE_MEMORY_CAPACITY + ". Message:\n" +
                    e.getMessage());
        } catch (ReflectionException e) {
            throw new FatalConfigurationException("ReflectionException " +
                    "encountered on reading " + 
                    ATTR_PENDING_QUEUE_MEMORY_CAPACITY + ". Message:\n" +
                    e.getMessage());
        }
    }
    
    /**
     * Create a UURISet that will serve as record of already seen URIs.
     * 
     * @param dir Directory where the set's files should be written
     * @param filePrefix Prefix to names of the set's files
     * @return A UURISet that will serve as a record of already seen URIs
     * @throws IOException If problems occur creating files on disk
     */
    protected UURISet createAlreadyIncluded(File dir, String filePrefix)
            throws IOException, FatalConfigurationException {
        // TODO: Make the uri set configurable? 
        // Can be overridden by subclasses
        return new FPUURISet(new MemLongFPSet(20,0.75f));
        //return new PagedUURISet(c.getScratchDisk());

        // alternative: pure disk-based set
        // return new FPUURISet(new DiskLongFPSet(c.getScratchDisk(),"alreadyIncluded",3,0.5f));

        // alternative: disk-based set with in-memory cache supporting quick positive contains() checks
        // return = new FPUURISet(
        //         new CachingDiskLongFPSet(
        //                 c.getScratchDisk(),
        //                 "alreadyIncluded",
        //                 23, // 8 million slots on disk (for start)
        //                 0.75f,
        //                 20, // 1 million slots in cache (always)
        //                 0.75f));
        
    }

    /**
     * Load up the seeds.
     * 
     * This method is called on initialize and inside in the crawlcontroller
     * when it wants to force reloading of configuration.
     * 
     * @see org.archive.crawler.framework.CrawlController#kickUpdate()
     */
    public void loadSeeds() {
        // Get the seeds to refresh and then get an iterator inside a 
        // synchronization block.  The seeds list may get updated during our
        // iteration. This will throw a concurrentmodificationexception unless
        // we synchronize.
        //
        // TODO:  THe calling the refreshSeeds method forces the reading of all 
        // seeds into a cache.  This might not be always what is wanted, 
        // particularly if broad crawl with millions of seeds.
        this.controller.getScope().refreshSeeds();
        List seeds = this.controller.getScope().getSeedlist();
        synchronized(seeds) {
            for (Iterator i = seeds.iterator(); i.hasNext();) {
                UURI u = (UURI)i.next();
                CandidateURI caUri = new CandidateURI(u);
                caUri.setSeed();
                caUri.setSchedulingDirective(CandidateURI.HIGH);
                innerSchedule(caUri);
            }
        }
    }

    private static class ThreadLocalQueue extends ThreadLocal implements Serializable {
        /* (non-Javadoc)
         * @see java.lang.ThreadLocal#initialValue()
         */
        protected Object initialValue() {
            return new MemQueue();
        }

        /**
         * @return Queue of 'batched' items
         */
        public Queue getQueue() {
            return (Queue)super.get();
        }

    }

    /**
     * @see org.archive.crawler.framework.URIFrontier#batchSchedule(org.archive.crawler.datamodel.CandidateURI)
     */
    public void batchSchedule(CandidateURI caUri) {
        threadWaiting.getQueue().enqueue(caUri);
    }

    /** 
     * 
     * @see org.archive.crawler.framework.URIFrontier#batchFlush()
     */
    public synchronized void batchFlush() {
        innerBatchFlush();
    }
    
    private void innerBatchFlush(){
        Queue q = threadWaiting.getQueue();
        while(!q.isEmpty()) {
            innerSchedule((CandidateURI) q.dequeue());
        }
    }

    /**
     * Arrange for the given CandidateURI to be visited, if it is not
     * already scheduled/completed.
     *
     * @see org.archive.crawler.framework.URIFrontier#schedule(org.archive.crawler.datamodel.CandidateURI)
     */
    public synchronized void schedule(CandidateURI caUri) {
        innerSchedule(caUri);
    }

    /**
     * Arrange for the given CandidateURI to be visited, if it is not
     * already scheduled/completed.
     *
     * @param caUri The CandidateURI to schedule
     */
    private void innerSchedule(CandidateURI caUri) {
        if(!caUri.forceFetch() && this.alreadyIncluded.contains(caUri)) {
            logger.finer("Disregarding alreadyIncluded "+caUri);
            return;
        }
        
        if(caUri.isSeed() && caUri.getVia() != null 
                && caUri.flattenVia().length()>0){
            // The only way a seed can have a non-empty via is if it is the 
            // result of a seed redirect.  Add it to the seeds list.
            // 
            // This is a feature.  This is handling for case where a seed 
            // gets immediately redirected to another page.  What we're doing is
            // treating the immediate redirect target as a seed.
            List seeds = this.controller.getScope().getSeedlist();
            synchronized(seeds) {
                seeds.add(caUri.getUURI());
            }
            // And it needs immediate scheduling.
            caUri.setSchedulingDirective(CandidateURI.HIGH);
        }

        if(caUri.needsImmediateScheduling()) {
            enqueueToKeyed(asCrawlUri(caUri));
        } else {
            this.pendingQueue.enqueue(caUri);
        }
        this.alreadyIncluded.add(caUri);
        this.queuedCount++;
        // Update recovery log.
        this.controller.recover.info("\n"+F_ADD+caUri.getURIString());
    }

    /**
     * Return the next CrawlURI to be processed (and presumably
     * visited/fetched) by a a worker thread.
     *
     * First checks any "Ready" per-host queues, then the global 
     * pending queue.
     *
     * @return next CrawlURI to be processed. Or null if none is availible.
     * 
     * @see org.archive.crawler.framework.URIFrontier#next(int)
     */
    private CrawlURI next() {
        long now = System.currentTimeMillis();
        CrawlURI curi = null;

        enforceBandwidthThrottle(now);
        
        CandidateURI caUri;

        // Check for snoozing queues who are ready to wake up.
        wakeReadyQueues(now);

        // now, see if any holding queues are ready with a CrawlURI
        if (!this.readyClassQueues.isEmpty()) {
            curi = dequeueFromReady();
            try {
                return emitCuri(curi);
            }
            catch (URIException e) {
                logger.severe("Failed holding emitcuri: " + e.getMessage());
            }
        }

        // if that fails to find anything, check the pending queue
        while ((caUri = dequeueFromPending()) != null) {
            curi = asCrawlUri(caUri);
            enqueueToKeyed(curi);
            if (!this.readyClassQueues.isEmpty()) {
                curi = dequeueFromReady();
                try {
                    return emitCuri(curi);
                }
                catch (URIException e) {
                    logger.severe("Failed dequeue emitcuri: " + e.getMessage());
                }
            }
        }

        // consider if URIs exhausted
        if(isEmpty()) {
            // nothing left to crawl
            logger.info("nothing left to crawl");
            return null;
        } else {
            // nothing to return now, but there are still URIs
            // held for the future
            return null;
        }
    }
    
    private CrawlURI asCrawlUri(CandidateURI caUri) {
        if(caUri instanceof CrawlURI) {
            return (CrawlURI) caUri;
        }
        return CrawlURI.from(caUri,nextOrdinal++);
    }

    private void enforceBandwidthThrottle(long now) {
        int maxBandwidthKB;
        try {
            maxBandwidthKB = ((Integer) getAttribute(
                    ATTR_MAX_OVERALL_BANDWIDTH_USAGE)).intValue();
        } catch (Exception e) {
            // Should never happen, but if, return default.
            logger.severe(e.getLocalizedMessage());
            maxBandwidthKB = DEFAULT_MAX_OVERALL_BANDWIDTH_USAGE.intValue();
        }
        if (maxBandwidthKB > 0) {
            // Make sure that new bandwidth setting doesn't affect total crawl
            if (maxBandwidthKB != lastMaxBandwidthKB) {
                lastMaxBandwidthKB = maxBandwidthKB;
                processedBytesAfterLastEmittedURI = totalProcessedBytes;
            }

            // Enforce bandwidth limit
            long sleepTime = nextURIEmitTime - now;

            float maxBandwidth = maxBandwidthKB * KILO_FACTOR;
            long processedBytes =
                totalProcessedBytes - processedBytesAfterLastEmittedURI;
            long shouldHaveEmittedDiff =
                nextURIEmitTime == 0 ? 0 : nextURIEmitTime - now;
            nextURIEmitTime = (long) (processedBytes / maxBandwidth)
                    + now + shouldHaveEmittedDiff;
            processedBytesAfterLastEmittedURI = totalProcessedBytes;

            if (sleepTime > 0) {
                synchronized(this) {
                    logger.fine("Frontier sleeps for: " + sleepTime
                            + "ms to respect bandwidth limit.");
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        logger.warning(e.getLocalizedMessage());
                    }
                }
            }
        }
    }

    /** 
     * @param timeout Time to wait on next CrawlURI.
     * @return The next CrawlURI eligible for processing.
     * @throws InterruptedException
     */
    public synchronized CrawlURI next(int timeout) throws InterruptedException {
        long now = System.currentTimeMillis();
        long until = now + timeout;
        
        while(now<until) {
            // Keep trying till we hit timeout.
            CrawlURI curi = next();
            if(curi!=null) {
                return curi;
            }
            long earliestWake = earliestWakeTime();
            // Sleep to timeout or earliestWakeup time, whichever comes first
            long sleepuntil = earliestWake < until ? earliestWake : until;
            if(sleepuntil > now){
                wait(sleepuntil-now); // If new URIs are scheduled, we will be woken
            }
            now = System.currentTimeMillis();
        }
        return null;
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
     * @see org.archive.crawler.framework.URIFrontier#finished(org.archive.crawler.datamodel.CrawlURI)
     */
    public synchronized void finished(CrawlURI curi) {
        logger.fine("Frontier.finished: " + curi.getURIString());
        // Catch up on scheduling
        innerBatchFlush();

        notify(); // new items might be available, let waiting threads know

        try {
            // no need to update queues if user deleted items since they
            // can not possibly have been in processing and do not affect
            // politeness
            if(curi.getFetchStatus() != S_DELETED_BY_USER){
                noteProcessingDone(curi);
            }
            
            if (curi.isSuccess()) {
                successDisposition(curi);
            } else if (needsPromptRetry(curi)) {
                // Consider statuses which allow nearly-immediate retry
                // (like deferred to allow precondition to be fetched)
                reschedule(curi);
            } else if (needsRetrying(curi)) {
                // Consider errors which can be retried
                scheduleForRetry(curi);
            } else if(isDisregarded(curi)) {
                // Check for codes that mean that while we the crawler did
                // manage to get it it must be disregarded for any reason.
                disregardDisposition(curi);
            } else {
                // In that case FAILURE, note & log
                failureDisposition(curi);
            }
            
        } catch (RuntimeException e) {
            curi.setFetchStatus(S_RUNTIME_EXCEPTION);
            // store exception temporarily for logging
            curi.getAList().putObject(A_RUNTIME_EXCEPTION, e);
            failureDisposition(curi);
        } catch (AttributeNotFoundException e) {
            logger.severe(e.getMessage());
        }
        
        finally {
            curi.processingCleanup();
        }
    }

    /**
     * @param curi
     */
    private void disregardDisposition(CrawlURI curi) {
        //Let interested listeners know of disregard disposition.
        controller.fireCrawledURIDisregardEvent(curi);

        // send to basic log
        curi.aboutToLog();
        Object array[] = { curi };
        controller.uriProcessing.log(
            Level.INFO,
            curi.getUURI().toString(),
            array);

        // if exception, also send to crawlErrors
        if (curi.getFetchStatus() == S_RUNTIME_EXCEPTION) {
            controller.runtimeErrors.log(
                Level.WARNING,
                curi.getUURI().toString(),
                array);
        }
        if (shouldBeForgotten(curi)) {
            // curi is dismissed without prejudice: it can be reconstituted
            forget(curi);
        } else {
            disregardedCount++;
            curi.stripToMinimal();
        }
    }

    private boolean isDisregarded(CrawlURI curi) {
        switch (curi.getFetchStatus()) {
            case S_ROBOTS_PRECLUDED :     // they don't want us to have it
            case S_OUT_OF_SCOPE :         // filtered out by scope
            case S_BLOCKED_BY_USER :      // filtered out by user
            case S_TOO_MANY_EMBED_HOPS :  // too far from last true link
            case S_TOO_MANY_LINK_HOPS :   // too far from seeds
            case S_DELETED_BY_USER :      // user deleted
                return true;
            default:
                return false;
        }
    }

    /**
     * Take note of any processor-local errors that have
     * been entered into the CrawlURI.
     * @param curi
     *
     */
    private void logLocalizedErrors(CrawlURI curi) {
        if(curi.getAList().containsKey(A_LOCALIZED_ERRORS)) {
            List localErrors = (List)curi.getAList().getObject(A_LOCALIZED_ERRORS);
            Iterator iter = localErrors.iterator();
            while(iter.hasNext()) {
                Object array[] = { curi, iter.next() };
                controller.localErrors.log(
                    Level.WARNING,
                    curi.getUURI().toString(),
                    array);
            }
            // once logged, discard
            curi.getAList().remove(A_LOCALIZED_ERRORS);
        }
    }

    /**
     * The CrawlURI has been successfully crawled, and will be
     * attempted no more.
     *
     * @param curi The CrawlURI
     */
    private void successDisposition(CrawlURI curi) {
        totalProcessedBytes += curi.getContentSize();

        curi.aboutToLog();
        Object array[] = { curi };
        controller.uriProcessing.log(
            Level.INFO,
            curi.getUURI().toString(),
            array);

        // note that CURI has passed out of scheduling
        if (shouldBeForgotten(curi)) {
            // curi is dismissed without prejudice: it can be reconstituted
            forget(curi);
        } else {
            successCount++;
        }
        controller.fireCrawledURISuccessfulEvent(curi); //Let everyone know in case they want to do something before we strip the curi.
        curi.stripToMinimal();
        controller.recover.info("\n"+F_SUCCESS+curi.getURIString());
    }



    /**
     * Store is empty only if all queues are empty and
     * no URIs are in-process
     *
     * @return True if queues are empty.
     */
    public boolean isEmpty() {
        return pendingQueue.isEmpty()
                && allClassQueuesMap.isEmpty();
    }


    /**
     * Wake any snoozed queues whose snooze time is up.
     * @param now Current time in millisec.
     */
    protected void wakeReadyQueues(long now) {
        while(!snoozeQueues.isEmpty()&&((KeyedQueue)snoozeQueues.first()).getWakeTime()<=now) {
            KeyedQueue awoken = (KeyedQueue)snoozeQueues.first();
            if (!snoozeQueues.remove(awoken)) {
                logger.severe("first() item couldn't be remove()d! - "+awoken+" - " + snoozeQueues.contains(awoken));
                logger.severe(report());
            }
            // assert awoken.getInProcessItem() == null : "false ready: class peer still in process";
            awoken.wake();
            updateQ(awoken);
        }
    }

    private void discardQueue(KeyedQueue q) {
        allClassQueuesMap.remove(q.getClassKey());
        q.discard();
        q.release();
        //assert !heldClassQueues.contains(q) : "heldClassQueues holding dead q";
        assert !readyClassQueues.contains(q) : "readyClassQueues holding dead q";
        assert !snoozeQueues.contains(q) : "snoozeQueues holding dead q";
        //assert heldClassQueues.size()+readyClassQueues.size()+snoozeQueues.size() <= allClassQueuesMap.size() : "allClassQueuesMap discrepancy";
    }

    private CrawlURI dequeueFromReady() {
        KeyedQueue firstReadyQueue = (KeyedQueue)readyClassQueues.getFirst();
        assert firstReadyQueue.getState() == KeyedQueue.READY : "top ready queue not ready but" + firstReadyQueue.getState();
        CrawlURI readyCuri = (CrawlURI) firstReadyQueue.dequeue();
        firstReadyQueue.checkEmpty();
        return readyCuri;
    }

    /**
     * Prepares a CrawlURI for crawling. Also marks it as 'being processed'.
     * @param curi The CrawlURI
     * @return The CrawlURI
     * @see #noteInProcess(CrawlURI)
     * @throws URIException
     */
    private CrawlURI emitCuri(CrawlURI curi) throws URIException {
        if(curi != null) {
            noteInProcess(curi);
            if (this.controller == null ||
                    this.controller.getServerCache() == null ) {
                logger.warning("Controller or ServerCache is null processing " +
                    curi);
            } else {
                CrawlServer cs = this.controller.getServerCache().
                    getServerFor(curi);
                if (cs != null) {
                    curi.setServer(cs);
                }
            }
        }
        logger.finer(this + ".emitCuri(" + curi + ")");
        this.controller.recover.info("\n"+F_EMIT+curi.getURIString());
        // One less URI in the queue.
        this.queuedCount--;
        return curi;
    }

    /**
     * Marks a CrawlURI as being in process.
     * @param curi The CrawlURI
     */
    protected void noteInProcess(CrawlURI curi) {
        KeyedQueue kq = keyedQueueFor(curi);
        if(kq==null){
            return; // Couldn't find/create kq.
        }
        
        // assert kq.getInProcessItem() == null : "two CrawlURIs with same classKey in process";

        assert kq.getState() == KeyedQueue.READY || kq.getState() == KeyedQueue.EMPTY : 
            "odd state "+ kq.getState() + " for classQueue "+ kq + "of to-be-emitted CrawlURI";

        kq.noteInProcess(curi);
        if(kq.getState()==KeyedQueue.BUSY || kq.getState() == KeyedQueue.EMPTY) {
            readyClassQueues.remove(kq);
        }
    }

    /**
     * Get the KeyedQueue for a CrawlURI. If it does not exist it will be
     * created.
     * @param curi The CrawlURI
     * @return The KeyedQueeu for the CrawlURI or null if it does not exist and
     *         an exception occured trying to create it.
     */
    protected KeyedQueue keyedQueueFor(CrawlURI curi) {
        KeyedQueue kq = null;
        try {
            kq = (KeyedQueue)this.allClassQueuesMap.get(curi.getClassKey());
        }
        catch (URIException e1) {
            logger.severe("Failed to get class key: " + e1.getMessage() + " " +
                curi);
        }
        if (kq==null) {
            try {
                try {
					kq = new KeyedQueue(curi.getClassKey(), 
					    this.controller.getStateDisk(),
					    ((Integer) getAttribute(ATTR_HOST_QUEUES_MEMORY_CAPACITY
                                ,curi)).intValue());
				} catch (AttributeNotFoundException e3) {
                    logger.severe(e3.getMessage());
				}
                try {
                    kq.setValence(((Integer)getAttribute(ATTR_HOST_VALENCE,curi)).intValue());
                } catch (AttributeNotFoundException e2) {
                    logger.severe(e2.getMessage());
                }
                kq.activate(); // TODO: have only a subset of queues by active at any one time
                this.allClassQueuesMap.put(kq.getClassKey(),kq);
            } catch (IOException e) {
                // An IOException occured trying to make new KeyedQueue.
                curi.getAList().putObject(A_RUNTIME_EXCEPTION,e);
                Object array[] = { curi };
                this.controller.runtimeErrors.log(
                        Level.SEVERE,
                        curi.getUURI().toString(),
                        array);
            }
        }
        return kq;
    }

    protected CandidateURI dequeueFromPending() {
        if (this.pendingQueue.isEmpty()) {
            return null;
        }
        return (CandidateURI)this.pendingQueue.dequeue();
    }

    /**
     * Place CrawlURI on the queue for its class (server). If KeyedQueue does
     * not exist it will be created. Failure to create the KeyedQueue (due
     * to errors) will cause the method to return without error. The failure
     * to create the KeyedQueue will have been logged.
     *
     * @param curi The CrawlURI
     */
    protected void enqueueToKeyed(CrawlURI curi) {
        KeyedQueue kq = keyedQueueFor(curi);
        if(kq==null){
            logger.severe("No workQueue found for "+curi);
            return; // Couldn't find/create kq.
        }

        kq.enqueue(curi);

        if(kq.checkEmpty()) {
            // if kq state changed, update frontier's internals
            updateQ(kq);
        }
    }

    /**
     * If an empty queue has become ready, add to ready queues.
     * Should only be called after state has changed. 
     * 
     * @param kq
     */
    private void updateQ(KeyedQueue kq) {
        Object state = kq.getState();
        if (kq.isDiscardable()) {
            // empty & ready; discard
            discardQueue(kq);
            return;
        }
        if (state == KeyedQueue.READY ) {
            // has become ready
            readyClassQueues.add(kq);
            synchronized (this) {
                notify(); // wake a waiting thread
            }
            return;
        } 
        // otherwise, no need to change whatever state it's in
        // TODO: verify this in only reached in sensible situations
    }
        
    /**
     * stop this queue from being actively scheduled
     * essentially: a reaction to a serious connectivity
     * problem or operator request
     * 
     * @param kq
     */ 
    public synchronized void freezeQueue(KeyedQueue kq) {
        if(kq.getState()== KeyedQueue.SNOOZED) {
            snoozeQueues.remove(kq);
        } else if (kq.getState() == KeyedQueue.READY ) {
            readyClassQueues.remove(kq);
        }
        kq.freeze();
    }
    
    /**
     * allow this queue to be actively scheduled
     * (by operator request)
     * 
     * @param kq
     */
    public synchronized void unfreezeQueue(KeyedQueue kq) {
        kq.unfreeze();
        kq.activate(); // TODO: implement active/inactive distinctions
    }

    protected long earliestWakeTime() {
        if (!snoozeQueues.isEmpty()) {
            return ((KeyedQueue)snoozeQueues.first()).getWakeTime();
        }
        return Long.MAX_VALUE;
    }

    /**
     *
     * @param curi
     * @throws AttributeNotFoundException
     */
    protected void noteProcessingDone(CrawlURI curi) throws AttributeNotFoundException {
        curi.incrementFetchAttempts();
        logLocalizedErrors(curi);
        KeyedQueue kq = keyedQueueFor(curi);
        if(kq==null){
            logger.severe("No workQueue found for "+curi);
            return; // Couldn't find/create kq.
        }
        Object startState = kq.getState();
        kq.noteProcessDone(curi);
        updateScheduling(curi, kq);
        if(startState!=kq.getState()) {
            updateQ(kq);
        }
    }

    /**
     * Update any scheduling structures with the new information
     * in this CrawlURI. Chiefly means make necessary arrangements
     * for no other URIs at the same host to be visited within the
     * appropriate politeness window.
     *
     * @param curi The CrawlURI
     * @param kq A KeyedQueue 
     * @throws AttributeNotFoundException
     */
    protected void updateScheduling(CrawlURI curi, KeyedQueue kq) throws AttributeNotFoundException {
        long durationToWait = 0;
        if (curi.getAList().containsKey(A_FETCH_BEGAN_TIME)
            && curi.getAList().containsKey(A_FETCH_COMPLETED_TIME)) {

            long completeTime = curi.getAList().getLong(A_FETCH_COMPLETED_TIME);
            long durationTaken = (completeTime - curi.getAList().getLong(A_FETCH_BEGAN_TIME));
            durationToWait =
                    (long) (((Float) getAttribute(ATTR_DELAY_FACTOR, curi)).floatValue()
                        * durationTaken);

            long minDelay = ((Integer) getAttribute(ATTR_MIN_DELAY, curi)).longValue();
            if (minDelay > durationToWait) {
                // wait at least the minimum
                durationToWait = minDelay;
            }

            long maxDelay = ((Integer) getAttribute(ATTR_MAX_DELAY, curi)).longValue();
            if (durationToWait > maxDelay) {
                // wait no more than the maximum
                durationToWait = maxDelay;
            }

            long minInterval = 
                ((Integer) getAttribute(ATTR_MIN_INTERVAL, curi)).longValue();
            if (durationToWait < (minInterval - durationTaken) ) {
                // wait at least as long as necessary to space off
                // from last fetch begin
                durationToWait = minInterval - durationTaken;
            }

            long now = System.currentTimeMillis();
            int maxBandwidthKB = ((Integer) getAttribute(
                        ATTR_MAX_HOST_BANDWIDTH_USAGE, curi)).intValue();
            if (maxBandwidthKB > 0) {
                // Enforce bandwidth limit
                CrawlHost host = curi.getServer().getHost();
                long minDurationToWait = 
                    host.getEarliestNextURIEmitTime() - now;
                float maxBandwidth = maxBandwidthKB * KILO_FACTOR;
                long processedBytes = curi.getContentSize();
                host.setEarliestNextURIEmitTime(
                        (long) (processedBytes / maxBandwidth) + now);

                if (minDurationToWait > durationToWait) {
                    durationToWait = minDurationToWait;
                }
            }
            
            if(durationToWait > 0) {
                snoozeQueueUntil(kq, completeTime + durationToWait);
                return;
            } 
        }
    }

    /**
     * The CrawlURI has encountered a problem, and will not
     * be retried.
     *
     * @param curi The CrawlURI
     */
    private void failureDisposition(CrawlURI curi) {
        //Let interested listeners know of failed disposition.
        this.controller.fireCrawledURIFailureEvent(curi);

        // send to basic log
        curi.aboutToLog();
        Object array[] = { curi };
        this.controller.uriProcessing.log(
            Level.INFO,
            curi.getUURI().toString(),
            array);

        // if exception, also send to crawlErrors
        if (curi.getFetchStatus() == S_RUNTIME_EXCEPTION) {
            this.controller.runtimeErrors.log(
                Level.WARNING,
                curi.getUURI().toString(),
                array);
        }
        if (shouldBeForgotten(curi)) {
            // curi is dismissed without prejudice: it can be reconstituted
            forget(curi);
        } else {
            this.failedCount++;
            curi.stripToMinimal();
        }
        this.controller.recover.info("\n"+F_FAILURE+curi.getURIString());
    }

    /**
     * Checks if a recently completed CrawlURI that did not finish successfully
     * needs to be retried immediately (processed again as soon as politeness
     * allows.)
     * 
     * @param curi The CrawlURI to check
     * @return True if we need to retry promptly.
     * @throws AttributeNotFoundException If problems occur trying to read the
     *            maximum number of retries from the settings framework.
     */
    private boolean needsPromptRetry(CrawlURI curi)
            throws AttributeNotFoundException {
        if (curi.getFetchAttempts() >=
                ((Integer)getAttribute(ATTR_MAX_RETRIES, curi)).intValue() ) {
            return false;
        }
        
        switch (curi.getFetchStatus()) {
            case S_DEFERRED:
                return true;
                
            case HttpStatus.SC_UNAUTHORIZED:
                // We can get here though usually a positive status code is
                // a success.  We get here if there is rfc2617 credential data
                // loaded and we're supposed to go around again.  See if any
                // rfc2617 credential present and if there, assume it got
                // loaded in FetchHTTP on expectation that we're to go around
                // again.  If no rfc2617 loaded, we should not be here.
                boolean loaded = curi.hasRfc2617CredentialAvatar();
                if (!loaded) {
                    logger.severe("Have 401 but no creds loaded " + curi);
                }
                return loaded;
            
            default:
                return false;
        }
    }
    
    /**
     * Checks if a recently completed CrawlURI that did not finish successfully
     * needs to be retried (processed again after some time elapses)
     * 
     * @param curi The CrawlURI to check
     * @return True if we need to retry.
     * @throws AttributeNotFoundException If problems occur trying to read the
     *            maximum number of retries from the settings framework.
     */
    private boolean needsRetrying(CrawlURI curi) throws AttributeNotFoundException {
        //
        if (curi.getFetchAttempts() >= ((Integer)getAttribute(ATTR_MAX_RETRIES,curi)).intValue() ) {
            return false;
        }
        switch (curi.getFetchStatus()) {
            case S_CONNECT_FAILED:
            case S_CONNECT_LOST:
                // these are all worth a retry
                // TODO: consider if any others (S_TIMEOUT in some cases?) deserve retry
                return true;
            default:
                return false;
        }
    }

    /**
     * @param curi
     * @throws AttributeNotFoundException
     */
    private void scheduleForRetry(CrawlURI curi) throws AttributeNotFoundException {
        long delay;

        if(curi.getAList().containsKey(A_RETRY_DELAY)) {
            delay = curi.getAList().getInt(A_RETRY_DELAY);
        } else {
            // use overall default
            delay = ((Long)getAttribute(ATTR_RETRY_DELAY,curi)).longValue();
        }
        if (delay>0) {
            // snooze to future
            logger.finer("inserting snoozed "+curi+" for "+delay);
            KeyedQueue kq = keyedQueueFor(curi);
            if(kq!=null){
                snoozeQueueUntil(kq,System.currentTimeMillis()+(delay*1000));
            }
        }

        reschedule(curi);
        
        controller.fireCrawledURINeedRetryEvent(curi); // Let everyone interested know that it will be retried.
        controller.recover.info("\n"+F_RESCHEDULE+curi.getURIString());
    }

    /**
     * Put near top of relevant keyedqueue (but behind anything recently
     * scheduled 'high')
     * 
     * @param curi CrawlURI to reschedule.
     */
    private void reschedule(CrawlURI curi) {
        // Eliminate state related to only prior processing passthrough.
        boolean isPrereq = curi.isPrerequisite(); 
        curi.processingCleanup(); // This will reset prereq value.
        if (isPrereq == false) {
        	curi.setSchedulingDirective(CandidateURI.MEDIUM);
        }
        enqueueToKeyed(curi);
        queuedCount++;
    }

    /**
     * Snoozes a queue until a fixed point in time has passed.
     *
     * @param kq A KeyedQueue that we want to snooze
     * @param wake Time (in millisec.) when we want the queue to stop snoozing.
     */
    protected void snoozeQueueUntil(KeyedQueue kq, long wake) {
        //assert kq.getStoreState() != KeyedQueue.IN_PROCESS : "snoozing queue should have been READY, EMPTY, or SNOOZED";
        if(kq.getState()==KeyedQueue.FROZEN) {
            // only explicit operator action my unfreeze a queue
            return;
        }
        if(kq.getState()== KeyedQueue.SNOOZED) {
            // must be removed before time may be mutated
            snoozeQueues.remove(kq);
        } else if (kq.getState() == KeyedQueue.READY ) {
            readyClassQueues.remove(kq);
        } 
        
        kq.setWakeTime(wake);
        snoozeQueues.add(kq);
        kq.snooze();
    }

    /**
     * Some URIs, if they recur,  deserve another
     * chance at consideration: they might not be too
     * many hops away via another path, or the scope
     * may have been updated to allow them passage.
     *
     * @param curi
     * @return True if curi should be forgotten.
     */
    private boolean shouldBeForgotten(CrawlURI curi) {
        switch(curi.getFetchStatus()) {
            case S_OUT_OF_SCOPE:
            case S_BLOCKED_BY_USER:
            case S_TOO_MANY_EMBED_HOPS:
            case S_TOO_MANY_LINK_HOPS:
                return true;
            default:
                return false;
        }
    }

    /**
     * Forget the given CrawlURI. This allows a new instance
     * to be created in the future, if it is reencountered under
     * different circumstances.
     *
     * @param curi The CrawlURI to forget
     */
    protected void forget(CrawlURI curi) {
        logger.finer("Forgetting "+curi);
        alreadyIncluded.remove(curi.getUURI());
    }

    /**  (non-Javadoc)
     * @see org.archive.crawler.framework.URIFrontier#discoveredUriCount()
     */
    public long discoveredUriCount(){
        return alreadyIncluded.size();
    }
    
    /** (non-Javadoc)
     * @see org.archive.crawler.framework.URIFrontier#queuedUriCount()
     */
    public long queuedUriCount(){
        return queuedCount;
    }
    
    /** (non-Javadoc)
     * @see org.archive.crawler.framework.URIFrontier#finishedUriCount()
     */
    public long finishedUriCount() {
        return successCount+failedCount+disregardedCount;
    }

    /** (non-Javadoc)
     * @see org.archive.crawler.framework.URIFrontier#pendingUriCount()
     */
    public long pendingUriCount() {
        // Always zero. No URI is kept pending. All are processed as soon as
        // they are scheduled.
        return 0;
    }

    /** (non-Javadoc)
     * @see org.archive.crawler.framework.URIFrontier#successfullyFetchedCount()
     */
    public long successfullyFetchedCount(){
        return successCount;
    }

    /** (non-Javadoc)
     * @see org.archive.crawler.framework.URIFrontier#failedFetchCount()
     */
    public long failedFetchCount(){
       return failedCount;
    }

    /** (non-Javadoc)
     * @see org.archive.crawler.framework.URIFrontier#disregardedFetchCount()
     */
    public long disregardedFetchCount() {
        return disregardedCount;
    }

    /** (non-Javadoc)
     * @see org.archive.crawler.framework.URIFrontier#totalBytesWritten()
     */
    public long totalBytesWritten() {
        return totalProcessedBytes;
    }

    /** (non-Javadoc)
     * @see org.archive.crawler.framework.URIFrontier#getInitialMarker(java.lang.String, boolean)
     */
    public URIFrontierMarker getInitialMarker(String regexpr, boolean inCacheOnly) {
        ArrayList keyqueueKeys = new ArrayList();
        if(allClassQueuesMap.size()!=0)
        {
            Iterator q = allClassQueuesMap.keySet().iterator();
            while(q.hasNext())
            {
                keyqueueKeys.add(q.next());
            }
        }
        return new FrontierMarker(regexpr,inCacheOnly,keyqueueKeys);
    }

    /** (non-Javadoc)
     * 
     * @param marker
     * @param numberOfMatches
     * @param verbose
     * @return List of URIS.
     * @throws InvalidURIFrontierMarkerException
     */
    public ArrayList getURIsList(URIFrontierMarker marker, int numberOfMatches,
            boolean verbose) throws InvalidURIFrontierMarkerException {
        if(marker instanceof FrontierMarker == false){
            throw new InvalidURIFrontierMarkerException();
        }
        
        FrontierMarker mark = (FrontierMarker)marker;
        ArrayList list = new ArrayList(numberOfMatches);

        // inspect the KeyedQueues
        while( numberOfMatches > 0 && mark.currentQueue != -1){
            String queueKey = (String)mark.keyqueues.get(mark.currentQueue);
            Queue keyq = (Queue)allClassQueuesMap.get(queueKey);
            if(keyq==null){
                throw new InvalidURIFrontierMarkerException();
            }
            
            numberOfMatches -= inspectQueue(keyq,"hostQueue("+queueKey+")",list,mark,verbose, numberOfMatches);
            if(numberOfMatches>0){
                mark.nextQueue();
            }
        }
        
        // inspect the PendingQueue
        if(numberOfMatches > 0 && mark.currentQueue == -1){
            numberOfMatches -= inspectQueue(pendingQueue,"pendingQueue",list,mark,verbose, numberOfMatches);
            if(numberOfMatches > 0){
                // reached the end
                mark.hasNext = false;
            }
        }

        return list;
    }

    /**
     * Adds any applicable URIs from a given queue to the given list.
     * 
     * @param queue
     *            The queue to inspect
     * @param queueName
     * @param list
     *            The list to add matched URIs to.
     * @param marker
     *            Where to start accepting matches from.
     * @param verbose
     *            List items are verbose
     * @param numberOfMatches
     *            maximum number of matches to add to list
     * @return the number of matches found
     * @throws InvalidURIFrontierMarkerException
     */
    private int inspectQueue( Queue queue,
                              String queueName,
                              ArrayList list,
                              FrontierMarker marker,
                              boolean verbose,
                              int numberOfMatches)
                          throws InvalidURIFrontierMarkerException{
        if(queue.length() < marker.absolutePositionInCurrentQueue){
            // Not good. Invalid marker.
            throw new InvalidURIFrontierMarkerException();
        }
        
        if(queue.length()==0){
            return 0;
        }
        
        Iterator it = queue.getIterator(marker.inCacheOnly);
        int foundMatches = 0;
        long itemsScanned = 0;
        while(it.hasNext() && foundMatches < numberOfMatches){
            Object o = it.next();
            if( itemsScanned >= marker.absolutePositionInCurrentQueue
                    && o instanceof CandidateURI ){
                // Ignore items that are in front of current position
                // and those that are not CandidateURIs.
                CandidateURI caURI = (CandidateURI)o;
                if(marker.match(caURI)){
                    // Found match.
                    String text;
                    if(verbose){
                        // A verbose description
                        PaddingStringBuffer verb = new PaddingStringBuffer();
                        verb.append(caURI.getURIString());
                        verb.append(" ("+queueName+":" + itemsScanned + ")");
                        verb.newline();
                        verb.padTo(2);
                        verb.append(caURI.getPathFromSeed());
                        if(caURI.getVia() != null 
                                && caURI.getVia() instanceof CandidateURI){
                            verb.append(" ");
                            verb.append(((CandidateURI)caURI.getVia()).getURIString());
                        }
                        text = verb.toString();
                    } else {
                        text = caURI.getURIString();
                    }
                    list.add(text);
                    foundMatches++;
                    marker.nextItemNumber++;
                }
            }
            itemsScanned++;
        }
        marker.absolutePositionInCurrentQueue = itemsScanned;
        return foundMatches;
    }

    /**
     * @param match String to  match.
     * @return Number of items deleted.
     */
    public long deleteURIs(String match) {
        long numberOfDeletes = 0;
        // Create QueueItemMatcher
        Predicate mat = new URIQueueMatcher(match, true, this);
        // Delete from all KeyedQueues
        if(allClassQueuesMap.size()!=0)
        {
            Iterator q = allClassQueuesMap.keySet().iterator();
            while(q.hasNext())
            {
                KeyedQueue kq = (KeyedQueue)allClassQueuesMap.get(q.next());
                numberOfDeletes += kq.deleteMatchedItems(mat);
                 
                // If our deleting has emptied the KeyedQueue then update it's
                // state.
                kq.checkEmpty();
            }
        }
        // Delete from pendingQueue
        numberOfDeletes += pendingQueue.deleteMatchedItems(mat);
        queuedCount -= numberOfDeletes;
        return numberOfDeletes;
    }

    /**
     * This method compiles a human readable report on the status of the frontier
     * at the time of the call.
     *
     * @return A report on the current status of the frontier.
     */
    public synchronized String report()
    {
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
        rep.append(" Already included size:     " + alreadyIncluded.size()+"\n");
        rep.append(" Pending queue length:      " + pendingQueue.length()+ "\n");
        rep.append("\n All class queues map size: " + allClassQueuesMap.size() + "\n");
        if(allClassQueuesMap.size()!=0)
        {
            Iterator q = allClassQueuesMap.keySet().iterator();
            while(q.hasNext())
            {
                KeyedQueue kq = (KeyedQueue)allClassQueuesMap.get(q.next());
                appendKeyedQueue(rep,kq,now);
            }
        }
        rep.append("\n Ready class queues size:   " + readyClassQueues.size() + "\n");
        for(int i=0 ; i < readyClassQueues.size() ; i++)
        {
            KeyedQueue kq = (KeyedQueue)readyClassQueues.get(i);
            appendKeyedQueue(rep,kq,now);
        }

        rep.append("\n Snooze queues size:        " + snoozeQueues.size() + "\n");
        if(snoozeQueues.size()!=0)
        {
            Object[] q = ((TreeSet)snoozeQueues).toArray();
            for(int i=0 ; i < q.length ; i++)
            {
                if(q[i] instanceof KeyedQueue)
                {
                    KeyedQueue kq = (KeyedQueue)q[i];
                    appendKeyedQueue(rep,kq,now);
                }
            }
        }

        return rep.toString();
    }


    private void appendKeyedQueue(StringBuffer rep, KeyedQueue kq, long now) {
        rep.append("    KeyedQueue  " + kq.getClassKey() + "\n");
        rep.append("     Length:    " + kq.length() + "\n");
//        rep.append("     Is ready:  " + kq.shouldWake() + "\n");
        rep.append("     Status:    " + kq.state.toString() + "\n");
        if(kq.getState()==KeyedQueue.SNOOZED) {
            rep.append("     Wakes in:  " + ArchiveUtils.formatMillisecondsToConventional(kq.getWakeTime()-now)+"\n");
        }
        if(kq.getInProcessItems().size()>0) {
            Iterator iter = kq.getInProcessItems().iterator();
            while (iter.hasNext()) {
                rep.append("     InProcess: " + iter.next() + "\n");
            }
        }
        if(!kq.isEmpty()) {
            rep.append("     Top URI:   " + ((CrawlURI)kq.peek()).getURIString()+"\n");

        }
    }

    /** (non-Javadoc)
     * @see org.archive.crawler.event.CrawlStatusListener#crawlPausing(java.lang.String)
     */
    public void crawlPausing(String statusMessage) {
        // We are not interested in the crawlPausing event
    }

    /** (non-Javadoc)
     * @see org.archive.crawler.event.CrawlStatusListener#crawlPaused(java.lang.String)
     */
    public void crawlPaused(String statusMessage) {
        // We are not interested in the crawlPaused event
    }

    /** (non-Javadoc)
     * @see org.archive.crawler.event.CrawlStatusListener#crawlResuming(java.lang.String)
     */
    public void crawlResuming(String statusMessage) {
        // We are not interested in the crawlResuming event
    }

    /** (non-Javadoc)
     * @see org.archive.crawler.event.CrawlStatusListener#crawlEnding(java.lang.String)
     */
    public void crawlEnding(String sExitMessage) {
        // We are not interested in the crawlEnding event
    }

    /** (non-Javadoc)
     * @see org.archive.crawler.event.CrawlStatusListener#crawlEnded(java.lang.String)
     */
    public void crawlEnded(String sExitMessage) {
        // Ok, if the CrawlController is exiting we delete our reference to it
        // to facilitate gc.
        this.controller = null;
    }

    /** (non-Javadoc)
     * @see org.archive.crawler.framework.URIFrontier#importRecoverLog(java.lang.String)
     */
    public void importRecoverLog(String pathToLog) throws IOException {
        // scan log for all 'Fs' lines: add as 'alreadyIncluded'
        BufferedReader reader = new BufferedReader(new FileReader(pathToLog));
        String read;
        while((read = reader.readLine()) != null) {
            if(read.startsWith(F_SUCCESS)) {
                UURI u;
                try {
                    u = new UURI(read.substring(3));
                    this.alreadyIncluded.add(u);
                } catch (URIException e) {
                    e.printStackTrace();
                }
            }
        }
        reader.close();
        // scan log for all 'F+' lines: if not alreadyIncluded, schedule for
        // visitation
        reader = new BufferedReader(new FileReader(pathToLog));
        while((read = reader.readLine()) != null) {
            if(read.startsWith(F_ADD)) {
                UURI u;
                try {
                    u = new UURI(read.substring(3));
                    if(!this.alreadyIncluded.contains(u)) {
                        CandidateURI caUri = new CandidateURI(u);
                        caUri.setVia(pathToLog);
                        // TODO: reevaluate if this is correct
                        caUri.setPathFromSeed("L"); 
                        schedule(caUri);
                    }
                } catch (URIException e) {
                    e.printStackTrace();
                }
            }
        }
        reader.close();

    }

    // custom serialization
    private void writeObject(ObjectOutputStream stream) throws IOException {
        ObjectPlusFilesOutputStream coostream = (ObjectPlusFilesOutputStream)stream;
        coostream.pushAuxiliaryDirectory("frontier");
        coostream.defaultWriteObject();
        coostream.popAuxiliaryDirectory();
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        ObjectPlusFilesInputStream coistream = (ObjectPlusFilesInputStream)stream;
        coistream.pushAuxiliaryDirectory("frontier");
        coistream.defaultReadObject();
        coistream.popAuxiliaryDirectory();
    }
}
