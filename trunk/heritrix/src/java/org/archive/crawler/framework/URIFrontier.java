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

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.exceptions.FatalConfigurationException;

/**
 * @author gojomo
 *
 */
public interface URIFrontier {
    final static String ATTR_NAME = "frontier";

    void initialize(CrawlController c) throws FatalConfigurationException, IOException;

    void schedule(CandidateURI caUri);
    CrawlURI next(int timeout);
    void finished(CrawlURI curi);

    boolean isEmpty();

    /**
     * Schedule at top priority (for example, before any
     * subsequently finished() items that must be retried)
     *
     * @param caUri
     */
    void scheduleHigh(CandidateURI caUri);

    /**
     * Get the number of successfully downloaded documents.
     * @return the number of successfully downloaded documents
     */
    public long successfullyFetchedCount();

    /**
     * Get the number of documents that failed to download.
     * @return the number of documents that faile to download
     */
    public long failedFetchCount();

    /**
     * Get the number of documents that were disregarded (robot exclusion etc.).
     * @return the number of documents that were disregarded
     */
    public long disregardedFetchCount();

    long discoveredUriCount();

    /**
     * @return Estimated number of URIs scheduled for prcoessing..
     */
    long pendingUriCount();

    /**
     * Put caUri into a queue of items to be scheduled
     * later (that is, avoid synchronization overhead)
     * @param caUri
     */
    void batchSchedule(CandidateURI caUri);

    /**
     * @param caUri
     */
    void batchScheduleHigh(CandidateURI caUri);

    /**
     * Force all batch-scheduled candidates to be
     * actually scheduled.
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
    * This methods compiles a human readable report on the status of the frontier
    * at the time of the call.
    *
    * @return A report on the current status of the frontier.
    */
    public String report();
    
    public void importRecoverLog(String pathToLog) throws IOException;
}
