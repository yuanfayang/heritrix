/*
 * SimpleHTTPFetcher.java
 * Created on Jun 5, 2003
 *
 * $Header$
 */
package org.archive.crawler.basic;

import java.io.IOException;
import java.util.logging.Logger;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.InstancePerThread;
import org.archive.crawler.framework.Processor;

/**
 * @author gojomo
 *
 */
public class SimpleHTTPFetcher extends Processor implements InstancePerThread, org.archive.crawler.datamodel.CoreAttributeConstants {
	private static Logger logger = Logger.getLogger("org.archive.crawler.basic.SimpleHTTPFetcher");
	HttpClient http = new HttpClient();
	
	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Processor#process(org.archive.crawler.datamodel.CrawlURI)
	 */
	public void process(CrawlURI curi) {
		super.process(curi);
		if(!curi.getUURI().getUri().getScheme().equals("http")) {
			// only handles plain http for now
			return;
		}
		
		curi.getAList().putLong(FETCH_BEGAN_AT,System.currentTimeMillis());
		GetMethod get = new GetMethod(curi.getUURI().getUri().toASCIIString());
		get.setFollowRedirects(false);
		get.setRequestHeader("User-Agent",controller.getOrder().getBehavior().getUserAgent());
		try {
			http.executeMethod(get);

			Header contentLength = get.getResponseHeader("Content-Length");
			logger.info(
				curi.getUURI().getUri()+": "
				+get.getStatusCode()+" "
				+(contentLength==null ? "na" : contentLength.getValue()));

			// TODO consider errors more carefully
			curi.getAList().putObject("http-transaction",get);
			curi.getAList().putLong("http-complete-time",System.currentTimeMillis());
			Header ct = get.getResponseHeader("content-type");
			if ( ct!=null ) {
				curi.getAList().putString("content-type", ct.getValue());
			}
			
		} catch (HttpException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
