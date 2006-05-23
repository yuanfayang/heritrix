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
package org.archive.configuration;

import java.io.IOException;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.archive.util.JmxUtils;

/**
 * Implementation of Configuration Handler that uses JMX Agent as
 * configuration registry.
 * Won't scale to hundreds of thousands of domains w/o customization
 * -- Interceptors or home-grown agent -- but will work prototyping, for
 * globals, and a small number of overrides.
 * <p>TODO: Get via a HandlerFactory.
 * <p>Can ask the MBeanServer for list of registered domains.
 * @author stack
 * @version $Date$ $Revision$
 */
class JmxRegistryHandler implements Handler {
    private final Logger LOGGER =
        Logger.getLogger(this.getClass().getName());
    private final MBeanServer registry;
    private final String basis;
    
    public JmxRegistryHandler() {
        this("org.archive");
    }
    
    public JmxRegistryHandler(final String domainBase) {
        super();
        this.registry = JmxUtils.getMBeanServer();
        if (this.registry == null) {
            throw new NullPointerException("MBeanServer cannot " +
                "be null");
        }
        this.basis = domainBase;
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
        return get(attributeName, component, null);
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

    public void load() throws IOException {
        // TODO Auto-generated method stub
    }

    public void load(String domain) throws IOException {
        // TODO Auto-generated method stub   
    }
}