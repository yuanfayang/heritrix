/*
 * Created on Jul 11, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.archive.crawler.admin;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.mortbay.http.HttpException;
import org.mortbay.http.HttpFields;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.http.handler.AbstractHttpHandler;
import org.mortbay.util.StringUtil;

import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.framework.CrawlController;

import sun.security.provider.certpath.CollectionCertStore;

/**
 * @author Administrator
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class CrawlerHandler extends AbstractHttpHandler {

	private static final int SHOW = 0;
	private static final int STOP = 1;
	private static final int UPDATE = 2;
	private static final int START = 3;
	private static final int STATS = 4;

	private String _start = "/startCrawler";
	private String _stop = "/stopCrawler";
	private String _init = "/initCrawler";
	private String _show = "/showOrder";
	private boolean _crawling = false;
	private CrawlController _controller;
	private int _crawlerAction = -1;
	public CrawlerHandler(CrawlController c) {
		super();
		_controller = c;
	}

	public void handle(
		String pathInContext,
		String pathParams,
		HttpRequest request,
		HttpResponse response)
		throws HttpException, IOException {
		{
			if (!isStarted())
				return;
			// Only handle GET, HEAD and POST
			if (!HttpRequest.__GET.equals(request.getMethod())
				&& !HttpRequest.__HEAD.equals(request.getMethod())
				&& !HttpRequest.__POST.equals(request.getMethod()))
				return;

			_crawlerAction =
				Integer
					.valueOf(request.getParameter("CrawlerAction"))
					.intValue();
			switch (_crawlerAction) {
				case STOP :
					outputMessage(
						"===>Crawling stoped!<===\nShutting Down the Server!",
						response);
					_controller.stopCrawl();
					_crawling = false;
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					Runtime.getRuntime().exit(0);
					break;
				case UPDATE :
					updateCrawlerOrder(request);
				case SHOW :
					outputCrawlOrder(_controller.getOrder(), response);
					break;
				case START :
					if (_crawling)
						outputMessage(
							"ERROR: Couldn't start a new crawl (crawler already running)",
							response);
					_controller.initialize(_controller.getOrder());
					outputMessage("Crawling Started", response);
					_controller.startCrawl();
					_crawling = true;
					break;
				case STATS :
					outputCrawlerStats(response);
					break;
				default :
					return;

			}
			request.setHandled(true);
		}
	}

	private void outputCrawlerStats(HttpResponse r)
		throws HttpException, IOException {

		r.setField(HttpFields.__ContentType, HttpFields.__TextHtml);
		OutputStream out = r.getOutputStream();
		ByteArrayOutputStream buf = new ByteArrayOutputStream(2048);
		Writer writer = new OutputStreamWriter(buf, StringUtil.__ISO_8859_1);
		StringBuffer sb = new StringBuffer(32768);
		sb.append("<html>\n<head>\n<title>Heritrix WUI</title>\n");
		if (_controller.getStatistics().activeThreadCount() != 0)
			sb.append(
				"<META HTTP-EQUIV=Refresh CONTENT=\"3\" URL=\"CrawlAction?CrawlAction=\"4\"\n");
		sb.append("</head>\n");
		sb.append(genPageHeader());
		sb.append("<br>Total Threads: ");
		sb.append(_controller.getStatistics().threadCount());
		sb.append("<br>Total Active Threads: <font color=\"red\">");
		sb.append(_controller.getStatistics().activeThreadCount());
		sb.append("</font>");
		sb.append(
			genProgressBar(
				"Downloaded/Discovered Document Ration",
				_controller.getStatistics().uriFetchSuccessCount(),
				_controller.getStatistics().urisEncounteredCount(),
				"darkorange",
				"lightblue"));
		Object objArray[] =
			_controller
				.getStatistics()
				.getFileDistribution()
				.values()
				.toArray();
		java.util.Arrays.sort(objArray);

		Iterator keys =
			_controller
				.getStatistics()
				.getFileDistribution()
				.keySet()
				.iterator();
		Iterator values =
			_controller
				.getStatistics()
				.getFileDistribution()
				.values()
				.iterator();
			List l = new ArrayList();
		while (values.hasNext()) {
			sb.append(
				genProgressBar(
					"Total Mime Type:" + keys.next().toString(),
					((Integer) values.next()).intValue(),
					_controller.getStatistics().urisEncounteredCount(),
					"darkorange",
					"lightblue"));
		}
		sb.append("<br>");
		sb.append(genNavigationLinks());
		sb.append(genPageEnd());
		writer.write(sb.toString());
		writer.flush();
		buf.writeTo(out);
		out.flush();
	}
	private void outputCrawlOrder(CrawlOrder o, HttpResponse r)
		throws HttpException, IOException {

		r.setField(HttpFields.__ContentType, HttpFields.__TextHtml);
		OutputStream out = r.getOutputStream();
		ByteArrayOutputStream buf = new ByteArrayOutputStream(2048);
		Writer writer = new OutputStreamWriter(buf, StringUtil.__ISO_8859_1);
		writer.write(genPageStart());
		writer.write(genPageHeader());
		writer.write("<h2>Crawl Order</h2>");
		writer.write("<FORM ACTION=\"CrawlerAction\" METHOD=GET>\n");
		writer.write(
			genHtmlTextField("/crawl-order/@name", "CRAWL NAME", o.getName()));
		//		writer.write(
		//			genHtmlTextField(
		//				"//crawl-order/comment",
		//				"COMMENT",
		//				o.getStringAt("//crawl-order/comment/")));
		writer.write(
			genHtmlTextField(
				"//http-headers/User-Agent",
				"USER AGENT",
				o.getBehavior().getUserAgent()));
		writer.write(
			genHtmlTextField(
				"//http-headers/From",
				" FROM",
				o.getBehavior().getFrom()));
		writer.write(
			genHtmlTextField(
				"//limits/max-toe-threads/@value",
				"MAX NUMBER OF THREADS",
				String.valueOf(o.getBehavior().getMaxToes())));
		writer.write(
			genHtmlTextField(
				"//limits/max-link-depth/@value",
				"MAX LINK DEPTH",
				String.valueOf(o.getBehavior().getMaxLinkDepth())));
		writer.write(
			genHtmlTextField(
				"/crawl-order/arc-file/@prefix",
				"ARC FILE PREFIX",
				o.getNodeAt("/crawl-order/arc-file/@prefix").getNodeValue()));
		writer.write(
			genHtmlTextField(
				"//processors/processor/arc-files/@max-size-bytes",
				"MAX ARC FILE SIZE",
				o
					.getNodeAt("//processors/processor/arc-files/@max-size-bytes")
					.getNodeValue()));
		writer.write(
			genHtmlTextField(
				"//disk/@path",
				"DISK PATH",
				o.getNodeAt("//disk/@path").getNodeValue()));
		writer.write(
			genHtmlTextField(
				"//processors/processor/compression/@use",
				"COMPRESS ARC FILES",
				o
					.getNodeAt("//processors/processor/compression/@use")
					.getNodeValue()));

		writer.write(
			genHtmlTextField(
				"//selector/seeds/@src",
				"SEEDS FILE",
				o.getBehavior().getStringAt("//selector/seeds/@src")));

		writer.write(
			genHtmlTextArea("seed-urls", "", o.getBehavior().getSeeds()));
		writer.write(
			"<INPUT type=hidden name=CrawlerAction value=2>\n<br><INPUT TYPE=submit VALUE=\"UpdateOrder\">\n</FORM>");
		writer.write(
			"<FORM ACTION=\"Crawleraction\" METHOD=GET>\n<INPUT type=hidden name=CrawlerAction value=3>\n<br><INPUT TYPE=submit VALUE=\" StartCrawler \">\n</FROM>");
		writer.write(genPageEnd());
		writer.flush();
		buf.writeTo(out);
		out.flush();
	}
	private String genHtmlTextField(
		String name,
		String description,
		String value) {
		return "<input type=\"text\" size=\"48\" value=\""
			+ value
			+ "\" name=\""
			+ name
			+ "\">:<STRONG>"
			+ description
			+ "</STRONG><BR>\n";
	}

	private String genHtmlTextArea(
		String name,
		String description,
		List items) {
		Iterator it = items.iterator();
		StringBuffer sb = new StringBuffer();
		sb.append("<textarea rows=\"8\" cols=\"64\" name=\"" + name + "\">\n");
		while (it.hasNext()) {
			sb.append(((UURI) it.next()).toExternalForm() + "\n");
		}
		sb.append("</textarea>\n");
		return sb.toString();
	}

	private void updateCrawlerOrder(HttpRequest req) {
		Iterator it = req.getParameterNames().iterator();
		String name;
		_controller.getOrder().clearCaches();
		_controller.getOrder().getBehavior().clearCaches();

		while (it.hasNext()) {
			name = it.next().toString();
			String value = req.getParameter(name);
			if (name.equals("seed-urls")) {
				String[] urls = value.split("\n");
				_controller.getOrder().getBehavior().clearSeeds();
				for (int i = 0; i < urls.length; i++)
					_controller.getOrder().getBehavior().addSeed(urls[i]);
			} else {
				if (_controller.getOrder().getNodeAt(name) != null) {
					_controller.getOrder().getNodeAt(name).setNodeValue(value);

				}
			}
		}
	}

	private String genNavigationLinks() {
		StringBuffer sb = new StringBuffer();
		sb.append(
			"<center><a href=\"./ShowCrawler?CrawlerAction=0\">Show Crawler's Order</a>&nbsp | \n");
		sb.append(
			"<a href=\"./StartCrawler?CrawlerAction=3\">Start Crawler</a>&nbsp |\n");
		sb.append(
			"<a href=\"./StopCrawler?CrawlerAction=1\">Stop Crawler</a>&nbsp |\n");
		sb.append(
			"<a href=\"./StatCrawler?CrawlerAction=4\">Crawler's Stats</a>&nbsp</center>\n");
		return sb.toString();
	}
	private String genPageHeader() {
		StringBuffer sb = new StringBuffer();
		sb.append(
			"<body>\n<table cellspacing=\"0\" width=\"100%\" bgcolor=\"white\" border=\"0\" cellpadding=\"0\">\n<tbody>");
		sb.append(
			"<tr>\n<td width=\"110\" height=\"72\" valign=\"bottom\" bgcolor=\"#000000\">\n");
		sb.append(
			"<img border=\"0\" src=\"./images/logo.jpg\" width=\"86\" heigth=\"72\"></a>\n");
		sb.append(
			"</td>\n<td valign=\"bottom\" background=\"./images/blendbar.jpg\">\n");
		sb.append(
			"<font color=\"white\">\n<h2>Internet Archive Open Source Web Crawler</h2>\n</font>");
		sb.append("</td>\n</tr>\n</tbody>\n</table>\n");
		return sb.toString();
	}

	private String genPageStart() {
		return "<html>\n<head>\n<title>Heritrix WUI</title>\n</head>\n";
	}
	private String genPageEnd() {
		return "</BODY>\n</HTML>\n";
	}

	public String genProgressBar(
		String description,
		int start,
		int end,
		String color1,
		String color2) {

		int ratio = (int) (100 * start / end);

		StringBuffer sb = new StringBuffer();
		sb.append(
			"<left><table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" bgcolor=\"white\"> <tr><td align=\"center\" colspan=\"200\">");
		sb.append("</td></tr>\n<tr>\n<td>");
		sb.append("<td bgcolor=\"");
		sb.append(color1);
		sb.append("\" colspan=\"2\"><strong>");
		sb.append(ratio);
		sb.append("</strong>%</td>\n");
		for (int i = 1; i < ratio; i++) {
			if ((i % 4) == 0)
				sb.append("\n");
			sb.append("<td bgcolor=\"");
			sb.append(color1);
			sb.append("\"> </td>");
		}
		for (int i = ratio; i < 100; i++) {
			if ((i % 4) == 0)
				sb.append("\n");
			sb.append("<td bgcolor=\"");
			sb.append(color2);
			sb.append("\"> </td>");
		}
		sb.append("<td bgcolor=\"");
		if (ratio == 100)
			sb.append(color1);
		else
			sb.append(color2);
		sb.append("\" colspan=\"3\"><strong>100%</strong></td><td>: \n");
		sb.append(description);
		sb.append(" (");
		sb.append(start);
		sb.append(" of ");
		sb.append(end);
		sb.append(")</td>\n");

		sb.append("</tr></table></left>");

		return sb.toString();
	}

	private void outputMessage(String message, HttpResponse r)
		throws HttpException, IOException {
		r.setField(HttpFields.__ContentType, HttpFields.__TextHtml);
		OutputStream out = r.getOutputStream();
		ByteArrayOutputStream buf = new ByteArrayOutputStream(2048);
		Writer writer = new OutputStreamWriter(buf, StringUtil.__ISO_8859_1);
		writer.write(genPageStart());
		writer.write(genPageHeader());
		writer.write(
			"<center><FONT size=\"14\" color=\"red\">"
				+ message
				+ "</FONT></center>\n");
		writer.write(genNavigationLinks());
		writer.write(genPageEnd());
		writer.flush();
		buf.writeTo(out);
		out.flush();
	}
}