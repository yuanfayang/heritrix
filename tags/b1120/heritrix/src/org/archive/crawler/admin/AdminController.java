package org.archive.crawler.admin;

/*
 * Created on Sep 16, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.exceptions.InitializationException;
import org.archive.util.ArchiveUtils;
import org.w3c.dom.Node;

/**
 * @author Administrator
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class AdminController extends HttpServlet implements AdminConstants {

	private String orderFile;
	private boolean crawling = false;
	private String statusMessage = "Welcome";
	private int crawlerAction = -1;
	private String diskPath;
	private String workingDirectory;
	private Node orderNode;
	private CrawlController controller;
	private OrderTransformation orderTransform;

	public AdminController() {
		// Default order file;
		orderFile = WEB_APP_PATH + DEFAULT_ORDER_FILE;

		String aOrderFile = System.getProperty("OrderFile");
		if (aOrderFile != null) {
			File f = new File(aOrderFile);
			if (!f.isAbsolute()) {
				orderFile =
					System.getProperty("user.dir")
						+ File.separator
						+ aOrderFile;
			} else {
				orderFile = aOrderFile;
			}
		}

		controller = new CrawlController();
		orderTransform = new OrderTransformation();

		try {
			orderNode = OrderTransformation.readDocumentFromFile(orderFile);
			orderTransform.setNode(orderNode);
			orderTransform.transformXMLtoHTML(
				WEB_APP_PATH + ORDER_HTML_FORM_PAGE,
				orderFile,
				WEB_APP_PATH + XSL_STYLE,
				ArchiveUtils.getFilePath(orderFile));
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace(System.out);
		}
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response)
		throws IOException, ServletException {
		int crawlerAction =
			Integer.valueOf(request.getParameter("CrawlerAction")).intValue();

		if (crawling) {
			setStatusMessage(CRAWLING_IN_PROGRESS);
		} else {
			setStatusMessage(NO_CRAWLING);
		}

		if (crawling && controller.getActiveToeCount() == 0) {
			setStatusMessage(NO_ACTIVE_THREADS);
		}
		switch (crawlerAction) {
			case UPDATEWM :
				File f = new File(controller.getOrder().getDefaultFileLocation() + File.separator + controller.getOrder().getStringAt("//disk/@path"));
				Runtime.getRuntime().exec("/home/igor/tools/wuiUpdateWM.sh " + f.getAbsolutePath());
				setStatusMessage("Wayback Machine Update Started");
				request.setAttribute("message", getStatusMessage());
				forwardToJsp(MAINMENU_JSP, request, response);
			break;
			case UPDATE :
				updateCrawlOrder(request);
				// Go to ORDER to show new order HTML page
			case ORDER :
				request.setAttribute("message", statusMessage);
				forwardToJsp(CRAWL_ORDER_JSP, request, response);
				break;
			case START :
				if (crawling) {
					setStatusMessage(CRAWLER_RUNNING_ERR);
				} else {
					startCrawling();
				}
				request.setAttribute("message", statusMessage);
				forwardToJsp(MAINMENU_JSP, request, response);
				break;
			case STOP :
				if (!crawling) {
					setStatusMessage(CRAWLER_NOT_RUNNING_ERR);
				} else {
					stopCrawling();
				}
				request.setAttribute("message", statusMessage);
				forwardToJsp(MAINMENU_JSP, request, response);
				break;
			case STATS :
				if (controller.getStatistics() == null) {
					request.setAttribute(
						"message",
						"No Stats Availabe (no crawling in progress)");
					forwardToJsp(MAINMENU_JSP, request, response);
					return;
				}
				showStats(request);
				request.setAttribute("DiskPath", diskPath);
				request.setAttribute("message", statusMessage);
				forwardToJsp(STATS_JSP, request, response);
				break;
			case TERMINATE :
				terminateAdminServer();
				break;
			case MAINMENU :
				request.setAttribute("message", statusMessage);
				forwardToJsp(MAINMENU_JSP, request, response);
				break;
			default :
				return;

		}
	}
	public void startCrawling() {
		try {
			CrawlOrder order = CrawlOrder.readFromFile(orderFile);
			controller.initialize(order);
		} catch (InitializationException e) {
			//TODO Report Error
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		controller.startCrawl();
		setCrawling(true);
		statusMessage = CRAWLER_STARTED;
		diskPath =
			controller.getOrder().getNodeAt("//disk/@path").getNodeValue();
	}

	public void stopCrawling() {
		controller.stopCrawl();
		statusMessage = CRAWLER_STOPPED;
		setCrawling(false);

	}
	public void terminateAdminServer() {
		Runtime.getRuntime().exit(0);
	}
	public boolean isCrawling() {
		return crawling;
	}

	public String getStatusMessage() {
		return statusMessage;
	}

	public void setCrawling(boolean b) {
		crawling = b;
	}

	public void updateCrawlOrder(HttpServletRequest req) {
		Enumeration it = req.getParameterNames();
		String name;
		String seedsFileName = null;
		String seeds = null;
		while (it.hasMoreElements()) {
			name = it.nextElement().toString();
			String value = req.getParameter(name);
			if (name.equals("seeds")) {
				seeds = value;
			} else {
				if (name != null && value != null) {
					if (name.equals("//selector/seeds/@src")) {
						seedsFileName = value;
					}
					orderTransform.setNodeValue(name, value);
				}
			}
		}
		if (seeds != null && seedsFileName != null) {
			try {
				BufferedWriter writer =
					new BufferedWriter(
						new FileWriter(ArchiveUtils.getFilePath(orderFile) + seedsFileName));
				if (writer != null) {
					writer.write(seeds);
					writer.close();
				}
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
		}
		orderFile = WEB_APP_PATH + "order.tmp.xml";
		orderTransform.serializeToXMLFile(orderFile);
		orderTransform.transformXMLtoHTML(
			WEB_APP_PATH + ORDER_HTML_FORM_PAGE,
			orderFile,
			WEB_APP_PATH + XSL_STYLE,
			ArchiveUtils.getFilePath(orderFile));
		setStatusMessage(CRAWL_ORDER_UPDATED);
	}

	public void showStats(HttpServletRequest req) {
		StatisticsTracker stats = controller.getStatistics();
		req.setAttribute(
			"ThreadCount",
			String.valueOf(controller.getToeCount()));
		req.setAttribute(
			"ActiveThreadCount",
			String.valueOf(stats.activeThreadCount()));
		req.setAttribute(
			"TotalBytesWritten",
			String.valueOf(stats.getTotalBytesWritten()));
		req.setAttribute(
			"UriFetchSuccessCount",
			String.valueOf(stats.uriFetchSuccessCount()));
		req.setAttribute(
			"UrisEncounteredCount",
			String.valueOf(stats.urisEncounteredCount()));
	}
	public void forwardToJsp(
		String aJspFile,
		HttpServletRequest req,
		HttpServletResponse res) {
		try {
			getServletConfig().getServletContext().getRequestDispatcher(
				aJspFile).forward(
				req,
				res);
		} catch (Exception e) {
			// TODO: Report Error
			e.printStackTrace();
		}
	}
	public void setStatusMessage(String string) {
		statusMessage = string;
	}
}
