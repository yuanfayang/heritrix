/* JmxRegistryHandler
 * 
 * $Id$
 * 
 * Created on Dec 30, 2005
 *
 * Copyright (C) 2005 Internet Archive.
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
package org.archive.configuration.prototyping;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.archive.configuration.Configuration;
import org.archive.configuration.Registry;
import org.archive.crawler.framework.exceptions.ConfigurationException;
import org.archive.util.JmxUtils;

/**
 * Implementation of Configuration Registry that uses JMX Agent.
 * Won't scale to hundreds of thousands of domains w/o customization
 * -- Interceptors or home-grown agent -- but will work prototyping, for
 * globals, and a small number of overrides.
 * <p>TODO: Get via a RegistryFactory.
 * <p>Can ask the MBeanServer for list of registered domains.
 * @author stack
 * @version $Date$ $Revision$
 */
class JmxRegistry implements Registry {
    private final Logger LOGGER =
        Logger.getLogger(this.getClass().getName());
    
    private final MBeanServer registry;
    private String baseDomain = null;
    
    
    public JmxRegistry() {
        super();
        this.registry = JmxUtils.getMBeanServer();
        if (this.registry == null) {
            throw new NullPointerException("MBeanServer cannot " +
                "be null");
        }
    }
    
    ObjectName getObjectName(final String type)
    throws MalformedObjectNameException {
        return getObjectName(type, null);
    }
    
    /**
     * Create an ObjectName
     * Default access so can be used by unit tests.
     * @param type
     * @param domain
     * @return An ObjectName made from passed <code>name</code>
     * and <code>domain</code>.
     * @throws MalformedObjectNameException
     */
    ObjectName getObjectName(final String type, final String domain)
    throws MalformedObjectNameException {
        /*
        String totalDomain = null;
        if (this.basis ==  null || this.basis.length() <= 0) {
            totalDomain = (domain == null || domain.length() <= 0)? "": domain;
        } else {
            if (domain == null || domain.length() <= 0) {
                totalDomain = this.basis;
            } else {
                totalDomain = this.basis + "." + domain;
            }
        }
        return new ObjectName(totalDomain, "type", type);
        */
        return new ObjectName(domain, "type", type);
    }
    
    public Object register(final String component,
            final String domain, final Object instance) {
        Object result = null;
        try {
            result = this.registry.registerMBean(instance,
                getObjectName(component, domain));
        } catch (MalformedObjectNameException e) {
            e.printStackTrace();
        } catch (InstanceAlreadyExistsException e) {
            e.printStackTrace();
        } catch (MBeanRegistrationException e) {
            e.printStackTrace();
        } catch (NotCompliantMBeanException e) {
            e.printStackTrace();
        }
        return result;
    }

    public Object register(String component, Object instance) {
        return register(component, getBaseDomain(), instance);
    }

    public boolean isRegistered(final String component) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isRegistered(final String component, final String domain) {
        // TODO Auto-generated method stub
        return false;
    }
    
    public void deregister(final Object on) {
        try {
            this.registry.unregisterMBean(((ObjectInstance)on).
                getObjectName());
        } catch (InstanceNotFoundException e) {
            e.printStackTrace();
        } catch (MBeanRegistrationException e) {
            e.printStackTrace();
        }
    }

    public Object get(String attributeName, String component) {
        return get(attributeName, component, getBaseDomain());
    }

    public Object get(String attributeName, String component,
            String domain) {
        ObjectName on = null;
        try {
            on = getObjectName(component, domain);
        } catch (MalformedObjectNameException e) {
            e.printStackTrace();
        }
        Object result = null;
        try {
            result = this.registry.getAttribute(on, attributeName);
        } catch (AttributeNotFoundException e) {
            e.printStackTrace();
        } catch (ReflectionException e) {
            e.printStackTrace();
        } catch (InstanceNotFoundException e) {
            e.printStackTrace();
        } catch (MBeanException e) {
            e.printStackTrace();
        }
        return result;
    }
    
    protected String getBaseDomain() {
        return this.baseDomain;
    }

    @SuppressWarnings("unused")
    public synchronized void load(String domain) throws IOException {
        // TODO: Do domain calculation.  If the based domain is a subdomain,
        // do not override our baseDomain.  If passed domain is super
        // domain, do put it in place of our baseDomain.
        this.baseDomain = domain;
    }

    public void save(String domain) throws IOException {
        // TODO Auto-generated method stub
        ObjectName on = null;
        try {
            on = new ObjectName(domain + ":*");
        } catch (MalformedObjectNameException e) {
            throw new IOException("Failed query of mbeans: " + e.toString());
        }
        Set mbeans = this.registry.queryMBeans(on, null);
        for (final Iterator i = mbeans.iterator(); i.hasNext();) {
            ObjectInstance oi = (ObjectInstance)i.next();
            MBeanInfo mi = null;
            try {
                mi = this.registry.getMBeanInfo(oi.getObjectName());
            } catch (InstanceNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IntrospectionException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ReflectionException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            MBeanAttributeInfo [] mbai = mi.getAttributes();
            for (int j = 0; j < mbai.length; j++) {
                System.out.println(mbai[j].getName());
            }
        }
    }
}