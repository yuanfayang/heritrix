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

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;

import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.CrawlScope;
import org.archive.crawler.framework.Processor;
import org.archive.crawler.framework.URIFrontier;
import org.archive.crawler.framework.exceptions.FatalConfigurationException;
import org.archive.crawler.settings.MapType;
import org.archive.crawler.settings.ModuleType;
import org.archive.crawler.settings.RegularExpressionConstraint;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.Type;

/**
 * Represents the 'root' of the settings hierarchy. Contains those settings that
 * do not belong to any specific module, but rather relate to the crawl as a
 * whole (much of this is used by the CrawlController directly or indirectly).
 *
 * @see org.archive.crawler.settings.ModuleType
 */
public class CrawlOrder extends ModuleType {
    private static Logger logger =
        Logger.getLogger("org.archive.crawler.datamodel.CrawlOrder");

    public static final String ATTR_NAME = "crawl-order";
    public static final String ATTR_SETTINGS_DIRECTORY = "settings-directory";
    public static final String ATTR_DISK_PATH = "disk-path";
    public static final String ATTR_LOGS_PATH = "logs-path";
    public static final String ATTR_CHECKPOINTS_PATH = "checkpoints-path";
    public static final String ATTR_STATE_PATH = "state-path";
    public static final String ATTR_SCRATCH_PATH = "scratch-path";
    public static final String ATTR_RECOVER_PATH = "recover-path";
    public static final String ATTR_MAX_BYTES_DOWNLOAD = "max-bytes-download";
    public static final String ATTR_MAX_DOCUMENT_DOWNLOAD = "max-document-download";
    public static final String ATTR_MAX_TIME_SEC = "max-time-sec";
    public static final String ATTR_MAX_TOE_THREADS = "max-toe-threads";
    public static final String ATTR_HTTP_HEADERS = "http-headers";
    public static final String ATTR_USER_AGENT = "user-agent";
    public static final String ATTR_FROM = "from";
    public static final String ATTR_PRE_FETCH_PROCESSORS = "pre-fetch-processors";
    public static final String ATTR_FETCH_PROCESSORS = "fetch-processors";
    public static final String ATTR_EXTRACT_PROCESSORS = "extract-processors";
    public static final String ATTR_WRITE_PROCESSORS = "write-processors";
    public static final String ATTR_POST_PROCESSORS = "post-processors";
    public static final String ATTR_LOGGERS = "loggers";


    String caseFlattenedUserAgent;

    private MapType httpHeaders;
    private MapType loggers;

    private CrawlController controller;
    

    /**
     * Regex for acceptable user-agent format.
     */
    private static String ACCEPTABLE_USER_AGENT =
        "\\S+.*\\(.*\\+http(s)?://\\S+\\.\\S+.*\\).*";
    
    /**
     * Regex for acceptable from address.
     */
    private static String ACCEPTABLE_FROM = "\\S+@\\S+\\.\\S+";
    

    /** Construct a CrawlOrder.
     */
    public CrawlOrder() {
        super(ATTR_NAME, "Heritrix crawl order. \nThis forms the root of " +
                "the settings framework.");
        Type e;

        e = addElementToDefinition(new SimpleType(ATTR_SETTINGS_DIRECTORY,
                "Directory where override settings are kept. \nThe settings " +
                "for many modules can be overridden based on the domain or " +
                "subdomain of the URI being processed. This setting specifies" +
                " a file level directory to store those settings. The path" +
                " is relative to the location of the global settings, unless" +
                " an absolute path is provided.", "settings"));
        e.setOverrideable(false);
        e.setExpertSetting(true);

        e = addElementToDefinition(new SimpleType(ATTR_DISK_PATH,
                "Directory where logs, arcs and other run time files will " +
                "be kept. If this path is a relative path, it will be " +
                "relative to the crawl order.", ""));
        e.setOverrideable(false);
        e.setExpertSetting(true);

        e = addElementToDefinition(new SimpleType(ATTR_LOGS_PATH,
                "Directory where crawler log files will be kept. If this path " +
                "is a relative path, it will be relative to the 'disk-path'.",
                "logs"));
        e.setOverrideable(false);
        e.setExpertSetting(true);

        e = addElementToDefinition(new SimpleType(ATTR_CHECKPOINTS_PATH,
                "Directory where crawler checkpoint files will be kept. If this path " +
                "is a relative path, it will be relative to the 'disk-path'.",
                "checkpoints"));
        e.setOverrideable(false);
        e.setExpertSetting(true);

        e = addElementToDefinition(new SimpleType(ATTR_STATE_PATH,
                "Directory where crawler-state files will be kept. If this path " +
                "is a relative path, it will be relative to the 'disk-path'.",
                "state"));
        e.setOverrideable(false);
        e.setExpertSetting(true);

        e = addElementToDefinition(new SimpleType(ATTR_SCRATCH_PATH,
                "Directory where discardable temporary files will be kept. If this path " +
                "is a relative path, it will be relative to the 'disk-path'.",
                "scratch"));
        e.setOverrideable(false);
        e.setExpertSetting(true);

        e = addElementToDefinition(new SimpleType(ATTR_MAX_BYTES_DOWNLOAD,
                "Maximum number of bytes to download. Once this number is"
                        + " exceeded the crawler will stop.", new Long(0)));
        e.setOverrideable(false);

        e = addElementToDefinition(new SimpleType(ATTR_MAX_DOCUMENT_DOWNLOAD,
                "Maximum number of documents to download. Once this number"
                        + " is exceeded the crawler will stop.", new Long(0)));
        e.setOverrideable(false);

        e = addElementToDefinition(new SimpleType(ATTR_MAX_TIME_SEC,
                "Maximum amount of time to crawl (in seconds). Once this"
                        + " much time has elapsed the crawler will stop.",
                new Long(0)));
        e.setOverrideable(false);

        e = addElementToDefinition(new SimpleType(ATTR_MAX_TOE_THREADS,
                "Maximum number of threads processing URIs at the same time.",
                new Integer(100)));
        e.setOverrideable(false);

        addElementToDefinition(new CrawlScope());

        httpHeaders = (MapType) addElementToDefinition(new MapType(
                ATTR_HTTP_HEADERS, "HTTP headers. \nInformation that will " +
                        "be used when constructing the HTTP headers of " +
                        "the crawler's HTTP requests."));

        e = httpHeaders.addElementToDefinition(new SimpleType(ATTR_USER_AGENT,
                "User agent to act as.\n Field must contain valid URL " +
                "that links to website of person or organization " +
                "running the crawl. Replace 'PROJECT_URL_HERE' in " +
                "initial template. E.g. If organization " +
                "is Library of Congress, a valid user agent would be:" +
                "'Mozilla/5.0 (compatible; loc-crawler/0.11.0 " +
                "+http://loc.gov)'. " +
                "Note, you must preserve the '+' before 'http' " +
                "and if present, '@VERSION@' is replaced by version.",
                "Mozilla/5.0 (compatible; heritrix/@VERSION@ +PROJECT_URL_HERE)"));
        e.addConstraint(new RegularExpressionConstraint(ACCEPTABLE_USER_AGENT,
                Level.WARNING,
                "This field must contain a valid URL leading to the website " +
                "of the person or organization responsible for this crawl."));

        e = httpHeaders.addElementToDefinition(new SimpleType(ATTR_FROM,
                "Contact information. \nThis field must contain a valid " +
                "e-mail address for the person or organization responsible" +
                "for this crawl: e.g. 'webmaster@loc.gov'",
                "CONTACT_EMAIL_ADDRESS_HERE"));
        e.addConstraint(new RegularExpressionConstraint(ACCEPTABLE_FROM,
                Level.WARNING, "This field must contain a valid e-mail " +
                        "address for the person or organization responsible " +
                        "for this crawl."));

        addElementToDefinition(new RobotsHonoringPolicy());

        e = addElementToDefinition(new ModuleType(
                URIFrontier.ATTR_NAME, "Frontier"));
        e.setLegalValueType(URIFrontier.class);

        e = addElementToDefinition(new MapType(
                ATTR_PRE_FETCH_PROCESSORS, "Processors to be run prior to" +
                        " fetching anything from the network.",
                        Processor.class));
        e.setOverrideable(false);

        e = addElementToDefinition(new MapType(
                ATTR_FETCH_PROCESSORS, "Processors that fetches documents."
                , Processor.class));
        e.setOverrideable(false);

        e = addElementToDefinition(new MapType(
                ATTR_EXTRACT_PROCESSORS, "Processors that extract new URIs" +
                        " from fetched documents.", Processor.class));
        e.setOverrideable(false);

        e = addElementToDefinition(new MapType(
                ATTR_WRITE_PROCESSORS, "Processors that write documents" +
                        " to archives.", Processor.class));
        e.setOverrideable(false);

        e = addElementToDefinition(new MapType(
                ATTR_POST_PROCESSORS, "Processors that do cleanup and feeds" +
                        " the frontier with new URIs.", Processor.class));
        e.setOverrideable(false);

        loggers = (MapType) addElementToDefinition(new MapType(ATTR_LOGGERS,
                "Statistics tracking modules. \nAny number of specialised " +
                "statistics tracker that monitor a crawl and write logs, " +
                "reports and/or provide information to the user interface."));

        e = addElementToDefinition(new SimpleType(ATTR_RECOVER_PATH,
                "Optional recover.log to preload Frontier.\n A recover log " +
                "is automatically generated during a crawl. If a crawl " +
                "crashes it can be used to recreate the status of the crawler" +
                " at the time of the crash to recover. This can take a long" +
                " time in somce cases, but is usually much quicker then " +
                "repeating a crawl.", ""));
        e.setOverrideable(false);
        e.setExpertSetting(true);

        e = addElementToDefinition(
           new CredentialStore(CredentialStore.ATTR_NAME));
        e.setOverrideable(true);
        e.setExpertSetting(true);
    }

    /**
     * @param curi
     * @return user-agent header value to use
     */
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

    /**
     * @param curi
     * @return from header value to use
     */
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
     * This method gets the RobotsHonoringPolicy object from the orders file.
     *
     * @return the new RobotsHonoringPolicy
     */
    public RobotsHonoringPolicy getRobotsHonoringPolicy() {
        try {
            return (RobotsHonoringPolicy) getAttribute(RobotsHonoringPolicy.ATTR_NAME);
        } catch (AttributeNotFoundException e) {
            logger.severe(e.getMessage());
            return null;
        } catch (MBeanException e) {
            logger.severe(e.getMessage());
            return null;
        } catch (ReflectionException e) {
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
     * Checks if the User Agent and From field are set 'correctly' in
     * the specified Crawl Order.
     *
     * @throws FatalConfigurationException
     */
    public void checkUserAgentAndFrom() throws FatalConfigurationException {
        // don't start the crawl if they're using the default user-agent
        String userAgent = this.getUserAgent(null);
        String from = this.getFrom(null);
        if (!(userAgent.matches(ACCEPTABLE_USER_AGENT)
            && from.matches(ACCEPTABLE_FROM))) {
            throw new FatalConfigurationException("unacceptable user-agent or from");
        }
    }

    /**
     * @return
     */
    public File getCheckpointsDirectory() {
        try {
            return getDirectoryRelativeToDiskPath((String) getAttribute(null, CrawlOrder.ATTR_CHECKPOINTS_PATH));
        } catch (AttributeNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    private File getDirectoryRelativeToDiskPath(String subpath) {
        File disk;
        try {
            disk = getSettingsHandler().getPathRelativeToWorkingDirectory(
                    (String) getAttribute(null, CrawlOrder.ATTR_DISK_PATH));
            return new File(disk, subpath);
        } catch (AttributeNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }
}
