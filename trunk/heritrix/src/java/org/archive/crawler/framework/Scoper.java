/* Scoper
 * 
 * Created on Jun 6, 2005
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
 */
package org.archive.crawler.framework;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.CandidateURI;

/**
 * Base class for Scopers.
 * Scopers test CrawlURIs against configured scope.
 * @author stack
 * @version $Date$, $Revision$
 */
public abstract class Scoper extends Processor {
    private static Logger logger =
        Logger.getLogger(Scoper.class.getName());
    
    /**
     * Constructor.
     * @param name
     * @param description
     */
    public Scoper(String name, String description) {
        super(name, description);
    }
    
    /**
     * Schedule the given {@link CandidateURI CandidateURI} with the Frontier.
     * @param caUri The CandidateURI to be scheduled.
     * @return true if CandidateURI was accepted by crawl scope, false
     * otherwise.
     */
    protected boolean isInScope(CandidateURI caUri) {
        if(getController().getScope().accepts(caUri)) {
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("Accepted: " + caUri);
            }
            return true;
        }
        outOfScope(caUri);
        return false;
    }
    
    /**
     * Called when a CandidateUri is ruled out of scope.
     * @param caUri CandidateURI that is out of scope.
     */
    protected void outOfScope(CandidateURI caUri) {
        // Default is do nothing.
    }
}
