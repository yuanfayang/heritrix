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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.ReflectionException;

import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CrawlOrder;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.UURI;
import org.archive.crawler.datamodel.settings.Constraint.FailedCheck;

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
    private static Logger logger =
        Logger.getLogger("org.archive.crawler.datamodel.settings.ComplexType");

    private SettingsHandler settingsHandler;
    private ComplexType parent;
    private String description;
    private String absoluteName;
    private final List definition = new ArrayList();
    protected final Map definitionMap = new HashMap();
    private boolean initialized = false;
    private String[] preservedFields = new String[0];
    
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
        this.description = description.intern();
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
        if (settingsHandler == null) {
            return null;
        }
        return settingsHandler.getSettingsObject(null);
        
//        try {
//            return settingsHandler.getSettingsObject(null);
//        } catch (NullPointerException e) {
//            return null;
//        }
    }

    public Type addElement(CrawlerSettings settings, Type type)
        throws InvalidAttributeValueException {
        getOrCreateDataContainer(settings).addElementType(type);
        if (type instanceof ComplexType) {
            addComplexType(settings, (ComplexType) type);
        }
        return type;
    }

    private ComplexType addComplexType(CrawlerSettings settings,
            ComplexType object) throws InvalidAttributeValueException {

        if (this.settingsHandler == null) {
            throw new IllegalStateException("Can't add ComplexType to 'free' ComplexType");
        }
        setupVariables(object);
        settings.addComplexType(object);
        if (!object.initialized) {
            Iterator it = object.definition.iterator();
            while (it.hasNext()) {
                Type t = (Type) it.next();
                object.addElement(settings, t);
            }
            object.earlyInitialize(settings);
        }
        object.initialized = true;

        return object;
    }

    private ComplexType replaceComplexType(CrawlerSettings settings, ComplexType object) throws InvalidAttributeValueException, AttributeNotFoundException {
        if (this.settingsHandler == null) {
            throw new IllegalStateException("Can't add ComplexType to 'free' ComplexType");
        }
        String[] preservedFields = object.getPreservedFields();
        
        setupVariables(object);
        
        DataContainer oldData = settings.getData(object);
        settings.addComplexType(object);
        DataContainer newData = settings.getData(object);
        
        if (!object.initialized) {
            Iterator it = object.definition.iterator();
            while (it.hasNext()) {
                Type t = (Type) it.next();
                
                // Check if attribute should be copied from old object.
                boolean found = false;
                if (preservedFields.length > 0) {
                    for (int i = 0; i < preservedFields.length; i++) {
                        if (preservedFields[i].equals(t.getName())) {
                            found = true;
                            break;
                        }
                    }
                }
                if (found && oldData.copyAttribute(t.getName(), newData)) {
                    if (t instanceof ComplexType) {
                        object.setupVariables((ComplexType) t);
                    }
                } else {
                    object.addElement(settings, t);
                }
            }
            object.earlyInitialize(settings);
        }
        object.initialized = true;

        return object;
    }
    
    /** Set a list of attribute names that the complex type should attempt to
     * preserve if the module is exchanged with an other one.
     * 
     * @param preservedFields array of attributenames to preserve.
     */
    protected void setPreservedFields(String[] preservedFields) {
        this.preservedFields = preservedFields;
    }
    
    /** Get a list of attribute names that the complex type should attempt to
     * preserve if the module is exchanged with an other one.
     * 
     * @return an array of attributenames to preserve.
     */
    protected String[] getPreservedFields() {
        return this.preservedFields;
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
    private void setupVariables(ComplexType object) {
        object.parent = this;
        object.settingsHandler = getSettingsHandler();
        object.absoluteName =
            (getAbsoluteName() + '/' + object.getName()).intern();
    }

    public SettingsHandler getSettingsHandler() {
        return settingsHandler;
    }

    /** Get the absolute name of this ComplexType.
     *
     * The absolute name is like a file path with the name of the element
     * prepended by all the parents names separated by slashes.
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
    public CrawlerSettings getSettingsFromObject(Object o) {
        CrawlerSettings settings = null;

        if (o instanceof CrawlerSettings) {
            settings = (CrawlerSettings) o;
        } else if (o instanceof CrawlURI) {
            try {
                settings = ((CrawlURI) o).getServer().getSettings();
            } catch (NullPointerException e) {
                // The URI don't know its settings
            }
        } else if (o instanceof UURI || o instanceof CandidateURI) {
            // Try to get settings for URI that has no references to a
            // CrawlServer
            UURI uri = (o instanceof CandidateURI) ? ((CandidateURI) o)
                    .getUURI() : (UURI) o;
            String hostName = uri.getHost() == null ? "" : uri.getHost();
            settings = getSettingsHandler().getSettings(hostName);
        }

        // if settings could not be resolved use globals.
        settings = settings == null ? globalSettings() : settings;
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
        if(uri == null || uri.getServer() == null) {
            settings = globalSettings();
        } else {
            settings = uri.getServer().getSettings();
        }
//        try {
//            settings = uri.getServer().getSettings();
//        } catch (NullPointerException e) {
//            // The URI don't know its settings, use globals
//            settings = globalSettings();
//        }
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

        // If settings is not set, return the default value
        if (settings == null) {
            try {
                return ((Type) definitionMap.get(name)).getDefaultValue();
            } catch (NullPointerException e) {
                throw new AttributeNotFoundException(
                        "Could not find attribute: " + name);
            }
        }
        
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
    public synchronized final void setAttribute(Attribute attribute)
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
    public synchronized final void setAttribute(CrawlerSettings settings,
            Attribute attribute) throws InvalidAttributeValueException,
            AttributeNotFoundException {
        
        if(settings==null){
            settings = globalSettings();
        }

        DataContainer data = getOrCreateDataContainer(settings);
        Object value = attribute.getValue();

        ModuleAttributeInfo attrInfo = (ModuleAttributeInfo) getAttributeInfo(
                settings.getParent(), attribute.getName());

        ModuleAttributeInfo localAttrInfo = (ModuleAttributeInfo) data
                .getAttributeInfo(attribute.getName());

        // Check if attribute exists
        if (attrInfo == null && localAttrInfo == null) {
            throw new AttributeNotFoundException(attribute.getName());
        }

        // Check if we are overriding and if that is allowed for this attribute
        if (localAttrInfo == null) {
            if (!attrInfo.isOverrideable()) {
                throw new InvalidAttributeValueException(
                        "Attribute not overrideable: " + attribute.getName());
            }
            localAttrInfo = new ModuleAttributeInfo(attrInfo);
        }
        
        // Check if value is of correct type. If not, see if it is
        // a string and try to turn it into right type
        Class typeClass = getDefinition(attribute).getLegalValueType();
        if (!(typeClass.isInstance(value)) && value instanceof String) {
            try {
                value = SettingsHandler.StringToType((String) value,
                        SettingsHandler.getTypeName(typeClass.getName()));
            } catch (ClassCastException e) {
                throw new InvalidAttributeValueException(
                        "Unable to decode string '" + value + "' into type '"
                                + typeClass.getName() + "'");
            }
        }

        // If it still isn't a legal type throw an error
        if (!typeClass.isInstance(value)) {
            throw new InvalidAttributeValueException("Value of illegal type: '"
                    + value.getClass().getName() + "', '" + typeClass.getName()
                    + "' was expected");
        }
        
        // Check if the attribute value is legal
        FailedCheck error = checkValue(settings, attribute);
        if (error != null) {
            if (error.getLevel() == Level.SEVERE) {
                throw new InvalidAttributeValueException(error.getMessage());
            } else if (error.getLevel() == Level.WARNING) {
                if (!getSettingsHandler().fireValueErrorHandlers(error)) {
                    throw new InvalidAttributeValueException(error.getMessage());
                }
            } else {
                getSettingsHandler().fireValueErrorHandlers(error);
            }
        }

        // Everything ok, set it
        localAttrInfo.setType(value);
        Object oldValue = data.put(attribute.getName(), localAttrInfo, value);
        
        // If the attribute is a complex type other than the old value,
        // make sure that all sub attributes are correctly set
        if (value instanceof ComplexType && value != oldValue) {
            ComplexType complex = (ComplexType) value;
            replaceComplexType(settings, complex);
        }
    }
    
    /**
     * Get the content type definition for an attribute.
     * 
     * @param attribute the attribut to get the definition for.
     * @return the content type definition for the attribute.
     */
    Type getDefinition(Attribute attribute) {
        return (Type) definitionMap.get(attribute.getName());
    }
    
    /**
     * Check an attribute to see if it fulfills all the constraints set on the
     * definition of this attribute.
     * 
     * @param settings the CrawlerSettings object for which this check was
     *            executed.
     * @param attribute the attribute to check.
     * @return null if everything is ok, otherwise it returns a FailedCheck
     *         object with detailed information of what went wrong.
     */
    public FailedCheck checkValue(CrawlerSettings settings, Attribute attribute) {
        return checkValue(settings, getDefinition(attribute), attribute);
    }

    FailedCheck checkValue(CrawlerSettings settings, Type definition,
            Attribute attribute) {
        FailedCheck res = null;

        // Check if value fulfills any constraints
        List constraints = definition.getConstraints();
        if (constraints != null) {
            for (Iterator it = constraints.iterator(); it.hasNext()
                    && res == null;) {
                res = ((Constraint) it.next()).check(settings, this,
                        definition, attribute);
            }
        }

        return res;
    }

    /** Unset an attribute on a per host level.
     *
     * This methods removes an override on a per host or per domain level.
     *
     * @param settings the settings object for which the attribute should be
     *        unset.
     * @param name the name of the attribute.
     * @return The removed attribute or null if nothing was removed.
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
                settings.addTopLevelModule((ModuleType) this);
            } else {
                DataContainer parentData =
                    settings.getData(parent);
                if (parentData == null) {
                    if (this instanceof ModuleType) {
                        settings.addTopLevelModule((ModuleType) this);
                    } else {
                        settings.addTopLevelModule((ModuleType) parent);
                        try {
                            parent.setAttribute(settings, this);
                        } catch (AttributeNotFoundException e) {
                            logger.severe(e.getMessage());
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
            if (this instanceof ModuleType) {
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
        if( settings == null ){
            // The use global settings
            settings = globalSettings();
        }
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

    /** Get the effective Attribute info for an element of this type from 
     * a settings object.
     * 
     * @param settings the settings object for which the Attribute info is
     *        effective.
     * @param name the name of the element to get the attribute for.
     * @return the attribute info
     */
    public MBeanAttributeInfo getAttributeInfo(CrawlerSettings settings,
            String name) {

        MBeanAttributeInfo info = null;
        
        DataContainer data = getDataContainerRecursive(settings);
        while (data != null && info == null) {
            info = data.getAttributeInfo(name);
            if (info == null) {
                data = getDataContainerRecursive(data.getSettings().getParent());
            }
        }

        return info;
    }
    
    /** Get the Attribute info for an element of this type from the global
     * settings.
     * 
     * @param name the name of the element to get the attribute for.
     * @return the attribute info
     */
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
     * This method can only be called before the ComplexType has been
     * initialized. This usally means that this method is available for
     * constructors of subclasses of this class.
     * 
     * @param type the type to add.
     * @return the newly added type.
     */
    public Type addElementToDefinition(Type type) {
        if (isInitialized()) {
            throw new IllegalStateException(
                    "Elements should only be added to definition in the " +
                    "constructor.");
        }
        if (definitionMap.containsKey(type.getName())) {
            definition.remove(type);
            definitionMap.remove(type.getName());
        }
        definition.add(type);
        definitionMap.put(type.getName(), type);
        return type;
    }
    
    /** Get an element definition from this complex type.
     * 
     * This method can only be called before the ComplexType has been
     * initialized. This usally means that this method is available for
     * constructors of subclasses of this class.
     * 
     * @param name name of element to get.
     * @return the requested element or null if non existent.
     */
    public Type getElementFromDefinition(String name) {
        if (isInitialized()) {
            throw new IllegalStateException(
                    "Elements definition can only be accessed in the " +
                    "constructor.");
        }
        return (Type) definitionMap.get(name);
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
