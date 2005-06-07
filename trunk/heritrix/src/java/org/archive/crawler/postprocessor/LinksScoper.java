/* Scoper
 * 
 * $Id$
 *
 * Created on Oct 2, 2003
 * 
 * Copyright (C) 2003 Internet Archive.
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
package org.archive.crawler.postprocessor;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.Heritrix;
import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.datamodel.UURIFactory;
import org.archive.crawler.extractor.Link;
import org.archive.crawler.framework.Filter;
import org.archive.crawler.framework.Scoper;
import org.archive.crawler.settings.MapType;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.Type;

/**
 * Determine which extracted links are within scope.
 * TODO: To test scope requires that Link be converted to
 * a CandidateURI.  Make it so do not need to make CandidateURIs
 * to test scope.
 * <p>Since this scoper creates CandidateURIs, no sense
 * discarding them. Put them into the CrawlURI in place of the
 * Link they wrap so we don't have to make new CandidateURIs
 * when we go to add links to the Frontier (Frontier#schedule
 * expects CandidateURI, not Link).
 *
 * @author gojomo
 * @author stack
 */
public class LinksScoper extends Scoper
implements FetchStatusCodes {
    private static Logger logger =
        Logger.getLogger(LinksScoper.class.getName());

    private final static String ATTR_SEED_REDIRECTS_NEW_SEEDS =
        "seed-redirects-new-seed";
    
    private final static Boolean DEFAULT_SEED_REDIRECTS_NEW_SEEDS =
        new Boolean(true);
    
    public static final String ATTR_LOG_REJECTS_ENABLED = "override-logger";
    
    public static final String ATTR_LOG_REJECT_FILTERS =
        "scope-rejected-url-filters";
    
    public static final String ATTR_SCOPE_EMBEDDED_LINKS =
        "scope-embedded-links";

    private final static Boolean DEFAULT_SCOPE_EMBEDDED_LINKS =
        new Boolean(true);
    
    /**
     * Instance of rejected uris log filters.
     */
    private MapType rejectLogFilters = null;
    
    
    /**
     * @param name Name of this filter.
     */
    public LinksScoper(String name) {
        super(name, "LinksScoper. Rules on which extracted links " +
            "are within configured scope.");
        
        Type t;
        t = addElementToDefinition(
            new SimpleType(ATTR_SEED_REDIRECTS_NEW_SEEDS,
            "If enabled, any URL found because a seed redirected to it " +
            "(original seed returned 301 or 302), will also be treated " +
            "as a seed.", DEFAULT_SEED_REDIRECTS_NEW_SEEDS));
        t.setExpertSetting(true);
        
        t = addElementToDefinition(new SimpleType(ATTR_LOG_REJECTS_ENABLED,
            "If enabled, all logging goes to a file named for this class in " +
            "the job log directory. Set the logging level in " +
            "heritrix.properites. Logging at level INFO will log URIs " +
            "rejected by scope.", new Boolean(true)));
        t.setExpertSetting(true);
        
        this.rejectLogFilters = (MapType)addElementToDefinition(
            new MapType(ATTR_LOG_REJECT_FILTERS, "Filters applied after" +
                " an URI has been rejected. If any filter returns" +
               " TRUE, the URI is logged if the logging level is INFO. " +
               "Depends on " + ATTR_LOG_REJECTS_ENABLED +
               " being enabled.", Filter.class));
        this.rejectLogFilters.setExpertSetting(true);
        
        t = addElementToDefinition(new SimpleType(ATTR_SCOPE_EMBEDDED_LINKS,
            "If enabled, embeded links (images etc.) are tested against " +
            "scope.", DEFAULT_SCOPE_EMBEDDED_LINKS));
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
        
        // If prerequisites, nothing to be done in here.
        if (curi.hasPrerequisitUri()) {
            return;
        }
        
        // Don't extract links of error pages.
        if (curi.getFetchStatus() < 200 || curi.getFetchStatus() >= 400) {
            curi.clearOutlinks();
            return;
        }
        
        if (curi.outlinksSize() <= 0) {
            // No outlinks to process.
            return;
        }

        final boolean scheduleEmbeds = ((Boolean)getUncheckedAttribute(curi,
            ATTR_SCOPE_EMBEDDED_LINKS)).booleanValue();
        final boolean redirectsNewSeeds = ((Boolean)getUncheckedAttribute(curi,
            ATTR_SEED_REDIRECTS_NEW_SEEDS)).booleanValue();
        Collection inscopeLinks = new HashSet();
        for (final Iterator iter = curi.getOutLinks().iterator();
                iter.hasNext();) {
            final Link wref = (Link)iter.next();
            try {
                final int directive = getSchedulingFor(wref, scheduleEmbeds);
                if (directive == CandidateURI.DONT_SCHEDULE) {
                    continue;
                }
                final CandidateURI caURI = curi.createCandidateURI(wref, directive,
                    considerAsSeed(curi, wref, redirectsNewSeeds));
                if (isInScope(caURI)) {
                    inscopeLinks.add(caURI);
                }
            } catch (URIException e) {
                getController().logUriError(e, curi.getUURI(), 
                    wref.getDestination().toString());
            }
        }
        // Replace current links collection w/ inscopeLinks.  May be
        // an empty collection.
        curi.replaceOutlinks(inscopeLinks);
    }
    
    protected void outOfScope(CandidateURI caUri) {
        super.outOfScope(caUri);
        
        // Run the curi through another set of filters to see
        // if we should log it to the scope rejection log.
        if (!logger.isLoggable(Level.INFO)) {
            return;
        }
        CrawlURI curi = (caUri instanceof CrawlURI)?
            (CrawlURI)caUri:
            new CrawlURI(caUri.getUURI());
        if (filtersAccept(this.rejectLogFilters, curi)) {
            logger.info("Rejected " + curi.getUURI().toString());
        }
    }
    
    private boolean considerAsSeed(final CrawlURI curi, final Link wref,
            final boolean redirectsNewSeeds) {
        // Check if this is a seed with a 301 or 302.
        if (curi.isSeed()
                && (curi.getFetchStatus() == 301 ||
                    curi.getFetchStatus() == 302)
                && wref.getHopType() == Link.REFER_HOP) {
            // Check if redirects from seeds should be treated as seeds.
            if (redirectsNewSeeds) {
                return true;
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
    
    protected int getSchedulingFor(final Link wref,
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
}