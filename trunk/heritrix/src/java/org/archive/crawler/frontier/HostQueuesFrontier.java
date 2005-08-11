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

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;

import org.apache.commons.collections.Predicate;
import org.apache.commons.httpclient.HttpStatus;
import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlHost;
import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.datamodel.CrawlServer;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.datamodel.UriUniqFilter;
import org.archive.crawler.datamodel.UriUniqFilter.HasUriReceiver;
import org.archive.crawler.event.CrawlStatusListener;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.Frontier;
import org.archive.crawler.framework.FrontierMarker;
import org.archive.crawler.framework.ToeThread;
import org.archive.crawler.framework.exceptions.EndedException;
import org.archive.crawler.framework.exceptions.FatalConfigurationException;
import org.archive.crawler.framework.exceptions.InvalidFrontierMarkerException;
import org.archive.crawler.settings.ModuleType;
import org.archive.crawler.settings.RegularExpressionConstraint;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.Type;
import org.archive.crawler.url.Canonicalizer;
import org.archive.crawler.util.BdbUriUniqFilter;
import org.archive.crawler.util.FPUriUniqFilter;
import org.archive.io.ObjectPlusFilesInputStream;
import org.archive.io.ObjectPlusFilesOutputStream;
import org.archive.net.UURI;
import org.archive.queue.MemQueue;
import org.archive.queue.Queue;
import org.archive.util.ArchiveUtils;
import org.archive.util.fingerprint.MemLongFPSet;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentReaderHashMap;

/**
 * A basic mostly breadth-first frontier, which refrains from
 * emitting more than one CrawlURI of the same 'key' (host) at
 * once, and respects minimum-delay and delay-factor specifications
 * for politeness.
 *
 * <p>There are an arbitrary number of 'KeyedQueues' each representing 
 * a certain 'key' class of URIs -- effectively, a single host (by 
 * hostname). 
 *
 * <p>KeyedQueues may have an item in-process -- in which case they
 * do not provide any other items for processing. KeyedQueues may
 * also be 'snoozed' -- when they should be kept inactive for a
 * period of time, to either enforce politeness policies or allow
 * a configurable amount of time between error retries.
 *
 * @author Gordon Mohr
 * @deprecated As of release 1.4, replaced by {@link BdbFrontier}.
 */
public class HostQueuesFrontier extends ModuleType
implements Frontier, FetchStatusCodes, CoreAttributeConstants,
HasUriReceiver,  CrawlStatusListener {
    // be robust against trivial implementation changes
    private static final long serialVersionUID =
        ArchiveUtils.classnameBasedUID(HostQueuesFrontier.class, 1);

    /** truncate reporting of queues at some large but not unbounded number */
    private static final int REPORT_MAX_QUEUES = 100;
    
    private static final Logger logger =
        Logger.getLogger(HostQueuesFrontier.class.getName());

    /** how many multiples of last fetch elapsed time to wait before recontacting same server */
    public final static String ATTR_DELAY_FACTOR = "delay-factor";
    protected final static Float DEFAULT_DELAY_FACTOR = new Float(5);
    
    /** always wait this long after one completion before recontacting
     * same server, regardless of multiple */
    public final static String ATTR_MIN_DELAY = "min-delay-ms";
    protected final static Integer DEFAULT_MIN_DELAY = new Integer(2000); //2 seconds
    
    /** never wait more than this long, regardless of multiple */
    public final static String ATTR_MAX_DELAY = "max-delay-ms";
    protected final static Integer DEFAULT_MAX_DELAY = new Integer(30000); //30 seconds
    
    /** maximum times to emit a CrawlURI without final disposition */
    public final static String ATTR_MAX_RETRIES = "max-retries";
    protected final static Integer DEFAULT_MAX_RETRIES = new Integer(30);

    /** for retryable problems, seconds to wait before a retry */
    public final static String ATTR_RETRY_DELAY = "retry-delay-seconds";
    protected final static Long DEFAULT_RETRY_DELAY = new Long(900); //15 minutes

    /** whether to hold queues INACTIVE until needed for throughput */
    public final static String ATTR_HOLD_QUEUES = "hold-queues";
    protected final static Boolean DEFAULT_HOLD_QUEUES = new Boolean(false); 

    /** maximum simultaneous requests in process to a host (queue) */
    public final static String ATTR_HOST_VALENCE = "host-valence";
    protected final static Integer DEFAULT_HOST_VALENCE = new Integer(1); 

    /** number of hops of embeds (ERX) to bump to front of host queue */
    public final static String ATTR_PREFERENCE_EMBED_HOPS = "preference-embed-hops";
    protected final static Integer DEFAULT_PREFERENCE_EMBED_HOPS = new Integer(1); 

    /** whether to reassign URIs to IP-address based queues when IP known */
    public final static String ATTR_IP_POLITENESS = "ip-politeness";
    // TODO: change default to true once well-tested
    protected final static Boolean DEFAULT_IP_POLITENESS = new Boolean(false); 
    
    /** queue assignment to force onto CrawlURIs; intended to be overridden */
    public final static String ATTR_FORCE_QUEUE = "force-queue-assignment";
    protected final static String DEFAULT_FORCE_QUEUE = "";
    protected final static String 
        ACCEPTABLE_FORCE_QUEUE = "[-\\w\\.]*"; // word chars, dash, period

    /** maximum overall bandwidth usage */
    public final static String ATTR_MAX_OVERALL_BANDWIDTH_USAGE =
        "total-bandwidth-usage-KB-sec";
    protected final static Integer DEFAULT_MAX_OVERALL_BANDWIDTH_USAGE =
        new Integer(0);

    /** maximum per-host bandwidth usage */
    public final static String ATTR_MAX_HOST_BANDWIDTH_USAGE =
        "max-per-host-bandwidth-usage-KB-sec";
    protected final static Integer DEFAULT_MAX_HOST_BANDWIDTH_USAGE =
        new Integer(0);

    /** maximum how many items to store in memory atop each keyedqueue
     * higher == more RAM used per active host; lower == more disk IO */
    public final static String ATTR_HOST_QUEUES_MEMORY_CAPACITY =
        "host-queues-memory-capacity";
    protected final static Integer DEFAULT_HOST_QUEUES_MEMORY_CAPACITY =
        new Integer(200);

    /**
     * True if we're to use the BDB already seen implementation
     * in place of the in-memory already included set.
     * 
     * @see <a href="http://crawler.archive.org/cgi-bin/wiki.pl?AlreadySeen">AlreadySeen</a>
     */
    private static String ATTR_USE_BDB_ALREADY_INCLUDED =
        "use-bdb-already-included";
    private static Boolean DEFAULT_USE_BDB_ALREADY_INCLUDED = Boolean.FALSE;
    
    protected final static float KILO_FACTOR = 1.024F;

    protected CrawlController controller;

    // those UURIs which are already in-process (or processed), and
    // thus should not be rescheduled
    protected UriUniqFilter alreadyIncluded;
    /** ordinal numbers to assign to created CrawlURIs */
    protected long nextOrdinal = 1;

    private ThreadLocalQueue threadWaiting = new ThreadLocalQueue();

    /** Policy for assigning CrawlURIs to named queues */
    protected QueueAssignmentPolicy queueAssignmentPolicy = null;

    /** 
     * All per-class queues.
     * 
     * Queues are of String (classKey) -&gt; KeyedQueue.
     */
    private ConcurrentReaderHashMap allClassQueuesMap =
        new ConcurrentReaderHashMap();

    /** 
     * All per-class queues whose first item may be handed out (that is, 
     * they are READY).
     */
    protected LinkedList readyClassQueues = new LinkedList(); // of KeyedQueues

    /**
     * All per-class queues who are on hold until a certain time.
     * Of KeyedQueue, sorted by wakeTime.
     */
    SortedSet snoozeQueues = new TreeSet(new SchedulingComparator());    
    
    /**
     * All per-class queues that are INACTIVE; will be empty unless
     * 'site-first'/'hold-queues' is set.
     */
    LinkedList inactiveClassQueues = new LinkedList(); // of KeyedQueues
    
    // top-level stats
    long queuedUriCount = 0;

    long succeededFetchCount = 0;
    long failedFetchCount = 0;
    long disregardedUriCount = 0; //URI's that are disregarded (for example because of robot.txt rules)

    long totalProcessedBytes = 0;

    // Used when bandwidth constraint are used
    long nextURIEmitTime = 0;
    long processedBytesAfterLastEmittedURI = 0;
    int lastMaxBandwidthKB = 0;

    // flags indicating operator-specified crawl pause/end 
    private boolean shouldPause = false;
    private boolean shouldTerminate = false;
    
    /**
     * Crawl replay logger.
     *
     * Currently captures Frontier/URI transitions.
     */
    transient private FrontierJournal recover = null;
    
    static final String LOGNAME_RECOVER = "recover";
    
  
    public HostQueuesFrontier(String name) {
        this(name,"HostQueuesFrontier. Maintains the internal " +
                " state of the crawl. It dictates the order in which URIs " +
                " will be scheduled. This frontier is mostly a breadth-first "+
                " frontier, which refrains from emitting more than one " +
                " CrawlURI of the same \'key\' (host) at once, and respects " +
                " minimum-delay and delay-factor specifications for " +
                " politeness.");
    }

    public HostQueuesFrontier(String q, String description) {
        //The 'name' of all frontiers should be the same (URIFrontier.ATTR_NAME)
        //therefore we'll ignore the supplied parameter.
        super(Frontier.ATTR_NAME, description);
        addElementToDefinition(new SimpleType(ATTR_DELAY_FACTOR,
            "How many multiples of last fetch elapsed time to wait before " +
            "recontacting same server", DEFAULT_DELAY_FACTOR));
        addElementToDefinition(new SimpleType(ATTR_MAX_DELAY,
            "Never wait more than this long, regardless of multiple",
            DEFAULT_MAX_DELAY));
        addElementToDefinition(new SimpleType(ATTR_MIN_DELAY,
            "Always wait this long after one completion before recontacting " +
            "same server, regardless of multiple", DEFAULT_MIN_DELAY));
         addElementToDefinition(new SimpleType(ATTR_MAX_RETRIES,
            "How often to retry fetching a URI that failed to be retrieved.\n" +
            "If zero, the crawler will get the robots.txt only.",
            DEFAULT_MAX_RETRIES));
        addElementToDefinition(new SimpleType(ATTR_RETRY_DELAY,
                "How long to wait by default until we retry fetching a" +
                " URI that failed to be retrieved (seconds). ",
                DEFAULT_RETRY_DELAY));
        Type t;
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
        addElementToDefinition(new SimpleType(ATTR_PREFERENCE_EMBED_HOPS,
                "Number of embedded (or redirected) hops up to which " +
                "a URI has higher priority scheduling. For example, if set" +
                "to 1 (the default), items such as inline images (1-hop" +
                "embedded resources) will be scheduled ahead of all regular" +
                "links (or many-hop resources, like nested frames). If set to" +
                "zero, no preferencing will occur, and embeds/redirects are" +
                "scheduled the same as regular links.",
                DEFAULT_PREFERENCE_EMBED_HOPS));
        t = addElementToDefinition(new SimpleType(ATTR_HOST_VALENCE,
                "Maximum number of simultaneous requests to a single" +
                " host.",
                DEFAULT_HOST_VALENCE));
        t.setExpertSetting(true);
        
        t = addElementToDefinition(new SimpleType(ATTR_IP_POLITENESS,
                "Whether to assign URIs to IP-address based queues "+
                "when possible, to remain polite on a per-IP-address "+
                "basis.",
                DEFAULT_IP_POLITENESS));
        t.setExpertSetting(true);
        t.setOverrideable(false);
        t = addElementToDefinition(new SimpleType(ATTR_FORCE_QUEUE,
                "The queue name into which to force URIs.\nShould " +
                "be left blank at global level.  Specify a " +
                "per-domain/per-host override to force URIs into " +
                "a particular named queue, regardless of the assignment " +
                "policy in effect (domain or ip-based politeness). " +
                "This could be used on domains known to all be from " +
                "the same small set of IPs (eg blogspot, dailykos, etc.) "+
                "to simulate IP-based politeness, or it could be used if " +
                "you wanted to enforce politeness over a whole domain, even " +
                "though the subdomains are split across many IPs.",
                DEFAULT_FORCE_QUEUE));
        t.setOverrideable(true);
        t.setExpertSetting(true);
        t.addConstraint(new RegularExpressionConstraint(ACCEPTABLE_FORCE_QUEUE,
                Level.WARNING, "This field must contain only alphanumeric " +
                        "characters plus period, dash, or underscore."));
        
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
                new SimpleType(ATTR_HOST_QUEUES_MEMORY_CAPACITY,
                "Size of each host queue's in-memory head.\n Once each grows " +
                "beyond this size additional items will be written to a file " +
                "on disk. Default value " + DEFAULT_HOST_QUEUES_MEMORY_CAPACITY+
                ". A high value means more RAM used per host queue while a low"+
                " value will require more disk I/O. Lowest legal value is one "
                +"(1).",
                DEFAULT_HOST_QUEUES_MEMORY_CAPACITY));
        t.setExpertSetting(true);
        t = addElementToDefinition(
            new SimpleType(ATTR_USE_BDB_ALREADY_INCLUDED,
            "Use disk-based Berkeley DB to hold the set of Already Included" +
            "URIs.\n" +
            "The Already Included URI set is by-default kept in memory." +
            " The continuous growth of this URI set in memory is one" +
            " cause of out of memory exceptions.  Enabling this option," +
            " the Already Included set is kept on-disk in a Berkeley DB" +
            " table. Enabling this feature will cause the crawler to" +
            " harvest more documents before" +
            " it runs out of memory at the cost of the crawler" +
            " harvesting at a (progressively) slightly slower rate. Lookups" +
            " in BDB as opposed to lookups" +
            " done against a RAM-based Already Included set take about 4" +
            " times longer when the Already Included set is 10million items" +
            " (0.5ms vs. ~2ms).  BDB Already Included lookups slow as the" +
            " the set grows" +
            " at ~Log(n) rate.", DEFAULT_USE_BDB_ALREADY_INCLUDED));
        t.setExpertSetting(true);        
    }

    /**
     * Initializes the Frontier, given the supplied CrawlController.
     *
     * @see org.archive.crawler.framework.Frontier#initialize(org.archive.crawler.framework.CrawlController)
     */
    public void initialize(CrawlController c)
    throws FatalConfigurationException, IOException {
        this.controller = c;
        c.addCrawlStatusListener(this);
        alreadyIncluded = createAlreadyIncluded(c.getStateDisk(),
            "alreadyIncluded");
        
        if(((Boolean)getUncheckedAttribute(null,ATTR_IP_POLITENESS)).booleanValue()) {
            queueAssignmentPolicy = new IPQueueAssignmentPolicy();
        } else {
            queueAssignmentPolicy = new HostnameQueueAssignmentPolicy();
        }
        
        loadSeeds();
        // The below code is from the AbstractFrontier.
        File logsDisk = null;
        try {
            logsDisk = c.getSettingsDir(CrawlOrder.ATTR_LOGS_PATH);
        } catch (AttributeNotFoundException e) {
            logger.severe("Failed to get logs directory " + e);
        }
        if (logsDisk != null) {
            String logsPath = logsDisk.getAbsolutePath() + File.separatorChar;
            this.recover = new RecoveryJournal(logsPath, LOGNAME_RECOVER);
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
    protected UriUniqFilter createAlreadyIncluded(File dir, String filePrefix)
    throws IOException {
        Boolean b = DEFAULT_USE_BDB_ALREADY_INCLUDED;
        try {
            b = (Boolean)getAttribute(null, ATTR_USE_BDB_ALREADY_INCLUDED);
        } catch (AttributeNotFoundException e) {
            e.printStackTrace();
        }
        UriUniqFilter uuf = (b.booleanValue())?
            (UriUniqFilter)new BdbUriUniqFilter(dir, 25):
            (UriUniqFilter)new FPUriUniqFilter(new MemLongFPSet(23,0.75f));
        uuf.setDestination(this);
        return uuf;
    }

    /**
     * Load up the seeds.
     *
     * This method is called on initialize and inside in the crawlcontroller
     * when it wants to force reloading of configuration.
     *
     * @see org.archive.crawler.framework.CrawlController#kickUpdate()
     */
    public synchronized void loadSeeds() {
        // Get the seeds to refresh and then get an iterator inside a
        // synchronization block.  The seeds list may get updated during our
        // iteration. This will throw a concurrentmodificationexception unless
        // we synchronize.
        Writer ignoredWriter = new StringWriter();
        // Get the seeds to refresh.
        Iterator iter = this.controller.getScope().seedsIterator(ignoredWriter);
        while (iter.hasNext()) {
            UURI u = (UURI)iter.next();
            CandidateURI caUri = CandidateURI.createSeedCandidateURI(u);
            caUri.setSchedulingDirective(CandidateURI.MEDIUM);
            innerSchedule(caUri);
        }
        // save ignored items (if any) where they can be consulted later
        AbstractFrontier.saveIgnoredItems(
                ignoredWriter.toString(), 
                controller.getDisk());
    }

    private static class ThreadLocalQueue
    extends ThreadLocal implements Serializable {
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

    protected void batchSchedule(CandidateURI caUri) {
        threadWaiting.getQueue().enqueue(caUri);
    }

    /**
     * Flush pending URI queues.
     * Used when scheduling URIs from the commandline.
     */
    public void batchFlush() {
        innerBatchFlush();
    }

    private void innerBatchFlush(){
        Queue q = threadWaiting.getQueue();
        while(!q.isEmpty()) {
            innerSchedule((CandidateURI)q.dequeue());
        }
    }

    /**
     * Arrange for the given CandidateURI to be visited, if it is not
     * already scheduled/completed.
     *
     * @see org.archive.crawler.framework.Frontier#schedule(org.archive.crawler.datamodel.CandidateURI)
     */
    public void schedule(CandidateURI caUri) {
        batchSchedule(caUri);
    }

    /**
     * Arrange for the given CandidateURI to be visited, if it is not
     * already scheduled/completed.
     *
     * @param caUri The CandidateURI to schedule
     */
    private void innerSchedule(CandidateURI caUri) {
        if(caUri.forceFetch()) {
            alreadyIncluded.addForce(canonicalize(caUri.getUURI()), caUri);
        } else {
            alreadyIncluded.add(canonicalize(caUri.getUURI()), caUri);
        }
    }

    /**
     * This method is called if the URI has not already been
     * seen.
     * 
     * This method is the implementation of the HasUriReceiver interface.
     * 
     * @param caUri An URI object that has not been seen before.
     */
    public void receive(CandidateURI caUri) {
        CrawlURI curi = asCrawlUri(caUri);
        if(curi.isSeed() && curi.getVia() != null
                && curi.flattenVia().length() > 0){
            // The only way a seed can have a non-empty via is if it is the
            // result of a seed redirect.  Add it to the seeds list.
            //
            // This is a feature.  This is handling for case where a seed
            // gets immediately redirected to another page.  What we're doing
            // is treating the immediate redirect target as a seed.
            this.controller.getScope().addSeed(curi);
            // And it needs rapid scheduling.
            curi.setSchedulingDirective(CandidateURI.MEDIUM);
        }
        
        // Optionally preferencing embeds up to MEDIUM
        int prefHops = ((Integer) getUncheckedAttribute(curi,
                ATTR_PREFERENCE_EMBED_HOPS)).intValue();
        if (prefHops > 0) {
            int embedHops = curi.getTransHops();
            if (embedHops > 0 && embedHops <= prefHops
                    && curi.getSchedulingDirective() == CandidateURI.NORMAL) {
                // number of embed hops falls within the preferenced range, and
                // uri is not already MEDIUM -- so promote it
                curi.setSchedulingDirective(CandidateURI.MEDIUM);
            }
        }

        enqueueToKeyed(curi);
    }

    /**
     * Return the next CrawlURI to be processed (and presumably
     * visited/fetched) by a a worker thread.
     *
     * First checks any "Ready" per-host queues, then the global
     * pending queue.
     *
     * @return next CrawlURI to be processed. Or null if none is available.
     * @throws InterruptedException
     * @throws EndedException
     *
     * @see org.archive.crawler.framework.Frontier#next()
     */
    synchronized public CrawlURI next()
    throws InterruptedException, EndedException {
        while(true) {
            long now = System.currentTimeMillis();
            CrawlURI curi = null;

            // check completion conditions
            controller.checkFinish();
            // enforce operator pause
            while(shouldPause) {
                controller.toePaused();
                wait();
            }
            
            // enforce operator terminate
            if(shouldTerminate) {
                throw new EndedException("terminated");
            }
           
            enforceBandwidthThrottle(now);
            
            // Check for snoozing queues who are ready to wake up.
            wakeReadyQueues(now);
    
            // if no ready queues among active, activate inactive queues.
            // TODO: avoid activating new queue if wait for another would be trivial
            // TODO: have inactive queues sorted by priority
            // TODO: (probably elsewhere) deactivate active queues that "have 
            // done enough for now" ("enough" to be defined)
            while(this.readyClassQueues.isEmpty() &&
                !inactiveClassQueues.isEmpty()) {
                KeyedQueue kq = (KeyedQueue) inactiveClassQueues.removeFirst();
                kq.activate();
                assert kq.isEmpty() == false :
                    "Empty queue was waiting for activation";
                kq.setMaximumMemoryLoad(((Integer) getUncheckedAttribute(curi,
                        ATTR_HOST_QUEUES_MEMORY_CAPACITY)).intValue());
                updateQ(kq);
            }
            
            // Now, see if any holding queues are ready with a CrawlURI
            while (!this.readyClassQueues.isEmpty()) {
                curi = dequeueFromReady();
                // check if curi belongs in different queue
                String currentQueueKey = getClassKey(curi);
                if (currentQueueKey.equals(curi.getClassKey())) {
                    // curi was in right queue, emit
                    return emitCuri(curi);
                }
                // URI's assigned queue has changed since it
                // was queued (eg because its IP has become
                // known). 
                // update old queue
                KeyedQueue kq = (KeyedQueue)curi.getHolder();
                if(kq.getState() == URIWorkQueue.EMPTY) {
                    // source queue depleted
                    readyClassQueues.removeFirst();
                    updateQ(kq); // discard
                }
                // send to new queue
                curi.setClassKey(currentQueueKey);
                enqueueToKeyed(curi);
            }
            
            // See if URIs exhausted
            if(isEmpty()) {
                continue; // next controller.checkFinish() will end cleanly
            } 
            
            if(alreadyIncluded.pending() > 0) {
                if(alreadyIncluded.flush() > 0) {
                // Will go to the while(true) with fresh URIs
                    continue;
                }
            } // Else
            
            // Wait until something changes
            waitForChange(now);
        }
    }

    private void waitForChange(long now) throws InterruptedException {
        long wait = 1000; // TODO: consider right value
        if(!snoozeQueues.isEmpty()) {
            wait = ((URIWorkQueue)snoozeQueues.first()).getWakeTime() - now;
        }
        wait(wait);
    }

    private CrawlURI asCrawlUri(CandidateURI caUri) {
        CrawlURI curi;
        if(caUri instanceof CrawlURI) {
            curi = (CrawlURI) caUri;
        } else {
            curi = CrawlURI.from(caUri,nextOrdinal++);
        }
        curi.setClassKey(getClassKey(curi));
        return curi;
    }

    /**
     * @param cauri CandidateURI to calculate class key for.
     * @return a String token representing a queue
     */
    public String getClassKey(CandidateURI cauri) {
        String queueKey = (String) getUncheckedAttribute(cauri,
            ATTR_FORCE_QUEUE);
        if ("".equals(queueKey)) {
            // Typical case, barring overrides
            queueKey =
                queueAssignmentPolicy.getClassKey(this.controller, cauri);
        }
        return queueKey;
    }
    
    /**
     * @param curi
     * @return the CrawlServer to be associated with this CrawlURI
     */
    protected CrawlServer getServer(CrawlURI curi) {
        return this.controller.getServerCache().getServerFor(curi);
    }
    
    /**
     * Ensure that any overall-bandwidth-usage limit is respected,
     * by pausing as long as necessary.
     * 
     * TODO: release frontier lock on scheduling, finishing URIs
     * while this pause is in effect.
     * 
     * @param now
     * @throws InterruptedException
     */
    private void enforceBandwidthThrottle(long now)
    throws InterruptedException {
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
                    Thread.sleep(sleepTime);

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
        logger.fine("Frontier.finished start: " + curi.toString());
        long start = System.currentTimeMillis();
        curi.incrementFetchAttempts();
        logLocalizedErrors(curi);
        try {
        	innerFinished(curi);
        }  finally {
        	// This method cleans out all curi state.
        	curi.processingCleanup();
        	long duration = System.currentTimeMillis() - start;
        	if(duration > 1000) {
        		logger.info("#" +
        				((ToeThread)Thread.currentThread()).getSerialNumber() +
						" " + duration + "ms" + " finished(" + curi.toString() +
						") via " + curi.flattenVia());
        	}
        }
    }
    
    protected synchronized void innerFinished(CrawlURI curi) {
        try {
            // Catch up on scheduling.  Can throw an URIException.
            innerBatchFlush();
            URIWorkQueue kq = (URIWorkQueue) curi.getHolder();
            Object startState = kq.getState();
            kq.noteProcessDone(curi);
            updateScheduling(curi, kq);
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

            if(startState != kq.getState() || kq.isDiscardable()) {
                updateQ(kq);
            }
            
            // New items might be available, let waiting threads know
            notify();
        } catch (RuntimeException e) {
            curi.setFetchStatus(S_RUNTIME_EXCEPTION);
            // store exception temporarily for logging
            curi.putObject(A_RUNTIME_EXCEPTION, e);
            failureDisposition(curi);
        } catch (AttributeNotFoundException e) {
            logger.severe(e.getMessage());
        }
    }

    protected void disregardDisposition(CrawlURI curi) {
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
            disregardedUriCount++;
            curi.stripToMinimal();
        }
    }

    protected boolean isDisregarded(CrawlURI curi) {
        switch (curi.getFetchStatus()) {
            case S_ROBOTS_PRECLUDED :     // they don't want us to have it
            case S_OUT_OF_SCOPE :         // filtered out by scope
            case S_BLOCKED_BY_CUSTOM_PROCESSOR:
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
     * @param curi CrawlURI with errors.
     */
    private void logLocalizedErrors(CrawlURI curi) {
        if(curi.containsKey(A_LOCALIZED_ERRORS)) {
            List localErrors = (List)curi.getObject(A_LOCALIZED_ERRORS);
            Iterator iter = localErrors.iterator();
            while(iter.hasNext()) {
                Object array[] = {curi, iter.next()};
                controller.localErrors.log(Level.WARNING,
                    curi.getUURI().toString(), array);
            }
            // Once logged, discard
            curi.remove(A_LOCALIZED_ERRORS);
        }
    }

    /**
     * The CrawlURI has been successfully crawled, and will be
     * attempted no more.
     *
     * @param curi The CrawlURI
     */
    protected void successDisposition(CrawlURI curi) {
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
            succeededFetchCount++;
        }
        // Let everyone know in case they want to do something before we strip
        // the curi.
        controller.fireCrawledURISuccessfulEvent(curi);
        curi.stripToMinimal();
        finishedSuccess(curi);
    }

    /**
     * Store is empty only if all queues are empty and
     * no URIs are in-process
     *
     * @return True if queues are empty.
     */
    public boolean isEmpty() {
        return 
            alreadyIncluded.pending()==0 &&
            allClassQueuesMap.isEmpty();
    }

    /**
     * Wake any snoozed queues whose snooze time is up.
     * @param now Current time in millisec.
     */
    protected void wakeReadyQueues(long now) {
        while(!snoozeQueues.isEmpty() &&
                ((URIWorkQueue)snoozeQueues.first()).getWakeTime() <= now) {
            URIWorkQueue awoken = (URIWorkQueue)snoozeQueues.first();
            if (!snoozeQueues.remove(awoken)) {
                logger.severe("First() item couldn't be remove()d! - " + awoken +
                    " - " + snoozeQueues.contains(awoken));
                logger.severe(singleLineReport());
            }
            awoken.wake();
            updateQ(awoken);
        }
    }

    protected void discardQueue(URIWorkQueue q) {
        allClassQueuesMap.remove(q.getClassKey());
        q.discard();
        assert !readyClassQueues.contains(q) : "readyClassQueues holding dead q";
        assert !snoozeQueues.contains(q) : "snoozeQueues holding dead q";
    }

    protected CrawlURI dequeueFromReady() {
        KeyedQueue firstReadyQueue =
            (KeyedQueue)readyClassQueues.getFirst();
        assert firstReadyQueue.getState() == URIWorkQueue.READY:
            "Top ready queue not ready but " + firstReadyQueue.getState() +
            ": " + firstReadyQueue.getClassKey();
        assert firstReadyQueue.isEmpty() == false :
            "Top ready queue inexplicably empty: " +
                firstReadyQueue.getClassKey();
        CrawlURI readyCuri = firstReadyQueue.dequeue();
        // For future convenient reference.
        readyCuri.setHolder(firstReadyQueue);
        firstReadyQueue.checkEmpty();
        return readyCuri;
    }

    /**
     * Prepares a CrawlURI for crawling. Also marks it as 'being processed'.
     * 
     * @param curi The CrawlURI
     * @return The CrawlURI
     * @see #noteInProcess(CrawlURI)
     */
    protected CrawlURI emitCuri(CrawlURI curi) {
        noteInProcess(curi);
        logger.finer(this + ".emitCuri(" + curi + ")");
        if (this.recover != null) {
            this.recover.emitted(curi);
        }
        // One less URI in the queue.
        this.queuedUriCount--;
        return curi;
    }

    /**
     * Marks a CrawlURI as being in process.
     * @param curi The CrawlURI to mark.
     */
    protected void noteInProcess(CrawlURI curi) {
        KeyedQueue kq = (KeyedQueue) curi.getHolder();
        if(kq==null){
            logger.severe("No workQueue found for "+curi);
            return; // Couldn't find/create kq.
        }

        assert kq.getState() == URIWorkQueue.READY || kq.getState() == URIWorkQueue.EMPTY : 
            "odd state "+ kq.getState() + " for classQueue "+ kq + "of to-be-emitted CrawlURI";

        kq.noteInProcess(curi);
        if(kq.getState()==URIWorkQueue.BUSY || kq.getState() == URIWorkQueue.EMPTY) {
            assert readyClassQueues.getFirst() == kq : "readClassQueues head object unexpected";
            readyClassQueues.removeFirst();
        }
    }

    /**
     * Get the KeyedQueue for a CrawlURI. If it does not exist it will be
     * created.
     * @param curi The CrawlURI
     * @return The KeyedQueue for the CrawlURI or null if it does not exist and
     * an exception occured trying to create it.
     */
    protected URIWorkQueue keyedQueueFor(CrawlURI curi) {
        URIWorkQueue kq = null;
        synchronized (allClassQueuesMap) {
            kq = (URIWorkQueue)this.allClassQueuesMap.get(curi.getClassKey());
            if (kq == null) {
                try {
                    String key = curi.getClassKey();
                    // The creation of disk directories makes this a potentially
                    // lengthy operation we don't want to hold full-frontier lock
                    // for 
                    kq = new KeyedQueue(key,
                        this.controller.getServerCache().getServerFor(curi),
						scratchDirFor(key),
						((Integer)getAttribute(ATTR_HOST_QUEUES_MEMORY_CAPACITY,
                            curi)).intValue());
                    kq.setValence(((Integer)getAttribute(ATTR_HOST_VALENCE,curi)).intValue());
                    this.allClassQueuesMap.put(kq.getClassKey(),kq);
                    if(((Boolean)getAttribute(ATTR_HOLD_QUEUES,curi)).booleanValue()) {
                        // set inactive in-mem caps to 1/20th of active TODO: configurable
                        ((KeyedQueue)kq).setMaximumMemoryLoad(((Integer) getUncheckedAttribute(curi,
                                ATTR_HOST_QUEUES_MEMORY_CAPACITY)).intValue()/20);
                        // leave inactive, add to inactive collection
                        // TODO: see if this can't be mvoed elsewhere
                        this.inactiveClassQueues.addLast(kq);
                    } else {
                        ((KeyedQueue)kq).setMaximumMemoryLoad(((Integer) getUncheckedAttribute(curi,
                                ATTR_HOST_QUEUES_MEMORY_CAPACITY)).intValue());
                        // make eligible for READY status immediately
                        kq.activate();
                    }
                } catch (AttributeNotFoundException e2) {
                    logger.severe(e2.getMessage());
                } catch (IOException e) {
                    // An IOException occured trying to make new KeyedQueue.
                    curi.putObject(A_RUNTIME_EXCEPTION,e);
                    Object array[] = { curi };
                    this.controller.runtimeErrors.log(Level.SEVERE,
                        curi.getUURI().toString(), array);
                }
            }
        }
        return kq;
    }

    /**
     * Return a scratch dir for the given key's temp files. Every key gets its
     * own subdir. To avoid having any one directory with thousands of files, 
     * there are also two levels of enclosing directory named by the least-significant
     * hex digits of the key string's java hashcode. 
     * 
     * @param key
     * @return Scratch directory.
     */
    private File scratchDirFor(String key) {
        String hex = Integer.toHexString(key.hashCode());
        while (hex.length()<4) {
            hex = "0"+hex;
        }
        int len = hex.length(); 
        return new File(this.controller.getStateDisk(), hex.substring(len-2, len)
                + File.separator + hex.substring(len-4, len-2) + File.separator + key);
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
        URIWorkQueue kq = keyedQueueFor(curi);
        if(kq == null){
            logger.severe("No workQueue found for " + curi);
            // Should only happen when other parts of the
            // system -- such as the U(sable)URI prescreening --
            // have problems.
            curi.setFetchStatus(S_UNQUEUEABLE); 
            failureDisposition(curi);
            return; // Couldn't find/create kq.
        }
        kq.enqueue(curi);
        if(kq.getState() != URIWorkQueue.INACTIVE) {
            // Active queue: may effect scheduling
            if(kq.checkEmpty()) {
                // If kq state changed, update frontier's internals.
                updateQ(kq);
            }
        }
        this.queuedUriCount++;
        // Update recovery log.
        if (this.recover != null) {
            this.recover.added(curi);
        }
        return;
    }

    /**
     * If an empty queue has become ready, add to ready queues.
     * Should only be called after state has changed.  Also
     * discards queue if discardable.
     *
     * @param kq Queue to update.
     */
    private void updateQ(URIWorkQueue kq) {
        Object state = kq.getState();
        if (kq.isDiscardable()) {
            // Empty & ready; discard
            discardQueue(kq);
            return;
        }
        
        if (state == URIWorkQueue.READY) {
            // Has become ready
            assert !kq.isEmpty(): "Adding to ready an empty queue.";
            readyClassQueues.addLast(kq);
            synchronized (this) {
                notify(); // wake a waiting thread
            }
            return;
        }
        
        // Otherwise, no need to change whatever state it's in
        // TODO: verify this in only reached in sensible situations
    }

    protected long earliestWakeTime() {
        if (!snoozeQueues.isEmpty()) {
            return ((URIWorkQueue)snoozeQueues.first()).getWakeTime();
        }
        return Long.MAX_VALUE;
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
    protected void updateScheduling(CrawlURI curi, URIWorkQueue kq)
    throws AttributeNotFoundException {
        long durationToWait = 0;
        if (curi.containsKey(A_FETCH_BEGAN_TIME)
            && curi.containsKey(A_FETCH_COMPLETED_TIME)) {

            long completeTime = curi.getLong(A_FETCH_COMPLETED_TIME);
            long durationTaken = (completeTime - curi.getLong(A_FETCH_BEGAN_TIME));
            durationToWait =
                (long)(((Float)getAttribute(ATTR_DELAY_FACTOR, curi)).
                    floatValue() * durationTaken);

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

            long now = System.currentTimeMillis();
            int maxBandwidthKB = ((Integer) getAttribute(
                        ATTR_MAX_HOST_BANDWIDTH_USAGE, curi)).intValue();
            if (maxBandwidthKB > 0) {
                // Enforce bandwidth limit
                CrawlHost host = controller.getServerCache().getHostFor(curi);
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
    protected void failureDisposition(CrawlURI curi) {
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
            this.failedFetchCount++;
            curi.stripToMinimal();
        }
        if (this.recover != null) {
            this.recover.finishedFailure(curi);
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
    protected boolean needsPromptRetry(CrawlURI curi)
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
    protected boolean needsRetrying(CrawlURI curi)
            throws AttributeNotFoundException {
        //
        if (curi.getFetchAttempts() >= ((Integer)getAttribute(ATTR_MAX_RETRIES,curi)).intValue() ) {
            return false;
        }
        switch (curi.getFetchStatus()) {
            case S_CONNECT_FAILED:
            case S_CONNECT_LOST:
            case S_DOMAIN_UNRESOLVABLE:
                // these are all worth a retry
                // TODO: consider if any others (S_TIMEOUT in some cases?) deserve retry
                return true;
            default:
                return false;
        }
    }

    protected void scheduleForRetry(CrawlURI curi) 
            throws AttributeNotFoundException {
        long delay = ((Long)getAttribute(ATTR_RETRY_DELAY,curi)).longValue();
        if (delay > 0) {
            // snooze to future
            logger.finer("Inserting snoozed " + curi + " for " + delay);
            URIWorkQueue kq = (URIWorkQueue) curi.getHolder();
            if(kq != null) {
                snoozeQueueUntil(kq, System.currentTimeMillis() + (delay * 1000));
            }
        }

        // now, reinsert CrawlURI in relevant queue
        reschedule(curi);

        // Let everyone interested know that it will be retried.
        controller.fireCrawledURINeedRetryEvent(curi); 
        if (this.recover != null) {
            this.recover.rescheduled(curi);
        }
    }

    /**
     * Put near top of relevant keyedqueue (but behind anything recently
     * scheduled 'high')
     *
     * @param curi CrawlURI to reschedule.
     */
    protected void reschedule(CrawlURI curi) {
        // Eliminate state related to only prior processing passthrough.
        boolean isPrereq = curi.isPrerequisite();
        curi.processingCleanup(); // This will reset prereq value.
        if (isPrereq == false) {
        curi.setSchedulingDirective(CandidateURI.MEDIUM);
        }
        enqueueToKeyed(curi);
        queuedUriCount++;
    }

    /**
     * Snoozes a queue until a fixed point in time has passed.
     *
     * @param kq A KeyedQueue that we want to snooze
     * @param wake Time (in millisec.) when we want the queue to stop snoozing.
     */
    protected void snoozeQueueUntil(URIWorkQueue kq, long wake) {
        if(kq.getState()== URIWorkQueue.INACTIVE) {
            // Likely a brand new queue under a site-first mode of operation
            // Must activate before snoozing
            inactiveClassQueues.remove(kq);
            kq.activate();
        } else if(kq.getState() == URIWorkQueue.SNOOZED) {
            // Must be removed before time may be mutated
            snoozeQueues.remove(kq);
        } else if (kq.getState() == URIWorkQueue.READY ) {
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
    protected boolean shouldBeForgotten(CrawlURI curi) {
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
        alreadyIncluded.forget(canonicalize(curi.getUURI()), curi);
    }

    /**  (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#discoveredUriCount()
     */
    public long discoveredUriCount(){
        return (this.alreadyIncluded != null)? this.alreadyIncluded.count(): 0;
    }

    /** (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#queuedUriCount()
     */
    public long queuedUriCount(){
        return queuedUriCount;
    }

    /** (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#finishedUriCount()
     */
    public long finishedUriCount() {
        return succeededFetchCount+failedFetchCount+disregardedUriCount;
    }

    /** (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#succeededFetchCount()
     */
    public long succeededFetchCount(){
        return succeededFetchCount;
    }

    /** (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#failedFetchCount()
     */
    public long failedFetchCount(){
       return failedFetchCount;
    }

    /** (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#disregardedUriCount()
     */
    public long disregardedUriCount() {
        return disregardedUriCount;
    }

    public long totalBytesWritten() {
        return totalProcessedBytes;
    }

    public FrontierMarker getInitialMarker(String regexpr, boolean inCacheOnly) {
        ArrayList keyqueueKeys = new ArrayList();
        if(allClassQueuesMap.size() != 0) {
            Iterator q = allClassQueuesMap.keySet().iterator();
            while(q.hasNext()) {
                keyqueueKeys.add(q.next());
            }
        }
        return new HostQueuesFrontierMarker(regexpr,inCacheOnly,keyqueueKeys);
    }

    public ArrayList getURIsList(FrontierMarker marker, int numberOfMatches,
            boolean verbose) throws InvalidFrontierMarkerException {
        if(marker instanceof HostQueuesFrontierMarker == false){
            throw new InvalidFrontierMarkerException();
        }

        HostQueuesFrontierMarker mark = (HostQueuesFrontierMarker)marker;
        ArrayList list = new ArrayList(numberOfMatches);

        // inspect the KeyedQueues
        while( numberOfMatches > 0 && mark.getCurrentQueue() != -1){
            String queueKey = (String)mark.getKeyQueues().
                get(mark.getCurrentQueue());
            KeyedQueue keyq = (KeyedQueue)allClassQueuesMap.get(queueKey);
            if(keyq==null){
                throw new InvalidFrontierMarkerException();
            }

            numberOfMatches -= inspectQueue(keyq,queueKey,list,mark,verbose, numberOfMatches);
            if(numberOfMatches>0){
                mark.nextQueue();
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
     * @throws InvalidFrontierMarkerException
     */
    private int inspectQueue( KeyedQueue queue,
                              String queueName,
                              ArrayList list,
                              HostQueuesFrontierMarker marker,
                              boolean verbose,
                              int numberOfMatches)
                          throws InvalidFrontierMarkerException{
        if(queue.length() < marker.getAbsolutePositionInCurrentQueue()) {
            // Not good. Invalid marker.
            throw new InvalidFrontierMarkerException();
        }

        if(queue.length()==0){
            return 0;
        }

        Iterator it = queue.getIterator(marker.isInCacheOnly());
        int foundMatches = 0;
        long itemsScanned = 0;
        while(it.hasNext() && foundMatches < numberOfMatches){
            Object o = it.next();
            if( itemsScanned >= marker.getAbsolutePositionInCurrentQueue()
                    && o instanceof CandidateURI ){
                // Ignore items that are in front of current position
                // and those that are not CandidateURIs.
                CandidateURI caURI = (CandidateURI)o;
                if(marker.match(caURI)){
                    // Found match.
                    String text;

                    // A verbose description
                    text = "["+queueName+" " + itemsScanned + "] "+caURI.singleLineReport();

                    list.add(text);
                    foundMatches++;
                    marker.incrementNextItemNumber();
                }
            }
            itemsScanned++;
        }
        marker.setAbsolutePositionInCurrentQueue(itemsScanned);
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
        if(allClassQueuesMap.size()!=0) {
            Iterator q = allClassQueuesMap.keySet().iterator();
            while(q.hasNext()) {
                URIWorkQueue kq = (URIWorkQueue)allClassQueuesMap.get(q.next());
                numberOfDeletes += kq.deleteMatchedItems(mat);

                // If our deleting has emptied the KeyedQueue then update it's
                // state.
                kq.checkEmpty();
            }
        }
        // Delete from pendingQueue
//        numberOfDeletes += pendingQueue.deleteMatchedItems(mat);
        queuedUriCount -= numberOfDeletes;
        return numberOfDeletes;
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

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.URIFrontier#deleted(org.archive.crawler.datamodel.CrawlURI)
     */
    public synchronized void deleted(CrawlURI curi) {
        disregardDisposition(curi);
        curi.processingCleanup();
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.URIFrontier#importRecoverLog(java.lang.String)
     */
    public void importRecoverLog(String pathToLog, boolean retainFailures)
			throws IOException {
        File source = new File(pathToLog);
        if (!source.isAbsolute()) {
            source = new File(getSettingsHandler().getOrder()
                    .getController().getDisk(), pathToLog);
        }
        RecoveryJournal.importRecoverLog(source,this,retainFailures);
    }

    public void considerIncluded(UURI u) {
        this.alreadyIncluded.note(canonicalize(u));
    }

    public void kickUpdate() {
        loadSeeds();
    }
    
    public void start() {
        unpause(); 
    }
    
    synchronized public void pause() { 
        shouldPause = true;
        notifyAll();
    }
    synchronized public void unpause() { 
        shouldPause = false;
        notifyAll();
    }
    
    synchronized public void terminate() { 
        shouldTerminate = true;
        if (this.recover != null) {
            this.recover.close();
            this.recover = null;
        }
    }  
    
    protected void finishedSuccess(CrawlURI c) {
        if (this.recover != null) {
            this.recover.finishedSuccess(c);
        }
    }
    
    /**
     * Canonicalize passed uuri.
     * Its would be sweeter if this canonicalize function was encapsulated
     * by that which it canonicalizes but because settings change with
     * context -- i.e. there may be overrides in operation for a
     * particular URI -- its not so easy; Each CandidateURI would need
     * a reference to the settings system.  That's awkward to pass in.
     * Copied from {@link AbstractFrontier}.
     * @param uuri Candidate URI to canonicalize.
     * @return Canonicalized version of passed <code>caUri</code>.
     * If a problem, no canonicalization is done and the
     * CandidateURI#getURIString() is returned.
     */
    protected String canonicalize(UURI uuri) {
        return Canonicalizer.canonicalize(uuri, this.controller.getOrder());
    }

    public FrontierJournal getFrontierJournal() {
        return this.recover;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.event.CrawlStatusListener#crawlEnding(java.lang.String)
     */
    public void crawlEnding(String sExitMessage) {
        // TODO Auto-generated method stub 
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.event.CrawlStatusListener#crawlEnded(java.lang.String)
     */
    public void crawlEnded(String sExitMessage) {
        // Ok, if the CrawlController is exiting we delete our
        // reference to it to facilitate gc.  In fact, do it for
        // all references because frontier instances stick around
        // betweeen crawls so the UI can build new jobs based off
        // the old and so old jobs can be looked at.
        if (this.alreadyIncluded != null) {
            this.alreadyIncluded.close();
            this.alreadyIncluded = null;
        }
        this.queueAssignmentPolicy = null;
        this.readyClassQueues = null;
        this.allClassQueuesMap = null;
        this.inactiveClassQueues = null;
        this.snoozeQueues = null;
        // Clearing controller is a problem. We get
        // NPEs in #preNext.
        // this.controller = null;
    }
   
    /* (non-Javadoc)
     * @see org.archive.crawler.event.CrawlStatusListener#crawlStarted(java.lang.String)
     */
    public void crawlStarted(String message) {
        // TODO Auto-generated method stub
        
    }
    
    /* (non-Javadoc)
     * @see org.archive.crawler.event.CrawlStatusListener#crawlStarted(java.lang.String)
     */
    public void crawlCheckpoint(File checkpointDir) {
        // TODO Auto-generated method stub
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.event.CrawlStatusListener#crawlPausing(java.lang.String)
     */
    public void crawlPausing(String statusMessage) {
        // TODO Auto-generated method stub
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.event.CrawlStatusListener#crawlPaused(java.lang.String)
     */
    public void crawlPaused(String statusMessage) {
        // TODO Auto-generated method stub
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.event.CrawlStatusListener#crawlResuming(java.lang.String)
     */
    public void crawlResuming(String statusMessage) {
        // TODO Auto-generated method stub
    }
    
    //
    // Reporter implementation
    //
    
    public String[] getReports() {
        // none but default for now
        return new String[] {};
    }
    
    /* (non-Javadoc)
     * @see org.archive.util.Reporter#singleLineReport()
     */
    public String singleLineReport() {
        return ArchiveUtils.singleLineReport(this);
    }

    /* (non-Javadoc)
     * @see org.archive.util.Reporter#reportTo(java.io.Writer)
     */
    public void reportTo(PrintWriter writer) throws IOException {
        reportTo(null,writer);
    }
    /**
     * @return One-line summary report, useful for display when full report
     * may be unwieldy. 
     * @throws IOException
     */
    public void singleLineReportTo(PrintWriter writer) throws IOException {
        writer.print(allClassQueuesMap.size()+" queues: ");
        writer.print(readyClassQueues.size()+" ready, ");
        writer.print(snoozeQueues.size()+" snoozed, ");
        writer.print(inactiveClassQueues.size()+" inactive");        
    }
    
    /**
     * This method compiles a human readable report on the status of the frontier
     * at the time of the call.
     *
     * @return A report on the current status of the frontier.
     * @throws IOException
     */
    public synchronized void reportTo(String name, PrintWriter writer) throws IOException
    {
        // ignore name, only one report type for now
        
        long now = System.currentTimeMillis();

        writer.print("Frontier report - "
                   + ArchiveUtils.TIMESTAMP12.format(new Date()) + "\n");
        writer.print(" Job being crawled: "
                   + controller.getOrder().getCrawlOrderName() + "\n");
        writer.print("\n -----===== STATS =====-----\n");
        writer.print(" Discovered:    " + discoveredUriCount() + "\n");
        writer.print(" Queued:        " + queuedUriCount() + "\n");
        writer.print(" Finished:      " + finishedUriCount() + "\n");
        writer.print("  Successfully: " + succeededFetchCount() + "\n");
        writer.print("  Failed:       " + failedFetchCount() + "\n");
        writer.print("  Disregarded:  " + disregardedUriCount() + "\n");
        writer.print("\n -----===== QUEUES =====-----\n");
        writer.print(" Already included size:     " + alreadyIncluded.count()+"\n");

        // show up to REPORT_MAX_QUEUES of ready queues
        int countOfQueues = readyClassQueues.size();
        writer.print("\n Ready class queues size:   " + countOfQueues + "\n");
        for(int i=0 ; i < countOfQueues && i < REPORT_MAX_QUEUES  ; i++)
        {
            KeyedQueue kq = (KeyedQueue)readyClassQueues.get(i);
            appendKeyedQueue(writer,kq,now);
        }
        if(countOfQueues>REPORT_MAX_QUEUES) {
            writer.print("...and "+(countOfQueues-REPORT_MAX_QUEUES)+" more.\n\n");
        }

        // show up to REPORT_MAX_QUEUES of snoozed queues
        countOfQueues = snoozeQueues.size();
        writer.print("\n Snooze queues size:        " + countOfQueues + "\n");
        int displayCount = 0;
        Iterator iter = snoozeQueues.iterator();
        while(iter.hasNext() && displayCount < REPORT_MAX_QUEUES) {
            Object q = iter.next();
            if(q instanceof KeyedQueue)
            {
                KeyedQueue kq = (KeyedQueue)q;
                appendKeyedQueue(writer,kq,now);
                displayCount++;
            }
        }
        if(countOfQueues>REPORT_MAX_QUEUES) {
            writer.print("...and "+(countOfQueues-REPORT_MAX_QUEUES)+" more.\n\n");
        }       
        
        // show up to REPORT_MAX_QUEUES of all queues, avoid dups of READY/SNOOZED
        countOfQueues = allClassQueuesMap.size();
        writer.print("\n All class queues map size: " + countOfQueues + "\n");
        displayCount = 0;
        iter = allClassQueuesMap.values().iterator();
        while(iter.hasNext() && displayCount < REPORT_MAX_QUEUES) {
            KeyedQueue kq = (KeyedQueue)iter.next();
            if(kq.getState()!=URIWorkQueue.READY && kq.getState()!=URIWorkQueue.SNOOZED)
            {
                appendKeyedQueue(writer,kq,now);
                displayCount++;
            }
        }
        if (displayCount < countOfQueues) {
            writer.print("...and "+(countOfQueues-displayCount)+" more.\n\n");
        }       

        writer.print("\n Inactive queues size:        " + inactiveClassQueues.size() + "\n");
    }


    private void appendKeyedQueue(PrintWriter writer, URIWorkQueue kq, long now) throws IOException {
        writer.print("    KeyedQueue  " + kq.getClassKey() + "\n");
        writer.print("     Length:        " + kq.length() + "\n");
//        writer.print("     Is ready:  " + kq.shouldWake() + "\n");
        if(kq instanceof KeyedQueue){
        writer.print("     Status:        " +
            ((KeyedQueue)kq).getState().toString() + "\n");
        }
        if(kq.getState()==URIWorkQueue.SNOOZED) {
            writer.print("     Wakes in:      " + ArchiveUtils.formatMillisecondsToConventional(kq.getWakeTime()-now)+"\n");
        }
        if(kq.getInProcessItems().size()>0) {
            Iterator iter = kq.getInProcessItems().iterator();
            while (iter.hasNext()) {
                writer.print("     InProcess:     " + iter.next() + "\n");
            }
        }
        writer.print("     Last enqueued: " + kq.getLastQueued()+"\n");
        writer.print("     Last dequeued: " + kq.getLastDequeued()+"\n");
    }

}
