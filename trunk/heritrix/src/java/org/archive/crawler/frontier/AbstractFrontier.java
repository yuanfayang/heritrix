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
import javax.management.MBeanException;
import javax.management.ReflectionException;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlHost;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.URIFrontier;
import org.archive.crawler.framework.exceptions.EndedException;
import org.archive.crawler.settings.ModuleType;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.Type;

/**
 * @author gojomo
 */
public abstract class AbstractFrontier extends ModuleType implements URIFrontier,
        FetchStatusCodes, CoreAttributeConstants {
    private static final Logger logger =
        Logger.getLogger(AbstractFrontier.class.getName());

    protected CrawlController controller;
    /** ordinal numbers to assign to created CrawlURIs */
    protected long nextOrdinal = 1;
    /** should the frontier hold any threads asking for URIs? */
    private boolean shouldPause = false;
    /** should the frontier send an EndedException to any threads 
     * asking for URIs? */
    private boolean shouldTerminate = false;
    
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
    protected final static Long DEFAULT_RETRY_DELAY = new Long(900); //15 minutes

    /** maximum times to emit a CrawlURI without final disposition */
    public final static String ATTR_MAX_RETRIES = "max-retries";
    protected final static Integer DEFAULT_MAX_RETRIES = new Integer(30);

    // top-level stats
    long queuedCount = 0;  // total URIs queued to be visited
    long successCount = 0; 
    long failedCount = 0;
    long disregardedCount = 0; //URIs that are disregarded (for example because of robot.txt rules)

    // Used when bandwidth constraint are used
    long totalProcessedBytes = 0;
    long nextURIEmitTime = 0;
    long processedBytesAfterLastEmittedURI = 0;
    int lastMaxBandwidthKB = 0;
    
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
        unpause();
    }
    
    /** (non-Javadoc)
     * @see org.archive.crawler.framework.URIFrontier#queuedUriCount()
     */
    public long queuedUriCount(){
        return queuedCount;
    }

    /** (non-Javadoc)
     * @see org.archive.crawler.framework.URIFrontier#finishedUriCount()
     */
    public long finishedUriCount() {
        return successCount+failedCount+disregardedCount;
    }

    /** (non-Javadoc)
     * @see org.archive.crawler.framework.URIFrontier#successfullyFetchedCount()
     */
    public long successfullyFetchedCount(){
        return successCount;
    }

    /** (non-Javadoc)
     * @see org.archive.crawler.framework.URIFrontier#failedFetchCount()
     */
    public long failedFetchCount(){
       return failedCount;
    }

    /** (non-Javadoc)
     * @see org.archive.crawler.framework.URIFrontier#disregardedFetchCount()
     */
    public long disregardedFetchCount() {
        return disregardedCount;
    }

    /** (non-Javadoc)
     * @see org.archive.crawler.framework.URIFrontier#totalBytesWritten()
     */
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
        // Get the seeds to refresh and then get an iterator inside a
        // synchronization block.  The seeds list may get updated during our
        // iteration. This will throw a concurrentmodificationexception unless
        // we synchronize.
        List seeds = this.controller.getScope().getSeedlist();
        synchronized(seeds) {
            for (Iterator i = seeds.iterator(); i.hasNext();) {
                UURI u = (UURI)i.next();
                CandidateURI caUri = new CandidateURI(u);
                caUri.setSeed();
                caUri.setSchedulingDirective(CandidateURI.MEDIUM);
                schedule(caUri);
            }
        }
    }
    
    protected CrawlURI asCrawlUri(CandidateURI caUri) {
        if(caUri instanceof CrawlURI) {
            return (CrawlURI) caUri;
        }
        return CrawlURI.from(caUri,nextOrdinal++);
    }
    
    /**
     * @param now
     * @throws InterruptedException
     * @throws EndedException
     */
    protected synchronized void preNext(long now) throws InterruptedException, EndedException {
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
    }
    
    /**
     * @param curi
     * @return
     * @throws AttributeNotFoundException
     * @throws MBeanException
     * @throws ReflectionException
     */
    protected long retryDelayFor(CrawlURI curi) {
        if ( curi.getFetchStatus() == S_CONNECT_FAILED || 
            curi.getFetchStatus()== S_CONNECT_LOST) {
            if(curi.getAList().containsKey(A_RETRY_DELAY)) {
                return curi.getAList().getInt(A_RETRY_DELAY);
            } else {
                // use overall default
                return ((Long)getUncheckedAttribute(curi,ATTR_RETRY_DELAY)).longValue();
            }
        } else {
            return 0; // no delay for most 
        }
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
    protected long politenessDelayFor(CrawlURI curi) {
        long durationToWait = 0;
        if (curi.getAList().containsKey(A_FETCH_BEGAN_TIME)
            && curi.getAList().containsKey(A_FETCH_COMPLETED_TIME)) {

            long completeTime = curi.getAList().getLong(A_FETCH_COMPLETED_TIME);
            long durationTaken = (completeTime - curi.getAList().getLong(A_FETCH_BEGAN_TIME));
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
                CrawlHost host = curi.getServer().getHost();
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
    private void enforceBandwidthThrottle(long now) throws InterruptedException {
        int maxBandwidthKB;

        maxBandwidthKB = ((Integer) getUncheckedAttribute(null,
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
                while(now<targetTime) {
                    synchronized(this) {
                        logger.fine("Frontier waits for: " + sleepTime
                                + "ms to respect bandwidth limit.");
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
        if(curi.getAList().containsKey(A_LOCALIZED_ERRORS)) {
            List localErrors = (List)curi.getAList().getObject(A_LOCALIZED_ERRORS);
            Iterator iter = localErrors.iterator();
            while(iter.hasNext()) {
                Object array[] = { curi, iter.next() };
                controller.localErrors.log(
                    Level.WARNING,
                    curi.getUURI().toString(),
                    array);
            }
            // once logged, discard
            curi.getAList().remove(A_LOCALIZED_ERRORS);
        }
    }
    
    /**
     * Utility method to reeturn a scratch dir for the given key's temp files. 
     * Every key gets its own subdir. To avoid having any one directory with 
     * thousands of files, there are also two levels of enclosing directory 
     * named by the least-significant hex digits of the key string's java 
     * hashcode. 
     * 
     * @param key
     * @return
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
    
    /* (non-Javadoc)
     * @see org.archive.crawler.framework.URIFrontier#importRecoverLog(java.lang.String)
     */
    public void importRecoverLog(String pathToLog) throws IOException {
        File source = new File(pathToLog);
        if (!source.isAbsolute()) {
            source = new File(getSettingsHandler().getOrder()
                    .getController().getDisk(), pathToLog);
        }
        RecoveryJournal.importRecoverLog(source,this);
    }
    
    /* (non-Javadoc)
     * @see org.archive.crawler.framework.URIFrontier#kickUpdate()
     */
    public void kickUpdate() {
        loadSeeds();
    }
}
