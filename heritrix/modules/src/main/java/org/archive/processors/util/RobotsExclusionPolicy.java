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
package org.archive.processors.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.httpclient.URIException;
import org.archive.processors.ProcessorURI;
import org.archive.state.StateProvider;

/**
 * RobotsExclusionPolicy represents the actual policy adopted with 
 * respect to a specific remote server, usually constructed from 
 * consulting the robots.txt, if any, the server provided. 
 * 
 * (The similarly named RobotsHonoringPolicy, on the other hand, 
 * describes the strategy used by the crawler to determine to what
 * extent it respects exclusion rules.)
 * 
 * The expiration of policies after a suitable amount of time has
 * elapsed since last fetch is handled outside this class, in 
 * CrawlServer itself. 
 * 
 * @author gojomo
 *
 */
public class RobotsExclusionPolicy implements Serializable {

    private static final long serialVersionUID = 3L;

    private static final Logger logger =
        Logger.getLogger(RobotsExclusionPolicy.class.getName());

    
    public static enum Type {
        NORMAL, 
        ALLOWALL, 
        DENYALL
    }

    private transient Type type = Type.NORMAL;

    final public static RobotsExclusionPolicy ALLOWALL =
        new RobotsExclusionPolicy(Type.ALLOWALL);
    final public static RobotsExclusionPolicy DENYALL =
        new RobotsExclusionPolicy(Type.DENYALL);

    private LinkedList<String> userAgents = null;
    private HashMap<String,List<String>> disallows = null;
    RobotsHonoringPolicy honoringPolicy = null; // was transient

    private String lastUsedUserAgent = null;
    private List<String> userAgentsToTest = null;

    /**
     * @param settings 
     * @param reader
     * @param honoringPolicy
     * @return Robot exclusion policy.
     * @throws IOException
     */
    public static RobotsExclusionPolicy policyFor(StateProvider settings,
            BufferedReader reader, RobotsHonoringPolicy honoringPolicy)
    throws IOException {
        LinkedList<String> userAgents = new LinkedList<String>();
        HashMap<String,List<String>> disallows
         = new HashMap<String,List<String>>();
        Robotstxt.parse(reader, userAgents, disallows);
        return (disallows.isEmpty())?
            ALLOWALL:
            new RobotsExclusionPolicy(settings, userAgents, disallows,
                honoringPolicy);
    }


    /**
     * @param settings 
     * @param u
     * @param d
     * @param honoringPolicy
     */
    public RobotsExclusionPolicy(StateProvider context, LinkedList<String> u,
            HashMap<String,List<String>> d, 
            RobotsHonoringPolicy honoringPolicy) {
        userAgents = u;
        disallows = d;
        this.honoringPolicy = honoringPolicy;

        if(honoringPolicy == null) {
            return;
        }

        // If honoring policy is most favored user agent, all rules should be checked
        if(honoringPolicy.isType(context, RobotsHonoringPolicy.Type.MOST_FAVORED)) {
            userAgentsToTest = userAgents;

        // IF honoring policy is most favored of set, then make a list with only the set as members
        } else if(honoringPolicy.isType(context, RobotsHonoringPolicy.Type.MOST_FAVORED_SET)) {
            userAgentsToTest = new ArrayList<String>();
            List<String> userAgentSet = honoringPolicy.getUserAgents(context);
            for (String userAgent: userAgentSet) {
                for (String ua: userAgents) {
                    if (userAgent.indexOf(ua)>-1) {
                        userAgentsToTest.add(ua);
                        break;
                    }
                }
            }
        }
    }

    public RobotsExclusionPolicy(Type type) {
        this(null, null, null, null);
        this.type = type;
    }

    public boolean disallows(ProcessorURI curi, String userAgent) {
        if (this == ALLOWALL)
            return false;
        if (this == DENYALL)
            return true;

        // In the common case with policy=Classic, the useragent is remembered from uri to uri on
        // the same server
        if((honoringPolicy.isType(curi, RobotsHonoringPolicy.Type.CLASSIC) 
                || honoringPolicy.isType(curi, RobotsHonoringPolicy.Type.CUSTOM))
            && (lastUsedUserAgent == null
            || !lastUsedUserAgent.equals(userAgent))) {

            lastUsedUserAgent = userAgent;
            userAgentsToTest = new ArrayList<String>();
            Iterator iter = userAgents.iterator();
            String lowerCaseUserAgent = userAgent.toLowerCase();
            while ( iter.hasNext() ) {
                String ua = (String)iter.next();
                // ua in below is already lowercase. See Robotstxt.java line 60.
                if (lowerCaseUserAgent.indexOf(ua)>-1) {
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
                    String p = curi.getUURI().getPathQuery();
                    if (p != null && p.startsWith(disallowedPath) ) {
                        // the user agent tested isn't allowed to get this uri
                        disallow = true;
                    }
                }
                catch (URIException e) {
                    logger.log(Level.SEVERE,"Failed getPathQuery from " + curi, e);
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
     * @throws IOException 
     */
    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.writeInt(type.ordinal());
        if (type == Type.NORMAL) {
            stream.defaultWriteObject();
        }
    }

    /** If object is DENYALL or ALLOWALL, only the object identity and type
     * is read from the serialization stream.
     *
     * @param stream the serialization stream.
     * @throws IOException 
     * @throws ClassNotFoundException 
     */
    private void readObject(ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
        int ordinal = stream.readInt();
        type = Type.values()[ordinal];
        if (type == Type.NORMAL) {
            stream.defaultReadObject();
        }
    }

    /** If object is DENYALL or ALLOWALL, the object is replaced by constants
     * so that check for object equality works.
     * @return Object.
     */
    private Object readResolve() {
        if (type == Type.NORMAL) {
            return this;
        } else if (type == Type.ALLOWALL) {
            return ALLOWALL;
        } else if (type == Type.DENYALL) {
            return DENYALL;
        }
        return null;
    }

}
