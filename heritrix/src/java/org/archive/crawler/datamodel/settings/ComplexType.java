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
public class ComplexType implements DynamicMBean {
    private AbstractSettingsHandler settingsHandler;
    private ComplexType parent;
    private String name;
    private String description;
    private String absoluteName;

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

    protected void setAsController(AbstractSettingsHandler settingsHandler) {
        this.settingsHandler = settingsHandler;
        this.parent = null;
        this.absoluteName = name;
        settingsHandler.getSettingsObject(null).addModule((CrawlerModule) this);
        settingsHandler.addToComplexTypeRegistry(this);
        settingsHandler.getSettingsObject(null).addComplexType(this);
        settingsHandler.addToModuleRegistry((CrawlerModule) this);
    }

    protected void setDefaults() {
        // Standard null implementation
    }

    public CrawlerSettings globalSettings() {
        return settingsHandler.getSettingsObject(null);
    }

    public void addElementType(
        String name,
        String description,
        boolean overrideable,
        Object[] legalValues,
        Object defaultValue) {
        addElementType(
            globalSettings(),
            name,
            description,
            overrideable,
            legalValues,
            defaultValue);
    }

    public void addElementType(
        CrawlerSettings settings,
        String name,
        String description,
        boolean overrideable,
        Object[] legalValues,
        Object defaultValue) {
        getDataContainer(settings).addElementType(
            name,
            description,
            overrideable,
            legalValues,
            defaultValue);
    }

    public void addElementType(
        String name,
        String description,
        Object defaultValue) {
        addElementType(name, description, true, null, defaultValue);
    }

    public ComplexType addComplexType(ComplexType object) {
        return addComplexType(globalSettings(), object);
    }

    public ComplexType addComplexType(CrawlerSettings settings, ComplexType object) {
        if(this.settingsHandler == null) {
            throw new IllegalStateException("Can't add ComplexType to 'free' ComplexType");
        }
        object.parent = this;
        object.settingsHandler = getSettingsHandler();
        object.absoluteName = getAbsoluteName() + '/' + object.getName();
        addElementType(settings, object.getName(), object.getDescription(), true, null, object);

        if (settings.getScope() == null) {
            // We're working with the global order file
            getSettingsHandler().addToComplexTypeRegistry(object);
            if (object instanceof CrawlerModule) {
                getSettingsHandler().addToModuleRegistry(
                    (CrawlerModule) object);
            }
        }
        settings.addComplexType(object);

        object.setDefaults();
        return object;
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
        //return null;
        return ((ModuleAttributeInfo) getAttributeInfo(name)).getDefaultValue();
    }

    public Object getAttribute(String name, CrawlURI uri)
        throws AttributeNotFoundException, MBeanException, ReflectionException {
        //return null;
        return ((ModuleAttributeInfo) getAttributeInfo(name)).getDefaultValue();
    }

    /*
    public Object getAttribute(String name, CrawlerSettings settings) {
        DataContainer data = settings.getData(getAbsoluteName());
        if(data == null) {
            try {
                return getAttribute(name);
            } catch (Exception e) {
                throw new IllegalArgumentException("XXX");
            }
        }
        return data.get(name);
    }
    */

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

    public void setAttribute(CrawlerSettings settings, Attribute attribute) {
        DataContainer data = getDataContainer(settings);
        data.put(attribute.getName(), attribute.getValue());
    }

    private DataContainer getDataContainer(CrawlerSettings settings) {
        DataContainer data = settings.getData(getAbsoluteName());
        if (data == null) {
            ComplexType type =
                settingsHandler.getComplexTypeFromRegistry(getAbsoluteName());
            ComplexType parent = type.getParent();
            if (parent == null) {
                settings.addModule((CrawlerModule) type);
            } else {
                DataContainer parentData =
                    settings.getData(parent.getAbsoluteName());
                if (parentData == null) {
                    settings.addModule((CrawlerModule) type);
                } else {
                    globalSettings().getData(parent.getAbsoluteName()).copyAttributeInfo(getName(), parentData);
                }
            }
            data = settings.addComplexType(type);
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
        return settings.getData(getAbsoluteName()).getAttributeInfo(name);
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
}
