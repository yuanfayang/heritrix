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
package org.archive.crawler.prefetch;

import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlHost;
import org.archive.crawler.datamodel.CrawlServer;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.CredentialStore;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.datamodel.credential.Credential;
import org.archive.crawler.datamodel.credential.CredentialAvatar;
import org.archive.crawler.framework.Processor;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.Type;

/**
 * Ensures the preconditions for a fetch -- such as
 * DNS lookup or acquiring a robots.txt policy -- are
 * satisfied before a URI is passed to subsequent
 * stages.
 *
 * @author gojomo
 *
 */
public class PreconditionEnforcer
        extends Processor
        implements CoreAttributeConstants, FetchStatusCodes {

    private static final Logger logger =
        Logger.getLogger(PreconditionEnforcer.class.getName());

    private final static Integer DEFAULT_IP_VALIDITY_DURATION = 
        new Integer(60*60*6); // six hours 
    private final static Integer DEFAULT_ROBOTS_VALIDITY_DURATION =
        new Integer(60*60*24); // one day

    /** seconds to keep IP information for */
    public final static String ATTR_IP_VALIDITY_DURATION
        = "ip-validity-duration-seconds";
    /** seconds to cache robots info */
    public final static String ATTR_ROBOTS_VALIDITY_DURATION
        = "robot-validity-duration-seconds";

    /**
     * @param name
     */
    public PreconditionEnforcer(String name) {
        super(name, "Precondition enforcer");

        Type e;

        e = addElementToDefinition(new SimpleType(ATTR_IP_VALIDITY_DURATION,
                "The minimum interval for which a dns-record will be considered " +
                "valid (in seconds). \n" +
                "If the record's DNS TTL is larger, that will be used instead.",
                DEFAULT_IP_VALIDITY_DURATION));
        e.setExpertSetting(true);

        e = addElementToDefinition(new SimpleType(ATTR_ROBOTS_VALIDITY_DURATION,
                "The time in seconds that fetched robots.txt information is " +
                "considered to be valid.\n" +
                "If the value is set to '0', then the robots.txt information" +
                " will never expire.",
                DEFAULT_ROBOTS_VALIDITY_DURATION));
        e.setExpertSetting(true);
    }

    protected void innerProcess(CrawlURI curi) {

        if (considerDnsPreconditions(curi)) {
            return;
        }

        // make sure we only process schemes we understand (i.e. not dns)
        String scheme = curi.getUURI().getScheme().toLowerCase();
        if (! (scheme.equals("http") || scheme.equals("https"))) {
            logger.fine("PolitenessEnforcer doesn't understand uri's of type " +
                scheme + " (ignoring)");
            return;
        }

        if (considerRobotsPreconditions(curi)) {
            return;
        }

        if (!curi.isPrerequisite() && credentialPrecondition(curi)) {
            return;
        }

        // OK, it's allowed

        // For all curis that will in fact be fetched, set appropriate delays.
        // TODO: SOMEDAY: allow per-host, per-protocol, etc. factors
        // curi.setDelayFactor(getDelayFactorFor(curi));
        // curi.setMinimumDelay(getMinimumDelayFor(curi));

        return;
    }

    /**
     * Consider the robots precondition.
     *
     * @param curi CrawlURI we're checking for any required preconditions.
     * @return True, if this <code>curi</code> has a precondition or processing
     *         should be terminated for some other reason.  False if
     *         we can precede to process this url.
     */
    private boolean considerRobotsPreconditions(CrawlURI curi) {
        // treat /robots.txt fetches specially
        UURI uuri = curi.getUURI();
        try {
            if (uuri != null && uuri.getPath() != null &&
                    curi.getUURI().getPath().equals("/robots.txt")) {
                // allow processing to continue
                curi.setPrerequisite(true);
                return false;
            }
        }
        catch (URIException e) {
            logger.severe("Failed get of path for " + curi);
        }
        // require /robots.txt if not present
        if (isRobotsExpired(curi)) {
        	// Need to get robots
            logger.fine( "No valid robots for " + curi.getServer()
                + "; deferring " + curi);

            // Robots expired - should be refetched even though its already
            // crawled.
            try {
                curi.markPrerequisite(
                    curi.getUURI().resolve("/robots.txt").toString(),
                    getController().getPostprocessorChain());
            }
            catch (URIException e1) {
                logger.severe("Failed resolve using " + curi);
            }
            return true;
        }
        // test against robots.txt if available
        if(curi.getServer().isValidRobots()){
            String ua = getController().getOrder().getUserAgent(curi);
            if( curi.getServer().getRobots().disallows(curi, ua)) {
                // Don't fetch and turn off later stages of processing.
                curi.skipToProcessorChain(getController().getPostprocessorChain());
                curi.setFetchStatus(S_ROBOTS_PRECLUDED);
                curi.getAList().putString("error","robots.txt exclusion");
                logger.fine("robots.txt precluded " + curi);
                return true;
            }
            return false;
        } else {
            // No valid robots found => Attempt to get robots.txt failed
            curi.skipToProcessorChain(getController().getPostprocessorChain());
            curi.setFetchStatus(S_PREREQUISITE_FAILURE);
            curi.getAList().putString("error","robots.txt prerequisite failed");
            logger.fine("robots.txt prerequisite failed " + curi);
            return true;
        }
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

        // if we've done a dns lookup and it didn't resolve a host
        // cancel further fetch-processing of this URI, because
        // the domain is unresolvable
        if (curi.getServer().getHost().hasBeenLookedUp()
            && curi.getServer().getHost().getIP() == null) {
            logger.fine(
                "no dns for "
                    + curi.getServer().toString()
                    + " cancelling processing for "
                    + curi.toString());
            curi.setFetchStatus(S_DOMAIN_UNRESOLVABLE);
            curi.skipToProcessorChain(getController().getPostprocessorChain());
            return true;
        }

        // if we haven't done a dns lookup  and this isn't a dns uri
        // shoot that off and defer further processing
        if (isIpExpired(curi) && !curi.getUURI().getScheme().equals("dns")) {
            logger.fine("Deferring processing of " + curi.toString()
                + " for dns lookup.");
            curi.markPrerequisite("dns:" + curi.getServer().getHostname(),
                getController().getPostprocessorChain());
            return true;
        }
        if(curi.getUURI().getScheme().equals("dns")){
            curi.setPrerequisite(true);
        }

        // DNS preconditions OK
        return false;
    }

    /**
     * Get the maximum time a dns-record is valid.
     *
     * @param curi the uri this time is valid for.
     * @return the maximum time a dns-record is valid -- in seconds -- or
     * negative if record's ttl should be used.
     */
    public long getIPValidityDuration(CrawlURI curi) {
        Integer d;
        try {
            d = (Integer)getAttribute(ATTR_IP_VALIDITY_DURATION, curi);
        } catch (AttributeNotFoundException e) {
            d = DEFAULT_IP_VALIDITY_DURATION;
        }

        return d.longValue();
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
            // Never expire ip if duration is null (set by user or more likely,
            // set to zero in case where we tried in FetchDNS but failed).
            return false;
        }

        long ttl = host.getIpTTL();
        if (ttl > duration) {
            // Use the larger of the operator-set minimum duration 
            // or the DNS record TTL
            duration = ttl;
        }
        
        // catch old "default" settings that are now problematic
        if (duration <= 0) {
            duration = DEFAULT_IP_VALIDITY_DURATION.intValue();
        }

        // Duration and ttl are in seconds.  Convert to millis.
        if (duration > 0) {
            duration *= 1000;
        }

        return (duration + host.getIpFetched()) < System.currentTimeMillis();
    }

    /** Get the maximum time a robots.txt is valid.
     *
     * @param curi
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
        // convert from seconds to milliseconds
        return d.longValue() * 1000;
    }

    /**
     * Is the robots policy expired.
     *
     * This method will also return true if we haven't tried to get the
     * robots.txt for this server.
     *
     * @param curi
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

   /**
    * Consider credential preconditions.
    *
    * Looks to see if any credential preconditions (e.g. html form login
    * credentials) for this <code>CrawlServer</code>. If there are, have they
    * been run already? If not, make the running of these logins a precondition
    * of accessing any other url on this <code>CrawlServer</code>.
    *
    * <p>
    * One day, do optimization and avoid running the bulk of the code below.
    * Argument for running the code everytime is that overrides and refinements
    * may change what comes back from credential store.
    *
    * @param curi CrawlURI we're checking for any required preconditions.
    * @return True, if this <code>curi</code> has a precondition that needs to
    *         be met before we can proceed. False if we can precede to process
    *         this url.
    */
    private boolean credentialPrecondition(final CrawlURI curi) {

        boolean result = false;

        CredentialStore cs =
            CredentialStore.getCredentialStore(getSettingsHandler());
        if (cs == null) {
            logger.severe("No credential store for " + curi);
            return result;
        }

        Iterator i = cs.iterator(curi);
        if (i == null) {
            return result;
        }

        while (i.hasNext()) {
            Credential c = (Credential)i.next();

            if (c.isPrerequisite(curi)) {
                // This credential has a prereq. and this curi is it.  Let it
                // through.  Add its avatar to the curi as a mark.  Also, does
                // this curi need to be posted?  Note, we do this test for
                // is it a prereq BEFORE we do the check that curi is of the
                // credential domain because such as yahoo have you go to
                // another domain altogether to login.
                c.attach(curi);
                curi.setPost(c.isPost(curi));
                break;
            }

            if (!c.rootUriMatch(curi)) {
                continue;
            }

            if (!c.hasPrerequisite(curi)) {
                continue;
            }

            if (!authenticated(c, curi)) {
                // Han't been authenticated.  Queue it and move on (Assumption
                // is that we can do one authentication at a time -- usually one
                // html form).
                String prereq = c.getPrerequisite(curi);
                if (prereq == null || prereq.length() <= 0) {
                    logger.severe(curi.getServer().getName() + " has "
                        + " credential(s) of type " + c + " but prereq"
                        + " is null.");
                } else {
                    curi.markPrerequisite(prereq,
                        getController().getPostprocessorChain());
                    result = true;
                    logger.fine("Queueing prereq " + prereq + " of type " + c +
                        " for " + curi);
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Has passed credential already been authenticated.
     *
     * @param credential Credential to test.
     * @param curi CrawlURI.
     * @return True if already run.
     */
    private boolean authenticated(final Credential credential,
            final CrawlURI curi) {

        boolean result = false;
        if (!curi.getServer().hasCredentialAvatars()) {
            return result;
        }
        Set avatars = curi.getServer().getCredentialAvatars();
        for (Iterator i = avatars.iterator(); i.hasNext();) {
            CredentialAvatar ca = (CredentialAvatar)i.next();
            String key = null;
            try {
                key = credential.getKey(curi);
            } catch (AttributeNotFoundException e) {
                logger.severe("Failed getting key for " + credential +
                    " for " + curi);
                continue;
            }
            if (ca.match(credential.getClass(), key)) {
                result = true;
            }
        }
        return result;
    }
}
