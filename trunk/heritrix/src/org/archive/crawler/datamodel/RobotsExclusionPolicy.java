/*
 * RobotsExclusionPolicy.java
 * Created on Apr 17, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * expiry handled outside, in CrawlHost
 * @author gojomo
 *
 */
public class RobotsExclusionPolicy {
	public static RobotsExclusionPolicy ALLOWALL = new RobotsExclusionPolicy(null, null);
	public static RobotsExclusionPolicy DENYALL = new RobotsExclusionPolicy(null, null);
	
	private LinkedList userAgents = null;
	private HashMap disallows = null; // of (String -> List)
		
	/**
	 * @param vb
	 */
	public static RobotsExclusionPolicy policyFor(BufferedReader reader) {
		String read;
		ArrayList current = null;
		LinkedList userAgents = new LinkedList();
		HashMap disallows = new HashMap(); 
 
		try {
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
						String ua = read.substring(11);
						if(current==null||current.size()==0) {
							// only create new rules-list if necessary
							// otherwise share with previous user-agent
							current = new ArrayList();
						} 
						if(ua.equals("*")) {
							ua = ".*";
							userAgents.addLast(ua); // put the catchall as a last entry
						} else {
							ua = "(?i)"+ua;
							userAgents.addFirst(ua);
						}
						disallows.put(ua,current);
						continue;
					}
					if (read.matches("(?i)Disallow:.*")) {
						String path = read.substring(9);
						current.add(path);
						continue;
					}
					//unknown line; do nothing for now
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			// TODO: be smarter here: perhaps use partial results, if avail?
			return ALLOWALL;
		}
		return new RobotsExclusionPolicy(userAgents, disallows);
	}
	
	
	/**
	 * @param userAgents
	 * @param disallows
	 */
	public RobotsExclusionPolicy(LinkedList u, HashMap d) {
		userAgents = u;
		disallows = d;
	}

	public boolean disallows(String path, String userAgent) {
		if (this == ALLOWALL)
			return false;
		if (this == DENYALL)
			return true;
		
		// TODO implement
		
		// TODO: improve behavior in common case: where only one entry matters,
		// because crawler user-agent never changes

		return false;
	}
 
}
