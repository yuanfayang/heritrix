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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.filter.OrFilter;
import org.archive.crawler.scope.SeedFileIterator;
import org.archive.crawler.settings.CrawlerSettings;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.Type;
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
 * should be made at another step.
 *
 * @author gojomo
 *
 */
public class CrawlScope extends Filter {

    private static final Logger logger =
        Logger.getLogger(CrawlScope.class.getName());
    public static final String ATTR_NAME = "scope";
    public static final String ATTR_SEEDS = "seedsfile";
    public static final String ATTR_EXCLUDE_FILTER = "exclude-filter";
    public static final String ATTR_MAX_LINK_HOPS = "max-link-hops";
    public static final String ATTR_MAX_TRANS_HOPS = "max-trans-hops";

    private OrFilter excludeFilter;


    /** Constructs a new CrawlScope.
     *
     * @param name the name is ignored since it always have to be the value of
     *        the constant ATT_NAME.
     */
    public CrawlScope(String name) {
        // 'name' is never used.
        super(ATTR_NAME, "Crawl scope");
        Type t;
        t = addElementToDefinition(new SimpleType(ATTR_SEEDS,
                "File from which to extract seeds.", "seeds.txt"));
        t.setOverrideable(false);
        t.setExpertSetting(true);
        addElementToDefinition(new SimpleType(ATTR_MAX_LINK_HOPS,
                "Max link hops to include. URIs more than this number " +
                "of links from a seed will not be ruled in-scope. (Such " +
                "determination does not preclude later inclusion if a " +
                "shorter path is later discovered.)", new Integer(25)));
        addElementToDefinition(new SimpleType(
                ATTR_MAX_TRANS_HOPS,
                "Max transitive hops (embeds, referrals, preconditions) to include. " +
                "URIs reached by more than this number of transitive hops will not " +
                "be ruled in-scope, even if otherwise on an in-focus site. (Such " +
                "determination does not preclude later inclusion if a " +
                "shorter path is later discovered.)",
                new Integer(5)));
        this.excludeFilter = (OrFilter) addElementToDefinition(new OrFilter(
                ATTR_EXCLUDE_FILTER));

        // Try to preserve the values of these attributes when we exchange
        // scopes.
        setPreservedFields(new String[] { ATTR_SEEDS, ATTR_MAX_LINK_HOPS,
                ATTR_MAX_TRANS_HOPS, ATTR_EXCLUDE_FILTER});
    }

    /** Default constructor.
     */
    public CrawlScope() {
        this(ATTR_NAME);
    }

    /**
     * Initialize is called just before the crawler starts to run.
     *
     * The settings system is up and initialized so can be used.  This
     * initialize happens after {@link #earlyInitialize(CrawlerSettings)}.
     *
     * @param controller Controller object.
     */
    public void initialize(CrawlController controller) {
        // by default do nothing (subclasses override)
    }

    public String toString() {
        return "CrawlScope<" + getName() + ">";
    }

    /**
     * @param o An instance of UURI or of CandidateURI.
     * @return Make into a UURI.
     */
    protected UURI getUURI(final Object o) {
        UURI u = null;
        if (o instanceof UURI) {
            u = (UURI)o;
        } else if (o instanceof CandidateURI) {
            u = ((CandidateURI) o).getUURI();
        } else {
            if (o != null) {
                throw new IllegalArgumentException("Passed wrong type: " + o);
            }
        }
        return u;
    }

    /**
     * Refresh seeds.
     *
     */
    public void refreshSeeds() {
        // by default do nothing (subclasses which cache should override)
    }

    /**
     * @return Seed list file or null if problem getting settings file.
     */
    protected File getSeedfile() {
        File file = null;
        try {
            file = getSettingsHandler().getPathRelativeToWorkingDirectory(
                (String)getAttribute(ATTR_SEEDS));
            if (!file.exists() || !file.canRead()) {
                throw new IOException("Seeds file " +
                    file.getAbsolutePath() + " does not exist or unreadable.");
            }
        } catch (IOException e) {
            DevUtils.warnHandle(e, "problem reading seeds");
        } catch (AttributeNotFoundException e) {
            DevUtils.warnHandle(e, "problem reading seeds");
        } catch (MBeanException e) {
            DevUtils.warnHandle(e, "problem reading seeds");
            e.printStackTrace();
        } catch (ReflectionException e) {
            DevUtils.warnHandle(e, "problem reading seeds");
            e.printStackTrace();
        }

        return file;
    }

    /**
     * Returns whether the given object (typically a CandidateURI) falls
     * within this scope.
     *
     * @param o Object to test.
     * @return Whether the given object (typically a CandidateURI) falls
     * within this scope.
     */
    protected boolean innerAccepts(Object o) {
        return ((isSeed(o) || focusAccepts(o)) || additionalFocusAccepts(o) ||
                transitiveAccepts(o)) && !excludeAccepts(o);
    }

    /** Check if there is too many hops
     *
     * @param o URI to check.
     * @return true if too many hops.
     */
    protected boolean exceedsMaxHops(Object o) {
        if(! (o instanceof CandidateURI)) {
            return false;
        }

        int maxLinkHops = 0;
        int maxTransHops = 0;

        try {
            maxLinkHops = ((Integer) getAttribute(
                    o, CrawlScope.ATTR_MAX_LINK_HOPS)).intValue();
            maxTransHops = ((Integer) getAttribute(
                    o, CrawlScope.ATTR_MAX_TRANS_HOPS)).intValue();
        } catch (AttributeNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        CandidateURI cand = (CandidateURI)o;

        String path = cand.getPathFromSeed();
        int linkCount = 0;
        int transCount = 0;
        for(int i=path.length()-1;i>=0;i--) {
            if(path.charAt(i)=='L') {
                linkCount++;
            } else if (linkCount==0) {
                transCount++;
            }
        }
        return (linkCount > maxLinkHops) || (transCount > maxTransHops);
    }

    /**
     * @param o the URI to check.
     * @return True if transitive filter accepts passed object.
     */
    protected boolean transitiveAccepts(Object o) {
        return false;
    }

    /** Check if URI is accepted by the focus of this scope.
     *
     * This method should be overridden in subclasses.
     *
     * @param o the URI to check.
     * @return True if focus filter accepts passed object.
     */
    protected boolean focusAccepts(Object o) {
        // The CrawlScope doesn't accept any URIs
        return false;
    }

    /** Check if URI is excluded by any filters.
     *
     * @param o the URI to check.
     * @return True if exclude filter accepts passed object.
     */
    protected boolean excludeAccepts(Object o) {
        return (this.excludeFilter.isEmpty(o))?
            exceedsMaxHops(o):
            this.excludeFilter.accepts(o) || exceedsMaxHops(o);
    }

    /** Check if a URI is in the seeds.
     *
     * @param o the URI to check.
     * @return true if URI is a seed.
     */
    private boolean isSeed(Object o) {
        return o instanceof CandidateURI && ((CandidateURI) o).isSeed();
    }

    /**
     * @param a First UURI of compare.
     * @param b Second UURI of compare.
     * @return True if UURIs are of same host.
     */
    protected boolean isSameHost(UURI a, UURI b) {
        boolean isSameHost = false;
        if (a != null && b != null) {
            // getHost can come back null.  See
            // "[ 910120 ] java.net.URI#getHost fails when leading digit"
            try {
                if (a.getHost() != null && b.getHost() != null) {
                    if (a.getHost().equals(b.getHost())) {
                        isSameHost = true;
                    }
                }
            }
            catch (URIException e) {
                logger.severe("Failed compare of " + a + " " + b + ": " +
                    e.getMessage());
            }
        }
        return isSameHost;
    }

    /** Check if URI is accepted by the additional focus of this scope.
     *
     * This method should be overridden in subclasses.
     *
     * @param o the URI to check.
     * @return True if additional focus filter accepts passed object.
     */
    protected boolean additionalFocusAccepts(Object o){
        return false;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.settings.ModuleType#listUsedFiles(java.util.List)
     */
    public void listUsedFiles(List list){
        // Add seed file
        try {
            File file = getSettingsHandler().getPathRelativeToWorkingDirectory(
                    (String)getAttribute(ATTR_SEEDS));
            list.add(file.getAbsolutePath());
        } catch (AttributeNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (MBeanException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ReflectionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Take note of a situation (such as settings edit) where
     * involved reconfiguration (such as reading from external
     * files) may be necessary.
     */
    public void kickUpdate() {
        // TODO:  evaluate whether refreshSeeds brings in too 
        // much (eg if crawling from a million seeds)    
        refreshSeeds();
        excludeFilter.kickUpdate();
    }

    /**
     * Gets an iterator over all configured seeds. Subclasses
     * which cache seeds in memory can override with more
     * efficient implementation. 
     *
     * @return Iterator, perhaps over a disk file, of seeds
     */
    public Iterator seedsIterator() {
        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(getSeedfile()));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        return new SeedFileIterator(br);
    }

    /**
     * Add a new seed to scope. By default, simply appends
     * to seeds file, though subclasses may handle differently.
     *
     * This method is *not* sufficient to get the new seed 
     * scheduled in the Frontier for crawling -- it only 
     * affects the Scope's seed record (and decisions which
     * flow from seeds). 
     *
     * @param uuri UURI to add
     * @return true if successful, false if add failed for any reason
     */
    public boolean addSeed(UURI uuri) {
        File f = getSeedfile();
        if (f != null) {
            try {
                FileWriter fw = new FileWriter(f, true);
                // Write to new (last) line the URL.
                fw.write("\n");
                fw.write(uuri.toString());
                fw.flush();
                fw.close();
                return true;
            } catch (IOException e) {
                DevUtils.warnHandle(e, "problem writing new seed");
            }
        }
        return false; 
    }
}
