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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
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
 * Configuration implements DynamicMBean.  It has getAttribute, setAttribute,
 * etc.<p>
 * An odd thing is invoke and perhaps getMBeanInfo.  Latter is ok as
 * means of getting a package of all the Attributes -- their types,
 * descriptions, etc. -- but the invoke is a little odd.  Leave it for
 * now (TODO).
 * @author stack
 * @version $Date$, $Revision$.
 */
public abstract class Configuration
implements DynamicMBean, Registration, Serializable {
    private final Logger LOGGER = Logger.getLogger(this.getClass().getName());
    
    public static final Boolean [] TRUE_FALSE_LEGAL_VALUES =
        new Boolean [] {Boolean.TRUE, Boolean.FALSE};
    
    public static final String ENABLED_ATTRIBUTE = "Enabled";
    
    /**
     * Make a String ArrayType to use later in definitions.
     */
    @SuppressWarnings("unused")
    private static ArrayType<String> STR_ARRAY_TYPE;
    static {
        try {
            STR_ARRAY_TYPE = new ArrayType<String>(1, SimpleType.STRING);
        } catch (OpenDataException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * List of all attribute names.
     * Make it an ArrayList rather than just a List so have an
     * 'addAll' operation to bulk add attribute names.  These are convenience
     * lists. Otherwise would have to parse through MBeanInfo each time.
     */
    private ArrayList<String> attributeNames = new ArrayList<String>();
    
    private ArrayList<String> operationNames = new ArrayList<String>();
   
    /**
     * This is used to package up all of the config. contained herein.
     */
    private MBeanInfo mbeanInfo = null;
    
    /**
     * Cache of attributes.
     */
    private HashMap<String, Object> attributes = new HashMap<String, Object>();
    
    
    public Configuration() throws ConfigurationException {
        super();
        try {
            this.mbeanInfo = createMBeanInfo(this.getClass().getName(),
                "Base abstract configuration instance.");
        } catch (OpenDataException e) {
            throw new ConfigurationException(e);
        }
    }
    
    protected Configuration(final AttributeList al)
    throws ConfigurationException {
    	this();
    	
    }
    
    protected List<String> getAttributeNames() {
        return this.attributeNames;
    }
    
    protected List<String> getOperationNames() {
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
        List<OpenMBeanAttributeInfo> attributeInfos =
        	addAttributeInfos(new ArrayList<OpenMBeanAttributeInfo>());
        // Calculate the attribute names array.
        for (final Iterator<OpenMBeanAttributeInfo> i = attributeInfos.iterator();
        		i.hasNext();) {
        	OpenMBeanAttributeInfo ombai = i.next();
        	this.attributeNames.add(ombai.getName());
        }
        // Need to precreate the array of OpenMBeanAttributeInfos and
        // pass this to attributes.toArray because can't cast an Object []
        // array to array of OpenMBeanAttributeInfos without CCE.
        OpenMBeanAttributeInfo [] ombai =
            new OpenMBeanAttributeInfo[attributeInfos.size()];
        attributeInfos.toArray(ombai);
        return ombai;
    }
    
    protected List<OpenMBeanAttributeInfo> addAttributeInfos(
    	final List<OpenMBeanAttributeInfo> infos)
    throws OpenDataException {
        infos.add(new OpenMBeanAttributeInfoSupport(ENABLED_ATTRIBUTE,
            "Enabled if true", SimpleType.BOOLEAN,
            true, true, true, Boolean.TRUE, TRUE_FALSE_LEGAL_VALUES));
        return infos;
    }
    
    /**
     * @return Array of OpenMBeanOperationInfos.
     * @throws OpenDataException
     */
    @SuppressWarnings("unused")
    protected OpenMBeanOperationInfo[] createOperationInfo()
    throws OpenDataException {
        List<MBeanOperationInfo> operations =
        	addOperations(new ArrayList<MBeanOperationInfo>());
        OpenMBeanOperationInfo[] omboi =
            new OpenMBeanOperationInfo[operations.size()];
        operations.toArray(omboi);
        return omboi;
    }
    
    protected List<MBeanOperationInfo> addOperations(
            ArrayList<MBeanOperationInfo> operations) {
        return operations;
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
    
    @SuppressWarnings("unused")
    public synchronized Object getAttribute(final String attributeName)
    throws AttributeNotFoundException, MBeanException, ReflectionException {
        checkValidAttributeName(attributeName);
        // Else attribute is known.  Do we have it in our cache?  If not,
        // create.
        return this.attributes.containsKey(attributeName)?
            this.attributes.get(attributeName):
            addAttribute(attributeName);
    }
    
    protected Object addAttribute(final String attributeName)
    throws AttributeNotFoundException {
    	MBeanAttributeInfo [] mbai = this.mbeanInfo.getAttributes();
    	Object result = null;
    	for (int i = 0; i < mbai.length; i++) {
    		OpenMBeanAttributeInfo info = (OpenMBeanAttributeInfo)mbai[i];
    		if (info.getName().equals(attributeName)) {
    			result = info.getDefaultValue();
    			// Add to cache.
    			this.attributes.put(attributeName, result);
    			break;
    		}
    	}
    	if (result == null) {
    		throw new AttributeNotFoundException("No value for " +
    		    attributeName);
    	}
    	return result;
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

    @SuppressWarnings("unused")
    public synchronized void setAttribute(Attribute attribute)
    throws AttributeNotFoundException, InvalidAttributeValueException,
    MBeanException, ReflectionException {
        checkValidAttributeName(attribute.getName());
        this.attributes.put(attribute.getName(), attribute.getValue());
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
    
    // Review.  Should this be in the Configuration interface?
    @SuppressWarnings("unused")
    public Object invoke(String actionName,
            @SuppressWarnings("unused") Object[] params,
            @SuppressWarnings("unused") String[] signature)
    throws MBeanException, ReflectionException {
        if (!this.operationNames.contains(actionName)) {
            throw new RuntimeOperationsException(
                new RuntimeException(actionName + " unknown operation"));
        }
        // TODO.
        return null;
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

    public void postRegister(@SuppressWarnings("unused") Boolean b) {
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