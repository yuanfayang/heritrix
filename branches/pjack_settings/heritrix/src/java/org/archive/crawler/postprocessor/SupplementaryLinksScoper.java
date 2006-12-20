/* SupplementaryLinksScoper
 * 
 * $Id$
 *
 * Created on Oct 2, 2003
 * 
 * Copyright (C) 2003 Internet Archive.
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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.Scoper;
import org.archive.processors.ProcessorURI;
import org.archive.processors.deciderules.DecideResult;
import org.archive.processors.deciderules.DecideRuleSequence;
import org.archive.state.Key;


/**
 * Run CandidateURI links carried in the passed CrawlURI through a filter
 * and 'handle' rejections.
 * Used to do supplementary processing of links after they've been scope
 * processed and ruled 'in-scope' by LinkScoper.  An example of
 * 'supplementary processing' would check that a Link is intended for
 * this host to crawl in a multimachine crawl setting. Configure filters to
 * rule on links.  Default handler writes rejected URLs to disk.  Subclass
 * to handle rejected URLs otherwise.
 * @author stack
 */
public class SupplementaryLinksScoper extends Scoper {

    private static final long serialVersionUID = -3L;

    private static Logger LOGGER =
        Logger.getLogger(SupplementaryLinksScoper.class.getName());
    

    /**
     * Rules to apply to each link carried by the URI.
     */
    final public static Key<DecideRuleSequence> LINK_RULES = 
        Key.makeExpert(new DecideRuleSequence());
    
    
    /**
     * @param name Name of this filter.
     */
    public SupplementaryLinksScoper(CrawlController controller) {
        super(controller);
    }

    
    protected boolean shouldProcess(ProcessorURI puri) {
        return puri instanceof CrawlURI;
    }
    
    
    protected void innerProcess(final ProcessorURI puri) {
        CrawlURI curi = (CrawlURI)puri;
        
        // If prerequisites or no links, nothing to be done in here.
        if (curi.hasPrerequisiteUri() || curi.getOutLinks().isEmpty()) {
            return;
        }
        
        Collection<CandidateURI> inScopeLinks = new HashSet<CandidateURI>();
        Iterator<CandidateURI> iter = curi.getOutCandidates().iterator();
        while (iter.hasNext()) {
            CandidateURI cauri = iter.next();
            if (!isInScope(cauri)) {
                iter.remove();
            }
        }
//        for (CandidateURI cauri: curi.getOutCandidates()) {
//            if (isInScope(cauri)) {
//                inScopeLinks.add(cauri);
//            }
//        }
        // Replace current links collection w/ inscopeLinks.  May be
        // an empty collection.
//        curi.replaceOutlinks(inScopeLinks);
    }
    
    protected boolean isInScope(CandidateURI caUri) {
        // TODO: Fix filters so work on CandidateURI.
        CrawlURI curi = (caUri instanceof CrawlURI)?
            (CrawlURI)caUri:
            new CrawlURI(caUri.getUURI());
        boolean result = false;
        DecideRuleSequence seq = curi.get(this, LINK_RULES);
        if (seq.decisionFor(curi) == DecideResult.ACCEPT) {
            result = true;
            if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.finer("Accepted: " + caUri);
            }
        } else {
            outOfScope(caUri);
        }
        return result;
    }
    
    /**
     * Called when a CandidateUri is ruled out of scope.
     * @param caUri CandidateURI that is out of scope.
     */
    protected void outOfScope(CandidateURI caUri) {
        if (!LOGGER.isLoggable(Level.INFO)) {
            return;
        }
        LOGGER.info(caUri.getUURI().toString());
    }
}