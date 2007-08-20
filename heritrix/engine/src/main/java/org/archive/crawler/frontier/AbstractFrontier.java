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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.httpclient.HttpStatus;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.SchedulingConstants;

import static org.archive.crawler.datamodel.CoreAttributeConstants.*;
import static org.archive.modules.fetcher.FetchStatusCodes.*;

import org.archive.crawler.event.CrawlStatusListener;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.CrawlerLoggerModule;
import org.archive.crawler.framework.Frontier;
import org.archive.crawler.framework.ToeThread;
import org.archive.crawler.framework.exceptions.EndedException;
import org.archive.crawler.scope.SeedModule;
import org.archive.crawler.scope.SeedRefreshListener;
import org.archive.crawler.url.CanonicalizationRule;
import org.archive.crawler.url.Canonicalizer;
import org.archive.modules.net.CrawlHost;
import org.archive.modules.net.CrawlServer;
import org.archive.modules.net.ServerCache;
import org.archive.modules.net.ServerCacheUtil;
import org.archive.net.UURI;
import org.archive.openmbeans.annotations.Bean;
import org.archive.settings.CheckpointRecovery;
import org.archive.settings.Sheet;
import org.archive.settings.SheetManager;
import org.archive.state.FileModule;
import org.archive.state.Expert;
import org.archive.state.Global;
import org.archive.state.Immutable;
import org.archive.state.Initializable;
import org.archive.state.Key;
import org.archive.state.StateProvider;
import org.archive.util.ArchiveUtils;

/**
 * Shared facilities for Frontier implementations.
 * 
 * @author gojomo
 */
public abstract class AbstractFrontier extends Bean
implements CrawlStatusListener, Frontier, Serializable, Initializable, SeedRefreshListener {
    private static final long serialVersionUID = 555881755284996860L;

    private static final Logger logger = Logger
            .getLogger(AbstractFrontier.class.getName());

    protected CrawlController controller;
    protected CrawlerLoggerModule loggerModule;

    /** ordinal numbers to assign to created CrawlURIs */
    protected AtomicLong nextOrdinal = new AtomicLong(1);

    /** should the frontier hold any threads asking for URIs? */
    protected boolean shouldPause = false;

    /**
     * should the frontier send an EndedException to any threads asking for
     * URIs?
     */
    protected transient boolean shouldTerminate = false;


    @Immutable
    final public static Key<FileModule> SCRATCH_DIR = 
        Key.make(FileModule.class, null);
    

    @Immutable
    final public static Key<FileModule> RECOVERY_DIR =
        Key.make(FileModule.class, null);
    
    
    
    /**
     * How many multiples of last fetch elapsed time to wait before recontacting
     * same server.
     */    
    final public static Key<Float> DELAY_FACTOR = Key.make((float)5);


    /**
     * always wait this long after one completion before recontacting same
     * server, regardless of multiple
     */
    final public static Key<Integer> MIN_DELAY_MS = Key.make(3000);
    

    /** never wait more than this long, regardless of multiple */
    final public static Key<Integer> MAX_DELAY_MS = Key.make(30000);
    

    /** number of hops of embeds (ERX) to bump to front of host queue */
    final public static Key<Integer> PREFERENCE_EMBED_HOPS = Key.make(1);


    /** maximum per-host bandwidth usage */
    @Expert
    final public static Key<Integer> MAX_PER_HOST_BANDWIDTH_USAGE_KB_SEC =
        Key.make(0);


    /** maximum overall bandwidth usage */
    @Global
    final public static Key<Integer> TOTAL_BANDWIDTH_USAGE_KB_SEC =
        Key.make(0);


    /** for retryable problems, seconds to wait before a retry */
    final public static Key<Long> RETRY_DELAY_SECONDS = Key.make(900L);

    
    /** maximum times to emit a CrawlURI without final disposition */
    final public static Key<Integer> MAX_RETRIES = Key.make(30);


    /** queue assignment to force onto CrawlURIs; intended to be overridden */
    @Immutable @Expert
    final public static Key<String> FORCE_QUEUE_ASSIGNMENT = 
        Key.make("");

    // word chars, dash, period, comma, colon
    protected final static String ACCEPTABLE_FORCE_QUEUE = "[-\\w\\.,:]*";

    
    /** whether pause, rather than finish, when crawl appears done */
    @Immutable
    final public static Key<Boolean> PAUSE_AT_FINISH = Key.make(false);
    
    
    /** whether to pause at crawl start */
    @Immutable
    final public static Key<Boolean> PAUSE_AT_START = Key.make(false);


    /**
     * Whether to tag seeds with their own URI as a heritable 'source' String,
     * which will be carried-forward to all URIs discovered on paths originating
     * from that seed. When present, such source tags appear in the
     * second-to-last crawl.log field.
     */
    @Immutable
    final public static Key<Boolean> SOURCE_TAG_SEEDS = Key.make(false);


    /**
     * Recover log on or off attribute.
     */
    @Immutable @Expert
    final public static Key<Boolean> RECOVERY_LOG_ENABLED = Key.make(true);


    // top-level stats
    /** total URIs queued to be visited */
    protected AtomicLong queuedUriCount = new AtomicLong(0); 

    protected AtomicLong succeededFetchCount = new AtomicLong(0);

    protected AtomicLong failedFetchCount = new AtomicLong(0);

    /** URIs that are disregarded (for example because of robot.txt rules */
    protected AtomicLong disregardedUriCount = new AtomicLong(0);
    
    /**
     * Used when bandwidth constraint are used.
     */
    protected long totalProcessedBytes = 0;

    private transient long nextURIEmitTime = 0;

    protected long processedBytesAfterLastEmittedURI = 0;
    
    protected int lastMaxBandwidthKB = 0;

    /**
     * Crawl replay logger.
     * 
     * Currently captures Frontier/URI transitions.
     * Can be null if user chose not to run a recovery.log.
     */
    private transient FrontierJournal recover = null;
    
    protected SheetManager manager;
    
    /** file collecting report of ignored seed-file entries (if any) */
    public static final String IGNORED_SEEDS_FILENAME = "seeds.ignored";

    
    /**
     * The crawl controller using this Frontier.
     */
    @Immutable
    final public static Key<CrawlController> CONTROLLER = 
        Key.makeAuto(CrawlController.class);
    
    
    @Immutable
    final public static Key<CrawlerLoggerModule> LOGGER_MODULE =
        Key.makeAuto(CrawlerLoggerModule.class);

    
    @Immutable
    final public static Key<SeedModule> SEEDS = 
        Key.makeAuto(SeedModule.class);
    
    
    /**
     * Ordered list of url canonicalization rules.  Rules are applied in the 
     * order listed from top to bottom.
     */
    @Immutable
    final public static Key<List<CanonicalizationRule>> RULES = 
        Key.makeList(CanonicalizationRule.class);
    
    @Immutable
    final public static Key<SheetManager> MANAGER =
        Key.makeAuto(SheetManager.class);

    
    private FileModule scratchDir;
    private FileModule recoveryDir;


    /**
     * Defines how to assign URIs to queues. Can assign by host, by ip, 
     * by SURT-ordered authority, by SURT-ordered authority truncated to 
     * a topmost-assignable domain, and into one of a fixed set of buckets 
     * (1k).
     */
    final public static Key<QueueAssignmentPolicy> QUEUE_ASSIGNMENT_POLICY =
        Key.make(QueueAssignmentPolicy.class, 
                new SurtAuthorityQueueAssignmentPolicy());
    
    /**
     * @param name Name of this frontier.
     * @param description Description for this frontier.
     */
    public AbstractFrontier() {
        super(Frontier.class);
    }

    
    public void initialTasks(StateProvider provider) {
        this.scratchDir = provider.get(this, SCRATCH_DIR);
        this.recoveryDir = provider.get(this, RECOVERY_DIR);
        this.controller = provider.get(this, CONTROLLER);
        this.loggerModule = provider.get(this, LOGGER_MODULE);
        this.manager = provider.get(this, MANAGER);
        
        SeedModule seeds = provider.get(this, SEEDS);
        seeds.addSeedRefreshListener(this);
        
        if (provider.get(this, RECOVERY_LOG_ENABLED)) try {
            initJournal(loggerModule.getLogsDir().getAbsolutePath());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        pause();
    }
    
    private void initJournal(String logsDisk) throws IOException {
        if (logsDisk != null) {
            String logsPath = logsDisk + File.separatorChar;
            this.recover = new RecoveryJournal(logsPath,
                    FrontierJournal.LOGNAME_RECOVER);
        }
    }
    
    
    public <T> T get(Key<T> key) {
        return manager.getGlobalSheet().get(this, key);
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
    public boolean isEmpty() {
        return queuedUriCount.get() == 0;
    }

    /**
     * Increment the running count of queued URIs. 
     */
    protected void incrementQueuedUriCount() {
        queuedUriCount.incrementAndGet();
    }

    /**
     * Increment the running count of queued URIs. Synchronized because
     * operations on longs are not atomic.
     * 
     * @param increment
     *            amount to increment the queued count
     */
    protected void incrementQueuedUriCount(long increment) {
        queuedUriCount.addAndGet(increment);
    }

    /**
     * Note that a number of queued Uris have been deleted.
     * 
     * @param numberOfDeletes
     */
    protected void decrementQueuedCount(long numberOfDeletes) {
        queuedUriCount.addAndGet(-numberOfDeletes);
    }

    /**
     * (non-Javadoc)
     * 
     * @see org.archive.crawler.framework.Frontier#queuedUriCount()
     */
    public long queuedUriCount() {
        return queuedUriCount.get();
    }

    /**
     * (non-Javadoc)
     * 
     * @see org.archive.crawler.framework.Frontier#finishedUriCount()
     */
    public long finishedUriCount() {
        return succeededFetchCount.get() + failedFetchCount.get() + disregardedUriCount.get();
    }

    /**
     * Increment the running count of successfully fetched URIs. 
     */
    protected void incrementSucceededFetchCount() {
        succeededFetchCount.incrementAndGet();
    }

    /**
     * (non-Javadoc)
     * 
     * @see org.archive.crawler.framework.Frontier#succeededFetchCount()
     */
    public long succeededFetchCount() {
        return succeededFetchCount.get();
    }

    /**
     * Increment the running count of failed URIs.
     */
    protected void incrementFailedFetchCount() {
        failedFetchCount.incrementAndGet();
    }

    /**
     * (non-Javadoc)
     * 
     * @see org.archive.crawler.framework.Frontier#failedFetchCount()
     */
    public long failedFetchCount() {
        return failedFetchCount.get();
    }

    /**
     * Increment the running count of disregarded URIs.
     */
    protected void incrementDisregardedUriCount() {
        disregardedUriCount.incrementAndGet();
    }

    public long disregardedUriCount() {
        return disregardedUriCount.get();
    }

    /** @deprecated misnomer; use StatisticsTracking figures instead */
    public long totalBytesWritten() {
        return totalProcessedBytes;
    }

    /**
     * Load up the seeds.
     * 
     * This method is called on initialize and inside in the crawlcontroller
     * when it wants to force reloading of configuration.
     */
    public void loadSeeds() {
        logger.info("beginning");
        // Get the seeds to refresh.
        Writer ignoredWriter = new StringWriter();
        Iterator iter = manager.getGlobalSheet().get(this, SEEDS)
            .seedsIterator(ignoredWriter);
        int count = 0; 
        while (iter.hasNext()) {
            UURI u = (UURI)iter.next();
            CrawlURI caUri = new CrawlURI(u);
            caUri.setStateProvider(manager);
            caUri.setSeed(true);
            caUri.setSchedulingDirective(SchedulingConstants.MEDIUM);
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
        saveIgnoredItems(ignoredWriter.toString(), recoveryDir.getFile());
        logger.info("finished");        
    }

    
    public void seedsRefreshed() {
        loadSeeds();
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

    protected CrawlURI asCrawlUri(CrawlURI curi) {
        if (curi.getOrdinal() == 0) {
            curi.setOrdinal(nextOrdinal.getAndIncrement());
        }
        curi.setClassKey(getClassKey(curi));
        curi.setStateProvider(manager);
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
            manager.getGlobalSheet().get(this, SEEDS).addSeed(curi);
            // And it needs rapid scheduling.
	    if (curi.getSchedulingDirective() == SchedulingConstants.NORMAL)
                curi.setSchedulingDirective(SchedulingConstants.MEDIUM);
        }

        // optionally preferencing embeds up to MEDIUM
        int prefHops = curi.get(this,PREFERENCE_EMBED_HOPS); 
        if (prefHops > 0) {
            int embedHops = curi.getTransHops();
            if (embedHops > 0 && embedHops <= prefHops
                    && curi.getSchedulingDirective() == SchedulingConstants.NORMAL) {
                // number of embed hops falls within the preferenced range, and
                // uri is not already MEDIUM -- so promote it
                curi.setSchedulingDirective(SchedulingConstants.MEDIUM);
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
                status == S_DOMAIN_UNRESOLVABLE)? curi.get(this,RETRY_DELAY_SECONDS) : 0;
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
            durationToWait = (long)(curi.get(this,DELAY_FACTOR) * durationTaken);

            long minDelay = curi.get(this,MIN_DELAY_MS);
            if (minDelay > durationToWait) {
                // wait at least the minimum
                durationToWait = minDelay;
            }

            long maxDelay = curi.get(this,MAX_DELAY_MS);
            if (durationToWait > maxDelay) {
                // wait no more than the maximum
                durationToWait = maxDelay;
            }

            long now = System.currentTimeMillis();
            int maxBandwidthKB = curi.get(this, MAX_PER_HOST_BANDWIDTH_USAGE_KB_SEC);
            if (maxBandwidthKB > 0) {
                // Enforce bandwidth limit
                ServerCache cache = controller.getServerCache();
                CrawlHost host = ServerCacheUtil.getHostFor(cache, curi.getUURI());
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
            Logger le = loggerModule.getLocalErrors();
            for (Throwable e : x) {
                le.log(Level.WARNING, curi.toString(), 
                        new Object[] { curi, e });
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
        return new File(scratchDir.getFile(), hex.substring(len - 2,
                len)
                + File.separator
                + hex.substring(len - 4, len - 2)
                + File.separator + key);
    }

    protected boolean overMaxRetries(CrawlURI curi) {
        // never retry more than the max number of times
        if (curi.getFetchAttempts() >= curi.get(this,MAX_RETRIES)) {
            return true;
        }
        return false;
    }

    public void importRecoverLog(String pathToLog, boolean retainFailures)
            throws IOException {
        File source = new File(pathToLog);
        if (!source.isAbsolute()) {
            source = new File(recoveryDir.getFile(), pathToLog);
        }
        RecoveryJournal.importRecoverLog(source, this, retainFailures);
    }


    /**
     * Log to the main crawl.log
     * 
     * @param curi
     */
    protected void log(CrawlURI curi) {
        curi.aboutToLog();
        Object array[] = {curi};
        this.loggerModule.getUriProcessing().log(Level.INFO,
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
     * for a particular URI -- its not so easy; Each CrawlURI would need a
     * reference to the settings system. That's awkward to pass in.
     * 
     * @param uuri Candidate URI to canonicalize.
     * @return Canonicalized version of passed <code>uuri</code>.
     */
    protected String canonicalize(UURI uuri) {
        Sheet global = manager.getGlobalSheet();
        List<CanonicalizationRule> rules = global.get(this, RULES);
        return Canonicalizer.canonicalize(global, uuri.toString(), rules);
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
     * @param cauri CrawlURI we're to get a key for.
     * @return a String token representing a queue
     */
    public String getClassKey(CrawlURI cauri) {
        String queueKey = cauri.get(this,FORCE_QUEUE_ASSIGNMENT);
        if ("".equals(queueKey)) {
            // Typical case, barring overrides
            queueKey = cauri.get(this, QUEUE_ASSIGNMENT_POLICY).getClassKey(
                    controller, cauri);
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
    }

    public void crawlPausing(String statusMessage) {
    }

    public void crawlPaused(String statusMessage) {
    }

    public void crawlResuming(String statusMessage) {
    }
    
    public void crawlCheckpoint(StateProvider context, File checkpointDir)
    throws Exception {
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


    private void writeObject(ObjectOutputStream out) 
    throws IOException {
        out.defaultWriteObject();
        boolean recoveryLogEnabled = get(RECOVERY_LOG_ENABLED);
        out.writeBoolean(recoveryLogEnabled);
        if (recoveryLogEnabled) {
            out.writeUTF(loggerModule.getLogsDir().getAbsolutePath());
        }
    }
    
    
    private void readObject(ObjectInputStream inp) 
    throws IOException, ClassNotFoundException {
        inp.defaultReadObject();
        boolean recoveryLogEnabled = inp.readBoolean();
        if (recoveryLogEnabled) {
            String path = inp.readUTF();
            if (inp instanceof CheckpointRecovery) {
                CheckpointRecovery cr = (CheckpointRecovery)inp;
                path = cr.translatePath(path);
                new File(path).mkdirs();
            }
            initJournal(path);
        }
        shouldPause = true;
    }
    
    
    protected void setStateProvider(CrawlURI curi) {
        StateProvider p = curi.getStateProvider();
        if (p == null) {
            curi.setStateProvider(manager);
        }
    }
    
    
    
}
