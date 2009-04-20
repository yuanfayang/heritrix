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

/**
 * Test HTTP basic authentication
 *
 * @contributor stack
 * @contributor gojomo
 */
public class HttpAuthSelfTest
    extends SelfTestBase
{
    /**
     * Files to find as a list.
     */
    final private static Set<String> EXPECTED = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList(new String[] {
            "index.html", "link1.html", "link2.html", "link3.html", 
            "basic/index.html", "basic/link1.html", "basic/link2.html", "basic/link3.html", 
            "robots.txt"
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

        this.httpServer = server;
        this.httpServer.start();
    }

    @Override
    protected String changeGlobalConfig(String config) {
        String newCredStore = 
            "<bean id=\"credentialStore\" class=\"org.archive.modules.credential.CredentialStore\">\n" + 
            "  <property name=\"credentials\">\n" + 
            "   <map>\n" + 
            "    <entry key=\"test\">\n" + 
            "     <bean class=\"org.archive.modules.credential.Rfc2617Credential\">\n" + 
            "      <property name=\"domain\" value=\"127.0.0.1:7777\"/>\n" + 
            "      <property name=\"realm\" value=\"Hyrule\"/>\n" + 
            "      <property name=\"login\" value=\"Mr. Happy Pants\"/>\n" + 
            "      <property name=\"password\" value=\"xyzzy\"/>\n" + 
            "     </bean>\n" + 
            "    </entry>\n" + 
            "   </map>\n" + 
            "  </property>\n" + 
            "</bean>";
        config = config.replaceFirst(
                "(?s)<bean id=\"credentialStore\" .*?</bean>", 
                newCredStore);
        return super.changeGlobalConfig(config);
    }

}

