/*
 * Created on Jun 11, 2003
 *
 */
package org.archive.crawler.basic.test;

import junit.framework.*;
import org.archive.crawler.basic.SimpleDNSFetcher;
import org.archive.crawler.datamodel.*;
import org.xbill.DNS.*;
import java.net.URI;

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
		/*URI testuri = null;
		try{
		 	testuri = new URI("dns:parkert.com?args");
		}catch(Exception e){}
		*/
		
		curi.setHost( new CrawlHost("parkert.com"));
		//curi.getUURI().getUri().
		
		fetcher.process(curi);
		
		// make sure we got the appropriate info into curi (dns info)
		// by looking at the dnsrecords Alist entry, and the curi's CrawlHost
		// which should have received and IP and a time to expire
		Record[] rrSet = (Record[])curi.getAList().getObject(SimpleDNSFetcher.RRECORDS_ALIST_LABEL);
		//Assert.assertNotNull(RRSet);
		
		CrawlHost hostAsString = curi.getHost();	// should be parkert.com
		long expireTime = hostAsString.getIpExpires();
		String HostAsString = hostAsString.getIP().toString();
		
		long timestamp = curi.getAList().getLong(SimpleDNSFetcher.DNSFETCH_TIMESTAMP_LABEL);
		
		for(int i=0; i<rrSet.length; i++)
			System.out.println("rr:\t\t\t\t" + rrSet[i].toString());
			
		System.out.println("host:\t\t\t" + HostAsString);
		System.out.println("expires:\t\t" + expireTime);
		System.out.println("timestamp:\t" + timestamp);
	}
}
