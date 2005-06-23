/* BdbFrontier
 * 
 * $Header$
* 
 * Created on Sep 24, 2004
 *
 *  Copyright (C) 2004 Internet Archive.
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
  */
package org.archive.crawler.frontier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.UriUniqFilter;
import org.archive.crawler.framework.FrontierMarker;
import org.archive.crawler.framework.exceptions.InvalidFrontierMarkerException;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.Type;
import org.archive.crawler.util.BdbUriUniqFilter;
import org.archive.crawler.util.BloomUriUniqFilter;
import org.archive.util.ArchiveUtils;

import com.sleepycat.je.DatabaseException;

/**
 * A Frontier using several BerkeleyDB JE Databases to hold its record of
 * known hosts (queues), and pending URIs. 
 *
 * @author Gordon Mohr
 */
public class BdbFrontier extends WorkQueueFrontier {
    // be robust against trivial implementation changes
    private static final long serialVersionUID = ArchiveUtils
        .classnameBasedUID(BdbFrontier.class, 1);

    private static final Logger logger =
        Logger.getLogger(BdbFrontier.class.getName());

    /** all URIs scheduled to be crawled */
    protected BdbMultipleWorkQueues pendingUris;

    /** all URI-already-included options available to be chosen */
    String[] AVAILABLE_INCLUDED_OPTIONS = new String[] {
            BdbUriUniqFilter.class.getName(),
            BloomUriUniqFilter.class.getName() };
    
    /** URI-already-included to use (by class name) */
    public final static String ATTR_INCLUDED = "uri-included-structure";
    
    protected final static String DEFAULT_INCLUDED = BdbUriUniqFilter.class
    .getName();
    
    public BdbFrontier(String name) {
        this(name, "BdbFrontier. "
            + "A Frontier using BerkeleyDB Java Edition databases for "
            + "persistence to disk.");
        Type t = addElementToDefinition(new SimpleType(ATTR_INCLUDED,
                "Structure to use for tracking already-seen URIs. Non-default " +
                "options may require additional configuration via system " +
                "properties.", DEFAULT_INCLUDED, AVAILABLE_INCLUDED_OPTIONS));
        t.setExpertSetting(true);
    }

    /**
     * Create the BdbFrontier
     * 
     * @param name
     * @param description
     */
    public BdbFrontier(String name, String description) {
        super(name, description);
    }

    /**
     * Create the single object (within which is one BDB database)
     * inside which all the other queues live. 
     * 
     * @return the created BdbMultipleWorkQueues
     * @throws DatabaseException
     */
    private BdbMultipleWorkQueues createMultipleWorkQueues()
    throws DatabaseException {
        return new BdbMultipleWorkQueues(this.controller.getBdbEnvironment(),
            this.controller.getClassCatalog());
    }

    /**
     * Create a UriUniqFilter that will serve as record 
     * of already seen URIs.
     *
     * @return A UURISet that will serve as a record of already seen URIs
     * @throws IOException
     */
    protected UriUniqFilter createAlreadyIncluded() throws IOException {
        UriUniqFilter uuf;
        String c = null;
        try {
            c = (String)getAttribute(null, ATTR_INCLUDED);
        } catch (AttributeNotFoundException e) {
            // Do default action if attribute not in order.
        }
        if (c != null && c.equals(BloomUriUniqFilter.class.getName())) {
            uuf = new BloomUriUniqFilter();
        } else {
            uuf = new BdbUriUniqFilter(this.controller.getBdbEnvironment());
        }
        uuf.setDestination(this);
        return uuf;
    }

    /**
     * Return the work queue for the given CrawlURI's classKey. URIs
     * are ordered and politeness-delayed within their 'class'.
     * 
     * @param curi CrawlURI to base queue on
     * @return the found or created BdbWorkQueue
     */
    protected WorkQueue getQueueFor(CrawlURI curi) {
        WorkQueue wq;
        String classKey = curi.getClassKey();
        synchronized (allQueues) {
            wq = (WorkQueue)allQueues.get(classKey);
            if (wq == null) {
                wq = new BdbWorkQueue(classKey);
                wq.setTotalBudget(((Long)getUncheckedAttribute(
                    curi,ATTR_QUEUE_TOTAL_BUDGET)).longValue());
                allQueues.put(classKey, wq);
            }
        }
        return wq;
    }

    /** (non-Javadoc)
     * @see org.archive.crawler.framework.Frontier#getInitialMarker(java.lang.String, boolean)
     */
    public FrontierMarker getInitialMarker(String regexpr, boolean inCacheOnly) {
        return pendingUris.getInitialMarker(regexpr);
    }

    /** (non-Javadoc)
     *
     * @param marker
     * @param numberOfMatches
     * @param verbose
     * @return List of URIs (strings).
     * @throws InvalidFrontierMarkerException
     */
    public ArrayList getURIsList(FrontierMarker marker, int numberOfMatches,
        boolean verbose) throws InvalidFrontierMarkerException {
        List curis;
        try {
            curis = pendingUris.getFrom(marker, numberOfMatches);
        } catch (DatabaseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        ArrayList results = new ArrayList(curis.size());
        Iterator iter = curis.iterator();
        while(iter.hasNext()) {
            CrawlURI curi = (CrawlURI) iter.next();
            results.add("["+curi.getClassKey()+"] "+curi.singleLineReport());
        }
        return results;
    }
    
    protected void initQueue() throws IOException {
        try {
            pendingUris = createMultipleWorkQueues();
        } catch(DatabaseException e) {
            throw (IOException)new IOException(e.getMessage()).initCause(e);
        }
    }
    
    protected void closeQueue() throws IOException {
        if(this.pendingUris != null) {
            this.pendingUris.close();
            this.pendingUris = null;
        }
    }
        
    protected BdbMultipleWorkQueues getWorkQueues() {
        return pendingUris;
    }

    protected boolean workQueueDataOnDisk() {
        return true;
    }
}

