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

import java.util.Date;
import java.util.HashMap;
import java.util.logging.Level;

import org.archive.crawler.datamodel.CrawlURI;
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
	protected int docsPerSecond = 0;
	protected int currentDocsPerSecond = 0;
	protected int currentKBPerSec = 0;
	protected long totalKBPerSec = 0;
	protected long downloadFailures = 0;
	protected int busyThreads = 0; 
	protected long totalProcessedBytes = 0;

	/*
	 * Cumulative data 
	 */
	/** Keep track of the file types we see (mime type -> count) */
	protected HashMap mimeTypeDistribution = new HashMap();
	/** Keep track of fetch status codes */
	protected HashMap statusCodeDistribution = new HashMap();
	/** Keep track of hosts */
	protected HashMap hostsDistribution = new HashMap();
	
	public StatisticsTracker(String name) {
		super(name, "A statistics tracker that's been designed to work well with the web UI and creates the progress-statistics log.");
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
		totalProcessedBytes = getTotalBytesWritten();
		
		if(totalFetchAttempts() == 0){
			docsPerSecond = 0;
			totalKBPerSec = 0;
		}
		else if(getCrawlerTotalElapsedTime() < 1000){
            return; //Not enough time has passed for a decent snapshot.
        }
        else{
			docsPerSecond = (int)(downloadedPages / ((getCrawlerTotalElapsedTime()) / 1000) + .5); // rounded to nearest int
			totalKBPerSec = (long)(((totalProcessedBytes / 1024) / ((getCrawlerTotalElapsedTime())	/ 1000)) + .5 ); // round to nearest long
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
			
				currentDocsPerSecond = (int) (samplePageCount / (sampleTime / 1000) + .5);
			
				lastPagesFetchedCount = currentPageCount;

				// Update kbytes/sec snapshot
				long currentProcessedBytes = totalProcessedBytes;
				long sampleProcessedBytes = currentProcessedBytes - lastProcessedBytesCount;

				currentKBPerSec = (int) (((sampleProcessedBytes/1024) / (sampleTime / 1000)) + .5);
		
				lastProcessedBytesCount = currentProcessedBytes;
			}
		}
		
		
		// 			mercator style log entry was
		//			[timestamp] [discovered-pages] [pending-pages] [downloaded-pages]
		//			[unique-pages] \
		//			[overall-docs-per-sec] [current-docs-per-sec] [overall-KB-sec]
		//			[current-KB-sec] \
		//			[download-failures] [stalled-threads] [memmory-usage]
		
		Date now = new Date();
		periodicLogger.log(
			Level.INFO,
			new PaddingStringBuffer()
				 .append(ArchiveUtils.TIMESTAMP14.format(now))
				 .raAppend(26,discoveredPages)
			 .raAppend(38,pendingPages)
			 .raAppend(51,downloadedPages)
			 .raAppend(64,currentDocsPerSecond+"("+docsPerSecond+")")
			 .raAppend(77,currentKBPerSec+"("+totalKBPerSec+")")
			 .raAppend(91,downloadFailures)
			 .raAppend(105,busyThreads)
			 .raAppend(118,Runtime.getRuntime().totalMemory()/1024)
			 .toString()
		);
		

		lastLogPointTime = System.currentTimeMillis();
	}
	

	/** 
	 * Returns the number of documents that have been processed
	 * per second over the life of the crawl (as of last snapshot)
	 * 
	 * @return  The rate per second of documents gathered so far
	 */
	public int processedDocsPerSec(){
		return docsPerSecond;
	}
	
	/** 
	 * Returns an estimate of recent document download rates
	 * based on a queue of recently seen CrawlURIs (as of last snapshot.)
	 * 
	 * @return The rate per second of documents gathered during the last snapshot
	 */
	public int currentProcessedDocsPerSec(){
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
	 * @return mimeTypeDistribution
	 */
	public HashMap getFileDistribution() {
		return mimeTypeDistribution;
	}


	/** 
	 * Increment a counter for a key in a given HashMap. Used for various
	 * aggregate data.
	 * 
	 * @param map The HashMap
	 * @param key The key for the counter to be incremented, if it does not 
	 * 	          exist it will be added (set to 1).  If null it will 
	 *            increment the counter "unknown".
	 */
	protected static void incrementMapCount(HashMap map, String key) {

		if (key == null) {
			key = "unknown";
		}

		if (map.containsKey(key)) {

			Integer matchValue = (Integer) map.get(key);
			matchValue = new Integer(matchValue.intValue() + 1);
			map.put(key, matchValue);

		} else {
			// if we didn't find this key add it
			map.put(key, new Integer(1));
		}
	}

	/** Return a HashMap representing the distribution of status codes for
	 *  successfully fetched curis, as represented by a hashmap where
	 *  key -> val represents (string)code -> (integer)count
	 * @return statusCodeDistribution
	 */
	public HashMap getStatusCodeDistribution() {
		return statusCodeDistribution;
	}

	/** Return a HashMap representing the distribution of hosts for
	 *  successfully fetched curis, as represented by a hashmap where
	 *  key -> val represents (string)code -> (integer)count
	 * @return Hosts distribution as a HashMap
	 */
	public HashMap getHostsDistribution() {
		return hostsDistribution;
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


	/* (non-Javadoc)
	 * @see org.archive.crawler.event.CrawlURIDispositionListener#crawledURISuccessful(org.archive.crawler.datamodel.CrawlURI)
	 */
	public void crawledURISuccessful(CrawlURI curi) {
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
		
		// Save hosts
		incrementMapCount(hostsDistribution, curi.getServer().getHostname());
		
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.event.CrawlURIDispositionListener#crawledURINeedRetry(org.archive.crawler.datamodel.CrawlURI)
	 */
	public void crawledURINeedRetry(CrawlURI curi) {
		// Not keeping track of this at the moment
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.event.CrawlURIDispositionListener#crawledURIDisregard(org.archive.crawler.datamodel.CrawlURI)
	 */
	public void crawledURIDisregard(CrawlURI curi) {
		// Not keeping track of this at the moment
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.event.CrawlURIDispositionListener#crawledURIFailure(org.archive.crawler.datamodel.CrawlURI)
	 */
	public void crawledURIFailure(CrawlURI curi) {
		// Not keeping track of this at the moment
	}

}
