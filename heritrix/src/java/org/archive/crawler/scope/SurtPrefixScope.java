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
 * SurtPrefixScope.java
 * Created on Oct 1, 2003
 *
 * $Header$
 */
package org.archive.crawler.scope;

import java.util.TreeSet;

import org.archive.crawler.datamodel.UURI;

/**
 * A specialized CrawlScope suitable for the most common crawl needs.
 * 
 * Roughly, as with other existing CrawlScope variants, SurtPrefixScope's logic
 * is that a URI is included if:
 * <pre>
 *  ( isSeed(uri) || focusFilter.accepts(uri) ) ||
 *     transitiveFilter.accepts(uri) ) && ! excludeFilter.accepts(uri)
 * </pre>
 * Specifically, SurtPrefixScope uses a SurtFilter to test for focus-inclusion.
 * 
 * @author gojomo
 *  
 */
public class SurtPrefixScope extends RefinedScope {
    TreeSet surts = new TreeSet();

    public SurtPrefixScope(String name) {
        super(name);
        setDescription(
                  "A scope for crawls limited to regions of the web defined by"
                + "a set of SURT prefixes. (The SURT form of a URI has had its"
                + "hostname reordered to ease sorting and grouping by domain"
                + "hierarchies.)");
    }

    /**
     * Check if a URI is part of this scope.
     * 
     * @param o
     *            An instance of UURI or of CandidateURI.
     * @return True if focus filter accepts passed object.
     */
    protected boolean focusAccepts(Object o) {
        UURI u = getUURI(o);
        if (u == null) {
            return false;
        }
        String candidateSurt = u.getSurtForm();
        // also want to treat https as http
        if(candidateSurt.startsWith("https:")) {
            candidateSurt = "http:"+candidateSurt.substring(6);
        }
        String precedent = (String) surts.subSet("",candidateSurt).last();
        return candidateSurt.startsWith(precedent);
    }
}
