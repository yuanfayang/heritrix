/*
 * Created on Jun 11, 2003
 *
 */
package org.archive.crawler.basic;

import java.net.URISyntaxException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlHost;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.UURI;
import org.xbill.DNS.Record;

/**
 * @author parkert
 *
 */
public class FetcherDNSTest extends TestCase implements CoreAttributeConstants {

	FetcherDNS fetcher;
	CrawlURI curiBasic;
	CrawlURI curiWithHostPort;
	CrawlURI curiWithQuery;

	public static void main(String[] args) {
		TestResult result = new TestResult();
		
		Test t = suite();
		t.run(result);
	}
	
	public FetcherDNSTest(String s){
		super(s);
	}
	
	public static Test suite(){
		
		FetcherDNSTest tp 	= new FetcherDNSTest("testParsing");
		FetcherDNSTest ths 	= new FetcherDNSTest("testHostSeeding");
		
		TestSuite s = new TestSuite();
		s.addTest(tp);
		s.addTest(ths);
		
		return s;
	}


	public void setUp(){
		
		fetcher 	= new FetcherDNS();
		
		try {
			curiBasic 				= new CrawlURI( UURI.createUURI("dns:parkert.com") );
			curiWithHostPort 	= new CrawlURI( UURI.createUURI("dns://ns1.archive.org/parkert.com"));
			curiWithQuery 		= new CrawlURI( UURI.createUURI("dns:parkert.com?TYPE=A&CLASS=IN"));
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		curiBasic.setHost( new CrawlHost("parkert.com"));
	}
	
	public void tearDown(){
	}
	
	// test the dns module's ability to parse different forms of dns uris
	public void testParsing(){
			String curiBasicStr 				= FetcherDNS.parseTargetDomain(curiBasic);
			String curiWithHostPortStr 	= FetcherDNS.parseTargetDomain(curiWithHostPort);
			String curiWithQueryStr	 		= FetcherDNS.parseTargetDomain(curiWithQuery);
			
			// make sure everything came out ok
		assertEquals(curiBasicStr, "parkert.com");
		assertEquals(curiWithHostPortStr, "parkert.com");
		assertEquals(curiWithQueryStr, "parkert.com");	
	}
	
	// test that the crawlhost is getting all the dns-related info 
	// it should be from the lookup (e.g. expire time)
	public void testHostSeeding(){
		
		fetcher.process(curiBasic);
		
		CrawlHost host = curiBasic.getHost();	
		
		Record[] rrSet = (Record[])curiBasic.getAList().getObject(A_RRECORD_SET_LABEL);
		
		long expireTime = host.getIpExpires();
		long timestamp = curiBasic.getAList().getLong(A_FETCH_BEGAN_TIME);
		
		// we should have at least one record
		assertTrue(rrSet.length > 0);
		
		assertTrue(expireTime >= 0);
		assertTrue(timestamp > 0);			

	}

}
