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
 * OrFilter.java
 * Created on Nov 13, 2003
 *
 * $Header$
 */
package org.archive.crawler.filter;

import java.util.ArrayList;
import java.util.Iterator;

import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.Filter;

/**
 * OrFilter allows any number of other filters to be set up
 * inside it, as <filter> child elements. If any of those
 * children accept a presented object, the OrFilter will 
 * also accept it. 
 * 
 * @author gojomo
 *
 */
public class OrFilter extends Filter {
	/**
	 * XPath to any specified filters
	 */
	private static String XP_FILTERS = "filter";
	ArrayList filters = new ArrayList();

	protected boolean innerAccepts(Object o) {
		if (filters.isEmpty()) {
			return true;
		}
		Iterator iter = filters.iterator();
		while(iter.hasNext()) {
			Filter f = (Filter)iter.next();
			if( f.accepts(o) ) {
				return true; 
			}
		}
		return false;
	}

	public void addFilter(Filter f) {
		filters.add(f);
	}

	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Filter#initialize()
	 */
	public void initialize(CrawlController c) {
		super.initialize(c);
		if(xNode!=null) {
			instantiateAllInto(XP_FILTERS,filters);
		}
		Iterator iter = filters.iterator();
		while(iter.hasNext()) {
			Object o = iter.next();
			Filter f = (Filter)o;
			f.initialize(c);
		}
	}
}
