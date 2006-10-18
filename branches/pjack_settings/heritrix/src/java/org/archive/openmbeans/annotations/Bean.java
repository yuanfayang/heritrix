/* Bean
 *
 * Created on October 18, 2006
 *
 * Copyright (C) 2006 Internet Archive.
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
package org.archive.openmbeans.annotations;


import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.ReflectionException;


/**
 * An open mbean whose metadata is defined by attributes.  Subclass this 
 * and annotate your attributes and operations for a quick and dirty way to
 * create compliant Open MBeans.
 * 
 * @author pjack
 */
public abstract class Bean implements DynamicMBean {


    /**
     * Impact code indicating that an operation modifies this bean.
     * 
     * @see MBeanOperationInfo#ACTION
     */
    final public static int ACTION = MBeanOperationInfo.ACTION;

    
    /**
     * Impact code indicating that an operation is both "read-like" and 
     * "write-like".
     * 
     * @see MBeanOperationInfo#ACTION_INFO
     */
    final public static int ACTION_INFO = MBeanOperationInfo.ACTION_INFO;

    
    /**
     * Impact code indicating that an operation does not modify this bean.
     * 
     * @see MBeanOperationInfo#INFO
     */
    final public static int INFO = MBeanOperationInfo.INFO;

    
    /**
     * The BeanSupport instance that DynamicMBean calls will delegate to.
     */
    final private BeanSupport support;
    
    
    /**
     * Constructor.  This constructor scans this instance's class for 
     * annotated attributes and operations, creating OpenMBeanInfo based on
     * that information.
     */
    public Bean() {
        support = new BeanSupport(this.getClass());
    }


    public Object getAttribute(String attribute) 
    throws AttributeNotFoundException, ReflectionException {
        return support.getAttribute(attribute);
    }


    public AttributeList getAttributes(String[] attributes) {
        return support.getAttributes(attributes);
    }


    public MBeanInfo getMBeanInfo() {
        return support.getMBeanInfo();
    }


    public Object invoke(String actionName, Object[] params, String[] sig) 
    throws MBeanException, ReflectionException {
        return support.invoke(actionName, params, sig);
    }


    public void setAttribute(Attribute attribute) 
    throws AttributeNotFoundException, ReflectionException {
        support.setAttribute(attribute);
    }


    public AttributeList setAttributes(AttributeList attributes) {
        return support.setAttributes(attributes);
    }

}
