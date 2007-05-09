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
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * Static utility methods for action code.
 * 
 * @author pjack
 *
 */
public class Misc {

    
    public static JMXConnector connect(
            String host, 
            int port, 
            String username, 
            String password) throws IOException {     
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



    protected static ObjectName waitFor(
            JMXConnector jmxc, 
            String query, 
            boolean exist) throws Exception {
        MBeanServerConnection server = jmxc.getMBeanServerConnection();
        int count = 0;
        ObjectName name = new ObjectName(query);
        Set set = server.queryNames(null, name);
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

}
