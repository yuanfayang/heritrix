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
 * Represents a 
 * @author gojomo
 *
 */
public class CrawlHost {
	private static long DEFAULT_VALIDITY_DURATION = 1000*60*60*24; // one day
	String hostname;
	InetAddress ip;
	long ipExpires = -1;
	RobotsExclusionPolicy robots;
	long robotsExpires = -1;
	Checksum robotstxtChecksum;
	
	/**
	 * @param h
	 */
	public CrawlHost(String h) {
		hostname = h;
	}
	
//	public void updateRobots(VirtualBuffer vb, long newExpires) {
//		robotsExpires = newExpires;
//		if ((robotstxtChecksum != null)
//			&& robotstxtChecksum.getValue() == vb.getChecksum().getValue()) {
//			// unchanged
//			return;
//		}
//		robotstxtChecksum = vb.getChecksum();
//		robots = RobotsExclusionPolicy.policyFor(vb);
//	}
	
	
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
		robotsExpires = System.currentTimeMillis()+DEFAULT_VALIDITY_DURATION;
		if (get.getStatusCode()==404) {
			// not found == all ok
			robots = RobotsExclusionPolicy.ALLOWALL;
			return;
		}
		if ((get.getStatusCode() >= 401) && (get.getStatusCode() <= 403)) {
			// authorization/allowed errors = all deny
			robots = RobotsExclusionPolicy.DENYALL;
			return;
		}
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
	}
	
	/**
	 * @param expires
	 */
	 public void setIpExpires(long expires){
	 	ipExpires = expires;
	 }
	 
	 public void setIpExpiresFromTTL(long ttl){
		long now = System.currentTimeMillis();
		setIpExpires(now + ttl);
	 }
	 
	 public InetAddress getIP(){
	 	return ip;
	 }
	 
	 public long getIpExpires(){ 
	 	return ipExpires;
	 }

}
