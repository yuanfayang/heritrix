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
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.SortedMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.apache.commons.collections.Bag;
import org.apache.commons.collections.BagUtils;
import org.apache.commons.collections.bag.HashBag;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.UriUniqFilter;
import org.archive.crawler.datamodel.UriUniqFilter.CrawlUriReceiver;
import org.archive.crawler.frontier.precedence.BaseQueuePrecedencePolicy;
import org.archive.crawler.frontier.precedence.CostUriPrecedencePolicy;
import org.archive.crawler.frontier.precedence.QueuePrecedencePolicy;
import org.archive.crawler.frontier.precedence.UriPrecedencePolicy;
import org.archive.net.UURI;
import org.archive.settings.KeyChangeEvent;
import org.archive.settings.KeyChangeListener;
import org.archive.state.Expert;
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
implements Closeable, CrawlUriReceiver, Serializable, KeyChangeListener {
    private static final long serialVersionUID = 570384305871965843L;

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
    @Expert
    final public static Key<Integer> BALANCE_REPLENISH_AMOUNT = 
        Key.make(3000);
    
    /** whether to hold queues INACTIVE until needed for throughput */
    @Expert
    final public static Key<Integer> ERROR_PENALTY_AMOUNT = 
        Key.make(100);

    /** total expenditure to allow a queue before 'retiring' it  */
    final public static Key<Long> QUEUE_TOTAL_BUDGET = Key.make(-1L);

    /** cost assignment policy to use. */
    @Expert
    final public static Key<CostAssignmentPolicy> COST_POLICY = 
        Key.make(CostAssignmentPolicy.class, new UnitCostAssignmentPolicy());
    
    /** queue precedence assignment policy to use. */
    @Expert
    final public static Key<QueuePrecedencePolicy> QUEUE_PRECEDENCE_POLICY = 
        Key.make(QueuePrecedencePolicy.class, new BaseQueuePrecedencePolicy());

    /** precedence rank at or below which queues are not crawled */
    @Expert @Immutable
    final public static Key<Integer> PRECEDENCE_FLOOR = 
        Key.make(255);
    
    /** URI precedence assignment policy to use. */
    @Expert
    final public static Key<UriPrecedencePolicy> URI_PRECEDENCE_POLICY = 
        Key.make(UriPrecedencePolicy.class, new CostUriPrecedencePolicy());

    /** those UURIs which are already in-process (or processed), and
     thus should not be rescheduled */
    protected UriUniqFilter alreadyIncluded;

    /** All known queues.
     */
    protected Map<String,WorkQueue> allQueues = null; 
    // of classKey -> ClassKeyQueue

    /**
     * All per-class queues whose first item may be handed out.
     * Linked-list of keys for the queues.
     */
    protected BlockingQueue<String> readyClassQueues;
    
    /** all per-class queues from whom a URI is outstanding */
    protected Bag inProcessQueues = 
        BagUtils.synchronizedBag(new HashBag()); // of ClassKeyQueue
    
    /**
     * All per-class queues held in snoozed state, sorted by wake time.
     */
    transient protected DelayQueue<WorkQueue> snoozedClassQueues;
    
    transient protected WorkQueue longestActiveQueue = null;

    protected int highestPrecedenceWaiting = Integer.MAX_VALUE;
    
    public <T> T get(Key<T> key) {
        return manager.getGlobalSheet().get(this, key);
    }

    /**
     * The UriUniqFilter to use.
     */
    @Immutable
    public final static Key<UriUniqFilter> URI_UNIQ_FILTER =
        Key.makeAuto(UriUniqFilter.class);
    
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
        
        try {
            initAllQueues(false);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        
//        loadSeeds();
    }

    /**
     * Initializes all queues.  May decide to keep all queues in memory based on
     * {@link QueueAssignmentPolicy#maximumNumberOfKeys}.  Otherwise invokes
     * {@link #initAllQueues()} to actually set up the queues.
     * 
     * Subclasses should invoke this method with recycle set to "true" in 
     * a private readObject method, to restore queues after a checkpoint.
     * 
     * @param recycle
     * @throws IOException
     * @throws DatabaseException
     */
    protected void initAllQueues(boolean recycle) 
    throws IOException, DatabaseException {
        if (workQueueDataOnDisk()
                && get(QUEUE_ASSIGNMENT_POLICY).maximumNumberOfKeys() >= 0
                && get(QUEUE_ASSIGNMENT_POLICY).maximumNumberOfKeys() <= 
                    MAX_QUEUES_TO_HOLD_ALLQUEUES_IN_MEMORY) {
            this.allQueues = Collections.synchronizedMap(
                    new HashMap<String,WorkQueue>());
        } else {
            this.initAllQueues();
        }
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
        
        this.allQueues.clear();
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
    protected void processScheduleAlways(CrawlURI caUri) {
        assert Thread.currentThread() == managerThread;
        
        CrawlURI curi = asCrawlUri(caUri);
        applySpecialHandling(curi);
        sendToQueue(curi);

    }
    
    /**
     * Arrange for the given CrawlURI to be visited, if it is not
     * already scheduled/completed.
     *
     * @see org.archive.crawler.framework.Frontier#schedule(org.archive.crawler.datamodel.CrawlURI)
     */
    protected void processScheduleIfUnique(CrawlURI caUri) {
        assert Thread.currentThread() == managerThread;
        
        // Canonicalization may set forceFetch flag.  See
        // #canonicalization(CrawlURI) javadoc for circumstance.
        caUri.setStateProvider(manager);
        String canon = canonicalize(caUri);
        if (caUri.forceFetch()) {
            alreadyIncluded.addForce(canon, caUri);
        } else {
            alreadyIncluded.add(canon, caUri);
        }
    }

	/* (non-Javadoc)
	 * @see org.archive.crawler.frontier.AbstractFrontier#asCrawlUri(org.archive.crawler.datamodel.CrawlURI)
	 */
	protected CrawlURI asCrawlUri(CrawlURI caUri) {
		CrawlURI curi = super.asCrawlUri(caUri);
		// force cost to be calculated, pre-insert
		getCost(curi);
        // set
        curi.get(this,URI_PRECEDENCE_POLICY)
            .uriScheduled(curi);
		return curi;
	}
	
    /**
     * Send a CrawlURI to the appropriate subqueue.
     * 
     * @param curi
     */
    protected void sendToQueue(CrawlURI curi) {
        assert Thread.currentThread() == managerThread;
        
        WorkQueue wq = getQueueFor(curi);
        int originalPrecedence = wq.getPrecedence();
        
        wq.enqueue(this, curi);
        // Update recovery log.
        doJournalAdded(curi);
        
        if(wq.isRetired()) {
            return; 
        }
        incrementQueuedUriCount();
        if(wq.isHeld()) {
            if(wq.isActive()) {
                // queue active -- promote will be handled ok by normal cycling
                // do nothing
            } else {
                // queue is already in a waiting inactive queue; update
                int currentPrecedence = wq.getPrecedence();
                if(currentPrecedence < originalPrecedence ) {
                    // queue bumped up; adjust ordering
                    deactivateQueue(wq);
                    // this intentionally places queue in duplicate inactiveQueue\
                    // only when it comes off the right queue will it activate;
                    // otherwise it reenqueues to right inactive queue, if not
                    // already there (see activateInactiveQueue())
                } else {
                    // queue bumped down or stayed same; 
                    // do nothing until it comes up
                }
            } 
        } else {
            // begin juggling queue between internal ordering structures
            wq.setHeld();
            if(holdQueues()) {
                deactivateQueue(wq);
            } else {
                replenishSessionBalance(wq);
                readyQueue(wq);
            }
        }
        WorkQueue laq = longestActiveQueue;
        if(((laq==null) || wq.getCount() > laq.getCount())) {
            longestActiveQueue = wq; 
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
        assert Thread.currentThread() == managerThread;

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
        assert Thread.currentThread() == managerThread;
        
        wq.setSessionBalance(0); // zero out session balance
        int precedence = wq.getPrecedence();
        if(!wq.getOnInactiveQueues().contains(precedence)) {
            // not already on target, add
            Queue<String> inactiveQueues = getInactiveQueuesForPrecedence(precedence);
            inactiveQueues.add(wq.getClassKey());
            wq.getOnInactiveQueues().add(precedence);
            if(wq.getPrecedence() < highestPrecedenceWaiting ) {
                highestPrecedenceWaiting = wq.getPrecedence();
            }
        }
        wq.setActive(this, false);
    }
    
    /**
     * Get the queue of inactive uri-queue names at the given precedence. 
     * 
     * @param precedence
     * @return queue of inacti
     */
    protected Queue<String> getInactiveQueuesForPrecedence(int precedence) {
        Map<Integer,Queue<String>> inactiveQueuesByPrecedence = 
            getInactiveQueuesByPrecedence();
        Queue<String> candidate = inactiveQueuesByPrecedence.get(precedence);
        if(candidate==null) {
            candidate = createInactiveQueueForPrecedence(precedence);
            inactiveQueuesByPrecedence.put(precedence,candidate);
        }
        return candidate;
    }

    /**
     * Return a sorted map of all inactive queues, keyed by precedence
     * @return SortedMap<Integer, Queue<String>> of inactiveQueues
     */
    abstract SortedMap<Integer, Queue<String>> getInactiveQueuesByPrecedence();

    /**
     * Create an inactiveQueue to hold queue names at the given precedence
     * @param precedence
     * @return Queue<String> for names of inactive queues
     */
    abstract Queue<String> createInactiveQueueForPrecedence(int precedence);

    /**
     * Put the given queue on the retiredQueues queue
     * @param wq
     */
    private void retireQueue(WorkQueue wq) {
        assert Thread.currentThread() == managerThread;

        getRetiredQueues().add(wq.getClassKey());
        decrementQueuedCount(wq.getCount());
        wq.setRetired(true);
        wq.setActive(this, false);
    }
    
    /**
     * Return queue of all retired queue names.
     * 
     * @return Queue<String> of retired queue names
     */
    abstract Queue<String> getRetiredQueues();

    /** 
     * Accomodate any changes in settings.
     */
    public void keyChanged(KeyChangeEvent event) {

        // The rules for a 'retired' queue may have changed; so,
        // unretire all queues to 'inactive'. If they still qualify
        // as retired/overbudget next time they come up, they'll
        // be re-retired; if not, they'll get a chance to become
        // active under the new rules.
        
        // TODO: Only do this when necessary.
        
        Object key = getRetiredQueues().poll();
        while (key != null) {
            WorkQueue q = (WorkQueue)this.allQueues.get(key);
            if(q != null) {
                unretireQueue(q);
            }
            key = getRetiredQueues().poll();
        }
    }
    /**
     * Restore a retired queue to the 'inactive' state. 
     * 
     * @param q
     */
    private void unretireQueue(WorkQueue q) {
        assert Thread.currentThread() == managerThread;

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
     * Return the next CrawlURI eligible to be processed (and presumably
     * visited/fetched) by a a worker thread.
     *
     * Relies on the readyClassQueues having been loaded with
     * any work queues that are eligible to provide a URI. 
     *
     * @return next CrawlURI eligible to be processed, or null if none available
     *
     * @see org.archive.crawler.framework.Frontier#next()
     */
    protected CrawlURI findEligibleURI() {
            assert Thread.currentThread() == managerThread;
            // wake any snoozed queues
            wakeQueues();
            // activate enough inactive queues to fill outbound
            int activationsWanted = 
                outbound.remainingCapacity() - readyClassQueues.size();
            while(activationsWanted > 0 
                    && !getInactiveQueuesByPrecedence().isEmpty() 
                    && highestPrecedenceWaiting < get(PRECEDENCE_FLOOR)) {
                activateInactiveQueue();
                activationsWanted--;
            }
                   
            WorkQueue readyQ = null;
            String key = readyClassQueues.poll();
            if (key != null) {
                readyQ = getQueueFor(key);
            }
            if (readyQ != null) {
                while(true) { // loop left by explicit return or break on empty
                    CrawlURI curi = null;
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
                        readyQ.dequeue(this);
                        doJournalRelocated(curi);
                        curi.setClassKey(currentQueueKey);
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
                
            if(inProcessQueues.size()==0) {
                // Nothing was ready or in progress or imminent to wake; ensure 
                // any piled-up pending-scheduled URIs are considered
                if(this.alreadyIncluded.requestFlush()>0) {
                    return findEligibleURI();
                }
            }
            // nothing eligible
            return null; 
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
        assert Thread.currentThread() == managerThread;

        SortedMap<Integer,Queue<String>> inactiveQueuesByPrecedence = 
            getInactiveQueuesByPrecedence();
        
        int targetPrecedence = highestPrecedenceWaiting;
        Queue<String> inactiveQueues = inactiveQueuesByPrecedence.get(
                targetPrecedence);

        Object key = inactiveQueues.poll();
        assert key != null : "empty precedence queue in map";
        
        if(inactiveQueues.isEmpty()) {
            updateHighestWaiting(targetPrecedence+1);
        }
        
        WorkQueue candidateQ = (WorkQueue) this.allQueues.get(key);
        
        assert candidateQ != null : "missing uri work queue";
        
        boolean was = candidateQ.getOnInactiveQueues().remove(targetPrecedence);
        
        assert was : "queue didn't know it was in "+targetPrecedence+" inactives";
        
        if(candidateQ.getPrecedence() < targetPrecedence) {
            // queue moved up; do nothing (already handled)
            return; 
        }
        if(candidateQ.getPrecedence() > targetPrecedence) {
            // queue moved down; deactivate to new level
            deactivateQueue(candidateQ);
            return; 
        }
        replenishSessionBalance(candidateQ);
        if (candidateQ.isOverBudget()) {
            // if still over-budget after an activation & replenishing,
            // retire
            retireQueue(candidateQ);
            return;
        }
//        long now = System.currentTimeMillis();
//        long delay_ms = candidateQ.getWakeTime() - now;
//        if (delay_ms > 0) {
//            // queue still due for snoozing
//            snoozeQueue(candidateQ, now, delay_ms);
//            return;
//        }
        candidateQ.setWakeTime(0); // clear obsolete wake time, if any
        readyQueue(candidateQ);
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("ACTIVATED queue: " + candidateQ.getClassKey());

        }
    }

    /**
     * Recalculate the value of thehighest-precedence queue waiting
     * among inactive queues. 
     * 
     * @param startFrom start looking at this precedence value
     */
    protected void updateHighestWaiting(int startFrom) {
        // probe for new highestWaiting
        for(int precedenceKey : getInactiveQueuesByPrecedence().tailMap(startFrom).keySet()) {
            if(!getInactiveQueuesByPrecedence().get(precedenceKey).isEmpty()) {
                highestPrecedenceWaiting = precedenceKey;
                return;
            }
        }
        // nothing waiting
        highestPrecedenceWaiting = Integer.MAX_VALUE;
    }

    /**
     * Replenish the budget of the given queue by the appropriate amount.
     * 
     * @param queue queue to replenish
     */
    private void replenishSessionBalance(WorkQueue queue) {
        // get a CrawlURI for override context purposes
        CrawlURI contextUri = queue.peek(this); 
        if(contextUri == null) {
            // use globals TODO: fix problems this will cause if 
            // global total budget < override on empty queue
            queue.setSessionBalance(get(BALANCE_REPLENISH_AMOUNT));
            queue.setTotalBudget(get(QUEUE_TOTAL_BUDGET));
            return;
        }
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
        wq.get(this,QUEUE_PRECEDENCE_POLICY).queueReevaluate(wq);
        if(highestPrecedenceWaiting < wq.getPrecedence() 
            || (wq.isOverBudget() && highestPrecedenceWaiting <= wq.getPrecedence())
            || wq.getPrecedence() >= get(PRECEDENCE_FLOOR)) {
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
    
    /* (non-Javadoc)
     * @see org.archive.crawler.frontier.AbstractFrontier#getMaxInWait()
     */
    @Override
    protected long getMaxInWait() {
        Delayed next = snoozedClassQueues.peek();
        return next == null ? 60000 : next.getDelay(TimeUnit.MILLISECONDS);
    }

    /**
     * Wake any queues sitting in the snoozed queue whose time has come.
     */
    protected void wakeQueues() {
        WorkQueue waked; 
        while((waked = snoozedClassQueues.poll())!=null) {
            waked.setWakeTime(0);
            reenqueueQueue(waked);
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
    protected void processFinish(CrawlURI curi) {
        assert Thread.currentThread() == managerThread;
        
        long now = System.currentTimeMillis();

        curi.incrementFetchAttempts();
        logNonfatalErrors(curi);
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

                wq.unpeek();
                // TODO: consider if this should happen automatically inside unpeek()
                wq.update(this, curi); // rewrite any changes
                if (delay_sec > 0) {
                    long delay_ms = delay_sec * 1000;
                    snoozeQueue(wq, now, delay_ms);
                } else {
                    reenqueueQueue(wq);
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
            doJournalDisregarded(curi);
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

            if (delay_ms > 0) {
                snoozeQueue(wq,now,delay_ms);
            } else {
                reenqueueQueue(wq);
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
//        long snoozeToInactiveDelayMs = get(SNOOZE_DEACTIVATE_MS);
//        if (delay_ms > snoozeToInactiveDelayMs && !inactiveQueues.isEmpty()) {
//            deactivateQueue(wq);
//        } else {
            snoozedClassQueues.add(wq);
//        }
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
    public long deleteURIs(String queueRegex, String uriRegex) {
        long count = 0;
        // TODO: DANGER/ values() may not work right from CachedBdbMap
        Pattern queuePat = Pattern.compile(queueRegex);
        for (String qname: allQueues.keySet()) {
            if (queuePat.matcher(qname).matches()) {
                WorkQueue wq = getQueueFor(qname);
                wq.unpeek();
                count += wq.deleteMatching(this, uriRegex);
            }
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
        int inactiveCount = getTotalEligibleInactiveQueues();
        int ineligibleCount = getTotalIneligibleInactiveQueues();
        int retiredCount = getRetiredQueues().size();
        int exhaustedCount = 
            allCount - activeCount - inactiveCount - retiredCount;
        int inCount = inbound.size();
        int outCount = outbound.size();
        State last = lastReachedState;
        w.print(allCount);
        w.print(" URI queues: ");
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
        w.print(ineligibleCount);
        w.print(" ineligible; ");
        w.print(retiredCount);
        w.print(" retired; ");
        w.print(exhaustedCount);
        w.print(" exhausted");
        w.print(" ["+last+ ": "+inCount+" in, "+outCount+" out]");        
        w.flush();
    }

    /**
     * Total of all URIs in inactive queues at all precedences
     * @return int total 
     */
    protected int getTotalInactiveQueues() {
        return tallyInactiveTotals(getInactiveQueuesByPrecedence());
    }
    
    /**
     * Total of all URIs in inactive queues at precedences above the floor
     * @return int total 
     */
    protected int getTotalEligibleInactiveQueues() {
        return tallyInactiveTotals(
                getInactiveQueuesByPrecedence().headMap(get(PRECEDENCE_FLOOR)));
    }
    
    /**
     * Total of all URIs in inactive queues at precedences at or below the floor
     * @return int total 
     */
    protected int getTotalIneligibleInactiveQueues() {
        return tallyInactiveTotals(
                getInactiveQueuesByPrecedence().tailMap(get(PRECEDENCE_FLOOR)));
    }

    /**
     * @param iqueue 
     * @return
     */
    private int tallyInactiveTotals(SortedMap<Integer,Queue<String>> iqueues) {
        int inactiveCount = 0; 
        for(Queue<String> q : iqueues.values()) {
            inactiveCount += q.size();
        }
        return inactiveCount;
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
        for(Queue<String> inactiveQueues : getInactiveQueuesByPrecedence().values()) {
            queueSingleLinesTo(writer, inactiveQueues.iterator());
        }
        
        writer.print("\n -----===== RETIRED QUEUES =====-----\n");
        queueSingleLinesTo(writer, getRetiredQueues().iterator());
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
        int inactiveCount = getTotalInactiveQueues();
        int retiredCount = getRetiredQueues().size();
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
        w.print(" (");
        Map<Integer,Queue<String>> inactives = getInactiveQueuesByPrecedence();
        boolean betwixt = false; 
        for(Integer k : inactives.keySet()) {
            if(betwixt) {
                w.print("; ");
            }
            w.print("p");
            w.print(k);
            w.print(": ");
            w.print(inactives.get(k).size());
            betwixt = true; 
        }
        w.print(")\n");
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
        for(Queue<String> inactiveQueues : getInactiveQueuesByPrecedence().values()) {
            appendQueueReports(w, inactiveQueues.iterator(),
                    inactiveQueues.size(), REPORT_MAX_QUEUES);
        }
        
        w.print("\n -----===== RETIRED QUEUES =====-----\n");
        appendQueueReports(w, getRetiredQueues().iterator(),
            getRetiredQueues().size(), REPORT_MAX_QUEUES);

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
    public void deleted(CrawlURI curi) {
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
        temp.setStateProvider(manager);
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
     * TODO: rename! (this is a very misleading name) or kill (don't
     * see any implementations that return false)
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
        int inactiveCount = getTotalInactiveQueues();
        int totalQueueCount = (activeCount+inactiveCount);
        return (totalQueueCount == 0) ? 0 : queuedUriCount.get() / totalQueueCount;
    }
    public float congestionRatio() {
        int inProcessCount = inProcessQueues.uniqueSet().size();
        int readyCount = readyClassQueues.size();
        int snoozedCount = snoozedClassQueues.size();
        int activeCount = inProcessCount + readyCount + snoozedCount;
        int eligibleInactiveCount = getTotalEligibleInactiveQueues();
        return (float)(activeCount + eligibleInactiveCount) / (inProcessCount + snoozedCount);
    }
    public long deepestUri() {
        return longestActiveQueue==null ? -1 : longestActiveQueue.getCount();
    }
    
    /** 
     * Return whether frontier is exhausted: all crawlable URIs done (none
     * waiting or pending). Only gives precise answer inside managerThread.
     * 
     * @see org.archive.crawler.framework.Frontier#isEmpty()
     */
    public boolean isEmpty() {
        return queuedUriCount.get() == 0 
            && alreadyIncluded.pending() == 0 
            && inbound.isEmpty();
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.frontier.AbstractFrontier#getInProcessCount()
     */
    @Override
    protected int getInProcessCount() {
        return inProcessQueues.size();
    }
    
    /**
     * Custom deserialization: bring in snoozed queues as array of
     * their names (aka 'classKeys').
     * @param stream
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void readObject(java.io.ObjectInputStream stream)
    throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        String[] snoozedNames = (String[]) stream.readObject();
        snoozedClassQueues = new DelayQueue<WorkQueue>();
        for(int i = 0; i < snoozedNames.length; i++) {
            snoozedClassQueues.add(getQueueFor(snoozedNames[i]));
        }
    }
    
    /**
     * Custom serialization: write snoozed queues as array of their
     * names (aka 'classKeys').
     * 
     * @param stream
     * @throws IOException
     */
    private void writeObject(java.io.ObjectOutputStream stream)
    throws IOException {
        stream.defaultWriteObject();
        WorkQueue[] snoozed = snoozedClassQueues.toArray(new WorkQueue[0]);
        String[] snoozedNames = new String[snoozed.length];
        for(int i = 0;i<snoozed.length;i++) {
            snoozedNames[i] = snoozed[i].getClassKey();
        }
        stream.writeObject(snoozedNames);
    }
}

