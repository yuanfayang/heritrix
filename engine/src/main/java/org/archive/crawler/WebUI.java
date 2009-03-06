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
import org.mortbay.jetty.security.SslSocketConnector;
import org.mortbay.jetty.webapp.WebAppContext;

/**
 * @author pjack
 *
 */
public class WebUI {

    public static final String KEYSTORE_PASSWORD = "heritrix";
    
    private Set<String> hosts;
    private int port;
    private String pathToWAR;
    private String uiPassword; 
    private Server server;
    private String keystore;
    private boolean ssl;
    
    public WebUI(WebUIConfig config) {
        this.hosts = new HashSet<String>(config.getHosts());
        this.port = config.getPort();
        this.pathToWAR = config.getPathToWAR();
        if (this.pathToWAR == null) {
            this.pathToWAR = getDefaultPathToWAR();
        }
        this.uiPassword = config.getUiPassword();
        this.keystore = config.getKeystore();
        this.ssl = config.isSsl();
    }

    
    private String getDefaultPathToWAR() {
        File file = new File("lib");
        if (!file.isDirectory()) {
            throw new IllegalStateException("No path to webapp specified, and no " +
                    "WAR found in ./lib.  Use --webui-war-path to specify a path to " +
                    "the Heritrix WAR file or uncompressed webapp directory.");
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

    private SocketConnector makeConnector(String host) throws Exception {
        SocketConnector sc;
        if (ssl && keystore != null) {
            SslSocketConnector ssc = new SslSocketConnector();
            ssc.setKeystore(keystore);
            ssc.setTruststore(keystore);
            ssc.setKeyPassword(KEYSTORE_PASSWORD);
            ssc.setTrustPassword(KEYSTORE_PASSWORD);
            ssc.setPassword(KEYSTORE_PASSWORD);
            sc = ssc;
        } else if (ssl) {
            try {
                // parameter sets the CN on the temporary certificate
                sc = new TempCertSslSocketConnector(host);
            } catch (Exception e) {
                throw new Exception("There was a problem initializing the " + 
                        "SSL web service with a temporary certificate. You " + 
                        "should try creating a keystore using the java " + 
                        "keytool and pass it to heritrix with --keystore. " + 
                        "Alternatively, you can run heritrix with --no-ssl. " +
                        e);
            }
        } else {
            sc = new SocketConnector();
        }
        
        sc.setHost(host);
        sc.setPort(port);
        
        return sc;
    }

    public void start() throws Exception {
        this.server = new Server();
        
        for (String host: hosts) {
            server.addConnector(makeConnector(host));
        }
        if (hosts.isEmpty()) {
            // listen on "any"
            server.addConnector(makeConnector(null));
        }

        WebAppContext webapp = new WebAppContext(pathToWAR, "/");
        webapp.setAttribute("uiPassword", uiPassword);

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
 
    public void stop() {
        try {
            this.server.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
