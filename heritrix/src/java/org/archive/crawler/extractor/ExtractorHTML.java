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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.GetMethod;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.RobotsHonoringPolicy;
import org.archive.crawler.framework.Processor;
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

    private static Logger logger = Logger.getLogger("org.archive.crawler.extractor.ExtractorHTML");

    // this pattern extracts either (1) whole <script>...</script>
    // ranges; or (2) any other open-tag with at least one attribute
    // (eg matches "<a href='boo'>" but not "</a>" or "<br>")
    static final String RELEVANT_TAG_EXTRACTOR =
    "(?is)<(?:((script.*?)>.*?</script)|(((meta)|(?:\\w+))\\s+.*?)|(!--.*?--))>";
    // groups:
    // 1: SCRIPT SRC=blah>blah</SCRIPT
    // 2: just script open tag
    // 3: entire other tag, without '<' '>'
    // 4: element
    // 5: META
    // 6: !-- blahcomment --


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
     +"|(codebase)|((?:classid)|(?:data))|(archive)"
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
    // 6: CODEBASE - a single URI relative to doc base, affecting other attributes
    // 7: CLASSID,DATA - a single URI relative to CODEBASE (if supplied)
    // 8: ARCHIVE - one or more space-delimited URIs relative to CODEBASE (if supplied)
    // 9: VALUE - often includes a uri path on forms
    // 10: any other attribute
    // 11: double-quote delimited attr value
    // 12: single-quote delimited attr value
    // 13: space-delimited attr value


    // much like the javascript likely-URI extractor, but
    // without requiring quotes -- this can indicate whether
    // an HTML tag attribute that isn't definitionally a
    // URI might be one anyway, as in form-tag VALUE attributes
    static final String LIKELY_URI_PATH =
     "(\\.{0,2}[^\\.\\n\\r\\s\"']*(\\.[^\\.\\n\\r\\s\"']+)+)";
    static final String ESCAPED_AMP = "&amp;";
    static final String WHITESPACE = "\\s";

    protected long numberOfCURIsHandled = 0;
    protected long numberOfLinksExtracted = 0;

    /**
     * @param name
     */
    public ExtractorHTML(String name) {
        super(name, "HTML extractor");
    }

    /**
     */
    protected void processGeneralTag(CrawlURI curi, CharSequence element, CharSequence cs) {
        Matcher attr = TextUtils.getMatcher(EACH_ATTRIBUTE_EXTRACTOR, cs);

        // Just in case it's an OBJECT tag
        String codebase = null;
        ArrayList resources = null;

        while (attr.find()) {
            int valueGroup =
                (attr.start(11) > -1) ? 11 : (attr.start(12) > -1) ? 12 : 13;
            CharSequence value =
                cs.subSequence(attr.start(valueGroup), attr.end(valueGroup));
            if (attr.start(2)>-1) {
                // HREF
                if(element.toString().equalsIgnoreCase("link")) {
                    // <LINK> elements treated as embeds (css, ico, etc)
                    processEmbed(curi, value);
                } else {
                    // other HREFs treated as links
                    processLink(curi, value);
                }
                if (element.toString().equalsIgnoreCase("base")) {
                    curi.getAList().putString(A_HTML_BASE,value.toString());
                }
            } else if (attr.start(3)>-1) {
                // ACTION
                processLink(curi, value);
            } else if (attr.start(4)>-1) {
                // ON____
                processScriptCode(curi, value);
            } else if (attr.start(5)>-1) {
                processEmbed(curi, value);
            } else if (attr.start(6)>-1) {
                // CODEBASE
                //codebase = value.toString();
                //codebase = codebase.replaceAll("&amp;","&"); // TODO: more HTML deescaping?
                codebase = TextUtils.replaceAll(ESCAPED_AMP, value, "&");
                processEmbed(curi,codebase);
            } else if (attr.start(7)>-1) {
                // CLASSID,DATA
                if (resources==null) { resources = new ArrayList(); }
                resources.add(value.toString());
            } else if (attr.start(8)>-1) {
                // ARCHIVE
                if (resources==null) { resources = new ArrayList(); }
                //String[] multi = value.toString().split("\\s");
                String[] multi = TextUtils.split(WHITESPACE, value);
                for(int i = 0; i < multi.length; i++ ) {
                    resources.add(multi[i]);
                }
            } else if (attr.start(9)>-1) {
                // VALUE
                if(TextUtils.matches(LIKELY_URI_PATH, value)) {
                    processLink(curi,value);
                }

            } else if (attr.start(10)>-1) {
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
        URI codebaseURI = null;
        String res = null;
        try {
            if (codebase != null) {
                codebaseURI = new URI(codebase);
            }
            while(iter.hasNext()) {
                res = iter.next().toString();
                //res = res.replaceAll("&amp;","&"); // TODO: more HTML deescaping?
                res = TextUtils.replaceAll(ESCAPED_AMP, res, "&");
                if (codebaseURI != null) {
                    res = codebaseURI.resolve(res).toString();
                }
                processEmbed(curi,res);
            }
        } catch (URISyntaxException e) {
            //System.out.println("BAD CODEBASE "+codebase+" at "+curi);
            // e.printStackTrace();
            curi.addLocalizedError(getName(),e,"BAD CODEBASE "+codebase);
        } catch (IllegalArgumentException e) {
            DevUtils.logger.log(
                Level.WARNING,
                "processGeneralTag()\n"+
                "codebase="+codebase+" res="+res+"\n"+
                DevUtils.extraInfo(),
                e);
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
        String code = cs.toString();
        //code = code.replaceAll("&amp;","&"); // TODO: more HTML deescaping?
        code = TextUtils.replaceAll(ESCAPED_AMP, code, "&");
    	numberOfLinksExtracted += ExtractorJS.considerStrings(curi,code);
    }

    static final String JAVASCRIPT = "(?i)^javascript:.*";

    /**
     * @param curi
     * @param value
     */
    protected void processLink(CrawlURI curi, CharSequence value) {
        //String link = value.toString();
        //link = link.replaceAll("&amp;","&"); // TODO: more HTML deescaping?
        String link = TextUtils.replaceAll(ESCAPED_AMP, value, "&");
        //if(link.matches("(?i)^javascript:.*")) {
        if(TextUtils.matches(JAVASCRIPT, link)) {
            processScriptCode(curi,value.subSequence(11,value.length()));
        } else {
            logger.finest("link: "+link+ " from "+curi);
            numberOfLinksExtracted++;
            curi.addLink(link);
        }
    }



    /**
     * @param curi
     * @param value
     */
    protected void processEmbed(CrawlURI curi, CharSequence value) {
        //String embed = value.toString();
        //embed = embed.replaceAll("&amp;","&"); // TODO: more HTML deescaping?
        String embed = TextUtils.replaceAll(ESCAPED_AMP, value, "&");
        logger.finest("embed: "+embed+ " from "+curi);
        numberOfLinksExtracted++;
        curi.addEmbed(embed);
    }



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

        numberOfCURIsHandled++;

        CharSequence cs = get.getHttpRecorder().getRecordedInput().getCharSequence();

        if (cs==null) {
            // TODO: note problem
            return;
        }

        Matcher tags = TextUtils.getMatcher(RELEVANT_TAG_EXTRACTOR, cs);
        while(tags.find()) {
            if (tags.start(6) > 0) {
                // comment match
                // for now do nothing
            } else if (tags.start(5) > 0) {
            // <meta> match
                if (processMeta(curi,cs.subSequence(tags.start(3), tags.end(3)))) {
                    // meta tag included NOFOLLOW; abort processing
                    TextUtils.freeMatcher(tags);
                    return;
                }
            } else if (tags.start(3) > 0) {
                // generic <whatever> match
                processGeneralTag(
                    curi,
                    cs.subSequence(tags.start(4),tags.end(4)),
                    cs.subSequence(tags.start(3),tags.end(3)));
            } else if (tags.start(1) > 0) {
                // <script> match
                processScript(curi, cs.subSequence(tags.start(1), tags.end(1)), tags.end(2)-tags.start(1));
            }
        }
        TextUtils.freeMatcher(tags);

        curi.linkExtractorFinished(); // Set flag to indicate that link extraction is completed.
    }


    static final String NON_HTML_PATH_EXTENSION =
        "(?i)(gif)|(jp(e)?g)|(png)|(tif(f)?)|(bmp)|(avi)|(mov)|(mp(e)?g)"+
        "|(mp3)|(mp4)|(swf)|(wav)|(au)|(aiff)|(mid)";

    /**
     * @param curi
     * @return True if HTML.
     */
    protected boolean expectedHTML(CrawlURI curi) {
        String path = curi.getUURI().getPath();
        int dot = path.lastIndexOf('.');
        if (dot<0) {
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

        // then, apply best-effort string-analysis heuristics
        // against any code present (false positives are OK)
        processScriptCode(curi,sequence.subSequence(endOfOpenTag,sequence.length()));
    }


    /**
     */
    protected boolean processMeta(CrawlURI curi, CharSequence cs) {
        Matcher attr = TextUtils.getMatcher(EACH_ATTRIBUTE_EXTRACTOR, cs);

        String name = null;
        String httpEquiv = null;
        String content = null;

        while (attr.find()) {
            int valueGroup =
                (attr.start(11) > -1) ? 11 : (attr.start(12) > -1) ? 12 : 13;
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

        if("robots".equalsIgnoreCase(name) && content != null ) {
            curi.getAList().putString(A_META_ROBOTS,content);
            RobotsHonoringPolicy policy =
                getSettingsHandler().getOrder().getRobotsHonoringPolicy();
            if ((policy == null
                || (!policy.isType(curi, RobotsHonoringPolicy.IGNORE)
                    && !policy.isType(curi, RobotsHonoringPolicy.CUSTOM)))
                && (content.indexOf("nofollow") >= 0
                    || content.indexOf("none") >= 0)) {
                // if 'nofollow' or 'none' is specified and the
                // honoring policy is not IGNORE or CUSTOM, end html extraction
                logger.fine("HTML extraction skipped due to robots meta-tag for: " + curi.getURIString());
                return true;
            }
        } else if ("refresh".equalsIgnoreCase(httpEquiv) && content != null) {
            curi.addLink(content.substring(content.indexOf("=")+1));
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Processor#report()
     */
    public String report() {
        StringBuffer ret = new StringBuffer();
        ret.append("Processor: org.archive.crawler.extractor.ExtractorHTML\n");
        ret.append("  Function:          Link extraction on HTML documents\n");
        ret.append("  CrawlURIs handled: " + numberOfCURIsHandled + "\n");
        ret.append("  Links extracted:   " + numberOfLinksExtracted + "\n\n");

        return ret.toString();
    }

}

