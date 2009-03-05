/* $Id$
 *
 * Created on Dec 12, 2005
 *
 * Copyright (C) 2005 Internet Archive.
 *  
 * This file is part of the Heritrix Cluster Controller (crawler.archive.org).
 *  
 * HCC is free software; you can redistribute it and/or modify
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
package org.archive.hcc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

/**
 * This invocation handler is responsible for proxying calls to and
 * remote notifications from remote mbean instances.
 * @author Daniel Bernstein (dbernstein@archive.org)
 */
public class RemoteMBeanInvocationHandler implements
        InvocationHandler {
    
    private static Logger log =
        Logger.getLogger(RemoteMBeanInvocationHandler.class.getName());

    private ObjectName remoteObjectName;

    private MBeanServerConnection connection;

    private ObjectName proxyObjectName;

    private NotificationListener spyListener;

    private Map<NotificationListener, NotificationListener> notificationProxyMap = 
        new HashMap<NotificationListener, NotificationListener>();

    /**
     * Constructs a remote mbean invocation handler.
     * @param remoteObjectName 
     * @param proxyObjectName
     * @param connection 
     * @param notificationInterceptor
     */
    public RemoteMBeanInvocationHandler(
            ObjectName remoteObjectName,
            ObjectName proxyObjectName,
            MBeanServerConnection connection,
            NotificationListener notificationInterceptor) {
        this.remoteObjectName = remoteObjectName;
        this.proxyObjectName = proxyObjectName;
        this.connection = connection;
        this.spyListener = notificationInterceptor;
    }

    public ObjectName getRemoteObjectName() {
        return remoteObjectName;
    }

    /**
     * Invokes the specified method on the remote mbean. If the method doesn't
     * match a method on the DynamicMBean or NotificationEmitter, the method is
     * invoked on the remote invocation handler directly (ie equals and
     * hashCode);
     * @param proxy 
     * @param method 
     * @param args 
     * @return Result object.
     * @throws Throwable 
     */
    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {

        if (log.isLoggable(Level.FINER)) {
            log.finer("method=" + method);
        }
        if (method.getName().equals("invoke")) {
            return this.connection.invoke(
                    this.remoteObjectName,
                    (String) args[0],
                    (Object[]) args[1],
                    (String[]) args[2]);
        } else if (method.getName().equals("getMBeanInfo")) {
            return this.connection.getMBeanInfo(this.remoteObjectName);
        } else if (method.getName().equals("getAttributes")) {
            return this.connection.getAttributes(
                    this.remoteObjectName,
                    (String[]) args[0]);
        } else if (method.getName().equals("getAttribute")) {
            return this.connection.getAttribute(
                    this.remoteObjectName,
                    (String) args[0]);
        } else if (method.getName().equals("setAttributes")) {
            return this.connection.setAttributes(
                    this.remoteObjectName,
                    (AttributeList) args[0]);
        } else if (method.getName().equals("setAttribute")) {
            this.connection.setAttribute(
                    this.remoteObjectName,
                    (Attribute) args[0]);
            return null;
        } else if (method.getName().equals("addNotificationListener")) {
            
            //reference the notification listener argument.
            final NotificationListener clientListener = 
                (NotificationListener) args[0];

            //wrap the listener with a interceptor
            //that translates the proxy object name
            NotificationListener interceptingListener = 
                new NotificationListener() {
                 public void handleNotification(
                         javax.management.Notification notification,
                         Object handback) {
                     notification.setSource(getProxyObjectName());
                     // notify the spy listener
                     spyListener.handleNotification(notification, handback);
 
                     // forward to registered listeners.
                     clientListener.handleNotification(notification, handback);
                 };
            };

            this.notificationProxyMap.put(clientListener, interceptingListener);

            //proxy the notification filter if 
            //the second argument is not null,
            //a filter that translates teh remote object name.
            NotificationFilter interceptingFilter = null;

            if (args[1] != null) {
                final NotificationFilter clientFilter = 
                    (NotificationFilter) args[1];
                interceptingFilter = 
                    new NotificationFilter() {
                     public boolean isNotificationEnabled(
                             Notification notification) {
                         Notification n = new Notification(notification
                                 .getType(), proxyObjectName, notification
                                 .getSequenceNumber(), notification
                                 .getTimeStamp(), notification.getMessage());
 
                         n.setUserData(notification.getUserData());
                         return clientFilter.isNotificationEnabled(n);
                     }
                };
            }

            this.connection.addNotificationListener(
                    this.remoteObjectName,
                    interceptingListener,
                    interceptingFilter,
                    (Object) args[2]);
            return null;
            
        } else if (method.getName().equals("removeNotificationListener")) {
            final NotificationListener clientListener = 
                    (NotificationListener) args[0];
            this.notificationProxyMap.remove(clientListener);
            this.connection.removeNotificationListener(
                    this.remoteObjectName,
                    clientListener);
        } else if (method.getName().equals("getNotificationInfo()")) {
            return this.connection
                    .getMBeanInfo(this.remoteObjectName)
                    .getNotifications();
        }

        Method m = this.getClass().getMethod(
                method.getName(),
                method.getParameterTypes());
        return m.invoke(this, args);
    }

    public ObjectName getProxyObjectName() {
        return proxyObjectName;
    }
}