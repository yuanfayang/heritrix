/*
 * RobotsExclusionPolicy.java
 * Created on Apr 17, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

import java.util.zip.Checksum;

import org.archive.crawler.io.*;

/**
 * @author gojomo
 *
 */
public class RobotsExclusionPolicy {
	public static RobotsExclusionPolicy ALLOWALL = new RobotsExclusionPolicy(true);
	public static RobotsExclusionPolicy DENYALL = new RobotsExclusionPolicy(true);;
	
	boolean immutable;
	long expires;
	Checksum robotstxtChecksum; 
	
	public boolean hasExpired() {
		return System.currentTimeMillis()>expires;
	}
	
	/**
	 * @param b
	 */
	public RobotsExclusionPolicy(boolean b) {
		immutable = b;
	}

	public boolean disallows(String path, String userAgent) {
		if (this == ALLOWALL)
			return false;
		if (this == DENYALL)
			return true;
		
		// TODO implement
		return false;
	}
	
	/**
	 * Replace existing policy with 
	 * @param fbis
	 */
	public RobotsExclusionPolicy updateWith(VirtualBuffer fbis, long newExpires) {
		// TODO implement
		
		// if immutable, don't change in place... return a fresh copy
		return null;
	}
}
