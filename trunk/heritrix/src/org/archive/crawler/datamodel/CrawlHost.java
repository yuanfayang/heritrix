/*
 * CrawlHost.java
 * Created on Apr 17, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

import java.net.InetAddress;
import java.util.zip.Checksum;

import org.archive.crawler.io.VirtualBuffer;


/**
 * Represents a 
 * @author gojomo
 *
 */
public class CrawlHost {
	String hostname;
	InetAddress ip;
	long ipExpires;
	RobotsExclusionPolicy robots;
	long robotsExpires;
	Checksum robotstxtChecksum;
	
	public void updateRobots(VirtualBuffer vb, long newExpires) {
		robotsExpires = newExpires;
		if ((robotstxtChecksum != null)
			&& robotstxtChecksum.getValue() == vb.getChecksum().getValue()) {
			// unchanged
			return;
		}
		robotstxtChecksum = vb.getChecksum();
		robots = RobotsExclusionPolicy.policyFor(vb);
	}
}
