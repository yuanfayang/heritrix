/* $Id$
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

import static org.archive.crawler.datamodel.CoreAttributeConstants.A_FORCE_RETIRE;
import static org.archive.modules.fetcher.FetchStatusCodes.S_DEFERRED;
import static org.archive.modules.fetcher.FetchStatusCodes.S_RUNTIME_EXCEPTION;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.SortedSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.collections.Bag;
import org.apache.commons.collections.BagUtils;
import org.apache.commons.collections.bag.HashBag;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.UriUniqFilter;
import org.archive.crawler.datamodel.UriUniqFilter.HasUriReceiver;
import org.archive.crawler.framework.exceptions.EndedException;
import org.archive.net.UURI;
import org.archive.state.Expert;
import org.archive.state.Global;
import org.archive.state.Immutable;
import org.archive.state.Key;
import org.archive.state.StateProvider;
import org.archive.util.ArchiveUtils;

import com.sleepycat.je.DatabaseException;

/**
 * A common Frontier base using several queues to hold pending URIs. 
 * 
 * Uses in-memory map of all known 'queues' inside a single database.
 * Round-robins between all queues.
 *
 * @author Gordon Mohr
 * @author Christian Kohlschuetter
 */
public abstract class WorkQueueFrontier extends AbstractFrontier
implements Closeable, HasUriReceiver, Serializable {
	private static final long serialVersionUID = 570384305871965843L;
	
    public class WakeTask extends TimerTask {
        @Override
        public void run() {
            synchronized(snoozedClassQueues) {
                if(this!=nextWake) {
                    // an intervening waketask was made
                    return;
                }
                wakeQueues();
            }
        }
    }

    /** truncate reporting of queues at some large but not unbounded number */
    private static final int REPORT_MAX_QUEUES = 2000;
    
    /**
     * If we know that only a small amount of queues is held in memory,
     * we can avoid using a disk-based BigMap.
     * This only works efficiently if the WorkQueue does not hold its
     * entries in memory as well.
     */ 
    private static final int MAX_QUEUES_TO_HOLD_ALLQUEUES_IN_MEMORY = 3000;

    /**
     * When a snooze target for a queue is longer than this amount, and 
     * there are already ready queues, deactivate rather than snooze 
     * the current queue -- so other more responsive sites get a chance
     * in active rotation. (As a result, queue's next try may be much
     * further in the future than the snooze target delay.)
     */
    @Immutable @Expert
    final public static Key<Long> SNOOZE_DEACTIVATE_MS = 
        Key.make(5L*60L*1000L);
    
    
    private static final Logger logger =
        Logger.getLogger(WorkQueueFrontier.class.getName());
    
    /** whether to hold queues INACTIVE until needed for throughput */
    final public static Key<Boolean> HOLD_QUEUES = Key.make(true);

    /** amount to replenish budget on each activation (duty cycle) */
    @Immutable @Expert
    final public static Key<Integer> BALANCE_REPLENISH_AMOUNT = 
        Key.make(3000);
    
    /** whether to hold queues INACTIVE until needed for throughput */
    @Immutable @Expert
    final public static Key<Integer> ERROR_PENALTY_AMOUNT = 
        Key.make(100);

    /** total expenditure to allow a queue before 'retiring' it  */
    final public static Key<Long> QUEUE_TOTAL_BUDGET = Key.make(-1L);

    /** cost assignment policy to use. */
    @Expert @Global
    final public static Key<CostAssignmentPolicy> COST_POLICY = 
        Key.make(CostAssignmentPolicy.class, new UnitCostAssignmentPolicy());

    /** target size of ready queues backlog */
    @Immutable @Expert
    final public static Key<Integer> TARGET_READY_BACKLOG = 
        Key.make(50);
    
    /** those UURIs which are already in-process (or processed), and
     thus should not be rescheduled */
    protected UriUniqFilter alreadyIncluded;

    /** All known queues.
     */
    protected transient Map<String,WorkQueue> allQueues = null; 
    // of classKey -> ClassKeyQueue

    /**
     * All per-class queues whose first item may be handed out.
     * Linked-list of keys for the queues.
     */
    protected BlockingQueue<String> readyClassQueues;
    
    /** Target (minimum) size to keep readyClassQueues */
    protected int targetSizeForReadyQueues;
    
    /** 
     * All 'inactive' queues, not yet in active rotation.
     * Linked-list of keys for the queues.
     */
    protected Queue<String> inactiveQueues;

    /**
     * 'retired' queues, no longer considered for activation.
     * Linked-list of keys for queues.
     */
    protected Queue<String> retiredQueues;
    
    /** all per-class queues from whom a URI is outstanding */
    protected Bag inProcessQueues = 
        BagUtils.synchronizedBag(new HashBag()); // of ClassKeyQueue
    
    /**
     * All per-class queues held in snoozed state, sorted by wake time.
     */
    protected SortedSet<WorkQueue> snoozedClassQueues;
    
    /** Timer for tasks which wake head item of snoozedClassQueues */
    protected transient Timer wakeTimer;
    
    /** Task for next wake */ 
    protected transient WakeTask nextWake; 
    
    protected WorkQueue longestActiveQueue = null;
    
    /** how long to wait for a ready queue when there's nothing snoozed */
    private static final long DEFAULT_WAIT = 1000; // 1 second
    

    
    public <T> T get(Key<T> key) {
        return global.get(this, key);
    }

    
    /**
     * The UriUniqFilter to use.
     */
    @Immutable
    public final static Key<UriUniqFilter> URI_UNIQ_FILTER =
        Key.make(UriUniqFilter.class, null);
    
    
    /**
     * Constructor.
     */
    public WorkQueueFrontier() {
        super();
    }
    
    
    public void initialTasks(StateProvider provider) {
        super.initialTasks(provider);
        this.alreadyIncluded = provider.get(this, URI_UNIQ_FILTER);
        alreadyIncluded.setDestination(this);
        
        this.targetSizeForReadyQueues = provider.get(this, TARGET_READY_BACKLOG);
        if (this.targetSizeForReadyQueues < 1) {
            this.targetSizeForReadyQueues = 1;
        }
        this.wakeTimer = new Timer("waker for " + controller.toString());
        
        try {
            initAllQueues(false);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        
        loadSeeds();
    }
    
    
    private void initAllQueues(boolean recycle) 
    throws IOException, DatabaseException {
        if (workQueueDataOnDisk()
                && get(QUEUE_ASSIGNMENT_POLICY).maximumNumberOfKeys() >= 0
                && get(QUEUE_ASSIGNMENT_POLICY).maximumNumberOfKeys() <= 
                    MAX_QUEUES_TO_HOLD_ALLQUEUES_IN_MEMORY) {
            this.allQueues = Collections.synchronizedMap(
                    new HashMap<String,WorkQueue>());
        } else {
            this.initAllQueues();
//            this.allQueues = controller.getBigMap("allqueues",
//                    String.class, WorkQueue.class);
//            if (logger.isLoggable(Level.FINE)) {
//                Iterator i = this.allQueues.keySet().iterator();
//                try {
//                    for (; i.hasNext();) {
//                        logger.fine((String) i.next());
//                    }
//                } finally {
//                    StoredIterator.close(i);
//                }
//            }
        }
//        this.alreadyIncluded = createAlreadyIncluded();
        initQueue(recycle);
        
    }

    
    protected abstract void initAllQueues() throws DatabaseException;

    /* (non-Javadoc)
     * @see org.archive.crawler.frontier.AbstractFrontier#crawlEnded(java.lang.String)
     */
    public void close() {
        // Cleanup.  CrawlJobs persist after crawl has finished so undo any
        // references.
        if (this.alreadyIncluded != null) {
            this.alreadyIncluded.close();
//            this.alreadyIncluded = null;
        }

//        this.queueAssignmentPolicy = null;
        
        try {
            closeQueue();
        } catch (IOException e) {
            // FIXME exception handling
            e.printStackTrace();
        }
        this.wakeTimer.cancel();
        
        this.allQueues.clear();
        
        
//        this.allQueues = null;
//        this.inProcessQueues = null;
//        this.readyClassQueues = null;
//        this.snoozedClassQueues = null;
//        this.inactiveQueues = null;
//        this.retiredQueues = null;
//        
//        this.costAssignmentPolicy = null;
        
        // Clearing controller is a problem. We get NPEs in #preNext.
//        super.crawlEnded(sExitMessage);
        //this.controller = null;
    }


    /**
     * Arrange for the given CrawlURI to be visited, if it is not
     * already scheduled/completed.
     *
     * @see org.archive.crawler.framework.Frontier#schedule(org.archive.crawler.datamodel.CrawlURI)
     */
    public void schedule(CrawlURI caUri) {
        // Canonicalization may set forceFetch flag.  See
        // #canonicalization(CrawlURI) javadoc for circumstance.
        String canon = canonicalize(caUri);
        if (caUri.forceFetch()) {
            alreadyIncluded.addForce(canon, caUri);
        } else {
            alreadyIncluded.add(canon, caUri);
        }
    }

    /**
     * Accept the given CrawlURI for scheduling, as it has
     * passed the alreadyIncluded filter. 
     * 
     * Choose a per-classKey queue and enqueue it. If this
     * item has made an unready queue ready, place that 
     * queue on the readyClassQueues queue. 
     * @param caUri CrawlURI.
     */
    public void receive(CrawlURI caUri) {
        CrawlURI curi = asCrawlUri(caUri);
        applySpecialHandling(curi);
        sendToQueue(curi);
        // Update recovery log.
        doJournalAdded(curi);
    }

	/* (non-Javadoc)
	 * @see org.archive.crawler.frontier.AbstractFrontier#asCrawlUri(org.archive.crawler.datamodel.CrawlURI)
	 */
	protected CrawlURI asCrawlUri(CrawlURI caUri) {
		CrawlURI curi = super.asCrawlUri(caUri);
		// force cost to be calculated, pre-insert
		getCost(curi);
		return curi;
	}
	
    /**
     * Send a CrawlURI to the appropriate subqueue.
     * 
     * @param curi
     */
    protected void sendToQueue(CrawlURI curi) {
        WorkQueue wq = getQueueFor(curi);
        synchronized (wq) {
            wq.enqueue(this, curi);
            if(!wq.isRetired()) {
                incrementQueuedUriCount();
            }
            if(!wq.isHeld()) {
                wq.setHeld();
                if(holdQueues() && readyClassQueues.size()>=targetSizeForReadyQueues()) {
                    deactivateQueue(wq);
                } else {
                    replenishSessionBalance(wq);
                    readyQueue(wq);
                }
            }
            WorkQueue laq = longestActiveQueue;
            if(!wq.isRetired()&&((laq==null) || wq.getCount() > laq.getCount())) {
                longestActiveQueue = wq; 
            }
        }
    }

    /**
     * Whether queues should start inactive (only becoming active when needed
     * to keep the crawler busy), or if queues should start out ready.
     * 
     * @return true if new queues should held inactive
     */
    private boolean holdQueues() {
        return get(HOLD_QUEUES);
    }

    /**
     * Put the given queue on the readyClassQueues queue
     * @param wq
     */
    private void readyQueue(WorkQueue wq) {
        try {
            wq.setActive(this, true);
            readyClassQueues.put(wq.getClassKey());
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.err.println("unable to ready queue "+wq);
            // propagate interrupt up 
            throw new RuntimeException(e);
        }
    }

    /**
     * Put the given queue on the inactiveQueues queue
     * @param wq
     */
    private void deactivateQueue(WorkQueue wq) {
        wq.setSessionBalance(0); // zero out session balance
        inactiveQueues.add(wq.getClassKey());
        wq.setActive(this, false);
    }
    
    /**
     * Put the given queue on the retiredQueues queue
     * @param wq
     */
    private void retireQueue(WorkQueue wq) {
        retiredQueues.add(wq.getClassKey());
        decrementQueuedCount(wq.getCount());
        wq.setRetired(true);
        wq.setActive(this, false);
    }
    
    /** 
     * Accomodate any changes in settings.
     * 
     * @see org.archive.crawler.framework.Frontier#kickUpdate()
     */
    public void kickUpdate() {
        super.kickUpdate();
        int target = get(TARGET_READY_BACKLOG);
        if (target < 1) {
            target = 1;
        }
        this.targetSizeForReadyQueues = target; 

        // TODO: See about caching costPolicy again
        //        try {
//            initCostPolicy();
//        } catch (FatalConfigurationException fce) {
//            throw new RuntimeException(fce);
//        }

        // The rules for a 'retired' queue may have changed; so,
        // unretire all queues to 'inactive'. If they still qualify
        // as retired/overbudget next time they come up, they'll
        // be re-retired; if not, they'll get a chance to become
        // active under the new rules.
        Object key = this.retiredQueues.poll();
        while (key != null) {
            WorkQueue q = (WorkQueue)this.allQueues.get(key);
            if(q != null) {
                unretireQueue(q);
            }
            key = this.retiredQueues.poll();
        }
    }
    /**
     * Restore a retired queue to the 'inactive' state. 
     * 
     * @param q
     */
    private void unretireQueue(WorkQueue q) {
        deactivateQueue(q);
        q.setRetired(false); 
        incrementQueuedUriCount(q.getCount());
    }

    /**
     * Return the work queue for the given CrawlURI's classKey. URIs
     * are ordered and politeness-delayed within their 'class'.
     * If the requested queue is not found, a new instance is created.
     * 
     * @param curi CrawlURI to base queue on
     * @return the found or created ClassKeyQueue
     */
    protected abstract WorkQueue getQueueFor(CrawlURI curi);

    /**
     * Return the work queue for the given classKey, or null
     * if no such queue exists.
     * 
     * @param classKey key to look for
     * @return the found WorkQueue
     */
    protected abstract WorkQueue getQueueFor(String classKey);
    
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
    public CrawlURI next()
    throws InterruptedException, EndedException {
        while (true) { // loop left only by explicit return or exception
            long now = System.currentTimeMillis();

            //this.reportTo(new java.io.PrintWriter(new java.io.OutputStreamWriter(System.out)));
            
            // Do common checks for pause, terminate, bandwidth-hold
            preNext(now);
            
            synchronized(readyClassQueues) {
                int activationsNeeded = targetSizeForReadyQueues() - readyClassQueues.size();
                while(activationsNeeded > 0 && !inactiveQueues.isEmpty()) {
                    activateInactiveQueue();
                    activationsNeeded--;
                }
            }
                   
            WorkQueue readyQ = null;
            Object key = readyClassQueues.poll(DEFAULT_WAIT,TimeUnit.MILLISECONDS);
            if (key != null) {
                readyQ = (WorkQueue)this.allQueues.get(key);
            }
            if (readyQ != null) {
                while(true) { // loop left by explicit return or break on empty
                    CrawlURI curi = null;
                    synchronized(readyQ) {
                        curi = readyQ.peek(this);                     
                        if (curi != null) {
                            curi.setStateProvider(manager);
                            
                            // check if curi belongs in different queue
                            String currentQueueKey = getClassKey(curi);
                            if (currentQueueKey.equals(curi.getClassKey())) {
                                // curi was in right queue, emit
                                noteAboutToEmit(curi, readyQ);
                                inProcessQueues.add(readyQ);
                                return curi;
                            }
                            // URI's assigned queue has changed since it
                            // was queued (eg because its IP has become
                            // known). Requeue to new queue.
                            curi.setClassKey(currentQueueKey);
                            readyQ.dequeue(this);
                            decrementQueuedCount(1);
                            curi.setHolderKey(null);
                            // curi will be requeued to true queue after lock
                            //  on readyQ is released, to prevent deadlock
                        } else {
                            // readyQ is empty and ready: it's exhausted
                            // release held status, allowing any subsequent 
                            // enqueues to again put queue in ready
                            readyQ.clearHeld();
                            break;
                        }
                    }
                    if(curi!=null) {
                        // complete the requeuing begun earlier
                        sendToQueue(curi);
                    }
                }
            } else {
                // ReadyQ key wasn't in all queues: unexpected
                if (key != null) {
                    logger.severe("Key "+ key +
                        " in readyClassQueues but not allQueues");
                }
            }

            if(shouldTerminate) {
                // skip subsequent steps if already on last legs
                throw new EndedException("shouldTerminate is true");
            }
                
            if(inProcessQueues.size()==0) {
                // Nothing was ready or in progress or imminent to wake; ensure 
                // any piled-up pending-scheduled URIs are considered
                this.alreadyIncluded.requestFlush();
            }    
        }
    }

    private int targetSizeForReadyQueues() {
        return targetSizeForReadyQueues;
    }

    /**
     * Return the 'cost' of a CrawlURI (how much of its associated
     * queue's budget it depletes upon attempted processing)
     * 
     * @param curi
     * @return the associated cost
     */
    private int getCost(CrawlURI curi) {
        int cost = curi.getHolderCost();
        if (cost == CrawlURI.UNCALCULATED) {
            cost = curi.get(this,COST_POLICY).costOf(curi);
            curi.setHolderCost(cost);
        }
        return cost;
    }
    
    /**
     * Activate an inactive queue, if any are available. 
     */
    private void activateInactiveQueue() {
        Object key = this.inactiveQueues.poll();
        if (key == null) {
            return;
        }
        WorkQueue candidateQ = (WorkQueue)this.allQueues.get(key);
        if(candidateQ != null) {
            synchronized(candidateQ) {
                replenishSessionBalance(candidateQ);
                if(candidateQ.isOverBudget()){
                    // if still over-budget after an activation & replenishing,
                    // retire
                    retireQueue(candidateQ);
                    return;
                } 
                long now = System.currentTimeMillis();
                long delay_ms = candidateQ.getWakeTime() - now;
                if(delay_ms>0) {
                    // queue still due for snoozing
                    snoozeQueue(candidateQ,now,delay_ms);
                    return;
                }
                candidateQ.setWakeTime(0); // clear obsolete wake time, if any
                readyQueue(candidateQ);
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("ACTIVATED queue: " +
                        candidateQ.getClassKey());
                   
                }
            }
        }
    }

    /**
     * Replenish the budget of the given queue by the appropriate amount.
     * 
     * @param queue queue to replenish
     */
    private void replenishSessionBalance(WorkQueue queue) {
        // get a CrawlURI for override context purposes
        CrawlURI contextUri = queue.peek(this); 
        // TODO: consider confusing cross-effects of this and IP-based politeness
        StateProvider p = contextUri.getStateProvider();
        if (p == null) {
            contextUri.setStateProvider(manager);
        }
        queue.setSessionBalance(contextUri.get(this, BALANCE_REPLENISH_AMOUNT));
        // reset total budget (it may have changed)
        // TODO: is this the best way to be sensitive to potential mid-crawl changes
        long totalBudget = contextUri.get(this, QUEUE_TOTAL_BUDGET);
        queue.setTotalBudget(totalBudget);
        queue.unpeek(); // don't insist on that URI being next released
    }

    /**
     * Enqueue the given queue to either readyClassQueues or inactiveQueues,
     * as appropriate.
     * 
     * @param wq
     */
    private void reenqueueQueue(WorkQueue wq) {
        if(wq.isOverBudget()) {
            // if still over budget, deactivate
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("DEACTIVATED queue: " +
                    wq.getClassKey());
            }
            deactivateQueue(wq);
        } else {
            readyQueue(wq);
        }
    }
    
    /**
     * Wake any queues sitting in the snoozed queue whose time has come.
     */
    void wakeQueues() {
        synchronized (snoozedClassQueues) {
            long now = System.currentTimeMillis();
            long nextWakeDelay = 0;
            int wokenQueuesCount = 0;
            while (true) {
                if (snoozedClassQueues.isEmpty()) {
                    return;
                }
                WorkQueue peek = (WorkQueue) snoozedClassQueues.first();
                nextWakeDelay = peek.getWakeTime() - now;
                if (nextWakeDelay <= 0) {
                    snoozedClassQueues.remove(peek);
                    peek.setWakeTime(0);
                    reenqueueQueue(peek);
                    wokenQueuesCount++;
                } else {
                    break;
                }
            }
            this.nextWake = new WakeTask();
            this.wakeTimer.schedule(nextWake,nextWakeDelay);
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
        WorkQueue wq = (WorkQueue) curi.getHolder();
        assert (wq.peek(this) == curi) : "unexpected peek " + wq;
        inProcessQueues.remove(wq, 1);

        if(includesRetireDirective(curi)) {
            // CrawlURI is marked to trigger retirement of its queue
            curi.processingCleanup();
            wq.unpeek();
            wq.update(this, curi); // rewrite any changes
            retireQueue(wq);
            return;
        }
        
        if (needsRetrying(curi)) {
            // Consider errors which can be retried, leaving uri atop queue
            if(curi.getFetchStatus()!=S_DEFERRED) {
                wq.expend(getCost(curi)); // all retries but DEFERRED cost
            }
            long delay_sec = retryDelayFor(curi);
            curi.processingCleanup(); // lose state that shouldn't burden retry
            synchronized(wq) {
                wq.unpeek();
                // TODO: consider if this should happen automatically inside unpeek()
                wq.update(this, curi); // rewrite any changes
                if (delay_sec > 0) {
                    long delay_ms = delay_sec * 1000;
                    snoozeQueue(wq, now, delay_ms);
                } else {
                    reenqueueQueue(wq);
                }
            }
            // Let everyone interested know that it will be retried.
            controller.fireCrawledURINeedRetryEvent(curi);
            doJournalRescheduled(curi);
            return;
        }

        // Curi will definitely be disposed of without retry, so remove from queue
        wq.dequeue(this);
        decrementQueuedCount(1);
        log(curi);

        if (curi.isSuccess()) {
            totalProcessedBytes += curi.getRecordedSize();
            incrementSucceededFetchCount();
            // Let everyone know in case they want to do something before we strip the curi.
            controller.fireCrawledURISuccessfulEvent(curi);
            doJournalFinishedSuccess(curi);
            wq.expend(getCost(curi)); // successes cost
        } else if (isDisregarded(curi)) {
            // Check for codes that mean that while we the crawler did
            // manage to schedule it, it must be disregarded for some reason.
            incrementDisregardedUriCount();
            // Let interested listeners know of disregard disposition.
            controller.fireCrawledURIDisregardEvent(curi);
            // if exception, also send to crawlErrors
            if (curi.getFetchStatus() == S_RUNTIME_EXCEPTION) {
                Object[] array = { curi };
                loggerModule.getRuntimeErrors().log(Level.WARNING, curi.getUURI()
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
                this.loggerModule.getRuntimeErrors().log(Level.WARNING, curi.getUURI()
                        .toString(), array);
            }
            incrementFailedFetchCount();
            // let queue note error
            setStateProvider(curi);
            wq.noteError(curi.get(this, ERROR_PENALTY_AMOUNT));
            doJournalFinishedFailure(curi);
            wq.expend(getCost(curi)); // failures cost
        }

        long delay_ms = politenessDelayFor(curi);
        synchronized(wq) {
            if (delay_ms > 0) {
                snoozeQueue(wq,now,delay_ms);
            } else {
                reenqueueQueue(wq);
            }
        }

        curi.stripToMinimal();
        curi.processingCleanup();

    }

    private boolean includesRetireDirective(CrawlURI curi) {
        return curi.containsDataKey(A_FORCE_RETIRE) 
         && (Boolean)curi.getData().get(A_FORCE_RETIRE);
    }

    /**
     * Place the given queue into 'snoozed' state, ineligible to
     * supply any URIs for crawling, for the given amount of time. 
     * 
     * @param wq queue to snooze 
     * @param now time now in ms 
     * @param delay_ms time to snooze in ms
     */
    private void snoozeQueue(WorkQueue wq, long now, long delay_ms) {
        long nextTime = now + delay_ms;
        wq.setWakeTime(nextTime);
        long snoozeToInactiveDelayMs = get(SNOOZE_DEACTIVATE_MS);
        if (delay_ms > snoozeToInactiveDelayMs && !inactiveQueues.isEmpty()) {
            deactivateQueue(wq);
        } else {
            synchronized(snoozedClassQueues) {
                snoozedClassQueues.add(wq);
                if(wq == snoozedClassQueues.first()) {
                    this.nextWake = new WakeTask();
                    this.wakeTimer.schedule(nextWake, delay_ms);
                }
            }
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
        logger.finer("Forgetting " + curi);
        alreadyIncluded.forget(canonicalize(curi.getUURI()), curi);
    }

    /**  (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#discoveredUriCount()
     */
    public long discoveredUriCount() {
        return (this.alreadyIncluded != null)? this.alreadyIncluded.count(): 0;
    }

    /**
     * @param match String to  match.
     * @return Number of items deleted.
     */
    public long deleteURIs(String match) {
        long count = 0;
        // TODO: DANGER/ values() may not work right from CachedBdbMap
        Iterator iter = allQueues.keySet().iterator(); 
        while(iter.hasNext()) {
            WorkQueue wq = getQueueFor(((String)iter.next()));
            wq.unpeek();
            count += wq.deleteMatching(this, match);
        }
        decrementQueuedCount(count);
        return count;
    }

    //
    // Reporter implementation
    //
    
    public static String STANDARD_REPORT = "standard";
    public static String ALL_NONEMPTY = "nonempty";
    public static String ALL_QUEUES = "all";
    protected static String[] REPORTS = {STANDARD_REPORT,ALL_NONEMPTY,ALL_QUEUES};
    
    public String[] getReports() {
        return REPORTS;
    }
    
    /**
     * @param w Where to write to.
     */
    public void singleLineReportTo(PrintWriter w) {
        if (this.allQueues == null) {
            return;
        }
        int allCount = allQueues.size();
        int inProcessCount = inProcessQueues.uniqueSet().size();
        int readyCount = readyClassQueues.size();
        int snoozedCount = snoozedClassQueues.size();
        int activeCount = inProcessCount + readyCount + snoozedCount;
        int inactiveCount = inactiveQueues.size();
        int retiredCount = retiredQueues.size();
        int exhaustedCount = 
            allCount - activeCount - inactiveCount - retiredCount;
        w.print(allCount);
        w.print(" queues: ");
        w.print(activeCount);
        w.print(" active (");
        w.print(inProcessCount);
        w.print(" in-process; ");
        w.print(readyCount);
        w.print(" ready; ");
        w.print(snoozedCount);
        w.print(" snoozed); ");
        w.print(inactiveCount);
        w.print(" inactive; ");
        w.print(retiredCount);
        w.print(" retired; ");
        w.print(exhaustedCount);
        w.print(" exhausted");
        w.flush();
    }
    
    /* (non-Javadoc)
     * @see org.archive.util.Reporter#singleLineLegend()
     */
    public String singleLineLegend() {
        return "total active in-process ready snoozed inactive retired exhausted";
    }

    /**
     * This method compiles a human readable report on the status of the frontier
     * at the time of the call.
     * @param name Name of report.
     * @param writer Where to write to.
     */
    public synchronized void reportTo(String name, PrintWriter writer) {
        if(ALL_NONEMPTY.equals(name)) {
            allNonemptyReportTo(writer);
            return;
        }
        if(ALL_QUEUES.equals(name)) {
            allQueuesReportTo(writer);
            return;
        }
        if(name!=null && !STANDARD_REPORT.equals(name)) {
            writer.print(name);
            writer.print(" unavailable; standard report:\n");
        }
        standardReportTo(writer);
    }   
    
    /** Compact report of all nonempty queues (one queue per line)
     * 
     * @param writer
     */
    private void allNonemptyReportTo(PrintWriter writer) {
        ArrayList<WorkQueue> inProcessQueuesCopy;
        synchronized(this.inProcessQueues) {
            // grab a copy that will be stable against mods for report duration 
            @SuppressWarnings("unchecked")
            Collection<WorkQueue> inProcess = this.inProcessQueues;
            inProcessQueuesCopy = new ArrayList<WorkQueue>(inProcess);
        }
        writer.print("\n -----===== IN-PROCESS QUEUES =====-----\n");
        queueSingleLinesTo(writer, inProcessQueuesCopy.iterator());

        writer.print("\n -----===== READY QUEUES =====-----\n");
        queueSingleLinesTo(writer, this.readyClassQueues.iterator());

        writer.print("\n -----===== SNOOZED QUEUES =====-----\n");
        queueSingleLinesTo(writer, this.snoozedClassQueues.iterator());
        
        writer.print("\n -----===== INACTIVE QUEUES =====-----\n");
        queueSingleLinesTo(writer, this.inactiveQueues.iterator());
        
        writer.print("\n -----===== RETIRED QUEUES =====-----\n");
        queueSingleLinesTo(writer, this.retiredQueues.iterator());
    }

    /** Compact report of all nonempty queues (one queue per line)
     * 
     * @param writer
     */
    private void allQueuesReportTo(PrintWriter writer) {
        queueSingleLinesTo(writer, allQueues.keySet().iterator());
    }
    
    /**
     * Writer the single-line reports of all queues in the
     * iterator to the writer 
     * 
     * @param writer to receive report
     * @param iterator over queues of interest.
     */
    private void queueSingleLinesTo(PrintWriter writer, Iterator iterator) {
        Object obj;
        WorkQueue q;
        boolean legendWritten = false;
        while( iterator.hasNext()) {
            obj = iterator.next();
            if (obj ==  null) {
                continue;
            }
            q = (obj instanceof WorkQueue)?
                (WorkQueue)obj:
                (WorkQueue)this.allQueues.get(obj);
            if(q == null) {
                writer.print(" ERROR: "+obj);
            }
            if(!legendWritten) {
                writer.println(q.singleLineLegend());
                legendWritten = true;
            }
            q.singleLineReportTo(writer);
        }       
    }

    /**
     * @param w Writer to print to.
     */
    private void standardReportTo(PrintWriter w) {
        int allCount = allQueues.size();
        int inProcessCount = inProcessQueues.uniqueSet().size();
        int readyCount = readyClassQueues.size();
        int snoozedCount = snoozedClassQueues.size();
        int activeCount = inProcessCount + readyCount + snoozedCount;
        int inactiveCount = inactiveQueues.size();
        int retiredCount = retiredQueues.size();
        int exhaustedCount = 
            allCount - activeCount - inactiveCount - retiredCount;

        w.print("Frontier report - ");
        w.print(ArchiveUtils.get12DigitDate());
        w.print("\n");
        w.print(" Job being crawled: ");
        w.print(controller.getSheetManager().getCrawlName());
        w.print("\n");
        w.print("\n -----===== STATS =====-----\n");
        w.print(" Discovered:    ");
        w.print(Long.toString(discoveredUriCount()));
        w.print("\n");
        w.print(" Queued:        ");
        w.print(Long.toString(queuedUriCount()));
        w.print("\n");
        w.print(" Finished:      ");
        w.print(Long.toString(finishedUriCount()));
        w.print("\n");
        w.print("  Successfully: ");
        w.print(Long.toString(succeededFetchCount()));
        w.print("\n");
        w.print("  Failed:       ");
        w.print(Long.toString(failedFetchCount()));
        w.print("\n");
        w.print("  Disregarded:  ");
        w.print(Long.toString(disregardedUriCount()));
        w.print("\n");
        w.print("\n -----===== QUEUES =====-----\n");
        w.print(" Already included size:     ");
        w.print(Long.toString(alreadyIncluded.count()));
        w.print("\n");
        w.print("               pending:     ");
        w.print(Long.toString(alreadyIncluded.pending()));
        w.print("\n");
        w.print("\n All class queues map size: ");
        w.print(Long.toString(allCount));
        w.print("\n");
        w.print( "             Active queues: ");
        w.print(activeCount);
        w.print("\n");
        w.print("                    In-process: ");
        w.print(inProcessCount);
        w.print("\n");
        w.print("                         Ready: ");
        w.print(readyCount);
        w.print("\n");
        w.print("                       Snoozed: ");
        w.print(snoozedCount);
        w.print("\n");
        w.print("           Inactive queues: ");
        w.print(inactiveCount);
        w.print("\n");
        w.print("            Retired queues: ");
        w.print(retiredCount);
        w.print("\n");
        w.print("          Exhausted queues: ");
        w.print(exhaustedCount);
        w.print("\n");
        
        w.print("\n -----===== IN-PROCESS QUEUES =====-----\n");
        @SuppressWarnings("unchecked")
        Collection<WorkQueue> inProcess = inProcessQueues;
        ArrayList<WorkQueue> copy = extractSome(inProcess, REPORT_MAX_QUEUES);
        appendQueueReports(w, copy.iterator(), copy.size(), REPORT_MAX_QUEUES);
        
        w.print("\n -----===== READY QUEUES =====-----\n");
        appendQueueReports(w, this.readyClassQueues.iterator(),
            this.readyClassQueues.size(), REPORT_MAX_QUEUES);

        w.print("\n -----===== SNOOZED QUEUES =====-----\n");
        copy = extractSome(snoozedClassQueues, REPORT_MAX_QUEUES);
        appendQueueReports(w, copy.iterator(), copy.size(), REPORT_MAX_QUEUES);
        
        WorkQueue longest = longestActiveQueue;
        if (longest != null) {
            w.print("\n -----===== LONGEST QUEUE =====-----\n");
            longest.reportTo(w);
        }

        w.print("\n -----===== INACTIVE QUEUES =====-----\n");
        appendQueueReports(w, this.inactiveQueues.iterator(),
            this.inactiveQueues.size(), REPORT_MAX_QUEUES);
        
        w.print("\n -----===== RETIRED QUEUES =====-----\n");
        appendQueueReports(w, this.retiredQueues.iterator(),
            this.retiredQueues.size(), REPORT_MAX_QUEUES);

        w.flush();
    }
    
    
    /**
     * Extract some of the elements in the given collection to an
     * ArrayList.  This method synchronizes on the given collection's
     * monitor.  The returned list will never contain more than the
     * specified maximum number of elements.
     * 
     * @param c    the collection whose elements to extract
     * @param max  the maximum number of elements to extract
     * @return  the extraction
     */
    private static <T> ArrayList<T> extractSome(Collection<T> c, int max) {
        // Try to guess a sane initial capacity for ArrayList
        // Hopefully given collection won't grow more than 10 items
        // between now and the synchronized block...
        int initial = Math.min(c.size() + 10, max);
        int count = 0;
        ArrayList<T> list = new ArrayList<T>(initial);
        synchronized (c) {
            Iterator<T> iter = c.iterator();
            while (iter.hasNext() && (count < max)) {
                list.add(iter.next());
                count++;
            }
        }
        return list;
    }

    /**
     * Append queue report to general Frontier report.
     * @param w StringBuffer to append to.
     * @param iterator An iterator over 
     * @param total
     * @param max
     */
    protected void appendQueueReports(PrintWriter w, Iterator iterator,
            int total, int max) {
        Object obj;
        WorkQueue q;
        for(int count = 0; iterator.hasNext() && (count < max); count++) {
            obj = iterator.next();
            if (obj ==  null) {
                continue;
            }
            q = (obj instanceof WorkQueue)?
                (WorkQueue)obj:
                (WorkQueue)this.allQueues.get(obj);
            if(q == null) {
                w.print("WARNING: No report for queue "+obj);
            }
            q.reportTo(w);
        }
        if(total > max) {
            w.print("...and " + (total - max) + " more.\n");
        }
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

    public void considerIncluded(UURI u) {
        this.alreadyIncluded.note(canonicalize(u));
        CrawlURI temp = new CrawlURI(u);
        temp.setClassKey(getClassKey(temp));
        getQueueFor(temp).expend(getCost(temp));
    }
    
    protected abstract void initQueue(boolean recycle) throws IOException;
    protected abstract void closeQueue() throws IOException;
    
    /**
     * Returns <code>true</code> if the WorkQueue implementation of this
     * Frontier stores its workload on disk instead of relying
     * on serialization mechanisms.
     * 
     * @return a constant boolean value for this class/instance
     */
    protected abstract boolean workQueueDataOnDisk();
    
    
    public FrontierGroup getGroup(CrawlURI curi) {
        return getQueueFor(curi);
    }
    
    
    public long averageDepth() {
        int inProcessCount = inProcessQueues.uniqueSet().size();
        int readyCount = readyClassQueues.size();
        int snoozedCount = snoozedClassQueues.size();
        int activeCount = inProcessCount + readyCount + snoozedCount;
        int inactiveCount = inactiveQueues.size();
        int totalQueueCount = (activeCount+inactiveCount);
        return (totalQueueCount == 0) ? 0 : queuedUriCount.get() / totalQueueCount;
    }
    public float congestionRatio() {
        int inProcessCount = inProcessQueues.uniqueSet().size();
        int readyCount = readyClassQueues.size();
        int snoozedCount = snoozedClassQueues.size();
        int activeCount = inProcessCount + readyCount + snoozedCount;
        int inactiveCount = inactiveQueues.size();
        return (float)(activeCount + inactiveCount) / (inProcessCount + snoozedCount);
    }
    public long deepestUri() {
        return longestActiveQueue==null ? -1 : longestActiveQueue.getCount();
    }
    
    
    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#isEmpty()
     */
    public synchronized boolean isEmpty() {
        return queuedUriCount.get() == 0 && alreadyIncluded.pending() == 0;
    }

    
    
    private void readObject(ObjectInputStream inp)
    throws IOException, ClassNotFoundException {
        inp.defaultReadObject();
        this.wakeTimer = new Timer("waker for " + controller.toString());
        try {
            initAllQueues(true);
        } catch (DatabaseException e) {
            IOException io = new IOException();
            io.initCause(e);
            throw io;
        }
    }


    private void writeObject(ObjectOutputStream out) 
    throws IOException {
        out.defaultWriteObject();
    }

}

