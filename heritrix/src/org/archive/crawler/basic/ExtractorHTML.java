/*
 * SimpleHTMLExtractor.java
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
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.Processor;

/**
 * Basic link-extraction, from an HTML content-body, 
 * using regular expressions. 
 *
 * @author gojomo
 *
 */
public class ExtractorHTML extends Processor implements CoreAttributeConstants {
	// TODO: correct this so that it handles name= and content= in alternate order, with intervening spaces, etc.
	static Pattern META_ROBOTS_EXTRACTOR = Pattern.compile(
	 "(?i)<meta\\s[^<]*?name=(?:\"|')robots(?:\"|')[^<]*?(?:content=(?:\"|')([^\"^']*)(?:\"|'))[^<]*>"
	);
	static Pattern BASE_EXTRACTOR = Pattern.compile(
	 "(?i)<base[^<]*\\s(?:href)=(?:(?:\"([^>\"]*)\")|(?:'([^>']*)')|(^\\[\\S&&[^>]]*))(?:[^>]+)*>"
	);
	static Pattern LINK_EXTRACTOR = Pattern.compile(
	 "(?i)<[^<]+\\s(?:href)=(?:(?:\"([^>\"]*)\")|(?:'([^>']*)')|(^\\[\\S&&[^>]]*))(?:[^>]+)*>"
	);
	static Pattern EMBED_EXTRACTOR = Pattern.compile(
	 "(?i)<[^<]+\\s(?:src)=(?:(?:\"([^>\"]*)\")|(?:'([^>']*)')|(^\\[\\S&&[^>]]*))(?:[^>]+)*>"
	);

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Processor#process(org.archive.crawler.datamodel.CrawlURI)
	 */
	public void innerProcess(CrawlURI curi) {

		if(! curi.getAList().containsKey(A_HTTP_TRANSACTION)) {
			return;
		}
		GetMethod get = (GetMethod)curi.getAList().getObject(A_HTTP_TRANSACTION);
		Header contentType = get.getResponseHeader("Content-Type");
		if ((contentType==null)||(!contentType.getValue().startsWith("text/html"))) {
			// nothing to extract for other types here
			return; 
		}
		
		ArrayList links  = new ArrayList();
		ArrayList embeds = new ArrayList();
		
		String cb = get.getResponseBodyAsString();
		
		if (cb==null) {
			// TODO: note problem
			return;
		}
		// extract meta robots
		Matcher m = META_ROBOTS_EXTRACTOR.matcher(cb);
		if(m.find()) {
			String directives = m.group(1).toLowerCase();
			curi.getAList().putString(A_META_ROBOTS,directives);
			// TODO handle multiple, name= qualifiers
			if(directives.indexOf("nofollow")>0) {
				// if 'nofollow' is specified, end html extraction
				return;
			}
		}
		
		// extract BASE HREF
		Matcher b = BASE_EXTRACTOR.matcher(cb);
		if(b.find()) {
			String baseHref;
			if (b.group(1)!=null) {
				// "" quoted attribute
				baseHref = b.group(1);
			} else if (b.group(2)!=null) {
				//   quoted attribute
				baseHref = b.group(2);
			} else {
				// unquoted attribute
				baseHref = b.group(3);
			}
			curi.getAList().putString("html-base-href",baseHref);
		}
		
		// extract links
		Matcher l = LINK_EXTRACTOR.matcher(cb);
		while(l.find()) {
			String match;
			if (l.group(1)!=null) {
				// "" quoted attribute
				match = l.group(1);
			} else if (l.group(2)!=null) {
				//   quoted attribute
				match = l.group(2);
			} else {
				// unquoted attribute
				match = l.group(3);
			}
			links.add(match);
		}
		
		if(links.size()>0) {
			curi.getAList().putObject("html-links", links);
		}

		// extract embeds
		Matcher e = EMBED_EXTRACTOR.matcher(cb);
		while(e.find()) {
			String match;
			if (e.group(1)!=null) {
				// "" quoted attribute
				match = e.group(1);
			} else if (e.group(2)!=null) {
				//   quoted attribute
				match = e.group(2);
			} else {
				// unquoted attribute
				match = e.group(3);
			}
			embeds.add(match);
		}
		
		if(embeds.size()>0) {
			curi.getAList().putObject("html-embeds", embeds);
		}
	}

}
