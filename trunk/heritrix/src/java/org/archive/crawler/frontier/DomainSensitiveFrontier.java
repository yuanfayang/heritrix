/* DomainSensitiveFrontier
 *
 * $Id$
 *
 * Created on 2004-dec-14
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

import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.collections.Predicate;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.filter.OrFilter;
import org.archive.crawler.filter.URIRegExpFilter;
import org.archive.crawler.scope.ClassicScope;
import org.archive.crawler.settings.CrawlerSettings;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.Type;

/**
 * Behaves like {@link HostQueuesFrontier}(i.e., a basic mostly breadth-first
 * frontier), but with the addition that you can set the number of documents to
 * download from a site. 
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
 * @deprecated Use {@link YetAnotherDomainSensitiveFrontier} instead (YADSF
 * will be renamed as DSF when this frontier is removed. Awaiting feedback
 * from Rob Eger that new YADSF does as this frontier does).
 */
public class DomainSensitiveFrontier extends HostQueuesFrontier {

    public static final String ATTR_MAX_DOCS = "max-docs";

    private static final Logger logger =
        Logger.getLogger(DomainSensitiveFrontier.class.getName());

    private Hashtable hostCounters = new Hashtable();

    public DomainSensitiveFrontier(String name) {
        super(ATTR_NAME, "DomainSensitiveFrontier.\n"
                + "Overrides HostQueuesFrontier to add"
                + " specification of number of documents to download"
                + " per host.");

        Type e = addElementToDefinition(new SimpleType(ATTR_MAX_DOCS,
                "Maximum number of documents to download for host or domain" +
                " (Zero means no limit).", new Long(0)));
        e.setOverrideable(true);
    }

    /**
     * Override that checks if we've reached download limits. The only thing
     * changed here is an added call to checkFinished at the end of the method.
     * Other than that it is identical to the one in {@link HostQueuesFrontier}.
     */
    public synchronized void finished(CrawlURI curi) {
        super.finished(curi);
        checkDownloadLimits(curi);
    }

    /**
     * Check if the max document download limit for this host or domain has been
     * reached.
     * 
     * If so, delete the rest of the URIs for this host or domain waiting in the
     * queue. Then add an URIRegExpFilter for this host or domain, so we won't
     * get any more URIs from this one later on.
     */
    private boolean checkDownloadLimits(CrawlURI curi) {
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
                controller.getSettingsHandler().getSettings(host);
            thisMaxDocs = ((Long) getAttribute(cs, ATTR_MAX_DOCS)).longValue();
            thisCounter = hostCounters.get(host) != null?
                ((Long) hostCounters.get(host)).longValue(): 0;
            // Have we hit the max document download limit for this host?
            if ((thisMaxDocs > 0 && thisCounter >= thisMaxDocs)) {
                logger.fine("** Discarding Queue: " + host + " **");
                if (!discarded) {
                    URIWorkQueue kq = keyedQueueFor(curi);
                    Predicate mat = new URIQueueMatcher(".*", true, this);
                    kq.deleteMatchedItems(mat);
                    kq.checkEmpty();
                    discarded = true;
                    if (kq.isDiscardable()) {
                        readyClassQueues.remove(kq);
                        discardQueue(kq);
                    }
                }
                // Adding an exclude filter for this host
                OrFilter or = (OrFilter) controller.getScope().getAttribute(
                        ClassicScope.ATTR_EXCLUDE_FILTER);
                String filter = "^((https?://)?[a-zA-Z0-9\\.]*)" + host
                        + "($|/.*)";
                logger.fine("** Adding filter: [" + filter + "] **");
                URIRegExpFilter urf = new URIRegExpFilter(curi.toString(),
                        filter);
                or.addFilter(controller.getSettingsHandler().getSettings(null),
                        urf);
                thisMaxDocs = 0;
                thisCounter = 0;
                retVal = true;
            }
        } catch (Exception e) {
            logger.severe("ERROR: checkIfSomethingFinished(), "
                    + "while processing {" + curi.toString() + "}"
                    + e.getClass()
                    + "message: " + e.getMessage() + ".  Stack trace:");
            e.printStackTrace();
        }
        return retVal;
    }

    /**
     * Overridden version of successDisposition that, in addition to
     * incrementing a global successCount, increments the counter for a
     * Hashtable containing key:host - value:counter.
     */
    protected void successDisposition(CrawlURI curi) {
        totalProcessedBytes += curi.getContentSize();

        curi.aboutToLog();
        Object array[] = { curi };
        controller.uriProcessing.log(Level.INFO, curi.getUURI().toString(),
                array);

        // note that CURI has passed out of scheduling
        if (shouldBeForgotten(curi)) {
            // curi is dismissed without prejudice: it can be reconstituted
            forget(curi);
        } else {
            succeededFetchCount++;
            // dns:host gets same classkey as host, but we don't want to count
            // them
            if (!curi.getUURI().toString().startsWith("dns:")) {
                try {
                    String host = curi.getUURI().getHost();
                    long counter = hostCounters.get(host) != null ?
                        ((Long) hostCounters.get(host)).longValue(): 0;
                    hostCounters.put(host, new Long(++counter));
                } catch (Exception e) {
                    logger.severe("ERROR: successDisposition() "
                            + e.getMessage());
                }
            }
        }
        // Let everyone know in case they want to do something before we strip
        // the curi.
        controller.fireCrawledURISuccessfulEvent(curi);
        curi.stripToMinimal();
        FrontierJournal j = getFrontierJournal();
        if (j != null) {
            j.finishedSuccess(curi);
        }
    }
}
