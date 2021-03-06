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
 * RobotsHonoringPolicy.java
 * Created on Oct 30, 2003
 *
 * $Header$
 */
package org.archive.modules.net;

import java.io.Serializable;
import java.util.List;

import org.archive.state.Key;
import org.archive.state.KeyManager;
import org.archive.state.StateProvider;

/**
 * RobotsHonoringPolicy represent the strategy used by the crawler 
 * for determining how robots.txt files will be honored. 
 *
 * Five kinds of policies exist:
 * <dl>
 * <dt>classic:</dt>
 *   <dd>obey the first set of robots.txt directives that apply to your 
 *   current user-agent</dd>
 * <dt>ignore:</dt>
 *   <dd>ignore robots.txt directives entirely</dd>
 * <dt>custom:</dt>
 *   <dd>obey a specific operator-entered set of robots.txt directives 
 *   for a given host</dd>
 * <dt>most-favored:</dt>
 *   <dd>obey the most liberal restrictions offered (if *any* crawler is 
 *   allowed to get a page, get it)</dd>
 * <dt>most-favored-set:</dt>
 *   <dd>given some set of user-agent patterns, obey the most liberal 
 *   restriction offered to any</dd>
 * </dl>
 *
 * The two last ones has the opportunity of adopting a different user-agent 
 * to reflect the restrictions we've opted to use.
 *
 * @author John Erik Halse
 *
 */
public class RobotsHonoringPolicy implements Serializable {

    private static final long serialVersionUID = 3L;

    
    /**
     * Policy type.
     */
    public static enum Type { 
        
        /** Obeys all robts.txt rules for the configured user-agent. */
        CLASSIC, 
        
        /** Ignores all robots rules. */
        IGNORE, 
        
        /** Defers to custom-robots setting. */
        CUSTOM, 
        
        /** Crawls URIs if the robots.txt allows any user-agent to crawl it. */
        MOST_FAVORED, 
        
        /**
         * Requires you to supply an list of alternate user-agents, and for
         * every page, if any agent of the set is allowed, the page will be
         * crawled.
         */
        MOST_FAVORED_SET 
    }

/*
    public final static int CLASSIC = 0;
    public final static int IGNORE = 1;
    public final static int CUSTOM = 2;
    public final static int MOST_FAVORED = 3;
    public final static int MOST_FAVORED_SET = 4;
*/

    /**
     * Policy type. The 'classic' policy simply obeys all robots.txt rules for
     * the configured user-agent. The 'ignore' policy ignores all robots rules.
     * The 'custom' policy allows you to specify a policy, in robots.txt format,
     * as a setting. The 'most-favored' policy will crawl an URL if the
     * robots.txt allows any user-agent to crawl it. The 'most-favored-set'
     * policy requires you to supply an list of alternate user-agents, and for
     * every page, if any agent of the set is allowed, the page will be crawled.
     */
    public final static Key<Type> TYPE = Key.make(Type.CLASSIC);

    
    /**
     * Should we masquerade as another user agent when obeying the rules
     * declared for it. Only relevant if the policy type is 'most-favored' or
     * 'most-favored-set'.
     */
    public final static Key<Boolean> MASQUERADE = Key.make(false);


    /**
     * Custom robots to use if policy type is 'custom'. Compose as if an actual
     * robots.txt file.
     */
    public final static Key<String> CUSTOM_ROBOTS = Key.make("");

    
    /**
     * Alternate user-agent values to consider using for the 'most-favored-set'
     * policy.
     */
    public final static Key<List<String>> USER_AGENTS = Key.makeList(String.class);


    /**
     * Creates a new instance of RobotsHonoringPolicy.
     */
    public RobotsHonoringPolicy() {
    }


    /**
     * If policy-type is most favored crawler of set, then this method
     * gets a list of all useragents in that set.
     *
     * @return List of Strings with user agents
     */
    public List<String> getUserAgents(StateProvider context) {
        if (isType(context,Type.MOST_FAVORED_SET)) {
            return context.get(this, USER_AGENTS);
        }
        return null;
    }

    /**
     * This method returns true if the crawler should masquerade as the user agent
     * which restrictions it opted to use.
     *
     * (Only relevant for  policy-types: most-favored and most-favored-set).
     *
     * @return true if we should masquerade
     */
    public boolean shouldMasquerade(StateProvider context) {
        return context.get(this, MASQUERADE);
    }

    /**
     * Get the supplied custom robots.txt
     *
     * @return String with content of alternate robots.txt
     */
    public String getCustomRobots(StateProvider context) {
        if (isType(context, Type.CUSTOM)) {
            return context.get(this, CUSTOM_ROBOTS);
        }
        return null;
    }


    /**
     * Get the policy-type.
     *
     * @return policy type
     */
    public Type getType(StateProvider context) {
        return context.get(this, TYPE);
    }

    /**
     * Check if policy is of a certain type.
     *
     * @param context   An object that can be resolved into a settings object.
     * @param type      the type to check against.
     * @return true     if the policy is of the submitted type
     */
    public boolean isType(StateProvider context, Type type) {
        return type == getType(context);
    }

    // good to keep at end of source: must run after all per-Key 
    // initialization values are set.
    static {
        KeyManager.addKeys(RobotsHonoringPolicy.class);
    }
}
