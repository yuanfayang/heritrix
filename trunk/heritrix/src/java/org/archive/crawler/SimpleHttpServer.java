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
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.mortbay.http.DigestAuthenticator;
import org.mortbay.http.HttpServer;
import org.mortbay.http.NCSARequestLog;
import org.mortbay.http.SocketListener;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.WebApplicationContext;


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
     * Webapp contexts returned out of a server start.
     */
    private List contexts = new ArrayList();
    
    /**
     * Name of the root webapp.
     */
    private static final String ROOT_WEBAPP = "root";
    
    /**
     * Name of the admin webapp.
     */
    private static final String ADMIN_WEBAPP = "admin";
    
    /**
     * List of webapps to deploy.
     */
    private static final List webapps =
        Arrays.asList(new String [] {ROOT_WEBAPP, ADMIN_WEBAPP});


    public SimpleHttpServer() throws Exception {
        
        this(DEFAULT_PORT);
    }
    
    public SimpleHttpServer(int port) throws Exception {
        
        this(SimpleHttpServer.webapps, port);
    }

    public SimpleHttpServer(List webapps, int port) throws Exception {
        
        this.server = new Server();
        this.port = port;
        SocketListener listener = new SocketListener();
        listener.setPort(port);
        // Bumpt UI thread priority up 1 above the normal
        listener.setThreadsPriority(Thread.NORM_PRIORITY);
        this.server.addListener(listener);
        // Add each of the webapps in turn. If we're passed the root webapp,
        // give it special handling -- assume its meant to be server root and
        // its meant to be mounted on '/'.  The below also favors the war file
        // if its present.
        for (Iterator i = webapps.iterator(); i.hasNext();) {
            String name = (String)i.next();
            File ptr = new File(getWARSPath(), name + ".war");
            if (!ptr.exists()) {
                ptr = new File(getWARSPath(), name);
                if (!ptr.exists()) {
                    throw new FileNotFoundException(ptr.getAbsolutePath());
                }
            }
            // If webapp name is for root, mount it on '/', else '/WEBAPP_NAME'.
            String context = "/" + ((name.equals(ROOT_WEBAPP))? "": name);
            WebApplicationContext c =
                this.server. addWebApplication(context, ptr.getAbsolutePath());
            if (name.equals(ROOT_WEBAPP)) {
                // If we've just mounted the root webapp, make it the root.
                this.server.setRootWebApp(name);
            }
            c.setExtractWAR(true);
            this.contexts.add(c);
        }

        // Have accesses go into the stdout/stderr log for now.  Later, if
        // demand, we'll have accesses go into their own file.
        NCSARequestLog a = new NCSARequestLog(Heritrix.HERITRIX_OUT_FILE);
        a.setRetainDays(90);
        a.setAppend(true);
        a.setExtended(false);
        a.setBuffered(false);
        a.setLogTimeZone("GMT");
        a.start();
        this.server.setRequestLog(a);
    }

    /**
     * Return the directory that holds the WARs we're to deploy.
     *
     * @return Return webapp path (Path returned has a trailing '/').
     */
    private static String getWARSPath() {
        
        String webappsPath = Heritrix.getWarsdir().getAbsolutePath();
        if (!webappsPath.endsWith(File.separator))
        {
            webappsPath = webappsPath + File.separator;
        }
        return webappsPath;
    }

    /**
     * Start the server.
     *
     * @throws Exception if problem starting server or if server already
     * started.
     */
    public synchronized void startServer()
        throws Exception {
        
        this.server.start();
    }

    /**
     * Stop the running server.
     *
     * @throws InterruptedException
     */
    public synchronized void stopServer() throws InterruptedException {
        
        if (this.server != null)
        {
            this.server.stop();
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#finalize()
     */
    protected void finalize()
        throws Throwable {
        
        stopServer();
        super.finalize();
    }

    /**
     * @return Port server is running on.
     */
    public int getPort() {
        
        return this.port;
    }

    /**
     * @return Server reference.
     */
    public HttpServer getServer() {
        
        return this.server;
    }

    /**
     * @param contextName Name of context to look for.  Possible names would be
     * '/admin', '/', or '/selftest'.
     *
     * @return named context.
     */
    private WebApplicationContext getContext(String contextName) {
        
        WebApplicationContext context = null;

        if (this.contexts == null)
        {
            throw new NullPointerException("No contexts available.");
        }

        if (!contextName.startsWith("/")) {
            contextName = '/' + contextName;
        }
        for (Iterator i = this.contexts.iterator(); i.hasNext();)
        {
            WebApplicationContext c = (WebApplicationContext)i.next();
            if (c.getHttpContextName().equals(contextName))
            {
                context = c;
                break;
            }
        }

        if (context == null)
        {
            throw new NoSuchElementException("Unknown webapp: " + contextName);
        }

        return context;
    }

    /**
     * Get path to named webapp.
     *
     * @param name Name of webpp.  Possible names are 'admin' or 'selftest'.
     *
     * @return Path to deployed webapp.
     */
    public File getWebappPath(String name) {
        
        if (this.server == null)
        {
            throw new NullPointerException("Server does not exist");
        }
        String contextName =
            (name.equals(this.server.getRootWebApp()))? "/": "/" + name;
        return new
            File(getContext(contextName).getServletHandler().getRealPath("/"));
    }
    
    /**
     * @return Returns the root webapp name.
     */
    public static String getRootWebappName()
    {
        return ROOT_WEBAPP;
    }
}
