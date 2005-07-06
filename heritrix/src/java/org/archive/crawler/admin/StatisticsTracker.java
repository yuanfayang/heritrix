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
 * Created on Jul 16, 2003
 *
 */
package org.archive.crawler.admin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.crawler.Heritrix;
import org.archive.crawler.datamodel.BigMapFactory;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.event.CrawlURIDispositionListener;
import org.archive.crawler.framework.AbstractTracker;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.exceptions.FatalConfigurationException;
import org.archive.util.ArchiveUtils;
import org.archive.util.LongWrapper;
import org.archive.util.PaddingStringBuffer;
import org.archive.crawler.checkpoint.Checkpoint;
import org.archive.crawler.checkpoint.Checkpointable;

/**
 * This is an implementation of the AbstractTracker. It is designed to function
 * with the WUI as well as performing various logging activity.
 * <p>
 * At the end of each snapshot a line is written to the
 * 'progress-statistics.log' file.
 * <p>
 * The header of that file is as follows:
 * <pre> [timestamp] [discovered]    [queued] [downloaded] [doc/s(avg)]  [KB/s(avg)] [dl-failures] [busy-thread] [mem-use-KB]</pre>
 * First there is a <b>timestamp</b>, accurate down to 1 second.
 * <p>
 * <b>discovered</b>, <b>queued</b>, <b>downloaded</b> and <b>dl-failures</b>
 * are (respectively) the discovered URI count, pending URI count, successfully
 * fetched count and failed fetch count from the frontier at the time of the
 * snapshot.
 * <p>
 * <b>KB/s(avg)</b> is the bandwidth usage.  We use the total bytes downloaded
 * to calculate average bandwidth usage (KB/sec). Since we also note the value
 * each time a snapshot is made we can calculate the average bandwidth usage
 * during the last snapshot period to gain a "current" rate. The first number is
 * the current and the average is in parenthesis.
 * <p>
 * <b>doc/s(avg)</b> works the same way as doc/s except it show the number of
 * documents (URIs) rather then KB downloaded.
 * <p>
 * <b>busy-threads</b> is the total number of ToeThreads that are not available
 * (and thus presumably busy processing a URI). This information is extracted
 * from the crawl controller.
 * <p>
 * Finally mem-use-KB is extracted from the run time environment
 * (<code>Runtime.getRuntime().totalMemory()</code>).
 * <p>
 * In addition to the data collected for the above logs, various other data
 * is gathered and stored by this tracker.
 * <ul>
 *   <li> Successfully downloaded documents per fetch status code
 *   <li> Successfully downloaded documents per document mime type
 *   <li> Amount of data per mime type
 *   <li> Successfully downloaded documents per host
 *   <li> Amount of data per host
 *   <li> Disposition of all seeds (this is written to 'reports.log' at end of
 *        crawl)
 * </ul>
 *
 * @author Parker Thompson
 * @author Kristinn Sigurdsson
 *
 * @see org.archive.crawler.framework.StatisticsTracking
 * @see org.archive.crawler.framework.AbstractTracker
 */
public class StatisticsTracker extends AbstractTracker
implements CrawlURIDispositionListener, Checkpointable, Serializable {
    private static final long serialVersionUID = 8004878315916392305L;

    /**
     * Messages from the StatisticsTracker.
     */
    private final static Logger logger =
        Logger.getLogger(StatisticsTracker.class.getName());
    
    // TODO: Need to be able to specify file where the object will be
    // written once the CrawlEnded event occurs

    protected long lastPagesFetchedCount = 0;
    protected long lastProcessedBytesCount = 0;

    /*
     * Snapshot data.
     */
    protected long discoveredUriCount = 0;
    protected long queuedUriCount = 0;
    protected long finishedUriCount = 0;

    protected long downloadedUriCount = 0;
    protected long downloadFailures = 0;
    protected long downloadDisregards = 0;
    protected double docsPerSecond = 0;
    protected double currentDocsPerSecond = 0;
    protected int currentKBPerSec = 0;
    protected long totalKBPerSec = 0;
    protected int busyThreads = 0;
    protected long totalProcessedBytes = 0;

    /*
     * Cumulative data
     */
    /** Keep track of the file types we see (mime type -> count) */
    protected Hashtable mimeTypeDistribution = new Hashtable();
    protected Hashtable mimeTypeBytes = new Hashtable();
    
    /** Keep track of fetch status codes */
    protected Hashtable statusCodeDistribution = new Hashtable();
    
    /** Keep track of hosts. 
     * 
     * Each of these Maps are individually unsynchronized, and cannot 
     * be trivially synchronized with the Collections wrapper. Thus
     * their synchronized access is enforced by this class.
     * 
     * <p>They're transient because usually bigmaps that get reconstituted
     * on recover from checkpoint.
     */
    protected transient Map hostsDistribution = null;
    protected transient Map hostsBytes = null;
    protected transient Map hostsLastFinished = null;

    /** Record of seeds' latest actions */
    protected Map processedSeedsRecords;

    // seeds tallies: ONLY UPDATED WHEN SEED REPORT WRITTEN
    private int seedsCrawled;
    private int seedsNotCrawled;
    // sExitMessage: only set at crawl-end
    private String sExitMessage = "Before crawl end";


    public StatisticsTracker(String name) {
        super( name, "A statistics tracker thats integrated into " +
            "the web UI and that creates the progress-statistics log.");
    }

    public void initialize(CrawlController c)
    throws FatalConfigurationException {
        super.initialize(c);
        try {
            this.hostsDistribution = 
                BigMapFactory.getBigMap(c.getSettingsHandler(), 
                    "hostsDistribution", String.class, LongWrapper.class);
            this.hostsBytes = 
                BigMapFactory.getBigMap(c.getSettingsHandler(),
                    "hostsBytes", String.class, LongWrapper.class);
            this.hostsLastFinished = 
                BigMapFactory.getBigMap(c.getSettingsHandler(), 
                    "hostsLastFinished", String.class, Long.class);
            this.processedSeedsRecords = makeSeedsMap();
            
        } catch (Exception e) {
            throw new FatalConfigurationException("Failed setup of" +
                " StatisticsTracker: " + e);
        }
        controller.addCrawlURIDispositionListener(this);
        // TODO: Not yet implemented.  Need to fix serialization of
        // this class first.
        // this.controller.registerCheckpointable(this);
    }
    
    private Map makeSeedsMap() {
        return Collections.synchronizedMap(new HashMap());
        // TODO: BDBify
    }

    protected void finalCleanup() {
        super.finalCleanup();
        if (this.hostsBytes != null) {
            this.hostsBytes.clear();
            this.hostsBytes = null;
        }
        if (this.hostsDistribution != null) {
            this.hostsDistribution.clear();
            this.hostsDistribution = null;
        }
        if (this.hostsLastFinished != null) {
            this.hostsLastFinished.clear();
            this.hostsLastFinished = null;
        }
    }

    protected synchronized void logActivity() {
        // This method loads "snapshot" data.
        discoveredUriCount = discoveredUriCount();
        downloadedUriCount = successfullyFetchedCount();
        finishedUriCount = finishedUriCount();
        queuedUriCount = queuedUriCount();
        downloadFailures = failedFetchAttempts();
        downloadDisregards = disregardedFetchAttempts();
        totalProcessedBytes = totalBytesWritten();

        if(finishedUriCount() == 0){
            docsPerSecond = 0;
            totalKBPerSec = 0;
        }
        else if(getCrawlerTotalElapsedTime() < 1000){
            return; //Not enough time has passed for a decent snapshot.
        }
        else{
            docsPerSecond = (double) downloadedUriCount /
                (double)(getCrawlerTotalElapsedTime() / 1000);
            // Round to nearest long.
            totalKBPerSec = (long)(((totalProcessedBytes / 1024) /
                 ((getCrawlerTotalElapsedTime()) / 1000)) + .5 );
        }

        busyThreads = activeThreadCount();

        if(shouldrun ||
            (System.currentTimeMillis() - lastLogPointTime) >= 1000) {
            // If shouldrun is false there is a chance that the time interval
            // since last time is too small for a good sample.  We only want
            // to update "current" data when the interval is long enough or
            // shouldrun is true.
            currentDocsPerSecond = 0;
            currentKBPerSec = 0;

            // Note time.
            long currentTime = System.currentTimeMillis();
            long sampleTime = currentTime - lastLogPointTime;

            // if we haven't done anyting or there isn't a reasonable sample
            // size give up.
            if(sampleTime >= 1000) {
                // Update docs/sec snapshot
                long currentPageCount = successfullyFetchedCount();
                long samplePageCount = currentPageCount - lastPagesFetchedCount;

                currentDocsPerSecond =
                    (double) samplePageCount / (double)(sampleTime / 1000);

                lastPagesFetchedCount = currentPageCount;

                // Update kbytes/sec snapshot
                long currentProcessedBytes = totalProcessedBytes;
                long sampleProcessedBytes =
                    currentProcessedBytes - lastProcessedBytesCount;

                currentKBPerSec =
                    (int)(((sampleProcessedBytes/1024)/(sampleTime/1000)) + .5);

                lastProcessedBytesCount = currentProcessedBytes;
            }
        }

        Date now = new Date();
        controller.progressStats.log(
            Level.INFO, progressStatisticsLine(now));
        lastLogPointTime = System.currentTimeMillis();
    }

    /**
     * Return one line of current progress-statistics
     * 
     * @param now
     * @return String of stats
     */
    public String progressStatisticsLine(Date now) {
        return new PaddingStringBuffer()
            .append(ArchiveUtils.TIMESTAMP14ISO8601Z.format(now))
            .raAppend(32, discoveredUriCount)
            .raAppend(44, queuedUriCount)
            .raAppend(57, downloadedUriCount)
            .raAppend(74, ArchiveUtils.
                doubleToString(currentDocsPerSecond,2) +
                "(" + ArchiveUtils.doubleToString(docsPerSecond,2) + ")")
            .raAppend(85, currentKBPerSec + "(" + totalKBPerSec + ")")
            .raAppend(99, downloadFailures)
            .raAppend(113, busyThreads)
            .raAppend(126, (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1024)
            .raAppend(140, Runtime.getRuntime().totalMemory()/1024)
            .toString();
    }

    /**
     * Return one line of current progress-statistics
     * 
     * @return String of stats
     */
    public String progressStatisticsLine() {
        return progressStatisticsLine(new Date());
    }
    
    public double processedDocsPerSec(){
        return docsPerSecond;
    }

    public double currentProcessedDocsPerSec(){
        return currentDocsPerSecond;
    }

    public long processedKBPerSec(){
        return totalKBPerSec;
    }

    public int currentProcessedKBPerSec(){
        return currentKBPerSec;
    }

    /** Returns a HashMap that contains information about distributions of
     *  encountered mime types.  Key/value pairs represent
     *  mime type -> count.
     * <p>
     * <b>Note:</b> All the values are wrapped with a {@link LongWrapper LongWrapper}
     * @return mimeTypeDistribution
     */
    public Hashtable getFileDistribution() {
        return mimeTypeDistribution;
    }


    /**
     * Increment a counter for a key in a given HashMap. Used for various
     * aggregate data.
     * 
     * As this is used to change Maps which depend on StatisticsTracker
     * for their synchronization, this method should only be invoked
     * from a a block synchronized on 'this'. 
     *
     * @param map The HashMap
     * @param key The key for the counter to be incremented, if it does not
     *               exist it will be added (set to 1).  If null it will
     *            increment the counter "unknown".
     */
    protected static void incrementMapCount(Map map, String key) {
    	incrementMapCount(map,key,1);
    }

    /**
     * Increment a counter for a key in a given HashMap by an arbitrary amount.
     * Used for various aggregate data. The increment amount can be negative.
     *
     * As this is used to change Maps which depend on StatisticsTracker
     * for their synchronization, this method should only be invoked
     * from a a block synchronized on 'this'. 
     *
     * @param map
     *            The HashMap
     * @param key
     *            The key for the counter to be incremented, if it does not exist
     *            it will be added (set to equal to <code>increment</code>).
     *            If null it will increment the counter "unknown".
     * @param increment
     *            The amount to increment counter related to the <code>key</code>.
     */
    protected static void incrementMapCount(Map map, String key,
            long increment) {
        if (key == null) {
            key = "unknown";
        }
        LongWrapper lw = (LongWrapper)map.get(key);
        if(lw == null) {
            map.put(key, new LongWrapper((long)1));
        } else {
            lw.longValue += increment;
        }
    }

    /**
     * Sort the entries of the given HashMap in descending order by their
     * values, which must be longs wrapped with <code>LongWrapper</code>.
     * <p>
     * Elements are sorted by value from largest to smallest. Equal values are
     * sorted in an arbitrary, but consistent manner by their keys. Only items
     * with identical value and key are considered equal.
     *
     * If the passed-in map requires access to be synchronized, the caller
     * should ensure this synchronization. 
     * 
     * @param mapOfLongWrapperValues
     *            Assumes values are wrapped with LongWrapper.
     * @return a sorted set containing the same elements as the map.
     */
    public TreeMap getReverseSortedCopy(final Map mapOfLongWrapperValues) {
        TreeMap sortedMap = new TreeMap(new Comparator() {
            public int compare(Object e1, Object e2) {
                long firstVal = ((LongWrapper)mapOfLongWrapperValues.get(e1)).
                    longValue;
                long secondVal = ((LongWrapper)mapOfLongWrapperValues.get(e2)).
                    longValue;
                if (firstVal < secondVal) {
                    return 1;
                }
                if (secondVal < firstVal) {
                    return -1;
                }
                // If the values are the same, sort by keys.
                return ((String)e1).compareTo((String)e2);
            }
        });
        try {
            sortedMap.putAll(mapOfLongWrapperValues);
        } catch (UnsupportedOperationException e) {
            Iterator i = mapOfLongWrapperValues.keySet().iterator();
            for (;i.hasNext();) {
                // Ok. Try doing it the slow way then.
                Object key = i.next();
                sortedMap.put(key, mapOfLongWrapperValues.get(key));
            }
        }
        return sortedMap;
    }

    /**
     * Return a HashMap representing the distribution of status codes for
     * successfully fetched curis, as represented by a hashmap where key -&gt;
     * val represents (string)code -&gt; (integer)count.
     * 
     * <b>Note: </b> All the values are wrapped with a
     * {@link LongWrapper LongWrapper}
     * 
     * @return statusCodeDistribution
     */
    public Hashtable getStatusCodeDistribution() {
        return statusCodeDistribution;
    }
    
    /**
     * Returns the time (in millisec) when a URI belonging to a given host was
     * last finished processing. 
     * 
     * @param host The host to look up time of last completed URI.
     * @return Returns the time (in millisec) when a URI belonging to a given 
     * host was last finished processing. If no URI has been completed for host
     * -1 will be returned. 
     */
    public synchronized long getHostLastFinished(String host){
    	Long l = (Long)hostsLastFinished.get(host);
        return (l != null)? l.longValue(): -1;
    }

    /**
     * Returns the accumulated number of bytes downloaded from a given host.
     * @param host name of the host
     * @return the accumulated number of bytes downloaded from a given host
     */
    public synchronized long getBytesPerHost(String host){
        return ((LongWrapper)hostsBytes.get(host)).longValue;
    }

    /**
     * Returns the accumulated number of bytes from files of a given file type.
     * @param filetype Filetype to check.
     * @return the accumulated number of bytes from files of a given mime type
     */
    public long getBytesPerFileType(String filetype){
        return ((LongWrapper)mimeTypeBytes.get(filetype)).longValue;
    }

    /**
     * Get the total number of ToeThreads (sleeping and active)
     *
     * @return The total number of ToeThreads
     */
    public int threadCount() {
        return controller.getToeCount();
    }

    public int activeThreadCount() {
        return controller.getActiveToeCount();
        // note: reuse of old busy value seemed misleading: anyone asking
        // for thread count when paused or stopped still wants accurate reading
    }

    /**
     * This returns the number of completed URIs as a percentage of the total
     * number of URIs encountered (should be inverse to the discovery curve)
     *
     * @return The number of completed URIs as a percentage of the total
     * number of URIs encountered
     */
    public int percentOfDiscoveredUrisCompleted() {
        long completed = finishedUriCount();
        long total = discoveredUriCount();

        if (total == 0) {
            return 0;
        }

        return (int) (100 * completed / total);
    }

    /**
     * Number of <i>discovered</i> URIs.
     *
     * <p>If crawl not running (paused or stopped) this will return the value of
     * the last snapshot.
     *
     * @return A count of all uris encountered
     *
     * @see org.archive.crawler.framework.Frontier#discoveredUriCount()
     */
    public long discoveredUriCount() {
        // While shouldrun is true we can use info direct from the crawler.
        // After that our last snapshot will have to do.
        return shouldrun ?
            controller.getFrontier().discoveredUriCount() : discoveredUriCount;
    }

    /**
     * Number of URIs that have <i>finished</i> processing.
     *
     * @return Number of URIs that have finished processing
     *
     * @see org.archive.crawler.framework.Frontier#finishedUriCount()
     */
    public long finishedUriCount() {
        return shouldrun ?
            controller.getFrontier().finishedUriCount() : finishedUriCount;
    }

    /**
     * Get the total number of failed fetch attempts (connection failures -> give up, etc)
     *
     * @return The total number of failed fetch attempts
     */
    public long failedFetchAttempts() {
        // While shouldrun is true we can use info direct from the crawler.
        // After that our last snapshot will have to do.
        return shouldrun ?
            controller.getFrontier().failedFetchCount() : downloadFailures;
    }

    /**
     * Get the total number of failed fetch attempts (connection failures -> give up, etc)
     *
     * @return The total number of failed fetch attempts
     */
    public long disregardedFetchAttempts() {
        // While shouldrun is true we can use info direct from the crawler.
        // After that our last snapshot will have to do.
        return shouldrun ? controller.getFrontier().disregardedUriCount() : downloadDisregards;
    }

    public long successfullyFetchedCount() {
        // While shouldrun is true we can use info direct from the crawler.
        // After that our last snapshot will have to do.
        return shouldrun ?
            controller.getFrontier().succeededFetchCount() : downloadedUriCount;
    }
    
    public long totalCount() {
        return queuedUriCount() + activeThreadCount() +
            successfullyFetchedCount();
    }

    /**
     * Number of URIs <i>queued</i> up and waiting for processing.
     *
     * <p>If crawl not running (paused or stopped) this will return the value
     * of the last snapshot.
     *
     * @return Number of URIs queued up and waiting for processing.
     *
     * @see org.archive.crawler.framework.Frontier#queuedUriCount()
     */
    public long queuedUriCount() {
        // While shouldrun is true we can use info direct from the crawler.
        // After that our last snapshot will have to do.
        return shouldrun ?
            controller.getFrontier().queuedUriCount() : queuedUriCount;
    }

    public long totalBytesWritten() {
        return shouldrun ?
            controller.getFrontier().totalBytesWritten() : totalProcessedBytes;
    }

    /**
     * If the curi is a seed, we update the processedSeeds table.
     *
     * @param curi The CrawlURI that may be a seed.
     * @param disposition The dispositino of the CrawlURI.
     */
    private void handleSeed(CrawlURI curi, String disposition) {
        if(curi.isSeed()){
            SeedRecord sr = new SeedRecord(curi,disposition);
            processedSeedsRecords.put(sr.getUri(),sr);
        }
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.event.CrawlURIDispositionListener#crawledURISuccessful(org.archive.crawler.datamodel.CrawlURI)
     */
    public void crawledURISuccessful(CrawlURI curi) {
        handleSeed(curi,SEED_DISPOSITION_SUCCESS);
        // Save status codes
        incrementMapCount(statusCodeDistribution,Integer.toString(curi.getFetchStatus()));

        // Save mime types
        // strip things like charset (e.g. text/html; charset=iso-blah-blah)
        String mime = curi.getContentType();
        if(mime!=null)
        {
            int semicolonLoc = mime.indexOf(';');
            if (semicolonLoc >= 0) {
                mime = mime.substring(0, semicolonLoc);
            }
            mime = mime.toLowerCase();
        } else {
            mime = "not set";
        }
        incrementMapCount(mimeTypeDistribution, mime);
        incrementMapCount(mimeTypeBytes, mime, curi.getContentSize());

        // Save hosts stats.
        saveHostStats((curi.getFetchStatus() == 1)? "dns:":
                this.controller.getServerCache().
                getHostFor(curi).getHostName(),
                curi.getContentSize());
    }
    
    protected synchronized void saveHostStats(String hostname, long size) {
        incrementMapCount(hostsDistribution, hostname);
        incrementMapCount(hostsBytes, hostname, size);
        hostsLastFinished.put(hostname, new Long(System.currentTimeMillis()));
    }
    

    /* (non-Javadoc)
     * @see org.archive.crawler.event.CrawlURIDispositionListener#crawledURINeedRetry(org.archive.crawler.datamodel.CrawlURI)
     */
    public void crawledURINeedRetry(CrawlURI curi) {
        handleSeed(curi,SEED_DISPOSITION_RETRY);
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.event.CrawlURIDispositionListener#crawledURIDisregard(org.archive.crawler.datamodel.CrawlURI)
     */
    public void crawledURIDisregard(CrawlURI curi) {
        handleSeed(curi,SEED_DISPOSITION_DISREGARD);
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.event.CrawlURIDispositionListener#crawledURIFailure(org.archive.crawler.datamodel.CrawlURI)
     */
    public void crawledURIFailure(CrawlURI curi) {
        handleSeed(curi,SEED_DISPOSITION_FAILURE);
    }

    /**
     * Get a seed iterator for the job being monitored. 
     * 
     * <b>Note:</b> This iterator will iterate over a list of <i>strings</i> not
     * UURIs like the Scope seed iterator. The strings are equal to the URIs'
     * getURIString() values.
     * @return the seed iterator
     */
    public Iterator getSeeds() {
        List seedsCopy = new Vector();
        for(Iterator i = controller.getScope().seedsIterator(); i.hasNext();) {
            seedsCopy.add(((UURI)i.next()).toString());
        }
        return seedsCopy.iterator();
    }

    public Iterator getSeedRecordsSortedByStatusCode() {
        return getSeedRecordsSortedByStatusCode(getSeeds());
    }
    
    protected Iterator getSeedRecordsSortedByStatusCode(Iterator i) {
        TreeSet sortedSet = new TreeSet(new Comparator() {
            public int compare(Object e1, Object e2) {
                SeedRecord sr1 = (SeedRecord)e1;
                SeedRecord sr2 = (SeedRecord)e2;
                int ret = 0;
                int code1 = sr1.getStatusCode();
                int code2 = sr2.getStatusCode();
                if (code1 == code2) {
                    // If the values are equal, sort by URIs.
                    return sr1.getUri().compareTo(sr2.getUri());
                }
                // mirror and shift the nubmer line so as to
                // place zero at the beginning, then all negatives 
                // in order of ascending absolute value, then all 
                // positives descending
                code1 = -code1 - Integer.MAX_VALUE;
                code2 = -code2 - Integer.MAX_VALUE;
                
                return new Integer(code1).compareTo(new Integer(code2));
            }
        });
        while (i.hasNext()) {
            String seed = (String)i.next();
            SeedRecord sr = (SeedRecord) processedSeedsRecords.get(seed);
            if(sr==null) {
                sr = new SeedRecord(seed,SEED_DISPOSITION_NOT_PROCESSED);
                processedSeedsRecords.put(seed,sr);
            }
            sortedSet.add(sr);
        }
        return sortedSet.iterator();
    }

    public void crawlEnded(String sExitMessage) {
        logger.info("Entered crawlEnded");
        this.sExitMessage = sExitMessage; // held for reference by reports
        super.crawlEnded(sExitMessage);
        logger.info("Leaving crawlEnded");
    }
    
    public void crawlStarted(String message) {
        ;
    }
    
    /**
     * @param c CrawlController instance.
     * @return A summary of seeds crawled and not crawled.
     * @throws IOException
     */
    protected void writeSeedsReportTo(PrintWriter writer) throws IOException {
        // Build header.
        writer.print("[code] [status] [seed] [redirect]\n");

        seedsCrawled = 0;
        seedsNotCrawled = 0;
        for (Iterator i = getSeedRecordsSortedByStatusCode(getSeeds());
                i.hasNext();) {
            SeedRecord sr = (SeedRecord)i.next();
            writer.print(sr.getStatusCode());
            writer.print(" ");
            if((sr.getStatusCode() > 0)) {
                seedsCrawled++;
                writer.print("CRAWLED");
            } else {
                seedsNotCrawled++;
                writer.print("NOTCRAWLED");
            }
            writer.print(" ");
            writer.print(sr.getUri());
            if(sr.getRedirectUri()!=null) {
                writer.print(" ");
                writer.print(sr.getRedirectUri());
            }
            writer.print("\n");
        }
    }
    
    protected void writeHostsReportTo(PrintWriter writer) {
        SortedMap hd = getReverseSortedHostsDistribution();
        // header
        writer.print("[#urls] [#bytes] [host]\n");
        for (Iterator i = hd.keySet().iterator(); i.hasNext();) {
            // Key is 'host'.
            Object key = i.next();
            if (hd.get(key)!=null) {
                writer.print(((LongWrapper)hd.get(key)).longValue);
            } else {
                writer.print("-");
            }
            writer.print(" ");
            writer.print(getBytesPerHost((String)key));
            writer.print(" ");
            writer.print((String)key);
            writer.print("\n");
        }
    }
    
    /**
     * Return a copy of the hosts distribution in reverse-sorted
     * (largest first) order. 
     * @return SortedMap of hosts distribution
     */
    public SortedMap getReverseSortedHostsDistribution() {
        return getReverseSortedCopy(hostsDistribution);
    }

    protected void writeMimetypesReportTo(PrintWriter writer) throws IOException {
        // header
        writer.print("[#urls] [#bytes] [mime-types]\n");
        TreeMap fd = getReverseSortedCopy(getFileDistribution());
        for (Iterator i = fd.keySet().iterator(); i.hasNext();) {
            Object key = i.next();
            // Key is mime type.
            writer.print(Long.toString(((LongWrapper)fd.get(key)).longValue));
            writer.print(Long.toString(getBytesPerFileType((String)key)));
            writer.print(" ");
            writer.print((String)key);
            writer.print("\n");
        }
    }
    
    protected void writeResponseCodeReportTo(PrintWriter writer) throws IOException {
        int maxCodeLength = 10;
        // Build header.
        writer.print("[rescode] [#urls]\n");
        TreeMap scd = getReverseSortedCopy(getStatusCodeDistribution());
        for (Iterator i = scd.keySet().iterator(); i.hasNext();) {
            Object key = i.next();
            writer.print((String)key);
            writer.print(" ");
            writer.print(Long.toString(((LongWrapper)scd.get(key)).longValue));
            writer.print("\n");
        }
    }
    
    protected void writeCrawlReportTo(PrintWriter writer) throws IOException {
        writer.print("Crawl Name: " + controller.getOrder().getCrawlOrderName());
        writer.print("\nCrawl Status: " + sExitMessage);
        writer.print("\nDuration Time: " +
                ArchiveUtils.formatMillisecondsToConventional(crawlDuration()));
        writer.print("\nTotal Seeds Crawled: " + seedsCrawled);
        writer.print("\nTotal Seeds not Crawled: " + seedsNotCrawled);
        // hostsDistribution contains all hosts crawled plus an entry for dns.
        writer.print("\nTotal Hosts Crawled: " + (hostsDistribution.size()-1));
        writer.print("\nTotal Documents Crawled: " + finishedUriCount);
        writer.print("\nProcessed docs/sec: " +
                ArchiveUtils.doubleToString(docsPerSecond,2));
        writer.print("\nBandwidth in Kbytes/sec: " + totalKBPerSec);
        writer.print("\nTotal Raw Data Size in Bytes: " + totalProcessedBytes +
                " (" + ArchiveUtils.formatBytesForDisplay(totalProcessedBytes) +
                ") \n");
    }
    
    protected void writeProcessorsReportTo(PrintWriter writer) throws IOException {
        controller.reportTo(CrawlController.PROCESSORS_REPORT,writer);
    }
    
    protected void writeReportFile(String reportName, String filename) {
        File f = new File(controller.getDisk().getPath(), filename);
        try {
            PrintWriter bw = new PrintWriter(new FileWriter(f));
            writeReportTo(reportName,bw);
            bw.close();
            controller.addToManifest(f.getAbsolutePath(),
                CrawlController.MANIFEST_REPORT_FILE, true);
        } catch (IOException e) {
            Heritrix.addAlert(new Alert("Unable to write " + f.getName(),
                "Unable to write " + f.getAbsolutePath() +
                " at the end of crawl.", e, Level.SEVERE));
            e.printStackTrace();
        }
        logger.info("wrote report: "+f.getAbsolutePath());
    }
    
    /**
     * @param w
     * @throws IOException
     */
    protected void writeManifestReportTo(PrintWriter writer) throws IOException {
        controller.reportTo(CrawlController.MANIFEST_REPORT,writer);
    }
    
    /**
     * @param reportName
     * @param bw
     * @throws IOException
     */
    private void writeReportTo(String reportName, PrintWriter w) throws IOException {
        if("hosts".equals(reportName)) {
            writeHostsReportTo(w);
        } else if ("mime types".equals(reportName)) {
            writeMimetypesReportTo(w);
        } else if ("response codes".equals(reportName)) {
            writeResponseCodeReportTo(w);
        } else if ("seeds".equals(reportName)) {
            writeSeedsReportTo(w);
        } else if ("crawl".equals(reportName)) {
            writeCrawlReportTo(w);
        } else if ("processors".equals(reportName)) {
            writeProcessorsReportTo(w);
        } else if ("manifest".equals(reportName)) {
            writeManifestReportTo(w);
        } else if ("frontier".equals(reportName)) {
            writeFrontierReportTo(w);
        } /// TODO else default/error
    }

    /**
     * Write the Frontier's 'nonempty' report (if available)
     * @param writer to report to
     * @throws IOException
     */
    protected void writeFrontierReportTo(PrintWriter writer) throws IOException {
        if(controller.getFrontier().isEmpty()) {
            writer.println("frontier empty");
        } else {
            controller.getFrontier().reportTo("nonempty",writer);
        }
    }

    /**
     * Run the reports.
     */
    public void dumpReports() {
        // Add all files mentioned in the crawl order to the
        // manifest set.
        controller.addOrderToManifest();
        writeReportFile("hosts","hosts-report.txt");
        writeReportFile("mime types","mimetype-report.txt");
        writeReportFile("response codes","responsecode-report.txt");
        writeReportFile("seeds","seeds-report.txt");
        writeReportFile("crawl","crawl-report.txt");
        writeReportFile("processors","processors-report.txt");
        writeReportFile("manifest","crawl-manifest.txt");
        writeReportFile("frontier","frontier-report.txt");
        // TODO: Save object to disk?
    }

    public void checkpoint(Checkpoint cp) throws Exception {
        // TODO: Not called because we don't register ourselves
        // yet.  See #initialize above.
        cp.writeObjectToFile(this,
            cp.getClassCheckpointFilename(this.getClass()));
    }
}