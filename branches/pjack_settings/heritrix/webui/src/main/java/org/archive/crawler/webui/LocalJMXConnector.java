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
 * LocalJMXConnector.java
 *
 * Created on May 30, 2007
 *
 * $Id:$
 */

package org.archive.crawler.webui;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Map;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.remote.JMXConnector;
import javax.security.auth.Subject;

/**
 * Connects to an MBeanServer running in the same JVM.
 * 
 * @author pjack
 */
public class LocalJMXConnector implements JMXConnector {

    
    final private MBeanServerConnection conn;
    
    
    public LocalJMXConnector() {
        this.conn = ManagementFactory.getPlatformMBeanServer();
    }
    
    public LocalJMXConnector(MBeanServerConnection conn) {
        this.conn = conn;
    }
    
    
    /**
     * No op.
     */
    public void addConnectionNotificationListener(
            NotificationListener arg0, 
            NotificationFilter arg1, 
            Object arg2) {
    }


    /**
     * No op.
     */
    public void close() throws IOException {
    }


    /**
     * No op.
     */
    public void connect() throws IOException {
    }

    
    /**
     * No op.
     */
    public void connect(Map<String, ?> arg0) throws IOException {
    }


    public String getConnectionId() throws IOException {
        return "local"; // FIXME
    }


    
    /**
     * Returns the MBeanServerConnection this LocalJMXConnector was constructed
     * with.
     */
    public MBeanServerConnection getMBeanServerConnection() throws IOException {
        return conn;
    }


    
    /**
     * Returns the MBeanServerConnection this LocalJMXConnector was constructed
     * with.
     */
    public MBeanServerConnection getMBeanServerConnection(Subject arg0) 
    throws IOException {
        return conn;
    }


    /**
     * No op.
     */
    public void removeConnectionNotificationListener(NotificationListener arg0, 
            NotificationFilter arg1, 
            Object arg2) throws ListenerNotFoundException {
    }

    
    /**
     * No op.
     */
    public void removeConnectionNotificationListener(NotificationListener arg0) 
    throws ListenerNotFoundException {
        
    }

}
