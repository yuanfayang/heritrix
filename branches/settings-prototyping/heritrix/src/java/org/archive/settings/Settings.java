/* Settings.java
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
package org.archive.settings;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;

/**
 * Configuration for a component homed on a domain.
 * @author stack
 */
public class Settings implements DynamicMBean, MBeanRegistration {
    public Object getAttribute(final String attributeName)
    throws AttributeNotFoundException, MBeanException, ReflectionException {
        // TODO Auto-generated method stub
        return null;
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

    public void setAttribute(Attribute attribute)
    throws AttributeNotFoundException, InvalidAttributeValueException,
    MBeanException, ReflectionException {
        // TODO Auto-generated method stub
        
    }

    public AttributeList getAttributes(String[] attributes) {
        // TODO Auto-generated method stub
        return null;
    }

    public AttributeList setAttributes(AttributeList attributes) {
        // TODO Auto-generated method stub
        return null;
    }
    
    public Object invoke(String actionName, Object[] params, String[] signature)
    throws MBeanException, ReflectionException {
        // Add getNonExpertAttributes and getOverrideables and getTransients.
        return null;
    }

    public MBeanInfo getMBeanInfo() {
        return null;
    }

    public ObjectName preRegister(MBeanServer server, ObjectName on)
    throws Exception {
        return null;
    }

    public void postRegister(Boolean arg0) {
        // TODO Auto-generated method stub
    }

    public void preDeregister() throws Exception {
        // TODO Auto-generated method stub
    }

    public void postDeregister() {
        // TODO Auto-generated method stub
    }
    
    public static void main(String[] args) {
        // TODO: How to get in context.
    }
}