/*
 * RobotsExclusionPolicy.java
 * Created on Apr 17, 2003
 *
 * $Header$
 */
package org.archive.crawler.framework;

/**
 * @author gojomo
 *
 */
public class RobotsExclusionPolicy {
	public static RobotsExclusionPolicy ALLOWALL;
	public static RobotsExclusionPolicy DENYALL;
	
	boolean immutable;
	long expires;
	int policyChecksum;
	
	public boolean hasExpired() {
		return System.currentTimeMillis()>expires;
	}
	
	public boolean disallows(String path, String userAgent) {
		// TODO implement
		return false;
	}
	
	/**
	 * Replace existing policy with 
	 * @param fbis
	 */
	public RobotsExclusionPolicy updateWith(FullyBufferedInputStream fbis, long newExpires) {
		// TODO implement
		return null;
	}
}
