/* FrontierJournal
 * 
 * Created on Oct 26, 2004
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
package org.archive.crawler.frontier;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.UURI;

/**
 * Record of key Frontier happenings.
 * @author stack
 * @version $Date$, $Revision$
 */
public interface FrontierJournal {
    /**
     * @param curi CrawlURI that has been scheduled to be added to the
     * Frontier.
     */
    public abstract void added(CrawlURI curi);

    /**
     * @param curi CrawlURI that finished successfully.
     */
    public abstract void finishedSuccess(CrawlURI curi);

    /**
     * @param curi UURI that finished successfully.
     */
    public abstract void finishedSuccess(UURI uuri);

    /**
     * Note that a CrawlURI was emitted for processing.
     * If not followed by a finished or rescheduled notation in
     * the journal, the CrawlURI was still in-process when the journal ended.
     * 
     * @param curi CrawlURI emitted.
     */
    public abstract void emitted(CrawlURI curi);

    /**
     * @param curi CrawlURI finished unsuccessfully.
     */
    public abstract void finishedFailure(CrawlURI curi);

    /**
     * @param curi CrawlURI that was returned to the Frontier for 
     * another try.
     */
    public abstract void rescheduled(CrawlURI curi);

    /**
     *  Flush and close any held objects.
     */
    public abstract void close();
}