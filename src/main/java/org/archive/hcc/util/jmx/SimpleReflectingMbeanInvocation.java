/* $Id$
 *
 * (Created on Dec 12, 2005
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
package org.archive.hcc.util.jmx;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.management.MBeanException;
import javax.management.MBeanParameterInfo;
import javax.management.ReflectionException;
import javax.management.openmbean.OpenMBeanOperationInfo;

public class SimpleReflectingMbeanInvocation implements
        MBeanInvocation {
    private Object target;

    private OpenMBeanOperationInfo info;

    private Object[] params;

    public SimpleReflectingMbeanInvocation(
            Object target,
            OpenMBeanOperationInfo info,
            Object[] params) {
        super();
        this.target = target;
        this.info = info;
        this.params = params;
    }

    public Object invoke() throws MBeanException, ReflectionException {

    	try {
            Method method = target.getClass().getMethod(
                    info.getName(),
                    convertSignature(info));

    		return method.invoke(this.target, params);
        } catch (SecurityException e) {
            e.printStackTrace();
            throw new ReflectionException(e);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            throw new ReflectionException(e);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new ReflectionException(e);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            throw new ReflectionException(e);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            throw new MBeanException(e);
        }
    }

    private static Class[] convertSignature(OpenMBeanOperationInfo info)
            throws ClassNotFoundException {
        MBeanParameterInfo[] params = info.getSignature();
        Class[] classes = new Class[params.length];
        for (int i = 0; i < params.length; i++) {
            String theType = params[i].getType();
            Class c = Class.forName(theType);
            classes[i] = c;
        }

        return classes;
    }

}
