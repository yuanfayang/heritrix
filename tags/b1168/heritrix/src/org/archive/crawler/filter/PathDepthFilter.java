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
 * HopsFilter.java
 * Created on Oct 3, 2003
 *
 * $Header$
 */
package org.archive.crawler.filter;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.Filter;

/**
 * Accepts all urls passed in with a path depth 
 * less or equal than the max-path-depth
 * value. 
 * 
 * @author Igor Ranitovic
 *
 */
public class PathDepthFilter extends Filter {
	int maxPathDepth = Integer.MAX_VALUE;
    char slash = '/';
	String path;
	
	/* (non-Javadoc)
	 * @see org.archive.crawler.framework.Filter#innerAccepts(java.lang.Object)
	 */
	protected boolean innerAccepts(Object o) {
		if(o instanceof CandidateURI) {
			path = ((CandidateURI)o).getUURI().getPath();
		} else if (o instanceof UURI ){
			path = ((UURI)o).getPath();
		}else{
			path = null;
		}

		if (path == null){
			return true;
		}
		
		int count = 0;
		for (int i = path.indexOf(slash);
			i != -1;
			i = path.indexOf(slash, i + 1)) {
			count++;
		}
		return (count <= maxPathDepth);
	}

	public void initialize(CrawlController c) {
		super.initialize(c);
		maxPathDepth = getIntAt("@max-path-depth", maxPathDepth);
	}

}