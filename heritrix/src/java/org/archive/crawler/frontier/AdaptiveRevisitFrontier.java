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

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;

import org.apache.commons.httpclient.HttpStatus;
import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlServer;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.Frontier;
import org.archive.crawler.framework.FrontierMarker;
import org.archive.crawler.framework.exceptions.EndedException;
import org.archive.crawler.framework.exceptions.FatalConfigurationException;
import org.archive.crawler.framework.exceptions.InvalidFrontierMarkerException;
import org.archive.crawler.settings.ModuleType;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.Type;
import org.archive.queue.MemQueue;
import org.archive.queue.Queue;
import org.archive.util.ArchiveUtils;


/**
 * A Frontier that will repeatedly visit all encountered URIs. 
 * <p>
 * Wait time between visits is configurable and varies based on observed 
 * changes of documents.
 * <p>
 * The Frontier borrows many things from HostQueuesFrontier, but implements 
 * an entirely different strategy in issuing URIs and consequently in keeping a
 * record of discovered URIs.
 * <p>
 * NOTE: When serializing a CrawlURI, any data in the AList other then Strings,
 * longs and ints, will be discarded because they may not be serializable.
 *
 * @author Kristinn Sigurdsson
 */
public class AdaptiveRevisitFrontier extends ModuleType 
implements Frontier, FetchStatusCodes, CoreAttributeConstants,
        AdaptiveRevisitAttributeConstants {
    private static final Logger logger =
        Logger.getLogger(AdaptiveRevisitFrontier.class.getName());

    /** How many multiples of last fetch elapsed time to wait before recontacting
     * same server */
    public final static String ATTR_DELAY_FACTOR = "delay-factor";
    private final static Float DEFAULT_DELAY_FACTOR = new Float(5);
    
    /** Always wait this long after one completion before recontacting
     * same server, regardless of multiple */
    public final static String ATTR_MIN_DELAY = "min-delay-ms";

    // 2 seconds
    private final static Integer DEFAULT_MIN_DELAY = new Integer(2000);
    
    /** Never wait more than this long, regardless of multiple */
    public final static String ATTR_MAX_DELAY = "max-delay-ms";
    
    // 30 seconds
    private final static Integer DEFAULT_MAX_DELAY = new Integer(30000);
    
    /** Maximum times to emit a CrawlURI without final disposition */
    public final static String ATTR_MAX_RETRIES = "max-retries";
    private final static Integer DEFAULT_MAX_RETRIES = new Integer(30);

    /** For retryable problems, seconds to wait before a retry */
    public final static String ATTR_RETRY_DELAY = "retry-delay-seconds";
    
    // 15 minutes
    private final static Long DEFAULT_RETRY_DELAY = new Long(900);
    
    /** Maximum simultaneous requests in process to a host (queue) */
    public final static String ATTR_HOST_VALENCE = "host-valence";
    private final static Integer DEFAULT_HOST_VALENCE = new Integer(1); 

    /** Number of hops of embeds (ERX) to bump to front of host queue */
    public final static String ATTR_PREFERENCE_EMBED_HOPS =
        "preference-embed-hops";
    private final static Integer DEFAULT_PREFERENCE_EMBED_HOPS = new Integer(0); 

    private CrawlController controller;
    
    private AdaptiveRevisitQueueList hostQueues;

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
    

    public AdaptiveRevisitFrontier(String name) {
        this(name, "AdaptiveRevisitFrontier. EXPERIMENTAL Frontier that " +
                "will repeatedly visit all " +
                "encountered URIs. Wait time between visits is configurable" +
                " and varies based on observed changes of documents. " +
                "See documentation for ARFrontier limitations.");        
    }

    public AdaptiveRevisitFrontier(String name, String description) {
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
            addElementToDefinition(new SimpleType(ATTR_PREFERENCE_EMBED_HOPS,
                    "Number of embedded (or redirected) hops up to which " +
                    "a URI has higher priority scheduling. For example, if set" +
                    "to 1 (the default), items such as inline images (1-hop" +
                    "embedded resources) will be scheduled ahead of all regular" +
                    "links (or many-hop resources, like nested frames). If set to" +
                    "zero, no preferencing will occur, and embeds/redirects are" +
                    "scheduled the same as regular links.",
                    DEFAULT_PREFERENCE_EMBED_HOPS));
            Type t;
            t = addElementToDefinition(new SimpleType(ATTR_HOST_VALENCE,
                    "Maximum number of simultaneous requests to a single" +
                    " host.",
                    DEFAULT_HOST_VALENCE));
            t.setExpertSetting(true);

        // Register persistent CrawlURI items 
        CrawlURI.addAlistPersistentMember(A_CONTENT_STATE_KEY);
        CrawlURI.addAlistPersistentMember(A_TIME_OF_NEXT_PROCESSING);
    }

    public synchronized void initialize(CrawlController c)
            throws FatalConfigurationException, IOException {
        controller = c;
        queueAssignmentPolicy = new HostnameQueueAssignmentPolicy();
        
        hostQueues = new AdaptiveRevisitQueueList(c.getBdbEnvironment(),
            c.getClassCatalog());
        
        loadSeeds();
    }

    /**
     * Loads the seeds
     * <p>
     * This method is called by initialize() and kickUpdate()
     */
    private void loadSeeds() {
        Writer ignoredWriter = new StringWriter();
        // Get the seeds to refresh.
        Iterator iter = this.controller.getScope().seedsIterator(ignoredWriter);
        while (iter.hasNext()) {
            CandidateURI caUri =
                CandidateURI.createSeedCandidateURI((UURI)iter.next());
            caUri.setSchedulingDirective(CandidateURI.MEDIUM);
            innerSchedule(caUri);
        }
        // save ignored items (if any) where they can be consulted later
        AbstractFrontier.saveIgnoredItems(
                ignoredWriter.toString(), 
                controller.getDisk());
    }
    
    public String getClassKey(CandidateURI cauri) {
        return this.queueAssignmentPolicy.getClassKey(controller, cauri);
    }

    /**
     * 
     * @param caUri The URI to schedule.
     */
    protected void innerSchedule(CandidateURI caUri) {
        CrawlURI curi;
        if(caUri instanceof CrawlURI) {
            curi = (CrawlURI) caUri;
        } else {
            curi = CrawlURI.from(caUri,System.currentTimeMillis());
            curi.putLong(A_TIME_OF_NEXT_PROCESSING,
                System.currentTimeMillis());
            // New CrawlURIs get 'current time' as the time of next processing.
        }
        
        curi.setClassKey(getClassKey(curi));

        if(curi.isSeed() && curi.getVia() != null
                && curi.flattenVia().length() > 0) {
            // The only way a seed can have a non-empty via is if it is the
            // result of a seed redirect.  Add it to the seeds list.
            //
            // This is a feature.  This is handling for case where a seed
            // gets immediately redirected to another page.  What we're doing
            // is treating the immediate redirect target as a seed.
            this.controller.getScope().addSeed(curi.getUURI());
            // And it needs rapid scheduling.
            curi.setSchedulingDirective(CandidateURI.MEDIUM);
        }
        
        // Optionally preferencing embeds up to MEDIUM
        int prefHops = ((Integer) getUncheckedAttribute(curi,
                ATTR_PREFERENCE_EMBED_HOPS)).intValue();
        boolean prefEmbed = false;
        if (prefHops > 0) {
            int embedHops = curi.getTransHops();
            if (embedHops > 0 && embedHops <= prefHops
                    && curi.getSchedulingDirective() == CandidateURI.NORMAL) {
                // number of embed hops falls within the preferenced range, and
                // uri is not already MEDIUM -- so promote it
                curi.setSchedulingDirective(CandidateURI.MEDIUM);
                prefEmbed = true;
            }
        }

        // Finally, allow curi to be fetched right now 
        // (while not overriding overdue items)
        curi.putLong(A_TIME_OF_NEXT_PROCESSING,
                System.currentTimeMillis());
        
        try {
            logger.finest("scheduling " + curi.toString());
            AdaptiveRevisitHostQueue hq = hostQueues.getHQ(curi.getClassKey());
            if(hq == null){
                // Need to create it.
                int valence = DEFAULT_HOST_VALENCE.intValue();
                try {
                    valence = ((Integer)getAttribute(curi,ATTR_HOST_VALENCE)).intValue();
                } catch (AttributeNotFoundException e2) {
                    logger.severe("Unable to load valence.");
                }
                hq = hostQueues.createHQ(curi.getClassKey(),valence);
            }
            hq.add(curi,prefEmbed);
        } catch (IOException e) {
            // TODO Handle IOExceptions
            e.printStackTrace();
        }
        
    }

    protected void batchSchedule(CandidateURI caUri) {
        threadWaiting.getQueue().enqueue(caUri);
    }

    protected void batchFlush() {
        innerBatchFlush();
    }

    private void innerBatchFlush() {
        Queue q = threadWaiting.getQueue();
        while(!q.isEmpty()) {
            innerSchedule((CandidateURI)q.dequeue());
        }
    }
    
    /**
     * @param curi
     * @return the CrawlServer to be associated with this CrawlURI
     */
    protected CrawlServer getServer(CrawlURI curi) {
        return this.controller.getServerCache().getServerFor(curi);
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#next()
     */
    public synchronized CrawlURI next() 
            throws InterruptedException, EndedException {
        controller.checkFinish();
        
        while(shouldPause){
            controller.toePaused();
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
            hq = hostQueues.getTopHQ(); //A busy hq may have become 'unbusy'
        }             

        if(shouldTerminate){
            // May have been terminated while thread was waiting for IO
            throw new EndedException("terminated");
        }
        
        try {
            CrawlURI curi = hq.next();
            // Populate CURI with 'transient' variables such as server.
            logger.fine("Issuing " + curi.toString());
            long temp = curi.getLong(A_TIME_OF_NEXT_PROCESSING);
            long currT = System.currentTimeMillis();
            long overdue = (currT-temp);
            if(logger.isLoggable(Level.FINER)){
                String waitI = "not set";
                if(curi.containsKey(A_WAIT_INTERVAL)){
                    waitI = ArchiveUtils.formatMillisecondsToConventional(
                            curi.getLong(A_WAIT_INTERVAL));
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
            curi.putLong(A_FETCH_OVERDUE,overdue);
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
     * @see org.archive.crawler.framework.Frontier#schedule(org.archive.crawler.datamodel.CandidateURI)
     */
    public void schedule(CandidateURI caURI) {
        batchSchedule(caURI);        
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#finished(org.archive.crawler.datamodel.CrawlURI)
     */
    public synchronized void finished(CrawlURI curi) {
        logger.fine(curi.toString()+ " " + 
                CrawlURI.fetchStatusCodesToString(curi.getFetchStatus()));
        curi.incrementFetchAttempts();
        logLocalizedErrors(curi);

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
                controller.fireCrawledURINeedRetryEvent(curi); // Let everyone interested know that it will be retried.
            } else if(isDisregarded(curi)) {
                // Check for codes that mean that while the crawler did
                // manage to get it it must be disregarded for any reason.
                disregardDisposition(curi);
            } else {
                // In that case FAILURE, note & log
                failureDisposition(curi);
            }

            // New items might be available, let waiting threads know
            notify();
        } catch (RuntimeException e) {
            curi.setFetchStatus(S_RUNTIME_EXCEPTION);
            // store exception temporarily for logging
            logger.warning("RTE in innerFinished() " +
                e.getMessage());
            e.printStackTrace();
            curi.putObject(A_RUNTIME_EXCEPTION, e);
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
    private void logLocalizedErrors(CrawlURI curi) {
        if(curi.containsKey(A_LOCALIZED_ERRORS)) {
            List localErrors = (List)curi.getObject(A_LOCALIZED_ERRORS);
            Iterator iter = localErrors.iterator();
            while(iter.hasNext()) {
                Object array[] = {curi, iter.next()};
                controller.localErrors.log(Level.WARNING,
                    curi.getUURI().toString(), array);
            }
            // once logged, discard
            curi.remove(A_LOCALIZED_ERRORS);
        }
    }
    
    /**
     * The CrawlURI has been successfully crawled. 
     *
     * @param curi The CrawlURI
     */
    protected void successDisposition(CrawlURI curi) {
        totalProcessedBytes += curi.getContentSize();
        curi.aboutToLog();
        
        if(curi.containsKey(A_WAIT_INTERVAL)){
            curi.addAnnotation("wt:" + 
                    ArchiveUtils.formatMillisecondsToConventional(
                    (curi.getLong(A_WAIT_INTERVAL))));
        } else {
            logger.severe("Missing wait interval for " + curi.toString() +
                    " WaitEvaluator may be missing.");
        }
        if(curi.containsKey(A_NUMBER_OF_VISITS)){
            curi.addAnnotation(curi.getInt(A_NUMBER_OF_VISITS) + "vis");
        }
        if(curi.containsKey(A_NUMBER_OF_VERSIONS)){
            curi.addAnnotation(curi.getInt(A_NUMBER_OF_VERSIONS) + "ver");
        }
        if(curi.containsKey(A_FETCH_OVERDUE)){
            curi.addAnnotation("ov:" +
                    ArchiveUtils.formatMillisecondsToConventional(
                    (curi.getLong(A_FETCH_OVERDUE))));
        }
        
        Object array[] = { curi };
        controller.uriProcessing.log(
            Level.INFO,
            curi.getUURI().toString(),
            array);

        succeededFetchCount++;
        totalProcessedBytes += curi.getContentSize();

        // Let everyone know in case they want to do something before we strip
        // the curi.
        controller.fireCrawledURISuccessfulEvent(curi);
        
        curi.setSchedulingDirective(CandidateURI.NORMAL);

        long waitInterval = curi.getLong(A_WAIT_INTERVAL);

        // Set time of next processing
        curi.putLong(A_TIME_OF_NEXT_PROCESSING,
                System.currentTimeMillis()+waitInterval);
        
        
        /* Update HQ */
        AdaptiveRevisitHostQueue hq = hostQueues.getHQ(curi.getClassKey());
        // Ready the URI for reserialization.
        curi.processingCleanup(); 
        curi.resetDeferrals();   
        curi.resetFetchAttempts();
        try {
            hq.update(curi, true,
                (curi.containsKey(A_FETCH_COMPLETED_TIME)?
                        curi.getLong(A_FETCH_COMPLETED_TIME):
                        (new Date()).getTime()) +
                    calculateSnoozeTime(curi));
        } catch (IOException e) {
            logger.severe("An IOException occured when updating " + 
                    curi.toString() + "\n" + e.getMessage());
            e.printStackTrace();
        } catch (AttributeNotFoundException e) {
            logger.severe("Unable to locate fetch completion time for " + 
                    curi.toString());
        }
    }

    /**
     * Put near top of relevant hostQueue (but behind anything recently
     * scheduled 'high')..
     *
     * @param curi CrawlURI to reschedule.
     * @param errorWait signals if there should be a wait before retrying.
     * @throws AttributeNotFoundException
     */
    protected void reschedule(CrawlURI curi, boolean errorWait)
            throws AttributeNotFoundException {
        long delay = 0;
        if(errorWait){
            if(curi.containsKey(A_RETRY_DELAY)) {
                delay = curi.getLong(A_RETRY_DELAY);
            } else {
                // use overall default
                delay = ((Long)getAttribute(ATTR_RETRY_DELAY,curi)).longValue();
            }
        }
        
        AdaptiveRevisitHostQueue hq = hostQueues.getHQ(curi.getClassKey());
        // Ready the URI for reserialization.
        curi.processingCleanup(); 
        if(errorWait){
            curi.resetDeferrals(); //Defferals only refer to immediate retries.
        }
        try {
            hq.update(curi, errorWait, delay);
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
        failedFetchCount++;
        
        // Put the failed URI at the very back of the queue.
        curi.setSchedulingDirective(CandidateURI.NORMAL);
        // TODO: reconsider this
        curi.putLong(A_TIME_OF_NEXT_PROCESSING,Long.MAX_VALUE);

        AdaptiveRevisitHostQueue hq = hostQueues.getHQ(curi.getClassKey());
        // Ready the URI for serialization.
        curi.processingCleanup();
        curi.resetDeferrals();
        curi.resetFetchAttempts();
        try {
            // No wait on failure. No contact was made with the server.
            hq.update(curi,false, 0, shouldBeForgotten(curi)); 
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
        disregardedUriCount++;
        
        curi.putLong(A_TIME_OF_NEXT_PROCESSING,Long.MAX_VALUE); //Todo: consider timout before retrying disregarded elements.
        curi.setSchedulingDirective(CandidateURI.NORMAL);

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
            case S_BLOCKED_BY_USER:
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
     * @throws AttributeNotFoundException
     */
    protected long calculateSnoozeTime(CrawlURI curi)
    throws AttributeNotFoundException {
        long durationToWait = 0;
        if (curi.containsKey(A_FETCH_BEGAN_TIME)
            && curi.containsKey(A_FETCH_COMPLETED_TIME)) {

            long completeTime = curi.getLong(A_FETCH_COMPLETED_TIME);
            long durationTaken = 
                (completeTime - curi.getLong(A_FETCH_BEGAN_TIME));
            
            durationToWait = (long)(
                    ((Float) getAttribute(ATTR_DELAY_FACTOR, curi))
                        .floatValue() * durationTaken);

            long minDelay = 
                ((Integer) getAttribute(ATTR_MIN_DELAY, curi)).longValue();
            
            if (minDelay > durationToWait) {
                // wait at least the minimum
                durationToWait = minDelay;
            }

            long maxDelay = ((Integer) getAttribute(ATTR_MAX_DELAY, curi)).longValue();
            if (durationToWait > maxDelay) {
                // wait no more than the maximum
                durationToWait = maxDelay;
            }

        }
        long ret = durationToWait > 0 ? durationToWait : 0;
        logger.finest("Snooze time for " + curi.toString() + " = " + ret );
        return ret;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#discoveredUriCount()
     */
    public synchronized long discoveredUriCount() {
        return hostQueues.getSize();
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

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#oneLineReport()
     */
    public synchronized String oneLineReport() {
        return hostQueues.oneLineReport();
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#report()
     */
    public synchronized String report() {
        return hostQueues.report();
    }

    /**
     * Method is not supported by this Frontier implementation..
     * @param pathToLog
     * @throws IOException
     */
    public void importRecoverLog(String pathToLog) throws IOException {
        throw new IOException("Unsupported by this frontier.");
    }

    public synchronized FrontierMarker getInitialMarker(String regexpr,
            boolean inCacheOnly) {
        return null;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#getURIsList(org.archive.crawler.framework.FrontierMarker, int, boolean)
     */
    public synchronized ArrayList getURIsList(FrontierMarker marker,
            int numberOfMatches, boolean verbose)
    throws InvalidFrontierMarkerException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#deleteURIs(java.lang.String)
     */
    public synchronized long deleteURIs(String match) {
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

    public void kickUpdate() {
        loadSeeds();
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
        hostQueues.close();
    }  

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#getFrontierJournal()
     */
    public FrontierJournal getFrontierJournal() {
        return null;
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

}
