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
 * CrawlOrder.java
 * Created on May 15, 2003
 *
 * $Header$
 */
package org.archive.crawler.datamodel;

import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;

import org.archive.crawler.basic.Scope;
import org.archive.crawler.datamodel.settings.CrawlerModule;
import org.archive.crawler.datamodel.settings.CrawlerSettings;
import org.archive.crawler.datamodel.settings.MapType;
import org.archive.crawler.datamodel.settings.SimpleType;
import org.archive.crawler.framework.CrawlController;

/** Read and manipulate configuration (order) file.
 */
public class CrawlOrder extends CrawlerModule {
    private static Logger logger =
        Logger.getLogger("org.archive.crawler.datamodel.CrawlOrder");

    public static final String ATTR_SETTINGS_DIRECTORY = "settings-directory";
    public static final String ATTR_DISK_PATH = "disk-path";
    public static final String ATTR_MAX_BYTES_DOWNLOAD = "max-bytes-download";
    public static final String ATTR_MAX_DOCUMENT_DOWNLOAD = "max-document-download";
    public static final String ATTR_MAX_TIME_SEC = "max-time-sec";
    public static final String ATTR_MAX_TOE_THREADS = "max-toe-threads";
    public static final String ATTR_HTTP_HEADERS = "http-headers";
    public static final String ATTR_USER_AGENT = "user-agent";
    public static final String ATTR_FROM = "from";
    public static final String ATTR_FRONTIER = "frontier";
    public static final String ATTR_PROCESSORS = "processors";

    String caseFlattenedUserAgent;

    private MapType httpHeaders;
    private MapType processors;
    
    private CrawlController controller;

    /** Construct a CrawlOrder instance given a Document.
     * @param doc
     * @throws InitializationException
     */
    public CrawlOrder() {
        super("crawl-order", "Heritrix crawl order");

        addElementToDefinition(
            new SimpleType(
                ATTR_SETTINGS_DIRECTORY,
                "Directory where per host settings are kept",
                "settings"));
        addElementToDefinition(
            new SimpleType(ATTR_DISK_PATH, "Working directory", "disk"));
        addElementToDefinition(
            new SimpleType(
                ATTR_MAX_BYTES_DOWNLOAD,
                "Max number of bytes to download",
                new Integer(0)));
        addElementToDefinition(
            new SimpleType(
                ATTR_MAX_DOCUMENT_DOWNLOAD,
                "Max number of documents to download",
                new Integer(0)));
        addElementToDefinition(
            new SimpleType(ATTR_MAX_TIME_SEC, "Max time", new Integer(0)));
        addElementToDefinition(
            new SimpleType(
                ATTR_MAX_TOE_THREADS,
                "Max number of threads",
                new Integer(100)));

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

        addElementToDefinition(new CrawlerModule(ATTR_FRONTIER, "Frontier"));

        processors =
            (MapType) addElementToDefinition(new MapType(ATTR_PROCESSORS,
                "URI processors"));
    }

    public String getUserAgent(CrawlerSettings settings) {
        if (caseFlattenedUserAgent == null) {
            try {
                caseFlattenedUserAgent =
                    ((String) httpHeaders
                        .getAttribute(settings, ATTR_USER_AGENT))
                        .toLowerCase();
            } catch (AttributeNotFoundException e) {
                logger.severe(e.getMessage());
            }
        }
        return caseFlattenedUserAgent;
    }

    public String getFrom(CrawlerSettings settings) {
        String res = null;
        try {
            res = (String) httpHeaders.getAttribute(settings, ATTR_FROM);
        } catch (AttributeNotFoundException e) {
            logger.severe(e.getMessage());
        }
        return res;
    }

    public int getMaxToes() {
        Integer res = null;
        try {
            res = (Integer) getAttribute(null, ATTR_MAX_TOE_THREADS);
        } catch (AttributeNotFoundException e) {
            logger.severe(e.getMessage());
        }
        return res.intValue();
    }

//    public String getCrawlOrderFilename() {
//        return crawlOrderFilename;
//    }

    /**
     * This method constructs a new RobotsHonoringPolicy object from the orders file.
     * 
     * If this method is called repeatedly it will return the same instance each time.
     * 
     * @return the new RobotsHonoringPolicy
     */
    public RobotsHonoringPolicy getRobotsHonoringPolicy(CrawlerSettings settings) {
        try {
            return (RobotsHonoringPolicy) getAttribute(settings,
                    RobotsHonoringPolicy.ATTR_NAME);
        } catch (AttributeNotFoundException e) {
            logger.severe(e.getMessage());
            return null;
        }
    }

    /** Get the name of the order file.
     * 
     * @return the name of the order file.
     */
    public String getName() {
        return getSettingsHandler().getSettingsObject(null).getName();
    }
    
    /**
     * @return
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
}
