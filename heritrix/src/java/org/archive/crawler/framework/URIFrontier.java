/* URIFrontierProposed
 *
 * $Id$
 *
 * Created on Mar 29, 2004
 *
 * Copyright (C) 2004 Internet Archive.
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
 */
package org.archive.crawler.framework;

import java.io.IOException;
import java.util.ArrayList;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.framework.exceptions.FatalConfigurationException;
import org.archive.crawler.framework.exceptions.InvalidURIFrontierMarkerException;


/**
 * An interface for URI Frontiers.
 *
 * <p>A URI Frontier is a pluggable module in Heritrix that maintains the
 * internal state of the crawl. This includes (but is not limited to):
 * <ul>
 *     <li>What URIs have been discovered
 *     <li>What URIs are being processed (fetched)
 *     <li>What URIs have been processed
 *     <li>In what order unprocessed URIs will be processed
 * </ul>
 *
 * <p>The Frontier is also responsible for enforcing any politeness restrictions
 * that may have been applied to the crawl. Such as limiting simultaneous
 * connection to the same host, server or IP number to 1 (or any other fixed
 * amount), delays between connections etc.
 *
 * <p>A URIFrontier is created by the
 * {@link org.archive.crawler.framework.CrawlController CrawlController} which
 * is in turn responsible for providing access to it. Most significant among
 * those modules interested in the Frontier are the
 * {@link org.archive.crawler.framework.ToeThread ToeThreads} who perform the
 * actual work of processing a URI.
 *
 * <p>The methods defined in this interface are those required to get URIs for
 * processing, report the results of processing back (ToeThreads) and to get
 * access to various statistical data along the way. The statistical data is
 * of interest to {@link org.archive.crawler.framework.StatisticsTracking
 * Statistics Tracking} modules. A couple of additional methods are provided
 * to be able to inspect and manipulate the Frontier at runtime.
 *
 * <p>The statistical data exposed by this interface is:
 * <ul>
 *     <li> {@link #discoveredUriCount() Discovered URIs}
 *     <li> {@link #queuedUriCount() Queued URIs}
 *     <li> {@link #finishedUriCount() Finished URIs}
 *     <li> {@link #pendingUriCount() Pending URIs}
 *     <li> {@link #successfullyFetchedCount() Successfully processed URIs}
 *     <li> {@link #failedFetchCount() Failed to process URIs}
 *     <li> {@link #disregardedFetchCount() Disregarded URIs}
 *     <li> {@link #totalBytesWritten() Total bytes written}
 * </ul>
 *
 * <p>In addition the frontier may optionally implement an interface that
 * exposes information about hosts.
 *
 * <p>Furthermore any implementation of the URI Frontier should trigger
 * {@link org.archive.crawler.event.CrawlURIDispositionListener
 * CrawlURIDispostionEvents} by invoking the proper methods on the
 * {@link org.archive.crawler.framework.CrawlController CrawlController}.
 * Doing this allows a custom built
 * {@link org.archive.crawler.framework.StatisticsTracking
 * Statistics Tracking} module to gather any other additional data it might be
 * interested in by examining the completed URIs.
 *
 * <p>All URI Frontiers inherit from
 * {@link org.archive.crawler.settings.ModuleType ModuleType}
 * and therefore creating settings follows the usual pattern of pluggable modules
 * in Heritrix.
 *
 * @author Gordon Mohr
 * @author Kristinn Sigurdsson
 *
 * @see org.archive.crawler.framework.CrawlController
 * @see org.archive.crawler.framework.CrawlController#fireCrawledURIDisregardEvent(CrawlURI)
 * @see org.archive.crawler.framework.CrawlController#fireCrawledURIFailureEvent(CrawlURI)
 * @see org.archive.crawler.framework.CrawlController#fireCrawledURINeedRetryEvent(CrawlURI)
 * @see org.archive.crawler.framework.CrawlController#fireCrawledURISuccessfulEvent(CrawlURI)
 * @see org.archive.crawler.framework.StatisticsTracking
 * @see org.archive.crawler.framework.ToeThread
 * @see org.archive.crawler.framework.URIFrontierHostStatistics
 * @see org.archive.crawler.settings.ModuleType
 */
public interface URIFrontier {

    /**
     * All URI Frontiers should have the same 'name' attribute. This constant
     * defines that name. This is a name used to reference the Frontier being
     * used in a given crawl order and since there can only be one Frontier
     * per crawl order a fixed, unique name for Frontiers is optimal.
     *
     * @see org.archive.crawler.settings.ModuleType#ModuleType(String)
     */
    public static final String ATTR_NAME = "frontier";

    /**
     * Initialize the Frontier.
     *
     * <p> This method is invoked by the CrawlController once it has
     * created the Frontier. The constructor of the Frontier should
     * only contain code for setting up it's settings framework. This
     * method should contain all other 'startup' code.
     *
     * @param c The CrawlController that created the Frontier.
     *
     * @throws FatalConfigurationException If provided settings are illegal or
     *            otherwise unusable.
     * @throws IOException If there is a problem reading settings or seeds file
     *            from disk.
     */
    public void initialize(CrawlController c)
            throws FatalConfigurationException, IOException;

    /**
     * Get the next URI that should be processed. If no URI becomes availible
     * during the time specified null will be returned.
     *
     * @param timeout how long the calling thread is willing to wait for the
     * next URI to become available (milliseconds).
     * @return the next URI that should be processed.
     * @throws InterruptedException
     */
    CrawlURI next(int timeout) throws InterruptedException;

    /**
     * Returns true if the frontier contains no more URIs to crawl.
     *
     * <p>That is to say that there are no more URIs either currently availible
     * (ready to be emitted), URIs belonging to deferred hosts or pending URIs
     * in the Frontier. Thus this method may return false even if there is no
     * currently availible URI.
     *
     * @return true if the frontier contains no more URIs to crawl.
     */
    boolean isEmpty();

    /**
     * Schedules a CandidateURI.
     *
     * <p>This method accepts one URI and schedules it immediately. This has
     * nothing to do with the priority of the URI being scheduled. Only that
     * it will be placed in it's respective queue at once. For priority
     * scheduling see {@link CandidateURI#setSchedulingDirective(String)
     * CandidateURI}
     *
     * <p>This method should be synchronized in all implementing classes.
     *
     * @param caURI The URI to schedule.
     *
     * @see #batchSchedule(CandidateURI)
     * @see CandidateURI#setSchedulingDirective(String)
     */
    public void schedule(CandidateURI caURI);

    /**
     * Schedules a CandidateURI.
     *
     * <p>This is a non-synchronized method for scheduling large numbers of
     * URIs at a time. All URIs scheduled with this method will be 'held' in
     * a thread specific container until {@link #batchFlush() batchFlush()} is
     * invoked.
     *
     * @param caURI The URI to schedule.
     *
     * @see #schedule(CandidateURI)
     * @see #batchFlush()
     */
    public void batchSchedule(CandidateURI caURI);

    /**
     * Forces all the URIs that have been batched up for scheduling by the
     * {@link #batchSchedule(CandidateURI) batchSchedule()} method to be
     * actually scheduled.
     *
     * <p>This is a synchronized method.
     */
    public void batchFlush();

    /**
     * Report a URI being processed as having finished processing.
     *
     * <p>ToeThreads will invoke this method once they have completed work on
     * their assigned URI.
     *
     * <p>This method is synchronized and also schedules any URIs that have been
     * batched up by {@link #batchSchedule(CandidateURI) batchSchedule()}
     *
     * @param cURI The URI that has finished processing.
     *
     * @see #batchFlush()
     */
    public void finished(CrawlURI cURI);

    /**
     * Number of <i>discovered</i> URIs.
     *
     * <p>That is any URI that has been confirmed be within 'scope'
     * (i.e. the Frontier decides that it should be processed). This
     * includes those that have been processed, are being processed
     * and have finished processing. Does not include URIs that have
     * been 'forgotten' (deemed out of scope when trying to fetch,
     * most likely due to operator changing scope definition).
     *
     * <p><b>Note:</b> This only counts discovered URIs. Since the same
     * URI can (at least in most frontiers) be fetched multiple times, this
     * number may be somewhat lower then the combined <i>queued</i>,
     * <i>in process</i> and <i>finished</i> items combined due to duplicate
     * URIs being queued and processed. This variance is likely to be especially
     * high in Frontiers implementing 'revist' strategies.
     *
     * @return Number of discovered URIs.
     */
    public long discoveredUriCount();

    /**
     * Number of URIs <i>queued</i> up and waiting for processing.
     *
     * <p>This includes any URIs that failed but will be retried. Basically this
     * is any <i>discovered</i> URI that has not either been processed or is
     * being processed. The same discovered URI can be queued multiple times.
     *
     * @return Number of queued URIs.
     */
    public long queuedUriCount();

    /**
     * Number of URIs that have <i>finished</i> processing.
     *
     * <p>Includes both those that were processed successfully and failed to be
     * processed (excluding those that failed but will be retried). Does not
     * include those URIs that have been 'forgotten' (deemed out of scope when
     * trying to fetch, most likely due to operator changing scope definition).
     *
     * @return Number of finished URIs.
     */
    public long finishedUriCount();

    /**
     * Number of URIs that are awaiting detailed processing.
     *
     * <p>Number of discovered URIs that have not been inspected for scope or
     * duplicates (generally referred to as <i>pending</i> URIs. Depending
     * on the implementation of the <tt>URIFrontier</tt> this might always be
     * zero. It may also be an adjusted number that tries to account for
     * duplicates by estimation.
     *
     * <p>This does not count URIs scheduled with
     * {@link #batchSchedule(CandidateURI) batchSchedule()} and are waiting for
     * the batch to be flushed.
     *
     * @return Estimated number of URIs scheduled for prcoessing.
     */
    public long pendingUriCount();

    /**
     * Number of <i>successfully</i> processed URIs.
     *
     * <p>Any URI that was processed successfully. This includes URIs that
     * returned 404s and other error codes that do not originate within the
     * crawler.
     *
     * @return Number of <i>successfully</i> processed URIs.
     */
    public long successfullyFetchedCount();

    /**
     * Number of URIs that <i>failed</i> to process.
     *
     * <p>URIs that could not be processed because of some error or failure in
     * the processing chain. Can include failure to acquire prerequisites, to
     * establish a connection with the host and any number of other problems.
     * Does not count those that will be retried, only those that have
     * permenantly failed.
     *
     * @return Number of URIs that failed to process.
     */
    public long failedFetchCount();

    /**
     * Number of URIs that were successfully fetched but have been
     * <i>disregarded</i>.
     *
     * <p>Counts any URI that is successfully fetched only to be disregarded
     * because it is determined to lie outside the scope of the crawl. Most
     * commonly this will be due to robots.txt exclusions.
     *
     * @return The number of URIs that have been disregarded.
     */
    public long disregardedFetchCount();

    /**
     * Total number of bytes contained in all URIs that have been processed.
     *
     * @return The total amounts of bytes in all processed URIs.
     */
    public long totalBytesWritten();

    /**
     * This methods compiles a human readable report on the status of the
     * frontier at the time of the call.
     *
     * <p>This report should give an accurate picture of the current state of
     * the frontier.
     *
     * @return A report on the current status of the frontier.
     */
    public String report();

    /**
     * Recover earlier state by reading a recovery log.
     *
     * <p>Some Frontiers are able to write detailed logs that can be loaded
     * after a system crash to recover the state of the Frontier prior to the
     * crash. This method is the one used to achive this.
     *
     * @param pathToLog The name (with full path) of the recover log.
     * @throws IOException If problems occur reading the recover log.
     */
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
     *
     * <p>Any encountered URI that has not been successfully crawled, terminally
     * failed, disregarded or is currently being processed is included. As
     * there may be duplicates in the frontier, there may also be duplicates
     * in the report. Thus this includes both discovered and pending URIs.
     *
     * <p>The list is a set of strings containing the URI strings. If verbose is
     * true the string will include some additional information (path to URI
     * and parent).
     *
     * <p>The <code>URIFrontierMarker</code> will be advanced to the position at
     * which it's maximum number of matches found is reached. Reusing it for
     * subsequent calls will thus effectively get the 'next' batch. Making
     * any changes to the frontier can invalidate the marker.
     *
     * <p>While the order returned is consistent, it does <i>not</i> have any
     * explicit relation to the likely order in which they may be processed.
     *
     * <p><b>Warning:</b> It is unsafe to make changes to the frontier while
     * this method is executing. The crawler should be in a paused state before
     * invoking it.
     *
     * @param marker
     *            A marker specifing from what position in the Frontier the
     *            list should begin.
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
     * @see #getInitialMarker(String, boolean)
     */
    public ArrayList getURIsList(URIFrontierMarker marker,
                                 int numberOfMatches,
                                 boolean verbose)
                             throws InvalidURIFrontierMarkerException;

    /**
     * Delete any URI that matches the given regular expression from the list
     * of discovered and pending URIs. This does not prevent them from being
     * rediscovered.
     *
     * <p>Any encountered URI that has not been successfully crawled, terminally
     * failed, disregarded or is currently being processed is considered to be
     * a pending URI.
     *
     * <p><b>Warning:</b> It is unsafe to make changes to the frontier while
     * this method is executing. The crawler should be in a paused state before
     * invoking it.
     *
     * @param match A regular expression, any URIs that matches it will be
     *              deleted.
     * @return The number of URIs deleted
     */
    public long deleteURIs(String match);

    /**
     * Notify Frontier that a CrawlURI has been deleted outside of the
     * normal next()/finished() lifecycle. 
     * 
     * @param curi Deleted CrawlURI.
     */
    public void deleted(CrawlURI curi);

    /**
     * Notify Frontier that it should consider the given UURI as if
     * already scheduled.
     * 
     * @param u
     */
    public void considerIncluded(UURI u);

    /**
     * Notify Frontier that it should consider updating configuration
     * info that may have changed in external files.
     */
    public void kickUpdate();
}
