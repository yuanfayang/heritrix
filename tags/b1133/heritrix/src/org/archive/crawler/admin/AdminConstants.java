/*
 * Created on Oct 1, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.archive.crawler.admin;

/**
 * @author Administrator
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
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
	public static final String WEB_APP_PATH = "WebUIResources/webapps/admin/";
	public static final String DEFAULT_ORDER_FILE = "order.xml";
	public static final String CRAWLING_IN_PROGRESS = "Crawling In Progress";
	public static final String NO_CRAWLING = "No Crawling in Progress";
	public static final String NO_ACTIVE_THREADS = "All threads are idle";
	public static final String CRAWL_ORDER_UPDATED = "Crawl Order Updated";
	
	// XPaths
	// From CrawlOrder
	public static final String XP_CRAWL_ORDER_NAME = "//crawl-order/@name";
	public static final String XP_HTTP_USER_AGENT = "//http-headers/@User-Agent";
	public static final String XP_HTTP_FROM = "//http-headers/@From";
	public static final String XP_MAX_TOE_THREADS = "//behavior/@max-toe-threads";
	public static final String XP_ROBOTS_HONORING_POLICY_NAME = "//behavior/robots-honoring-policy/@name";
	public static final String XP_ROBOTS_HONORING_POLICY_MASQUERADE = "//behavior/robots-honoring-policy/@masquerade";
	public static final String XP_ROBOTS_HONORING_POLICY_CUSTOM_ROBOTS = "//behavior/robots-honoring-policy/custom-robots";
	public static final String XP_ROBOTS_HONORING_POLICY_USER_AGENTS = "//behavior/robots-honoring-policy/user-agents/agent";
	// From CrawlController
	public static final String XP_STATS_LEVEL = "//loggers/crawl-statistics/@level";
	public static final String XP_STATS_INTERVAL = "//loggers/crawl-statistics/@interval-seconds";
	public static final String XP_DISK_PATH = "//behavior/@disk-path";
	public static final String XP_PROCESSORS = "//behavior/processors/processor";
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
	
	public static final String XP_CRAWL_COMMENT = "//crawl-order/@comment";
	public static final String XP_SEEDS = "//seeds";
	public static final String XP_SEEDS_FILE = "//seeds/@src";
}
