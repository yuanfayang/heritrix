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
 * ShowHome.java
 *
 * Created on May 7, 2007
 *
 * $Id:$
 */

package org.archive.crawler.webui;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.naming.Context;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;
import javax.servlet.ServletContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.archive.util.JmxUtils;
import org.archive.util.JndiUtils;

/**
 * Contains the action code for the webapp/home subdirectory.
 * 
 * @author pjack
 *
 */
public class Home {

    
    final private static String USERNAME = "jmx.username";
    final private static String PASSWORD = "jmx.password";
    
    
    private static volatile Set<Crawler> allCrawlers = 
        new TreeSet<Crawler>();
    
    private static Set<Crawler> manualCrawlers = 
        Collections.synchronizedSet(new TreeSet<Crawler>()); 

    private static volatile boolean jndiWarning = false;

    public static void showHome(
            ServletContext sc, 
            HttpServletRequest request, 
            HttpServletResponse response) {
        if(allCrawlers.isEmpty()) {
            tryLocal(sc);
        }
        request.setAttribute("crawlers", allCrawlers);
        request.setAttribute("jndiWarning", jndiWarning);
        Misc.forward(request, response, "page_home.jsp");
    }
    
    
    public static void refreshCrawlers(
            ServletContext sc, 
            HttpServletRequest request, 
            HttpServletResponse response) {
        populateCrawlers(sc);
        showHome(sc, request, response);
    }

    
    public static void showAuthenticateCrawler(
            ServletContext sc, 
            HttpServletRequest request, 
            HttpServletResponse response) {
        Crawler c = getCrawler(request);
        request.setAttribute("host", c.getHost());
        request.setAttribute("port", c.getPort());
        request.setAttribute("id", c.getIdentityHashCode());

        Misc.forward(request, response, "page_authenticate_crawler.jsp");
    }


    public static void authenticateCrawler(
            ServletContext sc, 
            HttpServletRequest request, 
            HttpServletResponse response) {
        Crawler c = getCrawler(request);
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        if (c.getSource() == Crawler.Source.JNDI) {
            sc.setAttribute(USERNAME, username);
            sc.setAttribute(PASSWORD, password);
            populateCrawlers(sc);
        } else {
            c.setUsername(request.getParameter("username"));
            c.setPassword(request.getParameter("password"));
            Collection<Crawler> all = c.testConnection();
            Collection<Crawler> retain = new ArrayList<Crawler>(manualCrawlers);
            retain.addAll(all);
            retain.remove(c);
            manualCrawlers.retainAll(retain);
        }
        showHome(sc, request, response);
    }
    
    
    public static void showAddCrawler(
            ServletContext sc, 
            HttpServletRequest request, 
            HttpServletResponse response) {
        Misc.forward(request, response, "page_add_crawler.jsp");
    }
    
    
    public static void addCrawler(
            ServletContext sc, 
            HttpServletRequest request, 
            HttpServletResponse response) {
        int port ;
        try {
            port = Integer.parseInt(request.getParameter("port"));
        } catch (NumberFormatException e) {
            request.setAttribute("error", "Invalid port, try again.");
            Misc.forward(request, response, "page_add_crawler.jsp");
            return;
        }
        
        String host = request.getParameter("host");
        if ((host == null) || (host.trim().length() == 0)) {
            request.setAttribute("error", "Host must not be blank, try again.");
            Misc.forward(request, response, "page_add_crawler.jsp");
            return;            
        }
        
        Crawler crawler = new Crawler();
        crawler.setHost(host);
        crawler.setPort(port);
        crawler.setUsername(request.getParameter("username"));
        crawler.setPassword(request.getParameter("password"));

        // Discover all crawlers.
        Collection<Crawler> all = crawler.testConnection();
        
        // Add the discovered crawlers.
        manualCrawlers.addAll(all);

        populateCrawlers(sc);
        
        showHome(sc, request, response);
    }
    
    protected static void tryLocal(ServletContext sc) {
        Crawler crawler = new Crawler();
        crawler.setHost("ignored");
        crawler.setPort(-1); // special flag value for 'local' 
        crawler.setUsername("ignored");
        crawler.setPassword("ignored");
        
        // Discover all crawlers.
        Collection<Crawler> all = crawler.testConnection();
        
        // Add the discovered crawlers.
        manualCrawlers.addAll(all);

        populateCrawlers(sc);
    }

    public static void removeCrawler(
            ServletContext sc, 
            HttpServletRequest request,
            HttpServletResponse response) {
        Crawler c = getCrawler(request);
        manualCrawlers.remove(c);
        populateCrawlers(sc);
        showHome(sc, request, response);
    }
    
    
    public static Crawler getCrawler(HttpServletRequest request) {
        String host = request.getParameter("host");
        int port = Integer.parseInt(request.getParameter("port"));
        int id = Integer.parseInt(request.getParameter("id"));
        for (Crawler c: allCrawlers) {
            if (c.getHost().equals(host) 
            && (c.getPort() == port)
            && (c.getIdentityHashCode() == id)) {
                request.setAttribute("crawler", c);
                return c;
            }
        }
        throw new IllegalArgumentException("No such crawler.");
    }

    
    private static void populateCrawlers(ServletContext sc) {
        try {
            populateCrawlers2(sc);
        } catch (MalformedObjectNameException e) {
            throw new IllegalStateException(e);
        } catch (NamingException e) {
            throw new IllegalStateException(e);
        }
    }

    
    private static void populateCrawlers2(ServletContext sc)
    throws NamingException, MalformedObjectNameException {
        String username = (String)sc.getAttribute(USERNAME);
        String password = (String)sc.getAttribute(PASSWORD);
        List<Crawler> result = new ArrayList<Crawler>();

        // First add JNDI crawlers.
        result.addAll(getJNDICrawlers(username, password));
        
        // Next test and then add manual crawlers.
        Iterator<Crawler> iter = manualCrawlers.iterator();
        while (iter.hasNext()) {
            Crawler crawler = iter.next();
            Collection<Crawler> all = crawler.testConnection();
            if (all.size() > 0) {
                iter.remove();
                result.addAll(all);
            } else {
                result.add(crawler);
            }
        }

        allCrawlers = new TreeSet<Crawler>(result);
    }


    private static List<Crawler> getJNDICrawlers(String user, String pass) 
    throws NamingException, MalformedObjectNameException {
        List<Crawler> result = new ArrayList<Crawler>();

        Context context;
        try {
            context = JndiUtils.getSubContext("org.archive.crawler");
        } catch (NoInitialContextException e) {
            jndiWarning = true;
            return result;
        }
        jndiWarning = false;
        
        NamingEnumeration<NameClassPair> enu = context.list("");
        while (enu.hasMore()) {
            NameClassPair pair = enu.next();
            ObjectName oname = new ObjectName("org.archive.crawler:" + pair.getName());
            String name = oname.getKeyProperty(JmxUtils.NAME);
            if ((name != null) && name.equals("CrawlJobManager")) {
                Crawler crawler = Crawler.fromObjectName(oname);
                crawler.setSource(Crawler.Source.JNDI);
                crawler.setUsername(user);
                crawler.setPassword(pass);
                crawler.testConnection();
                result.add(crawler);
            }
        }
        return result;        
    }


}
