/*
 * ExtractorCSS
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
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.Processor;
import org.archive.util.DevUtils;
import org.archive.util.TextUtils;

/**
 * This extractor is parsing URIs from CSS type files.
 * The format of a CSS URL value is 'url(' followed by optional white space
 * followed by an optional single quote (') or double quote (") character
 * followed by the URL itself followed by an optional single quote (') or
 * double quote (") character followed by optional white space followed by ')'.
 * Parentheses, commas, white space characters, single quotes (') and double
 * quotes (") appearing in a URL must be escaped with a backslash:
 * '\(', '\)', '\,'. Partial URLs are interpreted relative to the source of
 * the style sheet, not relative to the document. <a href="http://www.w3.org/TR/REC-CSS1#url">
 * Source: www.w3.org</a>
 *
 * @author Igor Ranitovic
 *
 **/

public class ExtractorCSS extends Processor implements CoreAttributeConstants {
    private static Logger logger =
        Logger.getLogger("org.archive.crawler.extractor.ExtractorCSS");

    static final String ESCAPED_AMP = "&amp;";
    static final String BACKSLAH = "\\\\";
    /**
     *  CSS URL extractor pattern.
     *
     *  This pattern extracts URIs for CSS files
     **/
    static final String CSS_URI_EXTRACTOR =
        "url[(][\"\'\\s]{0,2}(([^\\\\\'\"\\s)]*(\\\\[\'\"\\s()])*)*)[\'\"\\s)]";

    private long numberOfCURIsHandled = 0;
    private long numberOfLinksExtracted = 0;

    /**
     * @param name
     */
    public ExtractorCSS(String name) {
        super(name, "CSS Extractor");
    }

    /**
     * @param curi
     */
    public void innerProcess(CrawlURI curi) {

        if (!curi.getAList().containsKey(A_HTTP_TRANSACTION)) {
            return;
        }

        GetMethod get =
            (GetMethod) curi.getAList().getObject(A_HTTP_TRANSACTION);
        Header contentType = get.getResponseHeader("Content-Type");
        if ((contentType == null)) {
            return;
        }
        String mimeType = contentType.getValue();
        if ((mimeType.toLowerCase().indexOf("css") < 0)
            && (!curi.getURIString().toLowerCase().endsWith(".css"))) {
            return;
        }
        numberOfCURIsHandled++;

        CharSequence cs =
            get.getHttpRecorder().getRecordedInput().getCharSequence();

        if (cs == null) {
            // TODO: note problem
            return;
        }

        try {
            Matcher uris = TextUtils.getMatcher(CSS_URI_EXTRACTOR, cs);
            while (uris.find()) {
                String cssUri = uris.group(1);
    			// Decode HTML entities
    			// TODO: decode more than just '&amp;' entity
    			cssUri = TextUtils.replaceAll(ESCAPED_AMP, cssUri, "&");
    			// Remove backslash(s), an escape character used in CSS URL
                cssUri = TextUtils.replaceAll(BACKSLAH, cssUri, "");
                numberOfLinksExtracted++;
                curi.addCSSLink(cssUri);
            }
            TextUtils.freeMatcher(uris);
        } catch (StackOverflowError e) {
            DevUtils.warnHandle(e, "ExtractorCSS StackOverflowError");
        }
        curi.linkExtractorFinished(); // Set flag to indicate that link extraction is completed.
    }

    public String report() {
        StringBuffer ret = new StringBuffer();
        ret.append("Processor: org.archive.crawler.extractor.ExtractorCSS\n");
        ret.append("  Function:          Link extraction on Cascading Style Sheets (.css)\n");
        ret.append("  CrawlURIs handled: " + numberOfCURIsHandled + "\n");
        ret.append("  Links extracted:   " + numberOfLinksExtracted + "\n\n");

        return ret.toString();
    }
}
