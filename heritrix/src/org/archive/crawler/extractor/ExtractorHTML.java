/*
 * SimpleHTMLExtractor.java
 * Created on Jun 5, 2003
 *
 * $Header$
 */
package org.archive.crawler.extractor;

import java.util.ArrayList;
import java.util.logging.Logger;
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
	private static Logger logger = Logger.getLogger("org.archive.crawler.basic.ExtractorHTML");
	
//	// TODO: correct this so that it handles name= and content= in alternate order, with intervening spaces, etc.
//	static Pattern META_ROBOTS_EXTRACTOR = Pattern.compile(
//	 "(?i)<meta\\s[^<]*?name=(?:\"|')robots(?:\"|')[^<]*?(?:content=(?:\"|')([^\"^']*)(?:\"|'))[^<]*>"
//	);
//	static Pattern BASE_EXTRACTOR = Pattern.compile(
//	 "(?i)<base[^<]*\\s(?:href)=(?:(?:\"([^>\"]*)\")|(?:'([^>']*)')|(^\\[\\S&&[^>]]*))(?:[^>]+)*>"
//	);
//	static Pattern LINK_EXTRACTOR = Pattern.compile(
//	 "(?i)<[^<]+\\s(?:href)=(?:(?:\"([^>\"]*)\")|(?:'([^>']*)')|(^\\[\\S&&[^>]]*))(?:[^>]+)*>"
//	);
//	static Pattern EMBED_EXTRACTOR = Pattern.compile(
//	 "(?i)<[^<]+\\s(?:src)=(?:(?:\"([^>\"]*)\")|(?:'([^>']*)')|(^\\[\\S&&[^>]]*))(?:[^>]+)*>"
//	);

    // this pattern extracts either (1) whole <script>...</script>
    // ranges; or (2) any other open-tag with at least one attribute
    // (eg matches "<a href='boo'>" but not "</a>" or "<br>")
	static Pattern RELEVANT_TAG_EXTRACTOR = Pattern.compile(
	 "(?is)<(?:(script.*?>.*?</script)|((?:(base)|(meta)|(\\w+))\\s+.*?)|((!--).*?--))>");
	// this pattern extracts 'href' or 'src' attributes from 
	// any open-tag innards matched by the above
	static Pattern RELEVANT_ATTRIBUTE_EXTRACTOR = Pattern.compile(
	 "(?is)(\\w+)(?:\\s+|(?:\\s.*?\\s))(?:(href)|(src))\\s*=(?:(?:\\s*\"(.+?)\")|(?:\\s*'(.+?)')|(\\S+))");
	// this pattern extracts 'robots' attributes
	static Pattern ROBOTS_ATTRIBUTE_EXTRACTOR = Pattern.compile(
	 "(?is)(\\w+)\\s+.*?(?:(robots))\\s*=(?:(?:\\s*\"(.+)\")|(?:\\s*'(.+)')|(\\S+))");

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
		
		CharSequence cs = get.getResponseBodyAsString();
		
		if (cs==null) {
			// TODO: note problem
			return;
		}
		
		Matcher tags = RELEVANT_TAG_EXTRACTOR.matcher(cs);
		while(tags.find()) {
			if(tags.start(5)>0) {
				// generic <whatever> match
				processTagInto(cs.subSequence(tags.start(2),tags.end(2)),links,embeds);
			} else if(tags.start(3)>0) {
				// <base> match
				processBase(curi,cs.subSequence(tags.start(2),tags.end(2)));
			} else if (tags.start(4)>0) {
				// <meta> match
				if (processMeta(curi,cs.subSequence(tags.start(2),tags.end(2)))) {
					// meta tag included NOFOLLOW; abort processing
					return;
				}
			} else if (tags.start(1)>0) {
				// <script> match
				processScript(curi,cs.subSequence(tags.start(1),tags.end(1)));
			}
		}
		
		if(links.size()>0) {
			curi.getAList().putObject("html-links", links);
			logger.fine(curi+" has "+links.size()+" links.");
		}
		
		if(embeds.size()>0) {
			curi.getAList().putObject("html-embeds", embeds);
			logger.fine(curi+" has "+embeds.size()+" embeds.");
	
		}
	}
		
		
	/**
	 * @param curi
	 * @param sequence
	 */
	private void processScript(CrawlURI curi, CharSequence sequence) {
		// for now, do nothing
		// TODO: best effort extraction of strings
	}


	/**
	 * @param curi
	 * @param sequence
	 */
	private boolean processMeta(CrawlURI curi, CharSequence cs) {
		Matcher attr = ROBOTS_ATTRIBUTE_EXTRACTOR.matcher(cs);
		if(!attr.lookingAt()) {
			// no robots
			return false;
		}
		int which = attr.start(3) > 0 ? 3 : attr.start(4) > 0 ? 4 : 5;
		String directives = attr.group(which);
		curi.getAList().putString(A_META_ROBOTS,directives);
		if(directives.indexOf("nofollow")>0) {
			// if 'nofollow' is specified, end html extraction
			return true;
		}
		return false;
	}


	/**
	 * @param curi
	 * @param sequence
	 */
	private void processBase(CrawlURI curi, CharSequence cs) {
		Matcher attr = RELEVANT_ATTRIBUTE_EXTRACTOR.matcher(cs);
		if(!attr.lookingAt()) {
			// no href or src
			return;
		}
		int which = attr.start(4) > 0 ? 4 : attr.start(5) > 0 ? 5 : 6;
		String baseHref = attr.group(which);
		if (attr.start(2)>0) {
			// HREF match
			curi.getAList().putString(A_HTML_BASE,baseHref);
		} 
	}


	/**
	 * @param tags
	 * @param i
	 * @param links
	 * @param embeds
	 */
	private void processTagInto(CharSequence cs, ArrayList links, ArrayList embeds) {
		Matcher attr = RELEVANT_ATTRIBUTE_EXTRACTOR.matcher(cs);
		if(!attr.lookingAt()) {
			// no href or src
			return;
		}
		int which = attr.start(4) > 0 ? 4 : attr.start(5) > 0 ? 5 : 6;
		String uri = attr.group(which);
		if (attr.start(2)>0) {
			// HREF match
			links.add(uri);
		} else if (attr.start(3)>0) {
			// SRC match
			embeds.add(uri);
		}
	}
}

