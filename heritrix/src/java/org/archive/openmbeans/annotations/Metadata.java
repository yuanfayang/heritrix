/* Metadata
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


import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import javax.management.openmbean.OpenMBeanInfoSupport;

import org.archive.openmbeans.factory.Attr;
import org.archive.openmbeans.factory.Info;
import org.archive.openmbeans.factory.Op;
import org.archive.openmbeans.factory.Param;


/**
 * Discovered metadata for an annotation-based open MBean.  The metadata
 * consists of the standard OpenMBeanInfo, but also includes several maps
 * for efficiently locating a particular operation or attribute by name.
 * 
 * @author pjack
 */
class Metadata {

    
    private Info info;
    private OpenMBeanInfoSupport openInfo;
    private Map<String,Method> operations;
    private Map<String,Method> accessors;
    private Map<String,Method> mutators;
    
    
    public Metadata(Class<?> c) {
        info = new Info();
        info.name = c.getName();
        Header h = c.getAnnotation(Header.class);
        if (h != null) {
            info.desc = h.desc();
        }
        operations = new HashMap<String,Method>();
        accessors = new HashMap<String,Method>();
        mutators = new HashMap<String,Method>();
        Method[] methods = c.getMethods();
        Method hook = null;
        for (Method m: methods) {
            addOperation(m);
            addAttribute(m);
            if (isInstrospectionHook(m)) {
                hook = m;
            }
        }
        if (hook != null) {
            try {
                hook.invoke(null, info);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        openInfo = info.make();
        info = null;
    }

    
    private static boolean isInstrospectionHook(Method m) {
        if (!m.getName().equals("introspectionHook")) {
            return false;
        }
        int mods = m.getModifiers();
        if (!Modifier.isStatic(mods)) {
            return false;
        }
        if (!Modifier.isPublic(mods)) {
            return false;
        }
        Class[] ptypes = m.getParameterTypes();
        if (ptypes.length != 1) {
            return false;
        }
        if (ptypes[0] != Info.class) {
            return false;
        }
        if (m.getReturnType() != Void.TYPE) {
            return false;
        }
        return true;
    }
    
    public OpenMBeanInfoSupport getOpenMBeanInfo() {
        return openInfo;
    }
    
    
    public Method getAccessor(String attributeName) {
        return accessors.get(attributeName);
    }
    
    
    public Method getMutator(String attributeName) {
        return mutators.get(attributeName);
    }
    
    
    public Method getOperation(String opName) {
        return operations.get(opName);
    }
    
    
    private void addAttribute(Method m) {
        Attribute anno = m.getAnnotation(Attribute.class);
        if (anno == null) {
            return;
        }
        String propName = Zen.getAttributeName(m.getName());
        Method mutator = Zen.getMutator(m.getDeclaringClass(), m);
        Class type = m.getReturnType();
        Attr attr = new Attr();
        attr.name = propName;
        attr.desc = anno.desc();
        attr.read = true;
        attr.write = (mutator != null);
        attr.type = OpenTypes.toOpenType(type);
        attr.def = OpenTypes.parse(m.getReturnType(), anno.def());
        attr.min = parseMinOrMax(type, anno.min());
        attr.max = parseMinOrMax(type, anno.max());
        for (String s: anno.legal()) {
            attr.legal.add(OpenTypes.parse(type, s));
        }
        info.attrs.add(attr);
        accessors.put(propName, m);
        if (mutator != null) {
            mutators.put(propName, m);
        }
    }
    
    
    private static Comparable parseMinOrMax(Class c, String v) {
        if (v.equals("")) {
            return null;
        }
        return OpenTypes.parse(c, v);
    }
    
    
    private void addOperation(Method m) {
        Operation anno = m.getAnnotation(Operation.class);
        if (anno == null) {
            return;
        }
        Op op = new Op();
        op.name = m.getName();
        op.desc = anno.desc();
        op.ret = OpenTypes.toOpenType(m.getReturnType());
        op.impact = anno.impact();
        Annotation[][] pannos = m.getParameterAnnotations();
        Class[] ptypes = m.getParameterTypes();
        for (int i = 0; i < ptypes.length; i++) {
            Parameter panno = findParameterAnnotation(pannos[i]);
            Param param = new Param();
            if (panno == null) {
                param.name = "arg" + i;
                param.desc = "arg" + i;
            } else {
                param.name = panno.name();
                param.desc = panno.desc();
                param.def = OpenTypes.parse(ptypes[i], panno.def());
                param.min = parseMinOrMax(ptypes[i], panno.min());
                param.max = parseMinOrMax(ptypes[i], panno.max());
                for (String s: panno.legal()) {
                    param.legal.add(OpenTypes.parse(ptypes[i], s));
                }
            }
            param.type = OpenTypes.toOpenType(ptypes[i]);
            op.sig.add(param);
        }
        
        operations.put(m.getName(), m);
        info.ops.add(op);
    }


    private static Parameter findParameterAnnotation(Annotation[] arr) {
        for (Annotation a: arr) {
            if (a instanceof Parameter) {
                return (Parameter)a;
            }
        }
        return null;
    }


}
