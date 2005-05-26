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
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
implements CrawlURIDispositionListener {
    /**
     * Messages from the StatisticsTracker.
     */
    private final static Logger logger =
        Logger.getLogger(StatisticsTracker.class.getName());
    
    // TODO: Class needs to be serializable.
    // TODO: Need to be able to specify file where the object will be
    // written once the CrawlEnded event occurs
    // TODO: Need to be able to save object on Checkpointing as well
    // as CrawlEnded.

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
    
    /** Keep track of hosts */
    protected Map hostsDistribution = null;
    protected Map hostsBytes = null;
    protected Map hostsLastFinished = null;

    /** Keep track of processed seeds disposition*/
    protected Map processedSeedsDisposition = new Hashtable();

    /** Keep track of processed seeds status codes*/
    protected Map processedSeedsStatusCodes = new Hashtable();

    /** Cache seed list.
     */
    protected List allSeeds = new Vector();


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
            this.hostsBytes = BigMapFactory.getBigMap(c.getSettingsHandler(),
                    "hostsBytes", String.class, LongWrapper.class);
            // This map is different from the above in that it doesn't
            // increment a value per host.  Because of this, updates
            // don't go via the incrementMapCount method which takes
            // care to synchronize #puts.  This means any put into the
            // below HashMap needs to be in a synchronize block.
            this.hostsLastFinished =
                BigMapFactory.getBigMap(c.getSettingsHandler(),
                    "hostsLastFinished", String.class, Long.class);
        } catch (Exception e) {
            throw new FatalConfigurationException("Failed setup of" +
                " StatisticsTracker: " + e);
        }
        controller.addCrawlURIDispositionListener(this);
    }
    
    protected void cleanup() {
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
            Level.INFO, new PaddingStringBuffer()
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
                .toString());
        lastLogPointTime = System.currentTimeMillis();
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
        // TODO: Check this synchronized block is not a bottleneck.
        synchronized (map) {
            LongWrapper lw = (LongWrapper)map.get(key);
            if(lw == null) {
                map.put(key, new LongWrapper((long)1));
            } else {
                lw.longValue += increment;
            }
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
        synchronized (mapOfLongWrapperValues) {
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
     * Return a Hashtable representing the distribution of hosts for
     * successfully fetched curis, as represented by a hashmap where
     * key -&gt; val represents (string)code -&gt; (integer)count.
     *
     * <b>Note:</b> All the values are wrapped with a
     * {@link LongWrapper LongWrapper}
     *
     * @return Hosts distribution as a Hashtable
     */
    public Map getHostsDistribution() {
        return hostsDistribution;
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
    	Long l = (Long)hostsLastFinished.get(host);
        return (l != null)? l.longValue(): -1;
    }

    /**
     * Returns the accumulated number of bytes downloaded from a given host.
     * @param host name of the host
     * @return the accumulated number of bytes downloaded from a given host
     */
    public long getBytesPerHost(String host){
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

    public String getSeedDisposition(String UriString){
        String ret = SEED_DISPOSITION_NOT_PROCESSED;
        if(processedSeedsDisposition.containsKey(UriString)){
            ret = (String)processedSeedsDisposition.get(UriString);
        }
        return ret;
    }

    /**
     * Returns the status code of any seed. If the supplied URL is not a seed
     * or a seed that has not been crawled it will return zero.
     * @param UriString The URI of the seed
     * @return the disposition of the seed
     *
     */
    public int getSeedStatusCode(String UriString){
        int ret = 0;
        if(processedSeedsStatusCodes.containsKey(UriString)){
            ret = ((Integer)processedSeedsStatusCodes.get(UriString)).intValue();
        }
        return ret;
    }

    /**
     * If the curi is a seed, we update the processedSeeds table.
     *
     * @param curi The CrawlURI that may be a seed.
     * @param disposition The dispositino of the CrawlURI.
     */
    private void handleSeed(CrawlURI curi, String disposition) {
        if(curi.isSeed()){
            processedSeedsDisposition.put(curi.toString(), disposition);
            processedSeedsStatusCodes.put(curi.toString(),
                    new Integer(curi.getFetchStatus()));
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
    
    protected void saveHostStats(String hostname, long size) {
        incrementMapCount(hostsDistribution, hostname);
        incrementMapCount(hostsBytes, hostname, size);
        synchronized(this.hostsLastFinished) {
            hostsLastFinished.put(hostname,
                new Long(System.currentTimeMillis()));
        }
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
     * Get a seed iterator for the job being monitored. If job is no longer
     * running, stored values will be returned. If job is running, current
     * seed iterator will be fetched and stored values will be updated.
     * <p>
     * <b>Note:</b> This iterator will iterate over a list of <i>strings</i> not
     * UURIs like the Scope seed iterator. The strings are equal to the URIs'
     * getURIString() values.
     * @return the seed iterator
     */
    public Iterator getSeeds() {
        if (this.shouldrun) {
            this.allSeeds = getSeeds(this.controller);
        }
        return this.allSeeds.iterator();
    }
    
    protected List getSeeds(CrawlController c) {
        List seedsCopy = new Vector();
        for(Iterator i = c.getScope().seedsIterator(); i.hasNext();) {
            seedsCopy.add(((UURI)i.next()).toString());
        }
        return seedsCopy;
    }

    public Iterator getSeedsSortedByStatusCode() {
        return getSeedsSortedByStatusCode(getSeeds());
    }
    
    protected Iterator getSeedsSortedByStatusCode(Iterator i) {
        TreeSet sortedSet = new TreeSet(new Comparator() {
            public int compare(Object e1, Object e2) {
                int firstCode = getSeedStatusCode((String) e1);
                int secondCode = getSeedStatusCode((String) e2);
                int ret = 0;

                if (firstCode == secondCode) {
                    // If the values are equal, sort by URIs.
                    String firstURI = (String) e1;
                    String secondURI = (String) e2;
                    ret = firstURI.compareTo(secondURI);
                } else if ( (firstCode > 0 && secondCode > 0)
                          ||(firstCode < 0 && secondCode < 0) ){
                    // Both are either positve or negative,
                    // sort from largest to smallest
                    if (firstCode < secondCode) {
                        ret = 1;
                    } else {
                        ret = -1;
                    }
                } else {
                    // Negative or zero come before positive status codes.
                    if(firstCode > 0 || (firstCode < 0 && secondCode == 0 )) {
                        ret = 1;
                    } else {
                        ret = -1;
                    }
                }
                return ret;
            }
        });
        while (i.hasNext()) {
            sortedSet.add(i.next());
        }
        return sortedSet.iterator();
    }

    public void crawlEnded(String sExitMessage) {
        logger.info("Entered crawlEnded");
        CrawlController c = this.controller;
        super.crawlEnded(sExitMessage);
        report(c, sExitMessage);
        cleanup();
        logger.info("Leaving crawlEnded");
    }
    

    /* (non-Javadoc)
     * @see org.archive.crawler.event.CrawlStatusListener#crawlStarted(java.lang.String)
     */
    public void crawlStarted(String message) {
        // TODO Auto-generated method stub
        
    }
    
    /**
     * @param i Iterator.
     * @return Length of longest URL.
     */
    protected int getLongestString(Iterator i) {
        int max = 0;
        for (; i != null && i.hasNext();) {
            String tmp = (String)i.next();
            if (tmp != null && tmp.length() > max) {
                max = tmp.length();
            }
        }
        return max;
    }
    
    /**
     * @param i Iterator over map keys.
     * @return Get longest map entry key.
     */
    protected int getLongestMapEntryKey(Iterator i) {
        int max = 0;
        for (; i.hasNext();) {
            String key = i.next().toString();
            if (key != null && key.length() > max) {
                max = key.length();
            }
        }
        return max;
    }
    
    /**
     * @param c CrawlController instance.
     * @return A summary of seeds crawled and not crawled.
     */
    protected SeedsSummary writeSeedsReport(CrawlController c) {
        int maxURILength = getLongestString(getSeeds(c).iterator());
        // Ok, we now know how much space to allocate the seed name colum
        PaddingStringBuffer rep = new PaddingStringBuffer();

        // Build header.
        rep.append("[seeds]");
        rep.raAppend(maxURILength + 11, "[res-code]");
        rep.append(" [status]");
        rep.newline();

        int seedsCrawled = 0;
        int seedsNotCrawled = 0;
        for (Iterator i = getSeedsSortedByStatusCode(getSeeds(c).iterator());
                i.hasNext();) {
            String UriString = (String)i.next();
            int code = getSeedStatusCode(UriString);
            rep.append(UriString);
            rep.raAppend(maxURILength + 11, code);
            if (code > 0) {
                rep.append(" CRAWLED");
                seedsCrawled++;
            } else {
                rep.append(" NOTCRAWLED");
                seedsNotCrawled++;
            }
            rep.newline();
        }
        
        writeReport(c, "seeds-report.txt", rep.toString());
        return new SeedsSummary(seedsCrawled, seedsNotCrawled);
    }
    
    protected void writeHostsReport(CrawlController c) {
        int maxHostLength = getLongestMapEntryKey(getHostsDistribution().
            keySet().iterator());
        // Ok, we now know how much space to allocate the seed name colum
        PaddingStringBuffer rep = new PaddingStringBuffer();
        // Build header.
        rep.append("[host]");
        rep.raAppend(maxHostLength + 13, "[#urls]");
        rep.raAppend(maxHostLength + 26, "[#bytes]");
        rep.newline();
        TreeMap hd = getReverseSortedCopy(getHostsDistribution());
        for (Iterator i = hd.keySet().iterator(); i.hasNext();) {
            // Key is 'host'.
            Object key = i.next();
            rep.append((String)key);
            rep.raAppend(maxHostLength + 13,
                    ((LongWrapper)hd.get(key)).longValue);
            rep.raAppend(maxHostLength + 26, getBytesPerHost((String)key));
            rep.newline();
        }

        writeReport(c, "hosts-report.txt", rep.toString());
    }
    
    protected void writeMimetypesReport(CrawlController c) {
        int maxMimeLength = getLongestMapEntryKey(getFileDistribution().
            keySet().iterator());

        // Ok, we now know how much space to allocate the seed name colum
        PaddingStringBuffer rep = new PaddingStringBuffer();

        // Build header.
        rep.append("[mime-types]");
        rep.raAppend(maxMimeLength + 13, "[#urls]");
        rep.raAppend(maxMimeLength + 26, "[#bytes]");
        rep.newline();
        TreeMap fd = getReverseSortedCopy(getFileDistribution());
        for (Iterator i = fd.keySet().iterator(); i.hasNext();) {
            Object key = i.next();
            // Key is 'host'.
            rep.append((String)key);
            rep.raAppend(maxMimeLength + 13,
                    ((LongWrapper)fd.get(key)).longValue);
            rep.raAppend(maxMimeLength + 26, getBytesPerFileType((String)key));
            rep.newline();
        }
        writeReport(c, "mimetype-report.txt", rep.toString());
    }
    
    protected void writeResponseCodeReport(CrawlController c) {
        int maxCodeLength = 10;
        PaddingStringBuffer rep = new PaddingStringBuffer();
        // Build header.
        rep.append("[rescode]");
        rep.raAppend(maxCodeLength+13, "[#urls]");
        rep.newline();
        TreeMap scd = getReverseSortedCopy(getStatusCodeDistribution());
        for (Iterator i = scd.keySet().iterator(); i.hasNext();) {
            Object key = i.next();
            rep.append((String)key);
            rep.raAppend(maxCodeLength + 13,
                ((LongWrapper)scd.get(key)).longValue);
            rep.newline();
        }
        
        writeReport(c, "responsecode-report.txt", rep.toString());
    }
    
    protected void writeCrawlReport(CrawlController c, String exitMessage,
            SeedsSummary seedsSummary) {
        PaddingStringBuffer rep = new PaddingStringBuffer();
        rep.append("Crawl Name: " + c.getOrder().getCrawlOrderName());
        rep.newline();
        rep.append("Crawl Status: " + exitMessage);
        rep.newline();
        rep.append("Duration Time: " +
                ArchiveUtils.formatMillisecondsToConventional(crawlDuration()));
        rep.newline();
        rep.append("Total Seeds Crawled: " + seedsSummary.getCrawled());
        rep.newline();
        rep.append("Total Seeds not Crawled: " + seedsSummary.getNotCrawled());
        rep.newline();
        // hostsDistribution contains all hosts crawled plus an entry for dns.
        rep.append("Total Hosts Crawled: " + (hostsDistribution.size()-1));
        rep.newline();
        rep.append("Total Documents Crawled: " + finishedUriCount);
        rep.newline();
        rep.append("Processed docs/sec: " +
                ArchiveUtils.doubleToString(docsPerSecond,2));
        rep.newline();
        rep.append("Bandwidth in Kbytes/sec: " + totalKBPerSec);
        rep.newline();
        rep.append("Total Raw Data Size in Bytes: " + totalProcessedBytes +
                " (" + ArchiveUtils.formatBytesForDisplay(totalProcessedBytes) +
                ") ");
        rep.newline();

        writeReport(c, "crawl-report.txt", rep.toString());
    }
    
    protected void wroteProcessorsReport(CrawlController c) {
        writeReport(c, "processors-report.txt", c.reportProcessors());
    }
    
    protected void writeReport(CrawlController c, String name,
            String content) {
        File f = new File(c.getDisk().getPath(), name);
        try {
            FileWriter fw = new FileWriter(f);
            fw.write(content);
            fw.close();
            c.addToManifest(f.getAbsolutePath(),
                CrawlController.MANIFEST_REPORT_FILE, true);
        } catch (IOException e) {
            Heritrix.addAlert(new Alert("Unable to write " + f.getName(),
                "Unable to write " + f.getAbsolutePath() +
                " at the end of crawl.", e, Level.SEVERE));
            e.printStackTrace();
        }
        logger.info(f.getAbsolutePath());
    }
    
    /**
     * Run the reports.
     * @param c A CrawlController instance.
     * @param exitMessage
     */
    public void report(CrawlController c, String exitMessage) {
        // Add all files mentioned in the crawl order to the
        // manifest set.
        c.addOrderToManifest();
        writeHostsReport(c);
        writeMimetypesReport(c);
        writeResponseCodeReport(c);
        SeedsSummary seedsSummary = writeSeedsReport(c);
        writeCrawlReport(c, exitMessage, seedsSummary);
        wroteProcessorsReport(c);
        writeReport(c, "crawl-manifest.txt", c.getManifest());
        // TODO: Save object to disk?
    }
    
    protected class SeedsSummary {
        final int crawled;
        final int notCrawled;
        protected SeedsSummary(int crawled, int notCrawled) {
            this.crawled = crawled;
            this.notCrawled = notCrawled;
        }
        public int getCrawled() {
            return this.crawled;
        }
        public int getNotCrawled() {
            return this.notCrawled;
        }
    }
}


