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

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.Processor;
import org.archive.io.ReplayCharSequence;
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
        super(name, "CSS Extractor. Extracts links from Cascading Style" +
                " Sheets (.css).");
    }

    /**
     * @param curi
     */
    public void innerProcess(CrawlURI curi) {

        if (!curi.isHttpTransaction()) {
            return;
        }

        if ((curi.getContentType() == null)) {
            return;
        }
        String mimeType = curi.getContentType();
        if ((mimeType.toLowerCase().indexOf("css") < 0)
            && (!curi.getURIString().toLowerCase().endsWith(".css"))) {
            return;
        }
        this.numberOfCURIsHandled++;

        ReplayCharSequence cs = curi.getHttpRecorder().getReplayCharSequence();
        if (cs == null) {
            logger.warning("Failed getting ReplayCharSequence: " +
                curi.toString());
            return;
        }
        this.numberOfLinksExtracted += processStyleCode(curi, cs);
        // Set flag to indicate that link extraction is completed.
        curi.linkExtractorFinished();
        // Done w/ the ReplayCharSequence.  Close it.
        if (cs != null) {
            try {
                cs.close();
            } catch (IOException ioe) {
                logger.warning(DevUtils.format(
                    "Failed close of ReplayCharSequence.", ioe));
            }
        }
    }

    public static long processStyleCode (CrawlURI curi, CharSequence cs) {
        long foundLinks = 0;
        try {
            Matcher uris = TextUtils.getMatcher(CSS_URI_EXTRACTOR, cs);
            while (uris.find()) {
                String cssUri = uris.group(1);
                // Escaping is now done in UURI.normalize();
                // cssUri = TextUtils.replaceAll(ESCAPED_AMP, cssUri, "&");

                // Remove backslash(s), an escape character used in CSS URL
                cssUri = TextUtils.replaceAll(BACKSLAH, cssUri, "");
                foundLinks++;
                curi.addLinkToCollection(cssUri, A_CSS_LINKS);
            }
            TextUtils.freeMatcher(uris);
        } catch (StackOverflowError e) {
            DevUtils.warnHandle(e, "ExtractorCSS StackOverflowError");
        }
        return foundLinks;
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
