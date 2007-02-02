/* FrontierScheduler
 * 
 * $Id$
 *
 * Created on June 6, 2005
 * 
 * Copyright (C) 2005 Internet Archive.
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
 */
package org.archive.crawler.postprocessor;


import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.framework.Frontier;
import org.archive.crawler.frontier.EmptyFrontier;
import org.archive.processors.Processor;
import org.archive.processors.ProcessorURI;
import org.archive.state.Dependency;
import org.archive.state.Key;


/**
 * 'Schedule' with the Frontier CandidateURIs being carried by the passed
 * CrawlURI.
 * Adds either prerequisites or whatever is in CrawlURI outlinks to the
 * Frontier.  Run a Scoper ahead of this processor so only links that
 * are in-scope get scheduled.
 * @author stack
 */
public class FrontierScheduler extends Processor
implements FetchStatusCodes {

    private static final long serialVersionUID = -3L;

    
    final Frontier frontier;

    
    /**
     * The frontier to use.
     */
    @Dependency
    final public static Key<Frontier> FRONTIER = Key.make(Frontier.class, 
            new EmptyFrontier());

    /**
     */
    public FrontierScheduler(Frontier frontier) {
        this.frontier = frontier;
    }

    
    protected boolean shouldProcess(ProcessorURI puri) {
        return puri instanceof CrawlURI;
    }

    @Override
    protected void innerProcess(final ProcessorURI puri) {
        CrawlURI curi = (CrawlURI)puri;
        // Handle any prerequisites when S_DEFERRED for prereqs
        if (curi.hasPrerequisiteUri() && curi.getFetchStatus() == S_DEFERRED) {
            handlePrerequisites(curi);
            return;
        }

        synchronized(this) {
            for (CandidateURI cauri: curi.getOutCandidates()) {
                schedule(cauri);
            }
        }
    }

    protected void handlePrerequisites(CrawlURI curi) {
        schedule((CandidateURI)curi.getPrerequisiteUri());
    }

    /**
     * Schedule the given {@link CandidateURI CandidateURI} with the Frontier.
     * @param caUri The CandidateURI to be scheduled.
     */
    protected void schedule(CandidateURI caUri) {
        frontier.schedule(caUri);
    }
}
