/*
 * Created on Jul 11, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.archive.crawler.admin;

import org.mortbay.http.*;
import org.mortbay.jetty.Server;

/**
 * @author Administrator
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class SimpleHttpServer {

	private int _port;
	private Server _server = null;
	private static int DEFAULT_PORT = 8080;
	private static String _contextPath = "/";
	private static String _resourceBase = "./WebUIResources/";
	private HttpContext _context = new HttpContext();
	public SimpleHttpServer() throws Exception {
		initialize(DEFAULT_PORT);
	}
	public SimpleHttpServer(int port) throws Exception {
		initialize(port);
	}

	private void initialize(int port) throws Exception {
		if (_server == null) {
			_server = new Server();
			SocketListener listener = new SocketListener();
			listener.setPort(port);
			_server.addListener(listener);
			_server.addWebApplication("admin", "WebUIResources/webapps/admin/");
		}
	}

	public void startServer() throws Exception {
		_server.start();
	}
	public void stopServer() throws Exception {
		_server.stop();
	}

	public int getPort() {
		return _port;
	}

	public HttpServer getServer() {
		return _server;
	}

}
