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
package org.archive.crawler.framework;

import static org.archive.crawler.datamodel.CoreAttributeConstants.A_SOURCE_TAG;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.OpenDataException;

import org.apache.commons.collections.Closure;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.event.CrawlURIDispositionEvent;
import org.archive.crawler.util.CrawledBytesHistotable;
import org.archive.modules.net.CrawlHost;
import org.archive.modules.net.ServerCache;
import org.archive.modules.net.ServerCacheUtil;
import org.archive.modules.seeds.SeedModuleImpl;
import org.archive.net.UURI;
import org.archive.settings.file.BdbModule;
import org.archive.spring.ConfigPath;
import org.archive.util.ArchiveUtils;
import org.archive.util.LongWrapper;
import org.archive.util.MimetypeUtils;
import org.archive.util.PaddingStringBuffer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;

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
 *   <li> Successfully downloaded documents per host per source
 * </ul>
 *
 * @author Parker Thompson
 * @author Kristinn Sigurdsson
 *
 * @see org.archive.crawler.framework.StatisticsTracker
 * @see org.archive.crawler.framework.AbstractTracker
 */
public class StatisticsTrackerImpl extends AbstractTracker
implements Serializable {
    private static final long serialVersionUID = 8004878315916392305L;

    public enum Reports{
        FILETYPE_BYTES,
        FILETYPE_URIS,
        STATUSCODE,
        HOST_BYTES,
        HOST_URIS,
        HOST_LAST_ACTIVE
    }

    protected SeedModuleImpl seeds;
    public SeedModuleImpl getSeeds() {
        return this.seeds;
    }
    @Autowired
    public void setSeeds(SeedModuleImpl seeds) {
        this.seeds = seeds;
    }

    protected BdbModule bdb;
    @Autowired
    public void setBdbModule(BdbModule bdb) {
        this.bdb = bdb;
    }

    protected ConfigPath reportsDir = new ConfigPath(EngineImpl.REPORTS_DIR_NAME,".");
    public ConfigPath getReportsDir() {
        return reportsDir;
    }
    public void setReportsDir(ConfigPath reportsDir) {
        this.reportsDir = reportsDir;
    }
    
    protected ServerCache serverCache;
    public ServerCache getServerCache() {
        return this.serverCache;
    }
    @Autowired
    public void setServerCache(ServerCache serverCache) {
        this.serverCache = serverCache;
    }
    
    protected int liveHostReportSize = 20;
    public int getLiveHostReportSize() {
        return liveHostReportSize;
    }
    public void setLiveHostReportSize(int liveHostReportSize) {
        this.liveHostReportSize = liveHostReportSize;
    }
    
    /**
     * Messages from the StatisticsTracker.
     */
    private final static Logger logger =
        Logger.getLogger(StatisticsTrackerImpl.class.getName());
    
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
    protected float congestionRatio = 0; 
    protected long deepestUri;
    protected long averageDepth;
    
    /*
     * Cumulative data
     */
    /** tally sizes novel, verified (same hash), vouched (not-modified) */ 
    protected CrawledBytesHistotable crawledBytes = new CrawledBytesHistotable();
    
    /** Keep track of the file types we see (mime type -> count) */
    protected Hashtable<String,LongWrapper> mimeTypeDistribution
     = new Hashtable<String,LongWrapper>();
    protected Hashtable<String,LongWrapper> mimeTypeBytes
     = new Hashtable<String,LongWrapper>();
    
    /** Keep track of fetch status codes */
    protected Hashtable<String,LongWrapper> statusCodeDistribution
     = new Hashtable<String,LongWrapper>();
    
    /** Keep track of hosts. 
     * 
     * Each of these Maps are individually unsynchronized, and cannot 
     * be trivially synchronized with the Collections wrapper. Thus
     * their synchronized access is enforced by this class.
     */
    protected Map<String,LongWrapper> hostsDistribution = null;
    protected Map<String,LongWrapper> hostsBytes = null;
    protected Map<String,Long> hostsLastFinished = null;

    /** Keep track of URL counts per host per seed */
    protected  
    Map<String,HashMap<String,LongWrapper>> sourceHostDistribution = null;

    /* Keep track of 'top' hosts for live reports */
    protected LargestSet hostsDistributionTop;
    protected LargestSet hostsBytesTop;
    protected LargestSet hostsLastFinishedTop;
    
    /**
     * Record of seeds' latest actions.
     */
    protected Map<String,SeedRecord> processedSeedsRecords;

    // seeds tallies: ONLY UPDATED WHEN SEED REPORT WRITTEN
    private int seedsCrawled;
    private int seedsNotCrawled;

    public StatisticsTrackerImpl() {
        
    }
    
    @Override
    public void start() {
        super.start();
        try {
            this.sourceHostDistribution = bdb.getBigMap("sourceHostDistribution",
            	    false, String.class, HashMap.class);
            this.hostsDistribution = bdb.getBigMap("hostsDistribution",
                false, String.class, LongWrapper.class);
            this.hostsBytes = bdb.getBigMap("hostsBytes", false, String.class,
                LongWrapper.class);
            this.hostsLastFinished = bdb.getBigMap("hostsLastFinished",
                false, String.class, Long.class);
            this.processedSeedsRecords = bdb.getBigMap("processedSeedsRecords",
                    false, String.class, SeedRecord.class);
            
            this.hostsDistributionTop = new LargestSet(getLiveHostReportSize());
            this.hostsBytesTop = new LargestSet(getLiveHostReportSize());
            this.hostsLastFinishedTop = new LargestSet(getLiveHostReportSize());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
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
        if (this.processedSeedsRecords != null) {
            this.processedSeedsRecords.clear();
            this.processedSeedsRecords = null;
        }
        if (this.sourceHostDistribution != null) {
            this.sourceHostDistribution.clear();
            this.sourceHostDistribution = null;
        }

    }

    protected synchronized void progressStatisticsEvent(final EventObject e) {
        // This method loads "snapshot" data.
        discoveredUriCount = discoveredUriCount();
        downloadedUriCount = successfullyFetchedCount();
        finishedUriCount = finishedUriCount();
        queuedUriCount = queuedUriCount();
        downloadFailures = failedFetchAttempts();
        downloadDisregards = disregardedFetchAttempts();
        totalProcessedBytes = totalBytesCrawled();
        congestionRatio = congestionRatio();
        deepestUri = deepestUri();
        averageDepth = averageDepth();
        
        if (finishedUriCount() == 0) {
            docsPerSecond = 0;
            totalKBPerSec = 0;
        } else if (getCrawlerTotalElapsedTime() < 1000) {
            return; // Not enough time has passed for a decent snapshot.
        } else {
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
            if (sampleTime >= 1000) {
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

        if (this.controller != null) {
            this.controller.logProgressStatistics(getProgressStatisticsLine());
        }
        lastLogPointTime = System.currentTimeMillis();
        super.progressStatisticsEvent(e);
    }

    /**
     * Return one line of current progress-statistics
     * 
     * @param now
     * @return String of stats
     */
    public String getProgressStatisticsLine(Date now) {
        return new PaddingStringBuffer()
            .append(ArchiveUtils.getLog14Date(now))
            .raAppend(32, discoveredUriCount)
            .raAppend(44, queuedUriCount)
            .raAppend(57, downloadedUriCount)
            .raAppend(74, ArchiveUtils.
                doubleToString(currentDocsPerSecond, 2) +
                "(" + ArchiveUtils.doubleToString(docsPerSecond, 2) + ")")
            .raAppend(85, currentKBPerSec + "(" + totalKBPerSec + ")")
            .raAppend(99, downloadFailures)
            .raAppend(113, busyThreads)
            .raAppend(126, (Runtime.getRuntime().totalMemory() -
                Runtime.getRuntime().freeMemory()) / 1024)
            .raAppend(140, Runtime.getRuntime().totalMemory() / 1024)
            .raAppend(153, ArchiveUtils.doubleToString(congestionRatio, 2))
            .raAppend(165, deepestUri)
            .raAppend(177, averageDepth)
            .toString();
    }
    
    public Map<String,Number> getProgressStatistics() {
        Map<String,Number> stats = new HashMap<String,Number>();
        stats.put("discoveredUriCount", new Long(discoveredUriCount));
        stats.put("queuedUriCount", new Long(queuedUriCount));
        stats.put("downloadedUriCount", new Long(downloadedUriCount));
        stats.put("currentDocsPerSecond", new Double(currentDocsPerSecond));
        stats.put("docsPerSecond", new Double(docsPerSecond));
        stats.put("totalKBPerSec", new Long(totalKBPerSec));
        stats.put("totalProcessedBytes", new Long(totalProcessedBytes));
        stats.put("currentKBPerSec", new Long(currentKBPerSec));
        stats.put("downloadFailures", new Long(downloadFailures));
        stats.put("busyThreads", new Integer(busyThreads));
        stats.put("congestionRatio", new Double(congestionRatio));
        stats.put("deepestUri", new Long(deepestUri));
        stats.put("averageDepth", new Long(averageDepth));
        stats.put("totalMemory", new Long(Runtime.getRuntime().totalMemory()));
        stats.put("freeMemory", new Long(Runtime.getRuntime().freeMemory()));
        return stats;
    }

    /**
     * Return one line of current progress-statistics
     * 
     * @return String of stats
     */
    public String getProgressStatisticsLine() {
        return getProgressStatisticsLine(new Date());
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
    public Hashtable<String,LongWrapper> getFileDistribution() {
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
    protected static void incrementMapCount(Map<String,LongWrapper> map, 
            String key) {
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
    protected static void incrementMapCount(Map<String,LongWrapper> map, 
            String key, long increment) {
        if (key == null) {
            key = "unknown";
        }
        Object o = map.get(key);
        if (o == null) {
            // Considered normal
            map.put(key, new LongWrapper(increment));
        } else if (o instanceof LongWrapper) {
            LongWrapper lw = (LongWrapper)o;
            lw.longValue += increment;
        } else {
            // Abnormal
            logger.severe("Resetting " + key + ": Expected LongWrapper but got " 
                    + o.getClass().getName());
            map.put(key, new LongWrapper(increment));
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
    public TreeMap<String,LongWrapper> getReverseSortedCopy(
            final Map<String,LongWrapper> mapOfLongWrapperValues) {
        TreeMap<String,LongWrapper> sortedMap = 
          new TreeMap<String,LongWrapper>(new Comparator<String>() {
            public int compare(String e1, String e2) {
                long firstVal = mapOfLongWrapperValues.get(e1).
                    longValue;
                long secondVal = mapOfLongWrapperValues.get(e2).
                    longValue;
                if (firstVal < secondVal) {
                    return 1;
                }
                if (secondVal < firstVal) {
                    return -1;
                }
                // If the values are the same, sort by keys.
                return e1.compareTo(e2);
            }
        });
        try {
            sortedMap.putAll(mapOfLongWrapperValues);
        } catch (UnsupportedOperationException e) {
            Iterator<String> i = mapOfLongWrapperValues.keySet().iterator();
            for (;i.hasNext();) {
                // Ok. Try doing it the slow way then.
                String key = i.next();
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
    public Hashtable<String,LongWrapper> getStatusCodeDistribution() {
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
    public long getHostLastFinished(String host){
        Long l = null;
        synchronized(hostsLastFinished){
            l = (Long)hostsLastFinished.get(host);
        }
        return (l != null)? l.longValue(): -1;
    }

    /**
     * Returns the accumulated number of bytes downloaded from a given host.
     * @param host name of the host
     * @return the accumulated number of bytes downloaded from a given host
     */
    public long getBytesPerHost(String host){
        synchronized(hostsBytes){
            return getReportValue(hostsBytes, host);
        }
    }

    /**
     * Returns the accumulated number of bytes from files of a given file type.
     * @param filetype Filetype to check.
     * @return the accumulated number of bytes from files of a given mime type
     */
    public long getBytesPerFileType(String filetype){
        return getReportValue(mimeTypeBytes, filetype);
    }

    /**
     * Get the total number of ToeThreads (sleeping and active)
     *
     * @return The total number of ToeThreads
     */
    public int threadCount() {
        return this.controller != null? controller.getToeCount(): 0;
    }

    /**
     * @return Current thread count (or zero if can't figure it out).
     */ 
    public int activeThreadCount() {
        return this.controller != null? controller.getActiveToeCount(): 0;
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
        return shouldrun && this.controller != null &&
                this.controller.getFrontier() != null?
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
        return shouldrun && this.controller != null &&
                this.controller.getFrontier() != null ?
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
        return shouldrun && this.controller != null &&
                this.controller.getFrontier() != null ?
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
        return shouldrun && this.controller != null &&
                this.controller.getFrontier() != null?
            controller.getFrontier().disregardedUriCount() : downloadDisregards;
    }

    public long successfullyFetchedCount() {
        // While shouldrun is true we can use info direct from the crawler.
        // After that our last snapshot will have to do.
        return shouldrun && this.controller != null &&
                this.controller.getFrontier() != null?
            controller.getFrontier().succeededFetchCount() : downloadedUriCount;
    }
    
    public long totalCount() {
        return queuedUriCount() + activeThreadCount() +
            successfullyFetchedCount();
    }

    /**
     * Ratio of number of threads that would theoretically allow
     * maximum crawl progress (if each was as productive as current
     * threads), to current number of threads.
     * 
     * @return float congestion ratio 
     */
    public float congestionRatio() {
        // While shouldrun is true we can use info direct from the crawler.
        // After that our last snapshot will have to do.
        return shouldrun && this.controller != null &&
                this.controller.getFrontier() != null ?
            controller.getFrontier().congestionRatio() : congestionRatio;
    }
    
    /**
     * Ordinal position of the 'deepest' URI eligible 
     * for crawling. Essentially, the length of the longest
     * frontier internal queue. 
     * 
     * @return long URI count to deepest URI
     */
    public long deepestUri() {
        // While shouldrun is true we can use info direct from the crawler.
        // After that our last snapshot will have to do.
        return shouldrun && this.controller != null &&
                this.controller.getFrontier() != null ?
            controller.getFrontier().deepestUri() : deepestUri;
    }
    
    /**
     * Average depth of the last URI in all eligible queues.
     * That is, the average length of all eligible queues.
     * 
     * @return long average depth of last URIs in queues 
     */
    public long averageDepth() {
        // While shouldrun is true we can use info direct from the crawler.
        // After that our last snapshot will have to do.
        return shouldrun && this.controller != null &&
                this.controller.getFrontier() != null ?
            controller.getFrontier().averageDepth() : averageDepth;
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
        return shouldrun && this.controller != null &&
                this.controller.getFrontier() != null?
            controller.getFrontier().queuedUriCount() : queuedUriCount;
    }

    /** @deprecated use totalBytesCrawled */
    public long totalBytesWritten() {
        return shouldrun && this.controller != null &&
                this.controller.getFrontier() != null?
            controller.getFrontier().totalBytesWritten() : totalProcessedBytes;
    }

    public long totalBytesCrawled() {
        return shouldrun ?
            crawledBytes.getTotal() : totalProcessedBytes;
    }
            
    public String crawledBytesSummary() {
        return crawledBytes.summary();
    }
    
    /**
     * If the curi is a seed, we update the processedSeeds table.
     *
     * @param curi The CrawlURI that may be a seed.
     * @param disposition The dispositino of the CrawlURI.
     */
    private void handleSeed(CrawlURI curi, String disposition) {
        if(curi.isSeed()){
            SeedRecord sr = new SeedRecord(curi, disposition);
            processedSeedsRecords.put(sr.getUri(), sr);
        }
    }

    public void crawledURISuccessful(CrawlURI curi) {
        handleSeed(curi,SEED_DISPOSITION_SUCCESS);
        // save crawled bytes tally
        crawledBytes.accumulate(curi);

        // Save status codes
        incrementMapCount(statusCodeDistribution,
            Integer.toString(curi.getFetchStatus()));

        // Save mime types
        String mime = MimetypeUtils.truncate(curi.getContentType());
        incrementMapCount(mimeTypeDistribution, mime);
        incrementMapCount(mimeTypeBytes, mime, curi.getContentSize());

        // Save hosts stats.
        ServerCache sc = serverCache;
        saveHostStats((curi.getFetchStatus() == 1)? "dns:":
                ServerCacheUtil.getHostFor(sc, curi.getUURI()).getHostName(),
                curi.getContentSize());
        
        if (curi.getData().containsKey(A_SOURCE_TAG)) {
        	saveSourceStats((String)curi.getData().get(A_SOURCE_TAG),
                        ServerCacheUtil.getHostFor(sc, curi.getUURI()).
                    getHostName()); 
        }
    }
         
    protected void saveSourceStats(String source, String hostname) {
        synchronized(sourceHostDistribution) {
            HashMap<String,LongWrapper> hostUriCount = 
                sourceHostDistribution.get(source);
            if (hostUriCount == null) {
                hostUriCount = new HashMap<String,LongWrapper>();
            }
            // TODO: Dan suggests we don't need a hashtable value.  Might
            // be faster if we went without. Could just have keys of:
            //  seed | host (concatenated as string)
            // and values of: 
            //  #urls
            incrementMapCount(hostUriCount, hostname);
            sourceHostDistribution.put(source, hostUriCount);
        }
    }
    
    protected void saveHostStats(String hostname, long size) {
        synchronized(hostsDistribution){
            incrementMapCount(hostsDistribution, hostname);
            hostsDistributionTop.update(
                    hostname, getReportValue(hostsDistribution, hostname)); 
        }
        synchronized(hostsBytes){
            incrementMapCount(hostsBytes, hostname, size);
            hostsBytesTop.update(hostname, 
                    getReportValue(hostsBytes, hostname));
        }
        synchronized(hostsLastFinished){
            long time = new Long(System.currentTimeMillis());
            hostsLastFinished.put(hostname, time);
            hostsLastFinishedTop.update(hostname, time);
        }
    }

    public void crawledURINeedRetry(CrawlURI curi) {
        handleSeed(curi,SEED_DISPOSITION_RETRY);
    }

    public void crawledURIDisregard(CrawlURI curi) {
        handleSeed(curi,SEED_DISPOSITION_DISREGARD);
    }

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
     * FIXME: Consider using TransformingIterator here
     */
    public Iterator<String> getSeedsIterator() {
        List<String> seedsCopy = new Vector<String>();
        Iterator<UURI> i = seeds.seedsIterator();
        while (i.hasNext()) {
            seedsCopy.add(i.next().toString());
        }
        return seedsCopy.iterator();
    }

    public Iterator<SeedRecord> getSeedRecordsSortedByStatusCode() {
        return getSeedRecordsSortedByStatusCode(getSeedsIterator());
    }
    
    protected Iterator<SeedRecord> getSeedRecordsSortedByStatusCode(
            Iterator<String> i) {
        TreeSet<SeedRecord> sortedSet = 
          new TreeSet<SeedRecord>(new Comparator<SeedRecord>() {
            public int compare(SeedRecord sr1, SeedRecord sr2) {
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
            String seed = i.next();
            SeedRecord sr = (SeedRecord) processedSeedsRecords.get(seed);
            if(sr==null) {
                sr = new SeedRecord(seed,SEED_DISPOSITION_NOT_PROCESSED);
                processedSeedsRecords.put(seed,sr);
            }
            sortedSet.add(sr);
        }
        return sortedSet.iterator();
    }
    
    /**
     * @param writer Where to write.
     */
    public void writeSeedsReportTo(PrintWriter writer) {
        // Build header.
        writer.print("[code] [status] [seed] [redirect]\n");

        seedsCrawled = 0;
        seedsNotCrawled = 0;
        for (Iterator<SeedRecord> i = getSeedRecordsSortedByStatusCode(getSeedsIterator());
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
    
    protected void writeSourceReportTo(PrintWriter writer) {
        
        writer.print("[source] [host] [#urls]\n");
        // for each source
        for (Iterator i = sourceHostDistribution.keySet().iterator(); i.hasNext();) {
            Object sourceKey = i.next();
            Map<String,LongWrapper> hostCounts 
             = (Map<String,LongWrapper>)sourceHostDistribution.get(sourceKey);
            // sort hosts by #urls
            SortedMap sortedHostCounts = getReverseSortedHostCounts(hostCounts);
            // for each host
            for (Iterator j = sortedHostCounts.keySet().iterator(); j.hasNext();) {
                Object hostKey = j.next();
                LongWrapper hostCount = (LongWrapper) hostCounts.get(hostKey);
                writer.print(sourceKey.toString());
                writer.print(" ");
                writer.print(hostKey.toString());
                writer.print(" ");
                writer.print(hostCount.longValue);
                writer.print("\n");
            }
        }
    }

    /**
     * Write a report of all contexts (prefixes) to associated sheet names.
     * 
     * @param writer
     */
    protected void writeAssociationsReportTo(PrintWriter writer) {
        
        writer.print("[context] [sheet name]\n");
        // for each source
        //TODO:SPRINGY
//        for(String context : controller.getSheetManager().getContexts()) {
//            for(String sheetName : controller.getSheetManager().getAssociations(context)) {
//                writer.print(context);
//                writer.print(" ");
//                writer.print(sheetName);
//                writer.println();
//            }
//        }
    }
    
    /**
     * Return a copy of the hosts distribution in reverse-sorted (largest first)
     * order.
     * 
     * @return SortedMap of hosts distribution
     */
    public SortedMap getReverseSortedHostCounts(
            Map<String,LongWrapper> hostCounts) {
        synchronized(hostCounts){
            return getReverseSortedCopy(hostCounts);
        }
    }

    
    protected void writeHostsReportTo(final PrintWriter writer) {
        // TODO: use CrawlHosts for all stats; only perform sorting on 
        // manageable number of hosts
        SortedMap hd = getReverseSortedHostsDistribution();
        // header
        writer.print("[#urls] [#bytes] [host] [#robots] [#remaining]\n");
        for (Iterator i = hd.keySet().iterator(); i.hasNext();) {
            // Key is 'host'.
            String key = (String) i.next();
            CrawlHost host = serverCache.getHostFor(key);
            LongWrapper val = (LongWrapper)hd.get(key);
            writeReportLine(writer,
                    ((val==null)?"-":val.longValue),
                    getBytesPerHost(key),
                    key,
                    host.getSubstats().getRobotsDenials(),
                    host.getSubstats().getRemaining());
        }
        // StatisticsTracker doesn't know of zero-completion hosts; 
        // so supplement report with those entries from host cache
        Closure logZeros = new Closure() {
            public void execute(Object obj) {
                CrawlHost host = (CrawlHost)obj;
                if(host.getSubstats().getRecordedFinishes()==0) {
                    writeReportLine(writer,
                            host.getSubstats().getRecordedFinishes(),
                            host.getSubstats().getTotalBytes(),
                            host.getHostName(),
                            host.getSubstats().getRobotsDenials(),
                            host.getSubstats().getRemaining());
                }
            }};
        serverCache.forAllHostsDo(logZeros);
    }
    
    protected void writeReportLine(PrintWriter writer, Object  ... fields) {
       for(Object field : fields) {
           writer.print(field);
           writer.print(" ");
       }
       writer.print("\n");
    }
    
    /**
     * Return a copy of the hosts distribution in reverse-sorted
     * (largest first) order. 
     * @return SortedMap of hosts distribution
     */
    public SortedMap getReverseSortedHostsDistribution() {
        synchronized(hostsDistribution){
            return getReverseSortedCopy(hostsDistribution);
        }
    }

    protected void writeMimetypesReportTo(PrintWriter writer) {
        // header
        writer.print("[#urls] [#bytes] [mime-types]\n");
        TreeMap fd = getReverseSortedCopy(getFileDistribution());
        for (Iterator i = fd.keySet().iterator(); i.hasNext();) {
            Object key = i.next();
            // Key is mime type.
            writer.print(Long.toString(((LongWrapper)fd.get(key)).longValue));
            writer.print(" ");
            writer.print(Long.toString(getBytesPerFileType((String)key)));
            writer.print(" ");
            writer.print((String)key);
            writer.print("\n");
        }
    }
    
    protected void writeResponseCodeReportTo(PrintWriter writer) {
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
    
    protected void writeCrawlReportTo(PrintWriter writer) {
        writer.print("Crawl Name: " + controller.getMetadata().getJobName());
        writer.print("\nCrawl Status: " + controller.getCrawlExitStatus().desc);
        writer.print("\nDuration Time: " +
                ArchiveUtils.formatMillisecondsToConventional(crawlDuration()));
        writer.print("\nTotal Seeds Crawled: " + seedsCrawled);
        writer.print("\nTotal Seeds not Crawled: " + seedsNotCrawled);
        // hostsDistribution contains all hosts crawled plus an entry for dns.
        writer.print("\nTotal Hosts Crawled: " + (hostsDistribution.size()-1));
        writer.print("\nTotal URIs Processed: " + finishedUriCount);
        writer.print("\nURIs Crawled successfully: " + downloadedUriCount);
        writer.print("\nURIs Failed to Crawl: " + downloadFailures);
        writer.print("\nURIs Disregarded: " + downloadDisregards);
        writer.print("\nProcessed docs/sec: " +
                ArchiveUtils.doubleToString(docsPerSecond,2));
        writer.print("\nBandwidth in Kbytes/sec: " + totalKBPerSec);
        writer.print("\nTotal Raw Data Size in Bytes: " + totalProcessedBytes +
                " (" + ArchiveUtils.formatBytesForDisplay(totalProcessedBytes) +
                ") \n");
        writer.print("Novel Bytes: " 
                + crawledBytes.get(CrawledBytesHistotable.NOVEL)
                + " (" + ArchiveUtils.formatBytesForDisplay(
                        crawledBytes.get(CrawledBytesHistotable.NOVEL))
                +  ") \n");
        if(crawledBytes.containsKey(CrawledBytesHistotable.DUPLICATE)) {
            writer.print("Duplicate-by-hash Bytes: " 
                    + crawledBytes.get(CrawledBytesHistotable.DUPLICATE)
                    + " (" + ArchiveUtils.formatBytesForDisplay(
                            crawledBytes.get(CrawledBytesHistotable.DUPLICATE))
                    +  ") \n");
        }
        if(crawledBytes.containsKey(CrawledBytesHistotable.NOTMODIFIED)) {
            writer.print("Not-modified Bytes: " 
                    + crawledBytes.get(CrawledBytesHistotable.NOTMODIFIED)
                    + " (" + ArchiveUtils.formatBytesForDisplay(
                            crawledBytes.get(CrawledBytesHistotable.NOTMODIFIED))
                    +  ") \n");
        }        
    }
    
    protected void writeProcessorsReportTo(PrintWriter writer) {
        controller.reportTo(CrawlControllerImpl.PROCESSORS_REPORT,writer);
    }
    
    protected void writeReportFile(String reportName, String filename) {
        File f = new File(getReportsDir().getFile(), filename);
        try {
            PrintWriter bw = new PrintWriter(new FileWriter(f));
            writeReportTo(reportName, bw);
            bw.close();
            loggerModule.addToManifest(f.getAbsolutePath(),
                CrawlerLoggerModule.MANIFEST_REPORT_FILE, true);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to write " + f.getAbsolutePath() +
                " at the end of crawl.", e);
        }
        logger.info("wrote report: " + f.getAbsolutePath());
    }
    
    /**
     * @param writer Where to write.
     */
    protected void writeManifestReportTo(PrintWriter writer) {
        controller.reportTo(CrawlControllerImpl.MANIFEST_REPORT, writer);
    }
    
    /**
     * @param reportName Name of report.
     * @param w Where to write.
     */
    private void writeReportTo(String reportName, PrintWriter w) {
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
        } else if ("source".equals(reportName)) {
            writeSourceReportTo(w);
        } else if ("associations".equals(reportName)) {
            writeAssociationsReportTo(w);
        }// / TODO else default/error
    }

    /**
     * Write the Frontier's 'nonempty' report (if available)
     * @param writer to report to
     */
    protected void writeFrontierReportTo(PrintWriter writer) {
        if(controller.getFrontier().isEmpty()) {
            writer.println("frontier empty");
        } else {
            controller.getFrontier().reportTo("nonempty", writer);
        }
    }

    /**
     * Run the reports.
     */
    public void dumpReports() {
        // Add all files mentioned in the crawl order to the
        // manifest set.
        //controller.addOrderToManifest();
        writeReportFile("hosts","hosts-report.txt");
        writeReportFile("mime types","mimetype-report.txt");
        writeReportFile("response codes","responsecode-report.txt");
        writeReportFile("seeds","seeds-report.txt");
        writeReportFile("crawl","crawl-report.txt");
        writeReportFile("processors","processors-report.txt");
        writeReportFile("manifest","crawl-manifest.txt");
        writeReportFile("frontier","frontier-report.txt");
        if (!sourceHostDistribution.isEmpty()) {
            writeReportFile("source","source-report.txt");
        }
        writeReportFile("associations","associations-report.txt");
    }

    public void crawlCheckpoint(/*StateProvider*/ Object def, File cpDir) throws Exception {
        // CrawlController is managing the checkpointing of this object.
        logNote("CRAWL CHECKPOINTING TO " + cpDir.toString());
    }

    
    
    public String[] getReportKeys(String report) {
        Reports rep = Reports.valueOf(report);
        switch(rep){
        case FILETYPE_BYTES : 
            return (String[])mimeTypeBytes.keySet().toArray(new String[0]);
        case FILETYPE_URIS : 
            return (String[])mimeTypeDistribution.keySet().toArray(new String[0]);
        case HOST_BYTES :
            return (String[])hostsBytesTop.keySet();
        case HOST_LAST_ACTIVE :
            return (String[])hostsLastFinishedTop.keySet();
        case HOST_URIS :
            return (String[])hostsDistributionTop.keySet();
        case STATUSCODE :
            return (String[])statusCodeDistribution.keySet().toArray(new String[0]);
        }
        return null;
    }

    
    private long getReportValue(Map<String,LongWrapper> map, String key) {
        if (key == null) {
            return -1;
        }
        Object o = map.get(key);
        if (o == null) {
            return -2;
        }
        if (!(o instanceof LongWrapper)) {
            throw new IllegalStateException("Expected LongWrapper but got " 
                    + o.getClass() + " for " + key);
        }
        return ((LongWrapper)o).longValue;
    }
    
    
    public long getReportValue(String report, String key) {
        Reports rep = Reports.valueOf(report);
        switch(rep){
        case FILETYPE_BYTES : 
            return getReportValue(mimeTypeBytes, key);
        case FILETYPE_URIS : 
            return getReportValue(mimeTypeDistribution, key);
        case HOST_BYTES :
            return getReportValue(hostsBytes, key);
        case HOST_LAST_ACTIVE :
            return hostsLastFinished.get(key);
        case HOST_URIS :
            return getReportValue(hostsDistribution, key);
        case STATUSCODE :
            return getReportValue(statusCodeDistribution, key);
        }
        return -1;
    }
    
    public void onApplicationEvent(ApplicationEvent event) {
        super.onApplicationEvent(event); 
        if(event instanceof CrawlURIDispositionEvent) {
            CrawlURIDispositionEvent dvent = (CrawlURIDispositionEvent)event;
            switch(dvent.getDisposition()) {
                case SUCCEEDED:
                    this.crawledURISuccessful(dvent.getCrawlURI());
                    break;
                case FAILED:
                    this.crawledURIFailure(dvent.getCrawlURI());
                    break;
                case DISREGARDED:
                    this.crawledURIDisregard(dvent.getCrawlURI());
                    break;
                case DEFERRED_FOR_RETRY:
                    this.crawledURINeedRetry(dvent.getCrawlURI());
                    break;
                default:
                    throw new RuntimeException("Unknown disposition: " + dvent.getDisposition());
            }
        }
    }
}

class LargestSet implements Serializable {
    
    private static final long serialVersionUID = 1L;

    int maxsize;
    HashMap<String, Long> set;
    long smallestKnownValue;
    String smallestKnownKey;
    
    public LargestSet(int size){
        maxsize = size;
        set = new HashMap<String, Long>(size);
    }
    
    public void update(String key, long value){
        if(set.containsKey(key)) {
            // Update the value of an existing key
            set.put(key,value); 
            // This may promote the key if it was the smallest
            if(smallestKnownKey == null || smallestKnownKey.equals(key)){
                updateSmallest();
            }
        } else if(set.size()<maxsize) {
            // Can add a new key/value pair as we still have space
            set.put(key, value);
            // Check if this is new smallest known value
            if(value<smallestKnownValue){
                smallestKnownValue = value;
                smallestKnownKey = key;
            }
        } else {
            // Determine if value is large enough for inclusion
            if(value>smallestKnownValue){
                // Replace current smallest
                set.remove(smallestKnownKey);
                updateSmallest();
                set.put(key, value);
            } // Else do nothing.
        }
    }
    
    private void updateSmallest(){
        // Need to scan through for new smallest value.
        long oldSmallest = smallestKnownValue;
        smallestKnownValue = Long.MAX_VALUE;
        for(String k : set.keySet()){
            long v = set.get(k);
            if(v<smallestKnownValue){
                smallestKnownValue = v;
                smallestKnownKey = k;
                if(v==oldSmallest){
                    // Found another key matching old smallest known value
                    // Can not be anything smaller.
                    return;
                }
            }
        }
    }
    
    public String[] keySet(){
        return set.keySet().toArray(new String[0]);
    }
}
