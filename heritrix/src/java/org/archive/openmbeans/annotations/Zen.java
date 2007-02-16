/* Zen
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
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Provides tools for introspection.
 * 
 * @author pjack
 */
public class Zen {

    
    /** Static utility library, do not construct. */
    private Zen() {
    }

    
    /**
     * Returns the attribute name of an accessor or mutator method.  Eg, if
     * the given method name is "getFoo", then this method returns "foo".
     * 
     * @param methodName   the method name whose attribute name to retur
     * @return   the attribute name
     * @throws  IllegalArgumentException if the method name is not of the
     * form <code>getX</code>, <code>isX</code> or <code>setX</code>
     */
    public static String getAttributeName(String methodName) {
        if (methodName.startsWith("set")) {
            return methodName.substring(3).toLowerCase();
        }
        if (methodName.startsWith("get")) {
            return methodName.substring(3).toLowerCase();
        }
        if (methodName.startsWith("is")) {
            return methodName.substring(2).toLowerCase();
        }
        throw new IllegalArgumentException("Not an attribute name.");
    }

    
    /**
     * Returns true if the given method is a mutator with the given name
     * and parameter type.
     * 
     * @param m      the method to test
     * @param name   the name of the mutator we're looking for
     * @param type   the parameter type of the mutator we're looking for
     * @return   true if the given method is the mutator we're looking for
     */
    public static boolean isMutator(Method m, String name, Class type) {
        int mods = m.getModifiers();
        if (Modifier.isStatic(mods)) {
            return false;
        }
        if (!m.getName().equals(name)) {
            return false;
        }
        Class[] p = m.getParameterTypes();
        if (p.length != 1) {
            return false;
        }
        return p[0] == type;
    }

    
    /**
     * Returns the mutator corresponding to the given accessor.
     * 
     * @param c          the class that declared the accessor
     * @param accessor   the accessor
     * @return   the mutator that corresponds to that accessor, or null if no
     *   such mutator exists
     */
    public static Method getMutator(Class c, Method accessor) {
        String propName = getAttributeName(accessor.getName());
        String setterName = "set" + propName;
        for (Method m: c.getMethods()) {
            if (isMutator(m, setterName, accessor.getReturnType())) {
                return m;
            }
        }
        return null;
    }
    
    
    /**
     * Returns all methods declared by the given class and its superclasses,
     * including non-pubilc methods.
     * 
     * <p>Methods that are overridden only occur once in the returned array; 
     * the "most specific" version of the method is the one included.  For
     * instance, if class Foo defines two methods, fubar() and baz() method, 
     * and Bar extends Foo and overrides baz() but not fubar(), then the
     * returned list would be: { Bar.baz(), Foo.fubar() }.
     * 
     * <p>Note that it's impossible to override a private method, so the 
     * returned list may include private methods with duplicate signatures.
     * 
     * @param x   the class whose methods to return
     * @return   the methods in that class
     */
    public static List<Method> getAllMethods(Class x) {
        if (x.isInterface()) {
            List<Method> r = new ArrayList<Method>();
            getAllInterfaceMethods(r, x);
            return r;
        } else {
            return getAllClassMethods(x);
        }
    }
    
    
    private static List<Method> getAllClassMethods(Class x) {
        List<Method> result = new ArrayList<Method>();
        while (true) {
            for (Method m: x.getDeclaredMethods()) {
                int mods = m.getModifiers();
                if (Modifier.isPrivate(mods)) {
                    result.add(m);
                } else if (!contains(result, m)) {
                    result.add(m);
                }
            }
            if (x == Object.class) {
                return result;
            } else {
                x = x.getSuperclass();
            }
        }        
    }
    
    
    private static void getAllInterfaceMethods(List<Method> result, Class x) {
        for (Method m: x.getDeclaredMethods()) {
            if (!contains(result, m)) {
                result.add(m);
            }
        }
        for (Class s: x.getInterfaces()) {
            getAllInterfaceMethods(result, s);
        }
    }


    /**
     * Returns true if the given list already contains a method with the same
     * signature as the given method.
     * 
     * @param list   the list of methods to check
     * @param m      the method to check
     * @return   true if the list already contains a method with same 
     *             signature as m
     */
    private static boolean contains(List<Method> list, Method m) {
        for (Method test: list) {
            if (!test.getName().equals(m.getName())) {
                if (same(test, m)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    
    /**
     * Returns true if two methods have the same signature.  This is not
     * the same thing as the Method.equals(Object) method -- the equality 
     * test for methods includes the declaring class in the criteria.
     * This method, in contrast, only considers the method's name and its
     * parameter types.
     * 
     * @param m1   the first method to test
     * @param m2   the second method to test
     * @return   true if those two methods have the same signature
     */
    private static boolean same(Method m1, Method m2) {
        if (!m1.getName().equals(m2.getName())) {
            return false;
        }
        return Arrays.equals(m1.getParameterTypes(), m2.getParameterTypes());
    }
}
