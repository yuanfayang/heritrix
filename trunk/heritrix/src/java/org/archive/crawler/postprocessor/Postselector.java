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
package org.archive.crawler.postprocessor;

import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.framework.Processor;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.Type;

/**
 * Determine which extracted links etc get fed back into Frontier.
 *
 * Could in the future also control whether current URI is retried.
 *
 * @author gojomo
 *
 */
public class Postselector extends Processor implements CoreAttributeConstants, 
        FetchStatusCodes {
            
    private static Logger logger = 
        Logger.getLogger("org.archive.crawler.basic.Postselector");

    private final static Boolean DEFAULT_SEED_REDIRECTS_NEW_SEEDS = 
        new Boolean(true);
    private final static String ATTR_SEED_REDIRECTS_NEW_SEEDS = 
        "seed-redirects-new-seed";
    
    // limits on retries TODO: separate into retryPolicy?
    //private int maxDeferrals = 10; // should be at least max-retries plus 3 or so

    /**
     * @param name
     */
    public Postselector(String name) {
        super(name, "Post selector. \nDetermines which extracted links and " +
                "other related information gets fed back to the Frontier.");
        Type t;
        t = addElementToDefinition(new SimpleType(ATTR_SEED_REDIRECTS_NEW_SEEDS,
                "If enabled, any URL found because a seed redirected to it " +
                "(seed returned 301 or 302) will be treated as a seed.",
                DEFAULT_SEED_REDIRECTS_NEW_SEEDS));
        t.setExpertSetting(true);
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

        UURI baseUri = getBaseURI(curi);
        // handle http headers
        if (curi.getAList().containsKey(A_HTTP_HEADER_URIS)) {
            handleLinkCollection(curi, baseUri, A_HTTP_HEADER_URIS, 'R',
                CandidateURI.HIGH);
        }
        // handle embeds
        if (curi.getAList().containsKey(A_HTML_EMBEDS)) {
            handleLinkCollection(curi, baseUri, A_HTML_EMBEDS, 'E',
                CandidateURI.NORMAL);
        }
        // handle speculative embeds
        if (curi.getAList().containsKey(A_HTML_SPECULATIVE_EMBEDS)) {
            handleLinkCollection(curi, baseUri,A_HTML_SPECULATIVE_EMBEDS, 'X',
                CandidateURI.NORMAL);
        }
        // handle links
        if (curi.getAList().containsKey(A_HTML_LINKS)) {
            handleLinkCollection(
                curi, baseUri, A_HTML_LINKS, 'L', CandidateURI.NORMAL);
        }
        // handle css links
        if (curi.getAList().containsKey(A_CSS_LINKS)) {
            handleLinkCollection(
                curi, baseUri, A_CSS_LINKS, 'E', CandidateURI.NORMAL);
        }
        // handle js file links
        if (curi.getAList().containsKey(A_JS_FILE_LINKS)) {
            UURI viaURI = baseUri;
            if (curi.flattenVia() != null && curi.flattenVia().length() != 0) {
                try {
                    viaURI = new UURI(curi.flattenVia());
                } catch (URIException e) {
                    Object[] array = { curi, curi.flattenVia() };
                    getController().uriErrors.log(
                        Level.INFO, e.getMessage(), array);
                }
            }
            handleLinkCollection( curi, viaURI, 
                A_JS_FILE_LINKS, 'X', CandidateURI.NORMAL);
        }
        
    }

    private UURI getBaseURI(CrawlURI curi) {
        if (!curi.getAList().containsKey(A_HTML_BASE)) {
            return curi.getUURI();
        }
        String base = curi.getAList().getString(A_HTML_BASE);
        try {
            return new UURI(base);
        } catch (URIException e) {
            Object[] array = { curi, base };
            getController().uriErrors.log(Level.INFO,e.getMessage(), array);
            // next best thing: use self
            return curi.getUURI();
        }
    }

    protected void handlePrerequisites(CrawlURI curi) {
//        if ( curi.getDeferrals() > maxDeferrals ) {
//            // too many deferrals, equals failure
//            curi.setFetchStatus(S_PREREQUISITE_FAILURE);
//            return;
//        }
        
        try {
            // create and schedule prerequisite
            UURI prereq = new UURI(getBaseURI(curi),
                (String)curi.getPrerequisiteUri());
            CandidateURI caUri = new CandidateURI(prereq);
            caUri.setSchedulingDirective(CandidateURI.FORCE_REVISIT);                              
            caUri.setVia(curi);
            caUri.setPathFromSeed(curi.getPathFromSeed()+ "P");

            if (!schedule(caUri)) {
                // prerequisite cannot be scheduled (perhaps excluded by scope)
                // must give up on
                curi.setFetchStatus(S_PREREQUISITE_FAILURE);
                return;
            }
            // leave PREREQ in place so frontier can properly defer this curi
        } catch (URIException ex) {
            Object[] array = { curi, curi.getPrerequisiteUri() };
            getController().uriErrors.log(Level.INFO,ex.getMessage(), array);
        } catch (NumberFormatException e) {
            // UURI.createUURI will occasionally throw this error.
            Object[] array = { curi, curi.getPrerequisiteUri() };
            getController().uriErrors.log(
                Level.INFO,e.getMessage(), array);
        }
    }


    /**
     * Schedule the given {@link CandidateURI CandidateURI} with the Frontier.
     *
     * @param caUri The CandidateURI to be scheduled
     *
     * @return true if CandidateURI was accepted by crawl scope, false otherwise
     */
    private boolean schedule(CandidateURI caUri) {
        if(getController().getScope().accepts(caUri)) {
            logger.finer("URI accepted: "+caUri);
            getController().getFrontier().batchSchedule(caUri);
            return true;
        }
        logger.finer("URI rejected: " + caUri);
        return false;
    }
        
    /**
     * Method handles links according to the collection, type and scheduling 
     * priority.
     * 
     * @param curi CrawlURI that is origin of the links.
     * @param baseUri URI that is used to resolve links.
     * @param collection Collection name.
     * @param linkType Type of links.
     * @param directive how should URIs be scheduled
     */    
    private void handleLinkCollection(CrawlURI curi, UURI baseUri,
            String collection, char linkType, String directive)
    {
        if (curi.getFetchStatus() < 200 || curi.getFetchStatus() >= 400) {
            // do not follow links of error pages
            return;
        }
        
        // Check if this is a seed with a 301 or 302.
        boolean seed = false;
        if ( curi.isSeed() 
                && (curi.getFetchStatus()==301 || curi.getFetchStatus()==302)
                && collection.equals(A_HTTP_HEADER_URIS) ) {
            try {
                // Check if redirects from seeds should be treated as seeds.
                if(((Boolean) getAttribute(
                        ATTR_SEED_REDIRECTS_NEW_SEEDS)).booleanValue()){
                    // Treat any discovered URIs as seeds. Should only be 1.
                    seed = true;
                }
            } catch (MBeanException e1) {
                e1.printStackTrace();
            } catch (ReflectionException e1) {
                e1.printStackTrace();
            } catch (AttributeNotFoundException e) {
                e.printStackTrace();
            }
        }
        
        Collection links = (Collection)curi.getAList().getObject(collection);
        for (Iterator iter = links.iterator(); iter.hasNext(); ) {
            String link = (String)iter.next();
            if (link == null || link.length() <= 0) {
                continue;   
            }
            try {
                UURI uuri = new UURI(baseUri, link);
                CandidateURI caURI = new CandidateURI(uuri);
                caURI.setSchedulingDirective(directive);
                caURI.setIsSeed(seed);
                caURI.setVia(curi);
                caURI.setPathFromSeed(curi.getPathFromSeed()+ linkType);
                logger.finest("inserting link from " + collection + " of type "
                    + linkType + " at head " + uuri);
                    
                schedule(caURI);
            } catch (URIException ex) {
                Object[] array = { curi, link };
                getController().uriErrors.log(
                    Level.INFO,ex.getMessage(), array);
            } catch (NumberFormatException e) {
                // UURI.createUURI will occasionally throw this error.
                Object[] array = { curi, link };
                getController().uriErrors.log(
                    Level.INFO,e.getMessage(), array);
            }
        }
    }

}
