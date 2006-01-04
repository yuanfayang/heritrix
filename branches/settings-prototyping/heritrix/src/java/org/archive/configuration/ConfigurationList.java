/* ConfigurationList
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenMBeanAttributeInfo;
import javax.management.openmbean.OpenMBeanAttributeInfoSupport;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

/**
 * Holds ordered list references to Configurations.
 * 
 * References are of {@link ObjectName} (as CompositeType)  that can be
 * subsequently used doing lookups into registry.
 * 
 * <p>TODO: Should it be array of composite types?  Or just an array of
 * ObjectName.toString.  Should it be ObjectInstances so can include
 * implementing class name?
 * 
 * @author stack
 * @version $Date$ $Revision$
 */
public abstract class ConfigurationList extends Configuration {
    public static final String CONFIGURATIONS_ATTRIBUTE = "List";
    protected static final String DOMAIN_KEY = "domain";
    protected static final String LIST_STR_KEY = "keyPropertyListString"; 
    
    protected static CompositeType ON_COMPOSITE_TYPE;
    static {
        try {
            ON_COMPOSITE_TYPE = new CompositeType(ObjectName.class.getName(),
                "ObjectName as OpenMBean CompositeType",
                new String [] {DOMAIN_KEY, LIST_STR_KEY},
                new String [] {"ObjectName domain",
                    "ObjectName#getCanonicalKeyPropertyListString() output"},
                new OpenType [] {SimpleType.STRING, SimpleType.STRING});
        } catch (OpenDataException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Make a COMPOSITE_TYPE ArrayType to use later in definitions.
     */
    private static ArrayType ON_ARRAY_TYPE;
    static {
        try {
            ON_ARRAY_TYPE = new ArrayType(1, ON_COMPOSITE_TYPE);
        } catch (OpenDataException e) {
            e.printStackTrace();
        }
    }
    
    public ConfigurationList() throws OpenDataException {
        super();
    }
    
    protected List getAttributeNames() {
        return Arrays.asList(new String [] {CONFIGURATIONS_ATTRIBUTE});
    }
    
    protected List getOperationNames() {
        return Arrays.asList(new String [] {});
    }
    
    /**
     * @return Array of OpenMBeanAttributes.
     * @throws OpenDataException
     */
    protected OpenMBeanAttributeInfo [] createAttributeInfo()
    throws OpenDataException {
        List attributes = new ArrayList();
        attributes.add(new OpenMBeanAttributeInfoSupport(
            CONFIGURATIONS_ATTRIBUTE,
            "Ordered list of configuration references",
            ON_ARRAY_TYPE,
            true, true, false));
        // Need to precreate the array of OpenMBeanAttributeInfos and
        // pass this to attributes.toArray because can't case an Object []
        // array to array of OpenMBeanAttributeInfos without CCE.
        OpenMBeanAttributeInfo [] ombai =
            new OpenMBeanAttributeInfo[attributes.size()];
        attributes.toArray(ombai);
        return ombai;
    }
    
    public Object get(final String attributeName) {
        Object result = null;
        try {
            result = getAttribute(attributeName);
        } catch (AttributeNotFoundException e) {
            e.printStackTrace();
        } catch (MBeanException e) {
            e.printStackTrace();
        } catch (ReflectionException e) {
            e.printStackTrace();
        }
        return result;
    }
}