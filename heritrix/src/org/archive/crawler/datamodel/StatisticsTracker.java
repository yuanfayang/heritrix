/*
 * Created on Jul 16, 2003
 *
 */
package org.archive.crawler.datamodel;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.crawler.framework.CrawlController;
import org.archive.util.TimedQueue;

/**
 * Tracks statistics that relate to the crawl in progress.  Callers should be
 * aware that any "current" statistics (i.e. those involving calculation of 
 * rates) are good approximations, but work by looking at recently completed
 * CrawlURIs and thus may in some (rare and degenerative) cases return
 * data that is not useful, particularly with small/narrow crawls.  
 * 
 * @author Parker Thompson
 *
 */
public class StatisticsTracker implements Runnable, CoreAttributeConstants{

	// logging levels
	public static final int MERCATOR_LOGGING = 0;
	public static final int HUMAN_LOGGING = 1;
	public static final int VERBOSE_LOGGING = 2;
	
	protected int logLevel = MERCATOR_LOGGING;

	protected CrawlController controller;

	// keep track of the file types we see (mime type -> count)
	protected HashMap fileTypeDistribution = new HashMap();

	// keep track of fetch status codes
	protected HashMap statusCodeDistribution = new HashMap();

	protected int totalProcessedBytes = 0;
	protected TimedQueue recentlyCompletedFetches = new TimedQueue(60);

	protected Logger periodicLogger = null;
	protected int logInterval = 60;

	// default start time to the time this object was instantiated
	protected long crawlerStartTime = System.currentTimeMillis();

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
		periodicLogger = c.progressStats;
	}
	
	public StatisticsTracker(CrawlController c, int interval){
		controller = c;
		periodicLogger = c.progressStats;
		logInterval = interval;
	}
	
	public void setCrawlStartTime(long mili){
		crawlerStartTime = mili;
	}
	public long getCrawlStartTime(){
		return crawlerStartTime;
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

	/** This object can be run as a thread to enable periodic loggin */
	public void run() {
		// don't start logging if we have no logger
		if (periodicLogger == null) {
			return;
		}
	
		// log the legend	
		periodicLogger.log(Level.INFO,
				"[timestamp]\t\t[discovered-pages]\t[pending-pages]\t[downloaded-pages]"
					+ "\t[unique-pages]\t[overall-docs-per-sec]\t[current-docs-per-sec]\t[overall-KB-sec]"
					+ "\t[current-KB-sec]\t[download-failures]\t[stalled-threads]\t[memory-usage]"
			);

		// keep logging as long as this thang is running
		while (true) {
			
			String delimiter = "-----------------------------------";
			SimpleDateFormat timestamp = new SimpleDateFormat("yyyyMMddHHmmss");
			int discoveredPages = urisEncounteredCount();
			int pendingPages = urisInFrontierCount();
			int downloadedPages = successfulFetchAttempts();
			int uniquePages = uniquePagesCount();
			int docsPerSecond = processedDocsPerSec();
			int currentDocsPerSecond = currentProcessedDocsPerSec();
			int currentKBPerSec = currentProcessedKBPerSec();
			int totalKBPerSec = processedKBPerSec();
			int downloadFailures = totalFetchAttempts() - successfulFetchAttempts();
			int pausedThreads = threadCount() - activeThreadCount(); 
			Date now = new Date();

			// pause before writing the first entry (so we have real numbers)
			// and then pause between entries 
			try {
				Thread.sleep(logInterval * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
				controller.crawlErrors.log(
					Level.INFO,
					"Periodic stat logger interrupted while sleeping.");
			}

			// 			mercator style log entry
			//			[timestamp] [discovered-pages] [pending-pages] [downloaded-pages]
			//			[unique-pages] \
			//			[overall-docs-per-sec] [current-docs-per-sec] [overall-KB-sec]
			//			[current-KB-sec] \
			//			[download-failures] [stalled-threads] [memory-usage]
			periodicLogger.log(
				Level.INFO,
				timestamp.format(now)
				+ "\t\t" + discoveredPages
				+ "\t\t\t" + pendingPages
				+ "\t\t" + downloadedPages
				+ "\t\t\t" + uniquePages
				+ "\t\t" + docsPerSecond
				+ "\t\t\t" + currentDocsPerSecond
				+ "\t\t\t" + totalKBPerSec
				+ "\t\t\t" + currentKBPerSec
				+ "\t\t\t" + downloadFailures
				+ "\t\t\t" + pausedThreads
				+ "\t\t\t" + Runtime.getRuntime().totalMemory()
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
			//periodicLogger.log(Level.INFO, delimiter);
		}
	}
	
	/** Return the number of unique pages based on md5 calculations */
	public int uniquePagesCount(){
		//TODO implement md5 checksum comparisions
		return 0;
	}
	
	/** Returns the number of documents that have been processed
	 *  per second over the life of the crawl
	 * @return docsPerSec
	 */
	public int processedDocsPerSec(){
		if(totalFetchAttempts() == 0){
			return 0;
		}
		return (int)
				(successfulFetchAttempts()
					/ ((System.currentTimeMillis() - crawlerStartTime) / 1000)
				+ .5 // round to nearest int
		);
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
	 *  based on a queue of recently seen CrawlURIs.  
	 * @return currentDocsPerSec
	 */
	public int currentProcessedDocsPerSec(){
		// if we haven't done anyting or there isn't a reasonable sample size give up
		if(totalFetchAttempts() == 0 || recentlyCompletedFetches.size() < 2){
			return 0;
		}
		
		long sampleStartTime = ((ProcessedCrawlURIRecord)recentlyCompletedFetches.getFirst()).getStartTime();
		//long sampleEndTime = ((CrawlURI)recentCrawlURIs.getLast()).getAList().getLong(A_FETCH_COMPLETED_TIME);
		long sampleEndTime = System.currentTimeMillis();
		
		return (int)
				(recentlyCompletedFetches.size() / 
				 ((sampleEndTime - sampleStartTime)
					/ 1000 ) 
				+ .5 // round to nearest int
		);
	}
	
	/** Calculates the rate that data, in kb, has been processed
	 *  over the life of the crawl.
	 * @return kbPerSec
	 */ 
	public int processedKBPerSec(){
		if(totalFetchAttempts() == 0){
			return 0;
		}
		
		return (int)
				(((totalProcessedBytes / 1000)
					/ ((System.currentTimeMillis() - crawlerStartTime)
					/ 1000))
				+ .5 // round to nearest int
		);
	}
	
	/** Calculates an estimate of the rate, in kb, at which documents
	 *  are currently being processed by the crawler.  For more 
	 *  accurate estimates set a larger queue size, or get
	 *  and average multiple values.
	 * @return
	 */
	public int currentProcessedKBPerSec(){
		if(totalFetchAttempts() == 0 || recentlyCompletedFetches.size() < 2){
			return 0;
		}
		
		int totalRecentSize = 0;
		
		Iterator recentItr = recentlyCompletedFetches.iterator();
		while(recentItr.hasNext()){
			totalRecentSize += ((ProcessedCrawlURIRecord)recentItr.next()).getSize();
		}
		
		long sampleStartTime = ((ProcessedCrawlURIRecord)recentlyCompletedFetches.getFirst()).getStartTime();
		long sampleEndTime = System.currentTimeMillis();
		long samplePeriod = (sampleEndTime - sampleStartTime)/1000;
		int totalRecentKB = totalRecentSize / 1000;
		
		return (int)
				((totalRecentKB / samplePeriod)
				+ .5 // round to nearest int
		);
	}
	
	/** Keep track of  "completed" URIs so we can caluculate 
	 *  statistics based on when they were completed.
	 * @param CrawlURI
	 */
	public void completedProcessing(CrawlURI curi){
		
		// make sure it has the attributes we need for processing
		if(! curi.getAList().containsKey(A_FETCH_BEGAN_TIME)){
			return;
		}
		
		// get the size from the curi and make sure the size field is set
		int curiSize = getCrawlURISize(curi);
		curi.setContentSize(curiSize);
		
		// the selector is going to strip the original of its' alist, 
		// so let's keep a copy instead with just what we need
		ProcessedCrawlURIRecord record = new ProcessedCrawlURIRecord(curi);
		
		// store in the queue
		recentlyCompletedFetches.add(record);

		totalProcessedBytes += curiSize;
	}
	
	/** Determine the size of a URIs content by either 
	 *  asking it to self-report, or calculating the size
	 *  (if it reports a wonky value like -1)
	 * 
	 * @param CrawlURI
	 * @return the size of the CrawlURI's content
	 */
	protected int getCrawlURISize(CrawlURI curi){
		int size = 0;
		
		// try to let the uri self-report
		size = curi.getContentSize();
		if(size >= 0){
			return size;
		}
		
		// TODO do this the hard way (looking at AList)
		return 0;
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
		return controller.getActiveToeCount();
	}

	/**
	 * Get the number of URIs in the frontier (found but not fetched)
	 * @return
	 */
	public int urisInFrontierCount() {

		return urisEncounteredCount() - totalFetchAttempts();
	}

	/**
	 * Get the number of successul page fetches.
	 * @return
	 */
	public int uriFetchSuccessCount() {
		return controller.getFrontier().successfullyFetchedCount();
	}

	/** This returns the number of completed URIs as a percentage of the total
	 *   number of URIs encountered (should be inverse to the discovery curve)
	 * @return
	 */
	public int percentOfDiscoveredUrisCompleted() {
		int completed = totalFetchAttempts();
		int total = urisEncounteredCount();

		if (total == 0) {
			return 0;
		}

		return (int) (100 * completed / total);
	}

	/** Returns a count of all uris encountered.  This includes both the frontier 
	 * (unfetched pages) and fetched pages/failed fetch attempts.
	 */
	public int urisEncounteredCount() {
		return controller.getFrontier().discoveredUriCount();
	}

	/**
	 * Get the total number of URIs where fetches have been attempted.
	 */
	public int totalFetchAttempts() {
		return controller.getFrontier().successfullyFetchedCount()
			+ controller.getFrontier().failedFetchCount();
	}

	/** Get the total number of failed fetch attempts (404s, connection failures -> give up, etc)
	 * @return int
	 */
	public int failedFetchAttempts() {
		return controller.getFrontier().failedFetchCount();
	}

	/** Returns the total number of successul resources (web pages, images, etc)
	 *  that have been fetched to date.
	 * @return int
	 */
	public int successfulFetchAttempts() {
		return controller.getFrontier().successfullyFetchedCount();
	}

	/** Returns the total number of uncompressed bytes written to disk.  This may 
	 *  be different from the actual number if you are using compression.
	 * @return byteCount
	 */
	public int getTotalBytesWritten() {
		return totalProcessedBytes;
	}

	/** Returns the approximate rate at which we are writing uncompressed data
	 *  to disk as calculated using a finite set (see declaration of recentDiskWrites)
	 *  of recent disk writes.  THIS FUNCTION IS DEPRECIATED use 
	 *  currentProcessedKBPerSec()
	 */
	public int approximateDiskWriteRate() {
		return currentProcessedKBPerSec();
	}

	/** Returns the approximate rate at which we are writing uncompressed data
	 *  to disk.
	 */
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

//	public long getCrawlURIStartTime(CrawlURI curi){
//		return curi.getAList().getLong(A_FETCH_BEGAN_TIME);
//	}
//	public long getCrawlURIEndTime(CrawlURI curi){
//		return curi.getAList().getLong(A_FETCH_COMPLETED_TIME);
//	}
}
