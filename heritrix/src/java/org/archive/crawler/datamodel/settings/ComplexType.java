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
import java.util.Iterator;

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

/**
 * 
 * @author John Erik Halse
 */
public abstract class ComplexType implements DynamicMBean, Type {
    private AbstractSettingsHandler settingsHandler;
    private ComplexType parent;
    private String name;
    private String description;
    private String absoluteName;
    private final ArrayList definition = new ArrayList();
    private boolean initialized = false;

    /**
     * Private constructor to make sure that no one 
     * instantiates this class with the empty constructor.
     */
    private ComplexType() {
    }

    public ComplexType(String name, String description) {
        this.name = name;
        this.description = description;
    }

    protected void setAsController(AbstractSettingsHandler settingsHandler) throws InvalidAttributeValueException {
        this.settingsHandler = settingsHandler;
        this.parent = null;
        this.absoluteName = name;
        settingsHandler.getSettingsObject(null).addModule((CrawlerModule) this);
        settingsHandler.addToComplexTypeRegistry(this);
        settingsHandler.getSettingsObject(null).addComplexType(this);
        settingsHandler.addToModuleRegistry((CrawlerModule) this);
        initialize();
    }

    public CrawlerSettings globalSettings() {
        return settingsHandler.getSettingsObject(null);
    }

    public Type addElement(CrawlerSettings settings, Type type) throws InvalidAttributeValueException {
        getDataContainer(settings).addElementType(
            type.getName(),
            type.getDescription(),
            true,
            null,
            type.getDefaultValue());
        if (type instanceof ComplexType) {
            addComplexType(settings, (ComplexType) type);
        }
        return type;
    }

    private ComplexType addComplexType(
        CrawlerSettings settings,
        ComplexType object) throws InvalidAttributeValueException {
        if (this.settingsHandler == null) {
            throw new IllegalStateException("Can't add ComplexType to 'free' ComplexType");
        }
        initComplexType(object);

        if (settings.getScope() == null) {
            // We're working with the global order file
            getSettingsHandler().addToComplexTypeRegistry(object);
            if (object instanceof CrawlerModule) {
                getSettingsHandler().addToModuleRegistry(
                    (CrawlerModule) object);
            }
        }

        settings.addComplexType(object);
        object.initialize();

        return object;
    }
    
    /** Set initialation parameters for a complex type
     * 
     * @param object to be initialized
     */
    private void initComplexType(ComplexType object) {
        object.parent = this;
        object.settingsHandler = getSettingsHandler();
        object.absoluteName = getAbsoluteName() + '/' + object.getName();
    }

    public AbstractSettingsHandler getSettingsHandler() {
        return settingsHandler;
    }

    public String getName() {
        return name;
    }

    /**
     * @return
     */
    protected String getAbsoluteName() {
        return absoluteName;
    }

    /* (non-Javadoc)
     * @see javax.management.DynamicMBean#getAttribute(java.lang.String)
     */
    public Object getAttribute(String name)
        throws AttributeNotFoundException, MBeanException, ReflectionException {
        //return ((ModuleAttributeInfo) getAttributeInfo(name)).getDefaultValue();
        return getAttribute(null, name);
    }

    public Object getAttribute(String name, CrawlURI uri)
        throws AttributeNotFoundException, MBeanException, ReflectionException {
            // TODO: FIX THIS
        return ((ModuleAttributeInfo) getAttributeInfo(name)).getDefaultValue();
    }

    public Object getAttribute(CrawlerSettings settings, String name) throws AttributeNotFoundException {
        Object res = getLocalAttribute(settings, name);

        if (res == null && settings != null && settings.getScope() != null) {
            res = getLocalAttribute(null, name);
        }

        if (res == null) {
            ModuleAttributeInfo attInfo =
                (ModuleAttributeInfo) getAttributeInfo(settings, name);
            if (attInfo == null) {
                attInfo = (ModuleAttributeInfo) getAttributeInfo(name);
            }
            res = attInfo.getDefaultValue();
        }

        return res;
    }

    public Object getLocalAttribute(CrawlerSettings settings, String name) throws AttributeNotFoundException {
        if (settings == null) {
            settings = globalSettings();
        }
        DataContainer data = settings.getData(getAbsoluteName());
        if (data == null) {
            return null;
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

    public void setAttribute(CrawlerSettings settings, Attribute attribute) throws InvalidAttributeValueException, AttributeNotFoundException {
        DataContainer data = getDataContainer(settings);
        
        if (attribute.getValue() instanceof ComplexType) {
            initComplexType((ComplexType) attribute.getValue());
        }
        
        data.put(attribute.getName(), attribute.getValue());
    }

    private DataContainer getDataContainer(CrawlerSettings settings) throws InvalidAttributeValueException {
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
                    globalSettings().getData(
                        parent.getAbsoluteName()).copyAttributeInfo(
                        getName(),
                        parentData);
                }
            }
            
            // Create fresh DataContainer
            data = settings.addComplexType(this);
        }
        
        // Make sure that the DataContainer references right type
        if(data.complexType != this) {
            if (this instanceof CrawlerModule) {
                data.complexType = (ComplexType) this;
                data.complexType.initialize();
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

    protected MBeanAttributeInfo getAttributeInfo(
        CrawlerSettings settings,
        String name) {
        try {
            return settings.getData(getAbsoluteName()).getAttributeInfo(name);
        } catch (NullPointerException e) {
            return null;
        }
    }

    protected MBeanAttributeInfo getAttributeInfo(String name) {
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

    protected void initialize() throws InvalidAttributeValueException {
        Iterator it = definition.iterator();
        while (it.hasNext()) {
            addElement(globalSettings(), (Type) it.next());
        }
        initialized = true;
    }

    public boolean initialized() {
        return initialized;
    }
}
