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
 * SimpleDNSFetcher.java
 * Created on Jun 5, 2003
 *
 * $Header$
 */
package org.archive.crawler.fetcher;

import java.net.InetAddress;
import java.net.UnknownHostException;
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
        Logger.getLogger("org.archive.crawler.basic.FetcherDNS");

    // defaults
     private short ClassType = DClass.IN;
     private short TypeType = Type.A;

     protected InetAddress serverInetAddr = null;

    private static final String ATTR_ACCEPT_NON_DNS_RESOLVES = "accept-non-dns-resolves";
    private static final Boolean DEFAULT_ACCEPT_NON_DNS_RESOLVES = Boolean.FALSE;
    private static final long DEFAULT_TTL_FOR_NON_DNS_RESOLVES = 6*60*60; // 6 hrs
    
     // protected CrawlServer dnsServer = null;

    /** Create a new instance of FetchDNS.
     *
     * @param name the name of this attribute.
     */
    public FetchDNS(String name) {
        super(name, "DNS Fetcher. \nHandles DNS lookups.");
        org.archive.crawler.settings.Type e = addElementToDefinition(new SimpleType(
                ATTR_ACCEPT_NON_DNS_RESOLVES,
                "If a DNS lookup fails, whether or not to fallback to "
                        + "InetAddress resolution, which may use local 'hosts' files "
                        + "or other mechanisms.",
                DEFAULT_ACCEPT_NON_DNS_RESOLVES));
        e.setExpertSetting(true);
    }

    protected void innerProcess(CrawlURI curi) {
        if (!curi.getUURI().getScheme().equals("dns")) {
            // only handles dns
            return;
        }
        Record[] rrecordSet = null;         // store retrieved dns records
        long now;                           // the time this operation happened
        String dnsName = null;
        try {
            dnsName = curi.getUURI().getReferencedHost();
        } catch (URIException e) {
            logger.severe("Failed parse of dns record " + curi + " " +
                e.getMessage());
            return;
        }

        // Make sure we're in "normal operating mode", e.g. a cache +
        // controller exist to assist us
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
        // if it's an ip no need to do a lookup
        if (matcher != null && matcher.matches()) {
            // Ideally this branch would never be reached: no CrawlURI
            // would be created for numerical IPs
            logger.warning("Unnecessary DNS CrawlURI created: " + curi);
            try {
                targetHost.setIP(
                    InetAddress.getByAddress(dnsName,
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

        now = System.currentTimeMillis();
        curi.putLong(A_FETCH_BEGAN_TIME, now);

        // Try to get the records for this host (assume domain name)
        // TODO: Bug #935119 concerns potential hang here
        rrecordSet = dns.getRecords(dnsName, TypeType, ClassType);

        // Set null ip to mark that we have tried to look it up even if
        // dns fetch fails.
        targetHost.setIP(null, 0);
        if (rrecordSet != null) {
            curi.setFetchStatus(S_DNS_SUCCESS);
            curi.setContentType("text/dns");
            curi.putObject(A_RRECORD_SET_LABEL, rrecordSet);
            // Get TTL and IP info from the first A record (there may be
            // multiple, e.g. www.washington.edu) then update the CrawlServer
            for (int i = 0; i < rrecordSet.length; i++) {
                if (rrecordSet[i].getType() != Type.A) {
                    continue;
                }

                ARecord AsA = (ARecord) rrecordSet[i];
                targetHost.setIP(AsA.getAddress(), AsA.getTTL());
                break; // only need to process one record
            }
        } else {
            if (((Boolean) getUncheckedAttribute(null,
                    ATTR_ACCEPT_NON_DNS_RESOLVES)).booleanValue()) {
                InetAddress address = null;
                try {
                    address = InetAddress.getByName(dnsName);
                } catch (UnknownHostException e1) {
                    address = null;
                }
                if (address != null) {
                    targetHost.setIP(address, DEFAULT_TTL_FOR_NON_DNS_RESOLVES);
                    curi.setFetchStatus(S_GETBYNAME_SUCCESS);
                } else {
                    curi.setFetchStatus(S_DOMAIN_UNRESOLVABLE);
                }
            } else {
                curi.setFetchStatus(S_DOMAIN_UNRESOLVABLE);
            }
        }

        curi.putLong(A_FETCH_COMPLETED_TIME, System.currentTimeMillis());
    }
}
