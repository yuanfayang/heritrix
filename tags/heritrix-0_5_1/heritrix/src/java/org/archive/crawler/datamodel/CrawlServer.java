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
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.io.StringReader;
import java.util.zip.Checksum;

import org.archive.crawler.datamodel.settings.CrawlerSettings;
import org.archive.crawler.datamodel.settings.SettingsHandler;
import org.archive.io.ReplayInputStream;

/**
 * Represents a single remote "server".
 *
 * A server is a service on a host. There might be more than one service on a
 * host differentiated by a port number.
 *
 * @author gojomo
 */
public class CrawlServer implements Serializable {
    public static final long ROBOTS_NOT_FETCHED = -1;

    private final String server; // actually, host+port in the http case
    private int port;
    private CrawlHost host;
    private SettingsHandler settingsHandler;
    RobotsExclusionPolicy robots;
    long robotsFetched = ROBOTS_NOT_FETCHED;
    Checksum robotstxtChecksum;

    /** Creates a new CrawlServer object.
     * 
     * @param h the host string for the server.
     */
    public CrawlServer(String h) {
        // TODO: possibly check for illegal host string
        server = h;
        int colonIndex = server.lastIndexOf(":");
        if(colonIndex < 0) {
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

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "CrawlServer("+server+")";
    }

    /** Update the robots exclusion policy.
     * 
     * @param curi the crawl URI containing the fetched robots.txt
     * @throws IOException
     */
    public void updateRobots(CrawlURI curi)
            throws IOException {

        RobotsHonoringPolicy honoringPolicy =
            settingsHandler.getOrder().getRobotsHonoringPolicy();

        robotsFetched = System.currentTimeMillis();
        if (curi.getFetchStatus() != 200 ||
                honoringPolicy.getType(getSettings()) ==
                    RobotsHonoringPolicy.IGNORE)
        {
            // not found or other errors == all ok for now
            // TODO: consider handling server errors, redirects differently
            robots = RobotsExclusionPolicy.ALLOWALL;
            return;
        }
//    PREVAILING PRACTICE PER GOOGLE: treat these errors as all-allowed,
//  since they're usually indicative of a mistake
// Thus these lines are commented out:
//      if ((get.getStatusCode() >= 401) && (get.getStatusCode() <= 403)) {
//            // authorization/allowed errors = all deny
//            robots = RobotsExclusionPolicy.DENYALL;
//            return;
//        }
        ReplayInputStream contentBodyStream = null;
        try {
            BufferedReader reader;
            if (honoringPolicy.getType(getSettings())
                == RobotsHonoringPolicy.CUSTOM) {
                reader =
                    new BufferedReader(
                        new StringReader(
                            honoringPolicy.getCustomRobots(getSettings())));
            }
            else
            {
                contentBodyStream = curi.getHttpRecorder().getRecordedInput()
                    .getContentReplayInputStream();

                contentBodyStream.setToResponseBodyStart();
                reader = new BufferedReader(
                    new InputStreamReader(contentBodyStream));
            }
            robots = RobotsExclusionPolicy.policyFor(
                    getSettings(),
                    reader,
                    honoringPolicy);

        } catch (IOException e) {
            robots = RobotsExclusionPolicy.ALLOWALL;
            throw e; // rethrow
        } finally {
            if (contentBodyStream != null) {
                contentBodyStream.close();
            }
        }
        return;
    }

    /**
     * @return Returns the time when robots.txt was fetched.
     */
    public long getRobotsFetchedTime() {
        return robotsFetched;
    }

    /** Get the server string which might include a port number.
     *
     * @return the server string.
     */
    public String getServer(){
       return server;
    }

    /**
     * Get the associated CrawlHost
     *
     * @return host
     */
    public CrawlHost getHost() {
        return host;
    }

    /** Set the CrawlHost for which this server is a service.
     *
     * @param host the CrawlHost.
     */
    public void setHost(CrawlHost host) {
        this.host = host;
    }

    /** Get the hostname for this server.
     *
     * @return the hostname without any port numbers.
     */
    public String getHostname() {
        int colonIndex = server.indexOf(":");
        if(colonIndex < 0) {
            return server;
        }
        return server.substring(0,colonIndex);
    }

    /** Get the port number for this server.
     *
     * @return the port number or -1 if not known (uses default for protocol)
     */
    public int getPort() {
        return port;
    }

    /**
     * Refuse to be serialized, but do not halt serialization:
     * replace with null.
     *
     * @return Null.
     * @throws ObjectStreamException
     */
    protected Object writeReplace() throws ObjectStreamException {
        return null;
    }

//    private void writeObject(ObjectOutputStream stream)
//     throws IOException {
//         ObjectOutputStream.PutField puts = stream.putFields();
//         puts.put("server",server);
//         stream.writeFields();
//     }
//
//    private void readObject(ObjectInputStream stream)
//     throws IOException, ClassNotFoundException {
//         ObjectInputStream.GetField reads = stream.readFields();
//         server = (String)reads.get("server",null);
//     }

    /** Get the settings handler.
     *
     * @return the settings handler.
     */
    public SettingsHandler getSettingsHandler() {
        return settingsHandler;
    }

    /** Get the settings object in effect for this server.
     *
     * @return the settings object in effect for this server.
     */
    public CrawlerSettings getSettings() {
        return settingsHandler.getSettings(getHost().name, getPort());
    }

    /** Set the settings handler to be used by this server.
     *
     * @param settingsHandler the settings handler to be used by this server.
     */
    public void setSettingsHandler(SettingsHandler settingsHandler) {
        this.settingsHandler = settingsHandler;
    }
}
