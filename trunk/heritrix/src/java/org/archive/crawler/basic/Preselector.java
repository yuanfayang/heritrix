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
 * SimplePreselector.java
 * Created on Sep 22, 2003
 *
 * $Header$
 */
package org.archive.crawler.basic;

import javax.management.AttributeNotFoundException;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.datamodel.settings.SimpleType;
import org.archive.crawler.framework.CrawlScope;
import org.archive.crawler.framework.Processor;

/**
 * If set to recheck the crawl's scope, gives a yes/no on whether
 * a CrawlURI should be processed at all. If not, its status
 * will be marked OUT_OF_SCOPE and the URI will skip directly
 * to the first "postprocessor".
 *
 *
 * @author gojomo
 *
 */
public class Preselector extends Processor implements FetchStatusCodes {
    private boolean recheckScope;

    private static String ATTR_RECHECK_SCOPE="scope";

//    private static String XP_MAX_LINK_DEPTH="params/@max-link-depth";
//    private static String XP_MAX_EMBED_DEPTH="params/@max-embed-depth";
//    private int maxLinkDepth = -1;
//    private int maxEmbedDepth = -1;

    /**
     * @param name
     */
    public Preselector(String name) {
        super(name, "Preselector");
        addElementToDefinition(new SimpleType(ATTR_RECHECK_SCOPE, "Recheck scope", new Boolean(false)));
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Processor#innerProcess(org.archive.crawler.datamodel.CrawlURI)
     */
    protected void innerProcess(CrawlURI curi) {
        try {
            recheckScope = ((Boolean) getAttribute(ATTR_RECHECK_SCOPE, curi)).booleanValue();
        } catch (AttributeNotFoundException e) {
            recheckScope = false;
        }
        if (recheckScope) {
            CrawlScope scope = getController().getScope();
            if (curi.getScopeVersion() == scope.getVersion()) {
                // already checked
                return;
            }
            if(scope.accepts(curi)) {
                curi.setScopeVersion(scope.getVersion());
                return;
            }
            // scope rejected
            curi.setFetchStatus(S_OUT_OF_SCOPE);
            curi.skipToProcessor(getController().getPostprocessor());
        }


//        super.innerProcess(curi);
//
//        // check for too-deep
//        if(maxLinkDepth>=0 && curi.getLinkHopCount()>maxLinkDepth) {
//            curi.setFetchStatus(S_TOO_MANY_LINK_HOPS);
//            curi.cancelFurtherProcessing();
//            return;
//        }
//        if(maxEmbedDepth>=0 && curi.getEmbedHopCount()>maxEmbedDepth) {
//            curi.setFetchStatus(S_TOO_MANY_EMBED_HOPS);
//            curi.cancelFurtherProcessing();
//            return;
//        }
    }

//    /* (non-Javadoc)
//     * @see org.archive.crawler.framework.Processor#innerRejectProcess(org.archive.crawler.datamodel.CrawlURI)
//     */
//    protected void innerRejectProcess(CrawlURI curi) {
//        super.innerRejectProcess(curi);
//        // filter-rejection means out-of-scope for everything but embeds
//        if (curi.getEmbedHopCount() < 1) {
//            curi.setFetchStatus(S_OUT_OF_SCOPE);
//            curi.cancelFurtherProcessing();
//        } else {
//            // never mind; scope filters don't apply
//        }
//    }

}
