/* 
 * ExtractorHTML2
 * 
 * $Id$
 * 
 * Created on Jan 6, 2004
 *
 * Copyright (C) 2004 Internet Archive.
 *
 * This file is part of the Heritrix web crawler (crawler.archive.org).
 *
 * Heritrix is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * any later version.
 *
 * Heritrix is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with Heritrix; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.archive.crawler.extractor;

import java.util.logging.Logger;
import java.util.regex.Matcher;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.GetMethod;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.util.TextUtils;

/**
 * Extended version of ExtractorHTML that handles HTML STYLE tags
 * and has a more aggressive javascript link extraction where
 * javascript code is parsed first with general HTML tags regexp,
 * than by javascript speculative link regexp. 
 * 
 * @author Igor Ranitovic
 *
 * TODO: more testing
 */
public class ExtractorHTML2 extends ExtractorHTML {
    static Logger logger = Logger.getLogger("org.archive.crawler.extractor.ExtractorHTML");
	static final String BACKSLAH = "\\\\";
	/** Regular expression that parses URIs for the CSS URL syntax */
	static final String CSS_URI_EXTRACTOR =
        "url[(][\"\'\\s]{0,2}(([^\\\\\'\"\\s)]*(\\\\[\'\"\\s()])*)*)[\'\"\\s)]";

	/**
     * Compiled relevant tag extractor.
     *
	 * This pattern extracts either:<br>
	 * (1) whole &lt;script&gt;...&lt;/script&gt; or<br> 
	 * (2) &lt;style&gt;...&lt;/style&gt; or<br>
	 * (3) &lt;meta ...&gt; or<br>
	 * (3) any other open-tag with at least one attribute<br>
	 * (eg matches "&lt;a href='boo'&gt;" but not "&lt;/a&gt;" or "&lt;br&gt;")<br>
	 * groups:<br>
	 * 1: SCRIPT SRC=foo&gt;boo&lt;/SCRIPT<br> 
	 * 2: just script open tag<br>
	 * 3: STYLE TYPE=moo&gt;zoo&lt;/STYLE<br> 
	 * 4: just style open tag<br>
	 * 5: entire other tag, without '<' '>'<br>
	 * 6: element<br>
	 * 7: META<br>
	 * 8: !-- comment --<br>
	 */
	static final String RELEVANT_TAG_EXTRACTOR =
	 "(?is)<(?:((script.*?)>.*?</script)|((style.*?)>.*?</style)|(((meta)|(?:\\w+))\\s+.*?)|(!--.*?--))>";

    /**
     * @param name
     */
    public ExtractorHTML2(String name) {
        super(name);
    }

	/** 
	 * @param curi
	 */
	public void innerProcess(CrawlURI curi) {

		if(! curi.getAList().containsKey(A_HTTP_TRANSACTION)) {
			return;
		}
		
		if(ignoreUnexpectedHTML) {
			if(!expectedHTML(curi)) {
				// HTML was not expected (eg a GIF was expected) so ignore 
				// (as if a soft 404)
				return;
			}
		}
				
		GetMethod get = (GetMethod)curi.getAList().getObject(A_HTTP_TRANSACTION);
		Header contentType = get.getResponseHeader("Content-Type");
		if ((contentType==null)||(!contentType.getValue().startsWith("text/html"))) {
			// nothing to extract for other types here
			return; 
		}
			
        numberOfCURIsHandled++;
				
		CharSequence cs = get.getHttpRecorder().getRecordedInput().getCharSequence();
		
		if (cs==null) {
			// TODO: note problem
			return;
		}
		
		Matcher tags = TextUtils.getMatcher(RELEVANT_TAG_EXTRACTOR, cs);
		while(tags.find()) {
			if (tags.start(8) > 0) {
				// comment match
				// for now do nothing
			} else if (tags.start(7) > 0) {
			// <meta> match
				if (processMeta(curi,cs.subSequence(tags.start(5), tags.end(5)))) {
					// meta tag included NOFOLLOW; abort processing
					TextUtils.freeMatcher(tags);
					return;
				}
			} else if (tags.start(5) > 0) {
				// generic <whatever> match
				processGeneralTag(
					curi,
					cs.subSequence(tags.start(6),tags.end(6)),
					cs.subSequence(tags.start(5),tags.end(5)));
			} else if (tags.start(1) > 0) {
				// <script> match
				processScript(curi, cs.subSequence(tags.start(1), tags.end(1)), tags.end(2)-tags.start(1));
			} else if (tags.start(3) > 0){
				// <style... match
				processStyle(curi, cs.subSequence(tags.start(3), tags.end(3)), tags.end(4)-tags.start(3));
                
			}
		}
		TextUtils.freeMatcher(tags);
        curi.linkExtractorFinished(); // Set flag to indicate that link extraction is completed.
	}
		
	
	/**
	 * @param curi
	 * @param sequence
	 */
	protected void processScript(CrawlURI curi, CharSequence sequence, int endOfOpenTag) {
		// for now, do nothing
		// TODO: best effort extraction of strings
		
		// first, get attributes of script-open tag
		// as per any other tag
		processGeneralTag(curi,sequence.subSequence(0,6),sequence.subSequence(0,endOfOpenTag));
		// then, proccess entire javascript code as html code
		// this may cause a lot of false positves 
		processGeneralTag(curi,sequence.subSequence(0,6),sequence.subSequence(endOfOpenTag,sequence.length()));		
		// finally, apply best-effort string-analysis heuristics 
		// against any code present (false positives are OK)
		processScriptCode(curi,sequence.subSequence(endOfOpenTag,sequence.length()));
	}

	/**
	 * @param curi
	 * @param sequence
	 * @param endOfOpenTag
	 */
	protected void processStyle(CrawlURI curi, CharSequence sequence, int endOfOpenTag) {
		// first, get attributes of script-open tag
		// as per any other tag
		processGeneralTag(curi,sequence.subSequence(0,6),sequence.subSequence(0,endOfOpenTag));
        
		// then, parse for URI
		processStyleCode(curi,sequence.subSequence(endOfOpenTag,sequence.length()));
	}
	/**
	 * @param curi
	 * @param cs
	 */
	protected void processStyleCode(CrawlURI curi, CharSequence cs) {
		String code = cs.toString();
		Matcher candidates = TextUtils.getMatcher(CSS_URI_EXTRACTOR, code);
		String caUri = ""; // candidate uri
		while (candidates.find()) {
			caUri = candidates.group(1);
			caUri = TextUtils.replaceAll(ESCAPED_AMP, caUri, "&"); // TODO: more HTML deescaping?
			caUri = TextUtils.replaceAll(BACKSLAH, caUri, "");
			logger.finest("stlye: " + caUri + " from " + curi);
            numberOfLinksExtracted++;
            curi.addCSSLink(caUri);
		}
		TextUtils.freeMatcher(candidates);
        
	}

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Processor#report()
     */
    public String report() {
        StringBuffer ret = new StringBuffer();
        ret.append("Processor: org.archive.crawler.extractor.ExtractorHTML2\n");
        ret.append("  Function:          Link extraction on HTML documents (including embedded CSS)\n");
        ret.append("  CrawlURIs handled: " + numberOfCURIsHandled + "\n");
        ret.append("  Links extracted:   " + numberOfLinksExtracted + "\n\n");
        
        return ret.toString();
    }


}
