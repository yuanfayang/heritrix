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
 * Processes Javascript files for strings that are likely to be
 * crawlable URIs.
 *
 * @author gojomo
 *
 */
public class ExtractorJS extends Processor implements CoreAttributeConstants {
    private static Logger logger =
        Logger.getLogger("org.archive.crawler.extractor.ExtractorJS");

    static final String AMP = "&";
    static final String ESCAPED_AMP = "&amp;";
    static final String WHITESPACE = "\\s";

    // finds strings in Javascript
    // (areas between paired ' or " characters, possibly backslash-quoted
    // on the ends, but not in the middle)
    static final String JAVASCRIPT_STRING_EXTRACTOR =
     "(\\\\*(?:\"|\'))((?:[^\\n\\r]*?[^\\n\\r\\\\])??)(?:\\1)";

    // determines whether a string is likely URI
    // (no whitespace or '<' '>',  has an internal dot or some slash,
    // begins and ends with either '/' or a word-char)
    static final String STRING_URI_EXTRACTOR =
     "(\\w|/)[\\S&&[^<>]]*(\\.|/)[\\S&&[^<>]]*(\\w|/)";

    // finds strings in javascript likely to be URIs/paths
    // guessing based on '.' in string, so if highly likely to
    // get gifs/etc, unable to get many other paths
    // will find false positives
    // TODO: add '/' check, suppress strings being concatenated via '+'?
    static final String JAVASCRIPT_LIKELY_URI_EXTRACTOR =
     "(\\\\*\"|\\\\*\')(\\.{0,2}[^+\\.\\n\\r\\s\"\']+[^\\.\\n\\r\\s\"\']*(\\.[^\\.\\n\\r\\s\"\']+)+)(\\1)";

    protected long numberOfCURIsHandled = 0;
    protected static long numberOfLinksExtracted = 0;

    /**
     * @param name
     */
    public ExtractorJS(String name) {
        super(name, "JavaScript extractor. Link extraction on JavaScript" +
                " files (.js).");
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Processor#process(org.archive.crawler.datamodel.CrawlURI)
     */
    public void innerProcess(CrawlURI curi) {

        if (!curi.isHttpTransaction())
        {
            return;
        }

        String contentType = curi.getContentType();
        if ((contentType==null)) {
            return;
        }
        if((contentType.indexOf("javascript")<0)
            &&(contentType.indexOf("jscript")<0)
            &&(contentType.indexOf("ecmascript")<0)) {
            return;
        }

        this.numberOfCURIsHandled++;

        ReplayCharSequence cs = curi.getHttpRecorder().getReplayCharSequence();
        if (cs == null) {
            logger.warning("Failed getting ReplayCharSequence: " +
                curi.toString());
            return;
        }

        try {
            numberOfLinksExtracted += considerStrings(curi, cs, true);
        } catch (StackOverflowError e) {
            // TODO Auto-generated catch block
            DevUtils.warnHandle(e,"ExtractorJS StackOverflowError");
        }
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

    public static long considerStrings(
            CrawlURI curi, CharSequence cs, boolean handlingJSFile) {                
        long foundLinks = 0;
        Matcher strings = TextUtils.getMatcher(JAVASCRIPT_STRING_EXTRACTOR, cs);
        while(strings.find()) {
            CharSequence subsequence = 
                cs.subSequence(strings.start(2), strings.end(2));
                
            Matcher uri = 
                TextUtils.getMatcher(STRING_URI_EXTRACTOR, subsequence);
                
            if(uri.matches()) {
                String string = uri.group();
                string = TextUtils.replaceAll(ESCAPED_AMP, string, AMP);
                foundLinks++;
                if (handlingJSFile) {
                    curi.addLinkToCollection(string, A_JS_FILE_LINKS);
                } else {
                    curi.addLinkToCollection(string, A_HTML_SPECULATIVE_EMBEDS);
                }
            } else {
               foundLinks += considerStrings(curi,subsequence, handlingJSFile);
            }
            TextUtils.freeMatcher(uri);
        }
        TextUtils.freeMatcher(strings);
        return foundLinks;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Processor#report()
     */
    public String report() {
        StringBuffer ret = new StringBuffer();
        ret.append("Processor: org.archive.crawler.extractor.ExtractorJS\n");
        ret.append("  Function:          Link extraction on JavaScript code\n");
        ret.append("  CrawlURIs handled: " + numberOfCURIsHandled + "\n");
        ret.append("  Links extracted:   " + numberOfLinksExtracted + "\n\n");

        return ret.toString();
    }
}
