/* ComplexType
 * 
 * $Id$
 * 
 * Created on Dec 17, 2003
 *
 * Copyright (C) 2004 Internet Archive.
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
package org.archive.crawler.datamodel.settings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.ReflectionException;

import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.datamodel.CrawlURI;

/** Superclass of all configurable modules.
 * 
 * This class is in many ways the heart of the settings framework. All modules
 * that should be configurable extends this class or one of its subclasses.
 * 
 * All subclasses of this class will automatically conform to the
 * JMX DynamicMBean. You could then use the {@link #getMBeanInfo()} method to
 * investigate which attributes this module supports and then use the
 * {@link #getAttribute(String)} and {@link #setAttribute(Attribute)} methods to
 * alter the attributes values.
 * 
 * Because the settings framework supports per domain/host settings there is
 * also available context sensitive versions of the DynamicMBean methods.
 * If you use the non context sensitive methods, it is the global settings
 * that will be altered.
 * 
 * @author John Erik Halse
 */
public abstract class ComplexType extends Type implements DynamicMBean {
    private SettingsHandler settingsHandler;
    private ComplexType parent;
    private String description;
    private String absoluteName;
    private final List definition = new ArrayList();
    protected final Map definitionMap = new HashMap();
    private boolean initialized = false;
    /** Should this ComplexType be serialized to persistent storage */
    private boolean isTransient = false;

    /**
     * Private constructor to make sure that no one 
     * instantiates this class with the empty constructor.
     */
    private ComplexType() {
        super(null, null);
    }

    /** Creates a new instance of ComplexType.
     * 
     * @param name the name of the element.
     * @param description the description of the element.
     */
    public ComplexType(String name, String description) {
        super(name, null);
        this.description = description;
    }

    protected void setAsOrder(SettingsHandler settingsHandler)
        throws InvalidAttributeValueException {
        this.settingsHandler = settingsHandler;
        this.absoluteName = "";
        globalSettings().addTopLevelModule((CrawlOrder) this);
        addComplexType(settingsHandler.getSettingsObject(null), this);
        this.parent = null;
    }

    /** Get the global settings object (aka order).
     * 
     * @return the global settings object.
     */
    public CrawlerSettings globalSettings() {
        return settingsHandler.getSettingsObject(null);
    }

    public Type addElement(CrawlerSettings settings, Type type)
        throws InvalidAttributeValueException {
        getOrCreateDataContainer(settings).addElementType(
            type.getName(),
            type.getDescription(),
            type.getOverrideable(),
            type.getLegalValues(),
            type.getDefaultValue());
        if (type instanceof ComplexType) {
            addComplexType(settings, (ComplexType) type);
        }
        return type;
    }

    private ComplexType addComplexType(
            CrawlerSettings settings, ComplexType object)
            throws InvalidAttributeValueException {
                
        if (this.settingsHandler == null) {
            throw new IllegalStateException("Can't add ComplexType to 'free' ComplexType");
        }
        setupVaiables(object);
        settings.addComplexType(object);
        if (!object.initialized) {
            Iterator it = object.definition.iterator();
            while (it.hasNext()) {
                Type t = (Type) it.next();
                object.definitionMap.put(t.getName(), t);
                object.addElement(settings, t);
            }
            object.earlyInitialize(settings);
        }
        object.initialized = true;

        return object;
    }

    /** Get the active data container for this ComplexType for a specific
     * settings object.
     * 
     * If no value has been overridden on the settings object for this
     * ComplexType, then it traverses up until it find a DataContainer with
     * values for this ComplexType.
     * 
     * This method should probably not be called from user code. It is a helper
     * method for the settings framework.
     * 
     * @param settings the settings object for which the {@link DataContainer}
     *                 is active.
     * @return the active DataContainer.
     */
    protected DataContainer getDataContainerRecursive(CrawlerSettings settings) {
                
        if (settings == null) {
            return null;
        }

        DataContainer data = settings.getData(this);

        if (data == null && settings.getParent() != null) {
            data = getDataContainerRecursive(settings.getParent());
        }

        return data;
    }

    /** Get the active data container for this ComplexType for a specific
     * settings object.
     * 
     * If the key has not been overridden on the settings object for this
     * ComplexType, then it traverses up until it find a DataContainer with
     * the key for this ComplexType.
     *
     * This method should probably not be called from user code. It is a helper
     * method for the settings framework.
     * 
     * @param settings the settings object for which the {@link DataContainer}
     *                 is active.
     * @param key the key to look for.
     * @return the active DataContainer.
     */
    protected DataContainer getDataContainerRecursive(
            CrawlerSettings settings, String key)
            throws AttributeNotFoundException {
                
        DataContainer data = getDataContainerRecursive(settings);
        while (data != null) {
            if (data.containsKey(key)) {
                return data;
            } else {
                data = getDataContainerRecursive(data.getSettings().getParent());
            }
        }
        throw new AttributeNotFoundException(key);
    }

    /** Sets up some variables for a new complex type.
     * 
     * The complex type is set up to be an attribute of
     * this complex type.
     * 
     * @param object to be set up.
     */
    private void setupVaiables(ComplexType object) {
        object.parent = this;
        object.settingsHandler = getSettingsHandler();
        object.absoluteName = getAbsoluteName() + '/' + object.getName();
    }

    public SettingsHandler getSettingsHandler() {
        return settingsHandler;
    }

    /**
     * @return Absolute name.
     */
    public String getAbsoluteName() {
        return absoluteName;
    }

    /** Get settings object valid for a URI.
     * 
     * This method takes an object, try to convert it into a {@link CrawlURI}
     * and then tries to get the settings object from it. If this fails, then
     * the global settings object is returned.
     * 
     * @param o possible {@link CrawlURI}.
     * @return the settings object valid for the URI.
     */
    public CrawlerSettings getSettingsFromUri(Object o) {
        CrawlURI curi = (CrawlURI) ((o instanceof CrawlURI) ? o : null);
        CrawlerSettings settings;
        try {
            settings = curi.getServer().getSettings();
        } catch (NullPointerException e) {
            // The URI don't know its settings, use globals
            settings = globalSettings();
        }
        return settings;
    }
    
    /** Returns true if an element is overridden for this settings object.
     *
     * @param settings the settings object to investigate.
     * @param name the name of the element to check.
     * @return true if element is overridden for this settings object, false
     *              if not set here or is first defined here.
     * @throws AttributeNotFoundException if element doesn't exist.
     */
    public boolean isOverridden(CrawlerSettings settings, String name)
            throws AttributeNotFoundException {
        settings = settings == null ? globalSettings() : settings;
        DataContainer data = settings.getData(this);
        if (data == null || !data.containsKey(name)) {
            return false;
        }

        // Try to find attribute, will throw an exception if not found.
        getDataContainerRecursive(settings.getParent(), name);
        return true;
    }

    /** Obtain the value of a specific attribute from the crawl order.
     * 
     * If the attribute doesn't exist in the crawl order, the default
     * value will be returned.
     * 
     * @param name the name of the attribute to be retrieved.
     * @return The value of the attribute retrieved.
     * @throws AttributeNotFoundException
     * @throws MBeanException
     * @throws ReflectionException
     */
    public Object getAttribute(String name)
        throws AttributeNotFoundException, MBeanException, ReflectionException {
        return getAttribute(null, name);
    }

    /** Obtain the value of a specific attribute that is valid for a
     * specific CrawlURI.
     * 
     * This method will try to get the attribute from the host settings
     * valid for the CrawlURI. If it is not found it will traverse the
     * settings up to the order and as a last resort deliver the default
     * value. This is also the case if the CrawlURI is null or if the CrawlURI
     * hasn't been assigned a CrawlServer.
     * 
     * @param name the name of the attribute to be retrieved.
     * @param uri the CrawlURI that this attribute should be valid for.
     * @return The value of the attribute retrieved.
     * @see #getAttribute(CrawlerSettings settings, String name)
     * @throws AttributeNotFoundException
     */
    public Object getAttribute(String name, CrawlURI uri)
        throws AttributeNotFoundException {
        CrawlerSettings settings;
        try {
            settings = uri.getServer().getSettings();
        } catch (NullPointerException e) {
            // The URI don't know its settings, use globals
            settings = globalSettings();
        }
        return getAttribute(settings, name);
    }

    /** Obtain the value of a specific attribute that is valid for a
     * specific CrawlerSettings object.
     * 
     * This method will try to get the attribute from the supplied host
     * settings object. If it is not found it will traverse the settings
     * up to the order and as a last resort deliver the default value.
     * 
     * @param settings the CrawlerSettings object to search for this attribute.
     * @param name the name of the attribute to be retrieved.
     * @return The value of the attribute retrieved.
     * @see CrawlerSettings
     * @throws AttributeNotFoundException
     */
    public Object getAttribute(CrawlerSettings settings, String name)
        throws AttributeNotFoundException {
        settings = settings == null ? globalSettings() : settings;

        return getDataContainerRecursive(settings, name).get(name);
    }

    /** Obtain the value of a specific attribute that is valid for a
     * specific CrawlerSettings object.
     * 
     * This method will try to get the attribute from the supplied host
     * settings object. If it is not found it will return <code>null</code>
     * and not try to investigate the hierarchy of settings.  
     *  
     * @param settings the CrawlerSettings object to search for this attribute.
     * @param name the name of the attribute to be retrieved.
     * @return The value of the attribute retrieved or null if its not set.
     * @see CrawlerSettings
     * @throws AttributeNotFoundException is thrown if the attribute doesn't
     *         exist.
     */
    public Object getLocalAttribute(CrawlerSettings settings, String name)
            throws AttributeNotFoundException {

        settings = settings == null ? globalSettings() : settings;

        DataContainer data = settings.getData(this);
        if (data != null && data.containsKey(name)) {
            // Attribute was found return it.
            return data.get(name);
        } else {
            // Try to find the attribute, will throw an exception if not found.
            getDataContainerRecursive(settings, name);
            return null;
        }
    }

    /** Set the value of a specific attribute of the ComplexType.
     * 
     * This method sets the specific attribute for the order file.
     * 
     * @param attribute The identification of the attribute to be set and the
     *                  value it is to be set to.
     * @throws AttributeNotFoundException is thrown if there is no attribute
     *         with this name.
     * @throws InvalidAttributeValueException is thrown if the attribute is of
     *         wrong type and cannot be converted to the right type.
     * @throws MBeanException this is to conform to the MBean specification, but
     *         this exception is never thrown, though this might change in the
     *         future.
     * @throws ReflectionException this is to conform to the MBean specification, but
     *         this exception is never thrown, though this might change in the
     *         future.
     * @see javax.management.DynamicMBean#setAttribute(javax.management.Attribute)
     */
    public void setAttribute(Attribute attribute)
        throws
            AttributeNotFoundException,
            InvalidAttributeValueException,
            MBeanException,
            ReflectionException {
        setAttribute(settingsHandler.getSettingsObject(null), attribute);
    }

    /** Set the value of a specific attribute of the ComplexType.
     * 
     * This method is an extension to the Dynamic MBean specification so that
     * it is possible to set the value for a CrawlerSettings object other than
     * the settings object representing the order.
     * 
     * @param settings the settings object for which this attributes value is valid
     * @param attribute The identification of the attribute to be set and the
     *                  value it is to be set to.
     * @throws AttributeNotFoundException is thrown if there is no attribute
     *         with this name.
     * @throws InvalidAttributeValueException is thrown if the attribute is of
     *         wrong type and cannot be converted to the right type.
     * @see javax.management.DynamicMBean#setAttribute(javax.management.Attribute)
     */
    public void setAttribute(CrawlerSettings settings, Attribute attribute)
        throws InvalidAttributeValueException, AttributeNotFoundException {

        DataContainer data = getOrCreateDataContainer(settings);
        Object value = attribute.getValue();
        Object oldValue = data.put(attribute.getName(), value);

        if (value instanceof ComplexType && value != oldValue) {
            ComplexType complex = (ComplexType) value;
            setupVaiables(complex);
            addComplexType(settings, complex);
        }
    }
    
    /** Unset an attribute on a per host level.
     * 
     * This methods removes an override on a per host or per domain level.
     *
     * @param settings the settings object for which the attribute should be
     *        unset.
     * @param name the name of the attribute.
     * @throws AttributeNotFoundException is thrown if the attribute name
     *         doesn't exist.
     */
    public Object unsetAttribute(CrawlerSettings settings, String name)
            throws AttributeNotFoundException {

        if (settings == globalSettings()) {
            throw new IllegalArgumentException(
                "Not allowed to unset attributes in Crawl Order.");
        }

        DataContainer data = settings.getData(this);
        if (data != null && data.containsKey(name)) {
            // Remove value
            return data.removeElement(name);
        }

        // Value not found. Check if we should return null or throw an exception
        // This method throws an exception if not found.
        getDataContainerRecursive(settings, name);
        return null;
    }

    private DataContainer getOrCreateDataContainer(CrawlerSettings settings)
        throws InvalidAttributeValueException {

        // Get this ComplexType's data container for the submitted settings
        DataContainer data = settings.getData(this);

        // If there isn't a container, create one
        if (data == null) {
            ComplexType parent = getParent();
            if (parent == null) {
                settings.addTopLevelModule((CrawlerModule) this);
            } else {
                DataContainer parentData =
                    settings.getData(parent);
                if (parentData == null) {
                    if (this instanceof CrawlerModule) {
                        settings.addTopLevelModule((CrawlerModule) this);
                    } else {
                        settings.addTopLevelModule((CrawlerModule) parent);
                        try {
                            parent.setAttribute(settings, this);
                        } catch (AttributeNotFoundException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                } else {
                    globalSettings().getData(parent).copyAttributeInfo(
                        getName(),
                        parentData);
                }
            }

            // Create fresh DataContainer
            data = settings.addComplexType(this);
        }

        // Make sure that the DataContainer references right type
        if (data.getComplexType() != this) {
            if (this instanceof CrawlerModule) {
                data = settings.addComplexType(this);
            }
        }
        return data;
    }

    /* (non-Javadoc)
     * @see javax.management.DynamicMBean#getAttributes(java.lang.String[])
     */
    public AttributeList getAttributes(String[] name) {
        return null;
    }

    /* (non-Javadoc)
     * @see javax.management.DynamicMBean#setAttributes(javax.management.AttributeList)
     */
    public AttributeList setAttributes(AttributeList attributes) {
        return null;
    }

    /* (non-Javadoc)
     * @see javax.management.DynamicMBean#invoke(java.lang.String, java.lang.Object[], java.lang.String[])
     */
    public Object invoke(String arg0, Object[] arg1, String[] arg2)
        throws MBeanException, ReflectionException {
        throw new ReflectionException(
            new NoSuchMethodException("No methods to invoke."));
    }

    /* (non-Javadoc)
     * @see javax.management.DynamicMBean#getMBeanInfo()
     */
    public MBeanInfo getMBeanInfo() {
        return getMBeanInfo(globalSettings());
    }

    public MBeanInfo getMBeanInfo(CrawlerSettings settings) {
        Stack attributeStack = new Stack();
        int attributeCount = 0;
        DataContainer data = getDataContainerRecursive(settings);
        while (data != null) {
            attributeStack.push(data.getLocalAttributeInfoList());
            attributeCount += data.getLocalAttributeInfoList().size();
            data = getDataContainerRecursive(data.getSettings().getParent());
        }
        MBeanAttributeInfo[] attributes = new MBeanAttributeInfo[attributeCount];
        int offset = 0;
        while (!attributeStack.isEmpty()) {
            List attList = (List) attributeStack.pop();
            for (int i=0; i<attList.size(); i++) {
                attributes[offset + i] = (MBeanAttributeInfo) attList.get(i);
            }
            offset += attList.size();
        }

        MBeanInfo info =
            new MBeanInfo(getClass().getName(), getDescription(), attributes,
                null, null, null);
        return info;
    }

    public MBeanAttributeInfo getAttributeInfo(
        CrawlerSettings settings,
        String name) {
        try {
            return settings.getData(this).getAttributeInfo(name);
        } catch (NullPointerException e) {
            return null;
        }
    }

    public MBeanAttributeInfo getAttributeInfo(String name) {
        return getAttributeInfo(globalSettings(), name);
    }

    /** Get the description of this type
     * 
     * The description should be suitable for showing in a user interface.
     * 
     * @return this type's description
     */
    public String getDescription() {
        return description;
    }

    /** Get the parent of this ComplexType.
     * 
     * @return the parent of this ComplexType.
     */
    public ComplexType getParent() {
        return parent;
    }

    /** Set the description of this ComplexType
     * 
     * The description should be suitable for showing in a user interface.
     * 
     * @param string the description to set for this type.
     */
    public void setDescription(String string) {
        description = string;
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.datamodel.settings.Type#getDefaultValue()
     */
    public Object getDefaultValue() {
        return this;
    }

    /** Add a new attribute to the definition of this ComplexType.
     * 
     * @param type the type to add.
     * @return the newly added type.
     */
    public Type addElementToDefinition(Type type) {
        definition.add(type);
        return type;
    }

    /** Initializes this ComplexType with it's defined attributes and
     * default values.
     * 
     * @param settings the CrawlerSettings object for which this
     *        complex type is defined.
     * @throws InvalidAttributeValueException is thrown if default values
     *         is of wrong type.
     *//*
    private void initializeComplexType(CrawlerSettings settings)
        throws InvalidAttributeValueException {
        if (!initialized) {
            Iterator it = definition.iterator();
            while (it.hasNext()) {
                Type t = (Type) it.next();
                definitionMap.put(t.getName(), t);
                addElement(settings, t);
            }
            earlyInitialize(settings);
        }
        initialized = true;
    }*/

    /** This method can be overridden in subclasses to do local
     * initialisation.
     * 
     * This method is run before the class has been updated with
     * information from settings files. That implies that if you
     * call getAttribute inside this method you will only get the
     * default values.
     * 
     * @param settings the CrawlerSettings object for which this
     *        complex type is defined.
     */
    public void earlyInitialize(CrawlerSettings settings) {
    }

    /** Returns true if this ComplexType is initialized.
     * 
     * @return true if this ComplexType is initialized.
     */
    public boolean isInitialized() {
        return initialized;
    }

    public Object[] getLegalValues() {
        return null;
    }

    /** Returns true if this ComplexType should be saved to persistent storage.
     * 
     * @return true if this ComplexType should be saved to persistent storage.
     */
    public boolean isTransient() {
        return isTransient;
    }

    /** Set to false if this attribute should not be serialized to persistent
     * storage.
     * 
     * @param b if false this complexType will not be saved to persistent
     *          storage.
     */
    public void setTransient(boolean b) {
        isTransient = b;
    }

    /** Returns this object.
     * 
     * This method is implemented to be able to treat the ComplexType as an
     * subclass of {@link javax.management.Attribute}.
     * 
     * @return this object.
     * @see javax.management.Attribute#getValue()
     */
    public Object getValue() {
        return this;
    }

}
