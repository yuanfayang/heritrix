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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.datamodel.settings.CrawlerSettings;
import org.archive.crawler.datamodel.settings.SimpleType;
import org.archive.crawler.datamodel.settings.Type;
import org.archive.crawler.filter.OrFilter;
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
public class CrawlScope extends Filter {
    public static final String ATTR_NAME = "scope";
    public static final String ATTR_SEEDS = "seedsfile";
    public static final String ATTR_EXCLUDE_FILTER = "exclude-filter";
    public static final String ATTR_MAX_LINK_HOPS = "max-link-hops";
    public static final String ATTR_MAX_TRANS_HOPS = "max-trans-hops";

    private List seeds = null;
    private boolean seedsCached = false;
    private OrFilter excludeFilter;

    /** Constructs a new CrawlScope.
     * 
     * @param name the name is ignored since it always have to be the value of
     *        the constant ATT_NAME.
     */
    public CrawlScope(String name) {
        super(ATTR_NAME, "Crawl scope");
        Type t;
        t = addElementToDefinition(new SimpleType(ATTR_SEEDS,
                "File from which to extract seeds.", "seeds.txt"));
        t.setOverrideable(false);
        t.setExpertSetting(true);
        addElementToDefinition(new SimpleType(ATTR_MAX_LINK_HOPS,
                "Max link hops to include", new Integer(25)));
        addElementToDefinition(new SimpleType(
                ATTR_MAX_TRANS_HOPS,
                "Max transitive hops (embeds, referrals, preconditions) to include",
                new Integer(5)));
        excludeFilter = (OrFilter) addElementToDefinition(new OrFilter(
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

    public String toString() {
        return "CrawlScope<" + getName() + ">";
    }

    /** Refreshes the seeds cache.
     * 
     * This method could safely be overridden by a null implementation for
     * scopes that doesn't need the seeds to be cached. For example is there
     * no reason for the BroadScope to cache seeds since it doesn't have to
     * check the seeds to see if a URI is inside the scope.
     */
    public void refreshSeedsIteratorCache() {
        // seeds should be in memory for scope tests
        if (seeds == null) {
            seeds = Collections.synchronizedList(new ArrayList());
        } else {
            seeds.clear();
        }
        synchronized (seeds) {
            seedsCached = false;
            Iterator iter = getSeedsIterator();
            while (iter.hasNext()) {
                seeds.add(iter.next());
            }
            seedsCached = true;
        }
    }

    /**
     * Return an iterator of the seeds in this scope. The seed
     * input is taken from either the configuration file, or the
     * external seed file it specifies.
     *
     * @return An iterator of the seeds in this scope.
     */
    public Iterator getSeedsIterator() {
        Iterator seedIterator;

        if (seedsCached) {
            seedIterator = seeds.iterator();
        } else {
            try {
                File file = getSettingsHandler()
                    .getPathRelativeToWorkingDirectory(
                        (String)getAttribute(ATTR_SEEDS));
                if (!file.exists())
                {
                    throw new FileNotFoundException("Seeds file " +
                       file.getAbsolutePath() + " does not exist.");
                }
                BufferedReader reader = new BufferedReader(new FileReader(file));
                seedIterator = new SeedsInputIterator(reader,
                        getSettingsHandler().getOrder().getController());
            } catch (IOException e) {
                DevUtils.warnHandle(e, "problem reading seeds");
                seedIterator = null;
            } catch (AttributeNotFoundException e) {
                DevUtils.warnHandle(e, "problem reading seeds");
                seedIterator = null;
            } catch (MBeanException e) {
                DevUtils.warnHandle(e, "problem reading seeds");
                e.printStackTrace();
                seedIterator = null;
            } catch (ReflectionException e) {
                DevUtils.warnHandle(e, "problem reading seeds");
                e.printStackTrace();
                seedIterator = null;
            }
        }

        return seedIterator;
    }

    /**
     * Returns whether the given object (typically a CandidateURI) falls
     * within this scope.
     *
     * @param o
     * @return Whether the given object (typically a CandidateURI) falls
     * within this scope.
     */
    protected final boolean innerAccepts(Object o) {
        return ((isSeed(o) || focusAccepts(o)) || additionalFocusAccepts(o) ||
                transitiveAccepts(o)) && !excludeAccepts(o);
    }

    /** Check if there is too many hops
     *
     * @param o URI to check.
     * @return true if too many hops.
     */
    private boolean exeedsMaxHops(Object o) {
        if(! (o instanceof CandidateURI)) {
            return false;
        }

        int maxLinkHops = 0;
        int maxTransHops = 0;
        CrawlerSettings settings = getSettingsFromObject(o);

        try {
            maxLinkHops = ((Integer) getAttribute(
                    settings, CrawlScope.ATTR_MAX_LINK_HOPS)).intValue();
            maxTransHops = ((Integer) getAttribute(
                    settings, CrawlScope.ATTR_MAX_TRANS_HOPS)).intValue();
        } catch (AttributeNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // Don't check if maxLinkHops is set to zero.
        if (maxLinkHops == 0) {
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

        return (linkCount > maxLinkHops) || (transCount>maxTransHops);
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
    private boolean excludeAccepts(Object o) {
        if (excludeFilter.isEmpty(o)) {
            return exeedsMaxHops(o);
        } else {
            return excludeFilter.accepts(o) || exeedsMaxHops(o);
        }
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
    protected boolean isSameHost(UURI a, UURI b)
    {
        boolean isSameHost = false;
        if (a != null && b != null) {
            // getHost can come back null.  See
            // "[ 910120 ] java.net.URI#getHost fails when leading digit"
            if (a.getHost() != null && b.getHost() != null) {
                if (a.getHost().equals(b.getHost())) {
                    isSameHost = true;
                }
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
    
    /**
     * Add a URI to the list of seeds. Includes adding the URI to the seed file.
     * @param newSeed The new seed.
     */
    public void addSeed(UURI newSeed){
        // Add to cached list if exists.
        if(seedsCached){
            synchronized(seeds){
                seeds.add(newSeed);
            }
        }
        // TODO: Write to seedfile.
        try{
            File file = getSettingsHandler().getPathRelativeToWorkingDirectory(
                    (String)getAttribute(ATTR_SEEDS));
            if (!file.exists())
            {
                throw new FileNotFoundException("Seeds file " +
                   file.getAbsolutePath() + " does not exist.");
            }
            // Open file for reading (append)
            FileWriter fw = new FileWriter(file,true);
            // Write to new (last) line the URL.
            fw.write("\n");
            fw.write(newSeed.getURIString());
            fw.flush();
            fw.close();
        } catch (IOException e) {
            DevUtils.warnHandle(e, "problem writing new seed");
        } catch (AttributeNotFoundException e) {
            DevUtils.warnHandle(e, "problem writing new seed");
        } catch (MBeanException e) {
            DevUtils.warnHandle(e, "problem writing new seed");
        } catch (ReflectionException e) {
            DevUtils.warnHandle(e, "problem writing new seed");
        }
    }
    
    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.settings.ModuleType#listUsedFiles(java.util.List)
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
}
