/* Copyright (C) 2003 Internet Archive.
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
 *
 *
 */
package org.archive.crawler.extractor;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.GetMethod;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.util.TextUtils;

/**
 * Extended version of ExtractorHTML that handle HTML STYLE tags
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
    static final Pattern BACKSLAH = Pattern.compile("\\\\");
    // Regular expression that parses URIs for the CSS URL syntax
    static final Pattern CSS_URI_EXTRACTOR =
        Pattern.compile(
            "url[(][\"\'\\s]{0,2}(([^\\\\\'\"\\s)]*(\\\\[\'\"\\s()])*)*)[\'\"\\s)]");

    static final Pattern RELEVANT_TAG_EXTRACTOR = Pattern.compile(
     "(?is)<(?:((script.*?)>.*?</script)|((style.*?)>.*?</style)|(((meta)|(?:\\w+))\\s+.*?)|(!--.*?--))>");
    // groups:
    // 1: SCRIPT SRC=blah>blah</SCRIPT 
    // 2: just script open tag 
    // 3: STYLE TYPE=blah>blah</STYLE 
    // 4: just style open tag
    // 5: entire other tag, without '<' '>'
    // 6: element 
    // 7: META
    // 8: !-- blahcomment -- 


	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Processor#process(org.archive.crawler.datamodel.CrawlURI)
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
            curi.addCSSLink(caUri);
        }
        TextUtils.freeMatcher(candidates);
        
    }

}

