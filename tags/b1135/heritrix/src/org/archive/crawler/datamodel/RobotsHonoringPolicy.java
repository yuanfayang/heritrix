/*
 * RobotsHonoringPolicy.java
 * Created on Oct 30, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

import java.util.ArrayList;
import java.util.List;

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
 * @author johnh
 *
 */
public class RobotsHonoringPolicy {
	public final static int CLASSIC = 0;
	public final static int IGNORE = 1;
	public final static int CUSTOM = 2;
	public final static int MOST_FAVORED = 3;
	public final static int MOST_FAVORED_SET = 4;
	
	private String name;
	private int type;
	private String customRobots;
	private ArrayList userAgents;
	private boolean shouldMasquerade = false;
	
	/**
	 * Creates a new instance of RobotsHonoringPolicy.
	 * 
	 * @param name the type of policy to create
	 */
	public RobotsHonoringPolicy(String name) {
		if(name == null || name.length() == 0) {
			name = "classic";
		}

		name = name.toLowerCase();
		if(name.equals("classic")) {
			type = RobotsHonoringPolicy.CLASSIC;
		} else if(name.equals("ignore")) {
			type = RobotsHonoringPolicy.IGNORE;
		} else if(name.equals("custom")) {
			type = RobotsHonoringPolicy.CUSTOM;
		} else if(name.equals("most-favored")) {
			type = RobotsHonoringPolicy.MOST_FAVORED;
		} else if(name.equals("most-favored-set")) {
			type = RobotsHonoringPolicy.MOST_FAVORED_SET;
		} else {
			throw new IllegalArgumentException();
		}
		this.name = name;
	}
	
	/**
	 * If policy-type is most favored crawler of set, then this method is
	 * used to add crawlers to that set.
	 * 
	 * @param userAgent Name of user agent to add
	 */
	public void addUserAgent(String userAgent) {
		if(userAgents == null) {
			userAgents = new ArrayList();
		}
		userAgents.add(userAgent);
	}
	
	/**
	 * If policy-type is most favored crawler of set, then this method
	 * gets a list of all useragents in that set.
	 * 
	 * @return List of Strings with user agents
	 */
	public List getUserAgents() {
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
}
