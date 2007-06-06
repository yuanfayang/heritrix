/* Copyright (C) 2006 Internet Archive.
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
 * Types.java
 * Created on October 24, 2006
 *
 * $Header$
 */
package org.archive.settings.jmx;

import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;


public class Types {

    final public static ArrayType STRING_ARRAY;
    
    final public static CompositeType SET_DATA;

    final public static CompositeType SET_RESULT;

    final public static ArrayType SET_RESULT_ARRAY;
    
    final public static ArrayType SET_DATA_ARRAY;
    
    final public static CompositeType GET_DATA;
    
    final public static ArrayType GET_DATA_ARRAY;

    static {
        try {
            STRING_ARRAY = new ArrayType(1, SimpleType.STRING);
            
            SET_DATA = new CompositeType(
                    "set_data", 
                    "A path/value pair.", 
                    new String[] { "type", "path", "value" },
                    new String[] { 
                            "The data type of the value.", // FIXME: List choices
                            "The path to a module.", 
                            "The new value for that key in that module." }, 
                    new OpenType[] {
                            SimpleType.STRING,
                            SimpleType.STRING, 
                            SimpleType.STRING });

            SET_RESULT = new CompositeType(
                    "set_result", 
                    "The result of a set operation.", 
                    new String[] { "type", "path", "value", "error" },
                    new String[] {
                            "The data type of the value.", // FIXME: List choices
                            "The path to a setting.", 
                            "The new value for that setting.",
                            "A message describing an error while changing the setting."
                    }, 
                    new OpenType[] {
                            SimpleType.STRING,
                            SimpleType.STRING,
                            SimpleType.STRING,
                            SimpleType.STRING });

            SET_DATA_ARRAY = new ArrayType(1, SET_DATA);
            
            GET_DATA = new CompositeType(
                    "get_data",
                    "A path/value pair.",
                    new String[] { "path", "sheets", "value", "type" },
                    new String[] {
                            "The path to the value.",
                            "The sheets that led to the value.",
                            "The value for the path.",
                            "The type of the value."
                    },
                    new OpenType[] { 
                            SimpleType.STRING, 
                            STRING_ARRAY, 
                            SimpleType.STRING,
                            SimpleType.STRING }
                    );
            
            GET_DATA_ARRAY = new ArrayType(1, GET_DATA);
            
            SET_RESULT_ARRAY = new ArrayType(1, SET_RESULT);

        } catch (OpenDataException e) {
            throw new IllegalStateException(e);
        }
    }
    
    
    public static CompositeDataSupport composite(
            CompositeType type,
            String[] names,
            Object[] values) {
        try {
            return new CompositeDataSupport(type, names, values);
        } catch (OpenDataException e) {
            throw new IllegalArgumentException(e);
        }
    }


    public static CompositeData makeSetResult(
            String type,
            String path,
            String value,
            String error) {
        CompositeData result = composite(SET_RESULT,
                new String[] { "type", "path", "value", "error" },
                new Object[] { type, path, value, error });
        return result;
    }

    
    public static void main(String args[]) {
        
    }
}
