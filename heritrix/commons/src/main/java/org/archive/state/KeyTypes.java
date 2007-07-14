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


/**
 * Defines the simple types for configurable properties.  FIXME:  Shouldn't
 * we call this "SimpleTypes" then?
 * 
 * The simple types are:
 * 
 * <ol>
 * <li><code>java.lang.Boolean</code></li>
 * <li><code>java.lang.Byte</code></li>
 * <li><code>java.lang.Character</code></li>
 * <li><code>java.lang.Double</code></li>
 * <li><code>java.lang.Float</code></li>
 * <li><code>java.lang.Integer</code></li>
 * <li><code>java.lang.Long</code></li>
 * <li><code>java.lang.Short</code></li>
 * <li><code>java.lang.String</code></li>
 * <li><code>java.util.regex.Pattern</code></li>
 * <li><code>java.math.BigDecimal</code></li>
 * <li><code>java.math.BigInteger</code></li>
 * <li>Any subclass of <code>java.lang.Enum</code></li>.
 * </ol>
 * 
 * Note that all of these types represent immutable objects.
 * 
 * <p>This class also provides <i>type tags</i> for the simple types.  A 
 * type tag is a short string describing the type.  This useful for defining
 * file formats and so on.  The sheet files used by 
 * {@link org.archive.settings.file.FileSheetManager} use type tags, 
 * for instance.
 * 
 * <p>The type tags for simple types are:
 * 
 * <ol>
 * <li><code>boolean</code></li>
 * <li><code>byte</code></li>
 * <li><code>char</code></li>
 * <li><code>double</code></li>
 * <li><code>float</code></li>
 * <li><code>int</code></li>
 * <li><code>long</code></li>
 * <li><code>short</code></li>
 * <li><code>string</code></li>
 * <li><code>pattern</code></li>
 * <li><code>bigdecimal</code></li>
 * <li><code>biginteger</code></li>
 * <li><code>enum</code></li>
 * </ol>
 * 
 * @author pjack
 */
public class KeyTypes {


    /**
     * Maps Java class to type tag.
     */
    final private static Map<Class,String> TYPES;
    
    /**
     * Maps type tag to Java class.
     */
    final private static Map<String,Class> TAGS;
    
    /**
     * The type tag for Enum types.
     */
    final public static String ENUM_TAG = "enum";
    
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
        types.put(Enum.class, ENUM_TAG);
        TYPES = Collections.unmodifiableMap(types);
        
        Map<String,Class> tags = new HashMap<String,Class>();
        for (Map.Entry<Class,String> me: types.entrySet()) {
            tags.put(me.getValue(), me.getKey());
        }
        TAGS = Collections.unmodifiableMap(tags);
    }
    
    

    /**
     * Static utility library.
     */
    private KeyTypes() {
    }


    /**
     * Returns true if the given type is a simple type.
     * 
     * @param type   the type to test
     * @return   true if the type is a simple type
     */
    public static boolean isSimple(Class type) {
        if (Enum.class.isAssignableFrom(type)) {
            return true;
        }
        return TYPES.keySet().contains(type);
    }


    /**
     * Returns the type tag for the given simple type.  Or, returns null if
     * the given type is not simple.
     *  
     * @param type   the type whose tag to return
     * @return   the tag for that type
     */
    public static String getSimpleTypeTag(Class type) {
        if (Enum.class.isAssignableFrom(type)) {
            return ENUM_TAG;
        }
        return TYPES.get(type);
    }
    

    /**
     * Returns the Java class corresponding to the given type tag.  Or, 
     * returns null if the given tag is not a simple type tag.
     * 
     * @param tag   the tag whose class to return
     * @return   the type for that tag
     */
    public static Class getSimpleType(String tag) {
        return TAGS.get(tag);
    }
    
    
    /**
     * Converts a string to an object of the given type.  May raise 
     * arbitrary exceptions; these will be converted to IllegalArgumentException
     * so that they can be caught.
     * 
     * @param type    the type of the object to return
     * @param value   a string form of that object
     * @return    the parsed object
     */
    private static Object fromString2(Class type, String value) {
        if (type == Enum.class) {
            return parseEnum(value);
        }
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
    

    /**
     * Parses an enum value.  
     * 
     * @param value  the string form of the enum value
     * @return   the enum value
     */
    private static Object parseEnum(String value) {
        int p = value.indexOf('-');
        if (p < 0) {
            throw new IllegalArgumentException(value + 
                    " cannot be parsed as enum.");
        }
        String cname = value.substring(0, p);
        String field = value.substring(p + 1);
        try {
            Class c = Class.forName(cname);
            @SuppressWarnings("unchecked")
            Object result = Enum.valueOf(c, field);
            return result;
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }

    }

    
    /**
     * Returns the string form of a simple type value.  Enumeration values 
     * are returned as the name of the enumeration's class, followed by a
     * dash, followed by the name of the enumeration value.  For instance,
     * {@link Thread.State#NEW} would be returned as 
     * <code>java.lang.Thread$State-NEW</code>.
     * 
     * All other simple types are converted to strings using their 
     * {@link Object#toString()} methods.
     * 
     * @param simple   the object to convert
     * @return   the string form of that object
     */
    public static String toString(Object simple) {
        if (simple == null) {
            return "";
        }
        if (!isSimple(simple.getClass())) {
            throw new IllegalArgumentException();
        }
        if (simple instanceof Enum) {
            return simple.getClass().getName() + "-" + simple.toString();
        }
        return simple.toString();
    }


    /**
     * Converts a string form of a simple type into a Java object.  
     * 
     * <p>The following conversion rules are followed:
     * 
     * <ul>
     * <li>Byte, Double, Float, Integer, Long and Short are parsed using 
     * the <code>decode</code> method of the given type.  This allows 
     * Integers to be expressed in hexadecimal form, for instance.</li>
     * <li>Booleans are parsed using {@link Boolean#parseBoolean}.</li>
     * <li>BigInteger and BigDecimal have the given string passed to the
     * appropriate constructor.</li>
     * <li>Pattern uses {@link Pattern#compile(String)} with the given string
     * to form the pattern.</li>
     * <li>Enums must have strings in the form described by 
     * {@link #toString(Object)}.  This string is parsed to get the class
     * and field name of the enum constant, and that constant is then returned.
     * </ul>
     * 
     * If anything goes wrong, this method raises an IllegalArgumentException.
     * 
     * @param simpleType   the simple type to return
     * @param value    the string value to convert to an object
     * @return   the object value
     * @throws IllegalArgumentException   if the given string cannot be 
     *    parsed as the given type
     */
    public static Object fromString(Class simpleType, String value) {
        try {
            return fromString2(simpleType, value);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
    
    

}
