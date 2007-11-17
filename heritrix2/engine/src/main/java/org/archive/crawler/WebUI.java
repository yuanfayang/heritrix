/* 
 * Copyright (C) 2007 Internet Archive.
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
 * WebUI.java
 *
 * Created on Jun 29, 2007
 *
 * $Id:$
 */

package org.archive.crawler;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerList;
import org.mortbay.jetty.webapp.WebAppContext;

/**
 * @author pjack
 *
 */
public class WebUI {

    
    private Set<String> hosts;
    private int port;
    private String pathToWAR;
    private String uiPassword; 
    
    
    public WebUI(WebUIConfig config) {
        this.hosts = new HashSet<String>(config.getHosts());
        this.port = config.getPort();
        this.pathToWAR = config.getPathToWAR();
        if (this.pathToWAR == null) {
            this.pathToWAR = getDefaultPathToWAR();
        }
        this.uiPassword = config.getUiPassword();
    }

    
    private String getDefaultPathToWAR() {
        File file = new File("lib");
        if (!file.isDirectory()) {
            throw new IllegalStateException("No path to WAR specified, and no " +
                    "WAR found in ./lib.  Use -w to specify a path to " +
                    "the Heritrix WAR file.");
        }
        for (String filename: file.list()) {
            if (filename.endsWith(".war")) {
                return new File(file, filename).getAbsolutePath();
            }
        }
        throw new IllegalStateException("No path to WAR specified, and no " +
                        "WAR found in ./lib.  Use -w to specify a path to " +
                        "the Heritrix WAR file.");
    }

    public void start() throws Exception {
        Server server = new Server();
        
        for (String host: hosts) {
            SocketConnector sc = new SocketConnector();
            sc.setHost(host);
            sc.setPort(port);
            server.addConnector(sc);
        }
        
        if(hosts.isEmpty()) {
            // wildcard connector
            SocketConnector sc = new SocketConnector();
            sc.setPort(port);
            server.addConnector(sc);
        }

        WebAppContext webapp = new WebAppContext(pathToWAR, "/");
        webapp.setAttribute("uiPassword",uiPassword);

        // Make sure classes on the system classpath take precedence over 
        // classes in the webapp classpath.
        webapp.setParentLoaderPriority(true);
        
        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] { 
                webapp,
                new DefaultHandler() });
        server.setHandler(handlers);
        
        server.start();
    }

}
