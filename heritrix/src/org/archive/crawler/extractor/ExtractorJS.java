/*
 * Created on Nov 17, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.archive.crawler.extractor;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.GetMethod;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.Processor;
import org.archive.util.DevUtils;
import org.archive.util.TextUtils;

/**
 * @author gojomo
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class ExtractorJS extends Processor implements CoreAttributeConstants {
	private static Logger logger = Logger.getLogger("org.archive.crawler.extractor.ExtractorJS");

	static final Pattern ESCAPED_AMP = Pattern.compile("&amp;");
	static final Pattern WHITESPACE = Pattern.compile("\\s");

	// finds strings in javascript likely to be URIs/paths
	// guessing based on '.' in string, so if highly likely to 
	// get gifs/etc, unable to get many other paths
	// will find false positives
	// TODO: add '/' check, suppress strings being concatenated via '+'?
	static final Pattern JAVASCRIPT_LIKELY_URI_EXTRACTOR = Pattern.compile(
	 "(\"|\')(\\.{0,2}[^+\\.\\n\\r\\s\"\']+[^\\.\\n\\r\\s\"\']*(\\.[^\\.\\n\\r\\s\"\']+)+)(\\1)");	

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Processor#process(org.archive.crawler.datamodel.CrawlURI)
	 */
	public void innerProcess(CrawlURI curi) {

		if(! curi.getAList().containsKey(A_HTTP_TRANSACTION)) {
			return;
		}
		
		GetMethod get = (GetMethod)curi.getAList().getObject(A_HTTP_TRANSACTION);
		Header contentType = get.getResponseHeader("Content-Type");
		if ((contentType==null)) {
			return;
		}
		String mimeType = contentType.getValue();
		if((mimeType.indexOf("javascript")<0)
		    &&(mimeType.indexOf("jscript")<0)
		    &&(mimeType.indexOf("ecmascript")<0)) {
			return;
		}
				
		CharSequence cs = get.getHttpRecorder().getRecordedInput().getCharSequence();
		
		if (cs==null) {
			// TODO: note problem
			return;
		}
		
		try {
			Matcher likelyUris = TextUtils.getMatcher(JAVASCRIPT_LIKELY_URI_EXTRACTOR, cs);
			while(likelyUris.find()) {
				String code = likelyUris.group(2);
				code = TextUtils.replaceAll(ESCAPED_AMP, code, "&");
				curi.addSpeculativeEmbed(code);
			}
			TextUtils.freeMatcher(likelyUris);
		} catch (StackOverflowError e) {
			// TODO Auto-generated catch block
			DevUtils.warnHandle(e,"ExtractorJS StackOverflowError");
		}
	}
}
