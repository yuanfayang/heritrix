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
	public static final int ORDER = 0;
	public static final int STOP = 1;
	public static final int UPDATE = 2;
	public static final int START = 3;
	public static final int STATS = 4;
	public static final int TERMINATE = 5;
	public static final int MAINMENU = 6;
	public static final int UPDATEWM = 7;
	public static final String CRAWLER_STARTED = "Crawler Started!";
	public static final String CRAWLER_STOPPED = "Crawler Stopped!";
	public static final String CRAWLER_RUNNING_ERR =
		"ERROR: Couldn't start a new crawl (crawler is already running)";
	public static final String CRAWLER_NOT_RUNNING_ERR =
	"ERROR: Couldn't stop crawling (no running crawler was found)";
	public static final String CRAWLER_TERMINATED =
		"===>Crawling Stopped!<===<br>\nShutting Down the Server!";
	public static final String WEB_APP_PATH = "WebUIResources/webapps/admin/";
	public static final String XSL_STYLE = "order-conf.xsl";
	public static final String MAINMENU_JSP = "mainmenu.jsp";
	public static final String CRAWL_ORDER_JSP = "crawlorder.jsp";
	public static final String ORDER_HTML_FORM_PAGE = "orderform.html";
	public static final String STATS_JSP = "stats.jsp";
	public static final String DEFAULT_ORDER_FILE = "order.xml";
	public static final String CRAWLING_IN_PROGRESS = "Crawling In Progress";
	public static final String NO_CRAWLING = "No Crawling in Progress";
	public static final String NO_ACTIVE_THREADS = "All threads are idle";
	public static final String CRAWL_ORDER_UPDATED = "Crawl Order Updated";
}
