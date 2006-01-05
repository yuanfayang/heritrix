/* Configuration
 *
 * $Id$
 *
 * Created Dec 28, 2005
 *
 * Copyright (C) 2005 Internet Archive.
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
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.RuntimeOperationsException;
import javax.management.openmbean.ArrayType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenMBeanAttributeInfo;
import javax.management.openmbean.OpenMBeanAttributeInfoSupport;
import javax.management.openmbean.OpenMBeanConstructorInfo;
import javax.management.openmbean.OpenMBeanInfoSupport;
import javax.management.openmbean.OpenMBeanOperationInfo;
import javax.management.openmbean.OpenMBeanOperationInfoSupport;
import javax.management.openmbean.SimpleType;

import org.archive.util.JmxUtils;

/**
 * Configuration for a component homed on a domain.
 * <<abstract>>
 * <p>Based on OpenMBeans.  A Configuration has (MBean) Attributes.</p>
 * <p>Subclasses add reading of definition and population of OpenMBean
 * Attributes from a <i>store</i>.  A subclass might use an xml file to
 * populate a Configuration instance with definition of contained Attributes
 * and their values.</p>
 * @author stack
 * @version $Date$, $Revision$.
 */
public abstract class Configuration
implements DynamicMBean, MBeanRegistration {
    private final static Logger LOGGER =
        Logger.getLogger(Configuration.class.getName());
    
    private static final String NONEXPERT_OPERATION = "nonexpert";
    private static final String OVERRIDEABLE_OPERATION = "overrideable";
    protected static final Object [] TRUE_FALSE_LEGAL_VALUES =
        new Object [] {Boolean.TRUE, Boolean.FALSE};
    public static final String ENABLED_ATTRIBUTE = "Enabled";
    private static final List ATTRIBUTE_NAMES =
        Arrays.asList(new String [] {ENABLED_ATTRIBUTE});
    private Boolean enabled = Boolean.TRUE;
    
    /**
     * Make a String ArrayType to use later in definitions.
     */
    private static ArrayType STR_ARRAY_TYPE;
    static {
        try {
            STR_ARRAY_TYPE = new ArrayType(1, SimpleType.STRING);
        } catch (OpenDataException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * List of nonexpert Attribute names.
     */
    private List nonexpert = null;
    
    /**
     * List of overrideable Attribute names.
     */
    private List overrideables = null;
    
    /**
     * List of all attribute names.
     * Make it protected so accessible in subclass constructors.
     * Make it an ArrayList rather than just a List so have an
     * 'addAll' operation to bulk add attribute names.
     */
    protected ArrayList attributeNames = null;
    
    private List operationNames = null;
   
    private MBeanInfo mbeanInfo = null;
    
    
    public Configuration() throws OpenDataException {
        super();
        this.attributeNames =
            new ArrayList(Arrays.asList(new String [] {ENABLED_ATTRIBUTE}));
        this.nonexpert = getNonexpert();
        this.overrideables = getOverrideable();
        this.operationNames = Arrays.asList(new String [] {
            NONEXPERT_OPERATION, OVERRIDEABLE_OPERATION});
        this.mbeanInfo = createMBeanInfo(this.getClass().getName(),
            "Base abstract settings instance.");
    }
    
    protected List getAttributeNames() {
        return this.attributeNames;
    }
   
    protected List getNonexpert() {
        return getAttributeNames();
    }
    
    protected List getOverrideable() {
        return getAttributeNames();
    }
    
    protected List getOperationNames() {
        return this.operationNames;
    }
    
    /**
     * Create OpenMBeanInfo instance.
     * Called from constructor.  Shouldn't need to override.
     * Override {@link #createAttributeInfo()} and
     * {@link #createOperationInfo()} instead.
     * @param className Full name of class these settings are for.
     * @param description Description of this Settings.
     * @return An OpenMBeanInfo instance.
     * @throws OpenDataException
     */
    protected MBeanInfo createMBeanInfo(final String className,
            final String description)
    throws OpenDataException {
        return new OpenMBeanInfoSupport(className, description,
            createAttributeInfo(),
            new OpenMBeanConstructorInfo [] {},
            createOperationInfo(),
            new MBeanNotificationInfo [] {});
    }
    
    /**
     * @return Array of OpenMBeanAttributes.
     * @throws OpenDataException
     */
    protected OpenMBeanAttributeInfo [] createAttributeInfo()
    throws OpenDataException {
        List attributes = addAttributes(new ArrayList());
        // Need to precreate the array of OpenMBeanAttributeInfos and
        // pass this to attributes.toArray because can't case an Object []
        // array to array of OpenMBeanAttributeInfos without CCE.
        OpenMBeanAttributeInfo [] ombai =
            new OpenMBeanAttributeInfo[attributes.size()];
        attributes.toArray(ombai);
        return ombai;
    }
    
    protected List addAttributes(final List attributes)
    throws OpenDataException {
        attributes.add(new OpenMBeanAttributeInfoSupport(ENABLED_ATTRIBUTE,
            "Enabled if true", SimpleType.BOOLEAN,
            true, true, true, Boolean.TRUE, TRUE_FALSE_LEGAL_VALUES));
        return attributes;
    }
    
    /**
     * @return Array of OpenMBeanOperationInfos.
     * @throws OpenDataException
     */
    protected OpenMBeanOperationInfo[] createOperationInfo()
    throws OpenDataException {
        List operations = new ArrayList();
        if (this.operationNames.contains(NONEXPERT_OPERATION)) {
            operations.add(new OpenMBeanOperationInfoSupport(
                NONEXPERT_OPERATION, "List of all nonexpert Attributes",
                null, STR_ARRAY_TYPE, MBeanOperationInfo.INFO));
        }
        if (this.operationNames.contains(OVERRIDEABLE_OPERATION)) {
            operations.add(new OpenMBeanOperationInfoSupport(
                OVERRIDEABLE_OPERATION,
                "List of all overrideable Attributes", null,
                STR_ARRAY_TYPE, MBeanOperationInfo.INFO));
        }
        OpenMBeanOperationInfo[] omboi =
            new OpenMBeanOperationInfo[operations.size()];
        operations.toArray(omboi);
        return omboi;
    }
    
    protected void checkValidAttributeName(final String attributeName)
    throws AttributeNotFoundException {
        if (attributeName == null) {
            throw new AttributeNotFoundException("Null Attribute name");
        }
        if (!this.attributeNames.contains(attributeName)) {
            throw new AttributeNotFoundException("Unknown Attribute " +
                attributeName);
        }
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
    
    public Integer getInteger(final String attributeName) {
        return (Integer)get(attributeName);
    }
    
    public String getString(final String attributeName) {
        return (String)get(attributeName);
    }
    
    public Object getAttribute(final String attributeName)
    throws AttributeNotFoundException, MBeanException, ReflectionException {
        checkValidAttributeName(attributeName);
        // Else attribute is known.  Is it from this class or a subclass?
        return (ATTRIBUTE_NAMES.contains(attributeName))? this.enabled: null;
    }
    
    public AttributeList getAttributes(String[] attributes) {
        AttributeList result = new AttributeList(attributes.length);
        if (attributes.length == 0) {
            return result;
        }
        for (int i = 0; i < attributes.length; i++) {
            try {
                result.add(new Attribute(attributes[i],
                    getAttribute(attributes[i])));
            } catch (AttributeNotFoundException e) {
                e.printStackTrace();
            } catch (MBeanException e) {
                e.printStackTrace();
            } catch (ReflectionException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public void setAttribute(Attribute attribute)
    throws AttributeNotFoundException, InvalidAttributeValueException,
    MBeanException, ReflectionException {
        checkValidAttributeName(attribute.getName());
        if (attribute.getName().equals(ENABLED_ATTRIBUTE)) {
            this.enabled = (Boolean)attribute.getValue();
        }
        // Else its for subclass to handle.
    }

    public AttributeList setAttributes(AttributeList attributes) {
        AttributeList result = new AttributeList(attributes.size());
        for (final Iterator i = attributes.iterator(); i.hasNext();) {
            try {
                Attribute a = (Attribute) i.next();
                setAttribute(a);
                result.add(a);
            } catch (AttributeNotFoundException e) {
                e.printStackTrace();
            } catch (InvalidAttributeValueException e) {
                e.printStackTrace();
            } catch (MBeanException e) {
                e.printStackTrace();
            } catch (ReflectionException e) {
                e.printStackTrace();
            }
        }
        return result;
    }
    
    public Object invoke(String actionName, Object[] params, String[] signature)
    throws MBeanException, ReflectionException {
        if (!this.operationNames.contains(actionName)) {
            throw new RuntimeOperationsException(
                new RuntimeException(actionName + " unknown operation"));
        }
        // Assume nonexpert... implement. TODO.
        return this.nonexpert.toArray();
    }

    public MBeanInfo getMBeanInfo() {
        return this.mbeanInfo;
    }

    public ObjectName preRegister(MBeanServer server, ObjectName on)
    throws Exception {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(JmxUtils.getServerDetail(server) + " " +
                on.toString());
        }
        return on;
    }

    public void postRegister(Boolean b) {
        // TODO
    }

    public void preDeregister() throws Exception {
        // TODO
    }

    public void postDeregister() {
        // TODO
    }
    
    public static void main(String[] args) {
        // TODO: How to get in context.
    }
}