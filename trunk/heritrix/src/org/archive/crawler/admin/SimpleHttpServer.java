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
 * Created on Jul 11, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.archive.crawler.admin;

import java.io.IOException;

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
	public static final int DEFAULT_PORT = 8080;
	private static String _contextPath = "/";
	private static String _resourceBase = "./WebUIResources/";
	private HttpContext _context = new HttpContext();
	public SimpleHttpServer() throws Exception {
		initialize(DEFAULT_PORT);
	}
	public SimpleHttpServer(int port) throws Exception {
		initialize(port);
	}

	private void initialize(int port) throws IOException {
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
