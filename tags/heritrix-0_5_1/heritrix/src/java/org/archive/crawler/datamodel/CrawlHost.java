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
 * CrawlHost.java
 * Created on Aug 5, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

import java.net.InetAddress;
import java.net.UnknownHostException;

/** Represents a single remote "host".
 *
 * An host is a name for which there is a dns record or an IP-address. This
 * might be a machine or a virtual host.
 *
 * @author gojomo
 */
public class CrawlHost {
    public static final long IP_NEVER_EXPIRES = -1;
    public static final long IP_NEVER_LOOKED_UP = -2;
    String name;
    InetAddress ip;
    private long ipFetched = IP_NEVER_LOOKED_UP;
    private long ipTTL = IP_NEVER_LOOKED_UP;
    private boolean hasBeenLookedUp = false;


    /** Create a new CrawlHost object.
     * 
     * @param hostname the host name for this host.
     */
    public CrawlHost(String hostname) {
        name = hostname;
        if (hostname.matches(
                "[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}")) {
            try {
                String[] octets = hostname.split("\\.");

                setIP(
                    InetAddress.getByAddress(
                        hostname,
                        new byte[] {
                            (byte) (new Integer(octets[0])).intValue(),
                            (byte) (new Integer(octets[1])).intValue(),
                            (byte) (new Integer(octets[2])).intValue(),
                            (byte) (new Integer(octets[3])).intValue()}),
                IP_NEVER_EXPIRES);  // never expire numeric IPs
            } catch (UnknownHostException e) {
                // this should never happen as a dns lookup is not made
                e.printStackTrace();
            }
        }
    }

    /** Return true if the IP for this host has been looked up.
     * 
     * Returns true even if the lookup failed.
     * 
     * @return true if the IP for this host has been looked up.
     */
    public boolean hasBeenLookedUp() {
        return ipFetched != IP_NEVER_LOOKED_UP;
    }

    /** Set the IP address for this host.
     * 
     * @param address
     * @param ttl the TTL from the dns record or -1 if it should live forever
     *            (is a numeric IP).
     */
    public void setIP(InetAddress address, long ttl) {
        ip = address;
        // assume that a lookup as occurred by the time
        // a caller decides to set this (even to null)
        ipFetched = System.currentTimeMillis();
        ipTTL = ttl;
    }

    /** Get the IP address for this host.
     * 
     * @return the IP address for this host.
     */
    public InetAddress getIP() {
        return ip;
    }

    /** Get the time when the IP address for this host was last looked up.
     * 
     * @return the time when the IP address for this host was last looked up.
     */
    public long getIpFetched() {
        return ipFetched;
    }

    /** Get the TTL value from the dns record for this host.
     * 
     * @return the TTL value from the dns record for this host or -1 if this
     *         lookup should be valid forever (numeric ip).
     */
    public long getIpTTL() {
        return ipTTL;
    }

    public String toString() {
        return "CrawlHost<" + name + "(ip:" + ip + ")>";
    }
}
