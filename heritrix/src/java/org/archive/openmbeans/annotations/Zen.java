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
}
