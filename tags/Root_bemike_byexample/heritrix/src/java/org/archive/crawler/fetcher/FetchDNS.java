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
 * FetchDNS
 * Created on Jun 5, 2003
 *
 * $Header$
 */
package org.archive.crawler.fetcher;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlHost;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.framework.Processor;
import org.archive.crawler.settings.SimpleType;
import org.archive.util.InetAddressUtil;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.FindServer;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;
import org.xbill.DNS.dns;


/**
 * Processor to resolve 'dns:' URIs.
 * 
 * TODO: Refactor to use org.archive.util.DNSJavaUtils.
 *
 * @author multiple
 */
public class FetchDNS extends Processor
implements CoreAttributeConstants, FetchStatusCodes {
    private static Logger logger =
        Logger.getLogger(FetchDNS.class.getName());

    // Defaults.
    private short ClassType = DClass.IN;
    private short TypeType = Type.A;
    protected InetAddress serverInetAddr = null;
    
    private static final String ATTR_ACCEPT_NON_DNS_RESOLVES =
        "accept-non-dns-resolves";
    private static final Boolean DEFAULT_ACCEPT_NON_DNS_RESOLVES =
        Boolean.FALSE;
    private static final long DEFAULT_TTL_FOR_NON_DNS_RESOLVES
        = 6 * 60 * 60; // 6 hrs

    /** 
     * Create a new instance of FetchDNS.
     *
     * @param name the name of this attribute.
     */
    public FetchDNS(String name) {
        super(name, "DNS Fetcher. Handles DNS lookups.");
        org.archive.crawler.settings.Type e =
            addElementToDefinition(new SimpleType(ATTR_ACCEPT_NON_DNS_RESOLVES,
                "If a DNS lookup fails, whether or not to fallback to " +
                "InetAddress resolution, which may use local 'hosts' files " +
                "or other mechanisms.", DEFAULT_ACCEPT_NON_DNS_RESOLVES));
        e.setExpertSetting(true);
    }

    protected void innerProcess(CrawlURI curi) {
        if (!curi.getUURI().getScheme().equals("dns")) {
            // Only handles dns
            return;
        }
        Record[] rrecordSet = null; // Store retrieved dns records
        String dnsName = null;
        try {
            dnsName = curi.getUURI().getReferencedHost();
        } catch (URIException e) {
            logger.log(Level.SEVERE, "Failed parse of dns record " + curi, e);
        }
        if(dnsName==null) {
            curi.setFetchStatus(S_UNFETCHABLE_URI);
            return;
        }

        // Make sure we're in "normal operating mode", e.g. a cache +
        // controller exist to assist us.
        CrawlHost targetHost = null;
        if (getController() != null &&
                getController().getServerCache() != null) {
            targetHost =
                getController().getServerCache().getHostFor(dnsName);
        } else {
            // Standalone operation (mostly for test cases/potential other uses)
            targetHost = new CrawlHost(dnsName);
        }
        
        Matcher matcher = InetAddressUtil.IPV4_QUADS.matcher(dnsName);
        // If it's an ip no need to do a lookup
        if (matcher != null && matcher.matches()) {
            // Ideally this branch would never be reached: no CrawlURI
            // would be created for numerical IPs
            if (logger.isLoggable(Level.WARNING)) {
                logger.warning("Unnecessary DNS CrawlURI created: " + curi);
            }
            try {
                targetHost.setIP(InetAddress.getByAddress(dnsName,
                        new byte[] {
                            (byte)(new Integer(matcher.group(1)).intValue()),
                            (byte)(new Integer(matcher.group(2)).intValue()),
                            (byte)(new Integer(matcher.group(3)).intValue()),
                            (byte)(new Integer(matcher.group(4)).intValue())}),
                     CrawlHost.IP_NEVER_EXPIRES); // Never expire numeric IPs
            } catch (UnknownHostException e) {
                // This should never happen as a dns lookup is not made
                e.printStackTrace();
            }
            curi.setFetchStatus(S_DNS_SUCCESS);

            // No further lookup necessary
            return;
        }

        curi.putLong(A_FETCH_BEGAN_TIME, System.currentTimeMillis());

        // Try to get the records for this host (assume domain name)
        // TODO: Bug #935119 concerns potential hang here
        rrecordSet = dns.getRecords(dnsName, TypeType, ClassType);
        if (rrecordSet != null) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Found recordset for " + dnsName);
            }
            curi.setFetchStatus(S_DNS_SUCCESS);
            curi.setContentType("text/dns");
            curi.putObject(A_RRECORD_SET_LABEL, rrecordSet);
            // Get TTL and IP info from the first A record (there may be
            // multiple, e.g. www.washington.edu) then update the CrawlServer
            ARecord arecord = getFirstARecord(rrecordSet);
            if (arecord == null) {
                throw new NullPointerException("Got null arecord for " +
                    dnsName);
            }
            targetHost.setIP(arecord.getAddress(), arecord.getTTL());
        } else {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Failed find of recordset for " + dnsName);
            }
            if (((Boolean)getUncheckedAttribute(null,
                    ATTR_ACCEPT_NON_DNS_RESOLVES)).booleanValue()) {
                // Do lookup that bypasses javadns.
                InetAddress address = null;
                try {
                    address = InetAddress.getByName(dnsName);
                } catch (UnknownHostException e1) {
                    address = null;
                }
                if (address != null) {
                    targetHost.setIP(address, DEFAULT_TTL_FOR_NON_DNS_RESOLVES);
                    curi.setFetchStatus(S_GETBYNAME_SUCCESS);
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("Found address for " + dnsName +
                            " using native dns.");
                    }
                } else {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("Failed find of address for " + dnsName +
                            " using native dns.");
                    }
                    setUnresolvable(curi, targetHost);
                }
            } else {
                setUnresolvable(curi, targetHost);
            }
        }
        // Add the IP of the dns server to this dns crawluri.
        curi.putString(A_DNS_SERVER_IP_LABEL, FindServer.server());
        curi.putLong(A_FETCH_COMPLETED_TIME, System.currentTimeMillis());
    }
    
    protected void setUnresolvable(CrawlURI curi, CrawlHost host) {
        host.setIP(null, 0);
        curi.setFetchStatus(S_DOMAIN_UNRESOLVABLE); 
    }
    
    protected ARecord getFirstARecord(Record[] rrecordSet) {
        ARecord arecord = null;
        if (rrecordSet == null || rrecordSet.length == 0) {
            if (logger.isLoggable(Level.FINEST)) {
                logger.finest("rrecordSet is null or zero length: " +
                    rrecordSet);
            }
            return arecord;
        }
        for (int i = 0; i < rrecordSet.length; i++) {
            if (rrecordSet[i].getType() != Type.A) {
                if (logger.isLoggable(Level.FINEST)) {
                    logger.finest("Record " + Integer.toString(i) +
                        " is not A type but " + rrecordSet[i].getType());
                }
                continue;
            }
            arecord = (ARecord) rrecordSet[i];
            break;
        }
        return arecord;
    }
}
