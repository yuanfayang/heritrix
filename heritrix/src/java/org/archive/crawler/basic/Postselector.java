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
 * SimplePostselector.java
 * Created on Oct 2, 2003
 *
 * $Header$
 */
package org.archive.crawler.basic;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.framework.Processor;

/**
 * Determine which extracted links etc get fed back into Frontier.
 *
 * Could in the future also control whether current URI is retried.
 *
 * @author gojomo
 *
 */
public class Postselector extends Processor implements CoreAttributeConstants, FetchStatusCodes {
    private static Logger logger = Logger.getLogger("org.archive.crawler.basic.Postselector");

    // limits on retries TODO: separate into retryPolicy?
    private int maxDeferrals = 10; // should be at least max-retries plus 3 or so

    /**
     * @param name
     */
    public Postselector(String name) {
        super(name, "Post selector");
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Processor#innerProcess(org.archive.crawler.datamodel.CrawlURI)
     */
    protected void innerProcess(CrawlURI curi) {
        logger.finest(getName()+" processing "+curi);

        // handle any prerequisites
        if (curi.getAList().containsKey(A_PREREQUISITE_URI)) {
            handlePrerequisites(curi);
            return;
        }

        URI baseUri = getBaseURI(curi);
        // handle http headers
        if (curi.getAList().containsKey(A_HTTP_HEADER_URIS)) {
            handleLinkCollection(curi, baseUri, A_HTTP_HEADER_URIS, 'R',
                CandidateURI.HIGH_PRIORITY);
        }
        // handle embeds
        if (curi.getAList().containsKey(A_HTML_EMBEDS)) {
            handleLinkCollection(curi, baseUri, A_HTML_EMBEDS, 'E',
                CandidateURI.NORMAL_PRIORITY);
        }
        // handle speculative embeds
        if (curi.getAList().containsKey(A_HTML_SPECULATIVE_EMBEDS)) {
            handleLinkCollection(curi, baseUri,A_HTML_SPECULATIVE_EMBEDS, 'X',
                CandidateURI.NORMAL_PRIORITY);
        }
        // handle links
        if (curi.getAList().containsKey(A_HTML_LINKS)) {
            handleLinkCollection(
                curi, baseUri, A_HTML_LINKS, 'L', CandidateURI.NORMAL_PRIORITY);
        }
        // handle css links
        if (curi.getAList().containsKey(A_CSS_LINKS)) {
            handleLinkCollection(
                curi, baseUri, A_CSS_LINKS, 'E', CandidateURI.NORMAL_PRIORITY);
        }
        // handle js file links
        if (curi.getAList().containsKey(A_JS_FILE_LINKS)) {
            URI viaURI = baseUri;
            if (curi.flattenVia() != null) {
                try {
                    viaURI = URI.create(curi.flattenVia());
                } catch (Exception e) {
                    Object[] array = { curi, curi.flattenVia() };
                    getController().uriErrors.log(
                        Level.INFO, e.getMessage(), array);
                }
            }
            handleLinkCollection( curi, viaURI, 
                A_JS_FILE_LINKS, 'X', CandidateURI.NORMAL_PRIORITY);
        }
        
    }

    /**
     * @param curi
     */
    private URI getBaseURI(CrawlURI curi) {
        if (!curi.getAList().containsKey(A_HTML_BASE)) {
            return curi.getUURI().getRawUri();
        }
        String base = curi.getAList().getString(A_HTML_BASE);
        try {
            return UURI.createUURI(base).getRawUri();
        } catch (URISyntaxException e) {
            Object[] array = { curi, base };
            getController().uriErrors.log(Level.INFO,e.getMessage(), array);
            // next best thing: use self
            return curi.getUURI().getRawUri();
        }
    }

    protected void handlePrerequisites(CrawlURI curi) {
        try {
            if ( curi.getDeferrals() > maxDeferrals ) {
                // too many deferrals, equals failure
                curi.setFetchStatus(S_PREREQUISITE_FAILURE);
                //failureDisposition(curi);
                return;
            }
            // convert to UURI for convenience of Frontier
            UURI prereq = UURI.createUURI(
                (String)curi.getPrerequisiteUri(), getBaseURI(curi));
                
            curi.setPrerequisiteUri(prereq); 
            CandidateURI caUri = 
                new CandidateURI(prereq, CandidateURI.HIGH_PRIORITY);
                
            caUri.setVia(curi);
            caUri.setPathFromSeed(curi.getPathFromSeed()+ "P");

            if (!scheduleURI(caUri)) {
                // prerequisite cannot be scheduled (perhaps excluded by scope)
                // must give up on
                curi.setFetchStatus(S_PREREQUISITE_FAILURE);
                //failureDisposition(curi);
                return;
            }
            // leave PREREQ in place so frontier can properly defer this curi
        } catch (URISyntaxException ex) {
            Object[] array = { curi, curi.getPrerequisiteUri() };
            getController().uriErrors.log(Level.INFO,ex.getMessage(), array);
        }
    }


    /**
     * Schedule the given {@link CandidateURI CandidateURI} with the Frontier.
     *
     * @param caURI The CandidateURI to be scheduled
     *
     * @return true if CandidateURI was accepted by crawl scope, false otherwise
     */

    private boolean scheduleURI (CandidateURI caURI) {
        if(getController().getScope().accepts(caURI)) {
            logger.finer("URI accepted: " + caURI);
            getController().getFrontier().batchScheduleURI(caURI);
            return true;
        }
        logger.finer("URI rejected: " + caURI);
        return false;
    }
        
    /**
     * Method handles links arcording the collection, type and scheduling 
     * priority.
     * 
     * @param curi CrawlURI that is origin of the links.
     * @param baseUri URI that is used to resolve links.
     * @param collection Collection name.
     * @param linkType Type of links.
     * @param priority Scheduling priority of links.
     */    
    private void handleLinkCollection(CrawlURI curi, URI baseUri,
            String collection, char linkType, int priority)
    {
        if (curi.getFetchStatus() < 200 || curi.getFetchStatus() >= 400) {
            // do not follow links of error pages
            return;
        }
        Collection links = (Collection)curi.getAList().getObject(collection);
        Iterator iter = links.iterator();
        while(iter.hasNext()) {
            String link = (String)iter.next();
            try {
                UURI uuri = UURI.createUURI(link, baseUri);
                CandidateURI caURI = new CandidateURI(uuri, priority);
                caURI.setVia(curi);
                caURI.setPathFromSeed(curi.getPathFromSeed()+ linkType);
                logger.finest("inserting link from " + collection + " of type "
                    + linkType + " at head " + uuri);
                    
                scheduleURI(caURI);
            } catch (URISyntaxException ex) {
                Object[] array = { curi, link };
                getController().uriErrors.log(
                    Level.INFO,ex.getMessage(), array);
            }
        }
    }

}
