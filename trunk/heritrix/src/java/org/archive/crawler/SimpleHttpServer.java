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
package org.archive.crawler;

import java.io.File;
import org.mortbay.http.*;
import org.mortbay.jetty.Server;


/**
 * Wrapper for embedded Jetty server.
 * 
 * Loads up all webapps under webapp directory.
 * 
 */
public class SimpleHttpServer 
{   
    private int port;
    private Server server = null;
    
    /**
     * Default web port.
     */
    public static final int DEFAULT_PORT = 8080;
    
    /**
     * Name of the admin webapp.
     */
    private static final String ADMIN_WEBAPP_NAME = "admin";
    

    public SimpleHttpServer() throws Exception
    {
        this(DEFAULT_PORT);
    }

    public SimpleHttpServer(int port) throws Exception
    {
        this.server = new Server();
        this.port = port;
        SocketListener listener = new SocketListener();
        listener.setPort(port);
        server.addListener(listener);
        server.setRootWebApp("root");
        server.addWebApplications(null, getWebappsPath(), true);
    }
    
    /**
     * Return the webapp path.
     * 
     * Looks at system properties to see if default has been overridden.
     * 
     * @return Return webapp path (Path returned has a trailing '/').
     */
    private static String getWebappsPath()
    {
        String webappsPath = Heritrix.getWebappsdir().getAbsolutePath();
        if (!webappsPath.endsWith(File.separator))
        {
            webappsPath = webappsPath + File.separator;
        }
        return webappsPath;
    }

    /**
     * Return the admin webapp path.
     * 
     * Looks at system properties to see if default has been overridden.  This
     * method is used by CrawlJobHandler also.
     * 
     * @return Return admin webapp path (Path returned has a trailing '/').
     */
    public static String getAdminWebappPath()
    {
        return getWebappsPath() + ADMIN_WEBAPP_NAME + File.separator;
    }
    
    /**
     * Start the server.
     * 
     * @throws Exception
     */
    public void startServer()
        throws Exception
    {
        this.server.start();
    }
    
    /**
     * Stop the running server.
     * 
     * @throws Exception
     */
    public void stopServer()
        throws Exception
    {
        if (this.server != null)
        {
            this.server.stop();
        }
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#finalize()
     */
    protected void finalize()
        throws Throwable
    {
        stopServer();
        super.finalize();
    }

    /**
     * @return Port server is running on.
     */
    public int getPort()
    {
        return this.port;
    }

    /**
     * @return Server reference.
     */
    public HttpServer getServer()
    {
        return this.server;
    }
}
