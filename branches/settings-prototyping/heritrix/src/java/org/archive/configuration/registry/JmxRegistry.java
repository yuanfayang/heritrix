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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.Attribute;
import javax.management.AttributeList;
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

import org.archive.configuration.Configurable;
import org.archive.configuration.Configuration;
import org.archive.configuration.ConfigurationException;
import org.archive.configuration.Registry;
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
    private final String baseDomain;
    
    /**
     * Make configurable.
     * Make it so can have different store implementations.
     */
	private final File store =
        new File(System.getProperty(STORE_DIR_KEY, 
                System.getProperty("java.io.tmpdir", "/tmp")),
            this.getClass().getName());
	
    public JmxRegistry() {
        this("default.domain");

    }
    
    public JmxRegistry(final String d) {
        super();
        this.registry = JmxUtils.getMBeanServer();
        if (this.registry == null) {
            throw new NullPointerException("MBeanServer cannot " +
                "be null");
        }
        this.baseDomain = d;
    }
    
    ObjectName getObjectName(final String name, final Class type)
    throws MalformedObjectNameException {
        return getObjectName(name, type, this.baseDomain);
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
        ht.put(NAME_KEY, name);
        ht.put(TYPE_KEY, type.getName());
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
         
            /*
            if (this.load) {
            	ObjectName on = ((ObjectInstance)result).getObjectName();
            	AttributeList al = null;
				try {
					al = load(on);
				} catch (FileNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
            	if (al != null) {
            		try {
						this.registry.setAttributes(on, al);
					} catch (InstanceNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (ReflectionException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
            	}
               
            }*/
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
            result = this.registry.isRegistered(getObjectName(name, type, domain));
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
    
    protected File getDomainSubDir(final File storeDir,
    		final String domain) {
    	// TODO
    	storeDir.mkdirs();
    	return storeDir;
    }
    
    @SuppressWarnings("unused")
    public synchronized void load(String domain) throws IOException {
        final File subdir = getDomainSubDir(this.store, domain);
        File [] files = subdir.listFiles();
        for (int i = 0; i < files.length; i++) {
            Configuration c = load(files[i]);
            // Register?
        }
    }
    
    // Is this needed anymore?
    protected synchronized AttributeList load(final ObjectName on)
    throws FileNotFoundException, IOException {
        AttributeList result = null;
        final File subdir = getDomainSubDir(this.store, on.getDomain());
        File f = new File(subdir, on.getCanonicalKeyPropertyListString());
        if (!f.exists()) {
        	return result;
        }
        return load(f).getAttributes(new String [] {});
    }
        
    protected synchronized Configuration load(final File f)
        throws FileNotFoundException, IOException {
        Configuration result = null; 
        ObjectInputStream ois =
            new ObjectInputStream(new FileInputStream(f));
        try {
        	ObjectInstance oi = (ObjectInstance)ois.readObject();
        	String name = oi.getObjectName().getKeyProperty("name");
        	AttributeList al = (AttributeList)ois.readObject();
            Class c = Class.forName(oi.getClassName());
            c = c.getEnclosingClass();
            Constructor cons = c.getConstructor(new Class [] {String.class});
            Configurable configurable = (Configurable)cons.newInstance(new Object [] {name});
            Method defaultConfigMethod = c.getDeclaredMethod("getInitialConfiguration", new Class [] {});
            result = (Configuration) defaultConfigMethod.invoke(configurable, new Object [] {});
            result.setAttributes(al);
        } catch (IOException e) {
        	// TODO Auto-generated catch block
        	e.printStackTrace();
        } catch (ClassNotFoundException e) {
        	// TODO Auto-generated catch block
        	e.printStackTrace();
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InstantiationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result;
    }

	public void save(String domain) throws IOException {
        // TODO Auto-generated method stub
        ObjectName on = null;
        try {
            on = new ObjectName(domain + ":*");
        } catch (MalformedObjectNameException e) {
            throw new IOException("Failed query of mbeans: " + e.toString());
        }
        save(store, domain, this.registry.queryMBeans(on, null));
    }
    
    protected void save(final File storeDir, final String domain,
    		final Set objectInstances) throws IOException {
    	if (objectInstances == null || objectInstances.size() <= 0) {
    		return;
    	}
    	
    	final File subdir = getDomainSubDir(storeDir, domain);
        for (final Iterator i = objectInstances.iterator(); i.hasNext();) {
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
            String [] attributeNames = new String[mbai.length];
            for (int j = 0; j < mbai.length; j++) {
                attributeNames[j] = mbai[j].getName();
            }
            try {
				AttributeList al = this.registry.getAttributes(oi.getObjectName(), attributeNames);
				for (final Iterator k = al.iterator(); k.hasNext();) {
					Attribute a = (Attribute)k.next();
					System.out.println(a.getName() + " " + a.getValue());
				}
				File beanFile = new File(subdir, oi.getObjectName().getCanonicalKeyPropertyListString());
				if (beanFile.exists()) {
					beanFile.delete();
				}
				ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(beanFile));
				try {
					oos.writeObject(oi);
					oos.writeObject(al);
				} finally {
					oos.close();
				}
			} catch (InstanceNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ReflectionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
    }
}