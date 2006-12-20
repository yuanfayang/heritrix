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
import org.archive.crawler.util.LogUtils;
import org.archive.processors.deciderules.DecideResult;
import org.archive.state.Key;
import org.archive.state.StateProvider;

/**
 * Base class for Scopers.
 * Scopers test CandidateURIs against a scope.
 * Scopers allow logging of rejected CandidateURIs.
 * @author stack
 * @version $Date$, $Revision$
 */
public abstract class Scoper extends org.archive.processors.Processor {
    private static Logger LOGGER =
        Logger.getLogger(Scoper.class.getName());
    

    /**
     * If enabled, override default logger for this class (Default logger writes
     * the console). Override logger will instead send all logging to a file
     * named for this class in the job log directory. Set the logging level and
     * other characteristics of the override logger such as rotation size,
     * suffix pattern, etc. in heritrix.properties. This attribute is only
     * checked once, on startup of a job.
     */
    final public static Key<Boolean> OVERRIDE_LOGGER = Key.makeExpert(false);

    
    // FIXME: Weirdo log overriding might not work on a per-subclass basis,
    // we may need to cut and paste it to the three subclasses, or eliminate
    // it in favor of java.util.logging best practice.
    //
    // Also, eliminating weirdo log overriding would mean we wouldn't need to
    // tie into the CrawlController; we'd just need the scope.
    
    
    final CrawlController controller;


    /**
     * Constructor.
     * 
     * @param controller   the CrawlController
     */
    public Scoper(CrawlController controller) {
        this.controller = controller;
    }


    @Override
    public void initialTasks(StateProvider defaults) {
        super.initialTasks(defaults);
        if (!defaults.get(this, OVERRIDE_LOGGER)) {
            return;
        }

        // Set up logger for this instance.  May have special directives
        // since this class can log scope-rejected URLs.
        LogUtils.createFileLogger(getController().getLogsDir(),
            this.getClass().getName(),
            Logger.getLogger(this.getClass().getName()));
    }


    /**
     * Schedule the given {@link CandidateURI CandidateURI} with the Frontier.
     * @param caUri The CandidateURI to be scheduled.
     * @return true if CandidateURI was accepted by crawl scope, false
     * otherwise.
     */
    protected boolean isInScope(CandidateURI caUri) {
        boolean result = false;
        CrawlScope scope = getController().getScope();
        getController().setStateProvider(caUri);
        DecideResult dr = scope.decisionFor(caUri.asProcessorURI());
        if (dr == DecideResult.ACCEPT) {
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
     * Override if you don't want logs as coming from this class.
     * @param caUri CandidateURI that is out of scope.
     */
    protected void outOfScope(CandidateURI caUri) {
        if (!LOGGER.isLoggable(Level.INFO)) {
            return;
        }
        LOGGER.info(caUri.getUURI().toString());
    }


    protected CrawlController getController() {
        return controller;
    }
}
