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
package org.archive.crawler.basic;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
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

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlHost;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.datamodel.UURISet;
import org.archive.crawler.datamodel.settings.CrawlerModule;
import org.archive.crawler.datamodel.settings.SimpleType;
import org.archive.crawler.datamodel.settings.Type;
import org.archive.crawler.event.CrawlStatusListener;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.URIFrontier;
import org.archive.crawler.framework.URIFrontierMarker;
import org.archive.crawler.framework.exceptions.FatalConfigurationException;
import org.archive.crawler.framework.exceptions.InvalidURIFrontierMarkerException;
import org.archive.crawler.util.FPUURISet;
import org.archive.util.ArchiveUtils;
import org.archive.util.DiskBackedQueue;
import org.archive.util.MemLongFPSet;
import org.archive.util.MemQueue;
import org.archive.util.PaddingStringBuffer;
import org.archive.util.Queue;
import org.archive.util.QueueItemMatcher;

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
 * @author Gordon Mohr
 */
public class Frontier
    extends CrawlerModule
    implements URIFrontier, FetchStatusCodes, CoreAttributeConstants, CrawlStatusListener {

    private static final int DEFAULT_CLASS_QUEUE_MEMORY_HEAD = 200;
    // how many multiples of last fetch elapsed time to wait before recontacting same server
    public final static String ATTR_DELAY_FACTOR = "delay-factor";
    // always wait this long after one completion before recontacting same server, regardless of multiple
    public final static String ATTR_MIN_DELAY = "min-delay-ms";
    // never wait more than this long, regardless of multiple
    public final static String ATTR_MAX_DELAY = "max-delay-ms";
    // always wait at least this long between request *starts*
    // (contrasted with min-delay: if min-interval time has already elapsed during last
    // fetch, then next fetch may occur immediately; it constrains starts not off-cycles)
    public final static String ATTR_MIN_INTERVAL = "min-interval-ms";

    public final static String ATTR_MAX_RETRIES = "max-retries";
    public final static String ATTR_RETRY_DELAY = "retry-delay-seconds";
    public final static String ATTR_MAX_OVERALL_BANDWIDTH_USAGE =
        "total-bandwidth-usage-KB-sec";
    public final static String ATTR_MAX_HOST_BANDWIDTH_USAGE =
        "max-per-host-bandwidth-usage-KB-sec";

    private final static Float DEFAULT_DELAY_FACTOR = new Float(5);
    private final static Integer DEFAULT_MIN_DELAY = new Integer(500);
    private final static Integer DEFAULT_MAX_DELAY = new Integer(5000);
    private final static Integer DEFAULT_MIN_INTERVAL = new Integer(1000);
    private final static Integer DEFAULT_MAX_RETRIES = new Integer(30);
    private final static Long DEFAULT_RETRY_DELAY = new Long(900); //15 minutes
    private final static Integer DEFAULT_MAX_OVERALL_BANDWIDTH_USAGE =
        new Integer(0);
    private final static Integer DEFAULT_MAX_HOST_BANDWIDTH_USAGE =
        new Integer(0);
    private final static float KILO_FACTOR = 1.024F;
    
    private static Logger logger =
        Logger.getLogger("org.archive.crawler.basic.Frontier");

    private final static String F_ADD = "F+ ";
    private final static String F_EMIT = "Fe ";
    private final static String F_RESCHEDULE = "Fr ";
    private final static String F_SUCCESS = "Fs ";
    private final static String F_FAILURE = "Ff ";

    CrawlController controller;

    // those UURIs which are already in-process (or processed), and
    // thus should not be rescheduled
    UURISet alreadyIncluded;

    private ThreadLocalQueue threadWaiting = new ThreadLocalQueue();

    // every CandidateURI not yet in process or another queue;
    // all seeds start here; may contain duplicates
    Queue pendingQueue; // of CandidateURIs

    // all active per-class queues
    HashMap allClassQueuesMap = new HashMap(); // of String (classKey) -> KeyedQueue

    // all per-class queues whose first item may be handed out (that is, no CrawlURI
    // of the same class is currently in-process)
    LinkedList readyClassQueues = new LinkedList(); // of String (queueKey) -> KeyedQueue

    // all per-class queues who are on hold until a certain time
    SortedSet snoozeQueues = new TreeSet(new SchedulingComparator()); // of KeyedQueue, sorted by wakeTime    
    
    // top-level stats
    long discoveredCount = 0;
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
            "How often to retry fetching a URI that failed to be retrieved. ",
            DEFAULT_MAX_RETRIES));
        addElementToDefinition(new SimpleType(ATTR_RETRY_DELAY,
                "How long to wait by default until we retry fetching a URI that " +
                "failed to be retrieved (seconds). ",
                DEFAULT_RETRY_DELAY));
        Type t;
        t = addElementToDefinition(
            new SimpleType(ATTR_MAX_OVERALL_BANDWIDTH_USAGE,
            "The maximum average bandwidth the crawler is allowed to use. \n" +
            "The actual readspeed is not affected by this setting, it only " +
            "holds back new URIs from being processed when the bandwidth " +
            "usage has been to high.\n0 means no bandwidth limitation.",
            DEFAULT_MAX_OVERALL_BANDWIDTH_USAGE));
        t.setOverrideable(false);
        addElementToDefinition(
            new SimpleType(ATTR_MAX_HOST_BANDWIDTH_USAGE,
            "The maximum average bandwidth the crawler is allowed to use per " +
            "host. \nThe actual readspeed is not affected by this setting, " +
            "it only holds back new URIs from being processed when the " +
            "bandwidth usage has been to high.\n0 means no bandwidth " +
            "limitation.",
            DEFAULT_MAX_HOST_BANDWIDTH_USAGE));
    }

    /**
     * Initializes the Frontier, given the supplied CrawlController.
     *
     * @see org.archive.crawler.framework.URIFrontier#initialize(org.archive.crawler.framework.CrawlController)
     */
    public void initialize(CrawlController c)
        throws FatalConfigurationException, IOException {

        pendingQueue = new DiskBackedQueue(c.getScratchDisk(),"pendingQ",10000);

        alreadyIncluded = new FPUURISet(new MemLongFPSet(20,0.75f));
        
        this.controller = c;
        controller.addCrawlStatusListener(this);
        loadSeeds(c);
    }

    private synchronized void loadSeeds(CrawlController c) {
        c.getScope().refreshSeedsIteratorCache();
        Iterator iter = c.getScope().getSeedsIterator();
        while (iter.hasNext()) {
            UURI u = (UURI) iter.next();
            CandidateURI caUri = new CandidateURI(u);
            caUri.setSeed();
            caUri.setSchedulingDirective(CandidateURI.HIGH);
            innerSchedule(caUri);
        }
    }

    private static class ThreadLocalQueue extends ThreadLocal {
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

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.URIFrontier#batchSchedule(org.archive.crawler.datamodel.CandidateURI)
     */
    public void batchSchedule(CandidateURI caUri) {
        threadWaiting.getQueue().enqueue(caUri);
    }

    /* (non-Javadoc)
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
        if(!caUri.forceFetch() && alreadyIncluded.contains(caUri)) {
            logger.finer("Disregarding alreadyIncluded "+caUri);
            return;
        }
        
        if(caUri.isSeed() && caUri.getVia() != null 
                && caUri.flattenVia().length()>0){
            // The only way a seed can have a non empty via is if it is the 
            // result of a seed redirect. Add it to the seeds list.
            controller.getScope().addSeed(caUri.getUURI());
            // And it needs immediate scheduling.
            caUri.setSchedulingDirective(CandidateURI.HIGH);
        }

        if(caUri.needsImmediateScheduling()) {
            enqueueHigh(CrawlURI.from(caUri));
        } else {
            pendingQueue.enqueue(caUri);
        }
        alreadyIncluded.add(caUri);
        discoveredCount++;
        queuedCount++;
        // Update recovery log.
        controller.recover.info("\n"+F_ADD+caUri.getURIString());
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
        long waitMax = 0;
        CrawlURI curi = null;

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
        
        CandidateURI caUri;

        // Check for snoozing queues who are ready to wake up.
        wakeReadyQueues(now);

        // now, see if any holding queues are ready with a CrawlURI
        if (!readyClassQueues.isEmpty()) {
            curi = dequeueFromReady();
            return emitCuri(curi);
        }

        // if that fails to find anything, check the pending queue
        while ((caUri = dequeueFromPending()) != null) {
            curi = CrawlURI.from(caUri);
            enqueueToKeyed(curi);
            if (!readyClassQueues.isEmpty()) {
                curi = dequeueFromReady();
                return emitCuri(curi);
            }
        }

        // consider if URIs exhausted
        if(isEmpty()) {
            // nothing left to crawl
            logger.info("nothing left to crawl");
            return null;
        }

        // nothing to return, but there are still URIs
        // held for the future

        return null;
    }
    
    /* (non-Javadoc)
     * @see org.archive.crawler.framework.URIFrontier#next(int)
     */
    public synchronized CrawlURI next(int timeout) throws InterruptedException {
        long now = System.currentTimeMillis();
        long until = now + timeout;
        
        while(now<until) {
            // Keep trying till we hit timout.
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
            
            if (curi.getFetchStatus() > 0) {
                // Regard any status larger then 0 as success.
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
        controller.throwCrawledURIDisregardEvent(curi);

        // send to basic log
        curi.aboutToLog();
        Object array[] = { curi };
        controller.uriProcessing.log(
            Level.INFO,
            curi.getUURI().getURIString(),
            array);

        // if exception, also send to crawlErrors
        if (curi.getFetchStatus() == S_RUNTIME_EXCEPTION) {
            controller.runtimeErrors.log(
                Level.WARNING,
                curi.getUURI().getURIString(),
                array);
        }
        if (shouldBeForgotten(curi)) {
            // curi is dismissed without prejudice: it can be reconstituted
            forget(curi);
        } else {
            disregardedCount++;
            curi.setStoreState(URIStoreable.FINISHED);
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
                    curi.getUURI().getURIString(),
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
            curi.getUURI().getURIString(),
            array);

        // note that CURI has passed out of scheduling
        if (shouldBeForgotten(curi)) {
            // curi is dismissed without prejudice: it can be reconstituted
            forget(curi);
        } else {
            successCount++;
            curi.setStoreState(URIStoreable.FINISHED);
        }
        controller.throwCrawledURISuccessfulEvent(curi); //Let everyone know in case they want to do something before we strip the curi.
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
        while(!snoozeQueues.isEmpty()&&((URIStoreable)snoozeQueues.first()).getWakeTime()<=now) {
            URIStoreable awoken = (URIStoreable)snoozeQueues.first();
            if (!snoozeQueues.remove(awoken)) {
                logger.severe("first() item couldn't be remove()d! - "+awoken+" - " + snoozeQueues.contains(awoken));
                logger.severe(report());
            }
            if (awoken instanceof KeyedQueue) {
                KeyedQueue awokenKQ = (KeyedQueue)awoken;
                assert awokenKQ.getInProcessItem() == null : "false ready: class peer still in process";
                if(((KeyedQueue)awoken).isEmpty()) {
                    // just drop queue
                    discardQueue(awoken);
                    return;
                }
                readyQueue((KeyedQueue)awoken); 
            } else {
                assert false : "something evil has awoken!";
            }
        }
    }

    private void discardQueue(URIStoreable q) {
        allClassQueuesMap.remove(((KeyedQueue)q).getClassKey());
        q.setStoreState(URIStoreable.FINISHED);
        ((KeyedQueue)q).release();
        //assert !heldClassQueues.contains(q) : "heldClassQueues holding dead q";
        assert !readyClassQueues.contains(q) : "readyClassQueues holding dead q";
        assert !snoozeQueues.contains(q) : "snoozeQueues holding dead q";
        //assert heldClassQueues.size()+readyClassQueues.size()+snoozeQueues.size() <= allClassQueuesMap.size() : "allClassQueuesMap discrepancy";
    }

    private CrawlURI dequeueFromReady() {
        KeyedQueue firstReadyQueue = (KeyedQueue)readyClassQueues.getFirst();
        assert firstReadyQueue.getStoreState() == URIStoreable.READY : "top ready queue not ready";
        CrawlURI readyCuri = (CrawlURI) firstReadyQueue.dequeue();
        if (firstReadyQueue.isEmpty()) {
            firstReadyQueue.setStoreState(URIStoreable.EMPTY); 
        }
        return readyCuri;
    }

    /**
     * Prepares a CrawlURI for crawling. Also marks it as 'being processed'.
     * @param curi The CrawlURI
     * @return The CrawlURI
     * @see #noteInProcess(CrawlURI)
     */
    private CrawlURI emitCuri(CrawlURI curi) {
        if(curi != null) {
            if (curi.getStoreState() == URIStoreable.FINISHED) {
                System.out.println("break here");
            }
            assert curi.getStoreState() != URIStoreable.FINISHED : "state "+curi.getStoreState()+" instead of ready for "+ curi;
            //assert curi.getAList() != null : "null alist in curi " + curi + " state "+ curi.getStoreState();
            noteInProcess(curi);
            curi.setServer(controller.getServerCache().getServerFor(curi));
        }
        logger.finer(this+".emitCuri("+curi+")");
        controller.recover.info("\n"+F_EMIT+curi.getURIString());
        // One less URI in the queue.
        queuedCount--;
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
            
        assert kq.getInProcessItem() == null : "two CrawlURIs with same classKey in process";

        kq.setInProcessItem(curi);
        curi.setStoreState(URIStoreable.IN_PROCESS);
 
        assert kq.getStoreState() == URIStoreable.READY || kq.getStoreState() == URIStoreable.EMPTY : 
            "odd state "+ kq.getStoreState() + " for classQueue "+ kq + "of to-be-emitted CrawlURI";
        readyClassQueues.remove(kq);
        //enqueueToHeld(classQueue);
        kq.setStoreState(URIStoreable.IN_PROCESS);
        //releaseHeld(curi);
    }

    /**
     * Get the KeyedQueue for a CrawlURI. If it does not exist it will be
     * created.
     * @param curi The CrawlURI
     * @return The KeyedQueeu for the CrawlURI or null if it does not exist and
     *         an exception occured trying to create it.
     */
    private KeyedQueue keyedQueueFor(CrawlURI curi) {
        KeyedQueue kq = (KeyedQueue) allClassQueuesMap.get(curi.getClassKey());
        if (kq==null) {
            try {
                kq = new KeyedQueue(curi.getClassKey(),controller.getScratchDisk(),DEFAULT_CLASS_QUEUE_MEMORY_HEAD);
                kq.setStoreState(URIStoreable.EMPTY);
                allClassQueuesMap.put(kq.getClassKey(),kq);
            } catch (IOException e) {
                // An IOException occured trying to make new KeyedQueue.
                curi.getAList().putObject(A_RUNTIME_EXCEPTION,e);
                Object array[] = { curi };
                controller.runtimeErrors.log(
                        Level.SEVERE,
                        curi.getUURI().getURIString(),
                        array);
            }
        }
        return kq;
    }

    protected CandidateURI dequeueFromPending() {
        if (pendingQueue.isEmpty()) {
            return null;
        }
        return (CandidateURI)pendingQueue.dequeue();
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
            return; // Couldn't find/create kq.
        }
            
        assert kq.getStoreState() == URIStoreable.EMPTY 
            || kq.getStoreState() == URIStoreable.READY 
            || kq.getStoreState() == URIStoreable.SNOOZED 
            || kq.getStoreState() == URIStoreable.IN_PROCESS 
            : "unexpected keyedqueue state";

        if(curi.needsImmediateScheduling()) {
            kq.enqueueHigh(curi);
        } else {
            kq.enqueue(curi);
        }
        updateQueue(kq);
    }

    /**
     * Place CrawlURI on a queue for its class (server), if queue
     * exists and contains other items
     *
     * @param curi The CrawlURI
     * @return true if enqueued
     */
    protected boolean enqueueIfNecessary(CrawlURI curi) {
        KeyedQueue kq = keyedQueueFor(curi);
        if(kq==null){
            return false; // Couldn't find/create kq.
        }
            
        assert kq.getStoreState() == URIStoreable.EMPTY 
            || kq.getStoreState() == URIStoreable.READY 
            || kq.getStoreState() == URIStoreable.SNOOZED 
            || kq.getStoreState() == URIStoreable.IN_PROCESS 
            : "unexpected keyedqueue state";
        if((kq.getInProcessItem()!=null)
                || kq.getStoreState()==URIStoreable.SNOOZED
                || !kq.isEmpty()) {
            // must enqueue
            if(curi.needsImmediateScheduling()) {
                kq.enqueueHigh(curi);
            } else {
                kq.enqueue(curi);
            }
            updateQueue(kq);
            return true;
        }
        return false;
    }

    /**
     * If an empty queue has become ready, add to ready queues
     * @param kq
     */
    private void updateQueue(KeyedQueue kq) {
        Object initialState = kq.getStoreState();
        if (kq.isEmpty() && initialState!=URIStoreable.SNOOZED) {
            // empty & ready; discard
            discardQueue(kq);
            return;
        }
        if (initialState == URIStoreable.EMPTY && !kq.isEmpty()) {
            // has become ready
            readyQueue(kq);
            return;
        } 
        // otherwise, no need to change whatever state it's in
        // TODO: verify this in only reached in sensible situations
    }

    private void readyQueue(KeyedQueue kq) {
        if(kq.isEmpty()) {
            discardQueue(kq);
            return;
        }
        readyClassQueues.add(kq);
        kq.setStoreState(URIStoreable.READY);
        notify(); // wake a waiting thread
    }

    /**
     * @param curi
     */
    private void enqueueMedium(CrawlURI curi) {
        KeyedQueue kq = keyedQueueFor(curi);
        if(kq==null){
            return; // Couldn't find/create kq.
        }
            
        kq.enqueueMedium(curi);
        updateQueue(kq);
    }

    /**
     * @param curi
     */
    private void enqueueHigh(CrawlURI curi) {
        KeyedQueue kq = keyedQueueFor(curi);
        if(kq==null){
            return; // Couldn't find/create kq.
        }
            
        kq.enqueueHigh(curi);
        updateQueue(kq);
    }

    protected long earliestWakeTime() {
        if (!snoozeQueues.isEmpty()) {
            return ((URIStoreable)snoozeQueues.first()).getWakeTime();
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
            return; // Couldn't find/create kq.
        }

        assert kq.getInProcessItem() == curi : "CrawlURI done wasn't inProcess";
        assert kq.getStoreState()
            == URIStoreable.IN_PROCESS : "odd state for classQueue of remitted CrawlURI";
        
        kq.setInProcessItem(null);
        updateScheduling(curi, kq);
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
        // otherwise, just ready
        readyQueue(kq);
    }

    /**
     * The CrawlURI has encountered a problem, and will not
     * be retried.
     *
     * @param curi The CrawlURI
     */
    private void failureDisposition(CrawlURI curi) {
        controller.throwCrawledURIFailureEvent(curi); //Let interested listeners know of failed disposition.

        // send to basic log
        curi.aboutToLog();
        Object array[] = { curi };
        controller.uriProcessing.log(
            Level.INFO,
            curi.getUURI().getURIString(),
            array);

        // if exception, also send to crawlErrors
        if (curi.getFetchStatus() == S_RUNTIME_EXCEPTION) {
            controller.runtimeErrors.log(
                Level.WARNING,
                curi.getUURI().getURIString(),
                array);
        }
        if (shouldBeForgotten(curi)) {
            // curi is dismissed without prejudice: it can be reconstituted
            forget(curi);
        } else {
            failedCount++;
            curi.setStoreState(URIStoreable.FINISHED);
            curi.stripToMinimal();
        }
        controller.recover.info("\n"+F_FAILURE+curi.getURIString());
    }

    /**
     * Has the CrawlURI suffered a failure which completes
     * its processing?
     *
     * @param curi
     * @return True if failure.
     */
    private boolean isDispositiveFailure(CrawlURI curi) {
        switch (curi.getFetchStatus()) {

            case S_DOMAIN_UNRESOLVABLE :
                // network errors; perhaps some of these
                // should be scheduled for retries
            case S_RUNTIME_EXCEPTION :
                // something unexpectedly bad happened
            case S_UNFETCHABLE_URI :
                // no chance to fetch
            case S_TOO_MANY_RETRIES :
                // no success after configurable number of retries
            case S_UNATTEMPTED :
                // nothing happened to this URI: don't send it through again

                return true;

            default :
                return false;
        }
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
    private boolean needsPromptRetry(CrawlURI curi) throws AttributeNotFoundException {
        if (curi.getFetchAttempts()>= ((Integer)getAttribute(ATTR_MAX_RETRIES,curi)).intValue() ) {
            return false;
        }
        switch (curi.getFetchStatus()) {
            case S_DEFERRED:
                return true;
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
            // case S_TIMEOUT: may not deserve retry
                // these are all worth a retry
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
        
        controller.throwCrawledURINeedRetryEvent(curi); // Let everyone interested know that it will be retried.
        controller.recover.info("\n"+F_RESCHEDULE+curi.getURIString());
    }

    private void reschedule(CrawlURI curi) {
        // put near top of relevant keyedqueue (but behind anything
        // recently scheduled 'high')
        KeyedQueue kq = keyedQueueFor(curi);
        if(kq==null){
            return; // Couldn't find/create kq.
        }
            
        curi.processingCleanup(); // eliminate state related to only prior processing passthrough
        kq.enqueueMedium(curi);
        queuedCount++;
    }

    /**
     * Snoozes a queue until a fixed point in time has passed.
     *
     * @param kq A KeyedQueue that we want to snooze
     * @param wake Time (in millisec.) when we want the queue to stop snoozing.
     */
    protected void snoozeQueueUntil(KeyedQueue kq, long wake) {
        //assert kq.getStoreState() != URIStoreable.IN_PROCESS : "snoozing queue should have been READY, EMPTY, or SNOOZED";
        if(kq.getStoreState()== URIStoreable.SNOOZED) {
            // must be removed before time may be mutated
            snoozeQueues.remove(kq);
        } else if (kq.getStoreState() == URIStoreable.READY ) {
            readyClassQueues.remove(kq);
        }
        kq.setWakeTime(wake);
        snoozeQueues.add(kq);
        kq.setStoreState(URIStoreable.SNOOZED);
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
        discoveredCount--;
        curi.setStoreState(URIStoreable.FORGOTTEN);
    }

    /*  (non-Javadoc)
     * @see org.archive.crawler.framework.URIFrontier#discoveredUriCount()
     */
    public long discoveredUriCount(){
        return discoveredCount;
    }
    
    /* (non-Javadoc)
     * @see org.archive.crawler.framework.URIFrontier#queuedUriCount()
     */
    public long queuedUriCount(){
        return queuedCount;
    }
    
    /* (non-Javadoc)
     * @see org.archive.crawler.framework.URIFrontier#finishedUriCount()
     */
    public long finishedUriCount() {
        return successCount+failedCount+disregardedCount;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.URIFrontier#pendingUriCount()
     */
    public long pendingUriCount() {
        // Always zero. No URI is kept pending. All are processed as soon as
        // they are scheduled.
        return 0;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.URIFrontier#successfullyFetchedCount()
     */
    public long successfullyFetchedCount(){
        return successCount;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.URIFrontier#failedFetchCount()
     */
    public long failedFetchCount(){
       return failedCount;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.URIFrontier#disregardedFetchCount()
     */
    public long disregardedFetchCount() {
        return disregardedCount;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.URIFrontier#totalBytesWritten()
     */
    public long totalBytesWritten() {
        return totalProcessedBytes;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.URIFrontier#getInitialMarker(java.lang.String, boolean)
     */
    public URIFrontierMarker getInitialMarker(String regexpr, boolean inCacheOnly) {
        ArrayList keyqueueKeys = new ArrayList();
        if(allClassQueuesMap.size()!=0)
        {
            Iterator q = allClassQueuesMap.keySet().iterator();
            int i = 1;
            while(q.hasNext())
            {
                keyqueueKeys.add(q.next());
            }
        }
        return new FrontierMarker(regexpr,inCacheOnly,keyqueueKeys);
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.URIFrontier#getURIsList(org.archive.crawler.framework.URIFrontierMarker, int, boolean)
     */
    public ArrayList getURIsList(URIFrontierMarker marker, int numberOfMatches, boolean verbose) throws InvalidURIFrontierMarkerException {
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

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.URIFrontier#deleteURIsFromPending(java.lang.String)
     */
    public long deleteURIs(String match) {
        long numberOfDeletes = 0;
        // Create QueueItemMatcher
        QueueItemMatcher mat = new URIQueueMatcher(match, true, this);
        // Delete from all KeyedQueues
        if(allClassQueuesMap.size()!=0)
        {
            Iterator q = allClassQueuesMap.keySet().iterator();
            int i = 1;
            while(q.hasNext())
            {
                KeyedQueue kq = (KeyedQueue)allClassQueuesMap.get(q.next());
                numberOfDeletes += kq.deleteMatchedItems(mat);
                 
                // If our deleting has emptied the KeyedQueue then update it's
                // state.
                if(kq.isEmpty()){
                    if(kq.getStoreState() == URIStoreable.READY){
                        readyClassQueues.remove(kq);
                    } else if(kq.getStoreState() == URIStoreable.SNOOZED){
                        snoozeQueues.remove(kq);
                    }
                    kq.setStoreState(URIStoreable.EMPTY);
                }
            }
        }
        // Delete from pendingQueue
        numberOfDeletes += pendingQueue.deleteMatchedItems(mat);
        return numberOfDeletes;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.URIFrontier#report()
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
        rep.append(" Discovered:    " + discoveredCount + "\n");
        rep.append(" Queued:        " + queuedCount + "\n");
        rep.append(" Finished:      " + finishedUriCount() + "\n");
        rep.append("  Successfully: " + successCount + "\n");
        rep.append("  Failed:       " + failedCount + "\n");
        rep.append("  Disregarded:  " + discoveredCount + "\n");
        rep.append("\n -----===== QUEUES =====-----\n");
        rep.append(" Already included size:     " + alreadyIncluded.size()+"\n");
        rep.append(" Pending queue length:      " + pendingQueue.length()+ "\n");
        rep.append("\n All class queues map size: " + allClassQueuesMap.size() + "\n");
        if(allClassQueuesMap.size()!=0)
        {
            Iterator q = allClassQueuesMap.keySet().iterator();
            int i = 1;
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
        rep.append("     Is ready:  " + kq.isReady() + "\n");
        rep.append("     Status:    " + kq.state.toString() + "\n");
        if(kq.getStoreState()==URIStoreable.SNOOZED) {
            rep.append("     Wakes in:  " + ArchiveUtils.formatMillisecondsToConventional(kq.getWakeTime()-now)+"\n");
        }
        if(kq.getStoreState()==URIStoreable.IN_PROCESS) {
            rep.append("     InProcess: " + kq.getInProcessItem() + "\n");
        }
        if(!kq.isEmpty()) {
            rep.append("     Top URI:   " + ((CrawlURI)kq.peek()).getURIString()+"\n");

        }
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.event.CrawlStatusListener#crawlPausing(java.lang.String)
     */
    public void crawlPausing(String statusMessage) {
        // We are not interested in the crawlPausing event
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.event.CrawlStatusListener#crawlPaused(java.lang.String)
     */
    public void crawlPaused(String statusMessage) {
        // We are not interested in the crawlPaused event
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.event.CrawlStatusListener#crawlResuming(java.lang.String)
     */
    public void crawlResuming(String statusMessage) {
        // We are not interested in the crawlResuming event
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.event.CrawlStatusListener#crawlEnding(java.lang.String)
     */
    public void crawlEnding(String sExitMessage) {
        // We are not interested in the crawlEnding event
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.event.CrawlStatusListener#crawlEnded(java.lang.String)
     */
    public void crawlEnded(String sExitMessage) {
        // Ok, if the CrawlController is exiting we delete our reference to it to facilitate gc.
        controller = null;
    }

    /* (non-Javadoc)
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
                    u = UURI.createUURI(read.substring(3));
                    alreadyIncluded.add(u);
                    discoveredCount++;
                } catch (URISyntaxException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        reader.close();
        // scan log for all 'F+' lines: if not alreadyIncluded, schedule for visitation
        reader = new BufferedReader(new FileReader(pathToLog));
        while((read = reader.readLine()) != null) {
            if(read.startsWith(F_ADD)) {
                UURI u;
                try {
                    u = UURI.createUURI(read.substring(3));
                    if(!alreadyIncluded.contains(u)) {
                        CandidateURI caUri = new CandidateURI(u);
                        caUri.setVia(pathToLog);
                        caUri.setPathFromSeed("L"); // TODO: reevaluate if this is correct
                        schedule(caUri);
                    }
                } catch (URISyntaxException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        reader.close();

    }
}
