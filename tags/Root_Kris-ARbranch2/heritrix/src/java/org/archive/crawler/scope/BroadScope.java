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
 * CrawlScope.java
 * Created on Oct 1, 2003
 *
 * $Header$
 */
package org.archive.crawler.scope;

import java.io.File;

import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.CrawlScope;

/**
 * A CrawlScope instance defines which URIs are "in"
 * a particular crawl.
 *
 * It is essentially a Filter which determines, looking at
 * the totality of information available about a
 * CandidateURI/CrawlURI instamce, if that URI should be
 * scheduled for crawling.
 *
 * <p>Dynamic information inherent in the discovery of the
 * URI -- such as the path by which it was discovered --
 * may be considered.
 *
 * <p>Dynamic information which requires the consultation
 * of external and potentially volatile information --
 * such as current robots.txt requests and the history
 * of attempts to crawl the same URI -- should NOT be
 * considered. Those potentially high-latency decisions
 * should be made at another step. .
 *
 * @author gojomo
 *
 */
public class BroadScope extends CrawlScope {

    /**
     * Constructor.
     *
     * @param name Name of this crawlscope.
     */
    public BroadScope(String name) {
        super(name);
        setDescription(
            "A scope for broad crawls. Crawls made with this scope will not " +            "be limited to the hosts or domains of its seeds. NOTE: Such a " +
            "crawl will quickly discover enough unique hosts to exceed " +
			"Heritrix 1.0.0's memory resources. Thus broad crawls are not " +
			"recommended except as stress-tests.");
    }

    /**
     * Override so can intercept creation of seed list making it be a
     * non-caching seed list.
     *
     * @param seedfile Seedfile to use as seed source.
     * @param c CrawlController.
     * @param caching True if seed list created is to cache seeds.
     */
    protected synchronized void createSeedlist(File seedfile, CrawlController c,
            boolean caching) {
        super.createSeedlist(seedfile, c, false);
    }

    /**
     * @param o the URI to check.
     * @return True if transitive filter accepts passed object.
     */
    protected boolean transitiveAccepts(Object o) {
        return true;
    }

    /** Check if URI is accepted by the focus of this scope.
     *
     * This method should be overridden in subclasses.
     *
     * @param o the URI to check.
     * @return True if focus filter accepts passed object.
     */
    protected boolean focusAccepts(Object o) {
        return true;
    }
}
