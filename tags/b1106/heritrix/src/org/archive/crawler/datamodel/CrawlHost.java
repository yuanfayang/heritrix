/*
 * CrawlHost.java
 * Created on Aug 5, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author gojomo
 *
 */
public class CrawlHost {
	String name;
	InetAddress ip;
	long ipExpires = -1;
	private boolean hasBeenLookedUp = false;
	
	
	/**
     * @param hostname
	 */
	public CrawlHost(String hostname) {
		name = hostname;
		if (hostname.matches("[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}")) {
			try {
				String[] octets = hostname.split("\\.");

				setIP(
					InetAddress.getByAddress(
						hostname,
						new byte[] {
							(byte) (new Integer(octets[0])).intValue(),
							(byte) (new Integer(octets[1])).intValue(),
							(byte) (new Integer(octets[2])).intValue(),
							(byte) (new Integer(octets[3])).intValue()})
				);
			} catch (UnknownHostException e) {
				// this should never happen as a dns lookup is not made
				e.printStackTrace();
			}
			// never expire numeric IPs
			setIpExpires(Long.MAX_VALUE);
		}
	}
		
		
	/**
	 * @return
	 */
	public boolean hasBeenLookedUp() {
		return hasBeenLookedUp;
	}
	/**
	 * 
	 */
	public void setHasBeenLookedUp() {
		hasBeenLookedUp = true;
	}
	/**
	 * @param address
	 */
	public void setIP(InetAddress address) {
		ip = address;
		// assume that a lookup as occurred by the time
		// a caller decides to set this (even to null)
		setHasBeenLookedUp();
	}
	
	/**
	 * @param expires
	 */
	public void setIpExpires(long expires) {
		//ipExpires = System.currentTimeMillis() + 10000;
		ipExpires = expires;
	}

	public boolean isIpExpired() {
		if (ipExpires >= 0 && ipExpires < System.currentTimeMillis()) {
			return true;
		}
		return false;
	}

	public InetAddress getIP() {
		return ip;
	}

	public long getIpExpires() {
		return ipExpires;
	}


}
