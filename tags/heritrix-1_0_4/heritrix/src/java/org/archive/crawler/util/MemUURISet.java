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
 * MemUURISet.java
 * Created on Sep 15, 2003
 *
 * $Header$
 */
package org.archive.crawler.util;

import java.util.HashSet;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.datamodel.UURISet;

/**
 * @author gojomo
 *
 */
public class MemUURISet extends HashSet implements UURISet {

    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.UURISet#size()
     */
    public long count() {
        return size();
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.UURISet#contains(org.archive.crawler.datamodel.UURI)
     */
    public boolean contains(UURI u) {
        return contains((Object)u);
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.UURISet#contains(org.archive.crawler.datamodel.CrawlURI)
     */
    public boolean contains(CandidateURI curi) {
        return contains((Object)curi.getUURI());
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.UURISet#add(org.archive.crawler.datamodel.UURI)
     */
    public void add(UURI u) {
        add((Object)u);
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.UURISet#remove(org.archive.crawler.datamodel.UURI)
     */
    public void remove(UURI u) {
        remove((Object)u);
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.UURISet#add(org.archive.crawler.datamodel.CrawlURI)
     */
    public void add(CandidateURI curi) {
        add(curi.getUURI());
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.UURISet#remove(org.archive.crawler.datamodel.CrawlURI)
     */
    public void remove(CandidateURI curi) {
        remove(curi.getUURI());
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.UURISet#quickContains(org.archive.crawler.datamodel.UURI)
     */
    public boolean quickContains(UURI u) {
        return contains(u);
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.UURISet#quickContains(org.archive.crawler.datamodel.CandidateURI)
     */
    public boolean quickContains(CandidateURI curi) {
        return contains(curi);
    }

}
