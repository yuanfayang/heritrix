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

/** This class holds the data for a ComplexType for a settings object.
 * 
 * @author John Erik Halse
 */
public class DataContainer extends HashMap {
    /** The ComplexType for which this DataContainer keeps data */
    private ComplexType complexType;
    
    /** The Settings object for which this data is valid */
    private CrawlerSettings settings;
    
    /** The attributes defined for this DataContainers combination of
     * ComplexType and CrawlerSettings.
     */
    private List attributes;
    
    /** All attributes that have their value set for this DataContainers
     * combination of ComplexType and CrawlerSettings. This includes overrides.
     */
    private Map attributeNames;

    /** Create a data container for a module.
     * 
     * @param settings Settings to use.
     * @param module the module to create the data container for.
     */
    public DataContainer(CrawlerSettings settings, ComplexType module) {
        super();
        this.settings = settings;
        this.complexType = module;
        attributes = new ArrayList();
        attributeNames = new HashMap();
    }

    /** Add a new element to the data container.
     * 
     * @param name name of the element to add.
     * @param description description ef the element to add.
     * @param overrideable should this element be overrideable.
     * @param legalValues an array of legal values for this element or null if
     *                    there are no constraints.
     * @param defaultValue the default value for this element.
     * @param index index at which the specified element is to be inserted.
     * @throws InvalidAttributeValueException
     */
    public void addElementType(
        String name,
        String description,
        boolean overrideable,
        Object[] legalValues,
        Object defaultValue,
        int index)
        throws InvalidAttributeValueException {
        if (attributeNames.containsKey(name)) {
            throw new IllegalArgumentException("Duplicate field: " + name);
        }
        if (defaultValue == null) {
            throw new InvalidAttributeValueException(
                "null is not allowed as default value for attribute '"
                    + name
                    + "' in class '"
                    + complexType.getClass().getName()
                    + "'");
        }
        MBeanAttributeInfo attribute =
            new ModuleAttributeInfo(
                name,
                defaultValue,
                description,
                overrideable,
                legalValues,
                defaultValue);
        attributes.add(index, attribute);
        attributeNames.put(name, attribute);
        try {
            put(name, defaultValue);
        } catch (InvalidAttributeValueException e) {
            e.printStackTrace();
        } catch (AttributeNotFoundException e) {
            e.printStackTrace();
        }
    }

    /** Appends the specified element to the end of this data container.
     * 
     * @param name name of the element to add.
     * @param description description ef the element to add.
     * @param overrideable should this element be overrideable.
     * @param legalValues an array of legal values for this element or null if
     *                    there are no constraints.
     * @param defaultValue the default value for this element.
     * @throws InvalidAttributeValueException
     */
    public void addElementType(
        String name,
        String description,
        boolean overrideable,
        Object[] legalValues,
        Object defaultValue)
        throws InvalidAttributeValueException {
        addElementType(
            name,
            description,
            overrideable,
            legalValues,
            defaultValue,
            attributes.size());
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

    protected List getLocalAttributeInfoList() {
        return attributes;
    }

    protected boolean hasAttributes() {
        return !attributes.isEmpty();
    }

    public int size() {
        return attributes.size();
    }

    protected MBeanAttributeInfo getAttributeInfo(String name) {
        return (MBeanAttributeInfo) attributeNames.get(name);
    }

    protected void copyAttributeInfo(String name, DataContainer destination) {
        if (this != destination) {
            Object attribute = attributeNames.get(name);
            //destination.attributes.add(attribute);
            destination.attributeNames.put(name, attribute);
        }
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
            attrInfo.setType(value);
            //attributes.add(attrInfo);
            attributeNames.put(key, attrInfo);
        } else {
            value = localAttrInfo.checkValue(value);
            localAttrInfo.setType(value);
        }

        return super.put(key, value);
    }

    /* (non-Javadoc)
     * @see java.util.Map#get(java.lang.Object)
     */
    public Object get(String key) throws AttributeNotFoundException {
        Object res = super.get(key);
        if (res == null && complexType.definitionMap.get(key) == null) {
            throw new AttributeNotFoundException(key);
        }
        return res;
    }
    
    /** Move an attribute up one place in the list. 
     * 
     * @param key name of attribute to move.
     * @return true if attribute was moved, false if attribute was already
     *              at the top.
     * @throws AttributeNotFoundException is thrown if there is no attribute
     *         with the submitted key.
     */
    protected boolean moveElementUp(String key) throws AttributeNotFoundException {
        MBeanAttributeInfo element = getAttributeInfo(key);
        if (element == null) {
            throw new AttributeNotFoundException(key);
        }

        int prevIndex = attributes.indexOf(element);
        if (prevIndex == 0) {
            return false;
        }
        
        attributes.remove(prevIndex);
        attributes.add(prevIndex-1, element);
        
        return true;
    }
    
    /** Move an attribute down one place in the list. 
     * 
     * @param key name of attribute to move.
     * @return true if attribute was moved, false if attribute was already
     *              at bottom.
     * @throws AttributeNotFoundException is thrown if there is no attribute
     *         with the submitted key.
     */
    protected boolean moveElementDown(String key) throws AttributeNotFoundException {
        MBeanAttributeInfo element = getAttributeInfo(key);
        if (element == null) {
            throw new AttributeNotFoundException(key);
        }

        int prevIndex = attributes.indexOf(element);
        if (prevIndex == attributes.size()-1) {
            return false;
        }
        
        attributes.remove(prevIndex);
        attributes.add(prevIndex+1, element);
        
        return true;
    }

    /** Remove an attribute from the map.
     * 
     * @param key name of the attribute to remove.
     * @return the element that was removed.
     * @throws AttributeNotFoundException is thrown if there is no attribute
     *         with the submitted key.
     */
    protected Type removeElement(String key) throws AttributeNotFoundException {
        MBeanAttributeInfo element = getAttributeInfo(key);
        if (element == null) {
            throw new AttributeNotFoundException(key);
        }

        attributes.remove(element);
        attributeNames.remove(element.getName());
        return (Type) super.remove(element.getName()); 
    }
    
    /** Get the ComplexType for which this DataContainer keeps data.
     * 
     * @return the ComplexType for which this DataContainer keeps data.
     */
    protected ComplexType getComplexType() {
        return complexType;
    }

    /** Get the settings object for which this DataContainers data are valid.
     * 
     * @return the settings object for which this DataContainers data are valid.
     */
    protected CrawlerSettings getSettings() {
        return settings;
    }

}
