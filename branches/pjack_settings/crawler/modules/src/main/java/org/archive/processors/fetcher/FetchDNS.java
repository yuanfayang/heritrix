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
package org.archive.processors.fetcher;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import org.apache.commons.httpclient.URIException;
import static org.archive.processors.fetcher.FetchStatusCodes.*;
import org.archive.processors.Processor;
import org.archive.processors.ProcessorURI;
import org.archive.processors.util.CrawlHost;
import org.archive.processors.util.ServerCache;
import org.archive.state.Expert;
import org.archive.state.Immutable;
import org.archive.state.Initializable;
import org.archive.state.Key;
import org.archive.state.StateProvider;
import org.archive.util.ArchiveUtils;
import org.archive.util.Recorder;
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
public class FetchDNS extends Processor implements Initializable {

    private static final long serialVersionUID = 3L;

    private static Logger logger = Logger.getLogger(FetchDNS.class.getName());

    // Defaults.
    private short ClassType = DClass.IN;
    private short TypeType = Type.A;
    protected InetAddress serverInetAddr = null;

    private ServerCache crawlHostCache;

    /**
     * If a DNS lookup fails, whether or not to fallback to InetAddress
     * resolution, which may use local 'hosts' files or other mechanisms.
     */
    @Expert
    final public static Key<Boolean> ACCEPT_NON_DNS_RESOLVES = 
        Key.make(false);

    
    /**
     * Used to do DNS lookups.
     */
    @Immutable
    final public static Key<ServerCache> SERVER_CACHE = 
        Key.make(ServerCache.class, null);
    
    /**
     * Whether or not to perform an on-the-fly digest hash of retrieved
     * content-bodies.
     */
    @Expert
    final public static Key<Boolean> DIGEST_CONTENT = Key.make(true);


    /**
     * Which algorithm (for example MD5 or SHA-1) to use to perform an 
     * on-the-fly digest hash of retrieved content-bodies.
     */
    @Expert
    final public static Key<String> DIGEST_ALGORITHM = Key.make("sha1");


    private static final long DEFAULT_TTL_FOR_NON_DNS_RESOLVES
        = 6 * 60 * 60; // 6 hrs
    
    private byte [] reusableBuffer = new byte[1024];


    
    public FetchDNS() {
    }

    
    public void initialTasks(StateProvider p) {
        this.crawlHostCache = p.get(this, SERVER_CACHE);
    }
    
    protected boolean shouldProcess(ProcessorURI curi) {
        return curi.getUURI().getScheme().equals("dns");
    }
    
    
    protected void innerProcess(ProcessorURI curi) {
        Record[] rrecordSet = null; // Retrieved dns records
        String dnsName = null;
        try {
            dnsName = curi.getUURI().getReferencedHost();
        } catch (URIException e) {
            logger.log(Level.SEVERE, "Failed parse of dns record " + curi, e);
        }
        
        if(dnsName == null) {
            curi.setFetchStatus(S_UNFETCHABLE_URI);
            return;
        }

        CrawlHost targetHost = crawlHostCache.getHostFor(dnsName);
        if (isQuadAddress(curi, dnsName, targetHost)) {
        	// We're done processing.
        	return;
        }
        
        // Do actual DNS lookup.
        curi.setFetchBeginTime(System.currentTimeMillis());

        // Try to get the records for this host (assume domain name)
        // TODO: Bug #935119 concerns potential hang here
        rrecordSet = dns.getRecords(dnsName, TypeType, ClassType);
        curi.setContentType("text/dns");
        if (rrecordSet != null) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Found recordset for " + dnsName);
            }
        	storeDNSRecord(curi, dnsName, targetHost, rrecordSet);
        } else {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Failed find of recordset for " + dnsName);
            }
            if (curi.get(this, ACCEPT_NON_DNS_RESOLVES)) {
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
        curi.setFetchCompletedTime(System.currentTimeMillis());
    }
    
    protected void storeDNSRecord(final ProcessorURI curi, final String dnsName,
    		final CrawlHost targetHost, final Record[] rrecordSet) {
        // Get TTL and IP info from the first A record (there may be
        // multiple, e.g. www.washington.edu) then update the CrawlServer
        ARecord arecord = getFirstARecord(rrecordSet);
        if (arecord == null) {
            throw new NullPointerException("Got null arecord for " +
                dnsName);
        }
        targetHost.setIP(arecord.getAddress(), arecord.getTTL());
        try {
        	recordDNS(curi, rrecordSet);
            curi.setFetchStatus(S_DNS_SUCCESS);
            curi.setDNSServerIPLabel(FindServer.server());
        } catch (IOException e) {
        	logger.log(Level.SEVERE, "Failed store of DNS Record for " +
        		curi.toString(), e);
        	setUnresolvable(curi, targetHost);
        }
    }
    
    protected boolean isQuadAddress(final ProcessorURI curi, final String dnsName,
			final CrawlHost targetHost) {
		boolean result = false;
		Matcher matcher = InetAddressUtil.IPV4_QUADS.matcher(dnsName);
		// If it's an ip no need to do a lookup
		if (matcher == null || !matcher.matches()) {
			return result;
		}
		
		result = true;
		// Ideally this branch would never be reached: no ProcessorURI
		// would be created for numerical IPs
		if (logger.isLoggable(Level.WARNING)) {
			logger.warning("Unnecessary DNS ProcessorURI created: " + curi);
		}
		try {
			targetHost.setIP(InetAddress.getByAddress(dnsName, new byte[] {
					(byte) (new Integer(matcher.group(1)).intValue()),
					(byte) (new Integer(matcher.group(2)).intValue()),
					(byte) (new Integer(matcher.group(3)).intValue()),
					(byte) (new Integer(matcher.group(4)).intValue()) }),
					CrawlHost.IP_NEVER_EXPIRES); // Never expire numeric IPs
			curi.setFetchStatus(S_DNS_SUCCESS);
		} catch (UnknownHostException e) {
			logger.log(Level.SEVERE, "Should never be " + e.getMessage(), e);
			setUnresolvable(curi, targetHost);
		}
		return result;
	}
    
    protected void recordDNS(final ProcessorURI curi, final Record[] rrecordSet)
            throws IOException {
        final byte[] dnsRecord = getDNSRecord(curi.getFetchBeginTime(),
                rrecordSet);

        Recorder rec = curi.getRecorder();
        // Shall we get a digest on the content downloaded?
        boolean digestContent = curi.get(this, DIGEST_CONTENT);
        String algorithm = null;
        if (digestContent) {
            algorithm = curi.get(this, DIGEST_ALGORITHM);
            rec.getRecordedInput().setDigest(algorithm);
        } else {
            rec.getRecordedInput().setDigest((MessageDigest)null);
        }
        InputStream is = curi.getRecorder().inputWrap(
                new ByteArrayInputStream(dnsRecord));

        if (digestContent) {
            rec.getRecordedInput().startDigest();
        }

        // Reading from the wrapped stream, behind the scenes, will write
        // files into scratch space
        try {
            while (is.read(this.reusableBuffer) != -1) {
                continue;
            }
        } finally {
            is.close();
            rec.closeRecorders();
        }
        curi.setContentSize(dnsRecord.length);

        if (digestContent) {
            curi.setContentDigest(algorithm,
                rec.getRecordedInput().getDigestValue());
        }
    }
    
    protected byte [] getDNSRecord(final long fetchStart,
    		final Record[] rrecordSet)
    throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // Start the record with a 14-digit date per RFC 2540
        byte[] fetchDate = ArchiveUtils.get14DigitDate(fetchStart).getBytes();
        baos.write(fetchDate);
        // Don't forget the newline
        baos.write("\n".getBytes());
        int recordLength = fetchDate.length + 1;
        if (rrecordSet != null) {
            for (int i = 0; i < rrecordSet.length; i++) {
                byte[] record = rrecordSet[i].toString().getBytes();
                recordLength += record.length;
                baos.write(record);
                // Add the newline between records back in
                baos.write("\n".getBytes());
                recordLength += 1;
            }
        }
        return baos.toByteArray();
    }
    
    protected void setUnresolvable(ProcessorURI curi, CrawlHost host) {
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
