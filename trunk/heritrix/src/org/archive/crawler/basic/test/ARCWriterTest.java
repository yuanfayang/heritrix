/*
 * Created on Jun 12, 2003
 *
 */
package org.archive.crawler.basic.test;

import junit.framework.*;

import org.archive.crawler.basic.ARCWriter; 
import org.archive.crawler.basic.SimpleHTTPFetcher;
import org.archive.crawler.datamodel.*;
import org.archive.crawler.framework.*;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.*;

/**
 * @author Parker Thompson
 *  
 */
public class ARCWriterTest extends TestCase {
	
	ARCWriter writer;
	SimpleHTTPFetcher fetcher;
	CrawlURI curi;
	CrawlController controller;
	String orderFile;
	CrawlOrder order;
	CrawlHost crawlhost;
	
	public static void main(String[] args) {	
		ARCWriterTest a = new ARCWriterTest("testHttpWrite");
	}
	
	// initialize any variables used in the tests here
	public void setUp(){
		
		writer 		= new ARCWriter();
		curi 			= new CrawlURI("http://parkert.com");
		crawlhost	= new CrawlHost("parkert.com");
		
		try {
			crawlhost.setIP( InetAddress.getByName("parkert.com") );
			curi.setHost(crawlhost);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		curi.getAList().putString("content-type", "text/html");
	}
	
	// clean up after the test here if necessary
	public void tearDown(){
		
	}
	
	public ARCWriterTest(String s){
		super(s);
	}
	
	public void testHttpWrite(){
		
		HttpClient http = new HttpClient();
		
		GetMethod get = new GetMethod(curi.getUURI().getUri().toASCIIString());
		get.setFollowRedirects(false);
		get.setRequestHeader("User-Agent","ARCWriterTest");
		
		try {
			http.executeMethod(get);
			Header contentLength = get.getResponseHeader("Content-Length");
			
			curi.getAList().putObject("http-transaction",get);

			//fetcher.process(curi);
			writer.setOutputDir(new String("/home/parkert/"));
			writer.writeHttp(curi);
	
			System.out.println("test ran, check " + writer.getOutputDir() + " for resulting arc(s).");
						
		} catch (Exception e) {	
			System.out.println("error fetching page");
			e.printStackTrace();
		}
		

	}
	
}
