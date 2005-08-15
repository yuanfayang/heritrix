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
import java.io.StringWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.management.AttributeNotFoundException;

import org.apache.commons.httpclient.HttpStatus;
import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlHost;
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
import org.archive.crawler.settings.ModuleType;
import org.archive.crawler.settings.RegularExpressionConstraint;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.Type;
import org.archive.crawler.url.Canonicalizer;
import org.archive.net.UURI;
import org.archive.util.ArchiveUtils;

/**
 * Shared facilities for Frontier implementations.
 * 
 * @author gojomo
 */
public abstract class AbstractFrontier extends ModuleType implements
        CrawlStatusListener, Frontier, FetchStatusCodes, CoreAttributeConstants {
    private static final Logger logger = Logger
            .getLogger(AbstractFrontier.class.getName());

    protected CrawlController controller;

    /** ordinal numbers to assign to created CrawlURIs */
    protected long nextOrdinal = 1;

    /** should the frontier hold any threads asking for URIs? */
    protected boolean shouldPause = false;

    /**
     * should the frontier send an EndedException to any threads asking for
     * URIs?
     */
    protected boolean shouldTerminate = false;

    /**
     * how many multiples of last fetch elapsed time to wait before recontacting
     * same server
     */
    public final static String ATTR_DELAY_FACTOR = "delay-factor";

    protected final static Float DEFAULT_DELAY_FACTOR = new Float(5);

    /**
     * always wait this long after one completion before recontacting same
     * server, regardless of multiple
     */
    public final static String ATTR_MIN_DELAY = "min-delay-ms";

    // 3 secs.
    protected final static Integer DEFAULT_MIN_DELAY = new Integer(3000);

    /** never wait more than this long, regardless of multiple */
    public final static String ATTR_MAX_DELAY = "max-delay-ms";

    // 30 secs
    protected final static Integer DEFAULT_MAX_DELAY = new Integer(30000);

    /** number of hops of embeds (ERX) to bump to front of host queue */
    public final static String ATTR_PREFERENCE_EMBED_HOPS =
        "preference-embed-hops";

    protected final static Integer DEFAULT_PREFERENCE_EMBED_HOPS =
        new Integer(1);

    /** maximum per-host bandwidth usage */
    public final static String ATTR_MAX_HOST_BANDWIDTH_USAGE =
        "max-per-host-bandwidth-usage-KB-sec";

    protected final static Integer DEFAULT_MAX_HOST_BANDWIDTH_USAGE =
        new Integer(0);

    /** maximum overall bandwidth usage */
    public final static String ATTR_MAX_OVERALL_BANDWIDTH_USAGE =
        "total-bandwidth-usage-KB-sec";

    protected final static Integer DEFAULT_MAX_OVERALL_BANDWIDTH_USAGE =
        new Integer(0);

    /** for retryable problems, seconds to wait before a retry */
    public final static String ATTR_RETRY_DELAY = "retry-delay-seconds";

    // 15 mins
    protected final static Long DEFAULT_RETRY_DELAY = new Long(900);

    /** maximum times to emit a CrawlURI without final disposition */
    public final static String ATTR_MAX_RETRIES = "max-retries";

    protected final static Integer DEFAULT_MAX_RETRIES = new Integer(30);

    public final static String ATTR_QUEUE_ASSIGNMENT_POLICY =
        "queue-assignment-policy";

    /** queue assignment to force onto CrawlURIs; intended to be overridden */
    public final static String ATTR_FORCE_QUEUE = "force-queue-assignment";

    protected final static String DEFAULT_FORCE_QUEUE = "";

    // word chars, dash, period, comma, colon
    protected final static String ACCEPTABLE_FORCE_QUEUE = "[-\\w\\.,:]*";
        
    /** whether pause, rather than finish, when crawl appears done */
    public final static String ATTR_PAUSE_AT_FINISH = "pause-at-finish";
    // TODO: change default to true once well-tested
    protected final static Boolean DEFAULT_PAUSE_AT_FINISH =
        new Boolean(false);
    
    /** whether to pause at crawl start */
    public final static String ATTR_PAUSE_AT_START = "pause-at-start";
    protected final static Boolean DEFAULT_PAUSE_AT_START =
        new Boolean(false);

    // top-level stats
    private long queuedUriCount = 0; // total URIs queued to be visited

    private long succeededFetchCount = 0;

    private long failedFetchCount = 0;

    private long disregardedUriCount = 0; //URIs that are disregarded (for
                                          // example because of robot.txt rules)

    // Used when bandwidth constraint are used
    long totalProcessedBytes = 0;

    long nextURIEmitTime = 0;

    long processedBytesAfterLastEmittedURI = 0;

    int lastMaxBandwidthKB = 0;

    /** Policy for assigning CrawlURIs to named queues */
    protected QueueAssignmentPolicy queueAssignmentPolicy = null;

    /**
     * Crawl replay logger.
     * 
     * Currently captures Frontier/URI transitions.
     */
    transient private FrontierJournal recover = null;

    /** file collecting report of ignored seed-file entries (if any) */
    public static final String IGNORED_SEEDS_FILENAME = "seeds.ignored";

    /**
     * @param name
     * @param description
     */
    public AbstractFrontier(String name, String description) {
        super(name, description);
        addElementToDefinition(new SimpleType(ATTR_DELAY_FACTOR,
                "How many multiples of last fetch elapsed time to wait before "
                        + "recontacting same server", DEFAULT_DELAY_FACTOR));
        addElementToDefinition(new SimpleType(ATTR_MAX_DELAY,
                "Never wait more than this long.", DEFAULT_MAX_DELAY));
        addElementToDefinition(new SimpleType(ATTR_MIN_DELAY,
                "Always wait this long after one completion before recontacting "
                        + "same server.", DEFAULT_MIN_DELAY));
        addElementToDefinition(new SimpleType(ATTR_MAX_RETRIES,
                "How often to retry fetching a URI that failed to be retrieved. "
                        + "If zero, the crawler will get the robots.txt only.",
                DEFAULT_MAX_RETRIES));
        addElementToDefinition(new SimpleType(ATTR_RETRY_DELAY,
                "How long to wait by default until we retry fetching a"
                        + " URI that failed to be retrieved (seconds). ",
                DEFAULT_RETRY_DELAY));
        addElementToDefinition(new SimpleType(
                ATTR_PREFERENCE_EMBED_HOPS,
                "Number of embedded (or redirected) hops up to which "
                + "a URI has higher priority scheduling. For example, if set "
                + "to 1 (the default), items such as inline images (1-hop "
                + "embedded resources) will be scheduled ahead of all regular "
                + "links (or many-hop resources, like nested frames). If set to "
                + "zero, no preferencing will occur, and embeds/redirects are "
                + "scheduled the same as regular links.",
                DEFAULT_PREFERENCE_EMBED_HOPS));
        Type t;
        t = addElementToDefinition(new SimpleType(
                ATTR_MAX_OVERALL_BANDWIDTH_USAGE,
                "The maximum average bandwidth the crawler is allowed to use. "
                + "The actual read speed is not affected by this setting, it only "
                + "holds back new URIs from being processed when the bandwidth "
                + "usage has been to high. 0 means no bandwidth limitation.",
                DEFAULT_MAX_OVERALL_BANDWIDTH_USAGE));
        t.setOverrideable(false);
        t = addElementToDefinition(new SimpleType(
                ATTR_MAX_HOST_BANDWIDTH_USAGE,
                "The maximum average bandwidth the crawler is allowed to use per "
                + "host. The actual read speed is not affected by this setting, "
                + "it only holds back new URIs from being processed when the "
                + "bandwidth usage has been to high. 0 means no bandwidth "
                + "limitation.", DEFAULT_MAX_HOST_BANDWIDTH_USAGE));
        t.setExpertSetting(true);

        // Read the list of permissible choices from heritrix.properties.
        // Its a list of space- or comma-separated values.
        String queueStr = System.getProperty(AbstractFrontier.class.getName() +
                "." + ATTR_QUEUE_ASSIGNMENT_POLICY,
                HostnameQueueAssignmentPolicy.class.getName() + " " +
                IPQueueAssignmentPolicy.class.getName() + " " +
                BucketQueueAssignmentPolicy.class.getName() + " " +
                SurtAuthorityQueueAssignmentPolicy.class.getName());
        Pattern p = Pattern.compile("\\s*,\\s*|\\s+");
        String [] queues = p.split(queueStr);
        if (queues.length <= 0) {
            throw new RuntimeException("Failed parse of " +
                    " assignment queue policy string: " + queueStr);
        }
        t = addElementToDefinition(new SimpleType(ATTR_QUEUE_ASSIGNMENT_POLICY,
                "Defines how to assign URIs to queues. Can assign by host, " +
                "by ip, and into one of a fixed set of buckets (1k).",
                queues[0], queues));
        t.setExpertSetting(true);
        t.setOverrideable(false);

        t = addElementToDefinition(new SimpleType(
                ATTR_FORCE_QUEUE,
                "The queue name into which to force URIs. Should "
                + "be left blank at global level.  Specify a "
                + "per-domain/per-host override to force URIs into "
                + "a particular named queue, regardless of the assignment "
                + "policy in effect (domain or ip-based politeness). "
                + "This could be used on domains known to all be from "
                + "the same small set of IPs (eg blogspot, dailykos, etc.) "
                + "to simulate IP-based politeness, or it could be used if "
                + "you wanted to enforce politeness over a whole domain, even "
                + "though the subdomains are split across many IPs.",
                DEFAULT_FORCE_QUEUE));
        t.setOverrideable(true);
        t.setExpertSetting(true);
        t.addConstraint(new RegularExpressionConstraint(ACCEPTABLE_FORCE_QUEUE,
                Level.WARNING, "This field must contain only alphanumeric "
                + "characters plus period, dash, comma, colon, or underscore."));
        t = addElementToDefinition(new SimpleType(
                ATTR_PAUSE_AT_START,
                "Whether to pause when the crawl begins, before any URIs " +
                "are tried. This gives the operator a chance to verify or " +
                "adjust the crawl before actual work begins. " +
                "Default is false.", DEFAULT_PAUSE_AT_START));
        t = addElementToDefinition(new SimpleType(
                ATTR_PAUSE_AT_FINISH,
                "Whether to pause when the crawl appears finished, rather "
                + "than immediately end the crawl. This gives the operator an "
                + "opportunity to view crawl results, and possibly add URIs or "
                + "adjust settings, while the crawl state is still available. "
                + "Default is false.", DEFAULT_PAUSE_AT_FINISH));
        t.setOverrideable(false);
    }

    public void start() {
        if (((Boolean)getUncheckedAttribute(null, ATTR_PAUSE_AT_START))
                .booleanValue()) {
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
        try {
            logsDisk = c.getSettingsDir(CrawlOrder.ATTR_LOGS_PATH);
        } catch (AttributeNotFoundException e) {
            logger.log(Level.SEVERE, "Failed to get logs directory", e);
        }
        if (logsDisk != null) {
            String logsPath = logsDisk.getAbsolutePath() + File.separatorChar;
            this.recover = new RecoveryJournal(logsPath,
                    FrontierJournal.LOGNAME_RECOVER);
        }
        try {
            final Class qapClass = Class.forName((String)getUncheckedAttribute(
                    null, ATTR_QUEUE_ASSIGNMENT_POLICY));

            queueAssignmentPolicy =
                (QueueAssignmentPolicy)qapClass.newInstance();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Bad queue assignment policy class", e);
            throw new FatalConfigurationException(e.getMessage());
        }
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
     * @param ignoredWriter
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

        // check completion conditions
        if (controller.atFinish()) {
            if (((Boolean)getUncheckedAttribute(null, ATTR_PAUSE_AT_FINISH))
                    .booleanValue()) {
                controller.requestCrawlPause();
            } else {
                controller.beginCrawlStop();
            }
        }

        // enforce operator pause
        if (shouldPause) {
            while (shouldPause) {
                controller.toePaused();
                wait();
            }
            // exitted pause; possibly finish regardless of pause-at-finish
            if (controller.atFinish()) {
                controller.beginCrawlStop();
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
            curi.setSchedulingDirective(CandidateURI.MEDIUM);
        }

        // optionally preferencing embeds up to MEDIUM
        int prefHops = ((Integer)getUncheckedAttribute(curi,
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
        return this.controller.getServerCache().getServerFor(curi);
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
                status == S_DOMAIN_UNRESOLVABLE)?
            ((Long)getUncheckedAttribute(curi, ATTR_RETRY_DELAY)).longValue():
            0; // no delay for most
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
        if (curi.containsKey(A_FETCH_BEGAN_TIME)
                && curi.containsKey(A_FETCH_COMPLETED_TIME)) {

            long completeTime = curi.getLong(A_FETCH_COMPLETED_TIME);
            long durationTaken = (completeTime - curi
                    .getLong(A_FETCH_BEGAN_TIME));
            durationToWait = (long)(((Float)getUncheckedAttribute(curi,
                    ATTR_DELAY_FACTOR)).floatValue() * durationTaken);

            long minDelay = ((Integer)getUncheckedAttribute(curi,
                    ATTR_MIN_DELAY)).longValue();
            if (minDelay > durationToWait) {
                // wait at least the minimum
                durationToWait = minDelay;
            }

            long maxDelay = ((Integer)getUncheckedAttribute(curi,
                    ATTR_MAX_DELAY)).longValue();
            if (durationToWait > maxDelay) {
                // wait no more than the maximum
                durationToWait = maxDelay;
            }

            long now = System.currentTimeMillis();
            int maxBandwidthKB = ((Integer)getUncheckedAttribute(curi,
                    ATTR_MAX_HOST_BANDWIDTH_USAGE)).intValue();
            if (maxBandwidthKB > 0) {
                // Enforce bandwidth limit
                CrawlHost host = controller.getServerCache().getHostFor(curi);
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
        int maxBandwidthKB = ((Integer)getUncheckedAttribute(null,
                ATTR_MAX_OVERALL_BANDWIDTH_USAGE)).intValue();
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
        if (curi.containsKey(A_LOCALIZED_ERRORS)) {
            List localErrors = (List)curi.getObject(A_LOCALIZED_ERRORS);
            Iterator iter = localErrors.iterator();
            while (iter.hasNext()) {
                Object array[] = {curi, iter.next()};
                controller.localErrors.log(Level.WARNING, curi.getUURI()
                        .toString(), array);
            }
            // once logged, discard
            curi.remove(A_LOCALIZED_ERRORS);
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
        if (curi.getFetchAttempts() >= ((Integer)getUncheckedAttribute(curi,
                ATTR_MAX_RETRIES)).intValue()) {
            return true;
        }
        return false;
    }

    public void importRecoverLog(String pathToLog, boolean retainFailures)
            throws IOException {
        File source = new File(pathToLog);
        if (!source.isAbsolute()) {
            source = new File(getSettingsHandler().getOrder().getController()
                    .getDisk(), pathToLog);
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
        return Canonicalizer.canonicalize(uuri, this.controller.getOrder());
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
        String queueKey = (String)getUncheckedAttribute(cauri,
            ATTR_FORCE_QUEUE);
        if ("".equals(queueKey)) {
            // Typical case, barring overrides
            queueKey =
                queueAssignmentPolicy.getClassKey(this.controller, cauri);
        }
        return queueKey;
    }

    public FrontierJournal getFrontierJournal() {
        return this.recover;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.archive.crawler.event.CrawlStatusListener#crawlEnding(java.lang.String)
     */
    public void crawlEnding(String sExitMessage) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.archive.crawler.event.CrawlStatusListener#crawlEnded(java.lang.String)
     */
    public void crawlEnded(String sExitMessage) {
        if (logger.isLoggable(Level.INFO)) {
            logger.info("Closing with " + Long.toString(queuedUriCount()) +
                " urls still in queue.");
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.archive.crawler.event.CrawlStatusListener#crawlStarted(java.lang.String)
     */
    public void crawlStarted(String message) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.archive.crawler.event.CrawlStatusListener#crawlPausing(java.lang.String)
     */
    public void crawlPausing(String statusMessage) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.archive.crawler.event.CrawlStatusListener#crawlPaused(java.lang.String)
     */
    public void crawlPaused(String statusMessage) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.archive.crawler.event.CrawlStatusListener#crawlResuming(java.lang.String)
     */
    public void crawlResuming(String statusMessage) {
        // TODO Auto-generated method stub

    }
    
    //
    // Reporter implementation
    // 
    
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
}
