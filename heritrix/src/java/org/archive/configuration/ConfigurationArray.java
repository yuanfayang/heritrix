/* ConfigurationArray
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

import java.util.Arrays;
import java.util.List;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.ReflectionException;
import javax.management.openmbean.ArrayType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenMBeanAttributeInfo;
import javax.management.openmbean.OpenMBeanAttributeInfoSupport;

/**
 * Holds array of Configurations.
 * 
 * @author stack
 * @version $Date$ $Revision$
 */
public abstract class ConfigurationArray extends Configuration {
    public static final String ARRAYS_ATTRIBUTE = "Array";
    private static final List<String> ATTRIBUTE_NAMES =
        Arrays.asList(new String [] {ARRAYS_ATTRIBUTE});
    private Object [] array = null;
    
    public ConfigurationArray() throws OpenDataException {
        super();
        // Add my attributes to those of the base.
        getAttributeNames().addAll(ATTRIBUTE_NAMES);
    }
    
    protected List<OpenMBeanAttributeInfo> addAttributes(List attributes)
    throws OpenDataException {
        attributes = super.addAttributes(attributes);
        attributes.add(new OpenMBeanAttributeInfoSupport(
            ARRAYS_ATTRIBUTE,
            getArrayDescription(),
            getArrayType(),
            true, true, false));
        return attributes;
    }
    
    protected String getArrayDescription() {
        return "Ordered list";
    }
    
    /**
     * Has to be one of the OpenMBean types if its to show up as
     * other than 'unknown' in a remote JMX client.
     * @return The ArrayType for this ConfigurationArray.
     * @throws OpenDataException
     */
    protected abstract ArrayType getArrayType()
    throws OpenDataException;
    
    public Object getAttribute(String attributeName)
    throws AttributeNotFoundException, MBeanException, ReflectionException {
        Object obj = super.getAttribute(attributeName);
        if (obj != null) {
            // Found the attribute in the parent.
            return obj;
        }
        // Assume its the array.  Return it.
        return this.array;
    }
    
    public void setAttribute(Attribute attribute)
    throws AttributeNotFoundException, InvalidAttributeValueException,
    MBeanException, ReflectionException {
        super.setAttribute(attribute);
        if (attribute.getName().equals(ARRAYS_ATTRIBUTE)) {
            this.array = (Object [])attribute.getValue();
        }
    }
}