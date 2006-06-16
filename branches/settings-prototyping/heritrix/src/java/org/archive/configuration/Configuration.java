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
 * Configuration for a named component homed on a domain.
 * <<abstract>>
 * <p>This class has settings and description of whats 
 * settable, defaults, and value ranges. Uses OpenMBeans.
 * Implements DynamicMBean. Subclasses are meant to add
 * the Attributes particular to the associated {@link Configurable}.
 * Use the (DynamicMBean) getAttribute, setAttribute,
 * etc., to set/get values (or {@link #getMBeanInfo()} to get a
 * concise description of a particular Configuration instance.
 * This class adds convenience methods.<p>
 * See the package documentation for examples on how you'd add a
 * Configuration to a Configurable.<p>
 * An anamoly that comes of our implementing DynamicMBean is
 * {@link #invoke(String, Object[], String[])} (and being able to
 * describe constructors, etc.). Leave it for now.  Perhaps shut
 * it down? TODO.
 * <p>TODO: Should I add list of expert and overrideables here or
 * in subclass?
 * @author stack
 * @version $Date$, $Revision$.
 */
public class Configuration
implements DynamicMBean, Registration, Serializable {
    private static final long serialVersionUID = -1092504843609309721L;
    private final Logger LOGGER = Logger.getLogger(this.getClass().getName());
    
    public static final String NAME_KEY = "name";
    public static final String TYPE_KEY = "type";
    
    public static final Boolean [] TRUE_FALSE_LEGAL_VALUES =
        new Boolean [] {Boolean.TRUE, Boolean.FALSE};
    
    /**
     * This base Configuration adds the Enabled attribute, expert
     * list and overrideable list.
     */
    public static final String ATTR_ENABLED = "Enabled";
    public static final String ATTR_EXPERT = "Expert";
    public static final String ATTR_NO_OVERRIDE = "NotOverrideable";
    
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
    private final MBeanInfo mbeanInfo;
    
    /**
     * Cache of attributes.
     */
    private HashMap<String, Object> attributes =
        new HashMap<String, Object>();
    
    /**
     * Make a String ArrayType used later in subclass definitions.
     */
    @SuppressWarnings("unused")
    public static ArrayType STR_ARRAY_TYPE;
    static {
    	    try {
    		    STR_ARRAY_TYPE = new ArrayType(1, SimpleType.STRING);
    	    } catch (OpenDataException e) {
    		    e.printStackTrace();
    	    }
    }
    
    /**
     * Make a Pointer ArrayType used later in subclass definitions.
     */
    @SuppressWarnings("unused")
    public static ArrayType PTR_ARRAY_TYPE;
    static {
    	    try {
    		    PTR_ARRAY_TYPE = new ArrayType(1, Pointer.getCompositeType());
    	    } catch (OpenDataException e) {
    		    e.printStackTrace();
    	    }
    }
    
    /**
     * Closed down constructor.
     * @throws ConfigurationException
     */
    private Configuration()
    throws ConfigurationException {
        this((String)null);
    }
        
    /**
     * Constructor.
     * For use by Configurables.  They subclass, add attribute info
     * by calling {@link #addAttributeInfos(List)} then set values
     * in {@link #initialize()}.
     * @param description
     * @throws ConfigurationException
     */
    public Configuration(final String description)
    throws ConfigurationException {
        super();
        try {
            this.mbeanInfo = createMBeanInfo(this.getClass().getName(),
                description);
        } catch (OpenDataException e) {
            throw new ConfigurationException(e);
        }
        
        // Finish off the construction by calling initialize.
        try {
            initialize();
        } catch (AttributeNotFoundException e) {
            throw new ConfigurationException(e);
        } catch (InvalidAttributeValueException e) {
            throw new ConfigurationException(e);
        } catch (MBeanException e) {
            throw new ConfigurationException(e);
        } catch (ReflectionException e) {
            throw new ConfigurationException(e);
        }
    }
    
    /**
     * Constructor.
     * Used by Registry reconstituting a Configuration read from
     * Store so it can register the restored instance..
     * @param m
     * @param al
     */
    public Configuration(final MBeanInfo m, final AttributeList al) {
        this.mbeanInfo = m;
        MBeanAttributeInfo [] mbai = this.mbeanInfo.getAttributes();
        for (int i = 0; i < mbai.length; i++) {
            this.attributeNames.add(mbai[i].getName());
        }
        MBeanOperationInfo [] mboi = this.mbeanInfo.getOperations();
        for (int i = 0; i < mboi.length; i++) {
            this.operationNames.add(mboi[i].getName());
        }
        setAttributes(al);
    }
    
    @SuppressWarnings("unused")
    protected void initialize()
    throws AttributeNotFoundException, InvalidAttributeValueException,
            MBeanException, ReflectionException {
        // Does nothing.  Called by subclasses to finish up construction.
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
     * {@link #createOperationInfo()} instead.<p>
     * TOOD: Allow adding notifications at least, perhaps operations.
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
    
    /**
     * Override, call super and then add {@link Configurable}
     * attributes.
     * @param infos
     * @return List of attribute info.
     * @throws OpenDataException
     */
    protected List<OpenMBeanAttributeInfo> addAttributeInfos(
    	final List<OpenMBeanAttributeInfo> infos)
    throws OpenDataException {
        infos.add(new OpenMBeanAttributeInfoSupport(ATTR_ENABLED,
            "Enabled if true", SimpleType.BOOLEAN,
            true, true, true, Boolean.TRUE, TRUE_FALSE_LEGAL_VALUES));
        ArrayType at = new ArrayType(1, SimpleType.STRING);
        infos.add(new OpenMBeanAttributeInfoSupport(ATTR_EXPERT,
            "Array of expert attribute names", at,
            true, true, false));
        infos.add(new OpenMBeanAttributeInfoSupport(ATTR_NO_OVERRIDE,
            "Array of overrideable attribute names", at,
            true, true, false));
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
    
    public Object get(final String attributeName)
    throws AttributeNotFoundException {
        return getAttribute(attributeName);
    }
    
    public Integer getInteger(final String attributeName)
    throws AttributeNotFoundException {
        return (Integer)get(attributeName);
    }
    
    public String getString(final String attributeName)
    throws AttributeNotFoundException {
        return (String)get(attributeName);
    }
    
    @SuppressWarnings("unused")
    public synchronized Object getAttribute(final String attributeName)
    throws AttributeNotFoundException {
        checkValidAttributeName(attributeName);
        // Else attribute is known.  Do we have it in our cache?  If not,
        // create and add.
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
    
    public AttributeList getAttributes(String[] atts) {
        AttributeList result = new AttributeList(atts.length);
        if (atts.length == 0) {
            return result;
        }
        for (int i = 0; i < atts.length; i++) {
            try {
                result.add(new Attribute(atts[i], getAttribute(atts[i])));
            } catch (AttributeNotFoundException e) {
                e.printStackTrace();
            }
        }
        return result;
    }
    
    public AttributeList getAttributes() {
        List<String> names = getAttributeNames();
        return getAttributes(names.toArray(new String [names.size()]));
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
}