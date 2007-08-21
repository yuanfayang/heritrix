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
 * JMXModuleListener.java
 *
 * Created on Mar 1, 2007
 *
 * $Id:$
 */
package org.archive.settings.jmx;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import javax.management.DynamicMBean;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.archive.settings.ModuleListener;
import org.archive.settings.SheetManager;
import org.archive.util.JmxUtils;


/**
 * @author pjack
 *
 */
public class JMXModuleListener implements ModuleListener, Serializable {


    private static final long serialVersionUID = 1L;


    final public static String INSTANCE = "instance";
    

    private class ObjectInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        public int count;
        public ObjectName name;
    }
    
    
    final private String domain;
    private String name;
    transient private MBeanServer server;
    final private Map<Object,ObjectInfo> counts = 
        new IdentityHashMap<Object,ObjectInfo>();
    
    
    public JMXModuleListener(String domain, String name, MBeanServer server) {
        this.domain = domain;
        this.server = server;
        this.name = name;
    }


    public void setServer(MBeanServer server, String name) {
        this.server = server;
        this.name = name;
        for (Object o: counts.keySet()) {
            LoggingDynamicMBean.register(server, o, nameOf(o));
        }
    }


    public synchronized void moduleChanged(Object old, Object newModule) {        
        // These vars are used to prevent expensive registration/deregistration
        // from happening inside the synchronized block.
        ObjectName toBeRemoved = null;
        boolean toBeAdded = false;
        
        synchronized (this) {
            if (old instanceof DynamicMBean) {
                ObjectInfo info = counts.get(old);
                if (info != null) {
                    info.count--;
                    toBeRemoved = info.name;
                    counts.remove(old);
                }
            }
                
            if (newModule instanceof DynamicMBean) {
                ObjectInfo info = counts.get(newModule);
                if (info == null) {
                    info = new ObjectInfo();
                    DynamicMBean mbean = (DynamicMBean)newModule;
                    info.name = nameOf(mbean);
                    counts.put(newModule, info);
                    toBeAdded = true;
                }
            }
        }
        
        if (toBeRemoved != null) {
            try {
                server.unregisterMBean(toBeRemoved);
            } catch (InstanceNotFoundException e) {
                throw new IllegalStateException(e);
            } catch (MBeanRegistrationException e) {
                throw new IllegalStateException(e);
            }
        }
        
        if (toBeAdded) {
            LoggingDynamicMBean.register(server, newModule, nameOf(newModule));
        }
    }


    public ObjectName nameOf(Object o) {
        return nameOf(this.domain, this.name, o);
    }

    
    public static ObjectName nameOf(String domain, String name, Object o) {
        String host;
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            host = ".unknown";
        }
        String port = System.getProperty("com.sun.management.jmxremote.port");
        if (port == null) {
            port = "-1";
        }
        String type = getRegisteredType(o);

        return JmxUtils.makeObjectName(
                domain, 
                name, 
                type,
                INSTANCE, String.valueOf(System.identityHashCode(o)),
                JmxUtils.HOST, host,
                JmxUtils.JMX_PORT, port);        
    }


    private static String getRegisteredType(Object o) {
        String type;
        if (o instanceof DynamicMBean) {
            MBeanInfo info = ((DynamicMBean)o).getMBeanInfo();
            type = info.getClassName();
        } else {
            type = o.getClass().getName();
        }
        return type;
    }

    
    public static String getHost() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return ".unknown";
        }
    }
    
    
    public static int getPort() {
        String port = System.getProperty("com.sun.management.jmxremote.port");
        try {
            return Integer.parseInt(port);
        } catch (RuntimeException e) {
            return -1;
        }
        
    }
    
    

    public static JMXModuleListener get(SheetManager mgr) {
        JMXModuleListener result = null;
        for (ModuleListener ml: mgr.getModuleListeners()) {
            if (ml instanceof JMXModuleListener) {
                if (result != null) {
                    throw new IllegalStateException("More than one JMXModuleListener.");
                }
                result = (JMXModuleListener)ml;
            }
        }
        if (result == null) {
            throw new IllegalStateException("No JMXModuleListener.");
        }
        return result;
    }


    public Set<Object> getModules() {
        return new HashSet<Object>(counts.keySet());
    }

}
