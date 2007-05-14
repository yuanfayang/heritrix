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
 * SeedCachingScope.java
 * Created on Mar 25, 2005
 *
 * $Header$
 */
package org.archive.crawler.scope;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.CrawlController;
import org.archive.net.UURI;

/**
 * A CrawlScope that caches its seed list for the
 * convenience of scope-tests that are based on the 
 * seeds. 
 *
 * @author gojomo
 *
 */
public class SeedCachingScope extends ClassicScope {

    private static final long serialVersionUID = 3L;

    //private static final Logger logger =
    //    Logger.getLogger(SeedCachingScope.class.getName());
    List<UURI> seeds = null; 

    public SeedCachingScope() {
        super();
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.CrawlScope#addSeed(org.archive.crawler.datamodel.UURI)
     */
    public boolean addSeed(CrawlURI curi) {
        if (super.addSeed(curi) == false) {
            // failed
            return false;
        }
        seeds.add(curi.getUURI());
        return true;
    }
    
    /* (non-Javadoc)
     * @see org.archive.crawler.framework.CrawlScope#refreshSeeds()
     */
    public synchronized void refreshSeeds() {
        super.refreshSeeds();
        seeds = null;
        fillSeedsCache();
    }
    
    /* (non-Javadoc)
     * @see org.archive.crawler.framework.CrawlScope#seedsIterator()
     */
    public Iterator<UURI> seedsIterator() {
        fillSeedsCache();
        return seeds.iterator();
    }

    /**
     * Ensure seeds cache is created/filled
     */
    protected synchronized void fillSeedsCache() {
        if (seeds==null) {
            seeds = new ArrayList<UURI>();
            Iterator<UURI> iter = super.seedsIterator();
            while(iter.hasNext()) {
                seeds.add(iter.next());
            }
        }
    }
}
