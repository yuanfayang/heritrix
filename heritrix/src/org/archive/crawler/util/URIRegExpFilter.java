/* 
 * RegExpFilter.java
 * Created on Apr 16, 2003
 *
 * $Header$
 */
package org.archive.crawler.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.framework.Filter;


/**
 * Compares passed object -- a CrawlURI, UURI, or String --
 * against a regular expression, accepting matches. 
 * 
 * @author Gordon Mohr
 */
public class URIRegExpFilter extends Filter {
	Pattern pattern;

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Filter#accepts(java.lang.Object)
	 */
	protected boolean innerAccepts(Object o) {
		String input = null;
		// TODO consider changing this to ask o for its matchString
		if(o instanceof CrawlURI) {
			input = ((CrawlURI)o).getURIString();
		} else if (o instanceof UURI ){
			input = ((UURI)o).getUri().toString();
		} else {
			//TODO handle other inputs
			
		}
		Matcher m = pattern.matcher(input);
		return m.matches();
	}


	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Filter#initialize()
	 */
	public void initialize() {
		// TODO Auto-generated method stub
		super.initialize();
		String regexp = getStringAt("@regexp");
		pattern = Pattern.compile(regexp);
	}

}
