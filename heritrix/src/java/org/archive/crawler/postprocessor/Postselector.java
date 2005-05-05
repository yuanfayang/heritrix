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

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Iterator;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.Heritrix;
import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.datamodel.UURIFactory;
import org.archive.crawler.extractor.Link;
import org.archive.crawler.framework.Filter;
import org.archive.crawler.framework.Processor;
import org.archive.crawler.settings.MapType;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.Type;

/**
 * Determine which extracted links etc get fed back into Frontier.
 *
 * Could in the future also control whether current URI is retried.
 *
 * @author gojomo
 */
public class Postselector extends Processor
implements CoreAttributeConstants, FetchStatusCodes {

    private static Logger logger =
        Logger.getLogger(Postselector.class.getName());

    private final static Boolean DEFAULT_SEED_REDIRECTS_NEW_SEEDS =
        new Boolean(true);
    private final static String ATTR_SEED_REDIRECTS_NEW_SEEDS =
        "seed-redirects-new-seed";
    
    public static final String ATTR_LOG_REJECTS_ENABLED = "override-logger";
    
    public static final String ATTR_LOG_REJECT_FILTERS =
        "scope-rejected-uri-log-filters";
    
    public static final String ATTR_SCHEDULE_EMBEDDED_LINKS =
        "schedule-embedded-links";
    private final static Boolean DEFAULT_SCHEDULE_EMBEDDED_LINKS =
        new Boolean(true);
    
    /**
     * Instance of rejected uris log filters.
     */
    private MapType rejectLogFilters = null;
    
    /**
     * @param name Name of this filter.
     */
    public Postselector(String name) {
        super(name, "Post selector. Determines which extracted links and " +
                "other related information gets fed back to the Frontier.");
        Type t;
        t = addElementToDefinition(
                new SimpleType(ATTR_SEED_REDIRECTS_NEW_SEEDS,
                "If enabled, any URL found because a seed redirected to it " +
                "(seed returned 301 or 302) will be treated as a seed.",
                DEFAULT_SEED_REDIRECTS_NEW_SEEDS));
        t.setExpertSetting(true);
        
        t = addElementToDefinition(new SimpleType(ATTR_LOG_REJECTS_ENABLED,
            "If enabled, all logging goes to a file named for this class in" +
            " the job log" +
            " directory. Set the logging level in heritrix.properites." +
            " Logging at level INFO will log URIs rejected by scope.",
            new Boolean(true)));
        t.setExpertSetting(true);
        this.rejectLogFilters = (MapType)addElementToDefinition(
            new MapType(ATTR_LOG_REJECT_FILTERS, "Filters applied after" +
                " an URI has been rejected. If any filter returns" +
               " TRUE, the URI is logged if the logging level is INFO.",
            Filter.class));
        this.rejectLogFilters.setExpertSetting(true);
        
        t = addElementToDefinition(new SimpleType(ATTR_SCHEDULE_EMBEDDED_LINKS,
            "If enabled, embeded links (images etc.) are scheduled for " +
            "crawling.", DEFAULT_SCHEDULE_EMBEDDED_LINKS));
        t.setExpertSetting(true);
    }
   
    protected void initialTasks() {
        super.initialTasks();
        // Set up logger for this instance.  May have special directives
        // since this class can log scope-rejected URLs.
        if (isOverrideEnabled(null))    {
            int limit = Heritrix.getIntProperty(
                "java.util.logging.FileHandler.limit",
                1024 * 1024 * 1024 * 1024);
            int count = Heritrix.getIntProperty(
                "java.util.logging.FileHandler.count", 1);
            try {
                File logsDir = getController().getLogsDir();
                String tmp = Heritrix.
                    getProperty("java.util.logging.FileHandler.pattern");
                File logFile = new File(logsDir,
                    this.getClass().getName() +
                        ((tmp != null && tmp.length() > 0)? tmp: ".log"));
                FileHandler fh = new FileHandler(logFile.getAbsolutePath(),
                    limit, count, true);
                // Manage the formatter to use.
                tmp = Heritrix.
                    getProperty("java.util.logging.FileHandler.formatter");
                if (tmp != null && tmp.length() > 0) {
                        Constructor co = Class.forName(tmp).
                            getConstructor(new Class [] {});
                        Formatter f = (Formatter)co.
                            newInstance(new Object [] {});
                        fh.setFormatter(f);
                }
                logger.addHandler(fh);
                logger.setUseParentHandlers(false);
            } catch (Exception e) {
                logger.severe("Failed customization of logger: " +
                    e.getMessage());
            }
        }
    }

    protected void innerProcess(final CrawlURI curi) {
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(getName() + " processing " + curi);
        }
        
        // handle any prerequisites
        if (curi.containsKey(A_PREREQUISITE_URI)) {
            handlePrerequisites(curi);
            return;
        }
        
        if (curi.getFetchStatus() < 200 || curi.getFetchStatus() >= 400) {
            // do not follow links of error pages
            return;
        }

        final boolean scheduleEmbeds = ((Boolean)getUncheckedAttribute(curi,
            ATTR_SCHEDULE_EMBEDDED_LINKS)).booleanValue();
        final boolean redirectsNewSeeds = ((Boolean)getUncheckedAttribute(curi,
            ATTR_SEED_REDIRECTS_NEW_SEEDS)).booleanValue();
            
        for (final Iterator iter = curi.getOutLinks().iterator();
                iter.hasNext();) {
            final Link wref = (Link)iter.next();
            try {
                final int directive = getSchedulingFor(wref, scheduleEmbeds);
                if(directive != CandidateURI.DONT_SCHEDULE) {
                    final CandidateURI caURI = createCandidateURI(curi, wref);
                    caURI.setSchedulingDirective(directive);
                    caURI.setIsSeed(considerAsSeed(curi, wref,
                        redirectsNewSeeds));
                    schedule(caURI);
                }
            } catch (URIException e) {
                getController().logUriError(e,curi.getUURI(),wref.getDestination().toString());
            }
        }
    }
    
    private boolean considerAsSeed(final CrawlURI curi, final Link wref, final boolean redirectsNewSeeds) {
        // Check if this is a seed with a 301 or 302.
        if (curi.isSeed()
                && (curi.getFetchStatus() == 301 || curi.getFetchStatus() == 302)
                && wref.getHopType() == Link.REFER_HOP) {
            
            // Check if redirects from seeds should be treated as seeds.
            if (redirectsNewSeeds) {
                return true;
            }
        }
        return false;
    }

    private int getSchedulingFor(final Link wref,
            final boolean scheduleEmbeds) {
        final char c = wref.getHopType();
        switch (c) {
            case Link.REFER_HOP:
                // treat redirects somewhat urgently
                return CandidateURI.MEDIUM;
            case Link.EMBED_HOP:
                if(!scheduleEmbeds) {
                    return CandidateURI.DONT_SCHEDULE;
                }
            default:
                // everything else normal (at least for now)
                return CandidateURI.NORMAL;
        }
    }

    protected void handlePrerequisites(CrawlURI curi) {
        try {
            // create and schedule prerequisite
            CandidateURI caUri = createCandidateURI(curi,
                curi.getPrerequisiteUri());
            int prereqPriority = curi.getSchedulingDirective() - 1;
            if (prereqPriority < 0) {
                prereqPriority = 0;
                logger.severe("unable to promote prerequisite " + caUri +
                    " above " + curi);
            }
            caUri.setSchedulingDirective(curi.getSchedulingDirective() - 1);
            caUri.setForceFetch(true);
            if (!schedule(caUri)) {
                // prerequisite cannot be scheduled (perhaps excluded by scope)
                // must give up on
                curi.setFetchStatus(S_PREREQUISITE_UNSCHEDULABLE_FAILURE);
                return;
            }
            // leave PREREQ in place so frontier can properly defer this curi
       } catch (URIException ex) {
            Object[] array = {curi, curi.getPrerequisiteUri()};
            getController().uriErrors.log(Level.INFO,ex.getMessage(), array);
        } catch (NumberFormatException e) {
            // UURI.createUURI will occasionally throw this error.
            Object[] array = {curi, curi.getPrerequisiteUri()};
            getController().uriErrors.log(Level.INFO,e.getMessage(), array);
        }
    }

    /**
     * Schedule the given {@link CandidateURI CandidateURI} with the Frontier.
     * @param caUri The CandidateURI to be scheduled.
     * @return true if CandidateURI was accepted by crawl scope, false
     * otherwise.
     */
    protected boolean schedule(CandidateURI caUri) {
        if(getController().getScope().accepts(caUri)) {
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("Accepted: " + caUri);
            }
            getController().getFrontier().schedule(caUri);
            return true;
        }

        // Run the curi through another set of filters to see
        // if we should log it to the scope rejection log.
        if (logger.isLoggable(Level.INFO)) {
            CrawlURI curi = (caUri instanceof CrawlURI)?
                (CrawlURI)caUri: new CrawlURI(caUri.getUURI());
            if (filtersAccept(this.rejectLogFilters, curi)) {
                logger.info("Rejected " + curi.getUURI().toString());
            }
        }
        return false;
    }
    
    
    public boolean isOverrideEnabled(Object context) {
        boolean result = true;
        try {
            Boolean b = (Boolean)getAttribute(context,
                ATTR_LOG_REJECTS_ENABLED);
            if (b != null) {
                result = b.booleanValue();
            }
        } catch (AttributeNotFoundException e) {
            logger.warning("Failed get of 'enabled' attribute.");
        }

        return result;
    }
    
    protected CandidateURI createCandidateURI(CrawlURI curi, Link link)
            throws URIException {
        UURI uuri;
        if (link.getDestination() instanceof UURI) {
            uuri = (UURI) link.getDestination();
        } else {
            uuri = UURIFactory.getInstance(curi.getBaseURI(), link
                    .getDestination().toString());
        }
        CandidateURI caURI = new CandidateURI(uuri, curi.getPathFromSeed()
                + link.getHopType(), curi.getUURI(), link.getContext());
        return caURI;
    }
}
