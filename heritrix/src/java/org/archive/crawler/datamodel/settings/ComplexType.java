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

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.ReflectionException;

import org.archive.crawler.datamodel.CrawlURI;

/** Superclass of all configurable modules.
 * 
 * This class is in many ways the heart of the settings framework. All modules
 * that should be configurable extends this class or one of its subclasses.
 * 
 * All subclasses of this class will automatically conform to the
 * JMX DynamicMBean. You could then use the @link #getMBeanInfo() method to
 * investigate which attributes this module supports and then use the
 * @link #getAttribute(String) and @link #setAttribute(Attribute) methods to
 * alter the attributes values.
 * 
 * Because the settings framework supports per domain/host settings there is
 * also available context sensitive versions of the DynamicMBean methods.
 * If you use the non context sensitive methods, it is the global settings
 * that will be altered.
 * 
 * @author John Erik Halse
 */
public abstract class ComplexType implements DynamicMBean, Type {
    private SettingsHandler settingsHandler;
    private ComplexType parent;
    private String name;
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
    }

    /** Creates a new instance of ComplexType.
     * 
     * @param name the name of the element.
     * @param description the description of the element.
     */
    public ComplexType(String name, String description) {
        this.name = name;
        this.description = description;
    }

    protected void setAsOrder(SettingsHandler settingsHandler)
        throws InvalidAttributeValueException {
        this.settingsHandler = settingsHandler;
        this.parent = null;
        this.absoluteName = name;
        settingsHandler.getSettingsObject(null).addModule((CrawlerModule) this);
        settingsHandler.addToComplexTypeRegistry(this);
        settingsHandler.getSettingsObject(null).addComplexType(this);
        settingsHandler.addToModuleRegistry((CrawlerModule) this);
        initializeComplexType(globalSettings());
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
        getDataContainer(settings).addElementType(
            type.getName(),
            type.getDescription(),
            true,
            type.getLegalValues(),
            type.getDefaultValue());
        if (type instanceof ComplexType) {
            addComplexType(settings, (ComplexType) type);
        }
        return type;
    }

    private ComplexType addComplexType(
        CrawlerSettings settings,
        ComplexType object)
        throws InvalidAttributeValueException {
        if (this.settingsHandler == null) {
            throw new IllegalStateException("Can't add ComplexType to 'free' ComplexType");
        }
        setupVaiables(object);

        if (settings.getScope() == null) {
            // We're working with the global order file
            getSettingsHandler().addToComplexTypeRegistry(object);
            if (object instanceof CrawlerModule) {
                getSettingsHandler().addToModuleRegistry(
                    (CrawlerModule) object);
            }
        }

        settings.addComplexType(object);
        object.initializeComplexType(settings);

        return object;
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

    public String getName() {
        return name;
    }

    /**
     * @return
     */
    public String getAbsoluteName() {
        return absoluteName;
    }

    /** Obtain the value of a specific attribute from the crawl order.
     * 
     * If the attribute doesn't exist in the crawl order, the default
     * value will be returned.
     * 
     * @param name the name of the attribute to be retrieved.
     * @return The value of the attribute retrieved.
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
     */
    public Object getAttribute(String name, CrawlURI uri)
        throws AttributeNotFoundException {
        CrawlerSettings settings;
        try {
            settings = uri.getServer().getSettings();
        } catch (NullPointerException e) {
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
     */
    public Object getAttribute(CrawlerSettings settings, String name)
        throws AttributeNotFoundException {
        settings = settings == null ? globalSettings() : settings;

        Object res;
        try {
            res = getLocalAttribute(settings, name);
        } catch (AttributeNotFoundException e) {
            res = null;
        }

        if (res == null) {
            // If value wasn't found try recurse up settings hierarchy
            if (settings != null && settings.getParent() != null) {
                res = getAttribute(settings.getParent(), name);
            } else {
                throw new AttributeNotFoundException(name);
            }
        }
        return res;
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
     * @return The value of the attribute retrieved.
     * @see CrawlerSettings
     */
    public Object getLocalAttribute(CrawlerSettings settings, String name)
        throws AttributeNotFoundException {
        if (settings == null) {
            settings = globalSettings();
        }
        DataContainer data = settings.getData(getAbsoluteName());
        if (data == null) {
            //return null;
            throw new AttributeNotFoundException(name);
        }
        return data.get(name);
    }

    /* (non-Javadoc)
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

    public void setAttribute(CrawlerSettings settings, Attribute attribute)
        throws InvalidAttributeValueException, AttributeNotFoundException {
            setAttribute(settings, attribute.getName(), attribute.getValue());
    }

    public void setAttribute(Type element)
        throws AttributeNotFoundException, InvalidAttributeValueException {
        setAttribute(settingsHandler.getSettingsObject(null), element);
    }

    public void setAttribute(CrawlerSettings settings, Type element)
        throws InvalidAttributeValueException, AttributeNotFoundException {
            setAttribute(settings, element.getName(), element);
    }

    private void setAttribute(CrawlerSettings settings, String name, Object value)
        throws InvalidAttributeValueException, AttributeNotFoundException {
        DataContainer data = getDataContainer(settings);

        Object oldValue = data.put(name, value);

        if (value instanceof ComplexType && value != oldValue) {
            ComplexType object = (ComplexType) value;
            setupVaiables((ComplexType) value);
            //object.initializeComplexType(settings);
            addComplexType(settings, object);
        }

    }

    private DataContainer getDataContainer(CrawlerSettings settings)
        throws InvalidAttributeValueException {
        // Get this ComplexType's data container for the submitted settings
        DataContainer data = settings.getData(getAbsoluteName());

        // If there isn't a container, create one
        if (data == null) {
            ComplexType parent = getParent();
            if (parent == null) {
                settings.addModule((CrawlerModule) this);
            } else {
                DataContainer parentData =
                    settings.getData(parent.getAbsoluteName());
                if (parentData == null) {
                    settings.addModule((CrawlerModule) this);
                } else {
                    globalSettings().getData(parent.getAbsoluteName()).copyAttributeInfo(getName(), parentData);
                }
            }

            // Create fresh DataContainer
            data = settings.addComplexType(this);
        }

        // Make sure that the DataContainer references right type
        if (data.complexType != this) {
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
        return settings.getData(getAbsoluteName()).getMBeanInfo();
    }

    public MBeanAttributeInfo getAttributeInfo(
        CrawlerSettings settings,
        String name) {
        try {
            return settings.getData(getAbsoluteName()).getAttributeInfo(name);
        } catch (NullPointerException e) {
            return null;
        }
    }

    public MBeanAttributeInfo getAttributeInfo(String name) {
        return getAttributeInfo(globalSettings(), name);
    }

    /**
     * @return
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return
     */
    public ComplexType getParent() {
        return parent;
    }

    /**
     * @param string
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
     */
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
    }
    
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
    
    /**
     * @return
     */
    public boolean isTransient() {
        return isTransient;
    }

    /**
     * @param b
     */
    public void setTransient(boolean b) {
        isTransient = b;
    }

}
