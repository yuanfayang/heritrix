/*
 * Created on Jul 7, 2003
 *
 */
package org.archive.crawler.basic;

import java.util.ArrayList;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import org.apache.commons.httpclient.HttpClient;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlServer;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.extractor.ExtractorDOC;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.util.OfflineGet;


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
		protected CrawlServer crawlhost = null;
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
