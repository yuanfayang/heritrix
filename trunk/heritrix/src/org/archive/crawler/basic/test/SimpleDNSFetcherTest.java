/*
 * Created on Jun 11, 2003
 *
 */
package org.archive.crawler.basic.test;

import junit.framework.*;
import org.archive.crawler.basic.SimpleDNSFetcher;
import org.archive.crawler.datamodel.*;
import org.xbill.DNS.*;

/**
 * @author parkert
 *
 */
public class SimpleDNSFetcherTest extends TestCase {

	public static void main(String[] args) {
		
		// create the DNS Fetcher
		SimpleDNSFetcher fetcher = new SimpleDNSFetcher();
		
		// create a CrawlURI so we can test it and manually create the crawlhost 
		// TODO should the crawlhost creation be done in the curi constructor?
		CrawlURI curi = new CrawlURI( UURI.createUURI("dns:parkert.com") );
		curi.setHost( new CrawlHost("parkert.com"));
		//curi.getUURI().getUri().
		
		fetcher.process(curi);
		
		// make sure we got the appropriate info into curi (dns info)
		// by looking at the dnsrecords Alist entry, and the curi's CrawlHost
		// which should have received and IP and a time to expire
		Record[] RRSet = (Record[])curi.getAList().getObject(SimpleDNSFetcher.RRECORDS_ALIST_LABEL);
		//Assert.assertNotNull(RRSet);
		
		CrawlHost CHost = curi.getHost();	// should be parkert.com
		long ExpireTime = CHost.getIpExpires();
		String HostAsString = CHost.getIP().toString();
		
		long timestamp = curi.getAList().getLong(SimpleDNSFetcher.DNSFETCH_TIMESTAMP_LABEL);
		
		for(int i=0; i<RRSet.length; i++)
			System.out.println("rr:\t\t\t\t" + RRSet[i].toString());
			
		System.out.println("host:\t\t\t" + HostAsString);
		System.out.println("expires:\t\t" + ExpireTime);
		System.out.println("timestamp:\t" + timestamp);
	}
}
