/*
 * Created on Jun 12, 2003
 *
 */
package org.archive.crawler.basic;

import junit.framework.*;

import java.io.IOException;
import java.util.Date;

import org.archive.crawler.basic.ARCWriter; 
import org.archive.crawler.basic.FetcherHTTPSimple;
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
public class ARCWriterTest extends TestCase implements CoreAttributeConstants{
	
	ARCWriter writer;
	FetcherHTTPSimple fetcher;
	CrawlURI curi;
	CrawlController controller;
	String orderFile;
	CrawlOrder order;
	CrawlServer crawlserver;
	
	public static void main(String[] args) {	
		ARCWriterTest a = new ARCWriterTest("testHttpWrite");
	}
	
	// initialize any variables used in the tests here
	public void setUp(){
		
		writer 		= new ARCWriter();
		curi 			= new CrawlURI("http://parkert.com");
		crawlserver	= new CrawlServer("parkert.com");
		
		writer.setArcMaxSize(10000);
		writer.setArcPrefix("prefix");
		writer.setOutputDir(".");
		writer.setUseCompression(true);
		
		try{
		writer.createNewArcFile();
		}catch(IOException e){}
		
		try{
		crawlserver.getHost().setIP(InetAddress.getByName("parkert.com"));
		}catch(UnknownHostException e){}
		
		crawlserver.getHost().setHasBeenLookedUp();
		
		try {
			crawlserver.getHost().setIP( InetAddress.getByName("parkert.com") );
			curi.setServer(crawlserver);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		curi.getAList().putString("content-type", "text/html");
		curi.getAList().putLong(A_FETCH_BEGAN_TIME, (new Date()).getTime());
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
			//writer.setOutputDir(new String("./arcs/"));
			writer.process(curi);
	
			System.out.println("test ran, check " + writer.getOutputDir() + " for resulting arc(s).");
						
		} catch (Exception e) {	
			System.out.println("error fetching page");
			e.printStackTrace();
		}
		

	}
	
}
