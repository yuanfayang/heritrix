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
 * URIFrontier.java
 * Created on Oct 1, 2003
 *
 * $Header$
 */
package org.archive.crawler.framework;

import java.io.IOException;
import java.util.ArrayList;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.exceptions.FatalConfigurationException;
import org.archive.crawler.framework.exceptions.InvalidURIFrontierMarkerException;

/**
 * An interface for URI Frontiers. A URI Frontier maintains the internal
 * state of a crawl, which URIs have been crawled, discovered etc.
 * @author Gordon Mohr
 */
public interface URIFrontier {

    final static String ATTR_NAME = "frontier";

    void initialize(CrawlController c) throws FatalConfigurationException,
            IOException;

    /**
     * Schedules a new URI for crawling.
     * @param caUri URI to schedule
     */
    void schedule(CandidateURI caUri);

    /**
     * Get the next URI that should be crawled.
     * @param timeout how long the calling thread is willing to wait for the
     *                next URI to become availible.
     * @return the next URI that should be crawled
     */
    CrawlURI next(int timeout);

    /**
     * Report finished crawl URIs. Once the processors finish with a URI 
     * emited by <code>next()</code> they pass it back to the frontier via 
     * this method.
     * @param curi the finished URI
     */
    void finished(CrawlURI curi);

    /**
     * Returns true if the frontier contains no more URIs to crawl. This 
     * does not need to mean that there are any <i>availible</i> to be crawled
     * since politeness may be holding back URIs.
     * @return true if the frontier contains no more URIs to crawl
     */
    boolean isEmpty();

    /**
     * Schedule at top priority (for example, before any subsequently
     * finished() items that must be retried)
     * 
     * @param caUri URI to be scheduled.
     */
    void scheduleHigh(CandidateURI caUri);

    /**
     * Get the number of successfully downloaded documents.
     * 
     * @return the number of successfully downloaded documents
     */
    public long successfullyFetchedCount();

    /**
     * Get the number of documents that failed to download.
     * 
     * @return the number of documents that faile to download
     */
    public long failedFetchCount();

    /**
     * Get the number of documents that were disregarded (robot exclusion
     * etc.).
     * 
     * @return the number of documents that were disregarded
     */
    public long disregardedFetchCount();

    /**
     * The number of URIs that have been scheduled <i>and</i> have passed the 
     * scope test (i.e. and are confirmed for crawling). This does not include  
     * 'pending' URIs. It does include URIs that have been crawled.
     * @return number of URIs the frontier has crawled and those it knows that
     *         it wants to crawl
     */
    long discoveredUriCount();

    /**
     * Number of URIs that are awaiting detailed processing. Frontiers that
     * process URIs as it gets them may always return 0.
     * 
     * @return Estimated number of URIs scheduled for prcoessing..
     */
    long pendingUriCount();

    /**
     * Put caUri into a queue of items to be scheduled later (that is, avoid
     * synchronization overhead)
     * 
     * @param caUri URI to be scheduled
     */
    void batchSchedule(CandidateURI caUri);

    /**
     * Same as <code>batchSchedule</code> but for high priority.
     * @param caUri URI to be scheduled
     */
    void batchScheduleHigh(CandidateURI caUri);

    /**
     * Force all batch-scheduled candidates to be actually scheduled.
     *  
     */
    void batchFlush();

    /**
     * The total amounts of bytes written (uncompressed)
     * 
     * @return The total amounts of bytes written
     */
    public long totalBytesWritten();

    /**
     * This methods compiles a human readable report on the status of the
     * frontier at the time of the call.
     * 
     * @return A report on the current status of the frontier.
     */
    public String report();

    public void importRecoverLog(String pathToLog) throws IOException;

    /**
     * Get a <code>URIFrontierMarker</code> initialized with the given 
     * regular expression at the 'start' of the Frontier.
     * @param regexpr The regular expression that URIs within the frontier must
     *                match to be considered within the scope of this marker
     * @param inCacheOnly If set to true, only those URIs within the frontier
     *                that are stored in cache (usually this means in memory
     *                rather then on disk, but that is an implementation
     *                detail) will be considered. Others will be entierly 
     *                ignored, as if they dont exist. This is usefull for quick
     *                peeks at the top of the URI list. 
     * @return A URIFrontierMarker that is set for the 'start' of the frontier's
     *                URI list.
     */    
    public URIFrontierMarker getInitialMarker(String regexpr,
                                              boolean inCacheOnly);
    
    /**
     * Returns a list of all uncrawled URIs starting from a specified marker
     * until <code>numberOfMatches</code> is reached.
     * <p>
     * Any encountered URI that has not been successfully crawled, terminally
     * failed, disregarded or is currently being processed is included. As
     * there may be duplicates in the frontier, there may also be duplicates
     * in the report.
     * <p>
     * The list is a set of strings containing the URI string. If verbose is
     * true the string will include some additional information (path to URI
     * and parent).
     * <p>
     * The <code>URIFrontierMarker</code> will be advanced to the position at
     * which it's maximum number of matches found is reached. Reusing it for
     * subsequent calls will thus effectively get the 'next' batch. Making 
     * any changes to the frontier can invalidate the marker.
     * <p>
     * While the order returned is consistent, it does <i>not</i> have any
     * explicit relation to the likely order in which they may be processed.
     * <p>
     * <b>Warning:</b> It is unsafe to make changes to the frontier while
     * this method is executing. The crawler should be in a paused state before
     * invoking it.  
     * 
     * @param marker
     *            a marker returned from a previous call to this method. 
     *            <code>null</code> will start at the beginning of the pending 
     *            URI list. 
     * @param numberOfMatches
     *            how many URIs to add at most to the list before returning it
     * @param verbose
     *            if set to true the strings returned will contain additional
     *            information about each URI beyond their names.
     * @return a list of all pending URIs falling within the specification
     *            of the marker
     * @throws InvalidURIFrontierMarkerException when the 
     *            <code>URIFronterMarker</code> does not match the internal
     *            state of the frontier. Tolerance for this can vary 
     *            considerably from one URIFrontier implementation to the next.
     * @see URIFrontierMarker
     */
    public ArrayList getPendingURIsList(URIFrontierMarker marker,
                                        int numberOfMatches,
                                        boolean verbose)
                                    throws InvalidURIFrontierMarkerException;
    
    /**
     * Delete any URI that matches the given regular expression from the list
     * of pending URIs. This does not prevent them from being rediscovered.
     * <p>
     * Any encountered URI that has not been successfully crawled, terminally
     * failed, disregarded or is currently being processed is considered to be
     * a pending URI. 
     * <p>
     * <b>Warning:</b> It is unsafe to make changes to the frontier while
     * this method is executing. The crawler should be in a paused state before
     * invoking it.  
     * 
     * @param match A regular expression, any URIs that matches it will be 
     *              deleted.
     * @return the number of URIs deleted
     */
    public long deleteURIsFromPending(String match);
}
