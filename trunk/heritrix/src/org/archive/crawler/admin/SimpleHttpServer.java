/*
 * Created on Jul 11, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.archive.crawler.admin;

import org.mortbay.http.*;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.http.handler.*;
import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.framework.CrawlController;

/**
 * @author Administrator
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class SimpleHttpServer {

	private int _port;
	private HttpServer _server = null;
	private static int DEFAULT_PORT = 8080;
	private static String _contextPath = "/";
	private static String _resourceBase = "./WebUIResources/";
	private HttpContext _context = new HttpContext();
	private CrawlController _controller = new CrawlController();
	private CrawlOrder _order;
	public SimpleHttpServer() {
		initialize(DEFAULT_PORT);
	}
	public SimpleHttpServer(int port) {
		initialize(port);
	}

	private void initialize(int port) {
		if (_server == null) {
			// Start the crawl
	
			_server = new HttpServer();
			SocketListener listener = new SocketListener();
			listener.setPort(port);
			_server.addListener(listener);

			_context.setContextPath(_contextPath);
			_context.setResourceBase(_resourceBase);
			_context.addHandler(new ResourceHandler());
			_server.addContext(_context);

		}
	}

	public void startServer() throws Exception{
		// Add Crawler Handler
		CrawlerHandler handler = new CrawlerHandler(_controller);
		_context.addHandler(handler);
		_server.start();
	}
	public void stopServer() throws Exception {
		_server.stop();
	}
	/**
	 * @return
	 */
	public int getPort() {
		return _port;
	}

	/**
	 * @return
	 */
	public HttpServer getServer() {
		return _server;
	}

	/**
	 * @return
	 */
	public CrawlController getController() {
		return _controller;
	}
	/**
	 * 
	 */
	public void setControler(CrawlController c){
		_controller = c;
	}
	/**
	 * 
	 */
	public void setOrder(CrawlOrder o) {
		_order = o;
	}

}
