/*
 * CrawlServer.java
 * Created on Apr 17, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.zip.Checksum;

import org.apache.commons.httpclient.methods.GetMethod;
import org.archive.io.ReplayInputStream;

/**
 * Represents a single remote "host". 
 * 
 * @author gojomo
 *
 */
public class CrawlServer implements Serializable {
	public static long DEFAULT_ROBOTS_VALIDITY_DURATION = 1000*60*60*24; // one day 
	String server; // actually, host+port in the http case
	CrawlHost host;
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
		return "CrawlServer("+server+")";
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
			ReplayInputStream contentBodyStream = get.getHttpRecorder().getRecordedInput().getReplayInputStream();
			contentBodyStream.setToResponseBodyStart();
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(contentBodyStream));
			robots = RobotsExclusionPolicy.policyFor(reader);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			robots = RobotsExclusionPolicy.ALLOWALL;
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
	 * @return
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

	/**
	 * @return
	 */
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
	 * @return
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

}
