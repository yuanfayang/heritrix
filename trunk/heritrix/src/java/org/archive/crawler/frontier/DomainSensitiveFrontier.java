/* DomainSensitiveFrontier
*
* $Id$
*
 * Created on 2004-aug-24
*
* Copyright (C) 2004 National and University Library of Iceland.
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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.Attribute;
import org.apache.commons.collections.Predicate;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.filter.OrFilter;
import org.archive.crawler.filter.URIRegExpFilter;
import org.archive.crawler.framework.CrawlScope;
import org.archive.crawler.settings.CrawlerSettings;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.Type;

/**
 * Behaves like {@link Frontier} (i.e., a basic mostly breadth-first frontier),
 * but with the addition that you can override the number of documents to
 * download for a specific host or domain.  Useful for case of frequent
 * revisits of a site of frequent changes.
 * 
 * To do this you choose the number of docs you want to download and specify
 * this in the max-docs field under frontier in the relevant override. You also
 * have to check the counter attribute there as well (but no need to change the
 * default value of 0 for the counter).
 * 
 * @author Oskar Grenholm <oskar at kb dot se>
 */
public class DomainSensitiveFrontier extends Frontier {

    public static final String ATTR_COUNTER = "counter";

    public static final String ATTR_MAX_DOCS = "max-docs";

    private static final Logger logger = Logger
        .getLogger(DomainSensitiveFrontier.class.getName());

    public DomainSensitiveFrontier(String name) {
        super(ATTR_NAME, "" + "DomainSensitiveFrontier\n" +
        		"Overrides default Frontier to add" +
        	    " specification of number of documents to download" +
			" per host or domain (Useful for case of revisiting" +
			" a site that changes frequently).");

        Type e = addElementToDefinition(new SimpleType(ATTR_COUNTER,
            "A simple counter for documents", new Long(0)));
        e.setOverrideable(true);

        e = addElementToDefinition(new SimpleType(ATTR_MAX_DOCS,
            "Maximum number of documents to download for host or domain.",
            new Long(0)));
        e.setOverrideable(true);
    }

    /**
     * Override that checks if we've reached download limits.
     * The only thing changed here is an added call to checkFinished
     * at the end of the method. Other than that it is identical to the one in
     * {@link Frontier}.
     */
    public synchronized void finished(CrawlURI curi) {
        super.finished(curi);
        checkDownloadLimits(curi);
    }

    /**
     * Check if the max document download limit for this host or domain has
     * been reached.
     * 
     * If so, delete the rest of the URIs for this host or domain
     * waiting in the queue. Then add an URIRegExpFilter for this host or
     * domain, so we won't get any more URIs from this one later on.
     */
    private boolean checkDownloadLimits(CrawlURI curi) {
        long thisMaxDocs = 0;
        long thisCounter = 0;
        boolean discard = false;
        boolean retVal = false;
        try {
            String host = curi.getUURI().getHost();
            CrawlerSettings cs = controller.getSettingsHandler().
				getSettings(host);
            do {
                thisMaxDocs = ((Long) getAttribute(cs, ATTR_MAX_DOCS))
                    .longValue();
                thisCounter = ((Long) getAttribute(cs, ATTR_COUNTER))
                    .longValue();
                // Have we hit the max document download limit for this host or
                // domain?
                if ((thisMaxDocs > 0 && thisCounter >= thisMaxDocs)) {
                    logger.fine("** Discarding Queue: " + host + " **");
                    if (!discard) {
                        URIWorkQueue kq = keyedQueueFor(curi);
                        Predicate mat = new URIQueueMatcher(".*", true, this);
                        kq.deleteMatchedItems(mat);
                        kq.checkEmpty();
                        if (kq.isDiscardable()) {
                            readyClassQueues.remove(kq);
                            discardQueue(kq);
                            discard = true;
                        }
                    }
                    // Adding an exclude filter for this host or domain
                    OrFilter or = (OrFilter) controller.getScope()
                        .getAttribute(CrawlScope.ATTR_EXCLUDE_FILTER);
                    String scope = (scope = cs.getScope()) != null ?
                        scope : "";
                    String filter = "^((https?://)?[a-zA-Z0-9\\.]*)" + scope
                        + "($|/.*)";
                    logger.fine("** Adding filter: [" + filter + "] **");
                    URIRegExpFilter urf = new URIRegExpFilter(curi
                        .getURIString(), filter);
                    or.addFilter(controller.getSettingsHandler().
                    		getSettings(null), urf);
                    thisMaxDocs = 0;
                    thisCounter = 0;
                    retVal = true;
                }
            } while ((cs = cs.getParent()) != null);
        } catch (Exception e) {
            logger.severe("ERROR: checkIfSomethingFinished(), "
                + "while processing {" + curi.getURIString() + "}" +
				": " + e.getMessage());
        }
        return retVal;
    }

    /**
     * Ovverridden version of successDisposition that, in addition to
     * incrementing a global successCount, increments the counter attribute for
     * all crawler settings that are valid for the given CrawlURI.
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
            successCount++;
            // dns:host gets same classkey as host, but we don't want to count
            // them
            if (!curi.getUURI().toString().startsWith("dns:")) {
                try {
                    String host = curi.getUURI().getHost();
                    CrawlerSettings cs = controller.getSettingsHandler()
                        .getSettings(host);
                    do {
                        long counter = ((Long) getAttribute(cs, ATTR_COUNTER))
                            .longValue();
                        setAttribute(cs, new Attribute(ATTR_COUNTER,
                        		new Long(++counter)));
                    } while ((cs = cs.getParent()) != null);
                } catch (Exception e) {
                    logger.severe("ERROR: successDisposition() " +
                    		e.getMessage());
                }
            }
        }
        // Let everyone know in case they want to do something before we strip
        // the curi.
        controller.fireCrawledURISuccessfulEvent(curi);
        curi.stripToMinimal();
        controller.recover.finishedSuccess(curi);
    }
}