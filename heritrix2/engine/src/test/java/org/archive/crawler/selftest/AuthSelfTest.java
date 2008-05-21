/* AuthSelfTest
 *
 * Created on Feb 17, 2004
 *
 * Copyright (C) 2004 Internet Archive.
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
package org.archive.crawler.selftest;


import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerList;
import org.mortbay.jetty.handler.ResourceHandler;
import org.mortbay.jetty.security.Constraint;
import org.mortbay.jetty.security.ConstraintMapping;
import org.mortbay.jetty.security.HashUserRealm;
import org.mortbay.jetty.security.SecurityHandler;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;


/**
 * Test authentications, both basic/digest auth and html form logins.
 *
 * @author stack
 * @version $Id$
 */
public class AuthSelfTest
    extends SelfTestBase
{


    /**
     * Files to find as a list.
     */
    final private static Set<String> EXPECTED = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList(new String[] {
            "index.html", "link1.html", "link2.html", "link3.html", 
            "basic/index.html", "basic/link1.html", "basic/link2.html", "basic/link3.html", 
            "login/login.html", "success.html", "robots.txt"
    })));


    @Override
    protected void verify() throws Exception {
        Set<String> found = this.filesInArcs();
        assertEquals("wrong files in ARCs",EXPECTED,found);
    }


    @Override
    protected void startHttpServer() throws Exception {
        Server server = new Server();
        
        Constraint constraint = new Constraint();
        constraint.setName(Constraint.__BASIC_AUTH);;
        constraint.setRoles(new String[]{"user","admin","moderator"});
        constraint.setAuthenticate(true);

        ConstraintMapping cm = new ConstraintMapping();
        cm.setConstraint(constraint);
        cm.setPathSpec("/basic/*");
        
        HashUserRealm realm = new HashUserRealm();
        realm.setName("Hyrule");
        realm.put("Mr. Happy Pants", "xyzzy");
        realm.addUserToRole("Mr. Happy Pants", "user");
        
        SecurityHandler securityHandler = new SecurityHandler();
        securityHandler.setUserRealm(realm);
        securityHandler.setConstraintMappings(new ConstraintMapping[]{cm});
        
        SocketConnector sc = new SocketConnector();
        sc.setHost("localhost");
        sc.setPort(7777);
        server.addConnector(sc);
        ResourceHandler rhandler = new ResourceHandler();
        rhandler.setResourceBase(getSrcHtdocs().getAbsolutePath());
        
        ServletHandler servletHandler = new ServletHandler();        
        
        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] { 
                securityHandler, 
                rhandler, 
                servletHandler,
                new DefaultHandler() });
        server.setHandler(handlers);
        
        ServletHolder holder = new ServletHolder(new AuthServlet());
        servletHandler.addServletWithMapping(holder, "/login/*");

        this.httpServer = server;
        this.httpServer.start();
    }



    @Override
    protected String changeGlobalConfig(String config) {
        String token = "CredentialStore\n";        
        int p = config.indexOf(token);
        String newConfig = 
            "root:credential-store:credentials" +
            "=map, org.archive.modules.credential.Credential\n" +
            "root:credential-store:credentials:test" +
            "=object, org.archive.modules.credential.Rfc2617Credential\n" +
            "root:credential-store:credentials:test:" +
            "credential-domain=string, 127.0.0.1:7777\n" +
            "root:credential-store:credentials:test:realm" +
            "=string, Hyrule\n" +
            "root:credential-store:credentials:test:login" +
            "=string, Mr. Happy Pants\n" +
            "root:credential-store:credentials:test:password" +
            "=string, xyzzy\n" +
            "root:credential-store:credentials:test2" +
            "=object, org.archive.modules.credential.HtmlFormCredential\n" +
            "root:credential-store:credentials:test2:credential-domain" +
            "=string, 127.0.0.1:7777\n" +
            "root:credential-store:credentials:test2:login-uri" +
            "=string, http://127.0.0.1:7777/login/login.html\n" +
            "root:credential-store:credentials:test2:form-items" +
            "=map, java.lang.String\n" +
            "root:credential-store:credentials:test2:form-items" +
            ":username=string, Mr. Happy Pants\n" +
            "root:credential-store:credentials:test2:form-items" +
            ":password=string, xyzzy\n";
        String result = config.substring(0, p + token.length()) + 
            newConfig + config.substring(p + token.length());
        return result;
    }

}

