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
package org.archive.processors.extractor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import org.apache.commons.httpclient.URIException;
import org.archive.io.ReplayCharSequence;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;
import org.archive.processors.ProcessorURI;
import org.archive.processors.fetcher.RobotsHonoringPolicy;
import org.archive.state.Key;
import org.archive.state.KeyManager;
import org.archive.util.DevUtils;
import org.archive.util.TextUtils;

/**
 * Basic link-extraction, from an HTML content-body,
 * using regular expressions.
 *
 * @author gojomo
 *
 */
public class ExtractorHTML extends ContentExtractor {

    private static final long serialVersionUID = 2L;

    private static Logger logger =
        Logger.getLogger(ExtractorHTML.class.getName());

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
// version w/ less unnecessary backtracking
      private static final int MAX_ELEMENT_LENGTH =
          Integer.parseInt(System.getProperty(ExtractorHTML.class.getName() +
              ".maxElementNameLength", "1024"));
      
      static final String RELEVANT_TAG_EXTRACTOR =
          "(?is)<(?:((script[^>]*+)>.*?</script)" + // 1, 2
          "|((style[^>]*+)>[^<]*+</style)" + // 3, 4
          "|(((meta)|(?:\\w{1,"+MAX_ELEMENT_LENGTH+"}))\\s+[^>]*+)" + // 5, 6, 7
          "|(!--.*?--))>"; // 8 

//    version w/ problems with unclosed script tags 
//    static final String RELEVANT_TAG_EXTRACTOR =
//    "(?is)<(?:((script.*?)>.*?</script)|((style.*?)>.*?</style)|(((meta)|(?:\\w+))\\s+.*?)|(!--.*?--))>";


      
//    // this pattern extracts 'href' or 'src' attributes from
//    // any open-tag innards matched by the above
//    static Pattern RELEVANT_ATTRIBUTE_EXTRACTOR = Pattern.compile(
//     "(?is)(\\w+)(?:\\s+|(?:\\s.*?\\s))(?:(href)|(src))\\s*=(?:(?:\\s*\"(.+?)\")|(?:\\s*'(.+?)')|(\\S+))");
//
//    // this pattern extracts 'robots' attributes
//    static Pattern ROBOTS_ATTRIBUTE_EXTRACTOR = Pattern.compile(
//     "(?is)(\\w+)\\s+.*?(?:(robots))\\s*=(?:(?:\\s*\"(.+)\")|(?:\\s*'(.+)')|(\\S+))");

      private static final int MAX_ATTR_NAME_LENGTH =
          Integer.parseInt(System.getProperty(ExtractorHTML.class.getName() +
              ".maxAttributeNameLength", "1024")); // 1K; 
      
      static final int MAX_ATTR_VAL_LENGTH = 
          Integer.parseInt(System.getProperty(ExtractorHTML.class.getName() +
              ".maxAttributeValueLength", "16384")); // 16K; 
      
    // TODO: perhaps cut to near MAX_URI_LENGTH
    
    // this pattern extracts attributes from any open-tag innards
    // matched by the above. attributes known to be URIs of various
    // sorts are matched specially
    static final String EACH_ATTRIBUTE_EXTRACTOR =
      "(?is)\\s((href)|(action)|(on\\w*)" // 1, 2, 3, 4 
     +"|((?:src)|(?:lowsrc)|(?:background)|(?:cite)|(?:longdesc)" // ...
     +"|(?:usemap)|(?:profile)|(?:datasrc))" // 5
     +"|(codebase)|((?:classid)|(?:data))|(archive)|(code)" // 6, 7, 8, 9
     +"|(value)|(style)|([-\\w]{1,"+MAX_ATTR_NAME_LENGTH+"}))" // 10, 11, 12
     +"\\s*=\\s*"
     +"(?:(?:\"(.{0,"+MAX_ATTR_VAL_LENGTH+"}?)(?:\"|$))" // 13
     +"|(?:'(.{0,"+MAX_ATTR_VAL_LENGTH+"}?)(?:'|$))" // 14
     +"|(\\S{1,"+MAX_ATTR_VAL_LENGTH+"}))"; // 15
    // groups:
    // 1: attribute name
    // 2: HREF - single URI relative to doc base, or occasionally javascript:
    // 3: ACTION - single URI relative to doc base, or occasionally javascript:
    // 4: ON[WHATEVER] - script handler
    // 5: SRC,LOWSRC,BACKGROUND,CITE,LONGDESC,USEMAP,PROFILE, or DATASRC
    //    single URI relative to doc base
    // 6: CODEBASE - a single URI relative to doc base, affecting other
    //    attributes
    // 7: CLASSID, DATA - a single URI relative to CODEBASE (if supplied)
    // 8: ARCHIVE - one or more space-delimited URIs relative to CODEBASE
    //    (if supplied)
    // 9: CODE - a single URI relative to the CODEBASE (is specified).
    // 10: VALUE - often includes a uri path on forms
    // 11: STYLE - inline attribute style info
    // 12: any other attribute
    // 13: double-quote delimited attr value
    // 14: single-quote delimited attr value
    // 15: space-delimited attr value


    // much like the javascript likely-URI extractor, but
    // without requiring quotes -- this can indicate whether
    // an HTML tag attribute that isn't definitionally a
    // URI might be one anyway, as in form-tag VALUE attributes
    static final String LIKELY_URI_PATH =
     "(\\.{0,2}[^\\.\\n\\r\\s\"']*(\\.[^\\.\\n\\r\\s\"']+)+)";
    static final String WHITESPACE = "\\s";
    static final String CLASSEXT =".class";
    static final String APPLET = "applet";
    static final String BASE = "base";
    static final String LINK = "link";
    static final String FRAME = "frame";
    static final String IFRAME = "iframe";

    public static final Key<Boolean> TREAT_FRAMES_AS_EMBED_LINKS = 
        Key.makeExpert(true);
    
    public static final Key<Boolean> IGNORE_FORM_ACTION_URLS =
        Key.makeExpert(false);
    
    public static final Key<Boolean> OVERLY_EAGER_LINK_DETECTION =
        Key.makeExpert(true);
    
    public static final Key<Boolean> IGNORE_UNEXPECTED_HTML = 
        Key.makeExpert(true);
    
    
    static {
        KeyManager.addKeys(ExtractorHTML.class);
    }
    
    protected long numberOfCURIsHandled = 0;
    protected long numberOfLinksExtracted = 0;

    public ExtractorHTML() {
    }
    

    protected void processGeneralTag(ProcessorURI curi, CharSequence element,
            CharSequence cs) {

        Matcher attr = TextUtils.getMatcher(EACH_ATTRIBUTE_EXTRACTOR, cs);

        // Just in case it's an OBJECT or APPLET tag
        String codebase = null;
        ArrayList<String> resources = null;
        
        final boolean framesAsEmbeds = 
            curi.get(this, TREAT_FRAMES_AS_EMBED_LINKS);

        final boolean ignoreFormActions = 
            curi.get(this, IGNORE_FORM_ACTION_URLS);
        
        final boolean overlyEagerLinkDetection = 
            curi.get(this, OVERLY_EAGER_LINK_DETECTION);
        
        final String elementStr = element.toString();

        while (attr.find()) {
            int valueGroup =
                (attr.start(13) > -1) ? 13 : (attr.start(14) > -1) ? 14 : 15;
            int start = attr.start(valueGroup);
            int end = attr.end(valueGroup);
            assert start >= 0: "Start is: " + start + ", " + curi;
            assert end >= 0: "End is :" + end + ", " + curi;
            CharSequence value = cs.subSequence(start, end);
            value = TextUtils.unescapeHtml(value);
            if (attr.start(2) > -1) {
                // HREF
                CharSequence context = elementContext(element, attr.group(2));
                if(elementStr.equalsIgnoreCase(LINK)) {
                    // <LINK> elements treated as embeds (css, ico, etc)
                    processEmbed(curi, value, context);
                } else {
                    // other HREFs treated as links
                    processLink(curi, value, context);
                }
                if (elementStr.equalsIgnoreCase(BASE)) {
                    try {
                        UURI base = UURIFactory.getInstance(value.toString());
                        curi.setBaseURI(base);
                    } catch (URIException e) {
                        curi.addUriError(e, value.toString());
                    }
                }
            } else if (attr.start(3) > -1) {
                // ACTION
                if (!ignoreFormActions) {
                    CharSequence context = elementContext(element,
                        attr.group(3));
                    processLink(curi, value, context);
                }
            } else if (attr.start(4) > -1) {
                // ON____
                processScriptCode(curi, value); // TODO: context?
            } else if (attr.start(5) > -1) {
                // SRC etc.
                CharSequence context = elementContext(element, attr.group(5));
                
                // true, if we expect another HTML page instead of an image etc.
                final Hop hop;
                
                if(!framesAsEmbeds
                    && (elementStr.equalsIgnoreCase(FRAME) || elementStr
                        .equalsIgnoreCase(IFRAME))) {
                    hop = Hop.NAVLINK;
                } else {
                    hop = Hop.EMBED;
                }
                processEmbed(curi, value, context, hop);
            } else if (attr.start(6) > -1) {
                // CODEBASE
                codebase = (value instanceof String)?
                    (String)value: value.toString();
                CharSequence context = elementContext(element,
                    attr.group(6));
                processEmbed(curi, codebase, context);
            } else if (attr.start(7) > -1) {
                // CLASSID, DATA
                if (resources == null) {
                    resources = new ArrayList<String>();
                }
                resources.add(value.toString());
            } else if (attr.start(8) > -1) {
                // ARCHIVE
                if (resources==null) {
                    resources = new ArrayList<String>();
                }
                String[] multi = TextUtils.split(WHITESPACE, value);
                for(int i = 0; i < multi.length; i++ ) {
                    resources.add(multi[i]);
                }
            } else if (attr.start(9) > -1) {
                // CODE
                if (resources==null) {
                    resources = new ArrayList<String>();
                }
                // If element is applet and code value does not end with
                // '.class' then append '.class' to the code value.
                if (elementStr.equalsIgnoreCase(APPLET) &&
                        !value.toString().toLowerCase().endsWith(CLASSEXT)) {
                    resources.add(value.toString() + CLASSEXT);
                } else {
                    resources.add(value.toString());
                }
            } else if (attr.start(10) > -1) {
                // VALUE, with possibility of URI
                if (TextUtils.matches(LIKELY_URI_PATH, value)
                        && overlyEagerLinkDetection) {
                    CharSequence context = elementContext(element,
                        attr.group(10));
                    processLink(curi,value, context);
                }

            } else if (attr.start(11) > -1) {
                // STYLE inline attribute
                // then, parse for URIs
                this.numberOfLinksExtracted += ExtractorCSS.processStyleCode(
                    curi, value);
                
            } else if (attr.start(12) > -1) {
                // any other attribute
                // ignore for now
                // could probe for path- or script-looking strings, but
                // those should be vanishingly rare in other attributes,
                // and/or symptomatic of page bugs
            }
        }
        TextUtils.recycleMatcher(attr);

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
                res = (String) TextUtils.unescapeHtml(res);
                if (codebaseURI != null) {
                    res = codebaseURI.resolve(res).toString();
                }
                processEmbed(curi, res, element); // TODO: include attribute too
            }
        } catch (URIException e) {
            curi.getNonFatalFailures().add(e);
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
     "(\\\\{0,8}+\"|\\\\{0,8}+\')(\\.{0,2}[^+\\.\\n\\r\\s\"\']+[^\\.\\n\\r\\s\"\']*(\\.[^\\.\\n\\r\\s\"\']+)+)(\\1)";

    /**
     * @param curi
     * @param cs
     */
    protected void processScriptCode(ProcessorURI curi, CharSequence cs) {
        this.numberOfLinksExtracted +=
            ExtractorJS.considerStrings(curi, cs, false);
    }

    static final String JAVASCRIPT = "(?i)^javascript:.*";

    /**
     * Handle generic HREF cases.
     * 
     * @param curi
     * @param value
     * @param context
     */
    protected void processLink(ProcessorURI curi, final CharSequence value,
            CharSequence context) {
        if (TextUtils.matches(JAVASCRIPT, value)) {
            processScriptCode(curi, value. subSequence(11, value.length()));
        } else {    
            if (logger.isLoggable(Level.FINEST)) {
                logger.finest("link: " + value.toString() + " from " + curi);
            }
            addLinkFromString(curi,
                (value instanceof String)?
                    (String)value: value.toString(),
                context, Hop.NAVLINK);
            this.numberOfLinksExtracted++;
        }
    }

    private void addLinkFromString(ProcessorURI curi, String uri,
            CharSequence context, Hop hop) {
        try {
            // We do a 'toString' on context because its a sequence from
            // the underlying ReplayCharSequence and the link its about
            // to become a part of is expected to outlive the current
            // ReplayCharSequence.
            HTMLLinkContext hc = new HTMLLinkContext(context.toString());
            Link.addRelativeToBase(curi, uri, hc, hop);
        } catch (URIException e) {
            curi.addUriError(e, uri);
        }
    }

    protected final void processEmbed(ProcessorURI curi, CharSequence value,
            CharSequence context) {
        processEmbed(curi, value, context, Hop.EMBED);
    }

    protected void processEmbed(ProcessorURI curi, final CharSequence value,
            CharSequence context, Hop hop) {
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest("embed (" + hop.getHopChar() + "): " + value.toString() +
                " from " + curi);
        }
        addLinkFromString(curi,
            (value instanceof String)?
                (String)value: value.toString(),
            context, hop);
        this.numberOfLinksExtracted++;
    }

    
    protected boolean shouldExtract(ProcessorURI uri) {
        if (uri.get(this, IGNORE_UNEXPECTED_HTML)) {
            try {
                // HTML was not expected (eg a GIF was expected) so ignore
                // (as if a soft 404)
                if (!isHtmlExpectedHere(uri)) {
                    return false;
                }
            } catch (URIException e) {
                logger.severe("Failed expectedHTML test: " + e.getMessage());
                // assume it's okay to extract
            }
        }
        
        String mime = uri.getContentType().toLowerCase();
        if (mime.startsWith("text/html")) {
            return true;
        }
        if (mime.startsWith("application/xhtml")) {
            return true;
        }
        
        return false;
    }
    
    
    public boolean innerExtract(ProcessorURI curi) {
        this.numberOfCURIsHandled++;

        ReplayCharSequence cs = null;
        
        try {
           cs = curi.getRecorder().getReplayCharSequence();
        } catch (IOException e) {
            curi.getNonFatalFailures().add(e);
            //addLocalizedError(e,
            //    "Failed get of replay char sequence " + curi.toString() +
            //        " " + e.getMessage());
            logger.log(Level.SEVERE,"Failed get of replay char sequence in " +
                Thread.currentThread().getName(), e);
        }
        
        if (cs == null) {
            return false;
        }

        // We have a ReplayCharSequence open.  Wrap all in finally so we
        // for sure close it before we leave.
        try {
            // Extract all links from the charsequence
            extract(curi, cs);
            // Set flag to indicate that link extraction is completed.
            return true;
        } finally {
            if (cs != null) {
                try {
                    cs.close();
                } catch (IOException ioe) {
                    logger.warning(TextUtils.exceptionToString(
                        "Failed close of ReplayCharSequence.", ioe));
                }
            }
        }
    }

    /**
     * Run extractor.
     * This method is package visible to ease testing.
     * @param curi ProcessorURI we're processing.
     * @param cs Sequence from underlying ReplayCharSequence. This
     * is TRANSIENT data. Make a copy if you want the data to live outside
     * of this extractors' lifetime.
     */
    void extract(ProcessorURI curi, CharSequence cs) {
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
        TextUtils.recycleMatcher(tags);
    }


    static final String NON_HTML_PATH_EXTENSION =
        "(?i)(gif)|(jp(e)?g)|(png)|(tif(f)?)|(bmp)|(avi)|(mov)|(mp(e)?g)"+
        "|(mp3)|(mp4)|(swf)|(wav)|(au)|(aiff)|(mid)";

    /**
     * Test whether this HTML is so unexpected (eg in place of a GIF URI)
     * that it shouldn't be scanned for links.
     *
     * @param curi ProcessorURI to examine.
     * @return True if HTML is acceptable/expected here
     * @throws URIException
     */
    protected boolean isHtmlExpectedHere(ProcessorURI curi) throws URIException {
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

    protected void processScript(ProcessorURI curi, CharSequence sequence,
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

    /**
     * Process metadata tags.
     * @param curi ProcessorURI we're processing.
     * @param cs Sequence from underlying ReplayCharSequence. This
     * is TRANSIENT data. Make a copy if you want the data to live outside
     * of this extractors' lifetime.
     * @return True robots exclusion metatag.
     */
    protected boolean processMeta(ProcessorURI curi, CharSequence cs) {
        Matcher attr = TextUtils.getMatcher(EACH_ATTRIBUTE_EXTRACTOR, cs);
        String name = null;
        String httpEquiv = null;
        String content = null;
        while (attr.find()) {
            int valueGroup =
                (attr.start(13) > -1) ? 13 : (attr.start(14) > -1) ? 14 : 15;
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
        TextUtils.recycleMatcher(attr);

        // Look for the 'robots' meta-tag
        if("robots".equalsIgnoreCase(name) && content != null ) {
            // This attribute never seemed to get used, so I'm commented it out
            // curi.putString(A_META_ROBOTS, content);
            RobotsHonoringPolicy policy = curi.getRobotsHonoringPolicy();
            String contentLower = content.toLowerCase();
            if ((policy == null
                || (!policy.isType(curi, RobotsHonoringPolicy.Type.IGNORE)
                    && !policy.isType(curi, RobotsHonoringPolicy.Type.CUSTOM)))
                && (contentLower.indexOf("nofollow") >= 0
                    || contentLower.indexOf("none") >= 0)) {
                // if 'nofollow' or 'none' is specified and the
                // honoring policy is not IGNORE or CUSTOM, end html extraction
                logger.fine("HTML extraction skipped due to robots meta-tag for: "
                                + curi.toString());
                return true;
            }
        } else if ("refresh".equalsIgnoreCase(httpEquiv) && content != null) {
            String refreshUri = content.substring(content.indexOf("=") + 1);
            try {
                Link.addRelativeToBase(curi, refreshUri, HTMLLinkContext.META, Hop.REFER);
            } catch (URIException e) {
                curi.addUriError(e, refreshUri);
            }
        }
        return false;
    }

    /**
     * Process style text.
     * @param curi ProcessorURI we're processing.
     * @param sequence Sequence from underlying ReplayCharSequence. This
     * is TRANSIENT data. Make a copy if you want the data to live outside
     * of this extractors' lifetime.
     * @param endOfOpenTag
     */
    protected void processStyle(ProcessorURI curi, CharSequence sequence,
            int endOfOpenTag) {
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
        ret.append("  ProcessorURIs handled: " + this.numberOfCURIsHandled + "\n");
        ret.append("  Links extracted:   " + this.numberOfLinksExtracted +
            "\n\n");
        return ret.toString();
    }
    
    
    /**
     * Create a suitable XPath-like context from an element name and optional
     * attribute name. 
     * 
     * @param element
     * @param attribute
     * @return CharSequence context
     */
    public static CharSequence elementContext(CharSequence element, CharSequence attribute) {
        return attribute == null? "": element + "/@" + attribute;
    }

}

