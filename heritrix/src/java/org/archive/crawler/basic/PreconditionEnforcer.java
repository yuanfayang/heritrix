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
 * SimplePolitenessEnforcer.java
 * Created on May 22, 2003
 *
 * $Header$
 */
package org.archive.crawler.basic;

import java.util.logging.Logger;

import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.framework.Processor;

/**
 * Ensures the preconditions for a fetch -- such as
 * DNS lookup or acquiring a robots.txt policy -- are
 * satisfied before a URI is passed to subsequent
 * stages.
 *
 * @author gojomo
 *
 */
public class PreconditionEnforcer extends Processor implements CoreAttributeConstants, FetchStatusCodes {

    private static Logger logger = Logger.getLogger("org.archive.crawler.basic.SimplePolitenessEnforcer");

    /**
     * @param name
     */
    public PreconditionEnforcer(String name) {
        super(name, "Precondition enforcer");
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Processor#process(org.archive.crawler.datamodel.CrawlURI)
     */
    protected void innerProcess(CrawlURI curi) {

        if (considerDnsPreconditions(curi)) {
            return;
        }

        // make sure we only process schemes we understand (i.e. not dns)
        String scheme = curi.getUURI().getScheme().toLowerCase(); 
        if (! (scheme.equals("http") || scheme.equals("https")))
        {
            logger.fine("PolitenessEnforcer doesn't understand uri's of type " +
                scheme + " (ignoring)");
            return;
        }

        if (considerRobotsPreconditions(curi)) {
            return;
        }

        // OK, it's allowed

        // for all curis that will in fact be fetched, set appropriate delays
        // TODOSOMEDAY: allow per-host, per-protocol, etc. factors
//        curi.setDelayFactor(getDelayFactorFor(curi));
//        curi.setMinimumDelay(getMinimumDelayFor(curi));

        return;
    }

    private boolean considerRobotsPreconditions(CrawlURI curi) {
        // treat /robots.txt fetches specially
        if (curi.getUURI().getPath().equals("/robots.txt")) {
            // allow processing to continue
            return false;
        }
        // require /robots.txt if not present
        if (curi.getServer().isRobotsExpired()) {
            logger.fine("No valid robots for "+curi.getServer()+"; deferring "+curi);
            // Robots expired - should be refetched even though its already crawled
            curi.setPrerequisiteUri("/robots.txt");
            curi.incrementDeferrals();
            curi.setFetchStatus(S_DEFERRED);
            curi.skipToProcessorChain(getController().getPostprocessorChain());
            return true;
        }
        
        // test against robots.txt if available
        String ua = getController().getOrder().getUserAgent(curi);
        if (curi.getServer().getRobots().disallows(curi, ua)) {
            // don't fetch
            curi.skipToProcessorChain(getController().getPostprocessorChain()); // turn
                                                                                // off
                                                                                // later
                                                                                // stages
            curi.setFetchStatus(S_ROBOTS_PRECLUDED);
            curi.getAList().putString("error", "robots.txt exclusion");
            logger.fine("robots.txt precluded " + curi);
            return true;
        }
         return false;
    }

    /**
     * @param curi
     * @return true if no further processing in this module should occur
     */
    private boolean considerDnsPreconditions(CrawlURI curi) {

        if(curi.getServer()==null) {
            curi.setFetchStatus(S_UNFETCHABLE_URI);
            curi.skipToProcessorChain(getController().getPostprocessorChain());
            return true;
        }
        // if we haven't done a dns lookup  and this isn't a dns uri
        // shoot that off and defer further processing
        if (!curi.getServer().getHost().hasBeenLookedUp()
            && !curi.getUURI().getScheme().equals("dns")) {
            logger.fine(
                "deferring processing of "
                    + curi.toString()
                    + " for dns lookup.");

            String hostname = curi.getServer().getHostname();
            curi.setPrerequisiteUri("dns:" + hostname);
            //curi.getAList().putInt(A_RETRY_DELAY,0); // allow immediate retry
            curi.setFetchStatus(S_DEFERRED);
            curi.incrementDeferrals();
            curi.skipToProcessorChain(getController().getPostprocessorChain());
            return true;
        }

        // if we've done a dns lookup and it didn't resolve a host
        // cancel all processing of this URI
        if (curi.getServer().getHost().hasBeenLookedUp()
            && curi.getServer().getHost().getIP() == null) {
            logger.fine(
                "no dns for "
                    + curi.getServer().toString()
                    + " cancelling processing for "
                    + curi.toString());

            //TODO currently we're using FetchAttempts to denote both fetch attempts and
            // the choice to not attempt (here).  Eventually these will probably have to be treated seperately
            // to allow us to treat dns failures and connections failures (downed hosts, route failures, etc) seperately.
            curi.setFetchStatus(S_DOMAIN_UNRESOLVABLE);
            //curi.incrementFetchAttempts();
            curi.skipToProcessorChain(getController().getPostprocessorChain());
            return true;
        }
        return false;
    }

}
