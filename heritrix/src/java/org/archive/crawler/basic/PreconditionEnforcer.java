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

import javax.management.AttributeNotFoundException;

import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlHost;
import org.archive.crawler.datamodel.CrawlServer;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.datamodel.settings.SimpleType;
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

    private final static Integer DEFAULT_IP_VALIDITY_DURATION = new Integer(-1);
    private final static Integer DEFAULT_ROBOTS_VALIDITY_DURATION =
        new Integer(3*(60*24)); // three days

    public final static String ATTR_IP_VALIDITY_DURATION =
        "ip-validity-duration-m";
    public final static String ATTR_ROBOTS_VALIDITY_DURATION = "robot-validity-duration-m";

    /**
     * @param name
     */
    public PreconditionEnforcer(String name) {
        super(name, "Precondition enforcer");

        addElementToDefinition(new SimpleType(ATTR_IP_VALIDITY_DURATION,
                "How long a dns-record is considered valid (in minutes). \n" +
                "If the value is set to '-1', then the dns-record's ttl-value" +
                "will be used. If set to '0', then the dns will never expire.",
                DEFAULT_IP_VALIDITY_DURATION));

        addElementToDefinition(new SimpleType(ATTR_ROBOTS_VALIDITY_DURATION,
                "The time in minutes, between robots.txt are refreshed.\n" +
                "If the value is set to '0', then the robots will never" +
                " expire.",
                DEFAULT_ROBOTS_VALIDITY_DURATION));
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
        if (isRobotsExpired(curi)) {
            logger.fine( "No valid robots for " + curi.getServer()
                + "; deferring " + curi);
                
                // Robots expired - should be refetched even though its already
                // crawled.
             curi.setPrerequisiteUri(
                curi.getUURI().getRawUri().resolve("/robots.txt").toString());
                
             curi.incrementDeferrals();
             curi.setFetchStatus(S_DEFERRED);
             curi.skipToProcessorChain(getController().getPostprocessorChain());
             return true;
         }
         // test against robots.txt if available
          String ua = getController().getOrder().getUserAgent(curi);
         if( curi.getServer().getRobots().disallows(curi, ua)) {
             // don't fetch
             curi.skipToProcessorChain(getController().getPostprocessorChain());  // turn off later stages
             curi.setFetchStatus(S_ROBOTS_PRECLUDED);
             curi.getAList().putString("error","robots.txt exclusion");
             logger.fine("robots.txt precluded "+curi);
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
        if (isIpExpired(curi) && !curi.getUURI().getScheme().equals("dns")) {
            logger.fine(
                "deferring processing of "
                    + curi.toString()
                    + " for dns lookup.");

            curi.setPrerequisiteUri("dns:" + curi.getServer().getHostname());
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

    /** Get the maximum time a dns-record is valid.
     * 
     * @param curi the uri this time is valid for.
     * @return the maximum time a dns-record is valid or negative if record's
     *         ttl should be used.
     */
    public long getIPValidityDuration(CrawlURI curi) {
        Integer d;
        try {
            d = (Integer)getAttribute(ATTR_IP_VALIDITY_DURATION, curi);
        } catch (AttributeNotFoundException e) {
            d = DEFAULT_IP_VALIDITY_DURATION;
        }
        
        return d.longValue() * 60 * 1000;
    }

    /** Return true if ip should be looked up.
     * 
     * @param curi the URI to check.
     * @return true if ip should be looked up.
     */
    public boolean isIpExpired(CrawlURI curi) {
        CrawlHost host = curi.getServer().getHost();
        if (!host.hasBeenLookedUp()) {
            // IP has not been looked up yet.
            return true;
        }
        
        if (host.getIpTTL() == CrawlHost.IP_NEVER_EXPIRES) {
            // IP never expires (numeric IP)
            return false;
        }
        
        long duration = getIPValidityDuration(curi);
        if (duration == 0) {
            // Never expire ip if duration is null (set by user)
            return false;
        }
        
        if (duration < 0) {
            // If duration is negative dns record's ttl should be used
            duration = host.getIpTTL();
        }
        return (duration + host.getIpFetched()) < System.currentTimeMillis();
    }

    /** Get the maximum time a robots.txt is valid.
     * 
     * @param settings the settings object this time is valid for.
     * @return the time a robots.txt is valid in milliseconds.
     */
    public long getRobotsValidityDuration(CrawlURI curi) {
        Integer d;
        try {
            d = (Integer) getAttribute(ATTR_ROBOTS_VALIDITY_DURATION, curi);
        } catch (AttributeNotFoundException e) {
            // This should never happen, but if it does, return default
            logger.severe(e.getLocalizedMessage());
            d = DEFAULT_ROBOTS_VALIDITY_DURATION;
        }
        // convert from minutes to milliseconds
        return d.longValue() * 60 * 1000;
    }

    /** Is the robots policy expired.
    *
    * This method will also return true if we haven't tried to get the
    * robots.txt for this server. 
    * 
    * @return true if the robots policy is expired.
    */
   public boolean isRobotsExpired(CrawlURI curi) {
       long robotsFetched = curi.getServer().getRobotsFetchedTime();
       if (robotsFetched == CrawlServer.ROBOTS_NOT_FETCHED) {
           // Have not attempted to fetch robots
           return true;
       }

       long duration = getRobotsValidityDuration(curi);
       
       if (duration == 0) {
           // When zero, robots should be valid forever
           return false;
       }

       if (robotsFetched + duration < System.currentTimeMillis()) {
           // Robots is still valid
           return true;
       }
       
       return false;
   }

}
