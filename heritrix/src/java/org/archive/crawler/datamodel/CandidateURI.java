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
 * CandidateURI.java
 * Created on Sep 30, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

import java.io.IOException;
import java.io.Serializable;

import org.apache.commons.httpclient.URIException;
import org.archive.util.Lineable;

import st.ata.util.AList;
import st.ata.util.HashtableAList;

/**
 * A URI, discovered or passed-in, that may be scheduled (and
 * thus become a CrawlURI). Contains just the fields necessary
 * to perform quick in-scope analysis.
 *
 * @author Gordon Mohr
 */
public class CandidateURI
implements Serializable, Lineable {
    private static final long serialVersionUID = -7152937921526560388L;

    /** Highest scheduling priority.
     * Before any others of its class.
     */
    public static final int HIGHEST = 0;
    
    /** High scheduling priority.
     * After any {@link #HIGHEST}.
     */
    public static final int HIGH = 1;
    
    /** Medium priority.
     * After any {@link #HIGH}.
     */
    public static final int MEDIUM = 2;
    
    /** Normal/low priority.
     * Whenever/end of queue.
     */
    public static final int NORMAL = 3;
    
    private int schedulingDirective = NORMAL;
    
    /** Usuable URI under consideration */
    private UURI uuri;
    
    /** Seed status */
    private boolean isSeed = false;

    private boolean forceRevisit = false; // even if already visited
    
    /** String of letters indicating how this URI was reached from a seed.
     * <pre>
     * P precondition
     * R redirection
     * E embedded (as frame, src, link, codebase, etc.)
     * X speculative embed (as from javascript, some alternate-format extractors
     * L link</pre>
     * For example LLLE (an embedded image on a page 3 links from seed).
     */
    private String pathFromSeed;
    
    /**
     * Where this URI was (presently) discovered.
     * Can be String, CrawlURI or null.  Is turned into a String when
     * we serialize.
     */
    // mostly for debugging; will be a CrawlURI when memory is no object
    // just a string or null when memory is an object (configurable)
    private Object via;

    /**
     * Flexible dynamic attributes list.
     * <p>
     * The attribute list is a flexible map of key/value pairs for storing
     * status of this URI for use by other processors. By convention the
     * attribute list is keyed by constants found in the
     * {@link CoreAttributeConstants} interface.
     */
    private AList alist;
    
    /**
     * Cache of this candidate uuri as a string.
     *
     * Profiling shows us spending about 1-2% of total elapsed time in
     * toString.
     */
    private String cachedCandidateURIString = null;

    /**
     * Constructor.
     * Protected access to block access to default constructor.
     */
    protected CandidateURI () {
        super();
    }
    
    /**
     * @param u uuri instance this CandidateURI wraps.
     */
    public CandidateURI(UURI u) {
        this.uuri = u;
    }
    
    /**
     * @param u uuri instance this CandidateURI wraps.
     * @param pathFromSeed
     * @param via
     */
    public CandidateURI(UURI u, String pathFromSeed, Object via) {
        this.uuri = u;
        this.pathFromSeed = pathFromSeed;
        this.via = via;
    }

    /**
     * Set the <tt>isSeed</tt> attribute of this URI.
     * @param b Is this URI a seed, true or false.
     */
    public void setIsSeed(boolean b) {
        this.isSeed = b;
        if (this.isSeed) {
            setSeed();
        }
    }

    /**
     * A quick way to mark this URI as being a seed.
     *
     * <p>Equal to calling:
     * <code>
     *   setIsSeed(true);
     *   setPathFromSeed("");
     *   setVia("")
     * </code>
     */
    protected void setSeed(){
        isSeed = true;
        setPathFromSeed("");
        setVia("");
    }

    /**
     * @return UURI
     */
    public UURI getUURI() {
        return this.uuri;
    }

    /**
     * @return Whether seeded.
     */
    public boolean isSeed() {
        return this.isSeed;
    }

    /**
     * @return path (hop-types) from seed
     */
    public String getPathFromSeed() {
        return this.pathFromSeed;
    }

    /**
     * @return URI via which this one was discovered
     */
    public Object getVia() {
        return this.via;
    }

    /**
     * @param string
     */
    protected void setPathFromSeed(String string) {
        pathFromSeed = string;
    }
    
    /**
     * Called when making a copy of another CandidateURI.
     * @param alist AList to use.
     */
    protected void setAList(AList alist) {
        this.alist = alist;
    }

    /**
     * @param object
     */
    public void setVia(Object object) {
        via = object;
    }

    /**
     * @return This candidate URI as a string wrapped with 'CandidateURI(' +
     * ')'.
     */
    public String toString() {
        if (this.cachedCandidateURIString == null) {
            synchronized (this) {
                if (this.cachedCandidateURIString == null) {
                    this.cachedCandidateURIString =
                        "CandidateURI(" + getURIString() + ")";
                }
            }
        }
        return this.cachedCandidateURIString;
    }

    private void writeObject(java.io.ObjectOutputStream out)
    throws IOException {
         this.via = flattenVia();
         out.defaultWriteObject();
    }

    /**
     * Method returns string version of this URI's referral URI.
     * @return String version of referral URI
     */
    public String flattenVia() {
        if(via == null) {
            return "";
        }
        if (via instanceof String) {
            // already OK
            return (String) via;
        }
        if (via instanceof UURI) {
            return ((UURI)via).toString();
        }
        if (via instanceof CandidateURI) {
            return ((CandidateURI)via).getUURI().toString();
        }
        return via.toString();
    }

    public String getLine() {
        String className = this.getClass().getName();
        className = className.substring(className.lastIndexOf(".")+1);
        return className + " " + getUURI().toString() + " " + pathFromSeed +
            " " + flattenVia();
    }

    /**
     * @return URI String
     */
    public String getURIString() {
        return getUURI().toString();
    }

    /**
     * Compares the domain of this CandidateURI with that of another
     * CandidateURI
     *
     * @param other The other CandidateURI
     *
     * @return True if both are in the same domain, false otherwise.
     * @throws URIException
     */
    public boolean sameDomainAs(CandidateURI other) throws URIException {
        String domain = getUURI().getHost();
        if (domain == null) {
            return false;
        }
        while(domain.lastIndexOf('.') > domain.indexOf('.')) {
            // While has more than one dot, lop off first segment
            domain = domain.substring(domain.indexOf('.') + 1);
        }
        if(other.getUURI().getHost() == null) {
            return false;
        }
        return other.getUURI().getHost().endsWith(domain);
    }

    /**
     * If this method returns true, this URI should be fetched even though
     * it already has been crawled. This also implies
     * that this URI will be scheduled for crawl before any other waiting
     * URIs for the same host.
     *
     * This value is used to refetch any expired robots.txt or dns-lookups.
     *
     * @return true if crawling of this URI should be forced
     */
    public boolean forceFetch() {
        return forceRevisit;
    }

   /**
     * Method to signal that this URI should be fetched even though
     * it already has been crawled. Setting this to true also implies
     * that this URI will be scheduled for crawl before any other waiting
     * URIs for the same host.
     *
     * This value is used to refetch any expired robots.txt or dns-lookups.
     *
     * @param b set to true to enforce the crawling of this URI
     */
    public void setForceFetch(boolean b) {
        forceRevisit = b;
    }

    /**
     * @return Returns the schedulingDirective.
     */
    public int getSchedulingDirective() {
        return schedulingDirective;
    }
    /** 
     * @param schedulingDirective The schedulingDirective to set.
     */
    public void setSchedulingDirective(int schedulingDirective) {
        this.schedulingDirective = schedulingDirective;
    }


    /**
     * @return True if needs immediate scheduling.
     */
    public boolean needsImmediateScheduling() {
        return schedulingDirective == HIGH;
    }

    /**
     * @return True if needs soon but not top scheduling.
     */
    public boolean needsSoonScheduling() {
        return schedulingDirective == MEDIUM;
    }

    /**
     * Tally up the number of transitive (non-simple-link) hops at
     * the end of this CandidateURI's pathFromSeed.
     * 
     * In some cases, URIs with greater than zero but less than some
     * threshold such hops are treated specially. 
     * 
     * <p>TODO: consider moving link-count in here as well, caching
     * calculation, and refactoring CrawlScope.exceedsMaxHops() to use this. 
     * 
     * @return Transhop count.
     */
    public int getTransHops() {
        String path = getPathFromSeed();
        int transCount = 0;
        for(int i=path.length()-1;i>=0;i--) {
            if(path.charAt(i)=='L') {
                break;
            }
            transCount++;
        }
        return transCount;
    }

    /**
     * Given a string containing a URI, then optional whitespace
     * delimited hops-path and via info, create a CandidateURI 
     * instance.
     * 
     * @param uriHopsViaString String with a URI.
     * @return A CandidateURI made from passed <code>uriHopsViaString</code>.
     * @throws URIException
     */
    public static CandidateURI fromString(String uriHopsViaString)
            throws URIException {
        String args[] = uriHopsViaString.split("\\s+");
        String pathFromSeeds = (args.length > 1 && !args[1].equals("-"))?
            args[1]: "";
        Object via = (args.length > 2 && !args[2].equals("-"))? args[2]: "";
        return new CandidateURI(UURIFactory.getInstance(args[0]),
            pathFromSeeds, via);
    }
    
    public static CandidateURI createSeedCandidateURI(UURI uuri) {
        CandidateURI c = new CandidateURI(uuri);
        c.setIsSeed(true);
        return c;
    }
    
    /**
     * Assumption is that only one at a thread will ever be accessing
     * a CandidateURI.
     * 
     * @deprecated Public access will be deprecated.  This methods access
     * will change in next release.
     * @return the attribute list.
     */
    public AList getAList() {
        if (this.alist == null) {
            this.alist = new HashtableAList();
        }
        return this.alist;
    }
    
    protected void clearAList() {
        this.alist = null;
    }
    
    public void putObject(String key, Object value) {
        getAList().putObject(key, value);
    }
    
    public Object getObject(String key) {
        return getAList().getObject(key);
    }
    
    public String getString(String key) {
        return getAList().getString(key);
    }
    
    public void putString(String key, String value) {
        getAList().putString(key, value);
    }
    
    public long getLong(String key) {
        return getAList().getLong(key);
    }
    
    public void putLong(String key, long value) {
        getAList().putLong(key, value);
    }
    
    public int getInt(String key) {
        return getAList().getInt(key);
    }
    
    public void putInt(String key, int value) {
        getAList().putInt(key, value);
    }
    
    public boolean containsKey(String key) {
        return getAList().containsKey(key);
    }
    
    public void remove(String key) {
        getAList().remove(key);
    }
}
