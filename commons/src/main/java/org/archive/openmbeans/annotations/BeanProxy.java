/* 
 * Copyright (C) 2007 Internet Archive.
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
 *
 * BeanInvocationHandler.java
 *
 * Created on May 1, 2007
 *
 * $Id:$
 */

package org.archive.openmbeans.annotations;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;

/**
 * @author pjack
 *
 */
public class BeanProxy implements InvocationHandler {

    
    final private MBeanServerConnection connection;
    final private ObjectName oname;
    final private MBeanInfo info;
    
    
    public BeanProxy(MBeanServerConnection connection, ObjectName oname) 
    throws InstanceNotFoundException, IntrospectionException, 
                ReflectionException, IOException {
        this.connection = connection;
        this.oname = oname;
        this.info = connection.getMBeanInfo(oname);
    }
    
    
    
    public Object invoke(Object target, Method method, Object[] args) 
    throws Throwable {
        if (args == null) {
            args = new Object[0]; 
        }
        if (Zen.isAccessor(method)) {
            String attr = Zen.getAttributeName(method.getName());
            return connection.getAttribute(oname, attr);
        }
        if (Zen.isMutator(method)) {
            String attrName = Zen.getAttributeName(method.getName());
            javax.management.Attribute attr = 
                new javax.management.Attribute(attrName, args[0]);
            connection.setAttribute(oname, attr);
            return null;
        }
        
        MBeanOperationInfo info = getOperation(method);
        String[] sig = new String[args.length];
        for (int i = 0; i < sig.length; i++) {
            sig[i] = info.getSignature()[i].getType();
        }

        return connection.invoke(oname, method.getName(), args, sig);
    }

    
    private MBeanOperationInfo getOperation(Method method) {
        // FIXME: Compare parameter types instead of just checking length
        String name = method.getName();
        int plength = method.getParameterTypes().length;
        for (MBeanOperationInfo oi: info.getOperations()) {
            if (oi.getName().equals(name)
            && (oi.getSignature().length == plength)) {
                return oi;
            }
        }
        throw new IllegalStateException("No such operation: " + method);
    }
    
    
    public static <T> T proxy(MBeanServerConnection connection, 
            ObjectName oname, 
            Class<T> cls) throws InstanceNotFoundException, 
            IntrospectionException, ReflectionException, IOException {
        ClassLoader loader = cls.getClassLoader();
        InvocationHandler ih = new BeanProxy(connection, oname);
        Class[] classes = new Class[] { cls };
        return cls.cast(Proxy.newProxyInstance(loader, classes, ih));
    }


}
