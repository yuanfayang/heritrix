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
	long ipExpires = 0;
	RobotsExclusionPolicy robots;
	long robotsExpires = 0;
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
		if (get.getStatusCode()>=400) {
			// other errors = all deny
			robots = RobotsExclusionPolicy.DENYALL;
			return;
		}
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
}
