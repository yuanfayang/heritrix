/* OpenTypes
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


import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

import javax.management.ObjectName;
import javax.management.openmbean.ArrayType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;


/**
 * Static utility methods for dealing with {@link OpenType}.
 * 
 * @author pjack
 */
public class OpenTypes {


    /** Do not construct. */
    private OpenTypes() {
    }

    
    /**
     * Converts a class to its open type.  The given class must be listed 
     * in {@link OpenType#ALLOWED_CLASSNAMES}.
     * 
     * @param c  the class whose open type to return
     * @return   the open type for that class
     * @throws  IllegalArgumentException   if the given class is not a 
     *   valid open type
     */
    public static OpenType toOpenType(Class c) {
        try {
            return OpenTypes.innerToOpenType(c);
        } catch (OpenDataException e) {
            throw new IllegalStateException(e);
        }
    }

    private static OpenType innerToOpenType(Class c) throws OpenDataException {
        if (c.isArray()) {
            OpenType element = toOpenType(c.getComponentType());
            return new ArrayType(1, element);
        }
    
        if (c == Boolean.TYPE) return SimpleType.BOOLEAN;
        if (c == Integer.TYPE) return SimpleType.INTEGER;
        if (c == Double.TYPE) return SimpleType.DOUBLE;
        if (c == Float.TYPE) return SimpleType.FLOAT;
        if (c == Character.TYPE) return SimpleType.CHARACTER;
        if (c == Short.TYPE) return SimpleType.SHORT;
        if (c == Byte.TYPE) return SimpleType.BYTE;
        if (c == Long.TYPE) return SimpleType.LONG;
        if (c == Void.TYPE) return SimpleType.VOID;
    
        if (c == String.class) return SimpleType.STRING;
        if (c == Date.class) return SimpleType.DATE;
        if (c == BigDecimal.class) return SimpleType.BIGDECIMAL;
        if (c == BigInteger.class) return SimpleType.BIGINTEGER;
        if (c == ObjectName.class) return SimpleType.OBJECTNAME;
    
        if (c == Boolean.class) return SimpleType.BOOLEAN;
        if (c == Integer.class) return SimpleType.INTEGER;
        if (c == Double.class) return SimpleType.DOUBLE;
        if (c == Float.class) return SimpleType.FLOAT;
        if (c == Character.class) return SimpleType.CHARACTER;
        if (c == Short.class) return SimpleType.SHORT;
        if (c == Byte.class) return SimpleType.BYTE;
        if (c == Long.class) return SimpleType.LONG;
        if (c == Void.class) return SimpleType.VOID;
    
        throw new IllegalArgumentException(c.getName() + " is not an open type.");
    }


    /**
     * Parses a string value of a simple open type.
     * 
     * @param c       a class corresponding to a simple open type
     * @param value   the string value to parse
     * @return   the parsed value
     * @throws   IllegalArgumentException   if the given class is not 
     *                                      a simple open type
     * @throws   NumberFormatException    if the given value cannot be parsed
     *                                    into a numeric type
     */
    public static Comparable parse(Class c, String value) {
        if (c == String.class) {
            return value;
        }
        if (value.equals("")) {
            return null;
        }
        if ((c == Boolean.class) || (c == Boolean.TYPE)) {
            return Boolean.parseBoolean(value);
        }
        if ((c == Byte.class) || (c == Byte.TYPE)) {
            return Byte.parseByte(value);
        }
        if ((c == Character.class) || (c == Character.TYPE)) {
            if (value.length() != 1) {
                throw new IllegalStateException();
            }
            return value.charAt(0);
        }
        if ((c == Double.class) || (c == Double.TYPE)) {
            return Double.parseDouble(value);
        }
        if ((c == Float.class) || (c == Float.TYPE)) {
            return Float.parseFloat(value);
        }
        if ((c == Integer.class) || (c == Integer.TYPE)) {
            return Integer.parseInt(value);
        }
        if ((c == Long.class) || (c == Long.TYPE)) {
            return Long.parseLong(value);
        }
        if ((c == Short.class) || (c == Short.TYPE)) {
            return Short.parseShort(value);
        }
        if (c == BigInteger.class) {
            return new BigInteger(value);
        }
        if (c == BigDecimal.class) {
            return new BigDecimal(value);
        }
        throw new IllegalArgumentException(c + " is not a simple open type.");
    }

}
