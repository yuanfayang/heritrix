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

import javax.management.AttributeNotFoundException;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.CrawlScope;
import org.archive.crawler.framework.Filter;

/**
 * Accepts (returns  for)) for all CandidateURIs passed in
 * with a link-hop-count greater than the max-link-hops
 * value.
 *
 * @author gojomo
 *
 */
public class HopsFilter extends Filter {


    /**
     * @param name
     */
    public HopsFilter(String name) {
        super(name, "Hops filter");
    }

    int maxLinkHops = Integer.MAX_VALUE;
    int maxTransHops = Integer.MAX_VALUE;

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Filter#innerAccepts(java.lang.Object)
     */
    protected boolean innerAccepts(Object o) {
        if(! (o instanceof CandidateURI)) {
            return false;
        }
        String path = ((CandidateURI)o).getPathFromSeed();
        int linkCount = 0;
        int transCount = 0;
        for(int i=path.length()-1;i>=0;i--) {
            if(path.charAt(i)=='L') {
                linkCount++;
            } else if (linkCount==0) {
                transCount++;
            }
        }
        if (o instanceof CrawlURI) {
            CrawlURI curi = (CrawlURI) o;
            CrawlScope scope =
                (CrawlScope) globalSettings().getModule(CrawlScope.ATTR_NAME);
            try {
                maxLinkHops =
                    ((Integer) scope
                        .getAttribute(CrawlScope.ATTR_MAX_LINK_HOPS, curi))
                        .intValue();
                maxTransHops =
                    ((Integer) scope
                        .getAttribute(CrawlScope.ATTR_MAX_TRANS_HOPS, curi))
                        .intValue();
            } catch (AttributeNotFoundException e) {
                logger.severe(e.getMessage());
                return true; // Basically the filter is inactive if this occurs.
            }
        }

        return (linkCount > maxLinkHops)|| (transCount>maxTransHops);
    }
}
