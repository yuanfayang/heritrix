/* 
 * CrawlOrder
 *
 * $Header$ 
 * 
 * Created on May 15, 2003
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

package org.archive.crawler.datamodel;

import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;

import org.archive.crawler.basic.Scope;
import org.archive.crawler.datamodel.settings.CrawlerModule;
import org.archive.crawler.datamodel.settings.MapType;
import org.archive.crawler.datamodel.settings.SimpleType;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.URIFrontier;

/** 
 * Represents the 'root' of the settings hierarchy. Contains those settings that
 * do not belong to any specific module, but rather relate to the crawl as a 
 * whole (much of this is used by the CrawlController directly or indirectly).
 * 
 * @see org.archive.crawler.datamodel.settings.CrawlerModule
 */
public class CrawlOrder extends CrawlerModule {
    private static Logger logger =
        Logger.getLogger("org.archive.crawler.datamodel.CrawlOrder");

    public static final String ATTR_NAME = "crawl-order";
    public static final String ATTR_SETTINGS_DIRECTORY = "settings-directory";
    public static final String ATTR_DISK_PATH = "disk-path";
    public static final String ATTR_MAX_BYTES_DOWNLOAD = "max-bytes-download";
    public static final String ATTR_MAX_DOCUMENT_DOWNLOAD = "max-document-download";
    public static final String ATTR_MAX_TIME_SEC = "max-time-sec";
    public static final String ATTR_MAX_TOE_THREADS = "max-toe-threads";
    public static final String ATTR_HTTP_HEADERS = "http-headers";
    public static final String ATTR_USER_AGENT = "user-agent";
    public static final String ATTR_FROM = "from";
    public static final String ATTR_PROCESSORS = "processors";
    public static final String ATTR_LOGGERS = "loggers";

    String caseFlattenedUserAgent;

    private MapType httpHeaders;
    private MapType processors;
    private MapType loggers;
    
    private CrawlController controller;

    /** Construct a CrawlOrder. 
     */
    public CrawlOrder() {
        super(ATTR_NAME, "Heritrix crawl order");

        addElementToDefinition(
            new SimpleType(
                ATTR_SETTINGS_DIRECTORY,
                "Directory where override settings are kept",
                "settings",
                false));
        addElementToDefinition(
            new SimpleType(
                ATTR_DISK_PATH,
                "Directory relative to the crawl order where logs, arcs and other run time files will be kept",
                "disk",
                false));
        addElementToDefinition(
            new SimpleType(
                ATTR_MAX_BYTES_DOWNLOAD,
                "Maximum number of bytes to download. Once this number is exceeded the crawler will stop.",
                new Long(0),
                false));
        addElementToDefinition(
            new SimpleType(
                ATTR_MAX_DOCUMENT_DOWNLOAD,
                "Maximum number of documents to download. Once this number is exceeded the crawler will stop.",
                new Long(0),
                false));
        addElementToDefinition(
            new SimpleType(
                ATTR_MAX_TIME_SEC,
                "Maximum amount of time to crawl (in seconds). Once this much time has elapsed the crawler will stop.",
                new Long(0),
                false));
        addElementToDefinition(
            new SimpleType(
                ATTR_MAX_TOE_THREADS,
                "Max number of threads processing URIs at the same time.",
                new Integer(100),
                false));

        addElementToDefinition(new Scope());
        
        httpHeaders =
            (MapType) addElementToDefinition(new MapType(ATTR_HTTP_HEADERS,
                "HTTP headers"));
        httpHeaders.addElementToDefinition(
            new SimpleType(
                ATTR_USER_AGENT,
                "User agent to act as",
                "os-heritrix/@VERSION@ (+PROJECT_URL_HERE)"));
        httpHeaders.addElementToDefinition(
            new SimpleType(
                ATTR_FROM,
                "Contact information",
                "CONTACT_EMAIL_ADDRESS_HERE"));

        addElementToDefinition(new RobotsHonoringPolicy());

        addElementToDefinition(new CrawlerModule(URIFrontier.ATTR_NAME, "Frontier"));

        processors =
            (MapType) addElementToDefinition(new MapType(ATTR_PROCESSORS,
                "URI processors"));

        loggers =
            (MapType) addElementToDefinition(new MapType(ATTR_LOGGERS,
                "Loggers"));
    }

    public String getUserAgent(CrawlURI curi) {
        if (caseFlattenedUserAgent == null) {
            try {
                caseFlattenedUserAgent =
                    ((String) httpHeaders
                        .getAttribute(ATTR_USER_AGENT, curi))
                        .toLowerCase();
            } catch (AttributeNotFoundException e) {
                logger.severe(e.getMessage());
            }
        }
        return caseFlattenedUserAgent;
    }

    public String getFrom(CrawlURI curi) {
        String res = null;
        try {
            res = (String) httpHeaders.getAttribute(ATTR_FROM, curi);
        } catch (AttributeNotFoundException e) {
            logger.severe(e.getMessage());
        }
        return res;
    }

    /**
     * Returns the set number of maximum toe threads.
     * @return Number of maximum toe threads
     */
    public int getMaxToes() {
        Integer res = null;
        try {
            res = (Integer) getAttribute(null, ATTR_MAX_TOE_THREADS);
        } catch (AttributeNotFoundException e) {
            logger.severe(e.getMessage());
        }
        return res.intValue();
    }

    /**
     * This method constructs a new RobotsHonoringPolicy object from the orders file.
     * 
     * If this method is called repeatedly it will return the same instance each time.
     * 
     * @param curi
     * @return the new RobotsHonoringPolicy
     */
    public RobotsHonoringPolicy getRobotsHonoringPolicy(CrawlURI curi) {
        try {
            return (RobotsHonoringPolicy) getAttribute(RobotsHonoringPolicy.ATTR_NAME, curi);
        } catch (AttributeNotFoundException e) {
            logger.severe(e.getMessage());
            return null;
        }
    }

    /** Get the name of the order file.
     * 
     * @return the name of the order file.
     */
    public String getCrawlOrderName() {
        return getSettingsHandler().getSettingsObject(null).getName();
    }
    
    /**
     * @return The crawl controller.
     */
    public CrawlController getController() {
        return controller;
    }

    /**
     * @param controller
     */
    public void setController(CrawlController controller) {
        this.controller = controller;
    }
    
    /**
     * Returns the Map of the StatisticsTracking modules that are included in the 
     * configuration that the current instance of this class is representing.
     * @return Map of the StatisticsTracking modules
     */
    public MapType getLoggers() {
        return loggers;
    }
    
    /**
     * Returns the Map of the URI Processor series as defined by the configuration
     * that the current instance of this class is representing.
     * @return Map of the URI Processor series.
     */
    public MapType getProcessors() {
        return processors;
    }
}
