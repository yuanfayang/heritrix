/*
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
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.GetMethod;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.Processor;

/**
 * Basic link-extraction, from an HTML content-body, 
 * using regular expressions. 
 *
 * @author gojomo
 *
 */
public class ExtractorHTML extends Processor implements CoreAttributeConstants {
	private boolean ignoreUnexpectedHTML = true; // TODO: add config param to change

	private static Logger logger = Logger.getLogger("org.archive.crawler.basic.ExtractorHTML");

    // this pattern extracts either (1) whole <script>...</script>
    // ranges; or (2) any other open-tag with at least one attribute
    // (eg matches "<a href='boo'>" but not "</a>" or "<br>")
	static Pattern RELEVANT_TAG_EXTRACTOR = Pattern.compile(
	 "(?is)<(?:((script.*?)>.*?</script)|(((meta)|(?:\\w+))\\s+.*?)|(!--.*?--))>");
	// groups:
	// 1: SCRIPT SRC=blah>blah</SCRIPT 
	// 2: just script open tag 
	// 3: entire other tag, without '<' '>'
	// 4: element 
	// 5: META
	// 6: !-- blahcomment -- 
	
	
//	// this pattern extracts 'href' or 'src' attributes from 
//	// any open-tag innards matched by the above
//	static Pattern RELEVANT_ATTRIBUTE_EXTRACTOR = Pattern.compile(
//	 "(?is)(\\w+)(?:\\s+|(?:\\s.*?\\s))(?:(href)|(src))\\s*=(?:(?:\\s*\"(.+?)\")|(?:\\s*'(.+?)')|(\\S+))");
//	
//	// this pattern extracts 'robots' attributes
//	static Pattern ROBOTS_ATTRIBUTE_EXTRACTOR = Pattern.compile(
//	 "(?is)(\\w+)\\s+.*?(?:(robots))\\s*=(?:(?:\\s*\"(.+)\")|(?:\\s*'(.+)')|(\\S+))");
	
	// this pattern extracts attributes from any open-tag innards 
	// matched by the above. attributes known to be URIs of various
	// sorts are matched specially
	static Pattern EACH_ATTRIBUTE_EXTRACTOR = Pattern.compile(
	  "(?is)\\s((href)|(action)|(on\\w*)"
	 +"|((?:src)|(?:background)|(?:cite)|(?:longdesc)"
	 +"|(?:usemap)|(?:profile)|(?:datasrc)|(?:for))"
	 +"|(codebase)|((?:classid)|(?:data))|(archive)"
	 +"|(value)|([-\\w]+))"
	 +"\\s*=\\s*"
	 +"(?:(?:\"(.*?)(?:\"|$))"
	 +"|(?:'(.*?)(?:'|$))"
	 +"|(\\S+))");
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
	static Pattern LIKELY_URI_PATH = Pattern.compile(
	 "(\\.{0,2}[^\\.\\n\\r\\s\"']*(\\.[^\\.\\n\\r\\s\"']+)+)");

	/**
	 * @param tags
	 * @param i
	 * @param links
	 * @param embeds
	 */
	private void processGeneralTag(CrawlURI curi, CharSequence element, CharSequence cs) {
		Matcher attr = EACH_ATTRIBUTE_EXTRACTOR.matcher(cs);
		
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
				processLink(curi, value);
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
				codebase = value.toString();
				codebase = codebase.replaceAll("&amp;","&"); // TODO: more HTML deescaping?
				processEmbed(curi,codebase);
			} else if (attr.start(7)>-1) {
				// CLASSID,DATA
				if (resources==null) { resources = new ArrayList(); }
				resources.add(value);
			} else if (attr.start(8)>-1) {
				// ARCHIVE
				if (resources==null) { resources = new ArrayList(); }
				String[] multi = value.toString().split("\\s");
				for(int i = 0; i < multi.length; i++ ) {
					resources.add(multi[i]);
				}
			} else if (attr.start(9)>-1) {
				// VALUE
				if(LIKELY_URI_PATH.matcher(value).matches()) {
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
		// handle codebase/resources
		if (resources == null) {
			return;
		}
		Iterator iter = resources.iterator();
		URI codebaseURI = null;
		try {
			if (codebase != null) {
				codebaseURI = new URI(codebase.toString());
			}
			while(iter.hasNext()) {
				String res = iter.next().toString();
				res = res.replaceAll("&amp;","&"); // TODO: more HTML deescaping?
				if (codebaseURI != null) {
					res = codebaseURI.resolve(res).toString();
				}
				processEmbed(curi,res);
			}
		} catch (URISyntaxException e) {
			System.out.println("BAD CODEBASE "+codebase+" at "+curi);
			e.printStackTrace();
			curi.addLocalizedError(getName(),e,"BAD CODEBASE "+codebase);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
	}
	

	// finds strings in javascript likely to be URIs/paths
	// guessing based on '.' in string, so if highly likely to 
	// get gifs/etc, unable to get many other paths
	// will find false positives
	// TODO: add '/' check, suppress strings being concatenated via '+'?
	static Pattern JAVASCRIPT_LIKELY_URI_EXTRACTOR = Pattern.compile(
	 "(\"|\')(\\.{0,2}[^+\\.\\n\\r\\s\"\']+[^\\.\\n\\r\\s\"\']*(\\.[^\\.\\n\\r\\s\"\']+)+)(\\1)");
	
	/**
	 * @param curi
	 * @param value
	 */
	private void processScriptCode(CrawlURI curi, CharSequence cs) {
		String code = cs.toString();
		code = code.replaceAll("&amp;","&"); // TODO: more HTML deescaping?
		Matcher candidates = JAVASCRIPT_LIKELY_URI_EXTRACTOR.matcher(code);
		while (candidates.find()) {
			curi.addEmbed(candidates.group(2));
		}
	}

	/**
	 * @param curi
	 * @param value
	 */
	private void processLink(CrawlURI curi, CharSequence value) {
		String link = value.toString();
		link = link.replaceAll("&amp;","&"); // TODO: more HTML deescaping?
		if(link.matches("(?i)^javascript:.*")) {
			processScriptCode(curi,value.subSequence(11,value.length()));
		} else {
			curi.addLink(link);
		}
	}



	/**
	 * @param curi
	 * @param value
	 */
	private void processEmbed(CrawlURI curi, CharSequence value) {
		String embed = value.toString();
		embed = embed.replaceAll("&amp;","&"); // TODO: more HTML deescaping?
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
				
		CharSequence cs = get.getResponseBodyAsString();
		
		if (cs==null) {
			// TODO: note problem
			return;
		}
		
		Matcher tags = RELEVANT_TAG_EXTRACTOR.matcher(cs);
		while(tags.find()) {
			if (tags.start(6) > 0) {
				// comment match
				// for now do nothing
			} else if (tags.start(5) > 0) {
			// <meta> match
				if (processMeta(curi,cs.subSequence(tags.start(3), tags.end(3)))) {
					// meta tag included NOFOLLOW; abort processing
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
	}
		
	
	static Pattern NON_HTML_PATH_EXTENSION = Pattern.compile(
		"(?i)(gif)|(jp(e)?g)|(png)|(tif(f)?)|(bmp)|(avi)|(mov)|(mp(e)?g)"+
		"|(mp3)|(mp4)|(swf)|(wav)|(au)|(aiff)|(mid)");
	/**
	 * @param curi
	 * @return
	 */
	private boolean expectedHTML(CrawlURI curi) {
		String path = curi.getUURI().getUri().getPath();
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
		return ! NON_HTML_PATH_EXTENSION.matcher(ext).matches();
	}

	/**
	 * @param curi
	 * @param sequence
	 */
	private void processScript(CrawlURI curi, CharSequence sequence, int endOfOpenTag) {
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
	 * @param curi
	 * @param sequence
	 */
	private boolean processMeta(CrawlURI curi, CharSequence cs) {
		Matcher attr = EACH_ATTRIBUTE_EXTRACTOR.matcher(cs);

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
		if("robots".equalsIgnoreCase(name)) {
			curi.getAList().putString(A_META_ROBOTS,content);
			if(content.indexOf("nofollow")>0) {
				// if 'nofollow' is specified, end html extraction
				return true;
			}
		} else if ("refresh".equalsIgnoreCase(httpEquiv)) {
			curi.addLink(content.substring(content.indexOf("=")+1));
		}
		return false;
	}
}

