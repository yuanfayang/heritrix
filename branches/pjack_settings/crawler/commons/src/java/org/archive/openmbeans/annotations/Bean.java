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


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ReflectionException;


/**
 * Supporting class for creating annotation-based open MBeans.  There are two
 * ways to use this class: subclassing and composition.
 * 
 * <p>The easiest way is subclassing.  Simple extend this class and annotate
 * your attributes, operations and notifications.  When instances are 
 * constructed, reflection will be used to check for the annotations, and an
 * appropriate OpenMBeanInfo object will be automatically generated.  Since
 * this base class implements the DynamicMBean interface directly, you will
 * have a compliant OpenMBean.
 * 
 * <p>For instance:
 * 
 * <pre>
 * public class MyClass extends Bean {
 * 
 *     public MyClass() {
 *         super();
 *     }
 * 
 *     &#064;Operation(desc="My operation", impact=ACTION)
 *     public void myOperation() {
 *     
 *     }
 * }
 * </pre>
 * 
 * If you cannot subclass Bean directly, you can still use it to ease the 
 * creation of compliant open MBeans, but the process is a bit more 
 * complicated.
 * 
 * <p>For instance, let's say you have a class named Foo whose instances you 
 * want to manage via JMX.  Because the class relies on a third-party 
 * library, Foo must extend the third-party class Bar.  To use the 
 * annotation-based metadata, you would simply have to extend Bar and 
 * manually implement the DynamicMBean interface.  But your DynamicMBean
 * methods would simply delegate to an instance of this class (Bean).
 * The code might look like:
 * 
 * <pre>
 * public class Foo extends Bar implements DynamicMBean {
 * 
 * 
 *     final private Bean support;
 *     
 * 
 *     public Foo() {
 *         this.support = new Bean(this);
 *     }
 *     
 *     // DynamicMBean method
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
public class Bean implements DynamicMBean, NotificationEmitter {

    
    final public static int ACTION = MBeanOperationInfo.ACTION;
    final public static int INFO = MBeanOperationInfo.INFO;
    final public static int ACTION_INFO = MBeanOperationInfo.ACTION_INFO;
    
    final private AtomicLong sequenceNumber = new AtomicLong();
    
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
     * The actual DynamicMBean object.
     */
    final private Object target;

    
    final private NotificationBroadcasterSupport emitter;

    /**
     * Constructs a new BeanSupport for the given object.  The object's class
     * should be annotated; introspection will be used to provide the 
     * implementation of the DynamicMBean interface for that object.
     * 
     * @param target  the target MBean
     */
    public Bean(DynamicMBean target) {
        this(target, target.getClass());
    }

    public Bean(DynamicMBean target, Class c) {
        this.target = target;
        this.metadata = getInfo(c);
        this.emitter = new NotificationBroadcasterSupport();
    }

    
    public Bean(Class c) {
        this.target = this;
        this.metadata = getInfo(c);
        this.emitter = new NotificationBroadcasterSupport();
    }
    
    
    public Bean() {
        this.target = this;
        this.metadata = getInfo(getClass());
        this.emitter = new NotificationBroadcasterSupport();
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
            return a.invoke(target);
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
            return m.invoke(target, params);
        } catch (InvocationTargetException e) {
            Throwable t = e.getTargetException();
            t.printStackTrace();
            if (t instanceof Exception) {
                Exception te = (Exception)t;
                throw new ReflectionException(te);
            } else {
                throw new ReflectionException(e);
            }
        } catch (IllegalAccessException e) {
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
            m.invoke(target, attribute.getValue());
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


    public void removeNotificationListener(
            NotificationListener listener, 
            NotificationFilter filter, 
            Object handback) 
    throws ListenerNotFoundException {
        emitter.removeNotificationListener(listener, filter, handback);
    }


    public void addNotificationListener(
            NotificationListener listener, 
            NotificationFilter filter, 
            Object handback) 
    throws IllegalArgumentException {
        emitter.addNotificationListener(listener, filter, handback);
    }


    public MBeanNotificationInfo[] getNotificationInfo() {
        return metadata.getOpenMBeanInfo().getNotifications();
    }


    public void removeNotificationListener(NotificationListener listener) 
    throws ListenerNotFoundException {
        emitter.removeNotificationListener(listener);
    }


    /**
     * Sends the given notification to all registered listeners.
     * 
     * @param n   the notification to send
     */
    public void sendNotification(Notification n) {
        emitter.sendNotification(n);
    }


    public void sendNotification(String type, String message) {
        Notification n = new Notification(
                type, 
                this, 
                sequenceNumber.getAndIncrement(),
                System.currentTimeMillis(),
                message);
        sendNotification(n);
    }

}
