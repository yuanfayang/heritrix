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
 * CrawlServer.java
 * Created on Apr 17, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.zip.Checksum;

import org.apache.commons.httpclient.methods.GetMethod;
import org.archive.crawler.datamodel.settings.CrawlerSettings;
import org.archive.io.ReplayInputStream;

/**
 * Represents a single remote "host". 
 * 
 * @author gojomo
 *
 */
public class CrawlServer implements Serializable {
	public static long DEFAULT_ROBOTS_VALIDITY_DURATION = 3*(1000*60*60*24); // three days
	String server; // actually, host+port in the http case
	CrawlHost host;
    private CrawlerSettings settings;
	RobotsExclusionPolicy robots;
	long robotsExpires = -1;
	Checksum robotstxtChecksum;
	
	/**
	 * @param h
	 */
	public CrawlServer(String h) {
		// TODO: possibly check for illegal host string
		server = h;
	}
	
	public long getRobotsExpires() {
		return robotsExpires;
	}

	/**
	 * @param l
	 */
	public void setRobotsExpires(long l) {
		robotsExpires = l;
	}

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
		return "CrawlServer("+server+")";
	}

	/**
	 * @param get
	 * @param honoringPolicy
	 * @throws IOException
	 */
	public void updateRobots(GetMethod get)
            throws IOException {

        RobotsHonoringPolicy honoringPolicy =
            settings.getSettingsHandler().getOrder().getRobotsHonoringPolicy();

		robotsExpires = System.currentTimeMillis()
            + DEFAULT_ROBOTS_VALIDITY_DURATION;
		if (get.getStatusCode()!=200 || honoringPolicy.getType(settings)
            == RobotsHonoringPolicy.IGNORE) {
			// not found or other errors == all ok for now
			// TODO: consider handling server errors, redirects differently
			robots = RobotsExclusionPolicy.ALLOWALL;
			return;
		}
//	PREVAILING PRACTICE PER GOOGLE: treat these errors as all-allowed, 
//  since they're usually indicative of a mistake
// Thus these lines are commented out:
//      if ((get.getStatusCode() >= 401) && (get.getStatusCode() <= 403)) {
//			// authorization/allowed errors = all deny
//			robots = RobotsExclusionPolicy.DENYALL;
//			return;
//		}
		ReplayInputStream contentBodyStream = null;
		try {
			BufferedReader reader;
			if(honoringPolicy.getType(settings) == RobotsHonoringPolicy.CUSTOM) {
				reader = new BufferedReader(new StringReader(honoringPolicy.getCustomRobots(settings)));
			} else {
				contentBodyStream = get.getHttpRecorder().getRecordedInput().getContentReplayInputStream();
				contentBodyStream.setToResponseBodyStart();
				reader = new BufferedReader(
					new InputStreamReader(contentBodyStream));
			}
			robots = RobotsExclusionPolicy.policyFor(settings, reader, honoringPolicy);
		} catch (IOException e) {
			robots = RobotsExclusionPolicy.ALLOWALL;
			throw e; // rethrow
		} finally {
			if(contentBodyStream!=null) {
				contentBodyStream.close();
			}
		}
		return;
	}
	
	 
	public boolean isRobotsExpired() {
		if (robotsExpires >= 0 && robotsExpires < System.currentTimeMillis()) {
			return true;
		}
		return false;
	}
	 
	public String getServer(){
	   return server;
	}
	
	/**
	 * Get the associated CrawlHost
	 * 
	 * @return host
	 */
	public CrawlHost getHost() {
		return host;
	}

	/**
	 * @param host
	 */
	public void setHost(CrawlHost host) {
		this.host = host;
	}

	public String getHostname() {
		int colonIndex = server.indexOf(":");
		if(colonIndex < 0) {
			return server;
		}
		return server.substring(0,colonIndex);
	}


	/**
	 * Refuse to be serialized, but do not halt serialization:
	 * replace with null. 
	 * 
	 * @return Null.
	 * @throws ObjectStreamException
	 */
	protected Object writeReplace() throws ObjectStreamException {
		return null;
	}
	
//	private void writeObject(ObjectOutputStream stream)
//	 throws IOException {
//	 	ObjectOutputStream.PutField puts = stream.putFields();
//	 	puts.put("server",server);
//	 	stream.writeFields();
//	 }
// 
//	private void readObject(ObjectInputStream stream)
//	 throws IOException, ClassNotFoundException {
//	 	ObjectInputStream.GetField reads = stream.readFields();
//	 	server = (String)reads.get("server",null);
//	 }

    /**
     * @return Crawler settings.
     */
    public CrawlerSettings getSettings() {
        return settings;
    }

    /**
     * @param settings
     */
    public void setSettings(CrawlerSettings settings) {
        this.settings = settings;
    }
}
