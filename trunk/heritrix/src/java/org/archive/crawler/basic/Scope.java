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
package org.archive.crawler.basic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.settings.ComplexType;
import org.archive.crawler.datamodel.settings.CrawlerModule;
import org.archive.crawler.datamodel.settings.CrawlerSettings;
import org.archive.crawler.datamodel.settings.SimpleType;
import org.archive.crawler.filter.HopsFilter;
import org.archive.crawler.filter.OrFilter;
import org.archive.crawler.filter.SeedExtensionFilter;
import org.archive.crawler.filter.TransclusionFilter;
import org.archive.crawler.framework.CrawlController;
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
public class Scope extends CrawlScope {
    private static Logger logger =
        Logger.getLogger("org.archive.crawler.basic.Scope");
    public static final String ATTR_NAME = "scope";
    public static final String ATTR_MAX_LINK_HOPS = "max-link-hops";
    public static final String ATTR_MAX_TRANS_HOPS = "max-trans-hops";
    public static final String ATTR_MODE = "mode";
    public static final String ATTR_FOCUS_FILTER = "focusFilter";
    public static final String ATTR_TRANSITIVE_FILTER = "transitiveFilter";
    public static final String ATTR_EXCLUDE_FILTER = "excludeFilter";

    public static final String MODE_DOMAIN = "domain";
    public static final String MODE_BROAD = "broad";
    public static final String MODE_PATH = "path";
    public static final String MODE_HOST = "host";
    public static final String MODE_USER_DEFINED = "userdefined";

    static final String[] allowedModes =
        new String[] {
            MODE_BROAD,
            MODE_DOMAIN,
            MODE_HOST,
            MODE_PATH,
            MODE_USER_DEFINED };
    static final String defaultMode = MODE_DOMAIN;

    Filter focusFilter;
    Filter transitiveFilter;
    OrFilter excludeFilter;
    List seeds;
    String mode = null;

    public Scope(String name) {
        super(name);

        addElementToDefinition(
            new SimpleType(
                ATTR_MAX_LINK_HOPS,
                "Max link hops",
                new Integer(25)));
        addElementToDefinition(
            new SimpleType(
                ATTR_MAX_TRANS_HOPS,
                "Max trans hops",
                new Integer(5)));
        addElementToDefinition(
            new SimpleType(ATTR_MODE, "Mode", defaultMode, allowedModes));

        ComplexType filter =
            (ComplexType) addElementToDefinition(
                new CrawlerModule(ATTR_FOCUS_FILTER));
        filter.setTransient(true);
        filter =
            (ComplexType) addElementToDefinition(
                new CrawlerModule(ATTR_TRANSITIVE_FILTER));
        filter.setTransient(true);
        excludeFilter =
            (OrFilter) addElementToDefinition(
                new OrFilter(ATTR_EXCLUDE_FILTER));
    }

    public Scope() {
        this(ATTR_NAME);
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Filter#initialize(org.archive.crawler.framework.CrawlController)
     */
    public void initialize(CrawlController controller) {
        super.initialize(controller);
        CrawlerSettings settings = globalSettings();
        // setup focusFilter
        try {
            mode = (String) getAttribute(settings, ATTR_MODE);

            if (mode == null || mode.equals(MODE_USER_DEFINED)) {
                Object filter = getAttribute(settings, ATTR_FOCUS_FILTER);
                if (filter instanceof Filter) {
                    focusFilter = (Filter) filter;
                    focusFilter.setTransient(false);
                }
            } else if (mode.equals(MODE_BROAD)) {
                focusFilter = null;
            } else {
                // SeedExtensionFilter implied
                focusFilter = new SeedExtensionFilter(ATTR_FOCUS_FILTER);
                focusFilter.setTransient(true);
                setAttribute(settings, focusFilter);
            }
            if (focusFilter != null) {
                focusFilter.initialize(controller);
                // only set up transitiveFilter if focusFilter set
                Object filter = getAttribute(settings, ATTR_TRANSITIVE_FILTER);
                if (filter instanceof Filter) {
                    transitiveFilter = (Filter) filter;
                    transitiveFilter.setTransient(false);
                }
                if (transitiveFilter == null) {
                    transitiveFilter =
                        new TransclusionFilter(ATTR_TRANSITIVE_FILTER);
                    transitiveFilter.setTransient(true);
                    setAttribute(settings, transitiveFilter);
                }
                transitiveFilter.initialize(controller);
            }

            // setup exclude filter
            HopsFilter hopsFilter = null;
            if (((Integer) getAttribute(settings, ATTR_MAX_LINK_HOPS))
                .intValue()
                != 0) {
                // hopsFilter implied
                hopsFilter = new HopsFilter("hopsFilter");
                hopsFilter.setTransient(true);
                excludeFilter.addFilter(settings, hopsFilter);
            }
            if (excludeFilter.isEmpty(settings)) {
                excludeFilter = null;
            } else {
                excludeFilter.initialize(controller);
            }

        } catch (InvalidAttributeValueException e) {
            logger.severe(e.getMessage());
        } catch (AttributeNotFoundException e) {
            logger.severe(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 
     */
    private void cacheSeeds() {
        seeds = new ArrayList();
        Iterator iter = super.getSeedsIterator();
        while (iter.hasNext()) {
            seeds.add(iter.next());
        }
    }

    /** 
     * @see org.archive.crawler.framework.Filter#innerAccepts(java.lang.Object)
     */
    protected boolean innerAccepts(Object o) {
        return ((isSeed(o) || focusAccepts(o)) || transitiveAccepts(o))
            && !excludeAccepts(o);
    }

    //	/**
    //	 * @param o
    //	 * @return
    //	 */
    //	private boolean alwaysAccepts(Object o) {
    //		// TODO Auto-generated method stub
    //		return false;
    //	}

    /**
     * @param o
     * @return True if exclude filter accepts passed object.
     */
    private boolean excludeAccepts(Object o) {
        if (excludeFilter == null) {
            return false;
        }
        return excludeFilter.accepts(o);
    }

    /**
     * @param o
     * @return True if transitive filter accepts passed object.
     */
    private boolean transitiveAccepts(Object o) {
        if (transitiveFilter == null) {
            return true;
        }
        return transitiveFilter.accepts(o);
    }

    /**
     * @param o
     * @return True if focus filter accepts passed object.
     */
    private boolean focusAccepts(Object o) {
        if (focusFilter == null) {
            return true;
        }
        return focusFilter.accepts(o);
    }

    private boolean isSeed(Object o) {
        return o instanceof CandidateURI && ((CandidateURI) o).getIsSeed();
    }

    public Filter getExcludeFilter() {
        return excludeFilter;
    }

    public Filter getFocusFilter() {
        return focusFilter;
    }

    public Filter getTransitiveFilter() {
        return transitiveFilter;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.CrawlScope#getSeedsIterator()
     */
    public Iterator getSeedsIterator() {
        if (focusFilter == null) {
            // a cached seeds list isn't necessary for scope tests
            return super.getSeedsIterator();
        }
        // seeds should be in memory for scope tests
        if (seeds == null) {
            cacheSeeds();
        }
        return seeds.iterator();
    }

    /**
     * @return Mode
     */
    public String getMode() {
        return mode;
    }

}
