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
 * RegExpFilter.java
 * Created on Apr 16, 2003
 *
 * $Header$
 */
package org.archive.crawler.filter;

import java.util.regex.Pattern;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.Filter;
import org.archive.util.TextUtils;


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
		if(o instanceof CandidateURI) {
			input = ((CandidateURI)o).getURIString();
		} else if (o instanceof UURI ){
			input = ((UURI)o).getUriString();
		} else {
			//TODO handle other inputs
			input = o.toString();
		}
		return TextUtils.matches(pattern, input);
	}


	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Filter#initialize()
	 */
	public void initialize(CrawlController c) {
		// TODO Auto-generated method stub
		super.initialize(c);
		String regexp = getStringAt("@regexp");
		pattern = Pattern.compile(regexp);
	}

}