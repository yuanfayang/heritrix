/*
 * Created on Jul 16, 2003
 *
 */
package org.archive.crawler.admin;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.ProcessedCrawlURIRecord;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.CrawlListener;
import org.archive.util.ArchiveUtils;
import org.archive.util.PaddingStringBuffer;

/**
 * StatisticsTracker has a reference to a CrawlController.  At specified 
 * intevals it will extract status information about the crawl that that
 * controller is managing.  Parts of this information will be written out
 * in a pre-determinate way to a log file. 
 * All of the information will be kept in memory from one update to another.
 * Outside classes can query the StatisticsTracker for this data and receive
 * the <i>last known</i> state.  Once the crawl controller has finished, the
 * StatisticsTracker thus contains information about the final state of the
 * crawl in a self contained manner.
 * 
 * 
 * Callers should be
 * aware that any "current" statistics (i.e. those involving calculation of 
 * rates) are good approximations, but work by looking at recently completed
 * CrawlURIs and thus may in some (rare and degenerative) cases return
 * data that is not useful, particularly with small/narrow crawls.  
 * 
 * @author Parker Thompson
 */
public class StatisticsTracker implements Runnable, CoreAttributeConstants, CrawlListener{

	// logging levels
	public static final int MERCATOR_LOGGING = 0;
	public static final int HUMAN_LOGGING = 1;
	public static final int VERBOSE_LOGGING = 2;
	
	protected int logLevel = MERCATOR_LOGGING;

	protected CrawlController controller;

	//protected TimedFixedSizeList recentlyCompletedFetches = new TimedFixedSizeList(60);

	protected Logger periodicLogger = null;
	protected int logInterval = 20; // In seconds.

	protected boolean shouldrun = true;
	
	// default start time to the time this object was instantiated
	protected long crawlerStartTime;
	protected long crawlerEndTime = -1; // Until crawl ends, this value is -1.
	
	
	// timestamp of when this logger last wrote something to the log
	protected long lastLogPointTime;
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

	/** Construct a StatisticsTracker object by giving it a reference
	 *  to a controller it can query for statistics.
	 * 
	 * @param controller
	 */
	public StatisticsTracker(CrawlController c) {
		controller = c;
		controller.addListener(this);
		periodicLogger = c.progressStats;
	}
	
	public StatisticsTracker(CrawlController c, int interval){
		controller = c;
		controller.addListener(this);
		periodicLogger = c.progressStats;
		logInterval = interval;
	}
	
	public void setCrawlStartTime(long mili){
		crawlerStartTime = mili;
	}
	public long getCrawlStartTime(){
		return crawlerStartTime;
	}
	
	/**
	 * 
	 * @return If crawl has ended it will return the time 
	 *         it ended (given by System.currentTimeMillis() 
	 * 		   at that time).
	 *         If crawl is still going on it will return the
	 *         same as System.currentTimeMillis()
	 */
	public long getCrawlEndTime()
	{
		if(crawlerEndTime==-1)
		{
			return System.currentTimeMillis();
		}
		
		return crawlerEndTime;
	}

	public void setLogWriteInterval(int interval) {
		if(interval < 0){
			logInterval = 0;
			return;
		}
		
		logInterval = interval;
	}
	public int getLogWriteInterval() {
		return logInterval;
	}

	/**
	 * Terminates the logging done by the object. 
	 * Calling this method will cause the run() method to exit. 
	 */
	public void stop()
	{
		shouldrun = false;
		crawlerEndTime = System.currentTimeMillis(); //Note the time when the crawl stops.
		logActivity(); //Log end state		
	}

	/**
	 * Will be called once the crawl job we are monitoring has ended.
	 * 
	 *  (non-Javadoc)
	 * @see org.archive.crawler.framework.CrawlListener#crawlEnding(java.lang.String)
	 */
	public void crawlEnding(String sExitMessage) {
		stop();
	}


	/** This object can be run as a thread to enable periodic loggin */
	public void run() {
		// don't start logging if we have no logger
		if (periodicLogger == null) {
			return;
		}
		
		crawlerStartTime = System.currentTimeMillis(); //Note the time the crawl starts.
		lastLogPointTime = crawlerStartTime;
		shouldrun = true; //If we are starting, this should always be true.
		
		// log the legend	
		periodicLogger.log(Level.INFO,
				"   [timestamp] [discovered]    [queued] [downloaded]"
					+ " [doc/s(avg)]  [KB/s(avg)]"
					+ " [dl-failures] [busy-thread] [mem-use-KB]"
			);

		// keep logging until someone calls stop()
		while (shouldrun) 
		{
			// pause before writing the first entry (so we have real numbers)
			// and then pause between entries 
			try {
				Thread.sleep(controller.getOrder().getIntAt(CrawlController.XP_STATS_INTERVAL, CrawlController.DEFAULT_STATISTICS_REPORT_INTERVAL) * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
				controller.runtimeErrors.log(
					Level.INFO,
					"Periodic stat logger interrupted while sleeping.");
			}
			
			if(shouldrun) //In case stop() was invoked while the thread was sleeping.
			{
				logActivity();
			}
		}
	}

	private synchronized void logActivity() {
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
			docsPerSecond = (int)(downloadedPages / ((System.currentTimeMillis() - crawlerStartTime) / 1000) + .5); // rounded to nearest int
			totalKBPerSec = (long)(((totalProcessedBytes / 1024) / ((System.currentTimeMillis() - crawlerStartTime)	/ 1000)) + .5 ); // round to nearest long
		}
		
		busyThreads = activeThreadCount();
		 
		if(shouldrun || (System.currentTimeMillis() - lastLogPointTime) >= 1000)
		{
			// If shouldrun is false there is a chance that the time interval since
			// last time is too small for a good sample.  We only want to update
			// "current" data when the interval is long enough or shouldrun is true.
			currentDocsPerSecond = 0;
			currentKBPerSec = 0;
			
			// if we haven't done anyting or there isn't a reasonable sample size give up.
			if(totalFetchAttempts() != 0)
			{
				// Note time.
				long currentTime = System.currentTimeMillis();
				long sampleTime = currentTime - lastLogPointTime;

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
		
		if (logLevel >= HUMAN_LOGGING) {
		
			// some human readable stuff, in case we want to look at the log file
			periodicLogger.log(Level.INFO, now.toString());
			periodicLogger.log(
				Level.INFO,
				"\tURIs Completed:\t"
					+ percentOfDiscoveredUrisCompleted()
					+ "% (fetched/discovered)");
			periodicLogger.log(
				Level.INFO,
				"\tDocument Processing Rate:\t"
					+ currentKBPerSec
					+ " kb/sec.");
			periodicLogger.log(
				Level.INFO,
				"\tDocument Processing Rate:\t"
					+ docsPerSecond
					+ " documents/sec.");
			periodicLogger.log(
				Level.INFO,
				"\tTotal Processed Bytes:\t"
					+ (totalProcessedBytes / 1000000)
					+ " mb");
			periodicLogger.log(
				Level.INFO,
				"\tDiscovered URIs:\t" + urisEncounteredCount());
			periodicLogger.log(
				Level.INFO,
				"\tFrontier (unfetched):\t" + urisInFrontierCount());
			periodicLogger.log(
				Level.INFO,
				"\tFetch Attempts:\t" + totalFetchAttempts());
			periodicLogger.log(
				Level.INFO,
				"\tSuccesses:\t" + successfulFetchAttempts());
			periodicLogger.log(Level.INFO, "\tThreads:");
			periodicLogger.log(Level.INFO, "\t\tTotal:\t" + threadCount());
			periodicLogger.log(
				Level.INFO,
				"\t\tActive:\t" + activeThreadCount());
		}
		
		if (logLevel >= VERBOSE_LOGGING) {
		
			// print file type distribution (mime types)
			HashMap dist = getFileDistribution();
			Object[] keys = dist.keySet().toArray();
			Arrays.sort(keys);
		
			if (dist.size() > 0) {
				//	Iterator keyIterator = t.iterator(); //dist.keySet().iterator();
				periodicLogger.log(
					Level.INFO,
					"\tFetched Resources MIME Distribution:");
		
				for (int i = 0; i < keys.length; i++) {
					String key = (String) keys[i];
					String val = ((Integer) dist.get(key)).toString();
					periodicLogger.log(
						Level.INFO,
						"\t\t" + val + "\t" + key);
				}
		
			} else {
				periodicLogger.log(
					Level.INFO,
					"\tNo mime statistics currently available.");
			}
		
			// print status code distributions (e.g. 404, 200, etc)
			HashMap codeDist = getStatusCodeDistribution();
			Object[] cdKeys = codeDist.keySet().toArray();
			Arrays.sort(cdKeys);
		
			if (codeDist.size() > 0) {
				Iterator keyIterator = codeDist.keySet().iterator();
		
				periodicLogger.log(
					Level.INFO,
					"\tStatus Code Distribution:");
		
				for (int i = 0; i < cdKeys.length; i++) {
					String key = (String) cdKeys[i];
					String val = ((Integer) codeDist.get(key)).toString();
		
					periodicLogger.log(
						Level.INFO,
						"\t\t" + val + "\t" + key);
				}
		
			} else {
				periodicLogger.log(
					Level.INFO,
					"\tNo code sistribution statistics.");
			}
		}
		lastLogPointTime = System.currentTimeMillis();
	}
	
	/* Return the number of unique pages based on md5 calculations */
	/*public int uniquePagesCount(){
		//TODO implement sha1 checksum comparisions
		return 0;
	}*/
	
	/** Returns the number of documents that have been processed
	 *  per second over the life of the crawl (as of last snapshot)
	 * @return docsPerSec
	 */
	public int processedDocsPerSec(){
		return docsPerSecond;
	}
	
	/** Get the current logging level */
	public int getLogLevel(){
		return logLevel;
	}
	
	/** Set the log level.  See statically defined logging levels in this class
	 *  for potential values 
	 */
	public void setLogLevel(int ll){
		logLevel = ll;
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
	
	/** Keep track of  "completed" URIs so we can caluculate 
	 *  statistics based on when they were completed.
	 * @param CrawlURI
	 */
	public synchronized void completedProcessing(CrawlURI curi){
		
		// make sure it has the attributes we need for processing
		if(! curi.getAList().containsKey(A_FETCH_BEGAN_TIME)){
			return;
		}
		
		// get the size from the curi and make sure the size field is set
		long curiSize = curi.getContentSize();
		
		// the selector is going to strip the original of its' alist, 
		// so let's keep a copy instead with just what we need
		ProcessedCrawlURIRecord record = new ProcessedCrawlURIRecord(curi);
		
		// store in the queue
		//recentlyCompletedFetches.add(record);

		totalProcessedBytes += curiSize;
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
	public void incrementTypeCount(String mime) {

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
	public void incrementStatusCodeCount(int code) {
		incrementStatusCodeCount((new Integer(code)).toString());
	}

	/** Keeps a count of processed uri's status codes so that we can
	 *  generate histograms.
	 * @param code
	 */
	public void incrementStatusCodeCount(String code) {

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
	 * @see org.archive.crawler.framework.CrawlListener#crawlPausing(java.lang.String)
	 */
	public void crawlPausing(String statusMessage) {
		// TODO Auto-generated method stub
		
	}


	/** Returns the approximate rate at which we are writing uncompressed data
	 *  to disk.
	 */
	 /*
	public int approximateCompletionRate() {

		if (recentlyCompletedFetches.size() < 2) {
			return 0;
		}

		ProcessedCrawlURIRecord oldest = (ProcessedCrawlURIRecord)recentlyCompletedFetches.getFirst();
		ProcessedCrawlURIRecord newest = (ProcessedCrawlURIRecord)recentlyCompletedFetches.getLast();

		long period = newest.getEndTime() - oldest.getStartTime();
	
		int totalRecentBytes = 0;

		Iterator recentURIs = recentlyCompletedFetches.iterator();

		while (recentURIs.hasNext()) {
			ProcessedCrawlURIRecord current = (ProcessedCrawlURIRecord) recentURIs.next();
			totalRecentBytes += current.getSize();
		}

		// return bytes/sec
		return (int) (1000 * totalRecentBytes / period);
	}
*/


//	public long getCrawlURIStartTime(CrawlURI curi){
//		return curi.getAList().getLong(A_FETCH_BEGAN_TIME);
//	}
//	public long getCrawlURIEndTime(CrawlURI curi){
//		return curi.getAList().getLong(A_FETCH_COMPLETED_TIME);
//	}
}