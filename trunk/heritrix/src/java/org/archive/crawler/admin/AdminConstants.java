/* 
 * AdminConstants
 * 
 * $Id$
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
 */
package org.archive.crawler.admin;

/**
 * An aggregate of constants related to crawler admin.
 * <p>
 * The concept behind this interface is that implmenting classes have quick
 * access to these constants.
 * <p>
 * Most of the constants are XPaths in the configuration DOM.  Used at least by
 * the crawler web interface.
 */
public interface AdminConstants {
    public static final String CRAWLER_STARTED = "Crawler Started!";
    public static final String CRAWLER_STOPPED = "Crawler Stopped!";
    public static final String CRAWLER_RUNNING_ERR =
        "ERROR: Couldn't start a new crawl (crawler is already running)";
    public static final String CRAWLER_NOT_RUNNING_ERR =
    "ERROR: Couldn't stop crawling (no running crawler was found)";
    public static final String CRAWLER_TERMINATED =
        "===>Crawling Stopped!<===<br>\nShutting Down the Server!";
    public static final String CRAWLING_IN_PROGRESS = "Crawling In Progress";
    public static final String NO_CRAWLING = "No Crawling in Progress";
    public static final String NO_ACTIVE_THREADS = "All threads are idle";
    public static final String CRAWL_ORDER_UPDATED = "Crawl Order Updated";
  
    /**
     *  Default webapp path.
     */
    public static final String DEFAULT_WEBAPP_PATH = "webapps";
    
    /**
     * Name of system property whose specification overrides
     * DEFAULT_WEBAPP_PATH.
     */
    public static final String WEBAPP_PATH_NAME = "heritrix.webapp.path";

    /**
     * Default name of admin webapp.
     */
    public static final String ADMIN_WEBAPP_NAME = "admin";
    
    /**
     * Name of system property whose specification overrides default order file
     * used.
     * 
     * Default is WEBAPP_PATH + ADMIN_WEBAPP_NAME 
     * + DEFAULT_ORDER_FILE.  Pass an absolute or relative path.
     */
    public static final String DEFAULT_ORDER_FILE_NAME 
        = "heritrix.default.orderfile";
    
    /**
     * Default order file name.
     */
    public static final String DEFAULT_ORDER_FILE = "order.xml";
    
    // XPaths
    // From CrawlOrder
    public static final String XP_CRAWL_ORDER_NAME = "//crawl-order/@name";
    public static final String XP_HTTP_USER_AGENT 
        = "//http-headers/@User-Agent";
    public static final String XP_HTTP_FROM = "//http-headers/@From";
    public static final String XP_MAX_TOE_THREADS 
        = "//behavior/@max-toe-threads";
    public static final String XP_ROBOTS_HONORING_POLICY_NAME 
        = "//behavior/robots-honoring-policy/@name";
    public static final String XP_ROBOTS_HONORING_POLICY_MASQUERADE 
        = "//behavior/robots-honoring-policy/@masquerade";
    public static final String 
        XP_ROBOTS_HONORING_POLICY_CUSTOM_ROBOTS 
            = "//behavior/robots-honoring-policy/custom-robots";
    public static final String XP_ROBOTS_HONORING_POLICY_USER_AGENTS 
        = "//behavior/robots-honoring-policy/user-agents/agent";
    // From CrawlController
    public static final String XP_STATS_LEVEL 
        = "//loggers/crawl-statistics/@level";
    public static final String XP_STATS_INTERVAL 
        = "//loggers/crawl-statistics/@interval-seconds";
    public static final String XP_DISK_PATH = "//behavior/@disk-path";
    public static final String XP_PROCESSORS 
        = "//behavior/processors/processor";
    public static final String XP_FRONTIER = "//behavior/frontier";
    public static final String XP_CRAWL_SCOPE = "//scope";
    // From Frontier
    public static final String XP_DELAY_FACTOR = "@delay-factor";
    public static final String XP_MIN_DELAY = "@min-delay-ms";
    public static final String XP_MAX_DELAY = "@max-delay-ms";
    
    // Custom    
    public static final String XP_MAX_TRANS_HOPS = "//scope/@max-trans-hops";
    public static final String XP_MAX_LINK_HOPS = "//scope/@max-link-hops";
    public static final String XP_CRAWL_MODE = "//scope/@mode";
    
    public static final String XP_ARC_PREFIX = "//@prefix";
    public static final String XP_ARC_COMPRESSION_IN_USE = "//@compress";
    public static final String XP_MAX_ARC_SIZE = "//@max-arc-size";
    public static final String XP_ARC_DUMP_PATH = "//processor/@path";
    
    public static final String XP_CRAWL_COMMENT = "//crawl-order/@comment";
    public static final String XP_SEEDS = "//seeds";
    public static final String XP_SEEDS_FILE = "//seeds/@src";
    
    public static final String XP_POLITENESS_DELAY_FACTOR 
        = "//frontier/@delay-factor";
    public static final String XP_POLITENESS_MIN_DELAY 
        = "//frontier/@min-delay-ms";
    public static final String XP_POLITENESS_MAX_DELAY 
        = "//frontier/@max-delay-ms";
    public static final String XP_POLITENESS_MIN_INTERVAL 
        = "//frontier/@min-interval-ms";

    public static final String XP_HTTPFETCH_MAX_FETCH_ATTEMPTS 
        = "//processor/@max-fetch-attempts";
    public static final String XP_HTTPFETCH_MAX_LENGTH_BYTES 
        = "//processor/@max-length-bytes";
    public static final String XP_HTTPFETCH_SOTIMEOUT 
        = "//processor/@sotimeout-ms";
    public static final String XP_HTTPFETCH_TIMEOUT 
        = "//processor/@timeout-seconds";
        
	public static final String XP_MAX_BYTES_DOWNLOAD = "//behavior/@max-bytes-download";
	public static final String XP_MAX_DOCUMENT_DOWNLOAD = "//behavior/@max-document-download";
	public static final String XP_MAX_TIME = "//behavior/@max-time-sec";}