/* DataContainer
 * 
 * $Id$
 * 
 * Created on Dec 17, 2003
 *
 * Copyright (C) 2003 Internet Archive.
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
 */
package org.archive.crawler.datamodel.settings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;

/** 
 * 
 * @author John Erik Halse
 */
public class DataContainer extends HashMap {
    protected ComplexType complexType;
    private List attributes;
    private Map attributeNames;

    /**
     * 
     */
    public DataContainer(ComplexType module) {
        super();
        this.complexType = module;
        attributes = new ArrayList();
        attributeNames = new HashMap();
    }

    public void addElementType(
        String name,
        String description,
        boolean overrideable,
        Object[] legalValues,
        Object defaultValue)
        throws InvalidAttributeValueException {
        if (attributeNames.containsKey(name)) {
            throw new IllegalArgumentException("Duplicate field: " + name);
        }
        MBeanAttributeInfo attribute =
            new ModuleAttributeInfo(
                name,
                defaultValue,
                description,
                overrideable,
                legalValues,
                defaultValue);
        attributes.add(attribute);
        attributeNames.put(name, attribute);
        try {
            put(name, defaultValue);
        } catch (InvalidAttributeValueException e) {
            e.printStackTrace();
        } catch (AttributeNotFoundException e) {
            e.printStackTrace();
        }
    }

    public MBeanInfo getMBeanInfo() {
        MBeanAttributeInfo attrs[] =
            (MBeanAttributeInfo[]) attributes.toArray(
                new MBeanAttributeInfo[0]);
        MBeanInfo info =
            new MBeanInfo(
                complexType.getClass().getName(),
                complexType.getDescription(),
                attrs,
                null,
                null,
                null);
        return info;
    }

    protected MBeanAttributeInfo getAttributeInfo(String name) {
        return (MBeanAttributeInfo) attributeNames.get(name);
    }

    protected void copyAttributeInfo(String name, DataContainer destination) {
        Object attribute = attributeNames.get(name);
        destination.attributes.add(attribute);
        destination.attributeNames.put(name, attribute);
    }

    public Object put(Object key, Object value) {
        throw new UnsupportedOperationException();
    }

    public Object get(Object key) {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */
    protected Object put(String key, Object value)
        throws InvalidAttributeValueException, AttributeNotFoundException {
        ModuleAttributeInfo attrInfo =
            (ModuleAttributeInfo) complexType.getAttributeInfo((String) key);
        ModuleAttributeInfo localAttrInfo =
            (ModuleAttributeInfo) getAttributeInfo((String) key);

        if (attrInfo == null && localAttrInfo == null) {
            throw new AttributeNotFoundException(key);
        }

        if (localAttrInfo == null) {
            value = attrInfo.checkValue(value);
            attributes.add(attrInfo);
            attributeNames.put(key, attrInfo);
        } else {
            value = localAttrInfo.checkValue(value);
        }

        return super.put(key, value);
    }

    /* (non-Javadoc)
     * @see java.util.Map#get(java.lang.Object)
     */
    public Object get(String key) throws AttributeNotFoundException {
        if (complexType.definitionMap.get(key) == null) {
            throw new AttributeNotFoundException(key);
        }
        return super.get(key);
    }
}
