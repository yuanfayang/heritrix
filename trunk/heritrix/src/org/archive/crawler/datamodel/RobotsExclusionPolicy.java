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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * expiry handled outside, in CrawlServer
 * @author gojomo
 *
 */
public class RobotsExclusionPolicy {
	private static Logger logger = Logger.getLogger("org.archive.crawler.datamodel.RobotsExclusionPolicy");

	public static RobotsExclusionPolicy ALLOWALL = new RobotsExclusionPolicy(null, null, false);
	public static RobotsExclusionPolicy DENYALL = new RobotsExclusionPolicy(null, null, false);
	
	private LinkedList userAgents = null;
	private HashMap disallows = null; // of (String -> List)
	private boolean hasErrors = false; // flag for flawed bu workable robots.txts
	
	/**
	 * @param vb
	 */
	public static RobotsExclusionPolicy policyFor(BufferedReader reader) {
		String read;
		ArrayList current = null;
		LinkedList userAgents = new LinkedList();
		HashMap disallows = new HashMap(); 
 		boolean hasErrors = false;
 		String catchall = null;
 		
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
						String ua = read.substring(11).trim().toLowerCase();
						if(current==null||current.size()==0) {
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
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			// TODO: be smarter here: perhaps use partial results, if avail?
			return ALLOWALL;
		}
		if (catchall!=null) {
			userAgents.addLast(catchall);
		}
		return new RobotsExclusionPolicy(userAgents, disallows, hasErrors);
	}
	
	
	/**
	 * @param u
	 * @param d
	 * @param errs
	 */
	public RobotsExclusionPolicy(LinkedList u, HashMap d, boolean errs) {
		userAgents = u;
		disallows = d;
		hasErrors = errs;
	}

	public boolean disallows(String path, String userAgent) {
		if (this == ALLOWALL)
			return false;
		if (this == DENYALL)
			return true;
		
		// TODO: improve behavior in common case: where only one entry matters,
		// because crawler user-agent never changes
		
		Iterator iter = userAgents.iterator();
		while ( iter.hasNext() ) {
			String ua = (String)iter.next();
			if (userAgent.indexOf(ua)>-1) {
				Iterator i2 = ((List)disallows.get( ua )).iterator();
				while ( i2.hasNext() ) {
					String disallowedPath = (String)i2.next();
					if ( disallowedPath.length() == 0 ) {
						// blanket allow
						return false;
					}
					if ( path.startsWith(disallowedPath) ) {
						return true;
					}
				}
				return false; // no disallows for this ua
			}
		}
		// no matching ua
		return false;
	}
 
}
