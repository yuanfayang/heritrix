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
package org.archive.crawler.framework;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

import javax.management.AttributeNotFoundException;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.settings.CrawlerSettings;
import org.archive.crawler.datamodel.settings.SimpleType;
import org.archive.crawler.util.SeedsInputIterator;
import org.archive.util.DevUtils;

/**
 * A CrawlScope instance defines which URIs are "in"
 * a particular crawl. 
 * 
 * It is essentially a Filter which determines, looking at 
 * the totality of information available about a 
 * CandidateURI/CrawlURI instamce, if that URI should be 
 * scheduled for crawling.
 * 
 * Dynamic information inherent in the discovery of the
 * URI -- such as the path by which it was discovered --
 * may be considered.
 * 
 * Dynamic information which requires the consultation 
 * of external and potentially volatile information --
 * such as current robots.txt requests and the history
 * of attempts to crawl the same URI -- should NOT be
 * considered. Those potentially high-latency decisions
 * should be made at another step. . 
 * 
 * @author gojomo
 *
 */
public abstract class CrawlScope extends Filter {
    public static final String ATTR_SEEDS = "seedsfile";

    // a monotonically increasing version number, for scopes that may change
    int version = 0;

    /**
     * @param name
     * @param description
     */
    public CrawlScope(String name) {
        super(name, "Crawl scope");
        addElementToDefinition(new SimpleType(ATTR_SEEDS, "File from which to extract seeds", "seeds.txt"));
    }

    public void initialize(CrawlerSettings settings) {
        super.initialize(settings);
        // TODO let configuration info specify seedExtractor
    }

    /**
     * Return the scope version. Increments if the scope changes,
     * for example by operator edits during a crawl. A CandidateURI
     * remembers any scope version it was previously accepted by,
     * which helps avoid redundant scope checks. 
     * 
     * @return Scope version.
     */
    public int getVersion() {
        return version;
    }

    public String toString() {
        return "CrawlScope<" + getName() + ">";
    }

    /**
     * Return an iterator of the seeds in this scope. The seed
     * input is taken from either the configuration file, or the 
     * external seed file it specifies.
     * 
     * @return An iterator of the seeds in this scope.
     */
    public Iterator getSeedsIterator() {
        try {
            String fileName = (String) getAttribute(null, ATTR_SEEDS);
            BufferedReader reader = new BufferedReader(new FileReader(fileName));
            return new SeedsInputIterator(reader,
                getController());
        } catch (IOException e) {
            DevUtils.warnHandle(e, "problem reading seeds");
            return null;
        } catch (AttributeNotFoundException e) {
            DevUtils.warnHandle(e, "problem reading seeds");
            return null;
        }
    }

    //	/**
    //	 * Adds a seed to this scope -- and the associated crawl/frontier.
    //	 * 
    //	 * TODO determine if this is appropriate place for this
    //	 * @param u
    //	 */
    //	public void addSeed(UURI u){
    //		seeds.add(u);
    //		CandidateURI caUri = new CandidateURI(u);
    //		caUri.setIsSeed(true);
    //		controller.getFrontier().schedule(caUri);
    //	}

    /** 
     * Returns whether the given object (typically a CandidateURI) falls
     * within this scope. If so, stamps the object with the current scope
     * version, so that subsequent checks are expedited. IMPORTANT NOTE: 
     * assumes the same CandidateURI object will not be tested  against 
     * different CrawlScope objects. 
     * @param o
     * @return Whether the given object (typically a CandidateURI) falls
     * within this scope. If so, stamps the object with the current scope.
     */
    public boolean accepts(Object o) {
        // expedited check
        if (o instanceof CandidateURI
            && ((CandidateURI) o).getScopeVersion() == version) {
            return true;
        }
        boolean result = super.accepts(o);
        // stamp with version for expedited check
        if (result == true && o instanceof CandidateURI) {
            ((CandidateURI) o).setScopeVersion(version);
        }
        return result;
    }
}
