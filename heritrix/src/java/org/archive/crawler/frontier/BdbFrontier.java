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
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.collections.Bag;
import org.apache.commons.collections.BagUtils;
import org.apache.commons.collections.bag.HashBag;
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
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.Type;
import org.archive.crawler.util.BdbUriUniqFilter;
import org.archive.queue.LinkedQueue;
import org.archive.util.ArchiveUtils;

import com.sleepycat.bind.serial.StoredClassCatalog;
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
 * BDB lock timeout exceptions if more than a tiny crawl; these seem
 * to be harmless. 
 *
 * @author Gordon Mohr
 */
public class BdbFrontier extends AbstractFrontier
implements FetchStatusCodes, CoreAttributeConstants, HasUriReceiver {
    // be robust against trivial implementation changes
    private static final long serialVersionUID = ArchiveUtils
            .classnameBasedUID(BdbFrontier.class, 1);

    /** truncate reporting of queues at some large but not unbounded number */
    private static final int REPORT_MAX_QUEUES = 5000;

    private static final Logger logger = Logger.getLogger(BdbFrontier.class
            .getName());

    /** percentage of heap to allocate to bdb cache */
    public final static String ATTR_BDB_CACHE_PERCENT =
        "bdb-cache-percent";
    protected final static Integer DEFAULT_BDB_CACHE_PERCENT =
        new Integer(0);
    
    /** whether to hold queues INACTIVE until needed for throughput */
    public final static String ATTR_HOLD_QUEUES = "hold-queues";
    protected final static Boolean DEFAULT_HOLD_QUEUES = new Boolean(true); 

//    /** whether to hold queues INACTIVE until needed for throughput */
//    public final static String ATTR_USE_BUDGETTED_ROTATION = "use-budgetted-rotation";
//    protected final static Boolean DEFAULT_USE_BUDGETTED_ROTATION = new Boolean(true); 

    /** whether to hold queues INACTIVE until needed for throughput */
    public final static String ATTR_BALANCE_REPLENISH_AMOUNT = "balance-replenish-amount";
    protected final static Integer DEFAULT_BALANCE_REPLENISH_AMOUNT = new Integer(3000);

    /** total expenditure to allow a queue before 'retiring' it  */
    public final static String ATTR_QUEUE_TOTAL_BUDGET = "queue-total-budget";
    protected final static Long DEFAULT_QUEUE_TOTAL_BUDGET = new Long(-1);

    /** cost assignment policy to use (by class name) */
    public final static String ATTR_COST_POLICY = "cost-policy";
    protected final static String DEFAULT_COST_POLICY = ZeroCostAssignmentPolicy.class.getName();

    /** all URIs scheduled to be crawled */
    protected BdbMultipleWorkQueues pendingUris;

    /** those UURIs which are already in-process (or processed), and
     thus should not be rescheduled */
    protected UriUniqFilter alreadyIncluded;

    /** all known queues */
    protected Map allQueues = new HashMap(); // of classKey -> BDBWorkQueue

    /** all per-class queues whose first item may be handed out */
    protected LinkedQueue readyClassQueues = new LinkedQueue(); // of BDBWorkQueue

    /** all per-class queues from whom a URI is outstanding */
    protected Bag inProcessQueues = BagUtils.synchronizedBag(new HashBag()); // of BDBWorkQueue

    /** all per-class queues held in snoozed state, sorted by wake time */
    protected SortedSet snoozedClassQueues = Collections.synchronizedSortedSet(new TreeSet());

    /** all 'inactive' queues, not yet in active rotation */
    protected LinkedQueue inactiveQueues = new LinkedQueue();

    /** 'retired' queues, no longer considered for activation */
    protected LinkedQueue retiredQueues = new LinkedQueue();
    
    /** shared bdb Environment for Frontier subcomponents */
    // TODO: investigate using multiple environments to split disk accesses
    // across separate physical disks
    protected Environment dbEnvironment;

    /** how long to wait for a ready queue when there's nothing snoozed */
    private static final long DEFAULT_WAIT = 1000; // 1 second

    /** a policy for assigning 'cost' values to CrawlURIs */
    private CostAssignmentPolicy costAssignmentPolicy;
    
    /**
     * A snoozed queue will be available at this time.
     * Gets updated when queues are snoozed.
     */
    private volatile long nextWakeupTime = -1;
    
    
    /** all policies available to be chosen */
    String[] AVAILABLE_COST_POLICIES = new String[] {
            DEFAULT_COST_POLICY,
            UnitCostAssignmentPolicy.class.getName(),
            WagCostAssignmentPolicy.class.getName(),
            AntiCalendarCostAssignmentPolicy.class.getName()};
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
        Type t = addElementToDefinition(
                new SimpleType(ATTR_BDB_CACHE_PERCENT,
                "Percentage of heap to allocate to BerkeleyDB JE cache. " +
                "Default of zero means no preference (accept BDB's default" +
                "or properties setting).",
                DEFAULT_BDB_CACHE_PERCENT));
        t.setExpertSetting(true);
        t.setOverrideable(false);
        t = addElementToDefinition(new SimpleType(ATTR_HOLD_QUEUES,
                "Whether to hold newly-created per-host URI work" +
                " queues until needed to stay busy.\n If false (default)," +
                " all queues may contribute URIs for crawling at all" +
                " times. If true, queues begin (and collect URIs) in" +
                " an 'inactive' state, and only when the Frontier needs" +
                " another queue to keep all ToeThreads busy will new" +
                " queues be activated.",
                DEFAULT_HOLD_QUEUES));
        t.setExpertSetting(true);
        t.setOverrideable(false);
//        t = addElementToDefinition(new SimpleType(ATTR_USE_BUDGETTED_ROTATION,
//                "Whether to rotate queues out of active status based on " +
//                "a budgetting mechanism. If true, an active queue will move " +
//                "to inactive status -- making room for a waiting inactive queue " +
//                "to become active -- when its budget is depleted. When its " +
//                "turn to reactivate comes up, it will be given a refreshed " +
//                "budget. Each URI attempted from a queue depletes its budget by " +
//                "some amount. Default is true. ",
//                DEFAULT_USE_BUDGETTED_ROTATION));
//        t.setExpertSetting(true);
//        t.setOverrideable(true);
        t = addElementToDefinition(new SimpleType(ATTR_BALANCE_REPLENISH_AMOUNT,
                "Amount to replenish a queue's activity balance when it becomes " +
                "active. Larger amounts mean more URIs will be tried from the " +
                "queue before it is deactivated in favor of waiting queues. " +
                "Default is 3000",
                DEFAULT_BALANCE_REPLENISH_AMOUNT));
        t.setExpertSetting(true);
        t.setOverrideable(true);
        t = addElementToDefinition(new SimpleType(ATTR_QUEUE_TOTAL_BUDGET,
                "Total activity expenditure allowable to a single queue; queues " +
                "over this expenditure will be 'retired' and crawled no more. " +
                "Default of -1 means no ceiling on activity expenditures is " +
                "enforced.",
                DEFAULT_QUEUE_TOTAL_BUDGET));
        t.setExpertSetting(true);
        t.setOverrideable(true);

        addElementToDefinition(new SimpleType(ATTR_COST_POLICY,
                "Policy for calculating the cost of each URI attempted. " +
                "The default UnitCostAssignmentPolicy considers the cost of " +
                "each URI to be '1'.", DEFAULT_COST_POLICY, AVAILABLE_COST_POLICIES));
        t.setExpertSetting(true);
    }

    /**
     * Initializes the Frontier, given the supplied CrawlController.
     *
     * @see org.archive.crawler.framework.Frontier#initialize(org.archive.crawler.framework.CrawlController)
     */
    public void initialize(CrawlController c)
            throws FatalConfigurationException, IOException {
        // Call the super method. It sets up frontier journalling.
        super.initialize(c);
        this.controller = c;
        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setAllowCreate(true);
        // This setting required by Linda Lee of bdbje as part of the 
        // work on the bug #11552.
        envConfig.setConfigParam("je.evictor.criticalPercentage", "1");
        int bdbCachePercent = ((Integer) getUncheckedAttribute(null,
				ATTR_BDB_CACHE_PERCENT)).intValue();
        if(bdbCachePercent > 0) {
        	// Operator has expressed a preference; override BDB default or 
        	// je.properties value
        	envConfig.setCachePercent(bdbCachePercent);
        }
        try {
            dbEnvironment = new Environment(c.getStateDisk(), envConfig);
            if (logger.isLoggable(Level.INFO)) {
                // Write out the bdb configuration.
                envConfig = dbEnvironment.getConfig();
                logger.info("BdbConfiguration: Cache percentage " +
                    envConfig.getCachePercent() +
                    ", cache size " + envConfig.getCacheSize());
            }
            pendingUris = createMultipleWorkQueues();
            alreadyIncluded = createAlreadyIncluded();
        } catch (DatabaseException e) {
            e.printStackTrace();
            throw new FatalConfigurationException(e.getMessage());
        }
        try {
            costAssignmentPolicy = (CostAssignmentPolicy) Class.forName(
                    (String) getUncheckedAttribute(null, ATTR_COST_POLICY))
                    .newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            throw new FatalConfigurationException(e.getMessage());
        }
        
        loadSeeds();
    }
    
    /* (non-Javadoc)
     * @see org.archive.crawler.frontier.AbstractFrontier#crawlEnded(java.lang.String)
     */
    public void crawlEnded(String sExitMessage) {
        // Ok, if the CrawlController is exiting we delete our
        // reference to it to facilitate gc.  In fact, do it for
        // all references because frontier instances stick around
        // betweeen crawls so the UI can build new jobs based off
        // the old and so old jobs can be looked at.
        this.allQueues = null;
        this.inProcessQueues = null;
        if (this.alreadyIncluded != null) {
            this.alreadyIncluded.close();
            this.alreadyIncluded = null;
        }
        if (this.pendingUris != null) {
            this.pendingUris.close();
            this.pendingUris = null;
        }
        this.snoozedClassQueues = null;
        this.queueAssignmentPolicy = null;
        this.readyClassQueues = null;
        this.dbEnvironment = null;
        // Clearing controller is a problem. We get
        // NPEs in #preNext.
        // this.controller = null;
        super.crawlEnded(sExitMessage);
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
        return new BdbMultipleWorkQueues(this.dbEnvironment);
    }

    /**
     * Create a memory-based UriUniqFilter that will serve as record 
     * of already seen URIs.
     *
     * @return A UURISet that will serve as a record of already seen URIs
     * @throws IOException
     */
    protected UriUniqFilter createAlreadyIncluded() throws IOException {
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
     * @param caUri CandidateURI.
     */
    public void receive(CandidateURI caUri) {
        CrawlURI curi = asCrawlUri(caUri);
        applySpecialHandling(curi);
        incrementQueuedUriCount();
        sendToQueue(curi);
        // Update recovery log.
        doJournalAdded(curi);
    }

    /**
     * Send a CrawlURI to the appropriate subqueue.
     * 
     * @param curi
     */
    private void sendToQueue(CrawlURI curi) {
        BdbWorkQueue wq = getQueueFor(curi);
        synchronized (wq) {
            wq.enqueue(this.pendingUris, curi);
            if(!wq.isHeld()) {
                wq.setHeld();
                if(holdQueues()) {
                    deactivateQueue(wq);
                } else {
                    replenishSessionBalance(wq);
                    readyQueue(wq);
                }
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
        return ((Boolean) getUncheckedAttribute(null, ATTR_HOLD_QUEUES))
                .booleanValue();
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
     * Put the given queue on the inactiveQueues queue
     * @param wq
     */
    private void deactivateQueue(BdbWorkQueue wq) {
        try {
            wq.setSessionBalance(0); // zero out session balance
            inactiveQueues.put(wq);
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.err.println("unable to deactivate queue "+wq);
            // propagate interrupt up 
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Put the given queue on the inactiveQueues queue
     * @param wq
     */
    private void retireQueue(BdbWorkQueue wq) {
        try {
            retiredQueues.put(wq);
            decrementQueuedCount(wq.getCount());
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.err.println("unable to retire queue "+wq);
            // propagate interrupt up 
            throw new RuntimeException(e);
        }
    }
    
    /** 
     * Accomodate any changes in settings.
     * 
     * @see org.archive.crawler.framework.Frontier#kickUpdate()
     */
    public void kickUpdate() {
        super.kickUpdate();
        try {
            // The rules for a 'retired' queue may have changed; so,
            // unretire all queues to 'inactive'. If they still qualify
            // as retired/overbudget next time they come up, they'll
            // be re-retired; if not, they'll get a chance to become
            // active under the new rules.
            BdbWorkQueue q = (BdbWorkQueue) retiredQueues.poll(0);
            while(q != null) {
                unretireQueue(q);
                q = (BdbWorkQueue) retiredQueues.poll(0);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            // propagate interrupt up 
            throw new RuntimeException(e);
        }
    }
    /**
     * Restore a retired queue to the 'inactive' state. 
     * 
     * @param q
     */
    private void unretireQueue(BdbWorkQueue q) {
        deactivateQueue(q);
        incrementQueuedUriCount(q.getCount());
    }

    /**
     * Return the work queue for the given CrawlURI's classKey. URIs
     * are ordered and politeness-delayed within their 'class'.
     * 
     * @param curi CrawlURI to base queue on
     * @return the found or created BdbWorkQueue
     */
    private BdbWorkQueue getQueueFor(CrawlURI curi) {
        BdbWorkQueue wq;
        String classKey = curi.getClassKey();
        synchronized (allQueues) {
            wq = (BdbWorkQueue)allQueues.get(classKey);
            if (wq == null) {
                wq = new BdbWorkQueue(classKey);
                wq.setTotalBudget(((Long)getUncheckedAttribute(
                    curi,ATTR_QUEUE_TOTAL_BUDGET)).longValue());
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

            // Do common checks for pause, terminate, bandwidth-hold
            preNext(now);
            
            // If time, wake any snoozed queues.
            long timeTilWake = this.nextWakeupTime - now;
            if (timeTilWake <= 0) {
                this.wakeQueues();
                timeTilWake = this.nextWakeupTime - now;
            }
            // Don't wait if there are items buffered in alreadyIncluded or
            // inactive queues, or wait any longer than interval to next wake
            long wait = (alreadyIncluded.pending() > 0)
                    || (!inactiveQueues.isEmpty()) ? 0 : Math.min(DEFAULT_WAIT,timeTilWake);

            BdbWorkQueue readyQ = (BdbWorkQueue) readyClassQueues.poll(wait);
            if (readyQ != null) {
                while(true) { // loop left by explicit return or break on empty
                    CrawlURI curi = null;
                    synchronized(readyQ) {
                        curi = readyQ.peek(this.pendingUris);                     
                        if (curi != null) {
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
                            readyQ.dequeue(this.pendingUris);
                            curi.setHolderKey(null);
                            sendToQueue(curi);
                        } else {
                            // readyQ is empty and ready: release held, allowing
                            // subsequent enqueues to ready
                            readyQ.clearHeld();
                            break;
                        }
                    }
                }
            }

            if(shouldTerminate) {
                // skip subsequent steps if already on last legs
                throw new EndedException("shouldTerminate is true");
            }
                
            // Nothing was ready; ensure any piled-up scheduled URIs are considered
            this.alreadyIncluded.flush(); 
            
            // if still nothing ready, activate an inactive queue, if available
            if(readyClassQueues.isEmpty()) {
                activateInactiveQueue();
            }
        }
    }


    /* (non-Javadoc)
     * @see org.archive.crawler.frontier.AbstractFrontier#noteAboutToEmit(org.archive.crawler.datamodel.CrawlURI, org.archive.crawler.frontier.BdbFrontier.BdbWorkQueue)
     */
    protected void noteAboutToEmit(CrawlURI curi, BdbWorkQueue q) {
        super.noteAboutToEmit(curi, q);
//        if (((Boolean) getUncheckedAttribute(curi, ATTR_USE_BUDGETTED_ROTATION))
//                .booleanValue()) {
//            // only dock budget if rotation is in effect
            q.expend(getCost(curi));
//        }
        // TODO: is this the best way to be sensitive to potential mid-crawl changes
        long totalBudget = ((Long)getUncheckedAttribute(curi,ATTR_QUEUE_TOTAL_BUDGET)).longValue();
        q.setTotalBudget(totalBudget);
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
        if (cost == -1) {
            cost = costAssignmentPolicy.costOf(curi);
            curi.setHolderCost(cost);
        }
        return cost;
    }
    
    /**
     * Activate an inactive queue, if any are available. 
     * 
     * @throws InterruptedException
     */
    private void activateInactiveQueue() throws InterruptedException {
        BdbWorkQueue candidateQ = (BdbWorkQueue) inactiveQueues.poll(0);
        if(candidateQ != null) {
            synchronized(candidateQ) {
                replenishSessionBalance(candidateQ);
                if(candidateQ.isOverBudget()){
                    // if still over-budget after an activation & replenishing,
                    // retire
                    retireQueue(candidateQ);
                } else {
                    readyQueue(candidateQ);
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("ACTIVATED queue: " +
                            candidateQ.getClassKey());
                    }
                }
            }
        }
    }

    /**
     * Replenish the budget of the given queue by the appropriate amount.
     * 
     * @param queue queue to replenish
     */
    private void replenishSessionBalance(BdbWorkQueue queue) {
        // get a CrawlURI for override context purposes
        CrawlURI contextUri = queue.peek(this.pendingUris); 
        // TODO: consider confusing cross-effects of this and IP-based politeness
        queue.incrementSessionBalance(((Integer) getUncheckedAttribute(contextUri,
                ATTR_BALANCE_REPLENISH_AMOUNT)).intValue());
        queue.unpeek(); // don't insist on that URI being next released
    }

    /**
     * Enqueue the given queue to either readyClassQueues or inactiveQueues,
     * as appropriate.
     * 
     * @param wq
     */
    private void reenqueueQueue(BdbWorkQueue wq) {
        if(wq.isOverBudget()) {
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
        long now = System.currentTimeMillis();
        // Set default next wake time to be in one millisecond in case nothing
        // to wake.
        long nextWakeTime = now + DEFAULT_WAIT;
        int wokenQueuesCount = 0;
        synchronized (snoozedClassQueues) {
            while (true) {
                if (snoozedClassQueues.isEmpty()) {
                    break;
                }
                BdbWorkQueue peek = (BdbWorkQueue) snoozedClassQueues.first();
                nextWakeTime = peek.getWakeTime();
                if ((nextWakeTime - now) <= 0) {
                    snoozedClassQueues.remove(peek);
                    peek.setWakeTime(0);
                    reenqueueQueue(peek);
                    wokenQueuesCount++;
                } else {
                    break;
                }
            }
        }
        
        this.nextWakeupTime = Math.max(now, nextWakeTime);
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
        assert (wq.peek(this.pendingUris) == curi) : "unexpected peek " + wq;
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
                    reenqueueQueue(wq);
                }
            }
            // Let everyone interested know that it will be retried.
            controller.fireCrawledURINeedRetryEvent(curi);
            doJournalRescheduled(curi);
            return;
        }

        // curi will definitely be disposed of without retry, so remove from q
        wq.dequeue(this.pendingUris);
        decrementQueuedCount(1);
        log(curi);

        if (curi.isSuccess()) {
            totalProcessedBytes += curi.getContentSize();
            incrementSucceededFetchCount();
            // Let everyone know in case they want to do something before we strip the curi.
            controller.fireCrawledURISuccessfulEvent(curi);
            doJournalFinishedSuccess(curi);
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
            doJournalFinishedFailure(curi);
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

    /**
     * Place the given queue into 'snoozed' state, ineligible to
     * supply any URIs for crawling, for the given amount of time. 
     * 
     * @param wq queue to snooze 
     * @param now time now in ms 
     * @param delay_ms time to snooze in ms
     */
    private void snoozeQueue(BdbWorkQueue wq, long now, long delay_ms) {
        long nextTime = now + delay_ms;
        wq.setWakeTime(nextTime);
        snoozedClassQueues.add(wq);
        // Update nextWakeupTime if we're supposed to wake up even sooner.
        if (nextTime < this.nextWakeupTime) {
            this.nextWakeupTime = nextTime;
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
        alreadyIncluded.forget(canonicalize(curi.getUURI()));
    }

    /**  (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#discoveredUriCount()
     */
    public long discoveredUriCount() {
        return (this.alreadyIncluded != null)? this.alreadyIncluded.count(): 0;
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
        long count = 0;
        Iterator iter = allQueues.values().iterator();
        while(iter.hasNext()) {
            BdbWorkQueue wq = (BdbWorkQueue)iter.next();
            wq.unpeek();
            count += wq.deleteMatching(this.pendingUris, match);
        }
        decrementQueuedCount(count);
        return count;
    }

    /**
     * @return One-line summary report, useful for display when full report
     * may be unwieldy. 
     */
    public String oneLineReport() {
        int allCount = allQueues.size();
        int inProcessCount = inProcessQueues.uniqueSet().size();
        int readyCount = readyClassQueues.getCount();
        int snoozedCount = snoozedClassQueues.size();
        int activeCount = inProcessCount + readyCount + snoozedCount;
        int inactiveCount = inactiveQueues.getCount();
        int retiredCount = retiredQueues.getCount();
        StringBuffer rep = new StringBuffer();
        rep.append(allCount + " queues: " + 
                activeCount + " active (" + 
                inProcessCount + " in-process; " +
                readyCount + " ready; " + 
                snoozedCount + " snoozed); " +
                inactiveCount +" inactive; " +
                retiredCount + " retired");
        return rep.toString();
    }

    /**
     * This method compiles a human readable report on the status of the frontier
     * at the time of the call.
     *
     * @return A report on the current status of the frontier.
     */
    public synchronized String report() {
        int allCount = allQueues.size();
        int inProcessCount = inProcessQueues.uniqueSet().size();
        int readyCount = readyClassQueues.getCount();
        int snoozedCount = snoozedClassQueues.size();
        int activeCount = inProcessCount + readyCount + snoozedCount;
        int inactiveCount = inactiveQueues.getCount();
        int retiredCount = retiredQueues.getCount();
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
        rep.append("\n All class queues map size: " + allCount + "\n");
        rep.append(  "             Active queues: " + activeCount  + "\n");
        rep.append(  "                    In-process: " + inProcessCount  + "\n");
        rep.append(  "                         Ready: " + readyCount + "\n");
        rep.append(  "                       Snoozed: " + snoozedCount + "\n");
        rep.append(  "           Inactive queues: " + inactiveCount + "\n");
        rep.append(  "            Retired queues: " + retiredCount + "\n");
        rep.append("\n -----===== IN-PROCESS QUEUES =====-----\n");
        Iterator iter = inProcessQueues.iterator();
        int count = 0; 
        while(iter.hasNext() && count < REPORT_MAX_QUEUES ) {
            BdbWorkQueue q = (BdbWorkQueue) iter.next();
            count++;
            rep.append(q.report());
        }
        if(inProcessQueues.size()>REPORT_MAX_QUEUES) {
            rep.append("...and "+(inProcessQueues.size()-REPORT_MAX_QUEUES)+" more.\n");
        }
        rep.append("\n -----===== READY QUEUES =====-----\n");
        iter = readyClassQueues.iterator();
        count = 0; 
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
        rep.append("\n -----===== INACTIVE QUEUES =====-----\n");
        iter = inactiveQueues.iterator();
        count = 0; 
        while(iter.hasNext() && count < REPORT_MAX_QUEUES ) {
            BdbWorkQueue q = (BdbWorkQueue) iter.next();
            count++;
            rep.append(q.report());
        }
        if(inactiveQueues.getCount()>REPORT_MAX_QUEUES) {
            rep.append("...and "+(inactiveQueues.getCount()-REPORT_MAX_QUEUES)+" more.\n");
        }
        rep.append("\n -----===== RETIRED QUEUES =====-----\n");
        iter = retiredQueues.iterator();
        count = 0; 
        while(iter.hasNext() && count < REPORT_MAX_QUEUES ) {
            BdbWorkQueue q = (BdbWorkQueue) iter.next();
            count++;
            rep.append(q.report());
        }
        if(retiredQueues.getCount()>REPORT_MAX_QUEUES) {
            rep.append("...and "+(retiredQueues.getCount()-REPORT_MAX_QUEUES)+" more.\n");
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

    public void considerIncluded(UURI u) {
        this.alreadyIncluded.note(canonicalize(u));
    }

    /**
     * @return Returns the dbEnvironment.
     */
    public Environment getBdbEnvironment() {
        return this.dbEnvironment;
    }
    
    public StoredClassCatalog getBdbClassCatalog() {
        return this.pendingUris.getClassCatalog();
    }
}

