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
 * CrawlServer.java
 * Created on Apr 17, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.Checksum;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.framework.Checkpointer;
import org.archive.crawler.framework.ToeThread;
import org.archive.io.ReplayInputStream;
import org.archive.net.UURIFactory;
import org.archive.processors.credential.CredentialAvatar;
import org.archive.settings.SheetManager;

/**
 * Represents a single remote "server".
 *
 * A server is a service on a host. There might be more than one service on a
 * host differentiated by a port number.
 *
 * @author gojomo
 */
public class CrawlServer implements Serializable, CrawlSubstats.HasCrawlSubstats {

    private static final long serialVersionUID = -989714570750970369L;

    public static final long ROBOTS_NOT_FETCHED = -1;
    /** only check if robots-fetch is perhaps superfluous 
     * after this many tries */
    public static final long MIN_ROBOTS_RETRIES = 2;

    private final String server; // actually, host+port in the https case
    private int port;
    private transient SheetManager sheetManager;
    private RobotsExclusionPolicy robots;
    long robotsFetched = ROBOTS_NOT_FETCHED;
    boolean validRobots = false;
    Checksum robotstxtChecksum;
    CrawlSubstats substats = new CrawlSubstats();
    
    // how many consecutive connection errors have been encountered;
    // used to drive exponentially increasing retry timeout or decision
    // to 'freeze' entire class (queue) of URIs
    protected int consecutiveConnectionErrors = 0;

    /**
     * Set of credential avatars.
     */
    private transient Set<CredentialAvatar> avatars =  null;

    /**
     * Creates a new CrawlServer object.
     *
     * @param h the host string for the server.
     */
    public CrawlServer(String h) {
        // TODO: possibly check for illegal host string
        server = h;
        int colonIndex = server.lastIndexOf(":");
        if (colonIndex < 0) {
            port = -1;
        } else {
            try {
                port = Integer.parseInt(server.substring(colonIndex + 1));
            } catch (NumberFormatException e) {
                port = -1;
            }
        }
    }

    /** Get the robots exclusion policy for this server.
     *
     * @return the robots exclusion policy for this server.
     */
    public RobotsExclusionPolicy getRobots() {
        return robots;
    }

    /** Set the robots exclusion policy for this server.
     *
     * @param policy the policy to set.
     */
    public void setRobots(RobotsExclusionPolicy policy) {
        robots = policy;
    }

    public String toString() {
        return "CrawlServer("+server+")";
    }

    /** Update the robots exclusion policy.
     *
     * @param curi the crawl URI containing the fetched robots.txt
     * @throws IOException
     */
    public void updateRobots(CrawlURI curi) {
        CrawlOrder order = (CrawlOrder)sheetManager.getRoot("order");
        
        RobotsHonoringPolicy honoringPolicy = order.getRobotsHonoringPolicy();

        robotsFetched = System.currentTimeMillis();

        boolean gotSomething = curi.getFetchStatus() > 0
                && curi.isHttpTransaction();
        if (!gotSomething && curi.getFetchAttempts() < MIN_ROBOTS_RETRIES) {
            // robots.txt lookup failed, no reason to consider IGNORE yet
            validRobots = false;
            return;
        }
        
        RobotsHonoringPolicy.Type type = honoringPolicy.getType(curi);
        if (type == RobotsHonoringPolicy.Type.IGNORE) {
            // IGNORE = ALLOWALL
            robots = RobotsExclusionPolicy.ALLOWALL;
            validRobots = true;
            return;
        }
        
        if(!gotSomething) {
            // robots.txt lookup failed and policy not IGNORE
            validRobots = false;
            return;
        }
        
        if (!curi.is2XXSuccess()) {
            // Not found or anything but a status code in the 2xx range is
            // treated as giving access to all of a sites' content.
            // This is the prevailing practice of Google, since 4xx
            // responses on robots.txt are usually indicative of a 
            // misconfiguration or blanket-block, not an intentional
            // indicator of partial blocking. 
            // TODO: consider handling server errors, redirects differently
            robots = RobotsExclusionPolicy.ALLOWALL;
            validRobots = true;
            return;
        }

        ReplayInputStream contentBodyStream = null;
        try {
            try {
                BufferedReader reader;
                if (type == RobotsHonoringPolicy.Type.CUSTOM) {
                    reader = new BufferedReader(new StringReader(honoringPolicy
                            .getCustomRobots(curi)));
                } else {
                    contentBodyStream = curi.getHttpRecorder()
                            .getRecordedInput().getContentReplayInputStream();

                    contentBodyStream.setToResponseBodyStart();
                    reader = new BufferedReader(new InputStreamReader(
                            contentBodyStream));
                }
                robots = RobotsExclusionPolicy.policyFor(curi,
                        reader, honoringPolicy);
                validRobots = true;
            } finally {
                if (contentBodyStream != null) {
                    contentBodyStream.close();
                }
            }
        } catch (IOException e) {
            robots = RobotsExclusionPolicy.ALLOWALL;
            validRobots = true;
            curi.addLocalizedError(getName(), e,
                    "robots.txt parsing IOException");
        }
    }

    /**
     * @return Returns the time when robots.txt was fetched.
     */
    public long getRobotsFetchedTime() {
        return robotsFetched;
    }

    /**
     * @return The server string which might include a port number.
     */
    public String getName() {
       return server;
    }

    /** Get the port number for this server.
     *
     * @return the port number or -1 if not known (uses default for protocol)
     */
    public int getPort() {
        return port;
    }

    /** 
     * Called when object is being deserialized.
     * In addition to the default java deserialization, this method
     * re-establishes the references to settings handler and robots honoring
     * policy.
     *
     * @param stream the stream to deserialize from.
     * @throws IOException if I/O errors occur
     * @throws ClassNotFoundException If the class for an object being restored
     *         cannot be found.
     */
    private void readObject(ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        Thread t = Thread.currentThread();
        if (t instanceof Checkpointer.CheckpointingThread) {
            sheetManager = ((Checkpointer.CheckpointingThread)t)
        		.getController().getSheetManager();
        } else if (t instanceof ToeThread) {
            sheetManager = ((ToeThread) Thread.currentThread())
                .getController().getSheetManager();
        } else {
            // TODO: log differently? (if no throw here
            // NPE is inevitable)
            throw new RuntimeException("CrawlServer must deserialize " +
                    "in a ToeThread or CheckpointingThread");
        }
        postDeserialize();
    }
    
    private void postDeserialize() {
    	if (this.robots != null) {
            CrawlOrder order = (CrawlOrder)sheetManager.getRoot("order");
            RobotsHonoringPolicy honoringPolicy =
                order.getRobotsHonoringPolicy();
            this.robots.honoringPolicy = honoringPolicy;
    	}
    }

    /** Get the sheet manager.
     *
     * @return the settings handler.
     */
    public SheetManager getSheetManager() {
        return this.sheetManager;
    }

    /** Get the settings object in effect for this server.
     * @param curi
     *
     * @return the settings object in effect for this server.
     * @throws URIException
     */
/*    private CrawlerSettings getSettings(CandidateURI curi) {
        try {
            return this.settingsHandler.
                getSettings(curi.getUURI().getReferencedHost(),
                    curi.getUURI());
        } catch (URIException e) {
            return null;
        }
    } */

    /** Set the settings handler to be used by this server.
     *
     * @param settingsHandler the settings handler to be used by this server.
     */
    public void setSheetManager(SheetManager manager) {
        this.sheetManager = manager;
    }

    public void incrementConsecutiveConnectionErrors() {
        this.consecutiveConnectionErrors++;
    }

    public void resetConsecutiveConnectionErrors() {
        this.consecutiveConnectionErrors = 0;
    }

    /**
     * @return Credential avatars for this server.  Returns null if none.
     */
    public Set getCredentialAvatars() {
        return this.avatars;
    }

    /**
     * @return True if there are avatars attached to this instance.
     */
    public boolean hasCredentialAvatars() {
        return this.avatars != null && this.avatars.size() > 0;
    }

    /**
     * Add an avatar.
     *
     * @param ca Credential avatar to add to set of avatars.
     */
    public void addCredentialAvatar(CredentialAvatar ca) {
        if (this.avatars == null) {
            this.avatars = new HashSet<CredentialAvatar>();
        }
        this.avatars.add(ca);
    }
    
	/**
     * If true then valid robots.txt information has been retrieved. If false
     * either no attempt has been made to fetch robots.txt or the attempt
     * failed.
     *
	 * @return Returns the validRobots.
	 */
	public boolean isValidRobots() {
		return validRobots;
	}
    
    /**
     * Get key to use doing lookup on server instances.
     * @param cauri CandidateURI we're to get server key for.
     * @return String to use as server key.
     * @throws URIException
     */
	public static String getServerKey(CandidateURI cauri)
	throws URIException {
	    // TODO: evaluate if this is really necessary -- why not 
	    // make the server of a dns CandidateURI the looked-up domain,
	    // also simplifying FetchDNS?
	    String key = cauri.getUURI().getAuthorityMinusUserinfo();
	    if (key == null) {
	        // Fallback for cases where getAuthority() fails (eg 'dns:'.
	        // DNS UURIs have the 'domain' in the 'path' parameter, not
	        // in the authority).
	        key = cauri.getUURI().getCurrentHierPath();
	        if(key != null && !key.matches("[-_\\w\\.:]+")) {
	            // Not just word chars and dots and colons and dashes and
	            // underscores; throw away
	            key = null;
	        }
	    }
	    if (key != null &&
	            cauri.getUURI().getScheme().equals(UURIFactory.HTTPS)) {
	        // If https and no port specified, add default https port to
	        // distinuish https from http server without a port.
	        if (!key.matches(".+:[0-9]+")) {
	            key += ":" + UURIFactory.HTTPS_PORT;
	        }
	    }
	    return key;
	}

    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.CrawlSubstats.HasCrawlSubstats#getSubstats()
     */
    public CrawlSubstats getSubstats() {
        return substats;
    }
}
