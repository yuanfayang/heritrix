/*
 * SimpleHTTPExtractor.java
 * Created on Jul 3, 2003
 *
 * $Header$
 */
package org.archive.crawler.basic;

import java.util.ArrayList;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.GetMethod;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.Processor;

/**
 * @author gojomo
 *
 */
public class ExtractorHTTP extends Processor implements CoreAttributeConstants {

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Processor#process(org.archive.crawler.datamodel.CrawlURI)
	 */
	public void innerProcess(CrawlURI curi) {

		if(curi.getAList().containsKey(A_HTTP_TRANSACTION)) {
			GetMethod get = (GetMethod)curi.getAList().getObject(A_HTTP_TRANSACTION);
			CrawlURI curi1 = curi;
			GetMethod get1 = get;
			
			ArrayList uris = new ArrayList();
			Header loc = get1.getResponseHeader("Location");
			if ( loc != null ) {
				uris.add(loc.getValue());
			} 
			loc = get1.getResponseHeader("Content-Location");
			if ( loc != null ) {
				uris.add(loc.getValue());
			} 
			// TODO: consider possibility of multiple headers
			if(uris.size()>0) {
				curi1.getAList().putObject("uris-from-headers", uris);
			}
		}
	}
}
