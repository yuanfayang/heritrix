/*
 * Created on Jul 16, 2003
 *
 */
package org.archive.crawler.admin;

import java.text.SimpleDateFormat;
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
import org.archive.util.PaddingStringBuffer;

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
public class StatisticsTracker implements Runnable, CoreAttributeConstants, CrawlListener{

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
	//protected TimedFixedSizeList recentlyCompletedFetches = new TimedFixedSizeList(60);

	protected Logger periodicLogger = null;
	protected int logInterval = 60;

	protected boolean shouldrun = true;
	
	// default start time to the time this object was instantiated
	protected long crawlerStartTime = System.currentTimeMillis();
	protected long crawlerEndTime = -1; // Until crawl ends, this value is -1.
	
	
	// timestamp of when this logger last wrote something to the log
	protected long lastLogPointTime = crawlerStartTime;
	protected long lastPagesFetchedCount = 0;
	protected long lastProcessedBytesCount = 0;
	
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
		crawlerEndTime = System.currentTimeMillis();
		shouldrun = false;
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
				Thread.sleep(logInterval * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
				controller.runtimeErrors.log(
					Level.INFO,
					"Periodic stat logger interrupted while sleeping.");
			}
			
			logActivity();
			lastLogPointTime = System.currentTimeMillis();
		}
	}

	private synchronized void logActivity() {
		String delimiter = "-----------------------------------";
		SimpleDateFormat timestamp = new SimpleDateFormat("yyyyMMddHHmmss");
		long discoveredPages = urisEncounteredCount();
		long pendingPages = urisInFrontierCount();
		long downloadedPages = successfulFetchAttempts();
		long uniquePages = uniquePagesCount();
		int docsPerSecond = processedDocsPerSec();
		int currentDocsPerSecond = currentProcessedDocsPerSec();
		int currentKBPerSec = currentProcessedKBPerSec();
		long totalKBPerSec = processedKBPerSec();
		long downloadFailures = failedFetchAttempts();
		int busyThreads = activeThreadCount(); 
		Date now = new Date();
		
		
		// 			mercator style log entry was
		//			[timestamp] [discovered-pages] [pending-pages] [downloaded-pages]
		//			[unique-pages] \
		//			[overall-docs-per-sec] [current-docs-per-sec] [overall-KB-sec]
		//			[current-KB-sec] \
		//			[download-failures] [stalled-threads] [memmory-usage]
		
		periodicLogger.log(
			Level.INFO,
			new PaddingStringBuffer()
             .append(timestamp.format(now))
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
		//periodicLogger.log(Level.INFO, delimiter);
	}
	
	/** Return the number of unique pages based on md5 calculations */
	public int uniquePagesCount(){
		//TODO implement sha1 checksum comparisions
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
		//if(totalFetchAttempts() == 0 || recentlyCompletedFetches.size() < 2){
		if(totalFetchAttempts() == 0){
			return 0;
		}
		
		// long sampleStartTime = ((ProcessedCrawlURIRecord)recentlyCompletedFetches.getFirst()).getStartTime();
		//long sampleEndTime = ((CrawlURI)recentCrawlURIs.getLast()).getAList().getLong(A_FETCH_COMPLETED_TIME);
		// long sampleEndTime = System.currentTimeMillis();
		
//		return (int)
//				(recentlyCompletedFetches.size() / 
//				 ((sampleEndTime - sampleStartTime)
//					/ 1000 ) 
//				+ .5 // round to nearest int
//		);

		long currentTime = System.currentTimeMillis();
		long currentPageCount = successfulFetchAttempts();
		long sampleTime = currentTime - lastLogPointTime;
		long samplePageCount = currentPageCount - lastPagesFetchedCount;
		
		int currentDocsPerSecond = (int) (samplePageCount / (sampleTime / 1000) + .5);
		
		lastPagesFetchedCount = currentPageCount;
		
		return currentDocsPerSecond;
	}
	
	/** Calculates the rate that data, in kb, has been processed
	 *  over the life of the crawl.
	 * @return kbPerSec
	 */ 
	public long processedKBPerSec(){
		if(totalFetchAttempts() == 0){
			return 0;
		}
		
		return (long)
				(((totalProcessedBytes / 1024)
					/ ((System.currentTimeMillis() - crawlerStartTime)
					/ 1000))
				+ .5 // round to nearest long
		);
	}
	
	/** Calculates an estimate of the rate, in kb, at which documents
	 *  are currently being processed by the crawler.  For more 
	 *  accurate estimates set a larger queue size, or get
	 *  and average multiple values.
	 * @return
	 */
	public int currentProcessedKBPerSec(){
		//if(totalFetchAttempts() == 0 || recentlyCompletedFetches.size() < 2){
		if(totalProcessedBytes == 0){
			return 0;
		}
		/*
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
		*/
		
		long currentTime = System.currentTimeMillis();
		long currentProcessedBytes = totalProcessedBytes;
		long sampleTime = currentTime - lastLogPointTime;
		long sampleProcessedBytes = currentProcessedBytes - lastProcessedBytesCount;
		int currentProcessedKB = (int) (((sampleProcessedBytes/1024) / (sampleTime / 1000)) + .5);
		
		lastProcessedBytesCount = currentProcessedBytes;
		
		return currentProcessedKB;
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
		return controller.getActiveToeCount();
	}

	/**
	 * Get the number of URIs in the frontier (found but not fetched)
	 * @return
	 */
	public long urisInFrontierCount() {

		return controller.getFrontier().pendingUriCount();
	}

	/**
	 * Get the number of successul page fetches.
	 * @return
	 */
	public long uriFetchSuccessCount() {
		return controller.getFrontier().successfullyFetchedCount();
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
		return controller.getFrontier().discoveredUriCount();
	}

	/**
	 * Get the total number of URIs where fetches have been attempted.
	 */
	public long totalFetchAttempts() {
		return controller.getFrontier().successfullyFetchedCount()
			+ controller.getFrontier().failedFetchCount();
	}

	/** Get the total number of failed fetch attempts (404s, connection failures -> give up, etc)
	 * @return int
	 */
	public long failedFetchAttempts() {
		return controller.getFrontier().failedFetchCount();
	}

	/** Returns the total number of successul resources (web pages, images, etc)
	 *  that have been fetched to date.
	 * @return int
	 */
	public long successfulFetchAttempts() {
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