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
 * Processor.java
 * Created on Apr 16, 2003
 *
 * $Header$
 */
package org.archive.crawler.framework;

import java.lang.reflect.Constructor;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.settings.CrawlerModule;
import org.archive.crawler.datamodel.settings.CrawlerSettings;
import org.archive.crawler.datamodel.settings.MapType;
import org.archive.crawler.datamodel.settings.SimpleType;

/**
 * Base class for URI processing classes.
 * <p>
 * Each URI is processed be a user defined series of processors. This class
 * provides the basic infrastructure for these but does not actually do
 * anything. New processors can be easily created by subclassing this class.
 *
 * @author Gordon Mohr
 */
public class Processor extends CrawlerModule {
    /**
     * Key to use asking settings for filters value.
     */
    public final static String ATTR_FILTERS = "filters";

    /**
     * Key to use asking settings for enabled value.
     */
    public final static String ATTR_ENABLED = "enabled";

    /**
     * Key to use asking settings for postprocessor value.
     */
    public final static String ATTR_POSTPROCESSOR = "postprocessor";

    private MapType filters;
    private Processor defaultNextProcessor = null;

    private static Logger logger =
        Logger.getLogger("org.archive.crawler.framework.Processor");

    /**
     * @param name
     * @param description
     */
    public Processor(String name, String description) {
        super(name, description);
        addElementToDefinition(new SimpleType(ATTR_ENABLED,
            "Is processor enabled", new Boolean(true)));
        addElementToDefinition(new SimpleType(ATTR_POSTPROCESSOR,
            "Postprocessor", new Boolean(false)));
        filters = (MapType) addElementToDefinition(new MapType(ATTR_FILTERS,
            "Filters", Filter.class));
    }

    public final void process(CrawlURI curi) {
        // by default, arrange for curi to proceed to next processor
        curi.setNextProcessor(getDefaultNext(curi));
        // Check if this processor is enabled before processing
        try {
            if (!((Boolean) getAttribute(ATTR_ENABLED, curi)).booleanValue()) {
                return;
            }
        } catch (AttributeNotFoundException e) {
            logger.severe(e.getMessage());
        }

        if(filtersAccept(curi)) {
            innerProcess(curi);
        } else {
            innerRejectProcess(curi);
        }
    }

    /**
     * @param curi
     */
    protected void innerRejectProcess(CrawlURI curi) {
        // by default do nothing
    }

    /**
     * Classes subclassing this one should override this method to perfrom
     * their custom actions on the CrawlURI.
     *
     * @param curi The CrawlURI being processed.
     */
    protected void innerProcess(CrawlURI curi) {
        // by default do nothing
    }

    /**
     * Do all specified filters (if any) accept this CrawlURI?
     *
     * @param curi
     * @return True if all filters accept this CrawlURI.
     */
    protected boolean filtersAccept(CrawlURI curi) {
        CrawlerSettings settings = getSettingsFromObject(curi);
        if (filters.isEmpty(settings)) {
            return true;
        }
        Iterator iter = filters.iterator(settings);
        while(iter.hasNext()) {
            Filter f = (Filter)iter.next();
            if( !f.accepts(curi) ) {
                logger.info(f+" rejected "+curi+" in Processor "+getName());
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the next processor for the given CrawlURI in the processor chain.
     * @param curi The CrawlURI that we want to find the next processor for.
     * @return The next processor for the given CrawlURI in the processor chain.
     */
    private Processor getDefaultNext(CrawlURI curi) {
        return defaultNextProcessor;
    }

    public void setDefaultNextProcessor(Processor nextProcessor) {
        defaultNextProcessor = nextProcessor;
    }

    private Processor getPostprocessor(CrawlURI curi) {
        String processorName;
        Processor processor = null;
        try {
            processorName = (String) getAttribute(ATTR_POSTPROCESSOR, curi);
            processor = (Processor) getParent().getAttribute(processorName, curi);
        } catch (AttributeNotFoundException e) {
            logger.severe(e.getMessage());
        }
        return processor;
    }

    public CrawlController getController() {
        return getSettingsHandler().getOrder().getController();
    }

    public Processor spawn(int serialNum) {
        Processor newInstance = null;
        try {
            Constructor co =
                getClass().getConstructor(new Class[] { String.class });
            newInstance =
                (Processor) co.newInstance(new Object[] {
                    getName() + serialNum
                    });
            getParent().setAttribute(newInstance);
            newInstance.setTransient(true);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return newInstance;
    }

    /**
     * Compiles and returns a report (in human readable form) about the status
     * of the processor.  The processor's name (of implementing class) should
     * always be included.
     * <p>
     * Examples of stats declared would include:<br>
     * * Number of CrawlURIs handled.<br>
     * * Number of links extracted (for link extractors)<br>
     * etc.
     *
     * @return A human readable report on the processor's state.
     */
    public String report(){
        return ""; // Default behavior.
    }
}
