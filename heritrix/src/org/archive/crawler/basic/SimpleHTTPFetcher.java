/*
 * SimpleHTTPFetcher.java
 * Created on Jun 5, 2003
 *
 * $Header$
 */
package org.archive.crawler.basic;

import java.io.IOException;
import java.util.logging.Logger;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.Processor;

/**
 * @author gojomo
 *
 */
public class SimpleHTTPFetcher extends Processor {
	private static Logger logger = Logger.getLogger("org.archive.crawler.basic.SimpleHTTPFetcher");
	HttpClient http = new HttpClient();
	
	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Processor#process(org.archive.crawler.datamodel.CrawlURI)
	 */
	public void process(CrawlURI curi) {
		super.process(curi);
		if(!curi.getUURI().getUri().getScheme().equals("http")) {
			return;
		}
		GetMethod get = new GetMethod(curi.getUURI().getUri().toASCIIString());
		get.setFollowRedirects(true);
		get.setRequestHeader("User-Agent","Heritrix 0.1 (+gojomo@archive.org)");
		try {
			http.executeMethod(get);
		} catch (HttpException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		logger.info(
			curi.getUURI().getUri()+": "
			+get.getStatusCode()+" "
			+get.getResponseBody().length);

		// TODO consider errors
		curi.getAList().putObject("http-transaction",get);
		
	}

}
