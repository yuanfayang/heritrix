/* BeanSupport
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


import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.ReflectionException;


/**
 * Supporting class for creating annotation-based open MBeans.  Use this class
 * when you cannot subclass {@link Bean} directly.
 * 
 * <p>For instance, let's say you have a class named Foo whose instances you 
 * want to manage via JMX.  Because the class relies on a third-party 
 * library, Foo must extend the third-party class Bar.  To use the 
 * annotation-based metadata, you would simply have to extend Bar and 
 * manually implement the DynamicMBean interface.  But your DynamicMBean
 * methods would simply delegate to an instance of this class (BeanSupport).
 * The code might look like:
 * 
 * <pre>
 * public class Foo extends Bar implements DynamicMBean {
 * 
 * 
 *     final private BeanSupport support;
 *     
 * 
 *     public Foo() {
 *         this.support = new BeanSupport(Foo.class);
 *     }
 *     
 *     
 *     public Object getAttribute(String attribute) {
 *         return support.getAttribute(attribute);
 *     }
 * 
 *     // ... and similar implementations for the other DynamicMBean instances.
 * }
 * </pre>
 * 
 * @author pjack
 *
 */
public class BeanSupport implements DynamicMBean {

    
    /**
     * Maps a class to its metadata.
     */
    final private static Map<Class,Metadata> METADATA
     = new HashMap<Class,Metadata>();
    
    
    /**
     * The metadata used to implement the DynamicMBean interface.
     */
    final private Metadata metadata;
    
    
    /**
     * Constructs a new BeanSupport that will use metadata from the given
     * class.  The given class should have annotated attributes and
     * operations.
     * 
     * @param c   the annotated class
     */
    public BeanSupport(Class c) {
        metadata = getInfo(c);
    }


    /**
     * If the given class already has metadata defined for it, return that
     * metadata.  Otherwise use reflection to generate new metadata based on
     * annotations, remember that metadata for future reference, and return
     * it. 
     * 
     * @param c   the class whose metadata to return
     * @return   the metadata for that class
     */
    private static Metadata getInfo(Class c) {
        synchronized (METADATA) {
            Metadata result = METADATA.get(c);
            if (result != null) {
                return result;
            }
            result = new Metadata(c);
            METADATA.put(c, result);
            return result;
        }
    }


    public Object getAttribute(String attribute) 
    throws AttributeNotFoundException, ReflectionException {
        Method a = metadata.getAccessor(attribute);
        try {
            return a.invoke(this);
        } catch (Exception e) {
            throw new ReflectionException(e);
        }
    }


    public AttributeList getAttributes(String[] attributes) {
        AttributeList result = new AttributeList(attributes.length);
        for (String s: attributes) try {
            result.add(new Attribute(s, getAttribute(s)));
        } catch (Exception e) {
            // FIXME: Specification doesn't say what to do here.
            throw new RuntimeException(e);
        }
        return result;
    }


    public MBeanInfo getMBeanInfo() {
        return metadata.getOpenMBeanInfo();
    }


    public Object invoke(String actionName, Object[] params, String[] sig) 
    throws MBeanException, ReflectionException {
        Method m = metadata.getOperation(actionName);
        if (m == null) {
            // FIXME: Specification doesn't say what to do here.
            return null;
        }
        try {
            return m.invoke(this, params);
        } catch (Exception e) {
            throw new ReflectionException(e);
        }
    }


    public void setAttribute(Attribute attribute) 
    throws AttributeNotFoundException, ReflectionException {
        Method m = metadata.getMutator(attribute.getName());
        if (m == null) {
            throw new AttributeNotFoundException();
        }
        try {
            m.invoke(this, attribute.getValue());
        } catch (Exception e) {
            throw new ReflectionException(e);
        }        
    }


    public AttributeList setAttributes(AttributeList attributes) {
        // Specification doesn't say what this method should do.
        // Assuming it wants to return the original list.
        for (Object o: attributes) try {
            Attribute a = (Attribute)o;
            setAttribute(a);
        } catch (Exception e) {
            // FIXME: Specification doesn't say to do here.
            throw new RuntimeException(e);
        }
        return attributes;
    }

}
