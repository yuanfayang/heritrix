/*
 * Created on Jul 16, 2003
 *
 */
package org.archive.crawler.basic;

import org.apache.xalan.lib.Redirect;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.*;
import java.util.List;
import java.util.*;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.util.Queue;
import org.archive.util.DiskWrite;

/**
 * Tracks statistics that relate to the crawl in progress.
 * 
 * @author Parker Thompson
 *
 */
public class StatisticsTracker {

	protected CrawlController controller;

	// keep track of the file types we see (mime type -> count)
	protected HashMap fileTypeDistribution = new HashMap();
	
	// keep track of fetch status codes
	protected HashMap statusCodeDistribution = new HashMap();

	protected Queue recentDiskWrites = new Queue(10);
	protected int totalBytesToDisk = 0;


	public StatisticsTracker() {
		super();
	}
	
	/** Construct a StatisticsTracker object by giving it a reference
	 *  to a controller it can query for statistics.
	 * 
	 * @param controller
	 */
	public StatisticsTracker(CrawlController c){
		controller = c;
	}
	
	/** Returns a HashMap that contains information about distributions of 
	 *  encountered mime types.  Key/value pairs represent 
	 *  mime type -> count.
	 * @return fileTypeDistribution
	 */
	public HashMap getFileDistribution(){
		return fileTypeDistribution;
	}
	
	
	/** Let modules store statistics about mime types they've
	 *  encountered.  Note: these statistics are only as accurate
	 *  as the logic concerned with storing them to this object.
	 * @param mime
	 */
	public void incrementTypeCount(String mime){

		if(mime == null){
			mime = "unknown";
		}
		
		// strip things like charset (e.g. text/html; charset=iso-blah-blah)	
		int semicolonLoc = mime.indexOf(';'); 
		if(semicolonLoc >= 0){
			mime = mime.substring(0, semicolonLoc);
		}

		if(fileTypeDistribution.containsKey(mime)){

			Integer matchValue = (Integer)fileTypeDistribution.get(mime);
			matchValue = new Integer(matchValue.intValue() + 1);
			fileTypeDistribution.put(mime, matchValue);

		}else{
			// if we didn't find this mime type add it
			fileTypeDistribution.put(mime, new Integer(1));
		}
	}


	/** Keeps a count of processed uri's status codes so that we can
	 *  generate histograms.
	 * @param code
	 */
	public void incrementStatusCodeCount(int code){
		incrementStatusCodeCount( (new Integer(code)).toString());
	}
	
	/** Keeps a count of processed uri's status codes so that we can
	 *  generate histograms.
	 * @param code
	 */
	public void incrementStatusCodeCount(String code){

		if(code == null){
			code = "unknown";
		}
		
		if(statusCodeDistribution.containsKey(code)){

			Integer matchValue = (Integer)statusCodeDistribution.get(code);
			matchValue = new Integer(matchValue.intValue() + 1);
			statusCodeDistribution.put(code, matchValue);

		}else{
			// if we didn't find this mime type add it
			statusCodeDistribution.put(code, new Integer(1));
		}
	}
	
	
	/** Return a HashMap representing the distribution of status codes for
	 *  successfully fetched curis, as represented by a hashmap where
	 *  key -> val represents (string)code -> (integer)count
	 * @return statusCodeDistribution
	 */
	public HashMap getStatusCodeDistribution(){
		return statusCodeDistribution;
	}
	
	
	
	/**
	 * Get the number of threads in process (sleeping and active)
	 * @return
	 */
	public int threadCount(){
		return controller.getToeCount();
	}
	
	/**
	 * Get the number of active (non-paused) threads.
	 * @return
	 */
	public int activeThreadCount(){
		return controller.getActiveToeCount();
	}
	
	/**
	 * Get the number of URIs in the frontier (found but not fetched)
	 * @return
	 */
	public int urisInFrontierCount(){
			
			return urisEncounteredCount() - totalFetchAttempts();
	}
	
	/**
	 * Get the number of successul page fetches.
	 * @return
	 */
	public int uriFetchSuccessCount(){
		return controller.getSelector().successfullyFetchedCount();
	}
	
	/** This returns the number of completed URIs as a percentage of the total
	 *   number of URIs encountered (should be inverse to the discovery curve)
	 * @return
	 */
	public int percentOfDiscoveredUrisCompleted(){
		int completed = totalFetchAttempts();
		int total = urisEncounteredCount();
		
		return (int)(100*completed/total);
	}
	
	/** Returns a count of all uris encountered.  This includes both the frontier 
	 * (unfetched pages) and fetched pages/failed fetch attempts.
	 */
		public int urisEncounteredCount(){
		return controller.getStore().discoveredUriCount();
	}
	
	/**
	 * Get the total number of URIs where fetches have been attempted.
	 */
	public int totalFetchAttempts(){
		return controller.getSelector().successfullyFetchedCount() + controller.getSelector().failedFetchCount();
	}
	
	/** Get the total number of failed fetch attempts (404s, connection failures -> give up, etc)
	 * @return int
	 */
	public int failedFetchAttempts(){
		return controller.getSelector().failedFetchCount();
	}
	
	/** Returns the total number of successul resources (web pages, images, etc)
	 *  that have been fetched to date.
	 * @return int
	 */
	public int successfulFetchAttempts(){
		return controller.getSelector().successfullyFetchedCount();
	}
	
	/** Keep a record of how many bytes we think we're writing to disk. 
	 *  and stores descretely a certain number of the latest writes for 
	 *  relatively "real time" statistic calculation.
	 * @param bytes
	 */
	public void sentToDisk(DiskWrite latest){
		recentDiskWrites.add(latest);
		totalBytesToDisk += latest.getByteCount();
	}
	
	/** Returns the total number of uncompressed bytes written to disk.  This may 
	 *  be different from the actual number if you are using compression.
	 * @return byteCount
	 */
	public int getTotalBytesWritten(){
		return totalBytesToDisk;
	}
	
	/** Returns the approximate rate at which we are writing uncompressed data
	 *  to disk as calculated using a finite set (see declaration of recentDiskWrites)
	 *  of recent disk writes.
	 */
	public int approximateDiskWriteRate(){
		
		if(recentDiskWrites.size() < 2){
			return 0;
		}
		
		long startTime = ((DiskWrite)recentDiskWrites.getFirst()).getTime();
		long endTime = ((DiskWrite)recentDiskWrites.getLast()).getTime();
		long period = endTime - startTime;
		
		int totalRecentBytes = 0;
		
		Iterator recentWriteItr = recentDiskWrites.iterator();
		
		while(recentWriteItr.hasNext() ){
			DiskWrite current = (DiskWrite)recentWriteItr.next();
			
			// don't add the last value, since timestamps are *before* writes
			// adding the bytes from the last write would inflate our rate
			if(!recentWriteItr.hasNext()){
				break;
			}
			totalRecentBytes += current.getByteCount();
		}
		
		// return bytes/sec
		return (int)(1000*totalRecentBytes/period);
	}
	
	

	
	
//	/** Note the last X urls we've seen so we can use them
//	 *  to make estimates about what's happening right now (e.g. download rates)
//	 * @param crawluri
//	 */
//	public void noteLatestFetchedURI(CrawlURI c){
//		
//		if(latestFetchedCuris.size() <= MAX_LATEST_TO_TRACK){
//			latestFetchedCuris.add(c);
//		
//		}else{
//			latestFetchedCuris.add(latestIFetchedItem, c);
//			latestIFetchedItem++;
//		}
//		
//		if(latestIFetchedItem >= MAX_LATEST_TO_TRACK ){
//			latestIFetchedItem = 0;
//		}
//	}
//	
//	/** Look at our buffer of latest uris and attempt to estimate a
//	 *  download rate (in bytes) for those resources
//	 * @return rateBytes
//	 */
//	public int calculateRecentFetchRate(){
//		
//		int startTime = 
//		
//		
//		
//	}

}
