/*
 * Created on Jul 16, 2003
 *
 */
package org.archive.crawler.basic;

import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.*;
import java.util.List;
import java.util.*;

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
	
	
	

}
