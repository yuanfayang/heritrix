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

import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlHost;
import org.archive.crawler.datamodel.CrawlServer;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.framework.Processor;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;
import org.xbill.DNS.dns;


/**
 * Processor to resolve 'dns:' URIs.
 *
 * @author multiple
 */
public class FetchDNS extends Processor implements CoreAttributeConstants, FetchStatusCodes {
    private static Logger logger = Logger.getLogger("org.archive.crawler.basic.FetcherDNS");

    // defaults
     private short ClassType = DClass.IN;
     private short TypeType = Type.A;

     protected InetAddress serverInetAddr = null;
     // protected CrawlServer dnsServer = null;

    /** Create a new instance of FetchDNS.
     *
     * @param name the name of this attribute.
     */
    public FetchDNS(String name) {
        super(name, "DNS Fetcher. \nHandles DNS lookups.");
    }

    protected void innerProcess(CrawlURI curi) {

        Record[] rrecordSet = null;         // store retrieved dns records
        long now;                           // the time this operation happened
        CrawlServer targetServer = null;
        String dnsName = parseTargetDomain(curi);

        if (!curi.getUURI().getScheme().equals("dns")) {
            // only handles dns
            return;
        }

        // Make sure we're in "normal operating mode", e.g. a cache + controller
        // exist to assist us
        if (getController() != null &&
                getController().getServerCache() != null) {
            targetServer =
                getController().getServerCache().getServerFor(dnsName);
        } else {
            // Standalone operation (mostly for test cases/potential other uses)
            targetServer = new CrawlServer(dnsName);
        }

        // if it's an ip no need to do a lookup
        if (dnsName.matches(
                "[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}")) {
            // Ideally this branch would never be reached: no CrawlURI
            // would be created for numerical IPs
            logger.warning("unnecessary DNS CrawlURI created: "+curi);

            try {
                String[] octets = dnsName.split("\\.");

                targetServer.getHost().setIP (
                    InetAddress.getByAddress(
                        dnsName,
                        new byte[] {
                            (byte) (new Integer(octets[0])).intValue(),
                            (byte) (new Integer(octets[1])).intValue(),
                            (byte) (new Integer(octets[2])).intValue(),
                            (byte) (new Integer(octets[3])).intValue()}),
                        CrawlHost.IP_NEVER_EXPIRES); // never expire numeric IPs

            } catch (UnknownHostException e) {
                // this should never happen as a dns lookup is not made

                e.printStackTrace();
            }

            curi.setFetchStatus(S_DNS_SUCCESS);

            // no further lookup necessary
            return;
        }

        now = System.currentTimeMillis();
        curi.getAList().putLong(A_FETCH_BEGAN_TIME, now);

        // try to get the records for this host (assume domain name)
        // TODO: Bug #935119 concerns potential hang here
        rrecordSet = dns.getRecords(dnsName, TypeType, ClassType);

        // Set null ip to mark that we have tried to look it up even if
        // dns fetch fails.
        targetServer.getHost().setIP(null, 0);

        if (rrecordSet != null) {
            curi.setFetchStatus(S_DNS_SUCCESS);
            curi.setContentType("text/dns");
            curi.getAList().putObject(A_RRECORD_SET_LABEL, rrecordSet);

            // Get TTL and IP info from the first A record (there may be
            // multiple, e.g. www.washington.edu) then update the CrawlServer
            for (int i = 0; i < rrecordSet.length; i++) {

                if (rrecordSet[i].getType() != Type.A) {
                    continue;
                }

                ARecord AsA = (ARecord) rrecordSet[i];
                targetServer.getHost().setIP(
                        AsA.getAddress(), (long) AsA.getTTL());
                break; // only need to process one record
            }
        } else {
            curi.setFetchStatus(S_DOMAIN_UNRESOLVABLE);
        }

        curi.getAList().putLong(A_FETCH_COMPLETED_TIME, System.currentTimeMillis());
    }

    /**
     * Parse passed dns <code>CrawlURI</code> for its hostname component.
     *
     * CrawlURI should look like:
     * <code>"dns:" [ "//" hostport "/" ] dnsname [ "?" dnsquery ]</code>
     *
     * @param curi Crawl URI to parse.
     * @return The target hostname of a 'dns:' URI. Returns null for
     * non-'dns:' input.
     */
    public static String parseTargetDomain(CrawlURI curi){
        String uri = curi.getURIString();

        // if it's not a dns uri
        if(!uri.startsWith("dns:")){
            return null;
        }

        uri = uri.substring(4);                        // drop "dns:" prefix

        if(uri.startsWith("//")){                        // drop hostport
            uri = uri.replaceFirst("//[^/]+/", "");
        }

        // drop query string
        if(uri.indexOf("?") > -1){
            uri = uri.substring(0, uri.indexOf("?"));
        }

        return uri;
    }
}
