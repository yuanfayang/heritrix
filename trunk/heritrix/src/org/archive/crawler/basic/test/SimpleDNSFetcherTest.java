/*
 * Created on Jun 11, 2003
 *
 */
package org.archive.crawler.basic.test;

import junit.framework.*;
import org.archive.crawler.basic.SimpleDNSFetcher;
import org.archive.crawler.datamodel.*;
import org.xbill.DNS.*;
//import java.net.URI;

/**
 * @author parkert
 *
 */
public class SimpleDNSFetcherTest extends TestCase {

	SimpleDNSFetcher fetcher;
	CrawlURI curiBasic;
	CrawlURI curiWithHostPort;
	CrawlURI curiWithQuery;

	public static void main(String[] args) {
		TestResult result = new TestResult();
		
		Test t = suite();
		t.run(result);
	}
	
	public SimpleDNSFetcherTest(String s){
		super(s);
	}
	
	public static Test suite(){
		
		SimpleDNSFetcherTest tp 	= new SimpleDNSFetcherTest("testParsing");
		SimpleDNSFetcherTest ths 	= new SimpleDNSFetcherTest("testHostSeeding");
		
		TestSuite s = new TestSuite();
		s.addTest(tp);
		s.addTest(ths);
		
		return s;
	}


	public void setUp(){
		
		fetcher 	= new SimpleDNSFetcher();
		
		curiBasic 				= new CrawlURI( UURI.createUURI("dns:parkert.com") );
		curiWithHostPort 	= new CrawlURI( UURI.createUURI("dns://ns1.archive.org/parkert.com"));
		curiWithQuery 		= new CrawlURI( UURI.createUURI("dns:parkert.com?TYPE=A&CLASS=IN"));

		curiBasic.setHost( new CrawlHost("parkert.com"));
	}
	
	public void tearDown(){
	}
	
	// test the dns module's ability to parse different forms of dns uris
	public void testParsing(){
			String curiBasicStr 				= SimpleDNSFetcher.parseTargetDomain(curiBasic);
			String curiWithHostPortStr 	= SimpleDNSFetcher.parseTargetDomain(curiWithHostPort);
			String curiWithQueryStr	 		= SimpleDNSFetcher.parseTargetDomain(curiWithQuery);
			
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
		
		Record[] rrSet = (Record[])curiBasic.getAList().getObject(SimpleDNSFetcher.RRECORD_SET_LABEL);
		
		long expireTime = host.getIpExpires();
		long timestamp = curiBasic.getAList().getLong(SimpleDNSFetcher.DNSFETCH_TIMESTAMP_LABEL);
		
		// we should have at least one record
		assertTrue(rrSet.length > 0);
		
		assertTrue(expireTime >= 0);
		assertTrue(timestamp > 0);			

	}

}
