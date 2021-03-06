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
 * Crawler.java
 *
 * Created on May 7, 2007
 *
 * $Id:$
 */
package org.archive.crawler.webui;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.servlet.jsp.JspWriter;

import org.archive.util.JmxUtils;


/**
 * @author pjack
 *
 */
public class Crawler implements Comparable<Crawler> {

    
    /**
     * Logger.
     */
    final private static Logger LOGGER =
        Logger.getLogger(Crawler.class.getName());
    
    /**
     * How the crawler was discovered: Either via JNDI, or via manual entry.
     */
    public static enum Source {
        
        /** The crawler was listed in a JNDI service. */
        JNDI, 
        
        /** The crawler was manually added via the web UI. */
        MANUAL, 
        
        /** 
         * The crawler is running in the same JVM as the web UI, so the web UI
         * found it automatically. 
         */
        LOCAL
    }
    
    
    /**
     * The JMX name of the Engine.
     */
    private ObjectName objectName;
    
    
    /**
     * How this crawler was discovered.
     */
    private Source source = Source.MANUAL;
    
    
    /**
     * The host of the remote crawling node.  Used to connect via JMX.
     */
    private String host = "unknown";


    /**
     * The port number of the remote crawler node.  Used to connect via
     * JMX.
     */
    private int port = -1;

    
    /**
     * The System identityHashCode of the remote crawler.  This is the 
     * system id of the crawler in the <i>remote</i> JVM, not the JVM hosting
     * the webapp.  May be -1 if the remote JVM could not be contacted.
     */
    private int id = -1;
    
    /**
     * A description of the error if the remote crawler could not be contacted.
     */
    private String error;

    
    /**
     * The username to use when connecting via JMX.
     */
    private String username;
    
    
    /**
     * The password to use when connecting via JMX.
     */
    private String password;


    /**
     * Constructor.
     */
    public Crawler() {
    }

    
    public boolean isLocal() {
        return source == Source.LOCAL;
    }


    public String getError() {
        return error;
    }


    public void setError(String error) {
        this.error = error;
    }


    public String getHost() {
        return host;
    }


    public void setHost(String host) {
        this.host = host;
    }


    public ObjectName getObjectName() {
        return objectName;
    }


    public void setObjectName(ObjectName objectName) {
        this.objectName = objectName;
    }


    public int getPort() {
        return port;
    }


    public void setPort(int port) {
        this.port = port;
    }


    public Source getSource() {
        return source;
    }


    public void setSource(Source source) {
        this.source = source;
    }


    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }



    public String getUsername() {
        return username;
    }



    public void setUsername(String username) {
        this.username = username;
    }

    
    public int getIdentityHashCode() {
        return id;
    }
    
    
    public void setIdentityHashCode(int id) {
        this.id = id;
    }
    
    
    public static Crawler fromObjectName(ObjectName oname) {
        int id = Integer.parseInt(oname.getKeyProperty("instance"));
        int port = Integer.parseInt(oname.getKeyProperty(JmxUtils.JMX_PORT));
        Crawler crawler = new Crawler();
        crawler.setIdentityHashCode(id);
        crawler.setSource(Crawler.Source.JNDI);
        crawler.setHost(oname.getKeyProperty(JmxUtils.HOST));
        crawler.setPort(port);
        crawler.setObjectName(oname);
        return crawler;
    }
    
    
    /**
     * Tests the connection to the remote MBean server.  This method will 
     * do different things depending on what this Crawler contains.
     * 
     * <p>If the source is marked as JNDI, then this method just tries
     * to open a JMX connection using the host, port, username and password.
     * If successful, it returns a 1-element collection containing only
     * this crawler.  On failure, it sets the error field and returns the
     * same thing.
     * 
     * <p>If the source is marked as manual <i>and</i> the ObjectName
     * field isn't null, this method will do the same as for a JNDI-discovered
     * crawler:  Attempt to contact the remote MBean server, marking the
     * error field on failure, and returning a 1-element collection 
     * in either event.
     * 
     * <p>If the source is MANUAL and the ObjectName is not set, then 
     * this method contacts the remote MBean server, and performs an MBean
     * query to get all of the Engine beans registered in that server.
     * The returned collection will contain one crawler for each Engine
     * found in the query.  If any error occurs during processing any of
     * the Engines -- or if contact could not be established, or
     * if a network error occurs -- then this Crawler's error field is set
     * and a 1-element collection containing only this crawler is returned.
     * 
     * <p>It made sense at the time.
     * 
     * <p>Note this method never raises an exception.
     * 
     * @return   A 1-element collection if this crawler already has an 
     *    ObjectName; or a possibly multi-element collection if this crawler
     *    discovered multiple Engine ObjectNames in the remote server.
     */
    public Collection<Crawler> testConnection() {
        if ((username == null) || (password == null)) {
            this.error = "Unauthenticated: No username and/or password.";
            return Collections.singleton(this);
        }

        // If we have an objectName, then we were either discovered
        // via JNDI, or a manual connection successfully queried
        // the remote MBeanServer.
        if (this.objectName != null) {
            return Collections.singleton(this);
        }

        JMXConnector jmxc = null;
        try {
            jmxc = Misc.connect(host, port, username, password);
            this.error = null;

            
            // Otherwise, this is a newly minted manual connection from the
            // operator.  We need to connect to the remote MBeanServer and
            // ask it for ALL Engines it knows about; there may
            // be more than one.
            MBeanServerConnection conn = jmxc.getMBeanServerConnection();
            String query = "org.archive.crawler:*,name=Engine";
            ObjectName qname = new ObjectName(query);
            @SuppressWarnings("unchecked")
            Set<ObjectName> set = conn.queryNames(null, qname);
            if (set.isEmpty()) {
                error = "No Engines found on host.";
                return Collections.singleton(this);
            }
            
            Collection<Crawler> result = new ArrayList<Crawler>();
            for (ObjectName oname: set) {
                Crawler c = fromObjectName(oname);
                c.setSource(source); // Either MANUAL or LOCAL
                c.setUsername(username);
                c.setPassword(password);
                result.add(c);
            }
            
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            this.error = e.getMessage();
            return Collections.singleton(this);
        } finally {
            if (jmxc != null) try {
                jmxc.close();
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Could not close jmx connection.", e);
            }
        }
    }


    public String getQueryString() {
        // FIXME escape query string
        return "host=" + host + "&port=" + port + "&id=" + id;
    }
    
    
    public void printFormFields(JspWriter out) throws IOException {
        out.print("<input type=\"hidden\" name=\"host\" value=\"");
        out.print(Text.attr(host));
        out.println("\">");
        out.print("<input type=\"hidden\" name=\"port\" value=\"");
        out.print(port);
        out.println("\">");
        out.print("<input type=\"hidden\" name=\"id\" value=\"");
        out.print(id);
        out.println("\">");
    }
    
    
    public int hashCode() {
        return host.hashCode() ^ port ^ id ^ source.hashCode();
    }
    
    
    public boolean equals(Object o) {
        if (!(o instanceof Crawler)) {
            return false;
        }
        Crawler c = (Crawler)o;
        return compareTo(c) == 0;
    }

    /**
     * Compares by host, then by port, then by id, then by source.  No other
     * field is compared.
     * 
     * @return whether this Crawler is greater than another
     * @throws IllegalArgumentException if the given object is null or not a
     *    Crawler
     */
    public int compareTo(Crawler c) {
        int r = getHost().compareTo(c.getHost());
        if (r != 0) {
            return r;
        }
        
        r = getPort() - c.getPort();
        if (r != 0) {
            return r;
        }
        
        r = getIdentityHashCode() - c.getIdentityHashCode();
        if (r != 0) {
            return r;
        }
        
        r = getSource().compareTo(c.getSource());
        return r;
    }
    
    
    
    /**
     * Returns "host:port#id".
     */
    public String toString() {
        return host + ":" + port + "#" + id;
    }
    
    
    public String getLegend() {
        return host + ":" + port + "#" + id;
    }


    public JMXConnector connect() {
        if (isLocal()) {
            return new LocalJMXConnector();
        }
        try {
            return Misc.connect(host, port, username, password);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }


    

    
}

