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
	
	//private String name;
	private int type;
	private String customRobots;
	private StringList userAgents;
	private boolean shouldMasquerade = false;
	
    static final String ATTR_NAME = "robots-honoring-policy";
    static final String ATTR_TYPE = "type";
    static final String ATTR_MASQUERADE = "masquerade";
    static final String ATTR_CUSTOM_ROBOTS = "custom-robots";
    static final String ATTR_USER_AGENTS = "user-agents";

	/**
	 * Creates a new instance of RobotsHonoringPolicy.
	 * 
	 * @param name the name of the RobotsHonoringPolicy attirubte.
	 */
	public RobotsHonoringPolicy(String name) {
        super(name, "Robots honoring policy");

        String[] allowedTypes = new String[] {"classic", "ignore", "custom", "most-favored", "most-favored-set"};
        
        addElementToDefinition(new SimpleType(ATTR_TYPE, "Policy type", "classic", allowedTypes));
        addElementToDefinition(new SimpleType(ATTR_MASQUERADE, "Should we masquerade as another user agent", new Boolean(false)));
        addElementToDefinition(new SimpleType(ATTR_CUSTOM_ROBOTS, "Custom robots to use if type is custom", ""));
        addElementToDefinition(new StringList(ATTR_USER_AGENTS, "User agents"));
	}
    
    public RobotsHonoringPolicy() {
        this(ATTR_NAME);
    }
	
	/**
	 * If policy-type is most favored crawler of set, then this method is
	 * used to add crawlers to that set.
	 * 
	 * @param userAgent Name of user agent to add
	 */
	public void addUserAgent(String userAgent) {
        /*
		if(userAgents == null) {
			userAgents = new ArrayList();
		}
        */
		userAgents.add(userAgent);
	}
	
	/**
	 * If policy-type is most favored crawler of set, then this method
	 * gets a list of all useragents in that set.
	 * 
	 * @return List of Strings with user agents
	 */
	public StringList getUserAgents() {
		return userAgents;
	}
	
	/**
	 * Set this to true if the crawler should masquerade as the user agent
	 * which restrictions it opted to use.
	 * 
	 * (Only relevant for  policy-types: most-favored and most-favored-set).
	 * 
	 * @param m the string "true" or "1" if we should masquerade
	 */
	public void setMasquerade(String m) {
		if(m != null && (m.equalsIgnoreCase("true") || m.equals("1"))) {
			setMasquerade(true);
		} else {
			setMasquerade(false);
		}
	}
	
	/**
	 * Set this to true if the crawler should masquerade as the user agent
	 * which restrictions it opted to use.
	 * 
	 * (Only relevant for  policy-types: most-favored and most-favored-set).
	 * 
	 * @param m true if we should masquerade
	 */
	public void setMasquerade(boolean m) {
		shouldMasquerade = m;
	}
	
	/**
	 * This method returns true if the crawler should masquerade as the user agent
	 * which restrictions it opted to use.
	 * 
	 * (Only relevant for  policy-types: most-favored and most-favored-set).
	 * 
	 * @return true if we should masquerade
	 */
	public boolean shouldMasquerade() {
		return shouldMasquerade;
	}
	
	/**
	 * If policy-type is custom, this method is used to
	 * set the custom robots.txt file to use instead of
	 * the one the server provides.
	 * 
	 * @param robots String with contents of custom robots file
	 */
	public void setCustomRobots(String robots) {
		this.customRobots = robots;
	}
	
	/**
	 * Get the supplied custom robots.txt
	 * 
	 * @return String with content of alternate robots.txt 
	 */
	public String getCustomRobots() {
		return this.customRobots;
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
	public int getType() {
		return type;
	}
	
	/**
	 * Check if policy is of a certain type.
	 * 
	 * @param type
	 * @return true if the policy is of the submitted type
	 */
	public boolean isType(int type) {
		return this.type == type;
	}

    public void initialize(CrawlerSettings settings) {
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

            setMasquerade(((Boolean) getAttribute(settings, ATTR_MASQUERADE)).booleanValue());
            
            // if the policy type is custom, we should look up the admins robots.txt file
            if(isType(RobotsHonoringPolicy.CUSTOM)) {
                setCustomRobots((String) getAttribute(settings, ATTR_CUSTOM_ROBOTS));
            }
            if (isType(RobotsHonoringPolicy.MOST_FAVORED_SET)) {
                userAgents = (StringList) getAttribute(settings, ATTR_USER_AGENTS);
            }
        } catch (AttributeNotFoundException e) {
            logger.severe(e.getMessage());
        }
    }
}