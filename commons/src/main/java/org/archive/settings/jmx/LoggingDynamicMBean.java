/**
 * 
 */
package org.archive.settings.jmx;


import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationBroadcaster;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.ReflectionException;

/**
 * Invocation handler for MBeans used by JMXModuleListener.  Automatically logs
 * information regarding JMX events sent from clients.  Also, converts all
 * exceptions to IllegalStateException.
 * 
 * @author pjack
 */
public class LoggingDynamicMBean implements DynamicMBean, NotificationBroadcaster {

    final public static Logger LOGGER = 
        Logger.getLogger(LoggingDynamicMBean.class.getName());

    final private DynamicMBean target;
    
    private LoggingDynamicMBean(DynamicMBean target) {
        this.target = target;
    }


    public static void register(MBeanServer server, Object object, 
            ObjectName name) {
        Object proxy = new LoggingDynamicMBean((DynamicMBean)object);
        try {
            server.registerMBean(proxy, name);
        } catch (InstanceAlreadyExistsException e) {
            throw new IllegalStateException(e);
        } catch (MBeanRegistrationException e) {
            throw new IllegalStateException(e);
        } catch (NotCompliantMBeanException e) {
            throw new IllegalStateException(e);
        }
    }

    
    private void dealWithException(String name, Exception e) 
    throws ReflectionException, MBeanException { 
        if (e instanceof MBeanException) {
            MBeanException me = (MBeanException)e;
            LOGGER.log(Level.SEVERE, "MBeanException during " + name, 
                    me.getTargetException());
            // Because e might be caused by something nonstandard.
            throw new MBeanException(new IllegalStateException(e.getMessage()));            
        }
        if (e instanceof ReflectionException) {
            ReflectionException re = (ReflectionException)e;
            LOGGER.log(Level.SEVERE, "ReflectionException during " + name, 
                    re.getTargetException());
            throw new ReflectionException(
                    new IllegalStateException(e.getMessage()));            
        }
        LOGGER.log(Level.SEVERE, "UnknownException during " + name, e); 
        throw new MBeanException(new IllegalStateException(e.getMessage()));            
    }


    public Object getAttribute(String attr) throws AttributeNotFoundException,
            MBeanException, ReflectionException {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info(logHeader(attr).toString());
        }
        Object result = null;
        try {
            result = target.getAttribute(attr);
        } catch (AttributeNotFoundException e) {
            LOGGER.log(Level.SEVERE, "AttributeNotFoundException during " 
                    + attr, e);
            throw new AttributeNotFoundException(e.getMessage());
        } catch (Exception e) {
            dealWithException(attr, e);
        }
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.info(logResult(attr, result));
        }
        return result;
    }


    public AttributeList getAttributes(String[] arg0) {
        return target.getAttributes(arg0);
    }


    public MBeanInfo getMBeanInfo() {
        return target.getMBeanInfo();
    }

    
    private StringBuilder logHeader(String opName) {
        StringBuilder sb = new StringBuilder();
        sb.append(target.getClass().getName()).append('.').append(opName);
        return sb;
    }

    private void appendObject(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append(value);
        } else if (value.getClass().isArray()) {
            sb.append(Arrays.asList((Object[])value));
        } else {
            sb.append(value);
        }
    }
    
    private String logResult(String name, Object result) {
        StringBuilder sb = logHeader(name);
        sb.append(" returned ");
        appendObject(sb, result);
        return sb.toString();
    }
    

    public Object invoke(String name, Object[] params, String[] sig) 
    throws MBeanException, ReflectionException {
        if (LOGGER.isLoggable(Level.INFO)) {
            StringBuilder sb = logHeader(name);
            sb.append('(');
            if (params.length > 0) {
                appendObject(sb, params[0]);
                for (int i = 1; i < params.length; i++) {
                    sb.append(", ");
                    appendObject(sb, params[i]);
                }
            }
            sb.append(')');
            LOGGER.info(sb.toString());
        }
        Object result = null;
        try {
            result = target.invoke(name, params, sig);
        } catch (Exception e) {
            dealWithException(name, e);
        }
        
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest(logResult(name, result));
        }
        return result;
    }


    public void setAttribute(Attribute attr) throws AttributeNotFoundException,
            InvalidAttributeValueException, MBeanException, ReflectionException {
        if (LOGGER.isLoggable(Level.INFO)) {
            StringBuilder sb = logHeader(attr.getName());
            sb.append(" = ");
            appendObject(sb, attr.getValue());
            LOGGER.info(sb.toString());
        }
        try {
            target.setAttribute(attr);
        } catch (InvalidAttributeValueException e) {
            LOGGER.log(Level.SEVERE, "InvalidAttributeValueException during " 
                    + attr, e);
            throw new InvalidAttributeValueException(e.getMessage());
        } catch (AttributeNotFoundException e) {
            LOGGER.log(Level.SEVERE, "AttributeNotFoundException during " 
                    + attr, e);
            throw new AttributeNotFoundException(e.getMessage());
        } catch (Exception e) {
            dealWithException(attr.getName(), e);
        }
    }


    public AttributeList setAttributes(AttributeList arg0) {
        return target.setAttributes(arg0);
    }


    public void addNotificationListener(NotificationListener arg0,
            NotificationFilter arg1, Object arg2)
            throws IllegalArgumentException {
        ((NotificationBroadcaster)target).addNotificationListener(
                arg0, arg1, arg2);
    }


    public MBeanNotificationInfo[] getNotificationInfo() {
        return ((NotificationBroadcaster)target).getNotificationInfo();
    }


    public void removeNotificationListener(NotificationListener arg0)
            throws ListenerNotFoundException {
        ((NotificationBroadcaster)target).removeNotificationListener(arg0);
    }


}
