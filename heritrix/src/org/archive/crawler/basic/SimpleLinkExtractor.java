/*
 * SimpleLinkExtractor.java
 * Created on Jun 5, 2003
 *
 * $Header$
 */
package org.archive.crawler.basic;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.GetMethod;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.Processor;

/**
 * @author gojomo
 *
 */
public class SimpleLinkExtractor extends Processor {

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Processor#process(org.archive.crawler.datamodel.CrawlURI)
	 */
	public void process(CrawlURI curi) {
		super.process(curi);
		if(curi.getAList().containsKey("http-transaction")) {
			GetMethod get = (GetMethod)curi.getAList().getObject("http-transaction");
			
			processHeaders(curi, get);
			processContentBody(curi, get);
		}
		
		
	}
	/**
	 * @param curi
	 * @param get
	 */
	private void processContentBody(CrawlURI curi, GetMethod get) {
		Header contentType = get.getResponseHeader("Content-Type");
		if ((contentType==null)||(!contentType.getValue().startsWith("text/html"))) {
			// nothing to extract for other types yet
			return; 
		}
		
		ArrayList links  = new ArrayList();
		ArrayList embeds = new ArrayList();
		
		String cb = get.getResponseBodyAsString();
		
		// extract BASE HREF
		Pattern baseExtractor = Pattern.compile(
			"(?i)<base[^<]*\\s(?:href)=(?:(?:\"([^>\"]*)\")|([\\S&&[^>]]*))(?:[^>]+)*>");
		Matcher b = baseExtractor.matcher(cb);
		if(b.find()) {
			String baseHref;
			if(b.group(1)!=null) {
				// quoted href
				baseHref = b.group(1);
			} else {
				baseHref = b.group(2);
			}
			curi.getAList().putString("html-base-href",baseHref);
		}
		
		// extract links
		Pattern linkExtractor = Pattern.compile(
		 "(?i)<[^<]+\\s(?:href)=(?:(?:\"([^>\"]*)\")|([\\S&&[^>]]*))(?:[^>]+)*>"
		);
		Matcher l = linkExtractor.matcher(cb);
		while(l.find()) {
			String match;
			if (l.group(1)!=null) {
				// quoted attribute
				match = l.group(1);
			} else {
				// unquoted attribute
				match = l.group(2);
			}
			links.add(match);
		}
		
		if(links.size()>0) {
			curi.getAList().putObject("html-links", links);
		}

		// extract embeds
		Pattern embedExtractor = Pattern.compile(
		 "(?i)<[^<]+\\s(?:src)=(?:(?:\"([^>\"]*)\")|([\\S&&[^>]]*))(?:[^>]+)*>"
		);
		Matcher e = embedExtractor.matcher(cb);
		while(e.find()) {
			String match;
			if (e.group(1)!=null) {
				// quoted attribute
				match = e.group(1);
			} else {
				// unquoted attribute
				match = e.group(2);
			}
			embeds.add(match);
		}
		
		if(embeds.size()>0) {
			curi.getAList().putObject("html-embeds", embeds);
		}
	}
	
	private void processHeaders(CrawlURI curi, GetMethod get) {
		// read URIs from headers
		ArrayList uris = new ArrayList();
		Header loc = get.getResponseHeader("Location");
		if ( loc != null ) {
			uris.add(loc.getValue());
		} 
		loc = get.getResponseHeader("Content-Location");
		if ( loc != null ) {
			uris.add(loc.getValue());
		} 
		// TODO: consider possibility of multiple headers
		if(uris.size()>0) {
			curi.getAList().putObject("uris-from-headers", uris);
		}
	}

}
