/*
 * Created on Jul 7, 2003
 *
 */
package org.archive.crawler.basic;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import org.archive.crawler.basic.ExtractorDOC;
import org.archive.crawler.datamodel.*;
import org.archive.crawler.framework.*;

//import org.apache.commons.httpclient.methods.GetMethod;
import org.archive.crawler.util.OfflineGet;
import org.apache.commons.httpclient.HttpClient;

import java.net.*;
import java.util.ArrayList;


/**
 * @author Parker Thompson
 *
 */
public class ExtractorDOCTest
	extends TestCase
	implements CoreAttributeConstants {

		protected ExtractorDOC extractor = null;
		protected CrawlController controller = null;
		protected CrawlURI curi = null;
		protected CrawlHost crawlhost = null;
		protected OfflineGet get = null;
		HttpClient http = new HttpClient();
		
		public static void main(String[] args) {	
			TestResult result = new TestResult();
		
			Test t = suite();
			t.run(result);
		}
		
		public ExtractorDOCTest(String s){
			super(s);
		}
		
		public static Test suite(){
		
			ExtractorDOCTest one = new ExtractorDOCTest("testGetLinks");
		
			TestSuite s = new TestSuite();
			s.addTest(one);
		
			return s;
		}
		
		// see if we get all the links that should be in the test document
		public void testGetLinks(){
			
			extractor.process(curi);
			
			ArrayList list = (ArrayList)curi.getAList().getObject("html-links");
			
			// make sure there are three links
			assertTrue(list.size() == 4);
			
		}
	
		// initialize any variables used in the tests here
		//TODO figure out how to generate a get object we can process with ExtractorDOC
		public void setUp(){
		
			curi 			= new CrawlURI("http://parkert.com");
			extractor = new ExtractorDOC();
			controller = null;
			get = new OfflineGet();
			
			get.setResponseBody("./test-config/sampledocuments/test.doc");
			get.setResponseHeader("Content-Type", "application/msword");
		
			curi.getAList().putString("content-type", "application/msword");
			curi.getAList().putObject(A_HTTP_TRANSACTION, get);		
		}
		
		// tear it all down
		public void tearDown(){
		}
		
		


}
