/* Reference
 * 
 * $Id$
 * 
 * Created on Jan 4, 2006
 *
 * Copyright (C) 2006 Internet Archive.
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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

/**
 * Utility class to build {@link Configuration} Pointers. Pointers are
 * OpenMBean CompositeTypes. They point at other {@link Configuration}s
 * back in the Registry.  Their data are ObjectNames as Strings.<p>
 * Tried to do as subclass of CompositeDataSupport but then in remote
 * client, the resultant Reference composite is unrecognizable. Means
 * have to always register CompositeData rather than Pointer and
 * that ConfigurationArrays should be Arrays of CompositeData rather
 * than Pointer.  Added a {@link Pointer#isPointer(CompositeData)}
 * to test CompositeData for Pointer.  Because of this, Pointer is
 * mostly utilty for building a Pointer CompositeType.
 * @author stack
 * @version $Date$ $Revision$
 */
public class Pointer  {
    private static final String OBJECTNAME_KEY = "objectname";
    private static final String [] KEYS =
        new String [] {OBJECTNAME_KEY};
    static CompositeType COMPOSITE_TYPE;
    static {
        try {
            COMPOSITE_TYPE =
                new CompositeType(Pointer.class.getName(),
                    "ObjectName-as-String Reference to a " +
                        "Configuration in registry", KEYS,
                    new String [] {"ObjectName as String"},
                    new OpenType [] {SimpleType.STRING});
        } catch (OpenDataException e) {
            e.printStackTrace();
        }
    }
    
    private final CompositeData compositeData;
    
    /**
     * Shutdown constructor so no accidental instantiation of Reference.
     */
    private Pointer() {
        this((CompositeData)null);
    }
    
    /**
     * @param on ObjectName of an extant registered Configuration.
     * @throws OpenDataException
     */
    public Pointer(final ObjectName on) throws OpenDataException {
        this(create(on));
    }
    
    public Pointer(final CompositeData cd) {
        super();
        this.compositeData = cd;
    }
    
    public static CompositeType getCompositeType() {
        return Pointer.COMPOSITE_TYPE;
    }
    
    public CompositeData getCompositeData() {
        return this.compositeData;
    }
    
    public ObjectName getObjectName()
    throws MalformedObjectNameException {
    	    return new ObjectName((String)getCompositeData().
            get(OBJECTNAME_KEY));
    }
    
    protected static CompositeData create(final ObjectName on)
    throws OpenDataException {
        if (on == null) {
            throw new NullPointerException("Can't pass null ObjectName");
        }
        return new CompositeDataSupport(COMPOSITE_TYPE, KEYS,
            new String [] {on.getCanonicalName()});
    }
    
    /**
     * Test if a CompositeData is a Pointer.
     * @param cd CompositeData to test.
     * @return True if a pointer.
     */
    public static boolean isPointer(final CompositeData cd) {
        return cd.getCompositeType().equals(COMPOSITE_TYPE);
    }
    
    /**
     * Create an instance of pointed to Configurable with its
     * Configuration registered in the passed Registry.
     * @param r Registry to register with.
     * @return Configurable.
     * @throws ConfigurationException
     */
    public Configurable getRegisteredInstance(final Registry r)
    throws ConfigurationException {
        ObjectName on = null;
    	    try {
			on = getObjectName();
    	    } catch (MalformedObjectNameException e) {
            throw new ConfigurationException(e);
        }
		
        final String name = on.getKeyProperty("name");
        final String type = on.getKeyProperty("type");
        Class c;
        try {
			c = Class.forName(type);
		} catch (ClassNotFoundException e) {
			throw new ConfigurationException("Failed to find class " +
                type, e);
        }
        
        Configurable configurable = null;
		try {
			// Get the constructor that takes a String argument.
			Constructor cons = c.getConstructor(new Class [] {String.class});
			configurable =
				(Configurable)cons.newInstance(new Object [] {name});
		} catch (SecurityException e) {
			throw new ConfigurationException(e);
		} catch (NoSuchMethodException e) {
			throw new ConfigurationException(e);
		} catch (IllegalArgumentException e) {
			throw new ConfigurationException(e);
		} catch (InstantiationException e) {
			throw new ConfigurationException(e);
		} catch (IllegalAccessException e) {
			throw new ConfigurationException(e);
		} catch (InvocationTargetException e) {
			throw new ConfigurationException(e);
		}
        if (!r.isRegistered(name, c, on.getDomain())) {
            r.register(name, c, on.getDomain(), configurable.getConfiguration());
        }
        return configurable.initialize(r);
    }
}