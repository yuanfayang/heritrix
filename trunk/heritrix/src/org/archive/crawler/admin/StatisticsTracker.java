/*
 * Created on Jul 16, 2003
 *
 */
package org.archive.crawler.admin;

import java.util.Date;
import java.util.HashMap;
import java.util.logging.Level;

import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.AbstractTracker;
import org.archive.util.ArchiveUtils;
import org.archive.util.PaddingStringBuffer;

/**
 * @author Parker Thompson
 * @author Kristinn Sigurdsson
 * 
 * This is an implementation of the AbstractTracker.  It logs the 
 * following information at intervals specified in the crawl order:
 * 
 * 
 * @see org.archive.crawler.framework.AbstractTracker
 */
public class StatisticsTracker extends AbstractTracker implements CoreAttributeConstants{

	protected long lastPagesFetchedCount = 0;
	protected long lastProcessedBytesCount = 0;
	
	/*
	 * Snapshot data. 
	 */
	protected long discoveredPages = 0;
	protected long pendingPages = 0;
	protected long downloadedPages = 0;
	protected long uniquePages = 0;
	protected int docsPerSecond = 0;
	protected int currentDocsPerSecond = 0;
	protected int currentKBPerSec = 0;
	protected long totalKBPerSec = 0;
	protected long downloadFailures = 0;
	protected int busyThreads = 0; 

	/*
	 * Cumulative data 
	 */
	protected long totalProcessedBytes = 0;
	// keep track of the file types we see (mime type -> count)
	protected HashMap fileTypeDistribution = new HashMap();
	// keep track of fetch status codes
	protected HashMap statusCodeDistribution = new HashMap();

	
	public StatisticsTracker() {
		super();
	}

	protected synchronized void logActivity() {
		// This method loads "snapshot" data.
		discoveredPages = urisEncounteredCount();
		pendingPages = urisInFrontierCount();
		downloadedPages = successfulFetchAttempts();
		downloadFailures = failedFetchAttempts();		
		
		if(totalFetchAttempts() == 0){
			docsPerSecond = 0;
			totalKBPerSec = 0;
		}
		else
		{
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
			 //.raAppend(64,uniquePages)
			 .raAppend(64,currentDocsPerSecond+"("+docsPerSecond+")")
			 .raAppend(77,currentKBPerSec+"("+totalKBPerSec+")")
			 .raAppend(91,downloadFailures)
			 .raAppend(105,busyThreads)
			 .raAppend(118,Runtime.getRuntime().totalMemory()/1024)
			 .toString()
		);
		

		lastLogPointTime = System.currentTimeMillis();
	}
	

	/** Returns the number of documents that have been processed
	 *  per second over the life of the crawl (as of last snapshot)
	 * @return docsPerSec
	 */
	public int processedDocsPerSec(){
		return docsPerSecond;
	}
	
	/** Returns an estimate of recent document download rates
	 *  based on a queue of recently seen CrawlURIs (as of last snapshot.)
	 * @return currentDocsPerSec
	 */
	public int currentProcessedDocsPerSec(){
		return currentDocsPerSecond;
	}
	
	/** Calculates the rate that data, in kb, has been processed
	 *  over the life of the crawl (as of last snapshot.)
	 * @return kbPerSec
	 */ 
	public long processedKBPerSec(){
		return totalKBPerSec;
	}
	
	/** Calculates an estimate of the rate, in kb, at which documents
	 *  are currently being processed by the crawler.  For more 
	 *  accurate estimates set a larger queue size, or get
	 *  and average multiple values (as of last snapshot).
	 * @return
	 */
	public int currentProcessedKBPerSec(){
		return currentKBPerSec;
	}
	
	/** Returns a HashMap that contains information about distributions of 
	 *  encountered mime types.  Key/value pairs represent 
	 *  mime type -> count.
	 * @return fileTypeDistribution
	 */
	public HashMap getFileDistribution() {
		return fileTypeDistribution;
	}

	/** Let modules store statistics about mime types they've
	 *  encountered.  Note: these statistics are only as accurate
	 *  as the logic concerned with storing them to this object.
	 * @param mime
	 */
	protected void incrementTypeCount(String mime) {

		if (mime == null) {
			mime = "unknown";
		}

		// strip things like charset (e.g. text/html; charset=iso-blah-blah)	
		int semicolonLoc = mime.indexOf(';');
		if (semicolonLoc >= 0) {
			mime = mime.substring(0, semicolonLoc);
		}

		if (fileTypeDistribution.containsKey(mime)) {

			Integer matchValue = (Integer) fileTypeDistribution.get(mime);
			matchValue = new Integer(matchValue.intValue() + 1);
			fileTypeDistribution.put(mime, matchValue);

		} else {
			// if we didn't find this mime type add it
			fileTypeDistribution.put(mime, new Integer(1));
		}
	}

	/** Keeps a count of processed uri's status codes so that we can
	 *  generate histograms.
	 * @param code
	 */
	protected void incrementStatusCodeCount(String code) {

		if (code == null) {
			code = "unknown";
		}

		if (statusCodeDistribution.containsKey(code)) {

			Integer matchValue = (Integer) statusCodeDistribution.get(code);
			matchValue = new Integer(matchValue.intValue() + 1);
			statusCodeDistribution.put(code, matchValue);

		} else {
			// if we didn't find this mime type add it
			statusCodeDistribution.put(code, new Integer(1));
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

	/**
	 * Get the number of threads in process (sleeping and active)
	 * @return
	 */
	public int threadCount() {
		return controller.getToeCount();
	}

	/**
	 * Get the number of active (non-paused) threads.
	 * @return
	 */
	public int activeThreadCount() {
		return shouldrun ? controller.getActiveToeCount() : busyThreads;
	}

	/**
	 * Get the number of URIs in the frontier (found but not fetched)
	 * @return
	 */
	public long urisInFrontierCount() {

		// While shouldrun is true we can use info direct from the crawler.  
		// After that our last snapshot will have to do.
		return shouldrun ? controller.getFrontier().pendingUriCount() : pendingPages;
	}

	/**
	 * Get the number of successul page fetches.
	 * @return
	 */
	public long uriFetchSuccessCount() {
		return successfulFetchAttempts();
	}

	/** This returns the number of completed URIs as a percentage of the total
	 *   number of URIs encountered (should be inverse to the discovery curve)
	 * @return
	 */
	public int percentOfDiscoveredUrisCompleted() {
		long completed = totalFetchAttempts();
		long total = urisEncounteredCount();

		if (total == 0) {
			return 0;
		}

		return (int) (100 * completed / total);
	}

	/** Returns a count of all uris encountered.  This includes both the frontier 
	 * (unfetched pages) and fetched pages/failed fetch attempts.
	 */
	public long urisEncounteredCount() {
		// While shouldrun is true we can use info direct from the crawler.  
		// After that our last snapshot will have to do.
		return shouldrun ? controller.getFrontier().discoveredUriCount() : discoveredPages;
	}

	/**
	 * Get the total number of URIs where fetches have been attempted.
	 */
	public long totalFetchAttempts() {
		return successfulFetchAttempts() + failedFetchAttempts();
	}

	/** Get the total number of failed fetch attempts (404s, connection failures -> give up, etc)
	 * @return int
	 */
	public long failedFetchAttempts() {
		// While shouldrun is true we can use info direct from the crawler.  
		// After that our last snapshot will have to do.
		return shouldrun ? controller.getFrontier().failedFetchCount() : downloadFailures;
	}

	/** Returns the total number of successul resources (web pages, images, etc)
	 *  that have been fetched to date.
	 * @return int
	 */
	public long successfulFetchAttempts() {
		// While shouldrun is true we can use info direct from the crawler.  
		// After that our last snapshot will have to do.
		return shouldrun ? controller.getFrontier().successfullyFetchedCount() : downloadedPages;
	}

	/** Returns the total number of uncompressed bytes written to disk.  This may 
	 *  be different from the actual number if you are using compression.
	 * @return byteCount
	 */
	public long getTotalBytesWritten() {
		return totalProcessedBytes;
	}


	/* (non-Javadoc)
	 * @see org.archive.crawler.event.CrawlURIDispositionListener#crawledURISuccessful(org.archive.crawler.datamodel.CrawlURI)
	 */
	public void crawledURISuccessful(CrawlURI curi) {
		incrementStatusCodeCount(Integer.toString(curi.getFetchStatus()));
		incrementTypeCount(curi.getContentType());
		totalProcessedBytes += curi.getContentSize();
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
