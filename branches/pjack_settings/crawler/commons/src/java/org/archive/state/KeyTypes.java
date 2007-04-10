/* Copyright (C) 2007 Internet Archive.
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
 * KeyTypes.java
 * Created on January 16, 2007
 *
 * $Header$
 */
package org.archive.state;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class KeyTypes {

    
    final private static Map<Class,String> TYPES;
    final private static Map<String,Class> TAGS;

    static {
        Map<Class,String> types = new HashMap<Class,String>();
        types.put(Boolean.class, "boolean");
        types.put(Byte.class, "byte");
        types.put(Character.class, "char");
        types.put(Double.class, "double");
        types.put(Float.class, "float");
        types.put(Integer.class, "int");
        types.put(Long.class, "long");
        types.put(Short.class, "short");        
        types.put(String.class, "string");
        types.put(Pattern.class, "pattern");
        types.put(BigInteger.class, "biginteger");
        types.put(BigInteger.class, "bigdecimal");
        TYPES = Collections.unmodifiableMap(types);
        
        Map<String,Class> tags = new HashMap<String,Class>();
        for (Map.Entry<Class,String> me: types.entrySet()) {
            tags.put(me.getValue(), me.getKey());
        }
        TAGS = Collections.unmodifiableMap(tags);
    }
    
    

    
    private KeyTypes() {
    }

    
    public static boolean isSimple(Class type) {
        if (Enum.class.isAssignableFrom(type)) {
            return true;
        }
        return TYPES.keySet().contains(type);
    }

    
    public static String getSimpleTypeTag(Class type) {
        return TYPES.get(type);
    }
    
    
    public static Class getSimpleType(String tag) {
        return TAGS.get(tag);
    }
    
    
    private static Object fromString2(Class type, String value) {
        if (type == Boolean.class) {
            return Boolean.parseBoolean(value);
        }
        if (type == Byte.class) {
            return Byte.decode(value);
        }
        if (type == Character.class) {
            return value.charAt(0);
        }
        if (type == Double.class) {
            return Double.parseDouble(value);
        }
        if (type == Float.class) {
            return Float.parseFloat(value);
        }
        if (type == Integer.class) {
            return Integer.decode(value);
        }
        if (type == Long.class) {
            return Long.decode(value);
        }
        if (type == Short.class) {
            return Short.decode(value);
        }
        if (type == BigDecimal.class) {
            return new BigDecimal(value);
        }
        if (type == BigInteger.class) {
            return new BigInteger(value);
        }
        if (type == String.class) {
            return value;
        }
        if (type == Pattern.class) {
            return Pattern.compile(value);
        }
        
        throw new IllegalArgumentException("Not a simple type: " + type);
    }
    
    public static String toString(Object simple) {
        if (!isSimple(simple.getClass())) {
            throw new IllegalArgumentException();
        }
        return simple.toString();
    }


    public static Object fromString(Class simpleType, String value) {
        try {
            return fromString2(simpleType, value);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
    
    

}
