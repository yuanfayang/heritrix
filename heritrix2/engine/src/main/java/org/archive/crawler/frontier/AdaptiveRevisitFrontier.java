/* AdaptiveRevisitFrontier.java
*
* Created on Sep 13, 2004
*
* Copyright (C) 2004 Kristinn Sigur?sson.
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
*/
package org.archive.crawler.frontier;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;
import javax.management.openmbean.CompositeData;

import org.apache.commons.httpclient.HttpStatus;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.SchedulingConstants;

import static org.archive.crawler.datamodel.CoreAttributeConstants.*;
import static org.archive.modules.fetcher.FetchStatusCodes.*;

import org.archive.crawler.datamodel.UriUniqFilter;
import org.archive.crawler.datamodel.UriUniqFilter.CrawlUriReceiver;
import org.archive.crawler.event.CrawlStatusListener;
import org.archive.crawler.framework.CrawlControllerImpl;
import org.archive.crawler.framework.CrawlerLoggerModule;
import org.archive.crawler.framework.Frontier;
import org.archive.crawler.framework.exceptions.EndedException;
import org.archive.modules.ModuleAttributeConstants;
import org.archive.modules.canonicalize.CanonicalizationRule;
import org.archive.modules.canonicalize.Canonicalizer;
import org.archive.modules.deciderules.DecideRule;
import org.archive.modules.net.CrawlServer;
import org.archive.modules.net.ServerCache;
import org.archive.modules.net.ServerCacheUtil;
import org.archive.modules.seeds.SeedModuleImpl;
import org.archive.net.UURI;
import org.archive.settings.file.BdbModule;
import org.archive.state.Expert;
import org.archive.state.Immutable;
import org.archive.state.Key;
import org.archive.state.KeyManager;
import org.archive.state.Path;
import org.archive.state.StateProvider;
import org.archive.util.ArchiveUtils;

import static org.archive.crawler.frontier.AdaptiveRevisitAttributeConstants.*;

/**
 * A Frontier that will repeatedly visit all encountered URIs. 
 * <p>
 * Wait time between visits is configurable and varies based on observed 
 * changes of documents.
 * <p>
 * The Frontier borrows many things from HostQueuesFrontier, but implements 
 * an entirely different strategy in issuing URIs and consequently in keeping a
 * record of discovered URIs.
 *
 * @author Kristinn Sigurdsson
 */
public class AdaptiveRevisitFrontier  
implements Frontier, Serializable, CrawlStatusListener, CrawlUriReceiver {

    private static final long serialVersionUID = -3L;

    private static final Logger logger =
        Logger.getLogger(AdaptiveRevisitFrontier.class.getName());

    
    @Immutable
    final public static Key<CrawlControllerImpl> CONTROLLER = 
        Key.makeAuto(CrawlControllerImpl.class);
    
    @Immutable
    final public static Key<BdbModule> BDB =
        Key.makeAuto(BdbModule.class);
    
    @Immutable
    final public static Key<SeedModuleImpl> SEEDS =
        Key.makeAuto(SeedModuleImpl.class);
    
    @Immutable
    final public static Key<ServerCache> SERVER_CACHE =
        Key.makeAuto(ServerCache.class);
    
    @Immutable
    final public static Key<UriUniqFilter> URI_UNIQ_FILTER =
        Key.makeAuto(UriUniqFilter.class);
    
    @Immutable
    final public static Key<Path> DIR = Key.make(Path.EMPTY);
    
    /** How many multiples of last fetch elapsed time to wait before recontacting
     * same server */
    final public static Key<Float> DELAY_FACTOR = Key.make((float)5.0);
    
    /** Always wait this long after one completion before recontacting
     * same server, regardless of multiple */
    final public static Key<Integer> MIN_DELAY_MS = Key.make(2000);
    
    /** Never wait more than this long, regardless of multiple */
    final public static Key<Integer> MAX_DELAY_MS = Key.make(30000);
    
    /** Maximum times to emit a CrawlURI without final disposition */
    final public static Key<Integer> MAX_RETRIES = Key.make(30);

    /** For retryable problems, seconds to wait before a retry */
    final public static Key<Long> RETRY_DELAY = Key.make(900L);
    
    /** Maximum simultaneous requests in process to a host (queue) */
    @Expert
    final public static Key<Integer> HOST_VALENCE = Key.make(1);

    /** Number of hops of embeds (ERX) to bump to front of host queue */
    final public static Key<Integer> PREFERENCE_EMBED_HOPS = 
        Key.make(0);
    
    /** Queue assignment to force on CrawlURIs. Intended to be used 
     *  via overrides*/
    @Immutable
    final public static Key<String> FORCE_QUEUE_ASSIGNMENT = 
        Key.make("");
    
    @Immutable
    final public static Key<List<CanonicalizationRule>> URI_CANONICALIZATION_RULES =
        Key.makeList(CanonicalizationRule.class);
    
    @Immutable
    final public static Key<CrawlerLoggerModule> LOGGER_MODULE = 
        Key.makeAuto(CrawlerLoggerModule.class);

    /** Acceptable characters in forced queue names.
     *  Word chars, dash, period, comma, colon */
    protected final static String ACCEPTABLE_FORCE_QUEUE = "[-\\w\\.,:]*";

    /** Should the queue assignment ignore www in hostnames, effectively 
     *  stripping them away. 
     */
    @Expert
    final public static Key<Boolean> QUEUE_IGNORE_WWW = 
        Key.make(false);

    
    private CrawlControllerImpl controller;
    private SeedModuleImpl seeds;
    private BdbModule bdb;
    private ServerCache serverCache;
    
    private AdaptiveRevisitQueueList hostQueues;
    
    private UriUniqFilter alreadyIncluded;

    private ThreadLocalQueue threadWaiting = new ThreadLocalQueue();

    /** Policy for assigning CrawlURIs to named queues */
    private QueueAssignmentPolicy queueAssignmentPolicy = null;
    
    // top-level stats
    private long succeededFetchCount = 0;
    private long failedFetchCount = 0;
    // URI's that are disregarded (for example because of robot.txt rules)
    private long disregardedUriCount = 0;

    private long totalProcessedBytes = 0;
    
    // Flags indicating operator-specified crawl pause/end 
    private boolean shouldPause = false;
    private boolean shouldTerminate = false;
    
    private Path dir;
    
    private List<CanonicalizationRule> rules;
    
    private CrawlerLoggerModule loggerModule;

    public AdaptiveRevisitFrontier() {
    }

    
    public synchronized void initialTasks(StateProvider provider) {
        rules = provider.get(this, URI_CANONICALIZATION_RULES);
        
        controller = provider.get(this, CONTROLLER);
        dir = provider.get(this, DIR);
        this.serverCache = provider.get(this, SERVER_CACHE);

        queueAssignmentPolicy = new HostnameQueueAssignmentPolicy();
        alreadyIncluded = provider.get(this, URI_UNIQ_FILTER);
        
        seeds = provider.get(this, SEEDS);
        bdb = provider.get(this, BDB);

        try {
            hostQueues = new AdaptiveRevisitQueueList(bdb,
                bdb.getClassCatalog());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        
        loadSeeds();
    }


    /**
     * Loads the seeds
     */
    public void loadSeeds() {
        Writer ignoredWriter = new StringWriter();
        // Get the seeds to refresh.
        Iterator<UURI> iter = seeds.seedsIterator(ignoredWriter);
        while (iter.hasNext()) {
            CrawlURI caUri = new CrawlURI(iter.next());
            caUri.setSeed(true);
            caUri.setSchedulingDirective(SchedulingConstants.MEDIUM);
            schedule(caUri);
        }
        batchFlush();
        // save ignored items (if any) where they can be consulted later
        AbstractFrontier.saveIgnoredItems(
                ignoredWriter.toString(), 
                dir.toFile());
    }
    
    public String getClassKey(CrawlURI cauri) {
        String queueKey = cauri.get(this, FORCE_QUEUE_ASSIGNMENT);
            if ("".equals(queueKey)) {
                // Typical case, barring overrides
                queueKey =
                    queueAssignmentPolicy.getClassKey(cauri);
                // The queueAssignmentPolicy is always based on Hostnames
                // We may need to remove any www[0-9]{0,}\. prefixes from the
                // hostnames
                if(cauri.get(this, QUEUE_IGNORE_WWW)){
                    queueKey = queueKey.replaceAll("^www[0-9]{0,}\\.","");
                }
            }
            return queueKey;
    }

    /**
     * Canonicalize passed uuri. Its would be sweeter if this canonicalize
     * function was encapsulated by that which it canonicalizes but because
     * settings change with context -- i.e. there may be overrides in operation
     * for a particular URI -- its not so easy; Each CrawlURI would need a
     * reference to the settings system. That's awkward to pass in.
     * 
     * @param uuri Candidate URI to canonicalize.
     * @return Canonicalized version of passed <code>uuri</code>.
     */
    protected String canonicalize(UURI uuri) {
        StateProvider def = controller.getSheetManager().getGlobalSheet();
        return Canonicalizer.canonicalize(def, uuri.toString(), rules);
    }

    /**
     * Canonicalize passed CrawlURI. This method differs from
     * {@link #canonicalize(UURI)} in that it takes a look at
     * the CrawlURI context possibly overriding any canonicalization effect if
     * it could make us miss content. If canonicalization produces an URL that
     * was 'alreadyseen', but the entry in the 'alreadyseen' database did
     * nothing but redirect to the current URL, we won't get the current URL;
     * we'll think we've already see it. Examples would be archive.org
     * redirecting to www.archive.org or the inverse, www.netarkivet.net
     * redirecting to netarkivet.net (assuming stripWWW rule enabled).
     * <p>Note, this method under circumstance sets the forceFetch flag.
     * 
     * @param cauri CrawlURI to examine.
     * @return Canonicalized <code>cacuri</code>.
     */
    protected String canonicalize(CrawlURI cauri) {
        String canon = canonicalize(cauri.getUURI());
        if (cauri.isLocation()) {
            // If the via is not the same as where we're being redirected (i.e.
            // we're not being redirected back to the same page, AND the
            // canonicalization of the via is equal to the the current cauri, 
            // THEN forcefetch (Forcefetch so no chance of our not crawling
            // content because alreadyseen check things its seen the url before.
            // An example of an URL that redirects to itself is:
            // http://bridalelegance.com/images/buttons3/tuxedos-off.gif.
            // An example of an URL whose canonicalization equals its via's
            // canonicalization, and we want to fetch content at the
            // redirection (i.e. need to set forcefetch), is netarkivet.dk.
            if (!cauri.toString().equals(cauri.getVia().toString()) &&
                    canonicalize(cauri.getVia()).equals(canon)) {
                cauri.setForceFetch(true);
            }
        }
        return canon;
    }

    /**
     * 
     * @param caUri The URI to schedule.
     */
    protected void innerSchedule(CrawlURI curi) {
        // New CrawlURIs get 'current time' as the time of next processing.
        if (!curi.containsDataKey(A_TIME_OF_NEXT_PROCESSING)) {
            curi.getData().put(A_TIME_OF_NEXT_PROCESSING, 
                    System.currentTimeMillis());
        }
        
        if(curi.getClassKey() == null){
            curi.setClassKey(getClassKey(curi));
        }

        if(curi.isSeed() && curi.getVia() != null
                && curi.flattenVia().length() > 0) {
            // The only way a seed can have a non-empty via is if it is the
            // result of a seed redirect.  Add it to the seeds list.
            //
            // This is a feature.  This is handling for case where a seed
            // gets immediately redirected to another page.  What we're doing
            // is treating the immediate redirect target as a seed.
            seeds.addSeed(curi);
            // And it needs rapid scheduling.
            curi.setSchedulingDirective(SchedulingConstants.MEDIUM);
        }
        
        // Optionally preferencing embeds up to MEDIUM
        int prefHops = curi.get(this, PREFERENCE_EMBED_HOPS);
        boolean prefEmbed = false;
        if (prefHops > 0) {
            int embedHops = curi.getTransHops();
            if (embedHops > 0 && embedHops <= prefHops
                    && curi.getSchedulingDirective() == SchedulingConstants.NORMAL) {
                // number of embed hops falls within the preferenced range, and
                // uri is not already MEDIUM -- so promote it
                curi.setSchedulingDirective(SchedulingConstants.MEDIUM);
                prefEmbed = true;
            }
        }

        // Finally, allow curi to be fetched right now 
        // (while not overriding overdue items)
        curi.getData().put(A_TIME_OF_NEXT_PROCESSING,
                System.currentTimeMillis());
        
        try {
            logger.finest("scheduling " + curi.toString());
            AdaptiveRevisitHostQueue hq = getHQ(curi);
            hq.add(curi,prefEmbed);
        } catch (IOException e) {
            // TODO Handle IOExceptions
            e.printStackTrace();
        }
        
    }

    /**
     * Get the AdaptiveRevisitHostQueue for the given CrawlURI, creating
     * it if necessary. 
     * 
     * @param curi CrawlURI for which to get a queue
     * @return AdaptiveRevisitHostQueue for given CrawlURI
     * @throws IOException
     */
    protected AdaptiveRevisitHostQueue getHQ(CrawlURI curi) throws IOException {
        AdaptiveRevisitHostQueue hq = hostQueues.getHQ(curi.getClassKey());
        if(hq == null){
            // Need to create it.
            int valence = HOST_VALENCE.getDefaultValue();
            valence = curi.get(this, HOST_VALENCE);
            hq = hostQueues.createHQ(curi.getClassKey(),valence);
        }
        return hq;
    }

    protected void batchSchedule(CrawlURI caUri) {
        threadWaiting.getQueue().add(caUri);
    }

    protected void batchFlush() {
        innerBatchFlush();
    }

    private void innerBatchFlush() {
        Queue<CrawlURI> q = threadWaiting.getQueue();
        while(!q.isEmpty()) {
            CrawlURI caUri = (CrawlURI)q.remove();
            if(alreadyIncluded != null){
                String cannon = canonicalize(caUri);
                System.out.println("Cannon of " + caUri + " is " + cannon);
                if (caUri.forceFetch()) {
                    alreadyIncluded.addForce(cannon, caUri);
                } else {
                    alreadyIncluded.add(cannon, caUri);
                }
            } else {
                innerSchedule(caUri);
            }
        }
    }
    
    /**
     * @param curi
     * @return the CrawlServer to be associated with this CrawlURI
     */
    protected CrawlServer getServer(CrawlURI curi) {
        UURI uuri = curi.getUURI();
        return ServerCacheUtil.getServerFor(serverCache, uuri);
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#next()
     */
    public synchronized CrawlURI next() 
            throws InterruptedException, EndedException {
        controller.checkFinish();
        
        while(shouldPause){
//            controller.toePaused();
            // TODO: update to use noteFrontierState
            wait();
        }
        
        if(shouldTerminate){
            throw new EndedException("terminated");
        }
        
        AdaptiveRevisitHostQueue hq = hostQueues.getTopHQ();
        
        while(hq.getState() != AdaptiveRevisitHostQueue.HQSTATE_READY){
            // Ok, so we don't have a ready queue, wait until the top one
            // will become available.
            long waitTime = hq.getNextReadyTime() - System.currentTimeMillis();
            if(waitTime > 0){
                wait(waitTime);
            }
            // The top HQ may have changed, so get it again
            hq = hostQueues.getTopHQ(); 
        }             

        if(shouldTerminate){
            // May have been terminated while thread was waiting for IO
            throw new EndedException("terminated");
        }
        
        try {
            CrawlURI curi = hq.next();
            // Populate CURI with 'transient' variables such as server.
            logger.fine("Issuing " + curi.toString());
            long temp = (Long)curi.getData().get(A_TIME_OF_NEXT_PROCESSING);
            long currT = System.currentTimeMillis();
            long overdue = (currT-temp);
            if(logger.isLoggable(Level.FINER)){
                String waitI = "not set";
                if(curi.containsDataKey(A_WAIT_INTERVAL)){
                    waitI = ArchiveUtils.formatMillisecondsToConventional(
                            (Long)curi.getData().get(A_WAIT_INTERVAL));
                }
                logger.finer("Wait interval: " + waitI + 
                        ", Time of next proc: " + temp +
                        ", Current time: " + currT +
                        ", Overdue by: " + overdue + "ms");
            }
            if(overdue < 0){
                // This should never happen.
                logger.severe("Time overdue for " + curi.toString() + 
                        "is negative (" + overdue + ")!");
            }
            curi.getData().put(A_FETCH_OVERDUE, overdue);
            return curi;
        } catch (IOException e) {
            // TODO: Need to handle this in an intelligent manner. 
            //       Is probably fatal?
            e.printStackTrace();
        }

        return null;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#isEmpty()
     */
    public boolean isEmpty() {
        // Technically, the Frontier should never become empty since URIs are
        // only discarded under exceptional circumstances.
        return hostQueues.getSize() == 0;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#schedule(org.archive.crawler.datamodel.CrawlURI)
     */
    public void schedule(CrawlURI caURI) {
        batchSchedule(caURI);        
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#finished(org.archive.crawler.datamodel.CrawlURI)
     */
    public synchronized void finished(CrawlURI curi) {
        logger.fine(curi.toString()+ " " + 
                CrawlURI.fetchStatusCodesToString(curi.getFetchStatus()));
        curi.incrementFetchAttempts();
        logNonfatalErrors(curi);

        innerFinished(curi);
    }
    
    protected synchronized void innerFinished(CrawlURI curi) {
        try {
            innerBatchFlush();
            
            if (curi.isSuccess()) {
                successDisposition(curi);
            } else if (needsPromptRetry(curi)) {
                // Consider statuses which allow nearly-immediate retry
                // (like deferred to allow precondition to be fetched)
                reschedule(curi,false);
            } else if (needsRetrying(curi)) {
                // Consider errors which can be retried
                reschedule(curi,true);
                controller.fireCrawledURINeedRetryEvent(curi);
            } else if(isDisregarded(curi)) {
                // Check for codes that mean that while the crawler did
                // manage to get it it must be disregarded for any reason.
                disregardDisposition(curi);
            } else {
                // In that case FAILURE, note & log
                failureDisposition(curi);
            }

            // New items might be available, let waiting threads know
            // More then one queue might have become available due to 
            // scheduling of items outside the parent URIs host, so we
            // wake all waiting threads.
            notifyAll();
        } catch (RuntimeException e) {
            curi.setFetchStatus(S_RUNTIME_EXCEPTION);
            // store exception temporarily for logging
            logger.warning("RTE in innerFinished() " +
                e.getMessage());
            e.printStackTrace();
            curi.getData().put(A_RUNTIME_EXCEPTION, e);
            failureDisposition(curi);
        } catch (AttributeNotFoundException e) {
            logger.severe(e.getMessage());
        }
    }

    /**
     * Take note of any processor-local errors that have
     * been entered into the CrawlURI.
     * @param curi CrawlURI with errors.
     */
    private void logNonfatalErrors(CrawlURI curi) {
        if (curi.containsDataKey(A_NONFATAL_ERRORS)) {
        	Collection<Throwable> x = curi.getNonFatalFailures();
        	for (Throwable e: x) {
                    loggerModule.getNonfatalErrors()
                     .log(Level.WARNING, curi.toString(), e);
        	}
            // once logged, discard
            curi.getData().remove(A_NONFATAL_ERRORS);
        }
    }
    
    /**
     * The CrawlURI has been successfully crawled. 
     *
     * @param curi The CrawlURI
     */
    protected void successDisposition(CrawlURI curi) {
        curi.aboutToLog();
        Map<String,Object> cdata = curi.getData();

        long waitInterval = 0;
        
        if(curi.containsDataKey(A_WAIT_INTERVAL)){
            waitInterval = (Long)cdata.get(A_WAIT_INTERVAL);
            curi.getAnnotations().add("wt:" + 
                    ArchiveUtils.formatMillisecondsToConventional(
                            waitInterval));
        } else {
            logger.severe("Missing wait interval for " + curi.toString() +
                    " WaitEvaluator may be missing.");
        }
        if(curi.containsDataKey(A_NUMBER_OF_VISITS)){
            curi.getAnnotations().add(cdata.get(A_NUMBER_OF_VISITS) + "vis");
        }
        if(curi.containsDataKey(A_NUMBER_OF_VERSIONS)){
            curi.getAnnotations().add(cdata.get(A_NUMBER_OF_VERSIONS) + "ver");
        }
        if(curi.containsDataKey(A_FETCH_OVERDUE)){
            curi.getAnnotations().add("ov:" +
                    ArchiveUtils.formatMillisecondsToConventional(
                    (Long)cdata.get(A_FETCH_OVERDUE)));
        }
        
        Object array[] = { curi };
        loggerModule.getUriProcessing().log(
            Level.INFO,
            curi.getUURI().toString(),
            array);

        succeededFetchCount++;
        totalProcessedBytes += curi.getContentSize();

        // Let everyone know in case they want to do something before we strip
        // the curi.
        controller.fireCrawledURISuccessfulEvent(curi);
        
        curi.setSchedulingDirective(SchedulingConstants.NORMAL);

        // Set time of next processing
        cdata.put(A_TIME_OF_NEXT_PROCESSING, 
        		System.currentTimeMillis() + waitInterval);
        
        
        /* Update HQ */
        AdaptiveRevisitHostQueue hq = hostQueues.getHQ(curi.getClassKey());
        
        // Wake up time is based on the time when a fetch was completed + the
        // calculated snooze time for politeness. If the fetch completion time
        // is missing, we'll use current time.
        long wakeupTime = (curi.containsDataKey(A_FETCH_COMPLETED_TIME)?
                (Long)cdata.get(A_FETCH_COMPLETED_TIME):
                    (new Date()).getTime()) + calculateSnoozeTime(curi);
        
        // Ready the URI for reserialization.
        curi.processingCleanup(); 
        curi.resetDeferrals();   
        curi.resetFetchAttempts();
        try {
            hq.update(curi, true, wakeupTime);
        } catch (IOException e) {
            logger.severe("An IOException occured when updating " + 
                    curi.toString() + "\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Put near top of relevant hostQueue (but behind anything recently
     * scheduled 'high')..
     *
     * @param curi CrawlURI to reschedule. Its time of next processing is not
     *             modified.
     * @param errorWait signals if there should be a wait before retrying.
     * @throws AttributeNotFoundException
     */
    protected void reschedule(CrawlURI curi, boolean errorWait)
            throws AttributeNotFoundException {
        long delay = 0;
        if(errorWait){
            if(curi.containsDataKey(A_RETRY_DELAY)) {
                delay = (Long)curi.getData().get(A_RETRY_DELAY);
            } else {
                // use ARFrontier default
                delay = curi.get(this, RETRY_DELAY); 
            }
        }
        
        long retryTime = (curi.containsDataKey(A_FETCH_COMPLETED_TIME)?
                (Long)curi.getData().get(A_FETCH_COMPLETED_TIME):
                    (new Date()).getTime()) + delay;
        
        AdaptiveRevisitHostQueue hq = hostQueues.getHQ(curi.getClassKey());
        // Ready the URI for reserialization.
        curi.processingCleanup(); 
        if(errorWait){
            curi.resetDeferrals(); //Defferals only refer to immediate retries.
        }
        try {
            hq.update(curi, errorWait, retryTime);
        } catch (IOException e) {
            // TODO Handle IOException
            e.printStackTrace();
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
        loggerModule.getUriProcessing().log(
            Level.INFO,
            curi.getUURI().toString(),
            array);

        // if exception, also send to crawlErrors
        if (curi.getFetchStatus() == S_RUNTIME_EXCEPTION) {
            this.loggerModule.getRuntimeErrors().log(
                Level.WARNING,
                curi.getUURI().toString(),
                array);
        }
        failedFetchCount++;
        
        // Put the failed URI at the very back of the queue.
        curi.setSchedulingDirective(SchedulingConstants.NORMAL);
        // TODO: reconsider this
        curi.getData().put(A_TIME_OF_NEXT_PROCESSING, Long.MAX_VALUE);

        AdaptiveRevisitHostQueue hq = hostQueues.getHQ(curi.getClassKey());
        // Ready the URI for serialization.
        curi.processingCleanup();
        curi.resetDeferrals();
        curi.resetFetchAttempts();
        try {
            // No wait on failure. No contact was made with the server.
            boolean shouldForget = shouldBeForgotten(curi);
            if(shouldForget && alreadyIncluded != null){
                alreadyIncluded.forget(canonicalize(curi.getUURI()),curi);
            }
            hq.update(curi,false, 0, shouldForget); 
        } catch (IOException e) {
            // TODO Handle IOException
            e.printStackTrace();
        }
    }

    protected void disregardDisposition(CrawlURI curi) {
        //Let interested listeners know of disregard disposition.
        controller.fireCrawledURIDisregardEvent(curi);

        // send to basic log
        curi.aboutToLog();
        Object array[] = { curi };
        loggerModule.getUriProcessing().log(
            Level.INFO,
            curi.getUURI().toString(),
            array);

        disregardedUriCount++;
        
        // Todo: consider timout before retrying disregarded elements.
        //       Possibly add a setting to the WaitEvaluators?
        curi.getData().put(A_TIME_OF_NEXT_PROCESSING, Long.MAX_VALUE); 
        curi.setSchedulingDirective(SchedulingConstants.NORMAL);

        AdaptiveRevisitHostQueue hq = hostQueues.getHQ(curi.getClassKey());
        // Ready the URI for reserialization.
        curi.processingCleanup(); 
        curi.resetDeferrals();
        curi.resetFetchAttempts();
        try {
            // No politness wait on disregard. No contact was made with server
            hq.update(curi, false, 0, shouldBeForgotten(curi));
        } catch (IOException e) {
            // TODO Handle IOException
            e.printStackTrace();
        }
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
            case S_TOO_MANY_EMBED_HOPS:
            case S_TOO_MANY_LINK_HOPS:
                return true;
            default:
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
    protected boolean needsPromptRetry(CrawlURI curi)
            throws AttributeNotFoundException {
        if (curi.getFetchAttempts() >= curi.get(this, MAX_RETRIES)) {
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
        // Check to see if maximum number of retries has been exceeded.
        if (curi.getFetchAttempts() >= curi.get(this, MAX_RETRIES)) {
            return false;
        } else {
            // Check if FetchStatus indicates that a delayed retry is needed.
            switch (curi.getFetchStatus()) {
                case S_CONNECT_FAILED:
                case S_CONNECT_LOST:
                case S_DOMAIN_UNRESOLVABLE:
                    // these are all worth a retry
                    // TODO: consider if any others (S_TIMEOUT in some cases?) 
                    //       deserve retry
                    return true;
                default:
                    return false;
            }
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
     * Calculates how long a host queue needs to be snoozed following the
     * crawling of a URI.
     *
     * @param curi The CrawlURI
     * @return How long to snooze.
     */
    protected long calculateSnoozeTime(CrawlURI curi) {
        long durationToWait = 0;
        if (curi.containsDataKey(ModuleAttributeConstants.A_FETCH_BEGAN_TIME)
            && curi.containsDataKey(A_FETCH_COMPLETED_TIME)) {
            
            
                long completeTime = curi.getFetchCompletedTime();
                long durationTaken = 
                    (completeTime - curi.getFetchBeginTime());
                
                durationToWait = (long)(curi.get(this, DELAY_FACTOR) * durationTaken);
    
                long minDelay = curi.get(this, MIN_DELAY_MS);
                
                if (minDelay > durationToWait) {
                    // wait at least the minimum
                    durationToWait = minDelay;
                }
    
                long maxDelay = curi.get(this, MAX_DELAY_MS);
                if (durationToWait > maxDelay) {
                    // wait no more than the maximum
                    durationToWait = maxDelay;
                }

        }
        long ret = durationToWait > MIN_DELAY_MS.getDefaultValue() ? 
                durationToWait : MIN_DELAY_MS.getDefaultValue();
        logger.finest("Snooze time for " + curi.toString() + " = " + ret );
        return ret;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#discoveredUriCount()
     */
    public synchronized long discoveredUriCount() {
        return (this.alreadyIncluded != null) ? 
                this.alreadyIncluded.count() : hostQueues.getSize();
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#queuedUriCount()
     */
    public synchronized long queuedUriCount() {
        return hostQueues.getSize();
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#finishedUriCount()
     */
    public long finishedUriCount() {
        return succeededFetchCount+failedFetchCount+disregardedUriCount;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#succeededFetchCount()
     */
    public long succeededFetchCount() {
        return succeededFetchCount;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#failedFetchCount()
     */
    public long failedFetchCount() {
        return failedFetchCount;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#disregardedUriCount()
     */
    public long disregardedUriCount() {
        return disregardedUriCount++;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#totalBytesWritten()
     */
    public long totalBytesWritten() {
        return totalProcessedBytes;
    }

    /**
     * Method is not supported by this Frontier implementation..
     * @param params
     * @throws IOException
     */
    public void importURIs(String jsonParams) throws IOException {
        throw new IOException("Unsupported by this frontier.");
    }


    public synchronized CompositeData getURIsList(String marker,
            int numberOfMatches, String regex, boolean verbose) {
        return null;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#deleteURIs(java.lang.String)
     */
    public synchronized long deleteURIs(String queueRegex, String match) {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#deleted(org.archive.crawler.datamodel.CrawlURI)
     */
    public synchronized void deleted(CrawlURI curi) {
        // TODO Auto-generated method stub
    }

    public void considerIncluded(UURI u) {
        // This will cause the URI to be crawled!!!
        CrawlURI curi = new CrawlURI(u);
        innerSchedule(curi);

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
    }  

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#getFrontierJournal()
     */
    public FrontierJournal getFrontierJournal() {
        return null;
    }
    
    public DecideRule getScope() {
        return null;
    }

    private static class ThreadLocalQueue
    extends ThreadLocal<Queue<CrawlURI>> implements Serializable {

        private static final long serialVersionUID = 8268977225156462059L;

        protected Queue<CrawlURI> initialValue() {
            return new LinkedList<CrawlURI>();
        }

        /**
         * @return Queue of 'batched' items
         */
        public Queue<CrawlURI> getQueue() {
            return get();
        }
    }
    
    /**
     * This method is not supported by this Frontier implementation
     * @param pathToLog
     * @param retainFailures
     * @throws IOException
     */
    public void importRecoverLog(String pathToLog, boolean retainFailures)
    throws IOException {
        throw new IOException("Unsupported");
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
    
    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#oneLineReport()
     */
    public synchronized void singleLineReportTo(PrintWriter w) throws IOException {
        hostQueues.singleLineReportTo(w);
    }

    /* (non-Javadoc)
     * @see org.archive.util.Reporter#singleLineLegend()
     */
    public String singleLineLegend() {
        return hostQueues.singleLineLegend();
    }
    
    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#report()
     */
    public synchronized void reportTo(String name, PrintWriter writer) {
        // ignore name; only one report for now
        hostQueues.reportTo(name, writer);
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.event.CrawlStatusListener#crawlStarted(java.lang.String)
     */
    public void crawlStarted(String message) {
        // Not interested
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.event.CrawlStatusListener#crawlEnding(java.lang.String)
     */
    public void crawlEnding(String sExitMessage) {
        // Not interested
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.event.CrawlStatusListener#crawlEnded(java.lang.String)
     */
    public void crawlEnded(String sExitMessage) {
        // Cleanup!
        if (this.alreadyIncluded != null) {
            this.alreadyIncluded.close();
            this.alreadyIncluded = null;
        }
        hostQueues.close();
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.event.CrawlStatusListener#crawlPausing(java.lang.String)
     */
    public void crawlPausing(String statusMessage) {
        // Not interested
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.event.CrawlStatusListener#crawlPaused(java.lang.String)
     */
    public void crawlPaused(String statusMessage) {
        // Not interested
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.event.CrawlStatusListener#crawlResuming(java.lang.String)
     */
    public void crawlResuming(String statusMessage) {
        // Not interested
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.event.CrawlStatusListener#crawlCheckpoint(java.io.File)
     */
    public void crawlCheckpoint(StateProvider sp, File checkpointDir) 
    throws Exception {
        // Not interested
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.UriUniqFilter.HasUriReceiver#receive(org.archive.crawler.datamodel.CrawlURI)
     */
    public void receive(CrawlURI item) {
        System.out.println("Received " + item);
        innerSchedule(item);        
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#getGroup(org.archive.crawler.datamodel.CrawlURI)
     */
    public FrontierGroup getGroup(CrawlURI curi) {
        try {
            return getHQ(curi);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
    
    public long averageDepth() {
        return hostQueues.getAverageDepth();
    }
    
    public float congestionRatio() {
        return hostQueues.getCongestionRatio();
    }
    
    public long deepestUri() {
        return hostQueues.getDeepestQueueSize();
    }
    
    // good to keep at end of source: must run after all per-Key 
    // initialization values are set.
    static {
        KeyManager.addKeys(AdaptiveRevisitFrontier.class);
    }

    public void requestState(State target) {
        switch(target) {
        case HOLD:
        case PAUSE:
            pause();
            return;
        case RUN:
            unpause();
            return;
        case FINISH:
            terminate();
            return;
        }
    }
}
