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
 * @author Igor Ranitovic
 *
 * This extractor is parsing URIs from CSS type files.
 * The format of a CSS URL value is 'url(' followed by optional white space 
 * followed by an optional single quote (') or double quote (") character 
 * followed by the URL itself followed by an optional single quote (') or 
 * double quote (") character followed by optional white space followed by ')'. 
 * Parentheses, commas, white space characters, single quotes (') and double
 * quotes (") appearing in a URL must be escaped with a backslash:
 * '\(', '\)', '\,'. Partial URLs are interpreted relative to the source of 
 * the style sheet, not relative to the document. Source: 
 * http://www.w3.org/TR/REC-CSS1 (see 6.4 URL)
 * 
 **/

public class ExtractorCSS extends Processor implements CoreAttributeConstants {
    private static Logger logger =
        Logger.getLogger("org.archive.crawler.extractor.ExtractorCSS");

    static final Pattern ESCAPED_AMP = Pattern.compile("&amp;");
    static final Pattern BACKSLAH = Pattern.compile("\\\\");
    // Regular expression that parses CSS URL uris
    static final Pattern CSS_URI_EXTRACTOR =
        Pattern.compile(
            "url[(][\"\'\\s]{0,2}(([^\\\\\'\"\\s)]*(\\\\[\'\"\\s()])*)*)[\'\"\\s)]");

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Processor#innerProcess(org.archive.crawler.datamodel.CrawlURI)
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
        if ((mimeType.indexOf("css") < 0)
            && (!curi.toString().endsWith(".css"))) {
            return;
        }

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
                cssUri = TextUtils.replaceAll(ESCAPED_AMP, cssUri, "&");
                cssUri = TextUtils.replaceAll(BACKSLAH, cssUri, "");
                curi.addCSSLink(cssUri);
            }
            TextUtils.freeMatcher(uris);
        } catch (StackOverflowError e) {
            DevUtils.warnHandle(e, "ExtractorCSS StackOverflowError");
        }
    }
}
