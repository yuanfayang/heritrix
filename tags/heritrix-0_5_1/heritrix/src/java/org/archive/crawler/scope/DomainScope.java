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
 * BasicScope.java
 * Created on Oct 1, 2003
 *
 * $Header$
 */
package org.archive.crawler.scope;

import java.util.Iterator;
import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.filter.TransclusionFilter;
import org.archive.crawler.framework.CrawlScope;
import org.archive.crawler.framework.Filter;

/**
 * A core CrawlScope suitable for the most common
 * crawl needs.
 *
 * Roughly, its logic is that a URI is included if:
 *
 *    (( isSeed(uri) || focusFilter.accepts(uri) )
 *      || transitiveFilter.accepts(uri) )
 *     && ! excludeFilter.accepts(uri)
 *
 * The focusFilter may be specified by either:
 *   - adding a 'mode' attribute to the
 *     <code>scope</code> element. mode="broad" is equivalent
 *     to no focus; modes "path", "host", and "domain"
 *     imply a SeedExtensionFilter will be used, with
 *     the <code>scope</code> element providing its configuration
 *   - adding a <code>focus</code> subelement
 * If unspecified, the focusFilter will default to
 * an accepts-all filter.
 *
 * The transitiveFilter may be specified by supplying
 * a <code>transitive</code> subelement. If unspecified, a
 * TransclusionFilter will be used, with the <code>scope</code>
 * element providing its configuration.
 *
 * The excludeFilter may be specified by supplying
 * a <code>exclude</code> subelement. If unspecified, a
 * accepts-none filter will be used -- meaning that
 * no URIs will pass the filter and thus be excluded.
 *
 * @author gojomo
 *
 */
public class DomainScope extends CrawlScope {

    public static final String ATTR_TRANSITIVE_FILTER = "transitiveFilter";

    Filter transitiveFilter;

    public DomainScope(String name) {
        super(name);
        setDescription(
            "A scope for domain crawls. Crawls made with this scope will be " +
            "limited to the domain of it's seeds. It will however reach " +
            "subdomains of the seeds' original domains. www[#].host is considered " +
            "to be the same as host.");

        this.transitiveFilter = (Filter) addElementToDefinition(
                new TransclusionFilter(ATTR_TRANSITIVE_FILTER));
    }

    /**
     * @param o
     * @return True if transitive filter accepts passed object.
     */
    protected boolean transitiveAccepts(Object o) {
        return this.transitiveFilter.accepts(o);
    }

    /**
     * @param o An instance of UURI or of CandidateURI.
     * @return True if focus filter accepts passed object.
     */
    protected boolean focusAccepts(Object o) {
        UURI u = null;
        if (o instanceof UURI) {
            u = (UURI) o;
        } else if (o instanceof CandidateURI) {
            u = ((CandidateURI) o).getUURI();
        }
        if (u == null) {
            return false;
        }
        Iterator iter = getSeedsIterator();
        assert iter != null: "Iterator is null.";
        while (iter.hasNext()) {
            UURI s = (UURI) iter.next();
            if(isSameHost(s, u)) {
                return true;
            }

            // might be a close-enough match
            String seedDomain = s.getHost();
            if (seedDomain == null) {
                // getHost can come back null.  See
                // "[ 910120 ] java.net.URI#getHost fails when leading digit"
                continue;
            }
            // strip www[#]
            seedDomain = seedDomain.replaceFirst("^www\\d*", "");
            String candidateDomain = u.getHost();
            if (candidateDomain == null) {
                // either an opaque, unfetchable, or unparseable URI
                continue;
            }
            if (seedDomain
                .regionMatches(
                    0,
                    candidateDomain,
                    candidateDomain.length() - seedDomain.length(),
                    seedDomain.length())) {
                // domain suffix congruence
                return true;
            } // else keep trying other seeds
        }
        // if none found, fail
        return false;
    }

}
