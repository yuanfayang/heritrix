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

import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Level;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.framework.AbstractTracker;
import org.archive.util.ArchiveUtils;
import org.archive.util.PaddingStringBuffer;

/**
 * This is an implementation of the AbstractTracker. It is designed to function
 * with the WUI as well as performing various logging activity.
 * <p>
 * At the end of each snapshot a line is written to the progress-statistics.log file.
 * <p>
 * The header of that file is as follows:
 * <pre> [timestamp] [discovered]    [queued] [downloaded] [doc/s(avg)]  [KB/s(avg)] [dl-failures] [busy-thread] [mem-use-KB]</pre>
 * First there is a time stamp, accurate down to 1 second.
 * <p>
 * <b>discovered</b>, <b>queued</b>, <b>downloaded</b> and <b>dl-failures</b> are (respectively) the discovered URI count,
 * pending URI count, successfully fetched count and failed fetch count from the frontier at
 * the time of the snapshot.
 * <p>
 * KB/s(avg) is the bandwidth usage.  We use the total bytes downloaded to calculate average
 * bandwidth usage (KB/sec). Since we also note the value each time a snapshot is made we can
 * calculate the average bandwidth usage during the last snapshot period to gain a "current" rate.
 * The first number is the current and the average is in parenthesis.
 * <p>
 * doc/s(avg) works the same way as doc/s except it show the number of documents (URIs) rather then
 * KB downloaded.
 * <p>
 * busy-threads is the total number of ToeThreads that are not available (and thus presumably busy processing a URI).
 * This information is extracted from the crawl controller.
 * <p>
 * Finally mem-use-KB is extracted from the run time environment (Runtime.getRuntime().totalMemory()).
 *
 * @author Parker Thompson
 * @author Kristinn Sigurdsson
 *
 * @see org.archive.crawler.framework.StatisticsTracking
 * @see org.archive.crawler.framework.AbstractTracker
 */
public class StatisticsTracker extends AbstractTracker{

    protected long lastPagesFetchedCount = 0;
    protected long lastProcessedBytesCount = 0;

    /*
     * Snapshot data.
     */
    protected long discoveredPages = 0;
    protected long pendingPages = 0;
    protected long downloadedPages = 0;
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
    protected Hashtable hostsDistribution = new Hashtable();
    protected Hashtable hostsBytes = new Hashtable();

    /** Keep track of processed seeds disposition*/
    protected Hashtable processedSeedsDisposition = new Hashtable();

    /** Keep track of processed seeds status codes*/
    protected Hashtable processedSeedsStatusCodes = new Hashtable();

    /** Cache seed list */
    protected Vector allSeeds = new Vector();

    /** Seed successfully crawled */
    public static final String SEED_DISPOSITION_SUCCESS =
        "Seed successfully crawled";
    /** Failed to crawl seed */
    public static final String SEED_DISPOSITION_FAILURE =
        "Failed to crawl seed";
    /** Failed to crawl seed, will retry */
    public static final String SEED_DISPOSITION_RETRY =
        "Failed to crawl seed, will retry";
    /** Seed was disregarded */
    public static final String SEED_DISPOSITION_DISREGARD =
        "Seed was disregarded";
    /** Seed has not been processed */
    public static final String SEED_DISPOSITION_NOT_PROCESSED =
        "Seed has not been processed";

    public StatisticsTracker(String name) {
        super(
            name,
            "A statistics tracker that's been designed to work well with the web UI and creates the progress-statistics log.");
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.AbstractTracker#logActivity()
     */
    protected synchronized void logActivity() {
        // This method loads "snapshot" data.
        discoveredPages = urisEncounteredCount();
        pendingPages = urisInFrontierCount();
        downloadedPages = successfulFetchAttempts();
        downloadFailures = failedFetchAttempts();
        downloadDisregards = disregardedFetchAttempts();
        totalProcessedBytes = getTotalBytesWritten();

        if(totalFetchAttempts() == 0){
            docsPerSecond = 0;
            totalKBPerSec = 0;
        }
        else if(getCrawlerTotalElapsedTime() < 1000){
            return; //Not enough time has passed for a decent snapshot.
        }
        else{
            docsPerSecond = (double) downloadedPages / (double)(getCrawlerTotalElapsedTime() / 1000);
            totalKBPerSec = (long)(((totalProcessedBytes / 1024) / ((getCrawlerTotalElapsedTime())    / 1000)) + .5 ); // round to nearest long
        }

        busyThreads = activeThreadCount();

        if(shouldrun || (System.currentTimeMillis() - lastLogPointTime) >= 1000)
        {
            // If shouldrun is false there is a chance that the time interval since
            // last time is too small for a good sample.  We only want to update
            // "current" data when the interval is long enough or shouldrun is true.
            currentDocsPerSecond = 0;
            currentKBPerSec = 0;

            // Note time.
            long currentTime = System.currentTimeMillis();
            long sampleTime = currentTime - lastLogPointTime;

            // if we haven't done anyting or there isn't a reasonable sample size give up.
            if(sampleTime >= 1000)
            {

                // Update docs/sec snapshot
                long currentPageCount = successfulFetchAttempts();
                long samplePageCount = currentPageCount - lastPagesFetchedCount;

                currentDocsPerSecond = (double) samplePageCount / (double)(sampleTime / 1000);

                lastPagesFetchedCount = currentPageCount;

                // Update kbytes/sec snapshot
                long currentProcessedBytes = totalProcessedBytes;
                long sampleProcessedBytes = currentProcessedBytes - lastProcessedBytesCount;

                currentKBPerSec = (int) (((sampleProcessedBytes/1024) / (sampleTime / 1000)) + .5);

                lastProcessedBytesCount = currentProcessedBytes;
            }
        }

        Date now = new Date();
        periodicLogger.log(
            Level.INFO,
            new PaddingStringBuffer()
                .append(ArchiveUtils.TIMESTAMP14.format(now))
                .raAppend(26, discoveredPages)
                .raAppend(38, pendingPages)
                .raAppend(51, downloadedPages)
                .raAppend(66, ArchiveUtils.doubleToString(currentDocsPerSecond,2) + "(" + ArchiveUtils.doubleToString(docsPerSecond,2) + ")")
                .raAppend(79, currentKBPerSec + "(" + totalKBPerSec + ")")
                .raAppend(93, downloadFailures)
                .raAppend(107, busyThreads)
                .raAppend(120, Runtime.getRuntime().totalMemory() / 1024)
                .toString());


        lastLogPointTime = System.currentTimeMillis();
    }


    /**
     * Returns the number of documents that have been processed
     * per second over the life of the crawl (as of last snapshot)
     *
     * @return  The rate per second of documents gathered so far
     */
    public double processedDocsPerSec(){
        return docsPerSecond;
    }

    /**
     * Returns an estimate of recent document download rates
     * based on a queue of recently seen CrawlURIs (as of last snapshot.)
     *
     * @return The rate per second of documents gathered during the last snapshot
     */
    public double currentProcessedDocsPerSec(){
        return currentDocsPerSecond;
    }

    /**
     * Calculates the rate that data, in kb, has been processed
     * over the life of the crawl (as of last snapshot.)
     *
     * @return The rate per second of KB gathered so far
     */
    public long processedKBPerSec(){
        return totalKBPerSec;
    }

    /**
     * Calculates an estimate of the rate, in kb, at which documents
     * are currently being processed by the crawler.  For more
     * accurate estimates set a larger queue size, or get
     * and average multiple values (as of last snapshot).
     *
     * @return The rate per second of KB gathered during the last snapshot
     */
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
    protected static void incrementMapCount(Hashtable map, String key) {
    	incrementMapCount(map,key,1);
    }
    
    protected static void incrementMapCount(Hashtable map, String key, long increment) {
        if (key == null) {
            key = "unknown";
        }

        if (map.containsKey(key)) {
            ((LongWrapper) map.get(key)).longValue+=increment;

        } else {
            // if we didn't find this key add it
            synchronized(map){
                map.put(key, new LongWrapper(1));
            }
        }
    }


    /**
     * Sort the entries of the given HashMap in descending order
     * by their values, which must be Numbers
     * @param map Assumes values are wrapped with LongWrapper.
     * @return
     */
    public TreeSet getSortedByValue(Hashtable map) {
        TreeSet sortedSet = new TreeSet(
            new Comparator() {
                public int compare(Object e1, Object e2) {
                    long firstVal = ((LongWrapper)((Map.Entry)e1).getValue()).longValue;
                    long secondVal = ((LongWrapper)((Map.Entry)e2).getValue()).longValue;
                    if(firstVal < secondVal){
                        return 1;
                    } if(secondVal < firstVal){
                        return -1;
                    }
                    // If the values are the same, sort by keys.
                    String firstKey = (String)((Map.Entry)e1).getKey();
                    String secondKey = (String)((Map.Entry)e2).getKey();
                    return firstKey.compareTo(secondKey);
                }
            });
        sortedSet.addAll(map.entrySet());
        return sortedSet;
    }

    /** Return a HashMap representing the distribution of status codes for
     *  successfully fetched curis, as represented by a hashmap where
     *  key -> val represents (string)code -> (integer)count
     * <p>
     * <b>Note:</b> All the values are wrapped with a {@link LongWrapper LongWrapper}
     * @return statusCodeDistribution
     */
    public Hashtable getStatusCodeDistribution() {
        return statusCodeDistribution;
    }

    /** Return a Hashtable representing the distribution of hosts for
     *  successfully fetched curis, as represented by a hashmap where
     *  key -> val represents (string)code -> (integer)count
     * <p>
     * <b>Note:</b> All the values are wrapped with a {@link LongWrapper LongWrapper}
     * @return Hosts distribution as a Hashtable
     */
    public Hashtable getHostsDistribution() {
        return hostsDistribution;
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
     * @param filetype name of the mime type
     * @return the accumulated number of bytes from files of a given mime type
     */
    public long getBytesPerFileType(String filetype){
        return ((LongWrapper)mimeTypeBytes.get(filetype)).longValue;
    }

    /**
     * Get the total number of ToeThreads  (sleeping and active)
     *
     * @return The total number of ToeThreads
     */
    public int threadCount() {
        return controller.getToeCount();
    }

    /**
     * Get the number of active (non-paused) threads.
     * <p>
     * If crawl not running (paused or stopped) this will return the value of the last snapshot.
     *
     * @return The number of active (non-paused) threads
     */
    public int activeThreadCount() {
        return shouldrun ? controller.getActiveToeCount() : busyThreads;
    }

    /**
     * Get the number of URIs in the frontier (found but not processed).
     * <p>
     * If crawl not running (paused or stopped) this will return the value of the last snapshot.
     *
     * @return The number of URIs in the frontier (found but not processed)
     *
     * @see org.archive.crawler.framework.URIFrontier#pendingUriCount()
     */
    public long urisInFrontierCount() {

        // While shouldrun is true we can use info direct from the crawler.
        // After that our last snapshot will have to do.
        return shouldrun ? controller.getFrontier().pendingUriCount() : pendingPages;
    }

    /**
     * This returns the number of completed URIs as a percentage of the total
     * number of URIs encountered (should be inverse to the discovery curve)
     *
     * @return The number of completed URIs as a percentage of the total
     * number of URIs encountered
     */
    public int percentOfDiscoveredUrisCompleted() {
        long completed = totalFetchAttempts();
        long total = urisEncounteredCount();

        if (total == 0) {
            return 0;
        }

        return (int) (100 * completed / total);
    }

    /**
     * Returns a count of all uris encountered.  This includes both the frontier
     * (unfetched pages) and fetched pages/failed fetch attempts.
     * <p>
     * If crawl not running (paused or stopped) this will return the value of the last snapshot.
     *
     * @return A count of all uris encountered
     *
     * @see org.archive.crawler.framework.URIFrontier#discoveredUriCount()
     */
    public long urisEncounteredCount() {
        // While shouldrun is true we can use info direct from the crawler.
        // After that our last snapshot will have to do.
        return shouldrun ? controller.getFrontier().discoveredUriCount() : discoveredPages;
    }

    /**
     * Get the total number of URIs where fetches have been attempted.
     *
     * @return Equal to the sum of {@link StatisticsTracker#successfulFetchAttempts() successfulFetchAttempts()}
     * and {@link StatisticsTracker#failedFetchAttempts() failedFetchAttempts()}
     */
    public long totalFetchAttempts() {
        return successfulFetchAttempts() + failedFetchAttempts();
    }

    /**
     * Get the total number of failed fetch attempts (connection failures -> give up, etc)
     *
     * @return The total number of failed fetch attempts
     */
    public long failedFetchAttempts() {
        // While shouldrun is true we can use info direct from the crawler.
        // After that our last snapshot will have to do.
        return shouldrun ? controller.getFrontier().failedFetchCount() : downloadFailures;
    }

    /**
     * Get the total number of failed fetch attempts (connection failures -> give up, etc)
     *
     * @return The total number of failed fetch attempts
     */
    public long disregardedFetchAttempts() {
        // While shouldrun is true we can use info direct from the crawler.
        // After that our last snapshot will have to do.
        return shouldrun ? controller.getFrontier().disregardedFetchCount() : downloadDisregards;
    }

    /**
     * Get the number of successul document fetches.
     * <p>
     * If crawl not running (paused or stopped) this will return the value of the last snapshot.
     *
     * @return The number of successul document fetches
     *
     * @see org.archive.crawler.framework.URIFrontier#successfullyFetchedCount()
     */
    public long successfulFetchAttempts() {
        // While shouldrun is true we can use info direct from the crawler.
        // After that our last snapshot will have to do.
        return shouldrun ? controller.getFrontier().successfullyFetchedCount() : downloadedPages;
    }

    /**
     * Returns the total number of uncompressed bytes written to disk.  This may
     * be different from the actual number if you are using compression.
     *
     * @return The total number of uncompressed bytes written to disk
     */
    public long getTotalBytesWritten() {
        return shouldrun ? controller.getFrontier().totalBytesWritten() : totalProcessedBytes;
    }

    /**
     * Returns the disposition of any seed. If the supplied URL is not a seed
     * it will always return 'not processed'
     * @param UriString The URI of the seed
     * @return the disposition of the seed
     *
     * @see #SEED_DISPOSITION_NOT_PROCESSED
     * @see #SEED_DISPOSITION_SUCCESS
     * @see #SEED_DISPOSITION_FAILURE
     * @see #SEED_DISPOSITION_DISREGARD
     * @see #SEED_DISPOSITION_RETRY
     */
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
        if(curi.getIsSeed()){
            processedSeedsDisposition.put(curi.getURIString(),disposition);
            processedSeedsStatusCodes.put(curi.getURIString(),new Integer(curi.getFetchStatus()));
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
        }
        incrementMapCount(mimeTypeDistribution, mime);
        incrementMapCount(mimeTypeBytes,mime,curi.getContentSize());

        // Save hosts
        if(curi.getFetchStatus()==1){
            // DNS Lookup, handle it differently.
            incrementMapCount(hostsDistribution, "dns:");
            incrementMapCount(hostsBytes,"dns:",curi.getContentSize());
        } else {
        	incrementMapCount(hostsDistribution, curi.getServer().getHostname());
            incrementMapCount(hostsBytes,curi.getServer().getHostname(),curi.getContentSize());
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
    public Iterator getSeeds(){
        if(shouldrun){
            Iterator tmp = controller.getScope().getSeedsIterator();
            allSeeds = new Vector();
            while(tmp.hasNext()){
                String s = ((UURI)tmp.next()).getURIString();
                allSeeds.add(s);
            }
        }
        return allSeeds.iterator();
    }

    /**
     * Get a seed iterator for the job being monitored. If job is no longer
     * running, stored values will be returned. If job is running, current
     * seed iterator will be fetched and stored values will be updated.
     * <p>
     * Sort order is:<br>
     * No status code (not processed)<br>
     * Status codes smaller then 0 (largest to smallest)<br>
     * Status codes larger then 0 (largest to smallest)<br>
     * <p>
     * <b>Note:</b> This iterator will iterate over a list of <i>strings</i> not
     * UURIs like the Scope seed iterator. The strings are equal to the URIs'
     * getURIString() values.
     * @return the seed iterator
     */
    public Iterator getSeedsSortedByStatusCode() {
        Iterator tmp = getSeeds();
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
        while (tmp.hasNext()) {
            sortedSet.add(tmp.next());
        }
        return sortedSet.iterator();
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.event.CrawlStatusListener#crawlEnded(java.lang.String)
     */
    public void crawlEnded(String sExitMessage) {
        Iterator tmp = getSeeds(); //Make sure to save the seeds list.

        // Save seed report to reports.log
        int maxURILenght = 0;
        while(tmp.hasNext()){
            String tmpString = (String)tmp.next();
            if(tmpString.length()>maxURILenght){
                maxURILenght = tmpString.length();
            }
        }

        //Ok, we now know how much space to allocate the seed name colum
        //now build the report.
        PaddingStringBuffer rep = new PaddingStringBuffer();
        rep.append("----=== Seed disposition report ===----");
        rep.newline();
        rep.append("Seeds");
        rep.padTo(maxURILenght+2);
        rep.raAppend(maxURILenght+8,"Code");
        rep.padTo(maxURILenght+9);
        rep.append("Disposition");
        rep.newline();
        rep.append("-------------");
        rep.padTo(maxURILenght+2);
        rep.raAppend(maxURILenght+8,"-----");
        rep.padTo(maxURILenght+9);
        rep.append("-------------");
        rep.newline();

        tmp = getSeedsSortedByStatusCode();
        while(tmp.hasNext()){
            String UriString = (String)tmp.next();
            String disposition = getSeedDisposition(UriString);
            int code = getSeedStatusCode(UriString);
            String statusCode = "";
            if(code != 0){
                statusCode = Integer.toString(code);
            }
            rep.append(UriString);
            rep.padTo(maxURILenght+2);
            rep.raAppend(maxURILenght+8,code);
            rep.padTo(maxURILenght+9);
            rep.append(disposition);
            rep.newline();
        }
        rep.append("----=== End seed disposition report ===----");
        rep.newline();
        rep.newline();
        rep.newline();
        controller.reports.info(rep.toString()); //Write report to file.

        super.crawlEnded(sExitMessage);
    }

}


