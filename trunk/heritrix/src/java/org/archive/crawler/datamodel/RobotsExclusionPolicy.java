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
 * RobotsExclusionPolicy.java
 * Created on Apr 17, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.settings.CrawlerSettings;

/**
 * expiry handled outside, in CrawlServer
 * @author gojomo
 *
 */
public class RobotsExclusionPolicy implements Serializable {
    
    private static final Logger logger =
        Logger.getLogger(RobotsExclusionPolicy.class.getName());

    private final static int NORMAL_TYPE = 0;
    private final static int ALLOWALL_TYPE = 1;
    private final static int DENYALL_TYPE = 2;
    private transient int type = NORMAL_TYPE;

    public static RobotsExclusionPolicy ALLOWALL =
        new RobotsExclusionPolicy(ALLOWALL_TYPE);
    public static RobotsExclusionPolicy DENYALL =
        new RobotsExclusionPolicy(DENYALL_TYPE);

    private LinkedList userAgents = null;
    private HashMap disallows = null; // of (String -> List)
    transient RobotsHonoringPolicy honoringPolicy = null;

    private String lastUsedUserAgent = null;
    private List userAgentsToTest = null;

    /**
     * @param reader
     * @param honoringPolicy
     * @return Robot exclusion policy.
     * @throws IOException
     */
    public static RobotsExclusionPolicy policyFor(CrawlerSettings settings,
            BufferedReader reader, RobotsHonoringPolicy honoringPolicy)
            throws IOException {

        String read;
        ArrayList current = null;
        LinkedList userAgents = new LinkedList();
        HashMap disallows = new HashMap();
         boolean hasErrors = false;
         String catchall = null;
        while (reader != null) {
            do {read = reader.readLine();}
            while (
              (read != null)
              && ( (read=read.trim()).startsWith("#")
                   || read.length() == 0) ); // skip comments & blanks

            if (read == null) {
                reader.close();
                reader = null;
            } else {
                int commentIndex = read.indexOf("#");
                if( commentIndex >-1 ) {
                    // strip trailing comment
                    read = read.substring(0, commentIndex);
                }
                read = read.trim();
                if (read.matches("(?i)^User-agent:.*")) {
                    String ua = read.substring(11).trim().toLowerCase();
                    if(current==null||current.size()!=0) {
                        // only create new rules-list if necessary
                        // otherwise share with previous user-agent
                        current = new ArrayList();
                    }
                    if(ua.equals("*")) {
                        ua = "";
                        catchall = ua;
                    } else {
                        userAgents.addLast(ua);
                    }
                    disallows.put(ua,current);
                    continue;
                }
                if (read.matches("(?i)Disallow:.*")) {
                    if(current==null) {
                        // buggy robots.txt
                        hasErrors = true;
                        continue;
                    }
                    String path = read.substring(9).trim();
                    current.add(path);
                    continue;
                }
                //unknown line; do nothing for now
            }
        }

        if (catchall!=null) {
            userAgents.addLast(catchall);
        }

        if(disallows.isEmpty()) return ALLOWALL;
        return new RobotsExclusionPolicy(
            settings, userAgents, disallows, hasErrors, honoringPolicy);
    }


    /**
     * @param u
     * @param d
     * @param errs
     * @param honoringPolicy
     */
    public RobotsExclusionPolicy(CrawlerSettings settings, LinkedList u,
            HashMap d, boolean errs, RobotsHonoringPolicy honoringPolicy) {
        userAgents = u;
        disallows = d;
        this.honoringPolicy = honoringPolicy;

        if(honoringPolicy == null) return;

        // If honoring policy is most favored user agent, all rules should be checked
        if(honoringPolicy.isType(settings, RobotsHonoringPolicy.MOST_FAVORED)) {
            userAgentsToTest = userAgents;

        // IF honoring policy is most favored of set, then make a list with only the set as members
        } else if(honoringPolicy.isType(settings, RobotsHonoringPolicy.MOST_FAVORED_SET)) {
            userAgentsToTest = new ArrayList();
            Iterator userAgentSet = honoringPolicy.getUserAgents(settings).iterator();
            while(userAgentSet.hasNext()) {
                String userAgent = (String) userAgentSet.next();

                Iterator iter = userAgents.iterator();
                while ( iter.hasNext() ) {
                    String ua = (String)iter.next();
                    if (userAgent.indexOf(ua)>-1) {
                        userAgentsToTest.add(ua);
                        break;
                    }
                }
            }
        }
    }
    
    public RobotsExclusionPolicy(int type) {
        this(null, null, null, false, null);
        this.type = type;
    }

    public boolean disallows(CrawlURI curi, String userAgent) {
        if (this == ALLOWALL)
            return false;
        if (this == DENYALL)
            return true;

        // In the common case with policy=Classic, the useragent is remembered from uri to uri on
        // the same server
        if(honoringPolicy.isType(curi, RobotsHonoringPolicy.CLASSIC)
            && (lastUsedUserAgent == null
            || !lastUsedUserAgent.equals(userAgent))) {

            lastUsedUserAgent = userAgent;
            userAgentsToTest = new ArrayList();
            Iterator iter = userAgents.iterator();
            while ( iter.hasNext() ) {
                String ua = (String)iter.next();
                if (userAgent.indexOf(ua)>-1) {
                    userAgentsToTest.add(ua);
                    break; // consider no more sections
                }
            }
        }

        boolean disallow = false;
        boolean examined = false;
        String ua = null;

        // Go thru list of all user agents we might act as
        Iterator uas = userAgentsToTest.iterator();
        while(uas.hasNext() && examined == false) {
            disallow = false;
            ua = (String) uas.next();
            Iterator dis = ((List) disallows.get(ua)).iterator();

            // Check if the current user agent is allowed to crawl
            while(dis.hasNext() && examined == false && disallow == false) {
                String disallowedPath = (String) dis.next();
                if(disallowedPath.length() == 0) {
                    // blanket allow
                    examined = true;
                    disallow = false;
                    break;
                }
                try {
                    String p = curi.getUURI().getPath();
                    if (p != null && p.startsWith(disallowedPath) ) {
                        // the user agent tested isn't allowed to get this uri
                        disallow = true;
                    }
                }
                catch (URIException e) {
                    logger.severe("Failed getPath from " + curi);
                }
            }
            if(disallow == false) {
                // the user agent tested is allowed
                examined = true;
            }
        }

        // Are we supposed to masquerade as the user agent to which restrictions
        // we follow?
        if(honoringPolicy.shouldMasquerade(curi) && ua != null && !ua.equals("")) {
            curi.setUserAgent(ua);
        }
        return disallow;
    }

    // Methods for object serialization.
    
    /** If object is DENYALL or ALLOWALL, only the object identity and type
     * is written in the serialization stream.
     * 
     * @param stream the serialization stream.
     */
    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.writeInt(type);
        if (type == NORMAL_TYPE) {
            stream.defaultWriteObject();
        }
    }

    /** If object is DENYALL or ALLOWALL, only the object identity and type
     * is read from the serialization stream.
     * 
     * @param stream the serialization stream.
     */
    private void readObject(ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
        type = stream.readInt();
        if (type == NORMAL_TYPE) {
            stream.defaultReadObject();
        }
    }

    /** If object is DENYALL or ALLOWALL, the object is replaced by constants
     * so that check for object equality works.
     */
    private Object readResolve() throws ObjectStreamException {
        if (type == NORMAL_TYPE) {
            return this;
        } else if (type == ALLOWALL_TYPE) {
            return ALLOWALL;
        } else if (type == DENYALL_TYPE) {
            return DENYALL;
        }
        return null;
    }
    
}
