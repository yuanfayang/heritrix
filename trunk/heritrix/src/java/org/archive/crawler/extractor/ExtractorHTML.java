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
 * SimpleHTMLExtractor.java
 * Created on Jun 5, 2003
 *
 * $Header$
 */
package org.archive.crawler.extractor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.RobotsHonoringPolicy;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.datamodel.UURIFactory;
import org.archive.crawler.framework.Processor;
import org.archive.io.ReplayCharSequence;
import org.archive.util.DevUtils;
import org.archive.util.TextUtils;

/**
 * Basic link-extraction, from an HTML content-body,
 * using regular expressions.
 *
 * @author gojomo
 *
 */
public class ExtractorHTML extends Processor implements CoreAttributeConstants {

    protected boolean ignoreUnexpectedHTML = true; // TODO: add config param to change

    private static Logger logger =
        Logger.getLogger("org.archive.crawler.extractor.ExtractorHTML");

    /**
     * Compiled relevant tag extractor.
     *
     * <p>
     * This pattern extracts either:
     * <li> (1) whole &lt;script&gt;...&lt;/script&gt; or
     * <li> (2) &lt;style&gt;...&lt;/style&gt; or
     * <li> (3) &lt;meta ...&gt; or
     * <li> (4) any other open-tag with at least one attribute
     * (eg matches "&lt;a href='boo'&gt;" but not "&lt;/a&gt;" or "&lt;br&gt;")
     * <p>
     * groups:
     * <li> 1: SCRIPT SRC=foo&gt;boo&lt;/SCRIPT
     * <li> 2: just script open tag
     * <li> 3: STYLE TYPE=moo&gt;zoo&lt;/STYLE
     * <li> 4: just style open tag
     * <li> 5: entire other tag, without '<' '>'
     * <li> 6: element
     * <li> 7: META
     * <li> 8: !-- comment --
     */
    static final String RELEVANT_TAG_EXTRACTOR =
     "(?is)<(?:((script.*?)>.*?</script)|((style.*?)>.*?</style)|(((meta)|(?:\\w+))\\s+.*?)|(!--.*?--))>";


//    // this pattern extracts 'href' or 'src' attributes from
//    // any open-tag innards matched by the above
//    static Pattern RELEVANT_ATTRIBUTE_EXTRACTOR = Pattern.compile(
//     "(?is)(\\w+)(?:\\s+|(?:\\s.*?\\s))(?:(href)|(src))\\s*=(?:(?:\\s*\"(.+?)\")|(?:\\s*'(.+?)')|(\\S+))");
//
//    // this pattern extracts 'robots' attributes
//    static Pattern ROBOTS_ATTRIBUTE_EXTRACTOR = Pattern.compile(
//     "(?is)(\\w+)\\s+.*?(?:(robots))\\s*=(?:(?:\\s*\"(.+)\")|(?:\\s*'(.+)')|(\\S+))");

    // this pattern extracts attributes from any open-tag innards
    // matched by the above. attributes known to be URIs of various
    // sorts are matched specially
    static final String EACH_ATTRIBUTE_EXTRACTOR =
      "(?is)\\s((href)|(action)|(on\\w*)"
     +"|((?:src)|(?:background)|(?:cite)|(?:longdesc)"
     +"|(?:usemap)|(?:profile)|(?:datasrc)|(?:for))"
     +"|(codebase)|((?:classid)|(?:data))|(archive)|(code)"
     +"|(value)|([-\\w]+))"
     +"\\s*=\\s*"
     +"(?:(?:\"(.*?)(?:\"|$))"
     +"|(?:'(.*?)(?:'|$))"
     +"|(\\S+))";
    // groups:
    // 1: attribute name
    // 2: HREF - single URI relative to doc base, or occasionally javascript:
    // 3: ACTION - single URI relative to doc base, or occasionally javascript:
    // 4: ON[WHATEVER] - script handler
    // 5: SRC,BACKGROUND,CITE,LONGDESC,USEMAP,PROFILE,DATASRC, or FOR
    //    single URI relative to doc base
    // 6: CODEBASE - a single URI relative to doc base, affecting other
    //    attributes
    // 7: CLASSID, DATA - a single URI relative to CODEBASE (if supplied)
    // 8: ARCHIVE - one or more space-delimited URIs relative to CODEBASE
    //    (if supplied)
    // 9: CODE - a single URI relative to the CODEBASE (is specified).
    // 10: VALUE - often includes a uri path on forms
    // 11: any other attribute
    // 12: double-quote delimited attr value
    // 13: single-quote delimited attr value
    // 14: space-delimited attr value


    // much like the javascript likely-URI extractor, but
    // without requiring quotes -- this can indicate whether
    // an HTML tag attribute that isn't definitionally a
    // URI might be one anyway, as in form-tag VALUE attributes
    static final String LIKELY_URI_PATH =
     "(\\.{0,2}[^\\.\\n\\r\\s\"']*(\\.[^\\.\\n\\r\\s\"']+)+)";
    static final String ESCAPED_AMP = "&amp;";
    static final String AMP ="&";
    static final String WHITESPACE = "\\s";
    static final String CLASSEXT =".class";
    static final String APPLET = "applet";
    static final String BASE = "base";
    static final String LINK = "link";

    protected long numberOfCURIsHandled = 0;
    protected long numberOfLinksExtracted = 0;



    public ExtractorHTML(String name) {
        super(name, "HTML extractor. Extracts links from HTML documents");
    }

    protected void processGeneralTag(CrawlURI curi, CharSequence element,
            CharSequence cs) {

        Matcher attr = TextUtils.getMatcher(EACH_ATTRIBUTE_EXTRACTOR, cs);

        // Just in case it's an OBJECT or APPLET tag
        String codebase = null;
        ArrayList resources = null;

        while (attr.find()) {
            int valueGroup =
                (attr.start(12) > -1) ? 12 : (attr.start(13) > -1) ? 13 : 14;
            int start = attr.start(valueGroup);
            int end = attr.end(valueGroup);
            assert start >= 0: "Start is: " + start + ", " + curi;
            assert end >= 0: "End is :" + end + ", " + curi;
            CharSequence value = cs.subSequence(start, end);
            if (attr.start(2) > -1) {
                // HREF
                if(element.toString().equalsIgnoreCase(LINK)) {
                    // <LINK> elements treated as embeds (css, ico, etc)
                    processEmbed(curi, value);
                } else {
                    // other HREFs treated as links
                    processLink(curi, value);
                }
                if (element.toString().equalsIgnoreCase(BASE)) {
                    curi.getAList().putString(A_HTML_BASE, value.toString());
                }
            } else if (attr.start(3) > -1) {
                // ACTION
                processLink(curi, value);
            } else if (attr.start(4) > -1) {
                // ON____
                processScriptCode(curi, value);
            } else if (attr.start(5) > -1) {
                processEmbed(curi, value);
            } else if (attr.start(6) > -1) {
                // CODEBASE
                // TODO: more HTML deescaping?
                codebase = TextUtils.replaceAll(ESCAPED_AMP, value, AMP);
                processEmbed(curi,codebase);
            } else if (attr.start(7) > -1) {
                // CLASSID, DATA
                if (resources == null) {
                    resources = new ArrayList();
                }
                resources.add(value.toString());
            } else if (attr.start(8) > -1) {
                // ARCHIVE
                if (resources==null) {
                    resources = new ArrayList();
                }
                String[] multi = TextUtils.split(WHITESPACE, value);
                for(int i = 0; i < multi.length; i++ ) {
                    resources.add(multi[i]);
                }
            } else if (attr.start(9) > -1) {
                // CODE
                if (resources==null) {
                    resources = new ArrayList();
                }
                // If element is applet and code value does not end with
                // '.class' then append '.class' to the code value.
                if (element.toString().toLowerCase().equals(APPLET) &&
                        !value.toString().toLowerCase().endsWith(CLASSEXT)) {
                    resources.add(value.toString() + CLASSEXT);
                } else {
                    resources.add(value.toString());
                }

            } else if (attr.start(10) > -1) {
                // VALUE
                if(TextUtils.matches(LIKELY_URI_PATH, value)) {
                    processLink(curi,value);
                }

            } else if (attr.start(11) > -1) {
                // any other attribute
                // ignore for now
                // could probe for path- or script-looking strings, but
                // those should be vanishingly rare in other attributes,
                // and/or symptomatic of page bugs
            }
        }
        TextUtils.freeMatcher(attr);
        attr = null;

        // handle codebase/resources
        if (resources == null) {
            return;
        }
        Iterator iter = resources.iterator();
        UURI codebaseURI = null;
        String res = null;
        try {
            if (codebase != null) {
                // TODO: Pass in the charset.
                codebaseURI = UURIFactory.
                	getInstance(curi.getUURI(), codebase);
            }
            while(iter.hasNext()) {
                res = iter.next().toString();
                // TODO: more HTML deescaping?
                res = TextUtils.replaceAll(ESCAPED_AMP, res, AMP);
                if (codebaseURI != null) {
                    res = codebaseURI.resolve(res).toString();
                }
                processEmbed(curi, res);
            }
        } catch (URIException e) {
            curi.addLocalizedError(getName(),e,"BAD CODEBASE " + codebase);
        } catch (IllegalArgumentException e) {
            DevUtils.logger.log(Level.WARNING, "processGeneralTag()\n" +
                "codebase=" + codebase + " res=" + res + "\n" +
                DevUtils.extraInfo(), e);
        }
    }


    // finds strings in javascript likely to be URIs/paths
    // guessing based on '.' in string, so if highly likely to
    // get gifs/etc, unable to get many other paths
    // will find false positives
    // TODO: add '/' check, suppress strings being concatenated via '+'?
    static final String JAVASCRIPT_LIKELY_URI_EXTRACTOR =
     "(\\\\*\"|\\\\*\')(\\.{0,2}[^+\\.\\n\\r\\s\"\']+[^\\.\\n\\r\\s\"\']*(\\.[^\\.\\n\\r\\s\"\']+)+)(\\1)";

    /**
     * @param curi
     * @param cs
     */
    protected void processScriptCode(CrawlURI curi, CharSequence cs) {
        this.numberOfLinksExtracted +=
            ExtractorJS.considerStrings(curi, cs.toString(), false);
    }

    static final String JAVASCRIPT = "(?i)^javascript:.*";

    /**
     * @param curi
     * @param value
     */
    protected void processLink(CrawlURI curi, CharSequence value) {
        String link = TextUtils.replaceAll(ESCAPED_AMP, value, "&");

        if(TextUtils.matches(JAVASCRIPT, link)) {
            processScriptCode(curi,value.subSequence(11, value.length()));
        } else {
            logger.finest("link: " + link + " from " + curi);
            this.numberOfLinksExtracted++;
            curi.addLinkToCollection(link, A_HTML_LINKS);
        }
    }

    protected void processEmbed(CrawlURI curi, CharSequence value) {
        String embed = TextUtils.replaceAll(ESCAPED_AMP, value, "&");

        logger.finest("embed: " + embed + " from " + curi);
        this.numberOfLinksExtracted++;
        curi.addLinkToCollection(embed, A_HTML_EMBEDS);
    }

    public void innerProcess(CrawlURI curi) {

        if (!curi.isHttpTransaction())
        {
            return;
        }

        if(this.ignoreUnexpectedHTML) {
            try {
                if(!isHtmlExpectedHere(curi)) {
                    // HTML was not expected (eg a GIF was expected) so ignore
                    // (as if a soft 404)
                    return;
                }
            }
            catch (URIException e) {
                logger.severe("Failed expectedHTML test: " + e.getMessage());
            }
        }

        String contentType = curi.getContentType();
        if ((contentType==null) || (!contentType.startsWith("text/html")))
        {
            // nothing to extract for other types here
            return;
        }

        this.numberOfCURIsHandled++;

        ReplayCharSequence cs = null;
        try {
	       cs = curi.getHttpRecorder().getReplayCharSequence();
        } catch(Exception e) {
            curi.addLocalizedError(this.getName(), e,
                "Failed get of replay char sequence.");
        }
        if (cs == null) {
            logger.warning("Failed getting ReplayCharSequence: " +
                curi.toString());
            return;
        }

        // extract all links from the charsequence
        extract(curi, cs);

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

    // this method extracted, pacakage visible to ease testing
    void extract(CrawlURI curi, CharSequence cs) {
        Matcher tags = TextUtils.getMatcher(RELEVANT_TAG_EXTRACTOR, cs);
        while(tags.find()) {
            if(Thread.interrupted()){
                break;
            }
            if (tags.start(8) > 0) {
                // comment match
                // for now do nothing
            } else if (tags.start(7) > 0) {
                // <meta> match
                int start = tags.start(5);
                int end = tags.end(5);
                assert start >= 0: "Start is: " + start + ", " + curi;
                assert end >= 0: "End is :" + end + ", " + curi;
                if (processMeta(curi,
                    cs.subSequence(start, end))) {

                    // meta tag included NOFOLLOW; abort processing
                    break;
                }
            } else if (tags.start(5) > 0) {
                // generic <whatever> match
                int start5 = tags.start(5);
                int end5 = tags.end(5);
                assert start5 >= 0: "Start is: " + start5 + ", " + curi;
                assert end5 >= 0: "End is :" + end5 + ", " + curi;
                int start6 = tags.start(6);
                int end6 = tags.end(6);
                assert start6 >= 0: "Start is: " + start6 + ", " + curi;
                assert end6 >= 0: "End is :" + end6 + ", " + curi;
                processGeneralTag(curi,
                    cs.subSequence(start6, end6),
                    cs.subSequence(start5, end5));

            } else if (tags.start(1) > 0) {
                // <script> match
                int start = tags.start(1);
                int end = tags.end(1);
                assert start >= 0: "Start is: " + start + ", " + curi;
                assert end >= 0: "End is :" + end + ", " + curi;
                assert tags.end(2) >= 0: "Tags.end(2) illegal " + tags.end(2) +
                    ", " + curi;
                processScript(curi, cs.subSequence(start, end),
                    tags.end(2) - start);

            } else if (tags.start(3) > 0){
                // <style... match
                int start = tags.start(3);
                int end = tags.end(3);
                assert start >= 0: "Start is: " + start + ", " + curi;
                assert end >= 0: "End is :" + end + ", " + curi;
                assert tags.end(4) >= 0: "Tags.end(4) illegal " + tags.end(4) +
                    ", " + curi;
                processStyle(curi, cs.subSequence(start, end),
                    tags.end(4) - start);
            }
        }

        TextUtils.freeMatcher(tags);
    }


    static final String NON_HTML_PATH_EXTENSION =
        "(?i)(gif)|(jp(e)?g)|(png)|(tif(f)?)|(bmp)|(avi)|(mov)|(mp(e)?g)"+
        "|(mp3)|(mp4)|(swf)|(wav)|(au)|(aiff)|(mid)";

    /**
     * Test whether this HTML is so unexpected (eg in place of a GIF URI)
     * that it shouldn't be scanned for links.
     *
     * @param curi CrawlURI to examine.
     * @return True if HTML is acceptable/expected here
     * @throws URIException
     */
    protected boolean isHtmlExpectedHere(CrawlURI curi) throws URIException {
        String path = curi.getUURI().getPath();
        if(path==null) {
            // no path extension, HTML is fine
            return true;
        }
        int dot = path.lastIndexOf('.');
        if (dot < 0) {
            // no path extension, HTML is fine
            return true;
        }
        if(dot<(path.length()-5)) {
            // extension too long to recognize, HTML is fine
            return true;
        }
        String ext = path.substring(dot+1);
        return ! TextUtils.matches(NON_HTML_PATH_EXTENSION, ext);
    }

    protected void processScript(CrawlURI curi, CharSequence sequence,
            int endOfOpenTag) {
        // for now, do nothing
        // TODO: best effort extraction of strings

        // first, get attributes of script-open tag
        // as per any other tag
        processGeneralTag(curi,sequence.subSequence(0,6),
            sequence.subSequence(0,endOfOpenTag));

        // then, apply best-effort string-analysis heuristics
        // against any code present (false positives are OK)
        processScriptCode(
            curi, sequence.subSequence(endOfOpenTag, sequence.length()));
    }

    protected boolean processMeta(CrawlURI curi, CharSequence cs) {
        Matcher attr = TextUtils.getMatcher(EACH_ATTRIBUTE_EXTRACTOR, cs);

        String name = null;
        String httpEquiv = null;
        String content = null;

        while (attr.find()) {
            int valueGroup =
                (attr.start(12) > -1) ? 12 : (attr.start(13) > -1) ? 13 : 14;
            CharSequence value =
                cs.subSequence(attr.start(valueGroup), attr.end(valueGroup));
            if (attr.group(1).equalsIgnoreCase("name")) {
                name = value.toString();
            } else if (attr.group(1).equalsIgnoreCase("http-equiv")) {
                httpEquiv = value.toString();
            } else if (attr.group(1).equalsIgnoreCase("content")) {
                content = value.toString();
            }
            // TODO: handle other stuff
        }
        TextUtils.freeMatcher(attr);
        attr = null;

        // Look for the 'robots' meta-tag
        if("robots".equalsIgnoreCase(name) && content != null ) {
            curi.getAList().putString(A_META_ROBOTS, content);
            RobotsHonoringPolicy policy =
                getSettingsHandler().getOrder().getRobotsHonoringPolicy();
            String contentLower = content.toLowerCase();
            if ((policy == null
                || (!policy.isType(curi, RobotsHonoringPolicy.IGNORE)
                    && !policy.isType(curi, RobotsHonoringPolicy.CUSTOM)))
                && (contentLower.indexOf("nofollow") >= 0
                    || contentLower.indexOf("none") >= 0)) {
                // if 'nofollow' or 'none' is specified and the
                // honoring policy is not IGNORE or CUSTOM, end html extraction
                logger.fine("HTML extraction skipped due to robots meta-tag for: "
                                + curi.getURIString());
                return true;
            }
        } else if ("refresh".equalsIgnoreCase(httpEquiv) && content != null) {
            curi.addLinkToCollection(
                content.substring(content.indexOf("=") + 1), A_HTML_LINKS);
        }
        return false;
    }

    /**
     * @param curi
     * @param sequence
     * @param endOfOpenTag
     */
    protected void processStyle(CrawlURI curi, CharSequence sequence,
            int endOfOpenTag)
    {
        // First, get attributes of script-open tag as per any other tag.
        processGeneralTag(curi, sequence.subSequence(0,6),
            sequence.subSequence(0,endOfOpenTag));

        // then, parse for URIs
        this.numberOfLinksExtracted += ExtractorCSS.processStyleCode(
            curi, sequence.subSequence(endOfOpenTag,sequence.length()));
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Processor#report()
     */
    public String report() {
        StringBuffer ret = new StringBuffer();
        ret.append("Processor: org.archive.crawler.extractor.ExtractorHTML\n");
        ret.append("  Function:          Link extraction on HTML documents\n");
        ret.append("  CrawlURIs handled: " + this.numberOfCURIsHandled + "\n");
        ret.append("  Links extracted:   " + this.numberOfLinksExtracted +
            "\n\n");

        return ret.toString();
    }
}

