/* DNSJavaUtil
 * 
 * Created on Oct 8, 2004
 *
 * Copyright (C) 2004 Internet Archive.
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
 */
package org.archive.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xbill.DNS.ARecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;
import org.xbill.DNS.dns;

/**
 * Utility methods based on DNSJava.
 * Use these utilities to avoid having to use the native InetAddress lookup.
 * @author stack
 * @version $Date$, $Revision$
 */
public class DNSJavaUtil {
    private static Logger logger =
        Logger.getLogger(DNSJavaUtil.class.getName());
    
    /**
     * ipv4 address.
     */
    public static Pattern IPV4_QUADS = Pattern.compile(
        "([0-9]{1,3})\\.([0-9]{1,3})\\.([0-9]{1,3})\\.([0-9]{1,3})");
    
    /**
     * Return an InetAddress for passed <code>host</code>.
     * 
     * If passed host is an IPv4 address, we'll not do a DNSJava
     * lookup.
     * 
     * @param host Host to lookup in dnsjava.
     * @return A host address or null if not found.
     */
    static public InetAddress getHostAddress(String host) {
        Matcher matcher = IPV4_QUADS.matcher(host);
        InetAddress hostAddress = null;
        if (matcher != null && matcher.matches()) {
            try {
                // Doing an Inet.getByAddress() avoids a lookup.
                hostAddress = InetAddress.getByAddress(host,
                        new byte[] {
                        (byte)(new Integer(matcher.group(1)).intValue()),
                        (byte)(new Integer(matcher.group(2)).intValue()),
                        (byte)(new Integer(matcher.group(3)).intValue()),
                        (byte)(new Integer(matcher.group(4)).intValue())});
            } catch (NumberFormatException e) {
                logger.warning(e.getMessage());
            } catch (UnknownHostException e) {
                logger.warning(e.getMessage());
            }
        } else {
            // Ask dnsjava for the inetaddress.  Should be in its cache.
            Record[] rrecordSet = dns.getRecords(host, Type.A, DClass.IN);
            if (rrecordSet != null) {
                // Get TTL and IP info from the first A record (there may be
                // multiple, e.g. www.washington.edu).
                for (int i = 0; i < rrecordSet.length; i++) {
                    if (rrecordSet[i].getType() != Type.A) {
                        continue;
                    }
                    hostAddress = ((ARecord)rrecordSet[i]).getAddress();
                    break;
                }
            }
        }
        return hostAddress;
    }
}
