/* 
 * SimpleHttpServer
 * 
 * $Id$
 *
 * Created on Jul 11, 2003
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

import java.io.File;
import java.io.IOException;

import org.mortbay.http.*;
import org.mortbay.jetty.Server;


/**
 * Wrapper for embedded Jetty server.
 */
public class SimpleHttpServer 
{
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

    private int _port;
    private Server _server = null;
    public static final int DEFAULT_PORT = 8080;


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
            _server.addWebApplication(ADMIN_WEBAPP_NAME, 
                    getAdminWebappPath());
        }
    }

    /**
     * Return the admin webapp path.
     * 
     * Looks at system properties to see if default has been overridden.  This
     * method is used by CrawlJobHandler also.
     * 
     * @return Return admin webapp path (Path returned has a trailing '/').
     */
    protected static String getAdminWebappPath()
    {
        String webappPath = System.getProperty(WEBAPP_PATH_NAME);
        if (webappPath == null) {
            webappPath = DEFAULT_WEBAPP_PATH;
        }
        if (!webappPath.endsWith(File.separator)) {
            webappPath = webappPath + File.separator;
        }
        return webappPath + ADMIN_WEBAPP_NAME + File.separator;
    }
    
    /**
     * Start the server.
     * 
     * @throws Exception
     */
    public void startServer() throws Exception {
        _server.start();
    }
    
    /**
     * Stop the running server.
     * 
     * @throws Exception
     */
    public void stopServer() throws Exception {
        _server.stop();
    }

    /**
     * @return Port server is running on.
     */
    public int getPort() {
        return _port;
    }

    /**
     * @return Server reference.
     */
    public HttpServer getServer() {
        return _server;
    }
}
