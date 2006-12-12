/* AbstractFrontier
 *
 * $Id$
 *
 * Created on Aug 17, 2004
 *
 * Copyright (C) 2004 Internet Archive.
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.httpclient.HttpStatus;
import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.datamodel.CrawlServer;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.event.CrawlStatusListener;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.Frontier;
import org.archive.crawler.framework.ToeThread;
import org.archive.crawler.framework.exceptions.EndedException;
import org.archive.crawler.framework.exceptions.FatalConfigurationException;
import org.archive.crawler.url.CanonicalizationRule;
import org.archive.crawler.url.Canonicalizer;
import org.archive.net.UURI;
import org.archive.processors.fetcher.CrawlHost;
import org.archive.settings.Sheet;
import org.archive.state.Key;
import org.archive.state.KeyMaker;
import org.archive.state.StateProvider;
import org.archive.util.ArchiveUtils;

/**
 * Shared facilities for Frontier implementations.
 * 
 * @author gojomo
 */
public abstract class AbstractFrontier 
implements CrawlStatusListener, Frontier, FetchStatusCodes,
        CoreAttributeConstants, Serializable {
    private static final Logger logger = Logger
            .getLogger(AbstractFrontier.class.getName());

    protected transient CrawlController controller;

    /** ordinal numbers to assign to created CrawlURIs */
    protected long nextOrdinal = 1;

    /** should the frontier hold any threads asking for URIs? */
    protected boolean shouldPause = false;

    /**
     * should the frontier send an EndedException to any threads asking for
     * URIs?
     */
    protected transient boolean shouldTerminate = false;

    /**
     * How many multiples of last fetch elapsed time to wait before recontacting
     * same server.
     */    
    final public static Key<Float> DELAY_FACTOR = Key.make((float)5);


    /**
     * always wait this long after one completion before recontacting same
     * server, regardless of multiple
     */
    final public static Key<Integer> MIN_DELAY = Key.make(3000);
    

    /** never wait more than this long, regardless of multiple */
    final public static Key<Integer> MAX_DELAY = Key.make(30000);
    

    /** number of hops of embeds (ERX) to bump to front of host queue */
    final public static Key<Integer> PREFERENCE_EMBED_HOPS = Key.make(1);


    /** maximum per-host bandwidth usage */
    final public static Key<Integer> MAX_PER_HOST_BANDWIDTH_USAGE_KB_SEC =
        Key.makeExpert(0);


    /** maximum overall bandwidth usage */
    final public static Key<Integer> TOTAL_BANDWIDTH_USAGE_KB_SEC =
        Key.makeFinal(0);


    /** for retryable problems, seconds to wait before a retry */
    final public static Key<Long> RETRY_DELAY_SECONDS = Key.make(900L);

    
    /** maximum times to emit a CrawlURI without final disposition */
    final public static Key<Integer> MAX_RETRIES = Key.make(30);


    /**
     * Defines how to assign URIs to queues. Can assign by host, by ip, and into
     * one of a fixed set of buckets (1k).
     */
    final public static Key<QueueAssignmentPolicy> QUEUE_ASSIGNMENT_POLICY =
        makeQAP();
    
    
    /** queue assignment to force onto CrawlURIs; intended to be overridden */
    final public static Key<String> FORCE_QUEUE_ASSIGNMENT = 
        Key.makeExpertFinal("");

    // word chars, dash, period, comma, colon
    protected final static String ACCEPTABLE_FORCE_QUEUE = "[-\\w\\.,:]*";

    
    /** whether pause, rather than finish, when crawl appears done */
    final public static Key<Boolean> PAUSE_AT_FINISH = Key.makeFinal(false);
    
    
    /** whether to pause at crawl start */
    final public static Key<Boolean> PAUSE_AT_START = Key.make(false);


    /**
     * Whether to tag seeds with their own URI as a heritable 'source' String,
     * which will be carried-forward to all URIs discovered on paths originating
     * from that seed. When present, such source tags appear in the
     * second-to-last crawl.log field.
     */
    final public static Key<Boolean> SOURCE_TAG_SEEDS = Key.makeFinal(false);


    /**
     * Recover log on or off attribute.
     */
    final public static Key<Boolean> RECOVERY_LOG_ENABLED = 
        Key.makeExpertFinal(true);


    // top-level stats
    protected long queuedUriCount = 0; // total URIs queued to be visited

    protected long succeededFetchCount = 0;

    protected long failedFetchCount = 0;

    protected long disregardedUriCount = 0; //URIs that are disregarded (for
                                          // example because of robot.txt rules)

    /**
     * Used when bandwidth constraint are used.
     */
    protected long totalProcessedBytes = 0;

    private transient long nextURIEmitTime = 0;

    protected long processedBytesAfterLastEmittedURI = 0;
    
    protected int lastMaxBandwidthKB = 0;

    /** Policy for assigning CrawlURIs to named queues */
    protected transient QueueAssignmentPolicy queueAssignmentPolicy = null;

    /**
     * Crawl replay logger.
     * 
     * Currently captures Frontier/URI transitions.
     * Can be null if user chose not to run a recovery.log.
     */
    private transient FrontierJournal recover = null;

    /** file collecting report of ignored seed-file entries (if any) */
    public static final String IGNORED_SEEDS_FILENAME = "seeds.ignored";

    /**
     * @param name Name of this frontier.
     * @param description Description for this frontier.
     */
    public AbstractFrontier() {
    }
    
    
    public <T> T get(Key<T> key) {
        Sheet sheet = controller.getSheetManager().getDefault();
        return sheet.get(this, key);
    }

    public void start() {
        if (get(PAUSE_AT_START)) {
            // trigger crawl-wide pause
            controller.requestCrawlPause();
        } else {
            // simply begin
            unpause(); 
        }
    }
    
    synchronized public void pause() {
        shouldPause = true;
    }

    synchronized public void unpause() {
        shouldPause = false;
        notifyAll();
    }

    public void initialize(CrawlController c)
            throws FatalConfigurationException, IOException {
        c.addCrawlStatusListener(this);
        File logsDisk = null;
        logsDisk = c.getSettingsDir(CrawlOrder.LOGS_PATH);
        if (logsDisk != null) {
            String logsPath = logsDisk.getAbsolutePath() + File.separatorChar;
            if (get(RECOVERY_LOG_ENABLED)) {
                this.recover = new RecoveryJournal(logsPath,
                    FrontierJournal.LOGNAME_RECOVER);
            }
        }
        queueAssignmentPolicy = get(QUEUE_ASSIGNMENT_POLICY);
    }

    synchronized public void terminate() {
        shouldTerminate = true;
        if (this.recover != null) {
            this.recover.close();
            this.recover = null;
        }
        unpause();
    }

    protected void doJournalFinishedSuccess(CrawlURI c) {
        if (this.recover != null) {
            this.recover.finishedSuccess(c);
        }
    }

    protected void doJournalAdded(CrawlURI c) {
        if (this.recover != null) {
            this.recover.added(c);
        }
    }

    protected void doJournalRescheduled(CrawlURI c) {
        if (this.recover != null) {
            this.recover.rescheduled(c);
        }
    }

    protected void doJournalFinishedFailure(CrawlURI c) {
        if (this.recover != null) {
            this.recover.finishedFailure(c);
        }
    }

    protected void doJournalEmitted(CrawlURI c) {
        if (this.recover != null) {
            this.recover.emitted(c);
        }
    }

    /**
     * Frontier is empty only if all queues are empty and no URIs are in-process
     * 
     * @return True if queues are empty.
     */
    public synchronized boolean isEmpty() {
        return queuedUriCount == 0;
    }

    /**
     * Increment the running count of queued URIs. Synchronized because
     * operations on longs are not atomic.
     */
    protected synchronized void incrementQueuedUriCount() {
        queuedUriCount++;
    }

    /**
     * Increment the running count of queued URIs. Synchronized because
     * operations on longs are not atomic.
     * 
     * @param increment
     *            amount to increment the queued count
     */
    protected synchronized void incrementQueuedUriCount(long increment) {
        queuedUriCount += increment;
    }

    /**
     * Note that a number of queued Uris have been deleted.
     * 
     * @param numberOfDeletes
     */
    protected synchronized void decrementQueuedCount(long numberOfDeletes) {
        queuedUriCount -= numberOfDeletes;
    }

    /**
     * (non-Javadoc)
     * 
     * @see org.archive.crawler.framework.Frontier#queuedUriCount()
     */
    public long queuedUriCount() {
        return queuedUriCount;
    }

    /**
     * (non-Javadoc)
     * 
     * @see org.archive.crawler.framework.Frontier#finishedUriCount()
     */
    public long finishedUriCount() {
        return succeededFetchCount + failedFetchCount + disregardedUriCount;
    }

    /**
     * Increment the running count of successfully fetched URIs. Synchronized
     * because operations on longs are not atomic.
     */
    protected synchronized void incrementSucceededFetchCount() {
        succeededFetchCount++;
    }

    /**
     * (non-Javadoc)
     * 
     * @see org.archive.crawler.framework.Frontier#succeededFetchCount()
     */
    public long succeededFetchCount() {
        return succeededFetchCount;
    }

    /**
     * Increment the running count of failed URIs. Synchronized because
     * operations on longs are not atomic.
     */
    protected synchronized void incrementFailedFetchCount() {
        failedFetchCount++;
    }

    /**
     * (non-Javadoc)
     * 
     * @see org.archive.crawler.framework.Frontier#failedFetchCount()
     */
    public long failedFetchCount() {
        return failedFetchCount;
    }

    /**
     * Increment the running count of disregarded URIs. Synchronized because
     * operations on longs are not atomic.
     */
    protected synchronized void incrementDisregardedUriCount() {
        disregardedUriCount++;
    }

    public long disregardedUriCount() {
        return disregardedUriCount;
    }

    public long totalBytesWritten() {
        return totalProcessedBytes;
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
        Writer ignoredWriter = new StringWriter();
        logger.info("beginning");
        // Get the seeds to refresh.
        Iterator iter = this.controller.getScope().seedsIterator(ignoredWriter);
        int count = 0; 
        while (iter.hasNext()) {
            UURI u = (UURI)iter.next();
            CandidateURI caUri = CandidateURI.createSeedCandidateURI(u);
            caUri.setSchedulingDirective(CandidateURI.MEDIUM);
            if (get(SOURCE_TAG_SEEDS)) {
                caUri.setSourceTag(caUri.toString());
            }
            schedule(caUri);
            count++;
            if(count%1000==0) {
                logger.info(count+" seeds");
            }
        }
        // save ignored items (if any) where they can be consulted later
        saveIgnoredItems(ignoredWriter.toString(), controller.getDisk());
        logger.info("finished");
    }

    /**
     * Dump ignored seed items (if any) to disk; delete file otherwise.
     * Static to allow non-derived sibling classes (frontiers not yet 
     * subclassed here) to reuse.
     * 
     * @param ignoredItems
     * @param dir 
     */
    public static void saveIgnoredItems(String ignoredItems, File dir) {
        File ignoredFile = new File(dir, IGNORED_SEEDS_FILENAME);
        if(ignoredItems==null | ignoredItems.length()>0) {
            try {
                BufferedWriter bw = new BufferedWriter(new FileWriter(ignoredFile));
                bw.write(ignoredItems);
                bw.close();
            } catch (IOException e) {
                // TODO make an alert?
                e.printStackTrace();
            }
        } else {
            // delete any older file (if any)
            ignoredFile.delete();
        }
    }

    protected CrawlURI asCrawlUri(CandidateURI caUri) {
        CrawlURI curi;
        if (caUri instanceof CrawlURI) {
            curi = (CrawlURI)caUri;
        } else {
            curi = CrawlURI.from(caUri, nextOrdinal++);
        }
        curi.setClassKey(getClassKey(curi));
        return curi;
    }

    /**
     * @param now
     * @throws InterruptedException
     * @throws EndedException
     */
    protected synchronized void preNext(long now) throws InterruptedException,
            EndedException {
        if (this.controller == null) {
            return;
        }
        
        // Check completion conditions
        if (this.controller.atFinish()) {
            if (get(PAUSE_AT_FINISH)) {
                this.controller.requestCrawlPause();
            } else {
                this.controller.beginCrawlStop();
            }
        }

        // enforce operator pause
        if (shouldPause) {
            while (shouldPause) {
                this.controller.toePaused();
                wait();
            }
            // exitted pause; possibly finish regardless of pause-at-finish
            if (controller != null && controller.atFinish()) {
                this.controller.beginCrawlStop();
            }
        }

        // enforce operator terminate or thread retirement
        if (shouldTerminate
                || ((ToeThread)Thread.currentThread()).shouldRetire()) {
            throw new EndedException("terminated");
        }

        enforceBandwidthThrottle(now);
    }

    /**
     * Perform any special handling of the CrawlURI, such as promoting its URI
     * to seed-status, or preferencing it because it is an embed.
     * 
     * @param curi
     */
    protected void applySpecialHandling(CrawlURI curi) {
        if (curi.isSeed() && curi.getVia() != null
                && curi.flattenVia().length() > 0) {
            // The only way a seed can have a non-empty via is if it is the
            // result of a seed redirect. Add it to the seeds list.
            //
            // This is a feature. This is handling for case where a seed
            // gets immediately redirected to another page. What we're doing is
            // treating the immediate redirect target as a seed.
            this.controller.getScope().addSeed(curi);
            // And it needs rapid scheduling.
	    if (curi.getSchedulingDirective() == CandidateURI.NORMAL)
                curi.setSchedulingDirective(CandidateURI.MEDIUM);
        }

        // optionally preferencing embeds up to MEDIUM
        int prefHops = get(PREFERENCE_EMBED_HOPS); 
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
     * Perform fixups on a CrawlURI about to be returned via next().
     * 
     * @param curi
     *            CrawlURI about to be returned by next()
     * @param q
     *            the queue from which the CrawlURI came
     */
    protected void noteAboutToEmit(CrawlURI curi, WorkQueue q) {
        curi.setHolder(q);
        // if (curi.getServer() == null) {
        //    // TODO: perhaps short-circuit the emit here,
        //    // because URI will be rejected as unfetchable
        // }
        doJournalEmitted(curi);
    }

    /**
     * @param curi
     * @return the CrawlServer to be associated with this CrawlURI
     */
    protected CrawlServer getServer(CrawlURI curi) {
        return this.controller.getServerCache().getServerFor(curi.toString());
    }

    /**
     * Return a suitable value to wait before retrying the given URI.
     * 
     * @param curi
     *            CrawlURI to be retried
     * @return millisecond delay before retry
     */
    protected long retryDelayFor(CrawlURI curi) {
        int status = curi.getFetchStatus();
        return (status == S_CONNECT_FAILED || status == S_CONNECT_LOST ||
                status == S_DOMAIN_UNRESOLVABLE)? get(RETRY_DELAY_SECONDS) : 0;
                // no delay for most
    }

    /**
     * Update any scheduling structures with the new information in this
     * CrawlURI. Chiefly means make necessary arrangements for no other URIs at
     * the same host to be visited within the appropriate politeness window.
     * 
     * @param curi
     *            The CrawlURI
     * @return millisecond politeness delay
     */
    protected long politenessDelayFor(CrawlURI curi) {
        long durationToWait = 0;
        Map<String,Object> cdata = curi.getData();
        if (cdata.containsKey(A_FETCH_BEGAN_TIME)
                && cdata.containsKey(A_FETCH_COMPLETED_TIME)) {

            long completeTime = curi.getFetchCompletedTime();
            long durationTaken = (completeTime - curi.getFetchBeginTime());
            durationToWait = (long)(get(DELAY_FACTOR) * durationTaken);

            long minDelay = get(MIN_DELAY);
            if (minDelay > durationToWait) {
                // wait at least the minimum
                durationToWait = minDelay;
            }

            long maxDelay = get(MAX_DELAY);
            if (durationToWait > maxDelay) {
                // wait no more than the maximum
                durationToWait = maxDelay;
            }

            long now = System.currentTimeMillis();
            int maxBandwidthKB = get(MAX_PER_HOST_BANDWIDTH_USAGE_KB_SEC);
            if (maxBandwidthKB > 0) {
                // Enforce bandwidth limit
                CrawlHost host = curi.getCrawlHost();
                long minDurationToWait = host.getEarliestNextURIEmitTime()
                        - now;
                float maxBandwidth = maxBandwidthKB * 1.024F; // kilo factor
                long processedBytes = curi.getContentSize();
                host
                        .setEarliestNextURIEmitTime((long)(processedBytes / maxBandwidth)
                                + now);

                if (minDurationToWait > durationToWait) {
                    durationToWait = minDurationToWait;
                }
            }
        }
        return durationToWait;
    }

    /**
     * Ensure that any overall-bandwidth-usage limit is respected, by pausing as
     * long as necessary.
     * 
     * @param now
     * @throws InterruptedException
     */
    private void enforceBandwidthThrottle(long now) throws InterruptedException {
        int maxBandwidthKB = get(TOTAL_BANDWIDTH_USAGE_KB_SEC);
        if (maxBandwidthKB > 0) {
            // Make sure that new bandwidth setting doesn't affect total crawl
            if (maxBandwidthKB != lastMaxBandwidthKB) {
                lastMaxBandwidthKB = maxBandwidthKB;
                processedBytesAfterLastEmittedURI = totalProcessedBytes;
            }

            // Enforce bandwidth limit
            long sleepTime = nextURIEmitTime - now;
            float maxBandwidth = maxBandwidthKB * 1.024F; // Kilo_factor
            long processedBytes = totalProcessedBytes
                    - processedBytesAfterLastEmittedURI;
            long shouldHaveEmittedDiff = nextURIEmitTime == 0? 0
                    : nextURIEmitTime - now;
            nextURIEmitTime = (long)(processedBytes / maxBandwidth) + now
                    + shouldHaveEmittedDiff;
            processedBytesAfterLastEmittedURI = totalProcessedBytes;
            if (sleepTime > 0) {
                long targetTime = now + sleepTime;
                now = System.currentTimeMillis();
                while (now < targetTime) {
                    synchronized (this) {
                        if (logger.isLoggable(Level.FINE)) {
                            logger.fine("Frontier waits for: " + sleepTime
                                    + "ms to respect bandwidth limit.");
                        }
                        // TODO: now that this is a wait(), frontier can
                        // still schedule and finish items while waiting,
                        // which is good, but multiple threads could all
                        // wait for the same wakeTime, which somewhat
                        // spoils the throttle... should be fixed.
                        wait(targetTime - now);
                    }
                    now = System.currentTimeMillis();
                }
            }
        }
    }

    /**
     * Take note of any processor-local errors that have been entered into the
     * CrawlURI.
     * 
     * @param curi
     *  
     */
    protected void logLocalizedErrors(CrawlURI curi) {
        if (curi.containsDataKey(A_LOCALIZED_ERRORS)) {
        	Collection<Throwable> x = curi.getNonFatalFailures();
        	for (Throwable e: x) {
        		controller.localErrors.log(Level.WARNING, curi.toString(), e);
        	}
            // once logged, discard
            curi.getData().remove(A_LOCALIZED_ERRORS);
        }
    }

    /**
     * Utility method to return a scratch dir for the given key's temp files.
     * Every key gets its own subdir. To avoid having any one directory with
     * thousands of files, there are also two levels of enclosing directory
     * named by the least-significant hex digits of the key string's java
     * hashcode.
     * 
     * @param key
     * @return File representing scratch directory
     */
    protected File scratchDirFor(String key) {
        String hex = Integer.toHexString(key.hashCode());
        while (hex.length() < 4) {
            hex = "0" + hex;
        }
        int len = hex.length();
        return new File(this.controller.getStateDisk(), hex.substring(len - 2,
                len)
                + File.separator
                + hex.substring(len - 4, len - 2)
                + File.separator + key);
    }

    protected boolean overMaxRetries(CrawlURI curi) {
        // never retry more than the max number of times
        if (curi.getFetchAttempts() >= get(MAX_RETRIES)) {
            return true;
        }
        return false;
    }

    public void importRecoverLog(String pathToLog, boolean retainFailures)
            throws IOException {
        File source = new File(pathToLog);
        if (!source.isAbsolute()) {
            source = new File(controller.getDisk(), pathToLog);
        }
        RecoveryJournal.importRecoverLog(source, this, retainFailures);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.archive.crawler.framework.URIFrontier#kickUpdate()
     */
    public void kickUpdate() {
        // by default, do nothing
        // (scope will loadSeeds, if appropriate)
    }

    /**
     * Log to the main crawl.log
     * 
     * @param curi
     */
    protected void log(CrawlURI curi) {
        curi.aboutToLog();
        Object array[] = {curi};
        this.controller.uriProcessing.log(Level.INFO,
                curi.getUURI().toString(), array);
    }

    protected boolean isDisregarded(CrawlURI curi) {
        switch (curi.getFetchStatus()) {
        case S_ROBOTS_PRECLUDED: // they don't want us to have it
        case S_BLOCKED_BY_CUSTOM_PROCESSOR:
        case S_OUT_OF_SCOPE: // filtered out by scope
        case S_BLOCKED_BY_USER: // filtered out by user
        case S_TOO_MANY_EMBED_HOPS: // too far from last true link
        case S_TOO_MANY_LINK_HOPS: // too far from seeds
        case S_DELETED_BY_USER: // user deleted
            return true;
        default:
            return false;
        }
    }

    /**
     * Checks if a recently completed CrawlURI that did not finish successfully
     * needs to be retried (processed again after some time elapses)
     * 
     * @param curi
     *            The CrawlURI to check
     * @return True if we need to retry.
     */
    protected boolean needsRetrying(CrawlURI curi) {
        if (overMaxRetries(curi)) {
            return false;
        }

        switch (curi.getFetchStatus()) {
        case HttpStatus.SC_UNAUTHORIZED:
            // We can get here though usually a positive status code is
            // a success. We get here if there is rfc2617 credential data
            // loaded and we're supposed to go around again. See if any
            // rfc2617 credential present and if there, assume it got
            // loaded in FetchHTTP on expectation that we're to go around
            // again. If no rfc2617 loaded, we should not be here.
            boolean loaded = curi.hasRfc2617CredentialAvatar();
            if (!loaded && logger.isLoggable(Level.INFO)) {
                logger.info("Have 401 but no creds loaded " + curi);
            }
            return loaded;
        case S_DEFERRED:
        case S_CONNECT_FAILED:
        case S_CONNECT_LOST:
        case S_DOMAIN_UNRESOLVABLE:
            // these are all worth a retry
            // TODO: consider if any others (S_TIMEOUT in some cases?) deserve
            // retry
            return true;
        default:
            return false;
        }
    }

    /**
     * Canonicalize passed uuri. Its would be sweeter if this canonicalize
     * function was encapsulated by that which it canonicalizes but because
     * settings change with context -- i.e. there may be overrides in operation
     * for a particular URI -- its not so easy; Each CandidateURI would need a
     * reference to the settings system. That's awkward to pass in.
     * 
     * @param uuri Candidate URI to canonicalize.
     * @return Canonicalized version of passed <code>uuri</code>.
     */
    protected String canonicalize(UURI uuri) {
        StateProvider def = controller.getSheetManager().getDefault();
        List<CanonicalizationRule> rules = 
            controller.getOrderSetting(CrawlOrder.RULES);
        return Canonicalizer.canonicalize(def, uuri.toString(), rules);
    }

    /**
     * Canonicalize passed CandidateURI. This method differs from
     * {@link #canonicalize(UURI)} in that it takes a look at
     * the CandidateURI context possibly overriding any canonicalization effect if
     * it could make us miss content. If canonicalization produces an URL that
     * was 'alreadyseen', but the entry in the 'alreadyseen' database did
     * nothing but redirect to the current URL, we won't get the current URL;
     * we'll think we've already see it. Examples would be archive.org
     * redirecting to www.archive.org or the inverse, www.netarkivet.net
     * redirecting to netarkivet.net (assuming stripWWW rule enabled).
     * <p>Note, this method under circumstance sets the forceFetch flag.
     * 
     * @param cauri CandidateURI to examine.
     * @return Canonicalized <code>cacuri</code>.
     */
    protected String canonicalize(CandidateURI cauri) {
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
     * @param cauri CrawlURI we're to get a key for.
     * @return a String token representing a queue
     */
    public String getClassKey(CandidateURI cauri) {
        String queueKey = get(FORCE_QUEUE_ASSIGNMENT);
        if ("".equals(queueKey)) {
            // Typical case, barring overrides
            queueKey =
                queueAssignmentPolicy.getClassKey(this.controller, cauri);
        }
        return queueKey;
    }

    /**
     * @return RecoveryJournal instance.  May be null.
     */
    public FrontierJournal getFrontierJournal() {
        return this.recover;
    }

    public void crawlEnding(String sExitMessage) {
        // TODO Auto-generated method stub
    }

    public void crawlEnded(String sExitMessage) {
        if (logger.isLoggable(Level.INFO)) {
            logger.info("Closing with " + Long.toString(queuedUriCount()) +
                " urls still in queue.");
        }
    }

    public void crawlStarted(String message) {
        // TODO Auto-generated method stub
    }

    public void crawlPausing(String statusMessage) {
        // TODO Auto-generated method stub
    }

    public void crawlPaused(String statusMessage) {
        // TODO Auto-generated method stub
    }

    public void crawlResuming(String statusMessage) {
        // TODO Auto-generated method stub
    }
    
    public void crawlCheckpoint(StateProvider context, File checkpointDir)
    throws Exception {
        if (this.recover == null) {
            return;
        }
        this.recover.checkpoint(checkpointDir);
    }
    
    //
    // Reporter implementation
    // 
    public String singleLineReport() {
        return ArchiveUtils.singleLineReport(this);
    }

    public void reportTo(PrintWriter writer) {
        reportTo(null, writer);
    }
    
    
    private static Key<QueueAssignmentPolicy> makeQAP() {
        KeyMaker<QueueAssignmentPolicy> km = new KeyMaker<QueueAssignmentPolicy>();
        km.type = QueueAssignmentPolicy.class;
        km.def = new HostnameQueueAssignmentPolicy();
        km.overrideable = false;
        km.expert = true;
        return km.toKey();
    }

}
