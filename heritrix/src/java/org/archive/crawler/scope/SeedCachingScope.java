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
import java.util.logging.Logger;

import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.framework.CrawlScope;

/**
 * A CrawlScope that caches its seed list for the
 * convenience of scope-tests that are based on the 
 * seeds. 
 *
 * @author gojomo
 *
 */
public class SeedCachingScope extends CrawlScope {
    private static final Logger logger =
        Logger.getLogger(SeedCachingScope.class.getName());
    List seeds;

    public SeedCachingScope(String name) {
        super(name);
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.CrawlScope#addSeed(org.archive.crawler.datamodel.UURI)
     */
    public boolean addSeed(UURI uuri) {
        if (super.addSeed(uuri) == false) {
            // failed
            return false;
        }
        List newSeeds = new ArrayList(seeds);
        newSeeds.add(uuri);
        seeds = newSeeds;
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
    public Iterator seedsIterator() {
        fillSeedsCache();
        return seeds.iterator();
    }

    /**
     * Ensure seeds cache is created/filled
     */
    protected synchronized void fillSeedsCache() {
        if (seeds==null) {
            seeds = new ArrayList();
            Iterator iter = super.seedsIterator();
            while(iter.hasNext()) {
                seeds.add(iter.next());
            }
        }
    }
}
