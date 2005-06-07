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

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.extractor.Link;
import org.archive.crawler.framework.Processor;

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
    private static Logger LOGGER =
        Logger.getLogger(FrontierScheduler.class.getName());
    
    /**
     * @param name Name of this filter.
     */
    public FrontierScheduler(String name) {
        super(name, "FrontierScheduler. 'Schedule' with the Frontier " +
            "any CandidateURIs carried by the passed CrawlURI. " +
            "Run a Scoper before this " +
            "processor so links that are not in-scope get bumped from the " +
            "list of links (And so those in scope get promoted from Link " +
            "to CandidateURI).");
    }

    protected void innerProcess(final CrawlURI curi) {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest(getName() + " processing " + curi);
        }
        
        // Handle any prerequisites.
        if (curi.hasPrerequisitUri()) {
            handlePrerequisites(curi);
            return;
        }

        for (final Iterator iter = curi.getOutLinks().iterator();
                iter.hasNext();) {
            Object obj = iter.next();
            CandidateURI cauri = null;
            if (obj instanceof CandidateURI) {
                cauri = (CandidateURI)obj;
            } else {
                LOGGER.severe("Unexpected type: " + obj);
            }
            
            if (cauri != null) { 
                schedule(cauri);
            }
        }
    }

    protected void handlePrerequisites(CrawlURI curi) {
        try {
            // Create prerequisite.
            CandidateURI caUri =
                curi.createCandidateURI(curi.getPrerequisiteUri());
            int prereqPriority = curi.getSchedulingDirective() - 1;
            if (prereqPriority < 0) {
                prereqPriority = 0;
                LOGGER.severe("Unable to promote prerequisite " + caUri +
                    " above " + curi);
            }
            caUri.setSchedulingDirective(curi.getSchedulingDirective() - 1);
            caUri.setForceFetch(true);
            schedule(caUri);
       } catch (URIException ex) {
            Object[] array = {curi, curi.getPrerequisiteUri()};
            getController().uriErrors.log(Level.INFO,ex.getMessage(), array);
        } catch (NumberFormatException e) {
            // UURI.createUURI will occasionally throw this error.
            Object[] array = {curi, curi.getPrerequisiteUri()};
            getController().uriErrors.log(Level.INFO,e.getMessage(), array);
        }
    }

    /**
     * Schedule the given {@link CandidateURI CandidateURI} with the Frontier.
     * @param caUri The CandidateURI to be scheduled.
     */
    protected void schedule(CandidateURI caUri) {
        getController().getFrontier().schedule(caUri);
    }
}
