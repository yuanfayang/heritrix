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
package org.archive.configuration.registry;

import java.io.IOException;
import java.util.Hashtable;
import java.util.logging.Level;
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

import org.archive.configuration.Configuration;
import org.archive.configuration.ConfigurationException;
import org.archive.configuration.Registry;
import org.archive.configuration.Store;
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
    
    private MBeanServer registry;
    private String baseDomain;
    private Store store;
    
    public JmxRegistry() {
        super();
    }
    
   public void initialize(final String bd, final Store s) {
       this.baseDomain = bd;
       this.store = s;
       this.registry = JmxUtils.getMBeanServer();
       if (this.registry == null) {
           throw new NullPointerException("MBeanServer cannot " +
               "be null");
       }
   }
    
    ObjectName getObjectName(final String name, final Class type)
    throws MalformedObjectNameException {
        return getObjectName(name, type, this.baseDomain);
    }
    
    public void save(Store store, String domain)
    throws IOException {
        // TODO Auto-generated method stub
    }
    
    public void load(Store store, String domain)
    
    throws IOException {
        // TODO Auto-generated method stub
    }
    
    /**
     * Create an ObjectName
     * Default access so can be used by unit tests.
     * TODO: Create queryable ObjectNames.
     * @param name
     * @param domain
     * @return An ObjectName made from passed <code>name</code>
     * and <code>domain</code>.
     * @throws MalformedObjectNameException
     */
    ObjectName getObjectName(final String name, final Class type,
            final String domain)
    throws MalformedObjectNameException {
        /*
         * TODO Ensure the passed domain is in this Registry's namespace.
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
         */
        Hashtable<String, String> ht = new Hashtable<String, String>();
        ht.put(Configuration.NAME_KEY, name);
        ht.put(Configuration.TYPE_KEY, type.getName());
        return new ObjectName(domain, ht);
    }
    
    @SuppressWarnings("unused")
    public Object register(final String name, final Class type,
            final String domain, final Configuration instance)
    throws ConfigurationException {
        Object result = null;
        try {
            result = this.registry.registerMBean(instance,
                getObjectName(name, type, domain));
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

    public Object register(String name, Class type, Configuration instance)
    throws ConfigurationException {
        return register(name, type, getBaseDomain(), instance);
    }

    public boolean isRegistered(final String name, final Class type) {
        return isRegistered(name, type, getBaseDomain());
    }

    public boolean isRegistered(final String name,
         	    final Class type, final String domain) {
        boolean result = false;
        try {
            result = this.registry.
                isRegistered(getObjectName(name, type, domain));
        } catch (MalformedObjectNameException e) {
            LOGGER.log(Level.WARNING, "Failed check", e);
        }
        return result;
    }
    
    public void deRegister(final Object on) {
        try {
            this.registry.unregisterMBean(((ObjectInstance)on).
                getObjectName());
        } catch (InstanceNotFoundException e) {
            e.printStackTrace();
        } catch (MBeanRegistrationException e) {
            e.printStackTrace();
        }
    }
    
    public void deRegister(final String name, final Class type,
            final String domain) {
        // TODO
    }
    
    public void deRegister(final String name, final Class type) {
        // TODO
    }

    public Object get(String attributeName, String name)
    throws AttributeNotFoundException {
        return get(attributeName, name, null, getBaseDomain());
    }
    
    public Object get(String attributeName, String name, Class type)
    throws AttributeNotFoundException {
        return get(attributeName, name, type, getBaseDomain());
    }

    public Object get(String attributeName, String name,
         	final Class type, final String domain)
    throws AttributeNotFoundException {
        ObjectName on = null;
        try {
            // TODO: Add support for querying.  type or name could be
            // null so should use the incomplete ObjectName spec.
            // instead to do queries.
            on = getObjectName(name, type, domain);
        } catch (MalformedObjectNameException e) {
            e.printStackTrace();
        }
        Object result = null;
        try {
            result = this.registry.getAttribute(on, attributeName);
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
}