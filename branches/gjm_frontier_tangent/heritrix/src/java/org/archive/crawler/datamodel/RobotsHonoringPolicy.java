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
package org.archive.crawler.datamodel;

import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;
import org.archive.crawler.datamodel.settings.CrawlerModule;
import org.archive.crawler.datamodel.settings.CrawlerSettings;
import org.archive.crawler.datamodel.settings.SimpleType;
import org.archive.crawler.datamodel.settings.StringList;

/**
 * This class represent the policy to which Robots.txt files is
 * to honored.
 *
 * Five kinds of policies exist:
 * <dl>
 * <dt>classic:</dt>
 *   <dd>obey the first set of robots.txt directives that apply to your current user-agent</dd>
 * <dt>ignore:</dt>
 *   <dd>ignore robots.txt directives entirely</dd>
 * <dt>custom:</dt>
 *   <dd>obey a specific operator-entered set of robots.txt directives for a given host</dd>
 * <dt>most-favored:</dt>
 *   <dd>obey the most liberal restrictions offered (if *any* crawler is allowed to get a page, get it)</dd>
 * <dt>most-favored-set:</dt>
 *   <dd>given some set of user-agent patterns, obey the most liberal restriction offered to any</dd>
 * </dl>
 *
 * The two last ones has the opportunity of adopting a different user-agent to reflect the restrictions we've opted to use.
 *
 * @author John Erik Halse
 *
 */
public class RobotsHonoringPolicy  extends CrawlerModule {
    private static Logger logger =
        Logger.getLogger("org.archive.crawler.datamodel.RobotsHonoringPolicy");

    public final static int CLASSIC = 0;
    public final static int IGNORE = 1;
    public final static int CUSTOM = 2;
    public final static int MOST_FAVORED = 3;
    public final static int MOST_FAVORED_SET = 4;
    public final static Integer DEFAULT_ROBOTS_VALIDITY_DURATION =
        new Integer(3*(60*24)); // three days

    public final static String ATTR_NAME = "robots-honoring-policy";
    public final static String ATTR_TYPE = "type";
    public final static String ATTR_MASQUERADE = "masquerade";
    public final static String ATTR_CUSTOM_ROBOTS = "custom-robots";
    public final static String ATTR_USER_AGENTS = "user-agents";
    public final static String ATTR_ROBOTS_VALIDITY_DURATION = "robot-validity-duration-m";
    

    /**
     * Creates a new instance of RobotsHonoringPolicy.
     *
     * @param name the name of the RobotsHonoringPolicy attirubte.
     */
    public RobotsHonoringPolicy(String name) {
        super(name, "Robots honoring policy");

        String[] allowedTypes = new String[] {"classic", "ignore", "custom", "most-favored", "most-favored-set"};

        addElementToDefinition(new SimpleType(ATTR_TYPE,
                "Policy type", "classic", allowedTypes));
        addElementToDefinition(new SimpleType(ATTR_MASQUERADE,
                "Should we masquerade as another user agent", new Boolean(false)));
        addElementToDefinition(new SimpleType(ATTR_CUSTOM_ROBOTS,
                "Custom robots to use if type is custom", ""));
        addElementToDefinition(new StringList(ATTR_USER_AGENTS, "User agents"));
        addElementToDefinition(new SimpleType(ATTR_ROBOTS_VALIDITY_DURATION,
                "The time in minutes, between robots.txt are refreshed.",
                DEFAULT_ROBOTS_VALIDITY_DURATION));
    }

    public RobotsHonoringPolicy() {
        this(ATTR_NAME);
    }

    /**
     * If policy-type is most favored crawler of set, then this method
     * gets a list of all useragents in that set.
     *
     * @return List of Strings with user agents
     */
    public StringList getUserAgents(CrawlerSettings settings) {
        if (isType(settings, RobotsHonoringPolicy.MOST_FAVORED_SET)) {
            try {
                return (StringList) getAttribute(settings, ATTR_USER_AGENTS);
            } catch (AttributeNotFoundException e) {
                logger.severe(e.getMessage());
            }
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
    public boolean shouldMasquerade(CrawlURI curi) {
        try {
            return ((Boolean) getAttribute(
                getSettingsFromObject(curi), ATTR_MASQUERADE)).booleanValue();
        } catch (AttributeNotFoundException e) {
            logger.severe(e.getMessage());
            return false;
        }
    }

    /**
     * Get the supplied custom robots.txt
     *
     * @return String with content of alternate robots.txt
     */
    public String getCustomRobots(CrawlerSettings settings) {
        if(isType(settings, RobotsHonoringPolicy.CUSTOM)) {
            try {
                return (String) getAttribute(settings, ATTR_CUSTOM_ROBOTS);
            } catch (AttributeNotFoundException e) {
                logger.severe(e.getMessage());
            }
        }
        return null;
    }

    /**
     * Get the policy-type.
     *
     * @see #CLASSIC
     * @see #IGNORE
     * @see #CUSTOM
     * @see #MOST_FAVORED
     * @see #MOST_FAVORED_SET
     *
     * @return policy type
     */
    public int getType(CrawlerSettings settings) {
        int type = CLASSIC;
        try {
            String typeName = (String) getAttribute(settings, "type");
            if(typeName.equals("classic")) {
                type = RobotsHonoringPolicy.CLASSIC;
            } else if(typeName.equals("ignore")) {
                type = RobotsHonoringPolicy.IGNORE;
            } else if(typeName.equals("custom")) {
                type = RobotsHonoringPolicy.CUSTOM;
            } else if(typeName.equals("most-favored")) {
                type = RobotsHonoringPolicy.MOST_FAVORED;
            } else if(typeName.equals("most-favored-set")) {
                type = RobotsHonoringPolicy.MOST_FAVORED_SET;
            } else {
                throw new IllegalArgumentException();
            }
        } catch (AttributeNotFoundException e) {
            logger.severe(e.getMessage());
        }
        return type;
    }

    /**
     * Check if policy is of a certain type.
     *
     * @param An object that can be resolved into a settings object.
     * @param type the type to check against.
     * @return true if the policy is of the submitted type
     */
    public boolean isType(Object o, int type) {
        return type == getType(getSettingsFromObject(o));
    }

    /** Get the maximum time a robots.txt is valid.
     * 
     * @param curi the Crawl URI this time is valid for.
     * @return the time a robots.txt is valid in milliseconds.
     */
    public long getRobotsValidityDuration(CrawlerSettings settings) {
        Integer d;
        try {
            d = (Integer) getAttribute(settings, ATTR_ROBOTS_VALIDITY_DURATION);
        } catch (AttributeNotFoundException e) {
            logger.severe(e.getLocalizedMessage());
            d = DEFAULT_ROBOTS_VALIDITY_DURATION;
        }
        return d.longValue() * 1000 * 60;
    }
}
