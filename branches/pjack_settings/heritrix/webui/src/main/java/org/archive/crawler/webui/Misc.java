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
 * Actions.java
 *
 * Created on May 7, 2007
 *
 * $Id:$
 */

package org.archive.crawler.webui;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.archive.util.SURT;


/**
 * Static utility methods for action code.
 * 
 * @author pjack
 *
 */
public class Misc {

    
    final private static Pattern SURT_PATTERN = 
        Pattern.compile("^[a-z]+:(//){0,1}\\(");
    
    
    private static boolean isLocal(
            String host, 
            int port, 
            String username,
            String password) {
        return port == -1;
    }
    
    
    
    public static JMXConnector connect(
            String host, 
            int port, 
            String username, 
            String password) throws IOException {
        if (isLocal(host, port, username, password)) {
            return new LocalJMXConnector();
        }
        String hp = host + ":" + port;
        String s = "service:jmx:rmi://" + hp + "/jndi/rmi://" + hp + "/jmxrmi";
        JMXServiceURL url = new JMXServiceURL(s);
        Map<String,Object> env = new HashMap<String,Object>(1);
        String[] creds = new String[] { username, password };
        env.put(JMXConnector.CREDENTIALS, creds);

        JMXConnector jmxc = JMXConnectorFactory.connect(url, env);
        return jmxc;
    }

    
    
    public static void forward(HttpServletRequest request, 
            HttpServletResponse response, String path) {
        try {
            request.getRequestDispatcher(path).forward(request, response);
        } catch (ServletException e) {
            throw new IllegalStateException(e);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    
    public static void setCrawler(HttpServletRequest request) {
        String host = request.getParameter("host");
        int port = Integer.parseInt(request.getParameter("port"));
        int id = Integer.parseInt(request.getParameter("id"));
        request.setAttribute("host", host);
        request.setAttribute("port", port);
        request.setAttribute("id", id);
    }


    public static void close(JMXConnector conn) {
        if (conn == null) {
            return;
        }
        try {
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    
    public static Set<ObjectName> find(JMXConnector jmxc, String query) {
        try {
            MBeanServerConnection conn = jmxc.getMBeanServerConnection();
            @SuppressWarnings("unchecked")
            Set<ObjectName> set = conn.queryNames(null, new ObjectName(query));
            return set;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (MalformedObjectNameException e) {
            throw new IllegalStateException(e);
        }
        
    }
    
    public static ObjectName findUnique(
            JMXConnector jmxc, 
            String query) {
        Set<ObjectName> set = find(jmxc, query);
        int size = set.size();
        if (size == 1) {
            return (ObjectName)set.iterator().next();
        }
        throw new IllegalStateException("Expected unique MBean for " 
                + query + " but found " + set);
    }
    
    
    public static ObjectName findUnique(
            JMXConnector jmxc,
            String jobName,
            Class<?> cls) {
        String query = "org.archive.crawler:*,name=" + jobName + ",type=" +
        cls.getName();
        return findUnique(jmxc, query);
    }

    
    public static <T> T find(
            JMXConnector jmxc, 
            String jobName, 
            Class<T> cls) {
        String query = "org.archive.crawler:*,name=" + jobName + ",type=" +
        cls.getName();
        ObjectName oname = findUnique(jmxc, query);
        return Remote.make(jmxc, oname, cls).getObject();
    }


    public static ObjectName waitFor(
            JMXConnector jmxc, 
            String query, 
            boolean exist) throws Exception {
        MBeanServerConnection server = jmxc.getMBeanServerConnection();
        int count = 0;
        ObjectName name = new ObjectName(query);
        Set<?> set = server.queryNames(null, name);
        while (set.isEmpty() == exist) {
            count++;
            if (count > 40) {
                throw new IllegalStateException("Could not find " + 
                        name + " after 20 seconds.");
            }
            Thread.sleep(500);
            set = server.queryNames(null, name);
            if (set.size() > 1) {
                throw new IllegalStateException(set.size() + " matches for " + query);
            }
        }
        if (set.isEmpty()) {
            return null;
        } else {
            return (ObjectName)set.iterator().next();
        }
    }



    
    /**
     * Converts a line of user input to a SURT.  If the input already looks
     * like a SURT -- if it starts with xxx://( -- then the input is returned
     * unchanged.
     * 
     * <p>TODO: ? If the input doesn't start with a scheme, then http is assumed.
     * 
     * @param url
     * @return
     */
    public static String toSURT(String input) {
        // If it already looks like a surt, return it unchanged.
        if (SURT_PATTERN.matcher(input).find()) {
            return input;
        }
        return SURT.fromPlain(input);
    }
    
    /**
     * Converts a line of user input to a SURT prefix. Leaves lines
     * already looking like SURTs unchanged (so that expert users
     * can supply exact, atypical SURT prefixes). 
     * 
     * @param url
     * @return
     */
    public static String toSURTPrefix(String input) {
        // If it already looks like a surt, return it unchanged.
        if (SURT_PATTERN.matcher(input).find()) {
            return input;
        }
        return SURT.prefixFromPlain(input);
    }
    
}
