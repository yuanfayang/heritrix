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

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;

import org.apache.commons.httpclient.HttpStatus;
import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlHost;
import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.datamodel.CrawlServer;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.datamodel.UURI;
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

/**
 * Shared facilities for Frontier implementations. 
 * 
 * @author gojomo
 */
public abstract class AbstractFrontier extends ModuleType
implements CrawlStatusListener, Frontier, FetchStatusCodes,
CoreAttributeConstants {
    private static final Logger logger =
        Logger.getLogger(AbstractFrontier.class.getName());

    protected CrawlController controller;
    /** ordinal numbers to assign to created CrawlURIs */
    protected long nextOrdinal = 1;
    /** should the frontier hold any threads asking for URIs? */
    protected boolean shouldPause = false;
    /** should the frontier send an EndedException to any threads 
     * asking for URIs? */
    protected boolean shouldTerminate = false;
    
    /** how many multiples of last fetch elapsed time to wait before recontacting same server */
    public final static String ATTR_DELAY_FACTOR = "delay-factor";
    protected final static Float DEFAULT_DELAY_FACTOR = new Float(5);
    
    /** always wait this long after one completion before recontacting
     * same server, regardless of multiple */
    public final static String ATTR_MIN_DELAY = "min-delay-ms";
    protected final static Integer DEFAULT_MIN_DELAY = new Integer(3000); // 3 secs
    
    /** never wait more than this long, regardless of multiple */
    public final static String ATTR_MAX_DELAY = "max-delay-ms";
    protected final static Integer DEFAULT_MAX_DELAY = new Integer(30000); // 30 secs

    /** number of hops of embeds (ERX) to bump to front of host queue */
    public final static String ATTR_PREFERENCE_EMBED_HOPS = "preference-embed-hops";
    protected final static Integer DEFAULT_PREFERENCE_EMBED_HOPS = new Integer(1); 
    
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
    protected final static Long DEFAULT_RETRY_DELAY = new Long(900); // 15 mins

    /** maximum times to emit a CrawlURI without final disposition */
    public final static String ATTR_MAX_RETRIES = "max-retries";
    protected final static Integer DEFAULT_MAX_RETRIES = new Integer(30);

    /** whether to reassign URIs to IP-address based queues when IP known */
    public final static String ATTR_IP_POLITENESS = "ip-politeness";
    // TODO: change default to true once well-tested
    protected final static Boolean DEFAULT_IP_POLITENESS = new Boolean(false); 

    /** queue assignment to force onto CrawlURIs; intended to be overridden */
    public final static String ATTR_FORCE_QUEUE = "force-queue-assignment";
    protected final static String DEFAULT_FORCE_QUEUE = "";
    protected final static String 
        ACCEPTABLE_FORCE_QUEUE = "[-\\w\\.]*"; // word chars, dash, period
    
    /** whether pause, rather than finish, when crawl appears done */
    public final static String ATTR_PAUSE_AT_FINISH = "pause-at-finish";
    // TODO: change default to true once well-tested
    protected final static Boolean DEFAULT_PAUSE_AT_FINISH = new Boolean(false); 

    // top-level stats
    private long queuedUriCount = 0;  // total URIs queued to be visited
    private long succeededFetchCount = 0; 
    private long failedFetchCount = 0;
    private long disregardedUriCount = 0; //URIs that are disregarded (for example because of robot.txt rules)

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
    
    /**
     * @param name
     * @param description
     */
    public AbstractFrontier(String name, String description) {
        super(name, description);
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
        t = addElementToDefinition(new SimpleType(ATTR_IP_POLITENESS,
                "Whether to assign URIs to IP-address based queues "+
                "when possible, to remain polite on a per-IP-address "+
                "basis.",
                DEFAULT_IP_POLITENESS));
        t.setExpertSetting(true);
        t.setOverrideable(false);
        t = addElementToDefinition(
            new SimpleType(ATTR_FORCE_QUEUE,
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
        t = addElementToDefinition(new SimpleType(ATTR_PAUSE_AT_FINISH,
                "Whether to pause when the crawl appears finished, rather " +
                "than immediately end the crawl. This gives the operator an " +
                "opportunity to view crawl results, and possibly add URIs or " +
                "adjust settings, while the crawl state is still available. " +
                "Default is false.",
                DEFAULT_PAUSE_AT_FINISH));
        t.setOverrideable(false);

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
            logger.severe("Failed to get logs directory " + e);
        }
        if (logsDisk != null) {
            String logsPath = logsDisk.getAbsolutePath() + File.separatorChar;
            this.recover = new RecoveryJournal(logsPath, FrontierJournal.LOGNAME_RECOVER);
        }
        if(((Boolean)getUncheckedAttribute(null,ATTR_IP_POLITENESS)).booleanValue()) {
            queueAssignmentPolicy = new IPQueueAssignmentPolicy();
        } else {
            queueAssignmentPolicy = new HostnameQueueAssignmentPolicy();
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
            this.recover.added(c);
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
     * Frontier is empty only if all queues are empty and
     * no URIs are in-process
     *
     * @return True if queues are empty.
     */
    public synchronized boolean isEmpty() {
        return queuedUriCount == 0;
    }
    
    /**
     * Increment the running count of queued URIs. Synchronized
     * because operations on longs are not atomic. 
     */
    protected synchronized void incrementQueuedUriCount() {
        queuedUriCount++;
    }
    
    /**
     * Increment the running count of queued URIs. Synchronized
     * because operations on longs are not atomic. 
     * 
     * @param increment amount to increment the queued count
     */
    protected synchronized void incrementQueuedUriCount(long increment) {
        queuedUriCount += increment;
    }
    
    /**
     * Note that a number of queued Uris have been deleted. 
     * @param numberOfDeletes
     */
    protected synchronized void decrementQueuedCount(long numberOfDeletes) {
        queuedUriCount -= numberOfDeletes;
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

    /**
     * Increment the running count of successfully fetched URIs. Synchronized
     * because operations on longs are not atomic. 
     */
    protected synchronized void incrementSucceededFetchCount() {
        succeededFetchCount++;
    }
    
    /** (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#succeededFetchCount()
     */
    public long succeededFetchCount(){
        return succeededFetchCount;
    }

    /**
     * Increment the running count of failed URIs. Synchronized
     * because operations on longs are not atomic. 
     */
    protected synchronized void incrementFailedFetchCount() {
        failedFetchCount++;
    }
    
    /** (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#failedFetchCount()
     */
    public long failedFetchCount(){
       return failedFetchCount;
    }

    /**
     * Increment the running count of disregarded URIs. Synchronized
     * because operations on longs are not atomic. 
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
        // Get the seeds to refresh.
        for (Iterator iter = this.controller.getScope().seedsIterator();
                iter.hasNext();) {
            UURI u = (UURI)iter.next();
            CandidateURI caUri = CandidateURI.createSeedCandidateURI(u);
            caUri.setSchedulingDirective(CandidateURI.MEDIUM);
            schedule(caUri);
        }
    }
    
    protected CrawlURI asCrawlUri(CandidateURI caUri) {
        CrawlURI curi;
        if(caUri instanceof CrawlURI) {
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
    protected synchronized void preNext(long now) throws InterruptedException, EndedException {
        
        // check completion conditions
        if(controller.atFinish()) {
            if(((Boolean)getUncheckedAttribute(null,ATTR_PAUSE_AT_FINISH)).booleanValue()) {
                controller.requestCrawlPause();
            } else {
                controller.beginCrawlStop();
            }
        }
        
        // enforce operator pause
        if(shouldPause) {
            while(shouldPause) {
                controller.toePaused();
                wait();
            }
            // exitted pause; possibly finish regardless of pause-at-finish
            if(controller.atFinish()) {
                controller.beginCrawlStop();
            }
        }
        
        // enforce operator terminate or thread retirement
        if(shouldTerminate || ((ToeThread)Thread.currentThread()).shouldRetire()) {
            throw new EndedException("terminated");
        }
        
        enforceBandwidthThrottle(now);
    }
    
    /**
     * Perform any special handling of the CrawlURI, such as promoting
     * its URI to seed-status, or preferencing it because it is an 
     * embed. 
     *  
     * @param curi
     */
    protected void applySpecialHandling(CrawlURI curi) {
        if (curi.isSeed() && curi.getVia() != null
                && curi.flattenVia().length() > 0) {
            // The only way a seed can have a non-empty via is if it is the
            // result of a seed redirect.  Add it to the seeds list.
            //
            // This is a feature.  This is handling for case where a seed
            // gets immediately redirected to another page.  What we're doing is
            // treating the immediate redirect target as a seed.
            this.controller.getScope().addSeed(curi.getUURI());
            // And it needs rapid scheduling.
            curi.setSchedulingDirective(CandidateURI.MEDIUM);
        }

        
        // optionally preferencing embeds up to MEDIUM
        int prefHops = ((Integer) getUncheckedAttribute(curi,
                ATTR_PREFERENCE_EMBED_HOPS)).intValue();        if (prefHops > 0) {
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
     * @param curi CrawlURI about to be returned by next()
     * @param q the queue from which the CrawlURI came
     */
    protected void noteAboutToEmit(CrawlURI curi, BdbWorkQueue q) {
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
     * @param curi CrawlURI to be retried
     * @return millisecond delay before retry
     */
    protected long retryDelayFor(CrawlURI curi) {
        int status = curi.getFetchStatus();
        return ( status == S_CONNECT_FAILED || status == S_CONNECT_LOST 
                || status == S_DOMAIN_UNRESOLVABLE )?
            ((Long)getUncheckedAttribute(curi, ATTR_RETRY_DELAY)).longValue():
            0; // no delay for most 
    }
    
    /**
     * Update any scheduling structures with the new information
     * in this CrawlURI. Chiefly means make necessary arrangements
     * for no other URIs at the same host to be visited within the
     * appropriate politeness window.
     *
     * @param curi The CrawlURI
     * @return millisecond politeness delay
     */
    protected long politenessDelayFor(CrawlURI curi) {
        long durationToWait = 0;
        if (curi.containsKey(A_FETCH_BEGAN_TIME)
            && curi.containsKey(A_FETCH_COMPLETED_TIME)) {

            long completeTime = curi.getLong(A_FETCH_COMPLETED_TIME);
            long durationTaken = (completeTime - curi.getLong(A_FETCH_BEGAN_TIME));
            durationToWait =
                    (long) (((Float) getUncheckedAttribute(curi, ATTR_DELAY_FACTOR)).floatValue()
                        * durationTaken);

            long minDelay = ((Integer) getUncheckedAttribute(curi, ATTR_MIN_DELAY)).longValue();
            if (minDelay > durationToWait) {
                // wait at least the minimum
                durationToWait = minDelay;
            }

            long maxDelay = ((Integer) getUncheckedAttribute(curi, ATTR_MAX_DELAY)).longValue();
            if (durationToWait > maxDelay) {
                // wait no more than the maximum
                durationToWait = maxDelay;
            }

            long now = System.currentTimeMillis();
            int maxBandwidthKB = ((Integer) getUncheckedAttribute(curi, 
                        ATTR_MAX_HOST_BANDWIDTH_USAGE)).intValue();
            if (maxBandwidthKB > 0) {
                // Enforce bandwidth limit
                CrawlHost host = controller.getServerCache().getHostFor(curi);
                long minDurationToWait =
                    host.getEarliestNextURIEmitTime() - now;
                float maxBandwidth = maxBandwidthKB * 1.024F; // kilo factor
                long processedBytes = curi.getContentSize();
                host.setEarliestNextURIEmitTime(
                        (long) (processedBytes / maxBandwidth) + now);

                if (minDurationToWait > durationToWait) {
                    durationToWait = minDurationToWait;
                }
            }
        }
        return durationToWait;
    }
    
    /**
     * Ensure that any overall-bandwidth-usage limit is respected,
     * by pausing as long as necessary.
     * 
     * @param now
     * @throws InterruptedException
     */
    private void enforceBandwidthThrottle(long now)
    throws InterruptedException {
        int maxBandwidthKB = ((Integer) getUncheckedAttribute(null,
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
            long processedBytes =
                totalProcessedBytes - processedBytesAfterLastEmittedURI;
            long shouldHaveEmittedDiff =
                nextURIEmitTime == 0 ? 0 : nextURIEmitTime - now;
            nextURIEmitTime = (long) (processedBytes / maxBandwidth)
                    + now + shouldHaveEmittedDiff;
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
                        wait(targetTime-now);
                    }
                    now = System.currentTimeMillis();
                }
            }
        }
    }
    
    /**
     * Take note of any processor-local errors that have
     * been entered into the CrawlURI.
     * @param curi
     *
     */
    protected void logLocalizedErrors(CrawlURI curi) {
        if(curi.containsKey(A_LOCALIZED_ERRORS)) {
            List localErrors = (List)curi.getObject(A_LOCALIZED_ERRORS);
            Iterator iter = localErrors.iterator();
            while(iter.hasNext()) {
                Object array[] = { curi, iter.next() };
                controller.localErrors.log(
                    Level.WARNING,
                    curi.getUURI().toString(),
                    array);
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
        while (hex.length()<4) {
            hex = "0"+hex;
        }
        int len = hex.length(); 
        return new File(this.controller.getStateDisk(), hex.substring(len-2, len)
                + File.separator + hex.substring(len-4, len-2) + File.separator + key);
    }
    
    protected boolean overMaxRetries(CrawlURI curi) {
        // never retry more than the max number of times
        if (curi.getFetchAttempts() >= ((Integer)getUncheckedAttribute(curi,ATTR_MAX_RETRIES)).intValue() ) {
            return true;
        }
        return false;
    }
    
    public void importRecoverLog(String pathToLog, boolean retainFailures)
			throws IOException {
        File source = new File(pathToLog);
        if (!source.isAbsolute()) {
            source = new File(getSettingsHandler().getOrder()
                    .getController().getDisk(), pathToLog);
        }
        RecoveryJournal.importRecoverLog(source,this,retainFailures);
    }
    
    /* (non-Javadoc)
     * @see org.archive.crawler.framework.URIFrontier#kickUpdate()
     */
    public void kickUpdate() {
        loadSeeds();
    }
    
    /**
     * Log to the main crawl.log
     * 
     * @param curi
     */
    protected void log(CrawlURI curi) {
        curi.aboutToLog();
        Object array[] = { curi };
        this.controller.uriProcessing.log(
            Level.INFO,
            curi.getUURI().toString(),
            array );
    }
    
    protected boolean isDisregarded(CrawlURI curi) {
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
     * Checks if a recently completed CrawlURI that did not finish successfully
     * needs to be retried (processed again after some time elapses)
     *
     * @param curi The CrawlURI to check
     * @return True if we need to retry.
     */
    protected boolean needsRetrying(CrawlURI curi) {
        if (overMaxRetries(curi)) {
            return false; 
        }
        
        switch (curi.getFetchStatus()) {
            case HttpStatus.SC_UNAUTHORIZED:
                // We can get here though usually a positive status code is
                // a success.  We get here if there is rfc2617 credential data
                // loaded and we're supposed to go around again.  See if any
                // rfc2617 credential present and if there, assume it got
                // loaded in FetchHTTP on expectation that we're to go around
                // again.  If no rfc2617 loaded, we should not be here.
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
                // TODO: consider if any others (S_TIMEOUT in some cases?) deserve retry
                return true;
            default:
                return false;
        }
    }

    /**
     * Canonicalize passed uuri.
     * Its would be sweeter if this canonicalize function was encapsulated
     * by that which it canonicalizes but because settings change with
     * context -- i.e. there may be overrides in operation for a
     * particular URI -- its not so easy; Each CandidateURI would need
     * a reference to the settings system.  That's awkward to pass in.
     * @param uuri Candidate URI to canonicalize.
     * @return Canonicalized version of passed <code>uuri</code>.
    */
    protected String canonicalize(UURI uuri) {
        return Canonicalizer.canonicalize(uuri, this.controller.getOrder());
    }
    
    /**
     * Canonicalize passed CandidateURI but also look at the CandidateURI
     * context possibly skipping canonicalization if it means we may miss
     * content.
     * If canonicalization produces an URL that was 'alreadyseen',
     * but the entry in the 'alreadyseen' database did nothing but
     * redirect to the current URL, we won't
     * get the current URL; we'll think we've already seen it.
     * An example would be archive.org redirecting to
     * www.archive.org. This method
     * looks for this condition and DOES NOT canonicalize in this
     * case.  Otherwise it returns canonicalized form.
     * @param cauri CandidateURI to examine.
     * @return True if we are not to canonicalize before passing to
     * alreadyseen.
     */
    protected String conditionalCanonicalize(CandidateURI cauri) {
        if (cauri.isSeed()) {
            // Don't run canonicalization on seeds.
            return cauri.toString();
        }
        String c = canonicalize(cauri.getUURI());
        if (!cauri.isLocation()) {
            // If not result of a redirect, return canonicalized version.
            return c;
        }
        // This URI is result of a redirect.  Does the
        // canonicalization of this URI and that of its via
        // resolve to same thing?  If so, return uncanonicalized
        // version of this uri.
        boolean same = canonicalize(cauri.getVia()).equals(c);
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Result for " + cauri.toString() + ", " +
                cauri.getVia().toString() + ", " + cauri.getPathFromSeed() +
                " : " + same);
        }
        return same? cauri.toString(): c;
    }
    
    /**
     * @param curi CrawlURI we're to get a key for.
     * @return a String token representing a queue
     */
    protected String getClassKey(CrawlURI curi) {
        String queueKey =
            (String)getUncheckedAttribute(curi, ATTR_FORCE_QUEUE);
        if("".equals(queueKey)) {
            // Typical case, barring overrides
            queueKey =
                queueAssignmentPolicy.getClassKey(this.controller, curi);
        }
        return queueKey;
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
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.event.CrawlStatusListener#crawlStarted(java.lang.String)
     */
    public void crawlStarted(String message) {
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
}
