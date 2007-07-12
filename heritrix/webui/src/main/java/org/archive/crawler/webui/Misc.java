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

import java.io.File;
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

import org.archive.settings.path.PathChanger;
import org.archive.settings.path.PathValidator;
import org.archive.state.FileModule;
import org.archive.util.SURT;


/**
 * Static utility methods for action code.
 * 
 * @author pjack
 *
 */
public class Misc {

    
    final private static Pattern SURT_PATTERN = Pattern.compile("^[a-z]+:\\(");
    
    
    private static boolean isLocal(
            String host, 
            int port, 
            String username,
            String password) {
        return (port == -1) 
                && username.equals("local")
                && password.equals("local");
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
    
    
    /**
     * Returns the file represented by a settings path to a FileModule.
     * 
     * <p>If the given settings path contains any references, then they are
     * first dereferenced.  Eg, if you're looking for 
     * <tt>root:controller:seeds:dir</tt>, but <tt>root:controller:seeds</tt>
     * is a reference to <tt>root:foo</tt>, then this method will
     * actually return the file at <tt>root:foo:dir</tt>.
     * 
     * <p>The (dereferenced) settings path must point to a Setting whose
     * type is "object" and whose value is {@link FileModule} or one of its
     * subclasses.  This method will then examine the values of 
     * {@link FileModule#PARENT} and {@link FileModule#PATH} to determine 
     * the actual path represented by the module.
     * 
     * @param settings  The settings containing the file module
     * @param path  the settings path to the file module
     * @return  the file represented by that module
     */
    public static File getFile(Settings settings, String path) {
        String[] tokens = path.split(
                Character.toString(PathValidator.DELIMITER));
        String actual = null;
        for (int i = 0; i < tokens.length; i++) {
            if (actual == null) {
                actual = tokens[i];
            } else {
                actual = actual + PathValidator.DELIMITER + tokens[i];
            }
            Setting setting = settings.getSetting(actual);
            if (setting == null) {
                throw new IllegalArgumentException("No setting at " + actual);
            }
            if (setting.getType().equals(PathChanger.REFERENCE_TAG)) {
                actual = setting.getValue();
            }
        }
        
        Setting setting = settings.getSetting(actual);
        if (! setting.getType().equals(PathChanger.OBJECT_TAG)) {
            throw new IllegalArgumentException("Expected object at " + path);
        }
        
        Class c;
        try {
            c = Class.forName(setting.getValue());
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("No such class " +
                    setting.getValue() + " at " + path);
        }
        if (!FileModule.class.isAssignableFrom(c)) {
            throw new IllegalArgumentException("Object at " + path +
                    "is not a FileModule, it's a " + c.getName());
        }
        
        String parentPath = actual + PathValidator.DELIMITER + 
            FileModule.PARENT.getFieldName();
        Setting parentSetting = settings.getSetting(parentPath);
        String parentValue = parentSetting.getValue();
        File parent = parentValue.equals("null") ? null : getFile(settings, parentPath);

        String filePath = actual + PathValidator.DELIMITER + 
            FileModule.PATH.getFieldName();
        String fileValue = settings.getSetting(filePath).getValue();
        File result = new File(fileValue);
        if ((parent != null) && !result.isAbsolute()) {
            return new File(parent, fileValue);
        } else {
            return result;
        }        
    }

    
    /**
     * Converts a line of user input to a SURT.  If the input already looks
     * like a SURT -- if it starts with xxx://( -- then the input is returned
     * unchanged.
     * 
     * <p>If the input doesn't start with a scheme, then http is assumed.
     * 
     * @param url
     * @return
     */
    public static String toSURT(String input) {
        // If it already looks like a surt, return it unchanged.
        if (SURT_PATTERN.matcher(input).find()) {
            return input;
        }
        
        return SURT.fromURI(input);
    }
    
}
