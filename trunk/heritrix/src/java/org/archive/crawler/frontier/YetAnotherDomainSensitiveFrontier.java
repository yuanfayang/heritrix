/* YetAnotherDomainSensitiveFrontier
*
* $Id$
*
* Created on 2004-may-06
*
* Copyright (C) 2004 Royal Library of Sweden.
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
*/
package org.archive.crawler.frontier;

import java.io.IOException;
import java.util.Hashtable;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.event.CrawlURIDispositionListener;
import org.archive.crawler.filter.OrFilter;
import org.archive.crawler.filter.URIRegExpFilter;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.exceptions.FatalConfigurationException;
import org.archive.crawler.scope.ClassicScope;
import org.archive.crawler.settings.CrawlerSettings;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.Type;

/** 
 * Behaves like {@link BdbFrontier} (i.e., a basic mostly breadth-first
 * frontier), but with the addition that you can set the number of documents to
 * download on a per site basis. 
 *
 * <p>Useful for case of frequent revisits of a site of frequent changes.
 * 
 * <p>To do this you choose the number of docs you want to download and specify
 * this in the max-docs field under frontier. You set one global value and then
 * no site will download more than that number of docs. If you want, you can
 * create an override and then sites affected by this override will download
 * that many docs instead, whether it is higher or lower.
 * 
 * @author Oskar Grenholm <oskar dot grenholm at kb dot se>
 */
public class YetAnotherDomainSensitiveFrontier extends BdbFrontier
implements CrawlURIDispositionListener {
    private static final Logger logger =
        Logger.getLogger(YetAnotherDomainSensitiveFrontier.class.getName());
    
    public static final String ATTR_MAX_DOCS = "max-docs";
    
    // TODO: Make this a BigMap.
    private Hashtable hostCounters = new Hashtable();

    public YetAnotherDomainSensitiveFrontier(String name) {
        super(ATTR_NAME, "YetAnotherDomainSensitiveFrontier. " +
            "Overrides BdbFrontier to add specification of number of " +
            "documents to download per host (Expects 'exclude-filter' " +
            "to be part of CrawlScope).");
        Type e = addElementToDefinition(new SimpleType(ATTR_MAX_DOCS,
            "Maximum number of documents to download for host or domain" +
            " (Zero means no limit).", new Long(0)));
        e.setOverrideable(true);
    }

    public void initialize(CrawlController c)
    throws FatalConfigurationException, IOException {
        super.initialize(c);
        this.controller.addCrawlURIDispositionListener(this);
    }
    
    /**
     * Check if the max document download limit for this host or domain has been
     * reached.
     * 
     * If so, delete the rest of the URIs for this host or domain waiting in the
     * queue. Then add an URIRegExpFilter for this host or domain, so we won't
     * get any more URIs from this one later on.
     * @param curi CrawlURI.
     * @return True if discarded queue.
     */
    private synchronized boolean checkDownloadLimits(CrawlURI curi) {
        long thisMaxDocs = 0;
        long thisCounter = 0;
        boolean discarded = false;
        boolean retVal = false;
        if (curi.getUURI().getScheme().equals("dns")) {
            return false;
        }
        try {
            String host = curi.getUURI().getHost();
            CrawlerSettings cs =
                this.controller.getSettingsHandler().getSettings(host);
            thisMaxDocs = ((Long) getAttribute(cs, ATTR_MAX_DOCS)).longValue();
            thisCounter = this.hostCounters.get(host) != null?
                ((Long)this.hostCounters.get(host)).longValue(): 0;
            // Have we hit the max document download limit for this host?
            if ((thisMaxDocs > 0 && thisCounter >= thisMaxDocs)) {
                logger.fine("Discarding Queue: " + host + " ");
                if (!discarded) {
                    long count = 0;
                    WorkQueue wq = getQueueFor(curi);
                    wq.unpeek();
                    count += wq.deleteMatching(this, ".*");
                    decrementQueuedCount(count);
                    discarded = true;
                    // I tried adding annotation but we're past log time for
                    // Curi so it doesn't work.
                    // curi.addAnnotation("maxDocsForHost");
                }
                // Adding an exclude filter for this host
                OrFilter or = (OrFilter)this.controller.getScope().
                    getAttribute(ClassicScope.ATTR_EXCLUDE_FILTER);
                String filter = "^((https?://)?[a-zA-Z0-9\\.]*)" + host
                    + "($|/.*)";
                logger.fine("Adding filter: [" + filter + "].");
                URIRegExpFilter urf = new URIRegExpFilter(curi.toString(),
                    filter);
                or.addFilter(this.controller.getSettingsHandler().
                    getSettings(null),urf);
                thisMaxDocs = 0;
                thisCounter = 0;
                retVal = true;
            }
        } catch (Exception e) {
            logger.severe("ERROR: checkDownloadLimits(), "
                    + "while processing {" + curi.toString() + "}"
                    + e.getClass()
                    + "message: " + e.getMessage() + ".  Stack trace:");
            e.printStackTrace();
        }
        return retVal;
    }
    
    protected synchronized void incrementHostCounters(CrawlURI curi) {
        if (!curi.getUURI().toString().startsWith("dns:")) {
            try {
                String host = curi.getUURI().getHost();
                long counter = this.hostCounters.get(host) != null ?
                    ((Long)this.hostCounters.get(host)).longValue(): 0;
                this.hostCounters.put(host, new Long(++counter));
            } catch (Exception e) {
                logger.severe("ERROR: incrementHostCounters() " +
                    e.getMessage());
            }
        }
    }

    public void crawledURISuccessful(CrawlURI curi) {
        incrementHostCounters(curi);
        checkDownloadLimits(curi);
    }

    public void crawledURINeedRetry(CrawlURI curi) {
    }

    public void crawledURIDisregard(CrawlURI curi) {
    }

    public void crawledURIFailure(CrawlURI curi) {
    }
}
