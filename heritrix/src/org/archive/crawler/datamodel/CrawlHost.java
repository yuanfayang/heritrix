/*
 * CrawlHost.java
 * Created on Apr 17, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.zip.Checksum;

import org.apache.commons.httpclient.methods.GetMethod;

/**
 * Represents a single remote "host". 
 * 
 * @author gojomo
 *
 */
public class CrawlHost {
	public static long DEFAULT_ROBOTS_VALIDITY_DURATION = 1000*60*60*24; // one day 
	String hostname; // actually, host+port in the http case
	InetAddress ip;
	long ipExpires = -1;
	RobotsExclusionPolicy robots;
	long robotsExpires = -1;
	Checksum robotstxtChecksum;
	private boolean hasBeenLookedUp = false;
	
	/**
	 * @param h
	 */
	public CrawlHost(String h) {
		// TODO: possibly check for illegal host string
		hostname = h;
	}
		
	public boolean hasBeenLookedUp(){
		return hasBeenLookedUp;
	}
	
	public void setHasBeenLookedUp(){
		hasBeenLookedUp = true;
	}
	
	/**
	 * @return
	 */
	public long getRobotsExpires() {
		return robotsExpires;
	}

	/**
	 * @param l
	 */
	public void setRobotsExpires(long l) {
		robotsExpires = l;
	}

	/**
	 * @return
	 */
	public RobotsExclusionPolicy getRobots() {
		return robots;
	}

	/**
	 * @param policy
	 */
	public void setRobots(RobotsExclusionPolicy policy) {
		robots = policy;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "CrawlHost("+hostname+")";
	}

	/**
	 * @param get
	 */
	public void updateRobots(GetMethod get) {
		robotsExpires = System.currentTimeMillis()+DEFAULT_ROBOTS_VALIDITY_DURATION;
		if (get.getStatusCode()==404) {
			// not found == all ok
			robots = RobotsExclusionPolicy.ALLOWALL;
			return;
		}
//	PREVAILING PRACTICE PER GOOGLE: treat these errors as all-allowed, 
//  since they're usually indicative of a mistake
//      if ((get.getStatusCode() >= 401) && (get.getStatusCode() <= 403)) {
//			// authorization/allowed errors = all deny
//			robots = RobotsExclusionPolicy.DENYALL;
//			return;
//		}
		// TODO: handle other errors, perhaps redirects
		// note that akamai will return 400 for some "not founds"
		try {
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(
						get.getResponseBodyAsStream()));
			robots = RobotsExclusionPolicy.policyFor(reader);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			robots = RobotsExclusionPolicy.ALLOWALL;
		}
		return;
	}
	
	/**
	 * @param InetAddress
	 */
	public void setIP(InetAddress address){
		ip = address; 
		
		// assume that a lookup as occurred by the time
		// a caller decides to set this (even to null)
		setHasBeenLookedUp();
	}
	
	/**
	 * @param expires
	 */
	 public void setIpExpires(long expires){
	 	//ipExpires = System.currentTimeMillis() + 10000;
	 	ipExpires = expires;
	 }
	 
	public boolean isIpExpired() {
		if (ipExpires >= 0 && ipExpires < System.currentTimeMillis()) {
			return true;
		}
		return false;
	}
	 
	public boolean isRobotsExpired() {
		if (robotsExpires >= 0 && robotsExpires < System.currentTimeMillis()) {
			return true;
		}
		return false;
	}
	 
	 public InetAddress getIP(){
	 	return ip;
	 }
	 
	 public String getHostname(){
	 	return hostname;
	 }
	 
	 public long getIpExpires(){ 
	 	return ipExpires;
	 }

}
